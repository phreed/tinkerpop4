/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.tinkerpop.gremlin.tinkercat.process.computer

import org.apache.tinkerpop.gremlin.process.computer.Memory
import java.util.function.BinaryOperator
import kotlin.Throws
import java.lang.IllegalArgumentException
import java.util.HashMap

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class TinkerWorkerMemory(private val mainMemory: TinkerMemory) : Memory.Admin {
    private val workerMemory: MutableMap<String, Any> = HashMap()
    private val reducers: MutableMap<String, BinaryOperator<Any>> = HashMap()

    init {
        for (key in mainMemory.memoryKeys.values) {
            reducers[key.key] = key.clone().reducer as BinaryOperator<Any>
        }
    }

    override fun keys(): Set<String> {
        return mainMemory.keys()
    }

    override fun incrIteration() {
        mainMemory.incrIteration()
    }

    override fun setIteration(iteration: Int) {
        mainMemory.iteration = iteration
    }

    override fun getIteration(): Int {
        return mainMemory.iteration
    }

    override fun setRuntime(runTime: Long) {
        mainMemory.runtime = runTime
    }

    override fun getRuntime(): Long {
        return mainMemory.runtime
    }

    override fun isInitialIteration(): Boolean {
        return mainMemory.isInitialIteration
    }

    @Throws(IllegalArgumentException::class)
    override fun <R> get(key: String): R {
        return mainMemory.get(key)
    }

    override fun set(key: String, value: Any) {
        mainMemory[key] = value
    }

    override fun add(key: String, value: Any) {
        mainMemory.checkKeyValue(key, value)
        val v = workerMemory[key]
        workerMemory[key] = if (null == v) value else reducers[key]!!.apply(v, value)
    }

    override fun toString(): String {
        return mainMemory.toString()
    }

    fun complete() {
        for ((key, value) in workerMemory) {
            mainMemory.add(key, value)
        }
        workerMemory.clear()
    }
}