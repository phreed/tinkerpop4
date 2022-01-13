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

import org.apache.tinkerpop.gremlin.process.computer.*
import org.apache.tinkerpop.gremlin.process.traversal.Operator
import java.util.concurrent.ConcurrentHashMap
import kotlin.Throws
import java.lang.IllegalArgumentException
import java.util.function.BiFunction
import org.apache.tinkerpop.gremlin.structure.util.StringFactory
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.stream.Collectors

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class TinkerMemory(vertexProgram: VertexProgram<*>?, mapReducers: Set<MapReduce<*, *, *, *, *>>) : Memory.Admin {
    @JvmField
    val memoryKeys: MutableMap<String, MemoryComputeKey<*>> = HashMap()
    var previousMap: MutableMap<String, Optional<Any>>
    var currentMap: MutableMap<String, Optional<Any>> = ConcurrentHashMap()
    private val iteration = AtomicInteger(0)
    private val runtime = AtomicLong(0L)
    private var inExecute = false

    init {
        // ConcurrentHashMap makes us use Optional since you cant store null in them as values (or keys)
        previousMap = ConcurrentHashMap()
        if (null != vertexProgram) {
            for (memoryComputeKey in vertexProgram.memoryComputeKeys) {
                memoryKeys[memoryComputeKey.key] = memoryComputeKey
            }
        }
        for (mapReduce in mapReducers) {
            memoryKeys[mapReduce.memoryKey] = MemoryComputeKey.of(mapReduce.memoryKey, Operator.assign, false, false)
        }
    }

    override fun keys(): Set<String> {
        return previousMap.keys.stream().filter { key: String ->
            !inExecute || memoryKeys[key]!!
                .isBroadcast
        }.collect(Collectors.toSet())
    }

    override fun incrIteration() {
        iteration.getAndIncrement()
    }

    override fun setIteration(iteration: Int) {
        this.iteration.set(iteration)
    }

    override fun getIteration(): Int {
        return iteration.get()
    }

    override fun setRuntime(runTime: Long) {
        runtime.set(runTime)
    }

    override fun getRuntime(): Long {
        return runtime.get()
    }

    fun complete() {
        iteration.decrementAndGet()
        previousMap = currentMap
        memoryKeys.values.stream().filter { obj: MemoryComputeKey<*> -> obj.isTransient }
            .forEach { computeKey: MemoryComputeKey<*> -> previousMap.remove(computeKey.key) }
    }

    fun completeSubRound() {
        previousMap = ConcurrentHashMap(currentMap)
        inExecute = !inExecute
    }

    override fun isInitialIteration(): Boolean {
        return getIteration() == 0
    }

    @Throws(IllegalArgumentException::class)
    override fun <R> get(key: String): R {
        if (!previousMap.containsKey(key)) throw Memory.Exceptions.memoryDoesNotExist(key)
        val o = previousMap[key]!!
        val r = o.orElse(null) as R
        return if (inExecute && !memoryKeys[key]!!.isBroadcast) throw Memory.Exceptions.memoryDoesNotExist(
            key
        ) else r
    }

    override fun set(key: String, value: Any) {
        checkKeyValue(key, value)
        if (inExecute) throw Memory.Exceptions.memorySetOnlyDuringVertexProgramSetUpAndTerminate(key)
        currentMap[key] = Optional.ofNullable(value)
    }

    override fun add(key: String, value: Any) {
        checkKeyValue(key, value)
        if (!inExecute) throw Memory.Exceptions.memoryAddOnlyDuringVertexProgramExecute(key)
        currentMap.compute(key, BiFunction { k: String?, v: Optional<Any>? ->
            Optional.ofNullable(
                if (null == v || !v.isPresent) value else memoryKeys[key]!!
                    .reducer.apply(v.get() as Nothing, value as Nothing)
            )
        })
    }

    override fun toString(): String {
        return StringFactory.memoryString(this)
    }

    fun checkKeyValue(key: String, value: Any?) {
        if (!memoryKeys.containsKey(key)) throw GraphComputer.Exceptions.providedKeyIsNotAMemoryComputeKey(key)
    }
}