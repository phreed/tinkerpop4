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
package org.apache.tinkerpop4.gremlin.tinkercat.process.traversal.strategy.optimization

import org.apache.tinkerpop4.gremlin.process.traversal.Traversal
import org.apache.tinkerpop4.gremlin.process.traversal.TraversalStrategies
import org.apache.tinkerpop4.gremlin.process.traversal.TraversalStrategy
import org.apache.tinkerpop4.gremlin.process.traversal.Traverser
import org.apache.tinkerpop4.gremlin.process.traversal.dsl.graph.DefaultGraphTraversal
import org.apache.tinkerpop4.gremlin.process.traversal.dsl.graph.`__` as anon
import org.apache.tinkerpop4.gremlin.process.traversal.util.DefaultTraversalStrategies
import org.apache.tinkerpop4.gremlin.process.traversal.util.EmptyTraversal
import org.apache.tinkerpop4.gremlin.structure.Element
import org.apache.tinkerpop4.gremlin.structure.Vertex
import org.apache.tinkerpop4.gremlin.tinkercat.process.traversal.step.map.TinkerCountGlobalStep
import org.apache.tinkerpop4.gremlin.tinkercat.process.traversal.strategy.optimization.TinkerCatCountStrategy.Companion.instance
import org.apache.tinkerpop4.gremlin.tinkercat.structure.TinkerCat
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
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
            val myOptimized = original!!.asAdmin().clone()
            myOptimized.asAdmin().strategies = strategies
            myOptimized.asAdmin().applyStrategies()
            this.optimized = myOptimized
        }
        original!!.asAdmin().strategies = strategies
        original!!.asAdmin().applyStrategies()
        Assert.assertEquals(optimized, original)
    }

    companion object {
        private fun countStep(elementClass: Class<out Element?>): Traversal.Admin<Any, Long?> {
            val myStep = TinkerCountGlobalStep<Element?>(
                EmptyTraversal.instance<Any, Any>(),
                elementClass
            )
            val foo = DefaultGraphTraversal<Any, Any>()
            val bar = foo.addStep<Any>(myStep)
            return foo
        }

        @Parameterized.Parameters(name = "{0}")
        fun generateTestParameters(): Iterable<Array<Any?>> {
            return listOf(
                arrayOf(
                    anon.V<Any>().count(),
                    countStep(Vertex::class.java),
                    emptyList<Any>(),
                ),
                arrayOf(
                    anon.V<Any>().count(),
                    TraversalStrategies.GlobalCache.getStrategies(TinkerCat::class.java).toList()
                ),
                arrayOf(
                    anon.V<Any>().`as`("a").count(),
                    countStep(Vertex::class.java),
                    TraversalStrategies.GlobalCache.getStrategies(TinkerCat::class.java).toList()
                ),
                arrayOf(
                    anon.V<Any>().count().`as`("a"),
                    countStep(Vertex::class.java),
                    TraversalStrategies.GlobalCache.getStrategies(TinkerCat::class.java).toList()
                ),
                arrayOf(
                    anon.V<Any>().map(anon.out()).count().`as`("a"),
                    null,
                    TraversalStrategies.GlobalCache.getStrategies(TinkerCat::class.java).toList()
                ),
                arrayOf(
                    anon.V<Any>().map(anon.out()).identity().count().`as`("a"),
                    null,
                    TraversalStrategies.GlobalCache.getStrategies(TinkerCat::class.java).toList()
                ),
                arrayOf(
                    anon.V<Any>().map(anon.out().groupCount<Any>()).identity().count().`as`("a"),
                    null,
                    TraversalStrategies.GlobalCache.getStrategies(TinkerCat::class.java).toList()
                ),
                arrayOf(
                    anon.V<Any>().label().map { s: Traverser<String> -> s.get().length }.count(),
                    null,
                    TraversalStrategies.GlobalCache.getStrategies(TinkerCat::class.java).toList()),
                arrayOf(
                    anon.V<Any>().`as`("a").map(anon.select<Any, Any>("a")).count(),
                    null,
                    TraversalStrategies.GlobalCache.getStrategies(TinkerCat::class.java).toList()
                ),
                arrayOf(
                    anon.V<Any>(),
                    null,
                    emptyList<Any>()),
                arrayOf(
                    anon.V<Any>().out().count(),
                    null,
                    emptyList<Any>()),
                arrayOf(
                    anon.V<Any>(1).count(),
                    null,
                    emptyList<Any>()
                ),
                arrayOf(
                    anon.count<Any>(),
                    null,
                    emptyList<Any>()),
                arrayOf(
                    anon.V<Any>().map(anon.out().groupCount("m")).identity().count().`as`("a"),
                    null,
                    emptyList<Any>()
                ))
        }
    }
}