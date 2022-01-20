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
package org.apache.tinkerpop4.gremlin.process.traversal.util

import org.apache.tinkerpop4.gremlin.process.traversal.TraversalSideEffects

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class EmptyTraversalSideEffects private constructor() : TraversalSideEffects {
    @Override
    @Throws(IllegalArgumentException::class)
    operator fun set(key: String?, value: Object?) {
        throw TraversalSideEffects.Exceptions.sideEffectKeyDoesNotExist(key)
    }

    @Override
    @Throws(IllegalArgumentException::class)
    operator fun <V> get(key: String?): V {
        throw TraversalSideEffects.Exceptions.sideEffectKeyDoesNotExist(key)
    }

    @Override
    @Throws(IllegalArgumentException::class)
    fun remove(key: String?) {
        throw TraversalSideEffects.Exceptions.sideEffectKeyDoesNotExist(key)
    }

    @Override
    fun keys(): Set<String> {
        return Collections.emptySet()
    }

    @Override
    @Throws(IllegalArgumentException::class)
    fun add(key: String?, value: Object?) {
        throw TraversalSideEffects.Exceptions.sideEffectKeyDoesNotExist(key)
    }

    @Override
    fun <V> register(key: String?, initialValue: Supplier<V>?, reducer: BinaryOperator<V>?) {
    }

    @Override
    fun <V> registerIfAbsent(key: String?, initialValue: Supplier<V>?, reducer: BinaryOperator<V>?) {
    }

    @Override
    @Throws(IllegalArgumentException::class)
    fun <V> getReducer(key: String?): BinaryOperator<V> {
        throw TraversalSideEffects.Exceptions.sideEffectKeyDoesNotExist(key)
    }

    @Override
    @Throws(IllegalArgumentException::class)
    fun <V> getSupplier(key: String?): Supplier<V> {
        throw TraversalSideEffects.Exceptions.sideEffectKeyDoesNotExist(key)
    }

    @Override
    fun <S> setSack(initialValue: Supplier<S>?, splitOperator: UnaryOperator<S>?, mergeOperator: BinaryOperator<S>?) {
    }

    @Override
    fun <S> getSackInitialValue(): Supplier<S>? {
        return null
    }

    @Override
    fun <S> getSackSplitter(): UnaryOperator<S>? {
        return null
    }

    @Override
    fun <S> getSackMerger(): BinaryOperator<S>? {
        return null
    }

    @SuppressWarnings("CloneDoesntCallSuperClone")
    @Override
    fun clone(): TraversalSideEffects {
        return this
    }

    @Override
    fun mergeInto(sideEffects: TraversalSideEffects?) {
    }

    companion object {
        private val INSTANCE = EmptyTraversalSideEffects()
        fun instance(): EmptyTraversalSideEffects {
            return INSTANCE
        }
    }
}