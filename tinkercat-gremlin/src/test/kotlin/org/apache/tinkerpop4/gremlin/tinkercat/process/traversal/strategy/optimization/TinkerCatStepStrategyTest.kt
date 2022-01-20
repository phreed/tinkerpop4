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
package org.apache.tinkerpop4.gremlin.tinkercat.process.traversal.strategy.optimization

import org.apache.tinkerpop4.gremlin.process.traversal.P
import org.apache.tinkerpop4.gremlin.process.traversal.Traversal
import org.apache.tinkerpop4.gremlin.tinkercat.process.traversal.strategy.optimization.TinkerCatStepStrategy.Companion.instance
import org.apache.tinkerpop4.gremlin.tinkercat.process.traversal.step.sideEffect.TinkerCatStep.addHasContainer
import org.junit.runner.RunWith
import org.apache.tinkerpop4.gremlin.process.traversal.TraversalStrategy
import org.apache.tinkerpop4.gremlin.process.traversal.TraversalStrategies
import org.apache.tinkerpop4.gremlin.process.traversal.util.DefaultTraversalStrategies
import org.apache.tinkerpop4.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop4.gremlin.process.traversal.dsl.graph.DefaultGraphTraversal
import org.apache.tinkerpop4.gremlin.process.traversal.dsl.graph.__
import org.apache.tinkerpop4.gremlin.tinkercat.process.traversal.step.sideEffect.TinkerCatStep
import org.apache.tinkerpop4.gremlin.process.traversal.step.map.GraphStep
import org.apache.tinkerpop4.gremlin.process.traversal.step.util.HasContainer
import org.apache.tinkerpop4.gremlin.process.traversal.util.EmptyTraversal
import java.util.Arrays
import org.apache.tinkerpop4.gremlin.process.traversal.strategy.optimization.FilterRankingStrategy
import org.apache.tinkerpop4.gremlin.process.traversal.strategy.optimization.InlineFilterStrategy
import org.apache.tinkerpop4.gremlin.tinkercat.structure.TinkerCat
import org.apache.tinkerpop4.gremlin.process.traversal.strategy.optimization.LazyBarrierStrategy
import org.apache.tinkerpop4.gremlin.structure.Element
import org.apache.tinkerpop4.gremlin.structure.Vertex
import org.junit.Assert
import org.junit.Test
import org.junit.runners.Parameterized

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Daniel Kuppitz (http://gremlin.guru)
 */
@RunWith(Parameterized::class)
class TinkerCatStepStrategyTest {
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
        original!!.asAdmin().strategies = strategies
        original!!.asAdmin().applyStrategies()
        Assert.assertEquals(optimized, original)
    }

    companion object {
        private fun g_V(vararg hasKeyValues: Any): GraphTraversal.Admin<*, *> {
            val traversal: GraphTraversal.Admin<*, *> = DefaultGraphTraversal<Any, Any>()
            val graphStep = TinkerCatStep(GraphStep<Vertex, Vertex>(traversal, Vertex::class.java, true))
            var i = 0
            while (i < hasKeyValues.size) {
                graphStep.addHasContainer(HasContainer(hasKeyValues[i] as String, hasKeyValues[i + 1] as P<*>))
                i = i + 2
            }
            return traversal.addStep(graphStep)
        }

        private fun V(vararg hasKeyValues: Any): GraphStep<*, *> {
            val graphStep =
                TinkerCatStep(GraphStep<Vertex, Vertex>(EmptyTraversal.instance<Any, Any>(), Vertex::class.java, true))
            var i = 0
            while (i < hasKeyValues.size) {
                graphStep.addHasContainer(HasContainer(hasKeyValues[i] as String, hasKeyValues[i + 1] as P<*>))
                i = i + 2
            }
            return graphStep
        }

        @Parameterized.Parameters(name = "{0}")
        fun generateTestParameters(): Iterable<Array<Any>> {
            val LAZY_SIZE = 2500
            return Arrays.asList(
                *arrayOf(
                    arrayOf(__.V<Any>().out(), g_V().out(), emptyList<Any>()),
                    arrayOf(__.V<Any>().has("name", "marko").out(), g_V("name", P.eq("marko")).out(), emptyList<Any>()),
                    arrayOf(
                        __.V<Any>().has("name", "marko").has("age", P.gt(31).and(P.lt(10))).out(),
                        g_V("name", P.eq("marko"), "age", P.gt(31), "age", P.lt(10)).out(), emptyList<Any>()
                    ),
                    arrayOf(
                        __.V<Any>().has("name", "marko").or(__.has<Any>("age"), __.has<Any>("age", P.gt(32)))
                            .has("lang", "java"),
                        g_V("name", P.eq("marko"), "lang", P.eq("java")).or(
                            __.has<Any>("age"),
                            __.has<Any>("age", P.gt(32))
                        ), listOf(FilterRankingStrategy.instance())
                    ),
                    arrayOf(
                        __.V<Any>().has("name", "marko").`as`("a").or(__.has<Any>("age"), __.has<Any>("age", P.gt(32)))
                            .has("lang", "java"),
                        g_V("name", P.eq("marko")).`as`("a").or(__.has<Any>("age"), __.has<Any>("age", P.gt(32)))
                            .has("lang", "java"), emptyList<Any>()
                    ),
                    arrayOf(
                        __.V<Any>().has("name", "marko").`as`("a").or(__.has<Any>("age"), __.has<Any>("age", P.gt(32)))
                            .has("lang", "java"),
                        g_V("name", P.eq("marko"), "lang", P.eq("java")).or(
                            __.has<Any>("age"),
                            __.has<Any>("age", P.gt(32))
                        ).`as`("a"), listOf(FilterRankingStrategy.instance())
                    ),
                    arrayOf(
                        __.V<Any>().dedup().has("name", "marko").or(__.has<Any>("age"), __.has<Any>("age", P.gt(32)))
                            .has("lang", "java"),
                        g_V("name", P.eq("marko"), "lang", P.eq("java")).or(
                            __.has<Any>("age"),
                            __.has<Any>("age", P.gt(32))
                        ).dedup(), listOf(FilterRankingStrategy.instance())
                    ),
                    arrayOf(
                        __.V<Any>().`as`("a").dedup().has("name", "marko")
                            .or(__.has<Any>("age"), __.has<Any>("age", P.gt(32))).has("lang", "java"),
                        g_V("name", P.eq("marko"), "lang", P.eq("java")).or(
                            __.has<Any>("age"),
                            __.has<Any>("age", P.gt(32))
                        ).dedup().`as`("a"), listOf(FilterRankingStrategy.instance())
                    ),
                    arrayOf(
                        __.V<Any>().`as`("a").has("name", "marko").`as`("b")
                            .or(__.has<Any>("age"), __.has<Any>("age", P.gt(32))).has("lang", "java"),
                        g_V("name", P.eq("marko"), "lang", P.eq("java")).or(
                            __.has<Any>("age"),
                            __.has<Any>("age", P.gt(32))
                        ).`as`("b", "a"), listOf(FilterRankingStrategy.instance())
                    ),
                    arrayOf(
                        __.V<Any>().`as`("a").dedup().has("name", "marko")
                            .or(__.has<Any>("age"), __.has<Any>("age", P.gt(32))).filter(
                            __.has<Any>("name", "bob")
                        ).has("lang", "java"),
                        g_V("name", P.eq("marko"), "lang", P.eq("java"), "name", P.eq("bob")).or(
                            __.has<Any>("age"),
                            __.has<Any>("age", P.gt(32))
                        ).dedup().`as`("a"),
                        Arrays.asList(InlineFilterStrategy.instance(), FilterRankingStrategy.instance())
                    ),
                    arrayOf(
                        __.V<Any>().`as`("a").dedup().has("name", "marko")
                            .or(__.has<Any>("age", 10), __.has<Any>("age", P.gt(32))).filter(
                            __.has<Any>("name", "bob")
                        ).has("lang", "java"),
                        g_V(
                            "name",
                            P.eq("marko"),
                            "lang",
                            P.eq("java"),
                            "name",
                            P.eq("bob"),
                            "age",
                            P.eq(10).or(P.gt(32))
                        ).dedup().`as`("a"), TraversalStrategies.GlobalCache.getStrategies(
                            TinkerCat::class.java
                        ).toList()
                    ),
                    arrayOf(
                        __.V<Any>().has("name", "marko")
                            .or(__.not<Any>(__.has<Any>("age")), __.has<Any>("age", P.gt(32))).has("name", "bob")
                            .has("lang", "java"),
                        g_V(
                            "name",
                            P.eq("marko"),
                            "name",
                            P.eq("bob"),
                            "lang",
                            P.eq("java")
                        ).or(
                            __.not<Any>(__.filter<Any>(__.properties<Element, Any>("age"))),
                            __.has<Any>("age", P.gt(32))
                        ), TraversalStrategies.GlobalCache.getStrategies(
                            TinkerCat::class.java
                        ).toList()
                    ),
                    arrayOf(
                        __.V<Any>().has("name", P.eq("marko").or(P.eq("bob").and(P.eq("stephen")))).out("knows"),
                        g_V("name", P.eq("marko").or(P.eq("bob").and(P.eq("stephen")))).out("knows"), emptyList<Any>()
                    ),
                    arrayOf(
                        __.V<Any>().has("name", P.eq("marko").and(P.eq("bob").and(P.eq("stephen")))).out("knows"),
                        g_V("name", P.eq("marko"), "name", P.eq("bob"), "name", P.eq("stephen")).out("knows"),
                        emptyList<Any>()
                    ),
                    arrayOf(
                        __.V<Any>().has("name", P.eq("marko").and(P.eq("bob").or(P.eq("stephen")))).out("knows"),
                        g_V("name", P.eq("marko"), "name", P.eq("bob").or(P.eq("stephen"))).out("knows"),
                        emptyList<Any>()
                    ),
                    arrayOf(
                        __.V<Any>().out().out().V().has("name", "marko").out(),
                        g_V().out().barrier(LAZY_SIZE).out().barrier(LAZY_SIZE).asAdmin().addStep(
                            V("name", P.eq("marko"))
                        ).barrier(LAZY_SIZE).out(),
                        Arrays.asList(
                            InlineFilterStrategy.instance(),
                            FilterRankingStrategy.instance(),
                            LazyBarrierStrategy.instance()
                        )
                    ),
                    arrayOf(
                        __.V<Any>().out().out().V().has("name", "marko").`as`("a").out(),
                        g_V().out().barrier(LAZY_SIZE).out().barrier(LAZY_SIZE).asAdmin().addStep(
                            V("name", P.eq("marko"))
                        ).barrier(LAZY_SIZE).`as`("a").out(),
                        Arrays.asList(
                            InlineFilterStrategy.instance(),
                            FilterRankingStrategy.instance(),
                            LazyBarrierStrategy.instance()
                        )
                    ),
                    arrayOf(
                        __.V<Any>().out().V().has("age", P.gt(32)).barrier(10).has("name", "marko").`as`("a"),
                        g_V().out().barrier(LAZY_SIZE).asAdmin().addStep(
                            V("age", P.gt(32), "name", P.eq("marko"))
                        ).barrier(LAZY_SIZE).barrier(10).`as`("a"),
                        Arrays.asList(
                            InlineFilterStrategy.instance(),
                            FilterRankingStrategy.instance(),
                            LazyBarrierStrategy.instance()
                        )
                    ),
                    arrayOf(
                        __.V<Any>().out().V().has("age", P.gt(32)).barrier(10).has("name", "marko").`as`("a"),
                        g_V().out().barrier(LAZY_SIZE).asAdmin().addStep(
                            V("age", P.gt(32), "name", P.eq("marko"))
                        ).barrier(LAZY_SIZE).barrier(10).`as`("a"),
                        TraversalStrategies.GlobalCache.getStrategies(
                            TinkerCat::class.java
                        ).toList()
                    )
                )
            )
        }
    }
}