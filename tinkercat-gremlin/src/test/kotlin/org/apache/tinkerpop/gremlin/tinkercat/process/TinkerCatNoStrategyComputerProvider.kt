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
package org.apache.tinkerpop.gremlin.tinkercat.process

import org.apache.tinkerpop.gremlin.tinkercat.process.TinkerCatComputerProvider
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.process.traversal.TraversalStrategies
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerCat
import org.apache.tinkerpop.gremlin.process.traversal.TraversalStrategy
import org.apache.tinkerpop.gremlin.tinkercat.process.TinkerCatNoStrategyComputerProvider
import java.util.Arrays
import org.apache.tinkerpop.gremlin.process.traversal.strategy.AbstractTraversalStrategy
import org.apache.tinkerpop.gremlin.process.traversal.strategy.optimization.CountStrategy
import org.apache.tinkerpop.gremlin.process.traversal.strategy.verification.ComputerVerificationStrategy
import org.apache.tinkerpop.gremlin.process.traversal.strategy.finalization.ProfileStrategy
import org.apache.tinkerpop.gremlin.process.traversal.strategy.optimization.ProductiveByStrategy
import org.apache.tinkerpop.gremlin.process.traversal.strategy.optimization.FilterRankingStrategy
import org.apache.tinkerpop.gremlin.process.traversal.strategy.decoration.ConnectiveStrategy
import org.apache.tinkerpop.gremlin.process.traversal.strategy.decoration.SideEffectStrategy
import org.apache.tinkerpop.gremlin.structure.Graph
import java.util.HashSet
import java.util.function.Predicate
import java.util.stream.Collectors

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class TinkerCatNoStrategyComputerProvider : TinkerCatComputerProvider() {
    override fun traversal(graph: Graph?): GraphTraversalSource? {
        val toRemove = TraversalStrategies.GlobalCache.getStrategies(TinkerCat::class.java).toList().stream()
            .map { obj: TraversalStrategy<*>? -> obj!!.javaClass }
            .filter(Predicate<Class<TraversalStrategy<*>?>?> { clazz: Class<TraversalStrategy<*>?>? ->
                !REQUIRED_STRATEGIES!!.contains(
                    clazz
                )
            })
            .collect(Collectors.toList())
        return graph!!.traversal().withoutStrategies(*toRemove!!.toTypedArray()).withComputer()
    }

    companion object {
        private val REQUIRED_STRATEGIES: HashSet<Class<out TraversalStrategy<*>?>?>? = HashSet(
            Arrays.asList(
                CountStrategy::class.java,
                ComputerVerificationStrategy::class.java,
                ProfileStrategy::class.java,
                ProductiveByStrategy::class.java,  // this strategy is required to maintain 3.5.x null behaviors defined in tests
                FilterRankingStrategy::class.java,
                ConnectiveStrategy::class.java,
                SideEffectStrategy::class.java
            )
        )
    }
}