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
package org.apache.tinkerpop4.gremlin.process.computer.util

import org.apache.tinkerpop4.gremlin.process.computer.Memory

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class VertexProgramPool(vertexProgram: VertexProgram, poolSize: Int) {
    private val pool: LinkedBlockingQueue<VertexProgram<*>>

    init {
        pool = LinkedBlockingQueue(poolSize)
        while (pool.remainingCapacity() > 0) {
            pool.add(vertexProgram.clone())
        }
    }

    fun take(): VertexProgram {
        return try {
            pool.poll(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {
            throw IllegalStateException(e.getMessage(), e)
        }
    }

    fun offer(vertexProgram: VertexProgram<*>?) {
        try {
            pool.offer(vertexProgram, TIMEOUT_MS, TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {
            throw IllegalStateException(e.getMessage(), e)
        }
    }

    @kotlin.jvm.Synchronized
    fun workerIterationStart(memory: Memory?) {
        for (vertexProgram in pool) {
            vertexProgram.workerIterationStart(memory)
        }
    }

    @kotlin.jvm.Synchronized
    fun workerIterationEnd(memory: Memory?) {
        for (vertexProgram in pool) {
            vertexProgram.workerIterationEnd(memory)
        }
    }

    companion object {
        private const val TIMEOUT_MS = 10000
    }
}