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

import org.apache.tinkerpop4.gremlin.process.computer.GraphComputer

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class MapMemory : Memory.Admin, Serializable {
    @get:Override
    @set:Override
    var runtime = 0L

    @get:Override
    @set:Override
    var iteration = -1
    private val memoryMap: Map<String, Object> = HashMap()
    private val memoryComputeKeys: Map<String, MemoryComputeKey> = HashMap()

    constructor() {}
    constructor(otherMemory: Memory) {
        otherMemory.keys().forEach { key -> memoryMap.put(key, otherMemory.get(key)) }
        iteration = otherMemory.getIteration()
    }

    fun addVertexProgramMemoryComputeKeys(vertexProgram: VertexProgram<*>) {
        vertexProgram.getMemoryComputeKeys().forEach { key -> memoryComputeKeys.put(key.getKey(), key) }
    }

    fun addMapReduceMemoryKey(mapReduce: MapReduce) {
        memoryComputeKeys.put(
            mapReduce.getMemoryKey(),
            MemoryComputeKey.of(mapReduce.getMemoryKey(), Operator.assign, false, false)
        )
    }

    @Override
    fun keys(): Set<String> {
        return memoryMap.keySet()
    }

    @Override
    @Throws(IllegalArgumentException::class)
    operator fun <R> get(key: String): R {
        val r = memoryMap[key] as R?
        return r ?: throw Memory.Exceptions.memoryDoesNotExist(key)
    }

    @Override
    operator fun set(key: String?, value: Object?) {
        // this.checkKeyValue(key, value);
        memoryMap.put(key, value)
    }

    @Override
    fun add(key: String, value: Object) {
        checkKeyValue(key, value)
        if (memoryMap.containsKey(key)) {
            val newValue: Object = memoryComputeKeys[key].getReducer().apply(memoryMap[key], value)
            memoryMap.put(key, newValue)
        } else {
            memoryMap.put(key, value)
        }
    }

    @Override
    override fun toString(): String {
        return StringFactory.memoryString(this)
    }

    @Override
    fun incrIteration() {
        iteration = iteration + 1
    }

    private fun checkKeyValue(key: String, value: Object) {
        if (!memoryComputeKeys.containsKey(key)) throw GraphComputer.Exceptions.providedKeyIsNotAMemoryComputeKey(key)
        MemoryHelper.validateValue(value)
    }
}