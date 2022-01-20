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

import org.apache.tinkerpop4.gremlin.process.traversal.TraversalStrategies

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class DefaultTraversalStrategies : TraversalStrategies {
    protected var traversalStrategies: Set<TraversalStrategy<*>> = LinkedHashSet()
    @Override
    @SuppressWarnings(["unchecked", "varargs"])
    fun addStrategies(vararg strategies: TraversalStrategy<*>?): TraversalStrategies {
        for (addStrategy in strategies) {
            // search by class to prevent strategies from being added more than once
            getStrategy(addStrategy.getClass()).ifPresent { s -> traversalStrategies.remove(s) }
        }
        Collections.addAll(traversalStrategies, strategies)
        traversalStrategies = TraversalStrategies.sortStrategies(traversalStrategies)
        return this
    }

    @Override
    @SuppressWarnings(["unchecked", "varargs"])
    fun removeStrategies(vararg strategyClasses: Class<out TraversalStrategy?>?): TraversalStrategies {
        var removed = false
        for (strategyClass in strategyClasses) {
            val strategy: Optional<TraversalStrategy<*>> =
                traversalStrategies.stream().filter { s -> s.getClass().equals(strategyClass) }
                    .findAny()
            if (strategy.isPresent()) {
                traversalStrategies.remove(strategy.get())
                removed = true
            }
        }
        if (removed) traversalStrategies = TraversalStrategies.sortStrategies(traversalStrategies)
        return this
    }

    @Override
    fun toList(): List<TraversalStrategy<*>> {
        return Collections.unmodifiableList(ArrayList(traversalStrategies))
    }

    @Override
    operator fun iterator(): Iterator<TraversalStrategy<*>> {
        return traversalStrategies.iterator()
    }

    @Override
    fun <T : TraversalStrategy?> getStrategy(traversalStrategyClass: Class<T>): Optional<T> {
        for (traversalStrategy in traversalStrategies) {
            if (traversalStrategyClass.isAssignableFrom(traversalStrategy.getClass())) return Optional.of(
                traversalStrategy
            ) as Optional
        }
        return Optional.empty()
    }

    @Override
    fun clone(): DefaultTraversalStrategies {
        return try {
            val clone = super.clone() as DefaultTraversalStrategies
            clone.traversalStrategies = LinkedHashSet(traversalStrategies.size())
            clone.traversalStrategies.addAll(traversalStrategies)
            clone
        } catch (e: CloneNotSupportedException) {
            throw IllegalStateException(e.getMessage(), e)
        }
    }

    @Override
    override fun toString(): String {
        return StringFactory.traversalStrategiesString(this)
    }
}