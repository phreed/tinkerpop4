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
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class SideEffectStrategy private constructor() : AbstractTraversalStrategy<DecorationStrategy?>(), DecorationStrategy {
    private val sideEffects: List<Triplet<String, Supplier, BinaryOperator>> = ArrayList()
    @Override
    fun apply(traversal: Traversal.Admin<*, *>) {
        if (traversal.isRoot()) {
            sideEffects.forEach { triplet ->
                traversal.getSideEffects().register(triplet.getValue0(), triplet.getValue1(), triplet.getValue2())
            }
        }
    }

    operator fun contains(sideEffectKey: String?): Boolean {
        return sideEffects.stream().anyMatch { triplet -> triplet.getValue0().equals(sideEffectKey) }
    }

    fun initialValue(sideEffectKey: String?): Object? {
        for (triplet in sideEffects) {
            if (triplet.getValue0().equals(sideEffectKey)) {
                return triplet.getValue1().get()
            }
        }
        return null
    }

    companion object {
        fun <A> addSideEffect(
            traversalStrategies: TraversalStrategies,
            key: String?,
            value: A,
            reducer: BinaryOperator<A>?
        ) {
            var strategy: SideEffectStrategy? =
                traversalStrategies.getStrategy(SideEffectStrategy::class.java).orElse(null)
            if (null == strategy) {
                strategy = SideEffectStrategy()
                traversalStrategies.addStrategies(strategy)
            } else {
                val cloneStrategy = SideEffectStrategy()
                cloneStrategy.sideEffects.addAll(strategy.sideEffects)
                strategy = cloneStrategy
                traversalStrategies.addStrategies(strategy)
            }
            strategy.sideEffects.add(
                Triplet(
                    key,
                    if (value is Supplier) value as Supplier else ConstantSupplier(value),
                    reducer
                )
            )
        }
    }
}