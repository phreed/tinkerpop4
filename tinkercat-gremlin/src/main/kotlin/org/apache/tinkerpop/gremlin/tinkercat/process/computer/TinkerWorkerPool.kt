/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.tinkerpop.gremlin.tinkercat.process.computer

import org.apache.commons.lang3.concurrent.BasicThreadFactory
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerCat
import java.lang.AutoCloseable
import java.util.concurrent.ExecutorService
import java.util.concurrent.CompletionService
import org.apache.tinkerpop.gremlin.process.computer.util.VertexProgramPool
import org.apache.tinkerpop.gremlin.process.computer.util.MapReducePool
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.ExecutorCompletionService
import org.apache.tinkerpop.gremlin.process.computer.VertexProgram
import org.apache.tinkerpop.gremlin.process.computer.MapReduce
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerHelper
import org.apache.tinkerpop.gremlin.util.function.TriConsumer
import java.lang.Exception
import kotlin.Throws
import java.lang.InterruptedException
import java.lang.IllegalStateException
import java.util.*
import java.util.function.Consumer

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class TinkerWorkerPool(graph: TinkerCat, memory: TinkerMemory?, private val numberOfWorkers: Int) : AutoCloseable {
    private val workerPool: ExecutorService
    private val completionService: CompletionService<Any?>
    private var vertexProgramPool: VertexProgramPool? = null
    private var mapReducePool: MapReducePool? = null
    private val workerMemoryPool: Queue<TinkerWorkerMemory> = ConcurrentLinkedQueue()
    private val workerVertices: MutableList<MutableList<Vertex>> = ArrayList()

    init {
        workerPool = Executors.newFixedThreadPool(numberOfWorkers, THREAD_FACTORY_WORKER)
        completionService = ExecutorCompletionService(workerPool)
        for (i in 0 until numberOfWorkers) {
            workerMemoryPool.add(TinkerWorkerMemory(memory!!))
            workerVertices.add(ArrayList())
        }
        var batchSize = TinkerHelper.getVertices(graph).size / numberOfWorkers
        if (0 == batchSize) batchSize = 1
        var counter = 0
        var index = 0
        var currentWorkerVertices = workerVertices[index]
        val iterator = graph.vertices()
        while (iterator.hasNext()) {
            val vertex = iterator.next()
            if (counter++ < batchSize || index == workerVertices.size - 1) {
                currentWorkerVertices.add(vertex)
            } else {
                currentWorkerVertices = workerVertices[++index]
                currentWorkerVertices.add(vertex)
                counter = 1
            }
        }
    }

    fun setVertexProgram(vertexProgram: VertexProgram<*>?) {
        vertexProgramPool = VertexProgramPool(vertexProgram, numberOfWorkers)
    }

    fun setMapReduce(mapReduce: MapReduce<*, *, *, *, *>?) {
        mapReducePool = MapReducePool(mapReduce, numberOfWorkers)
    }

    @Throws(InterruptedException::class)
    fun executeVertexProgram(worker: TriConsumer<Iterator<Vertex?>?, VertexProgram<*>?, TinkerWorkerMemory?>) {
        for (i in 0 until numberOfWorkers) {
            completionService.submit {
                val vp = vertexProgramPool!!.take()
                val workerMemory = workerMemoryPool.poll()
                val vertices: List<Vertex> = workerVertices[i]
                worker.accept(vertices.iterator(), vp, workerMemory)
                vertexProgramPool!!.offer(vp)
                workerMemoryPool.offer(workerMemory)
                null
            }
        }
        for (i in 0 until numberOfWorkers) {
            try {
                completionService.take().get()
            } catch (ie: InterruptedException) {
                throw ie
            } catch (e: Exception) {
                throw IllegalStateException(e.message, e)
            }
        }
    }

    @Throws(InterruptedException::class)
    fun executeMapReduce(worker: Consumer<MapReduce<*, *, *, *, *>?>) {
        for (i in 0 until numberOfWorkers) {
            completionService.submit {
                val mr = mapReducePool!!.take()
                worker.accept(mr)
                mapReducePool!!.offer(mr)
                null
            }
        }
        for (i in 0 until numberOfWorkers) {
            try {
                completionService.take().get()
            } catch (ie: InterruptedException) {
                throw ie
            } catch (e: Exception) {
                throw IllegalStateException(e.message, e)
            }
        }
    }

    @Throws(Exception::class)
    fun closeNow() {
        workerPool.shutdownNow()
    }

    @Throws(Exception::class)
    override fun close() {
        workerPool.shutdown()
    }

    companion object {
        private val THREAD_FACTORY_WORKER = BasicThreadFactory.Builder().namingPattern("tinker-worker-%d").build()
    }
}