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
package org.apache.tinkerpop4.gremlin.tinkercat.process

import org.apache.tinkerpop4.gremlin.tinkercat.TinkerCatProvider
import org.apache.tinkerpop4.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop4.gremlin.process.traversal.TraversalStrategies
import org.apache.tinkerpop4.gremlin.tinkercat.structure.TinkerCat
import org.apache.tinkerpop4.gremlin.process.traversal.TraversalStrategy
import org.apache.tinkerpop4.gremlin.tinkercat.process.TinkerCatNoStrategyProvider
import java.util.Arrays
import org.apache.tinkerpop4.gremlin.process.traversal.strategy.AbstractTraversalStrategy
import org.apache.tinkerpop4.gremlin.tinkercat.process.traversal.strategy.optimization.TinkerCatStepStrategy
import org.apache.tinkerpop4.gremlin.process.traversal.strategy.finalization.ProfileStrategy
import org.apache.tinkerpop4.gremlin.process.traversal.strategy.optimization.ProductiveByStrategy
import org.apache.tinkerpop4.gremlin.process.traversal.strategy.decoration.ConnectiveStrategy
import org.apache.tinkerpop4.gremlin.process.traversal.strategy.decoration.SideEffectStrategy
import org.apache.tinkerpop4.gremlin.structure.Graph
import java.util.HashSet
import java.util.function.Predicate
import java.util.stream.Collectors

/**
 * A [GraphProvider] that constructs a [TraversalSource] with no default strategies applied.  This allows
 * the process tests to be executed without strategies applied.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class TinkerCatNoStrategyProvider : TinkerCatProvider() {
    override fun traversal(graph: Graph?): GraphTraversalSource? {
        val toRemove = TraversalStrategies.GlobalCache.getStrategies(TinkerCat::class.java).toList().stream()
            .map { obj: TraversalStrategy<*>? -> obj!!.javaClass }
            .filter { clazz -> !REQUIRED_STRATEGIES!!.contains(clazz) }
            .collect(Collectors.toList())
        return graph!!.traversal().withoutStrategies(*toRemove!!.toTypedArray())
    }

    companion object {
        private val REQUIRED_STRATEGIES = setOf<Class<*>>(
                TinkerCatStepStrategy::class.java,
                ProfileStrategy::class.java,
                ProductiveByStrategy::class.java,  // this strategy is required to maintain 3.5.x null behaviors defined in tests
                ConnectiveStrategy::class.java,
                SideEffectStrategy::class.java,
            )
    }
}