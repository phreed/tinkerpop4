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
package org.apache.tinkerpop4.gremlin.process.computer.traversal

import org.apache.tinkerpop4.gremlin.process.computer.Memory

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class MemoryTraversalSideEffects : TraversalSideEffects {
    private var sideEffects: TraversalSideEffects? = null
    private var memory: Memory? = null
    private var phase: ProgramPhase? = null

    private constructor() {
        // for serialization
    }

    constructor(sideEffects: TraversalSideEffects?) {
        this.sideEffects = sideEffects
        memory = null
    }

    fun getSideEffects(): TraversalSideEffects? {
        return sideEffects
    }

    @Override
    operator fun set(key: String?, value: Object?) {
        sideEffects.set(key, value)
        if (null != memory) memory.set(key, value)
    }

    @Override
    @Throws(IllegalArgumentException::class)
    operator fun <V> get(key: String?): V {
        return if (null != memory && memory.exists(key)) memory.get(key) else sideEffects.get(key)
    }

    @Override
    fun remove(key: String?) {
        sideEffects.remove(key)
    }

    @Override
    fun keys(): Set<String> {
        return sideEffects.keys()
    }

    @Override
    fun add(key: String?, value: Object?) {
        if (phase.workerState()) memory.add(key, value) else memory.set(
            key,
            sideEffects.getReducer(key).apply(memory.get(key), value)
        )
    }

    @Override
    fun <V> register(key: String?, initialValue: Supplier<V>?, reducer: BinaryOperator<V>?) {
        sideEffects.register(key, initialValue, reducer)
    }

    @Override
    fun <V> registerIfAbsent(key: String?, initialValue: Supplier<V>?, reducer: BinaryOperator<V>?) {
        sideEffects.registerIfAbsent(key, initialValue, reducer)
    }

    @Override
    fun <V> getReducer(key: String?): BinaryOperator<V> {
        return sideEffects.getReducer(key)
    }

    @Override
    fun <V> getSupplier(key: String?): Supplier<V> {
        return sideEffects.getSupplier(key)
    }

    @Override
    fun <S> setSack(initialValue: Supplier<S>?, splitOperator: UnaryOperator<S>?, mergeOperator: BinaryOperator<S>?) {
        sideEffects.setSack(initialValue, splitOperator, mergeOperator)
    }

    @Override
    fun <S> getSackInitialValue(): Supplier<S> {
        return sideEffects.getSackInitialValue()
    }

    @Override
    fun <S> getSackSplitter(): UnaryOperator<S> {
        return sideEffects.getSackSplitter()
    }

    @Override
    fun <S> getSackMerger(): BinaryOperator<S> {
        return sideEffects.getSackMerger()
    }

    @Override
    fun clone(): TraversalSideEffects {
        return try {
            val clone = super.clone() as MemoryTraversalSideEffects
            clone.sideEffects = sideEffects.clone()
            clone
        } catch (e: CloneNotSupportedException) {
            throw IllegalStateException(e.getMessage(), e)
        }
    }

    @Override
    fun mergeInto(sideEffects: TraversalSideEffects?) {
        this.sideEffects.mergeInto(sideEffects)
    }

    fun storeSideEffectsInMemory() {
        if (phase.workerState()) sideEffects.forEach(memory::add) else sideEffects.forEach(memory::set)
    }

    companion object {
        fun setMemorySideEffects(traversal: Traversal.Admin<*, *>, memory: Memory?, phase: ProgramPhase?) {
            val sideEffects: TraversalSideEffects = traversal.getSideEffects()
            if (sideEffects !is MemoryTraversalSideEffects) {
                traversal.setSideEffects(MemoryTraversalSideEffects(sideEffects))
            }
            val memoryTraversalSideEffects = traversal.getSideEffects() as MemoryTraversalSideEffects
            memoryTraversalSideEffects.memory = memory
            memoryTraversalSideEffects.phase = phase
        }

        fun getMemorySideEffectsPhase(traversal: Traversal.Admin<*, *>): ProgramPhase? {
            return if (traversal.getSideEffects() is MemoryTraversalSideEffects) (traversal.getSideEffects() as MemoryTraversalSideEffects).phase else null
        }

        fun getMemoryComputeKeys(traversal: Traversal.Admin<*, *>): Set<MemoryComputeKey> {
            val keys: Set<MemoryComputeKey> = HashSet()
            val sideEffects: TraversalSideEffects =
                if (traversal.getSideEffects() is MemoryTraversalSideEffects) (traversal.getSideEffects() as MemoryTraversalSideEffects).sideEffects else traversal.getSideEffects()
            sideEffects.keys().stream()
                .forEach { key -> keys.add(MemoryComputeKey.of(key, sideEffects.getReducer(key), true, false)) }
            return keys
        }
    }
}