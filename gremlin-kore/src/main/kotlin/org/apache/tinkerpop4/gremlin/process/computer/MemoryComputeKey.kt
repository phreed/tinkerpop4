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
package org.apache.tinkerpop4.gremlin.process.computer

import org.apache.tinkerpop4.gremlin.process.computer.util.MemoryHelper

/**
 * A `MemoryComputeKey` specifies what keys will be used by a [Memory] during a [GraphComputer] computation.
 * A MemoryComputeKey maintains a [BinaryOperator] which specifies how to reduce parallel values into a single value.
 * A MemoryComputeKey can be broadcasted and as such, the workers will receive mutations to the [Memory] value.
 * A MemoryComputeKey can be transient and thus, will not be accessible once the [GraphComputer] computation is complete.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class MemoryComputeKey<A> private constructor(
    val key: String,
    reducer: BinaryOperator<A>,
    isBroadcast: Boolean,
    isTransient: Boolean
) : Serializable, Cloneable {
    private var reducer: BinaryOperator<A>
    val isTransient: Boolean
    val isBroadcast: Boolean

    init {
        this.reducer = reducer
        this.isTransient = isTransient
        this.isBroadcast = isBroadcast
        MemoryHelper.validateKey(key)
    }

    fun getReducer(): BinaryOperator<A> {
        return reducer
    }

    @Override
    override fun hashCode(): Int {
        return key.hashCode()
    }

    @Override
    override fun equals(`object`: Object): Boolean {
        return `object` is MemoryComputeKey<*> && (`object` as MemoryComputeKey<*>).key.equals(key)
    }

    @Override
    fun clone(): MemoryComputeKey<A> {
        return try {
            val clone = super.clone() as MemoryComputeKey<A>
            try {
                val cloneMethod: Method = reducer.getClass().getMethod("clone")
                if (cloneMethod != null) clone.reducer = cloneMethod.invoke(reducer) as BinaryOperator<A>
            } catch (ignored: Exception) {
            }
            clone
        } catch (e: CloneNotSupportedException) {
            throw IllegalStateException(e.getMessage(), e)
        }
    }

    companion object {
        fun <A> of(
            key: String,
            reducer: BinaryOperator<A>,
            isBroadcast: Boolean,
            isTransient: Boolean
        ): MemoryComputeKey<A> {
            return MemoryComputeKey<Any>(key, reducer, isBroadcast, isTransient)
        }
    }
}