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
import org.apache.tinkerpop.gremlin.process.computer.*
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerCat
import org.apache.tinkerpop.gremlin.process.computer.GraphComputer.ResultGraph
import org.apache.tinkerpop.gremlin.process.computer.GraphComputer.Persist
import java.util.concurrent.ThreadFactory
import org.apache.tinkerpop.gremlin.tinkercat.process.computer.TinkerCatComputer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import org.apache.tinkerpop.gremlin.process.computer.util.GraphComputerHelper
import org.apache.tinkerpop.gremlin.tinkercat.process.computer.TinkerCatComputerView
import org.apache.tinkerpop.gremlin.process.traversal.util.TraversalInterruptedException
import org.apache.tinkerpop.gremlin.process.computer.util.ComputerGraph
import org.apache.tinkerpop.gremlin.process.computer.util.DefaultComputerResult
import java.lang.InterruptedException
import java.lang.RuntimeException
import org.apache.tinkerpop.gremlin.structure.util.StringFactory
import org.apache.tinkerpop.gremlin.process.traversal.TraversalStrategies
import org.apache.tinkerpop.gremlin.process.computer.traversal.strategy.optimization.GraphFilterStrategy
import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.structure.Edge
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerHelper
import java.lang.Exception
import java.util.*
import java.util.concurrent.Future

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class TinkerCatComputer(private val graph: TinkerCat) : GraphComputer {
    private var resultGraph: ResultGraph? = null
    private var persist: Persist? = null
    private var vertexProgram: VertexProgram<*>? = null
    private var memory: TinkerMemory? = null
    private val messageBoard = TinkerMessageBoard<Any?>()
    private var executed = false
    private val mapReducers: MutableSet<MapReduce<Any, Any, *, *, *>> = HashSet()
    private var workers = Runtime.getRuntime().availableProcessors()
    private val graphFilter = GraphFilter()
    private val threadFactoryBoss: ThreadFactory = BasicThreadFactory.Builder().namingPattern(
        TinkerCatComputer::class.java.simpleName + "-boss"
    ).build()

    /**
     * An `ExecutorService` that schedules up background work. Since a [GraphComputer] is only used once
     * for a [VertexProgram] a single threaded executor is sufficient.
     */
    private val computerService = Executors.newSingleThreadExecutor(threadFactoryBoss)
    override fun result(resultGraph: ResultGraph): GraphComputer {
        this.resultGraph = resultGraph
        return this
    }

    override fun persist(persist: Persist): GraphComputer {
        this.persist = persist
        return this
    }

    override fun program(vertexProgram: VertexProgram<*>?): GraphComputer {
        this.vertexProgram = vertexProgram
        return this
    }

    override fun mapReduce(mapReduce: MapReduce<Any, Any, *, *, *>): GraphComputer {
        mapReducers.add(mapReduce)
        return this
    }

    override fun workers(workers: Int): GraphComputer {
        this.workers = workers
        return this
    }

    override fun vertices(vertexFilter: Traversal<Vertex, Vertex>): GraphComputer {
        graphFilter.setVertexFilter(vertexFilter)
        return this
    }

    override fun edges(edgeFilter: Traversal<Vertex, Edge>): GraphComputer {
        graphFilter.setEdgeFilter(edgeFilter)
        return this
    }

    override fun submit(): Future<ComputerResult> {
        // a graph computer can only be executed once
        if (executed) throw GraphComputer.Exceptions.computerHasAlreadyBeenSubmittedAVertexProgram() else executed =
            true
        // it is not possible execute a computer if it has no vertex program nor mapreducers
        if (null == vertexProgram && mapReducers.isEmpty()) throw GraphComputer.Exceptions.computerHasNoVertexProgramNorMapReducers()
        // it is possible to run mapreducers without a vertex program
        if (null != vertexProgram) {
            GraphComputerHelper.validateProgramOnComputer(this, vertexProgram)
            mapReducers.addAll(vertexProgram!!.mapReducers)
        }
        // get the result graph and persist state to use for the computation
        resultGraph = GraphComputerHelper.getResultGraphState(
            Optional.ofNullable(vertexProgram), Optional.ofNullable(
                resultGraph
            )
        )
        persist = GraphComputerHelper.getPersistState(
            Optional.ofNullable(vertexProgram), Optional.ofNullable(
                persist
            )
        )
        if (!features().supportsResultGraphPersistCombination(
                resultGraph,
                persist
            )
        ) throw GraphComputer.Exceptions.resultGraphPersistCombinationNotSupported(
            resultGraph, persist
        )
        // ensure requested workers are not larger than supported workers
        if (workers > features().maxWorkers) throw GraphComputer.Exceptions.computerRequiresMoreWorkersThanSupported(
            workers, features().maxWorkers
        )

        // initialize the memory
        memory = TinkerMemory(vertexProgram, mapReducers)
        val result = computerService.submit<ComputerResult> {
            val time = System.currentTimeMillis()
            val view = TinkerHelper.createGraphComputerView(
                graph,
                graphFilter,
                if (null != vertexProgram) vertexProgram!!.vertexComputeKeys else emptySet()
            )
            val workers = TinkerWorkerPool(graph, memory, workers)
            try {
                if (null != vertexProgram) {
                    // execute the vertex program
                    vertexProgram!!.setup(memory)
                    while (true) {
                        if (Thread.interrupted()) throw TraversalInterruptedException()
                        memory!!.completeSubRound()
                        workers.setVertexProgram(vertexProgram)
                        workers.executeVertexProgram { vertices: Iterator<Vertex?>,
                                                       vertexProgram: VertexProgram<*>,
                                                       workerMemory: TinkerWorkerMemory ->
                            vertexProgram.workerIterationStart(workerMemory.asImmutable())
                            while (vertices.hasNext()) {
                                val vertex = vertices.next()
                                if (Thread.interrupted()) throw TraversalInterruptedException()
                                val vertexProgrammer = ComputerGraph.vertexProgram(vertex, vertexProgram)
                                val messageCombiner = vertexProgram.messageCombiner as Optional<MessageCombiner<Any?>>
                                val messenger = TinkerMessenger<Any?>(vertex, messageBoard, messageCombiner)
                                /* TODO: KOTLIN: messenger is not typed correctly
                                vertexProgram.execute(vertexProgrammer, messenger, workerMemory)
                                 */
                            }
                            vertexProgram.workerIterationEnd(workerMemory.asImmutable())
                            workerMemory.complete()
                        }
                        messageBoard.completeIteration()
                        memory!!.completeSubRound()
                        if (vertexProgram!!.terminate(memory)) {
                            memory!!.incrIteration()
                            break
                        } else {
                            memory!!.incrIteration()
                        }
                    }
                    view.complete() // drop all transient vertex compute keys
                }

                // execute mapreduce jobs
                for (mapReduce in mapReducers) {
                    val mapEmitter = TinkerMapEmitter<Any, Any>(mapReduce.doStage(MapReduce.Stage.REDUCE))
                    val vertices = SynchronizedIterator(
                        graph.vertices()
                    )
                    workers.setMapReduce(mapReduce)
                    workers.executeMapReduce { workerMapReduce ->
                        workerMapReduce.workerStart(MapReduce.Stage.MAP)
                        while (true) {
                            if (Thread.interrupted()) throw TraversalInterruptedException()
                            val vertex = vertices.next() ?: break
                            workerMapReduce.map(ComputerGraph.mapReduce(vertex), mapEmitter)
                        }
                        workerMapReduce.workerEnd(MapReduce.Stage.MAP)
                    }
                    // sort results if a map output sort is defined
                    mapEmitter.complete(mapReduce)

                    // no need to run combiners as this is single machine
                    if (mapReduce.doStage(MapReduce.Stage.REDUCE)) {
                        val reduceEmitter = TinkerReduceEmitter<Any, Any>()
                        val iterator = mapEmitter.reduceMap.entries.iterator()
                        if (iterator != null) {
                            val keyValues = SynchronizedIterator(iterator)
                            workers.executeMapReduce { workerMapReduce ->
                                workerMapReduce.workerStart(MapReduce.Stage.REDUCE)
                                while (true) {
                                    if (Thread.interrupted()) throw TraversalInterruptedException()
                                    val (key, value) = keyValues.next() ?: break
                                    workerMapReduce.reduce(key, value.iterator(), reduceEmitter)
                                }
                                workerMapReduce.workerEnd(MapReduce.Stage.REDUCE)
                            }
                            /* TODO: KOTLIN: mapReduce is not typed correctly
                            reduceEmitter.complete(mapReduce) // sort results if a reduce output sort is defined
                            mapReduce.addResultToMemory(memory, reduceEmitter.reduceQueue.iterator())
                             */
                        }
                    } else {
                        /* TODO: KOTLIN:
                        mapReduce.addResultToMemory(memory, mapEmitter.mapQueue.iterator())
                         */
                    }
                }
                // update runtime and return the newly computed graph
                memory!!.runtime = System.currentTimeMillis() - time
                memory!!.complete() // drop all transient properties and set iteration
                // determine the resultant graph based on the result graph/persist state
                val resultGraph = view.processResultGraphPersist(resultGraph, persist)
                TinkerHelper.dropGraphComputerView(graph) // drop the view from the original source graph
                return@submit DefaultComputerResult(resultGraph, memory!!.asImmutable())
            } catch (ie: InterruptedException) {
                workers.closeNow()
                throw TraversalInterruptedException()
            } catch (ex: Exception) {
                workers.closeNow()
                throw RuntimeException(ex)
            } finally {
                workers.close()
            }
        }
        computerService.shutdown()
        return result
    }

    override fun toString(): String {
        return StringFactory.graphComputerString(this)
    }

    private class SynchronizedIterator<V>(private val iterator: Iterator<V>) {
        @Synchronized
        operator fun next(): V? {
            return if (iterator.hasNext()) iterator.next() else null
        }
    }

    override fun features(): GraphComputer.Features {
        return object : GraphComputer.Features {
            override fun getMaxWorkers(): Int {
                return Runtime.getRuntime().availableProcessors()
            }

            override fun supportsVertexAddition(): Boolean {
                return false
            }

            override fun supportsVertexRemoval(): Boolean {
                return false
            }

            override fun supportsVertexPropertyRemoval(): Boolean {
                return false
            }

            override fun supportsEdgeAddition(): Boolean {
                return false
            }

            override fun supportsEdgeRemoval(): Boolean {
                return false
            }

            override fun supportsEdgePropertyAddition(): Boolean {
                return false
            }

            override fun supportsEdgePropertyRemoval(): Boolean {
                return false
            }
        }
    }

    companion object {
        init {
            // GraphFilters are expensive w/ TinkerCatComputer as everything is already in memory
            TraversalStrategies.GlobalCache.registerStrategies(
                TinkerCatComputer::class.java,
                TraversalStrategies.GlobalCache.getStrategies(GraphComputer::class.java).clone().removeStrategies(
                    GraphFilterStrategy::class.java
                )
            )
        }
    }
}