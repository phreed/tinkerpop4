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
package org.apache.tinkerpop4.gremlin.tinkercat.process.traversal.strategy.decoration

import org.apache.tinkerpop4.gremlin.process.traversal.Traverser
import org.apache.tinkerpop4.gremlin.tinkercat.structure.TinkerCat.Companion.open
import org.apache.tinkerpop4.gremlin.tinkercat.structure.TinkerCat
import org.apache.tinkerpop4.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop4.gremlin.process.traversal.strategy.decoration.OptionsStrategy
import org.apache.tinkerpop4.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop4.gremlin.process.traversal.step.map.ScalarMapStep
import org.apache.tinkerpop4.gremlin.structure.Graph
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Test
import java.util.Arrays

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
open class OptionsStrategyTest {
    @Test
    fun shouldAddOptionsToTraversal() {
        val graph: Graph = open()
        val optionedG = graph.traversal().withStrategies(OptionsStrategy.build().with("a", "test").with("b").create())
        assertOptions(optionedG)
    }

    @Test
    fun shouldAddOptionsToTraversalUsingWith() {
        val graph: Graph = open()
        val optionedG = graph.traversal().with("a", "test").with("b")
        assertOptions(optionedG)
    }

    companion object {
        private fun assertOptions(optionedG: GraphTraversalSource) {
            var t: GraphTraversal<*, *> = optionedG.inject(1)
            t = t.asAdmin().addStep(object : ScalarMapStep<Any?, Any?>(t.asAdmin()) {
                override fun map(traverser: Traverser.Admin<Any?>): Any? {
                    val strategy = traversal.asAdmin().strategies.getStrategy(
                        OptionsStrategy::class.java
                    ).get()
                    return listOf(strategy.options["a"], strategy.options["b"])
                }

                override fun remove() {
                    TODO("Not yet implemented")
                }

            })
            MatcherAssert.assertThat(t.next() as Collection<Any>, Matchers.contains("test", true))
        }
    }
}