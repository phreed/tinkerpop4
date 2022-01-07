/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.tinkerpop.gremlin.tinkercat.process.traversal.strategy.optimization

import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.tinkercat.process.traversal.strategy.optimization.TinkerCatCountStrategy.Companion.instance
import org.junit.runner.RunWith
import org.apache.tinkerpop.gremlin.process.traversal.TraversalStrategy
import org.apache.tinkerpop.gremlin.process.traversal.TraversalStrategies
import org.apache.tinkerpop.gremlin.process.traversal.Traverser
import org.apache.tinkerpop.gremlin.process.traversal.util.DefaultTraversalStrategies
import org.apache.tinkerpop.gremlin.tinkercat.process.traversal.strategy.optimization.TinkerCatCountStrategy
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.DefaultGraphTraversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__
import org.apache.tinkerpop.gremlin.process.traversal.util.EmptyTraversal
import org.apache.tinkerpop.gremlin.structure.Element
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.tinkercat.process.traversal.step.map.TinkerCountGlobalStep
import java.util.Arrays
import org.apache.tinkerpop.gremlin.tinkercat.process.traversal.strategy.optimization.TinkerCatCountStrategyTest
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerCat
import org.junit.Assert
import org.junit.Test
import org.junit.runners.Parameterized

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
@RunWith(Parameterized::class)
class TinkerCatCountStrategyTest {
    @Parameterized.Parameter(value = 0)
    var original: Traversal<*, *>? = null

    @Parameterized.Parameter(value = 1)
    var optimized: Traversal<*, *>? = null

    @Parameterized.Parameter(value = 2)
    var otherStrategies: Collection<TraversalStrategy<*>>? = null
    @Test
    fun doTest() {
        val strategies: TraversalStrategies = DefaultTraversalStrategies()
        strategies.addStrategies(instance())
        for (strategy in otherStrategies!!) {
            strategies.addStrategies(strategy)
        }
        if (optimized == null) {
            optimized = original!!.asAdmin().clone()
            optimized.asAdmin().strategies = strategies
            optimized.asAdmin().applyStrategies()
        }
        original!!.asAdmin().strategies = strategies
        original!!.asAdmin().applyStrategies()
        Assert.assertEquals(optimized, original)
    }

    companion object {
        private fun countStep(elementClass: Class<out Element?>): Traversal.Admin<*, *> {
            return DefaultGraphTraversal<Any, Any>().addStep<Any>(
                TinkerCountGlobalStep<Any?>(
                    EmptyTraversal.instance<Any, Any>(),
                    elementClass
                )
            )
        }

        @Parameterized.Parameters(name = "{0}")
        fun generateTestParameters(): Iterable<Array<Any>> {
            return Arrays.asList<Array<Any>>(*arrayOf(arrayOf<Any?>(
                __.V<Any>().count(), countStep(
                    Vertex::class.java
                ), emptyList<Any>()
            ),
                arrayOf<Any?>(
                    __.V<Any>().count(), countStep(
                        Vertex::class.java
                    ), TraversalStrategies.GlobalCache.getStrategies(
                        TinkerCat::class.java
                    ).toList()
                ),
                arrayOf<Any?>(
                    __.V<Any>().`as`("a").count(), countStep(
                        Vertex::class.java
                    ), TraversalStrategies.GlobalCache.getStrategies(
                        TinkerCat::class.java
                    ).toList()
                ),
                arrayOf<Any?>(
                    __.V<Any>().count().`as`("a"), countStep(
                        Vertex::class.java
                    ), TraversalStrategies.GlobalCache.getStrategies(
                        TinkerCat::class.java
                    ).toList()
                ),
                arrayOf<Any?>(
                    __.V<Any>().map(__.out()).count().`as`("a"),
                    null,
                    TraversalStrategies.GlobalCache.getStrategies(
                        TinkerCat::class.java
                    ).toList()
                ),
                arrayOf<Any?>(
                    __.V<Any>().map(__.out()).identity().count().`as`("a"),
                    null,
                    TraversalStrategies.GlobalCache.getStrategies(
                        TinkerCat::class.java
                    ).toList()
                ),
                arrayOf<Any?>(
                    __.V<Any>().map(__.out().groupCount<Any>()).identity().count().`as`("a"),
                    null,
                    TraversalStrategies.GlobalCache.getStrategies(
                        TinkerCat::class.java
                    ).toList()
                ),
                arrayOf<Any?>(__.V<Any>().label().map { s: Traverser<String> -> s.get().length }
                    .count(), null, TraversalStrategies.GlobalCache.getStrategies(
                    TinkerCat::class.java
                ).toList()),
                arrayOf<Any?>(
                    __.V<Any>().`as`("a").map(__.select<Any, Any>("a")).count(),
                    null,
                    TraversalStrategies.GlobalCache.getStrategies(
                        TinkerCat::class.java
                    ).toList()
                ),
                arrayOf<Any?>(__.V<Any>(), null, emptyList<Any>()),
                arrayOf<Any?>(__.V<Any>().out().count(), null, emptyList<Any>()),
                arrayOf<Any?>(
                    __.V<Any>(1).count(), null, emptyList<Any>()
                ),
                arrayOf<Any?>(__.count<Any>(), null, emptyList<Any>()),
                arrayOf<Any?>(
                    __.V<Any>().map(__.out().groupCount("m")).identity().count().`as`("a"), null, emptyList<Any>()
                )))
        }
    }
}