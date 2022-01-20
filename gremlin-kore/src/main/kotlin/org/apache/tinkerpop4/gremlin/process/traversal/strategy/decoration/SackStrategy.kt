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
package org.apache.tinkerpop4.gremlin.process.traversal.strategy.decoration

import org.apache.tinkerpop4.gremlin.process.traversal.Traversal

/**
 * The `SackStrategy` is used internal to the `withSack()` steps of [TraversalSource] and is not
 * typically constructed directly.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class SackStrategy private constructor(
    initialValue: Supplier?,
    splitOperator: UnaryOperator,
    mergeOperator: BinaryOperator
) : AbstractTraversalStrategy<DecorationStrategy?>(), DecorationStrategy {
    private val initialValue: Supplier
    private val splitOperator: UnaryOperator
    private val mergeOperator: BinaryOperator

    init {
        if (null == initialValue) throw IllegalArgumentException("The initial value of a sack can not be null")
        this.initialValue = initialValue
        this.splitOperator = splitOperator
        this.mergeOperator = mergeOperator
    }

    @Override
    fun apply(traversal: Traversal.Admin<*, *>) {
        if (traversal.isRoot()) traversal.getSideEffects().setSack(initialValue, splitOperator, mergeOperator)
    }

    class Builder<A> private constructor() {
        private var initialValue: Supplier<A>? = null
        private var splitOperator: UnaryOperator<A>? = null
        private var mergeOperator: BinaryOperator<A>? = null
        fun initialValue(initialValue: Supplier<A>): Builder<*> {
            this.initialValue = initialValue
            return this
        }

        fun splitOperator(splitOperator: UnaryOperator<A>): Builder<*> {
            this.splitOperator = splitOperator
            return this
        }

        fun mergeOperator(mergeOperator: BinaryOperator<A>): Builder<*> {
            this.mergeOperator = mergeOperator
            return this
        }

        fun create(): SackStrategy {
            return SackStrategy(initialValue, splitOperator, mergeOperator)
        }
    }

    companion object {
        fun <A> build(): Builder<A> {
            return Builder()
        }
    }
}