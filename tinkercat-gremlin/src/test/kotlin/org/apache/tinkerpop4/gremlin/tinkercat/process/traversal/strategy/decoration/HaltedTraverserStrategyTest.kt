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

import org.apache.commons.configuration2.MapConfiguration
import org.apache.tinkerpop4.gremlin.process.traversal.Path
import org.apache.tinkerpop4.gremlin.tinkercat.structure.TinkerFactory.createModern
import org.apache.tinkerpop4.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop4.gremlin.process.traversal.strategy.decoration.HaltedTraverserStrategy
import org.apache.tinkerpop4.gremlin.structure.Edge
import org.apache.tinkerpop4.gremlin.structure.Graph
import org.apache.tinkerpop4.gremlin.structure.Property
import org.apache.tinkerpop4.gremlin.structure.Vertex
import org.apache.tinkerpop4.gremlin.structure.util.detached.DetachedFactory
import org.apache.tinkerpop4.gremlin.structure.util.detached.DetachedVertex
import org.apache.tinkerpop4.gremlin.structure.util.detached.DetachedVertexProperty
import org.apache.tinkerpop4.gremlin.structure.util.detached.DetachedEdge
import org.apache.tinkerpop4.gremlin.structure.util.detached.DetachedProperty
import org.apache.tinkerpop4.gremlin.structure.util.detached.DetachedPath
import org.apache.tinkerpop4.gremlin.structure.util.reference.*
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.HashMap
import java.util.function.Consumer

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class HaltedTraverserStrategyTest {
    @Before
    fun setup() {
        // necessary as ComputerResult step for testing purposes attaches Attachables
        System.setProperty("is.testing", "false")
    }

    @After
    fun shutdown() {
        System.setProperty("is.testing", "true")
    }

    @Test
    fun shouldReturnDetachedElements() {
        val graph: Graph = createModern()
        val g = graph.traversal().withComputer()
            .withStrategies(HaltedTraverserStrategy.create(MapConfiguration(object : HashMap<String?, Any?>() {
                init {
                    put(HaltedTraverserStrategy.HALTED_TRAVERSER_FACTORY, DetachedFactory::class.java.canonicalName)
                }
            })))
        g.V().out().forEachRemaining { vertex: Vertex ->
            Assert.assertEquals(
                DetachedVertex::class.java, vertex.javaClass
            )
        }
        g.V().out().properties<Any>("name").forEachRemaining { vertexProperty: Property<Any>? ->
            Assert.assertEquals(
                DetachedVertexProperty::class.java, vertexProperty!!.javaClass
            )
        }
        g.V().out().values<Any>("name").forEachRemaining { value: Any ->
            Assert.assertEquals(
                String::class.java, value.javaClass
            )
        }
        g.V().out().outE().forEachRemaining { edge: Edge ->
            Assert.assertEquals(
                DetachedEdge::class.java, edge.javaClass
            )
        }
        g.V().out().outE().properties<Any>("weight").forEachRemaining { property: Property<Any>? ->
            Assert.assertEquals(
                DetachedProperty::class.java, property!!.javaClass
            )
        }
        g.V().out().outE().values<Any>("weight").forEachRemaining { value: Any ->
            Assert.assertEquals(
                Double::class.java, value.javaClass
            )
        }
        g.V().out().out().forEachRemaining { vertex: Vertex ->
            Assert.assertEquals(
                DetachedVertex::class.java, vertex.javaClass
            )
        }
        g.V().out().out().path().forEachRemaining { path: Path ->
            Assert.assertEquals(
                DetachedPath::class.java, path.javaClass
            )
        }
        g.V().out().pageRank().forEachRemaining { vertex: Vertex ->
            Assert.assertEquals(
                DetachedVertex::class.java, vertex.javaClass
            )
        }
        g.V().out().pageRank().out().forEachRemaining { vertex: Vertex ->
            Assert.assertEquals(
                DetachedVertex::class.java, vertex.javaClass
            )
        }
        // should handle nested collections
        g.V().out().fold().next().forEach(Consumer { vertex: Vertex ->
            Assert.assertEquals(
                DetachedVertex::class.java, vertex.javaClass
            )
        })
    }

    @Test
    fun shouldReturnReferenceElements() {
        val graph: Graph = createModern()
        var g = graph.traversal().withComputer().withStrategies(HaltedTraverserStrategy.reference())
        g.V().out().forEachRemaining { vertex: Vertex ->
            Assert.assertEquals(
                ReferenceVertex::class.java, vertex.javaClass
            )
        }
        g.V().out().properties<Any>("name").forEachRemaining { vertexProperty: Property<Any>? ->
            Assert.assertEquals(
                ReferenceVertexProperty::class.java, vertexProperty!!.javaClass
            )
        }
        g.V().out().values<Any>("name").forEachRemaining { value: Any ->
            Assert.assertEquals(
                String::class.java, value.javaClass
            )
        }
        g.V().out().outE().forEachRemaining { edge: Edge ->
            Assert.assertEquals(
                ReferenceEdge::class.java, edge.javaClass
            )
        }
        g.V().out().outE().properties<Any>("weight").forEachRemaining { property: Property<Any>? ->
            Assert.assertEquals(
                ReferenceProperty::class.java, property!!.javaClass
            )
        }
        g.V().out().outE().values<Any>("weight").forEachRemaining { value: Any ->
            Assert.assertEquals(
                Double::class.java, value.javaClass
            )
        }
        g.V().out().out().forEachRemaining { vertex: Vertex ->
            Assert.assertEquals(
                ReferenceVertex::class.java, vertex.javaClass
            )
        }
        g.V().out().out().path().forEachRemaining { path: Path ->
            Assert.assertEquals(
                ReferencePath::class.java, path.javaClass
            )
        }
        g.V().out().pageRank().forEachRemaining { vertex: Vertex ->
            Assert.assertEquals(
                ReferenceVertex::class.java, vertex.javaClass
            )
        }
        g.V().out().pageRank().out().forEachRemaining { vertex: Vertex ->
            Assert.assertEquals(
                ReferenceVertex::class.java, vertex.javaClass
            )
        }
        // the default should be reference elements
        g = graph.traversal().withComputer()
        g.V().out().forEachRemaining { vertex: Vertex ->
            Assert.assertEquals(
                ReferenceVertex::class.java, vertex.javaClass
            )
        }
        g.V().out().properties<Any>("name").forEachRemaining { vertexProperty: Property<Any>? ->
            Assert.assertEquals(
                ReferenceVertexProperty::class.java, vertexProperty!!.javaClass
            )
        }
        g.V().out().values<Any>("name").forEachRemaining { value: Any ->
            Assert.assertEquals(
                String::class.java, value.javaClass
            )
        }
        g.V().out().outE().forEachRemaining { edge: Edge ->
            Assert.assertEquals(
                ReferenceEdge::class.java, edge.javaClass
            )
        }
        g.V().out().outE().properties<Any>("weight").forEachRemaining { property: Property<Any>? ->
            Assert.assertEquals(
                ReferenceProperty::class.java, property!!.javaClass
            )
        }
        g.V().out().outE().values<Any>("weight").forEachRemaining { value: Any ->
            Assert.assertEquals(
                Double::class.java, value.javaClass
            )
        }
        g.V().out().out().forEachRemaining { vertex: Vertex ->
            Assert.assertEquals(
                ReferenceVertex::class.java, vertex.javaClass
            )
        }
        g.V().out().out().path().forEachRemaining { path: Path ->
            Assert.assertEquals(
                ReferencePath::class.java, path.javaClass
            )
        }
        g.V().out().pageRank().forEachRemaining { vertex: Vertex ->
            Assert.assertEquals(
                ReferenceVertex::class.java, vertex.javaClass
            )
        }
        g.V().out().pageRank().out().forEachRemaining { vertex: Vertex ->
            Assert.assertEquals(
                ReferenceVertex::class.java, vertex.javaClass
            )
        }
        // should handle nested collections
        g.V().out().fold().next().forEach(Consumer { vertex: Vertex ->
            Assert.assertEquals(
                ReferenceVertex::class.java, vertex.javaClass
            )
        })
    }
}