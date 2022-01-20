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

import org.apache.tinkerpop4.gremlin.process.traversal.Operator

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class DefaultTraversalSideEffects : TraversalSideEffects {
    protected var keys: Set<String> = HashSet()
    protected var objectMap: Map<String, Object> = HashMap()
    protected var supplierMap: Map<String, Supplier?> = HashMap()
    protected var reducerMap: Map<String, BinaryOperator?> = HashMap()
    protected var sackSplitOperator: UnaryOperator? = null
    protected var sackMergeOperator: BinaryOperator? = null
    protected var sackInitialValue: Supplier? = null

    /**
     * {@inheritDoc}
     */
    @Override
    fun exists(key: String): Boolean {
        return keys.contains(key)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Throws(IllegalArgumentException::class)
    operator fun <V> get(key: String): V {
        val value = objectMap[key] as V?
        return if (null != value) value else {
            val v: V = getSupplier<V>(key).get()
            objectMap.put(key, v)
            v
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Throws(IllegalArgumentException::class)
    operator fun set(key: String, value: Object?) {
        if (!keys.contains(key)) throw TraversalSideEffects.Exceptions.sideEffectKeyDoesNotExist(key)
        objectMap.put(key, value)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Throws(IllegalArgumentException::class)
    fun add(key: String, value: Object?) {
        this[key] = getReducer<Any>(key).apply(this[key], value)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun <V> register(key: String?, initialValue: Supplier<V>?, reducer: BinaryOperator<V>?) {
        SideEffectHelper.validateSideEffectKey(key)
        keys.add(key)
        if (null != initialValue) supplierMap.put(key, initialValue)
        if (null != reducer) reducerMap.put(key, reducer)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun <V> registerIfAbsent(key: String, initialValue: Supplier<V>?, reducer: BinaryOperator<V>?) {
        SideEffectHelper.validateSideEffectKey(key)
        keys.add(key)
        if (null == supplierMap[key] && null != initialValue) {
            supplierMap.put(key, initialValue)
        }
        if (null == reducerMap[key] && null != reducer) {
            reducerMap.put(key, reducer)
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Throws(IllegalArgumentException::class)
    fun <V> getReducer(key: String): BinaryOperator<V> {
        if (!keys.contains(key)) throw TraversalSideEffects.Exceptions.sideEffectKeyDoesNotExist(key)
        return reducerMap.getOrDefault(key, Operator.assign)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Throws(IllegalArgumentException::class)
    fun <V> getSupplier(key: String): Supplier<V> {
        return supplierMap[key]
            ?: throw TraversalSideEffects.Exceptions.sideEffectKeyDoesNotExist(key)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun <S> setSack(initialValue: Supplier<S>?, splitOperator: UnaryOperator<S>?, mergeOperator: BinaryOperator<S>?) {
        sackInitialValue = initialValue
        sackSplitOperator = splitOperator
        sackMergeOperator = mergeOperator
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun <S> getSackInitialValue(): Supplier<S>? {
        return sackInitialValue
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun <S> getSackSplitter(): UnaryOperator<S>? {
        return sackSplitOperator
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun <S> getSackMerger(): BinaryOperator<S>? {
        return sackMergeOperator
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun remove(key: String?) {
        objectMap.remove(key)
        supplierMap.remove(key)
        reducerMap.remove(key)
        keys.remove(key)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun keys(): Set<String> {
        return Collections.unmodifiableSet(keys)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun mergeInto(sideEffects: TraversalSideEffects) {
        for (key in keys) {
            sideEffects.registerIfAbsent(key, supplierMap[key], reducerMap[key])
            if (objectMap.containsKey(key)) sideEffects.set(key, objectMap[key])
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    override fun toString(): String {
        return StringFactory.traversalSideEffectsString(this)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("CloneDoesntDeclareCloneNotSupportedException")
    fun clone(): DefaultTraversalSideEffects {
        return try {
            val sideEffects = super.clone() as DefaultTraversalSideEffects
            sideEffects.keys = HashSet(keys)
            sideEffects.objectMap = HashMap(objectMap)
            sideEffects.supplierMap = HashMap(supplierMap)
            sideEffects.reducerMap = HashMap(reducerMap)
            sideEffects
        } catch (e: CloneNotSupportedException) {
            throw IllegalStateException(e.getMessage(), e)
        }
    }
}