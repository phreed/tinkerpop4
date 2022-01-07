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
package org.apache.tinkerpop.gremlin.tinkercat.structure

import org.apache.commons.configuration2.BaseConfiguration
import org.apache.commons.configuration2.Configuration
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerCat.Companion.open
import org.junit.runner.RunWith
import org.junit.experimental.runners.Enclosed
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerCat
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.VertexProperty
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runners.Parameterized
import java.util.*

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
@RunWith(Enclosed::class)
class TinkerCatIdManagerTest {
    @RunWith(Parameterized::class)
    class NumberIdManagerTest {
        @Parameterized.Parameter(value = 0)
        var name: String? = null

        @Parameterized.Parameter(value = 1)
        var vertexIdValue: Any? = null

        @Parameterized.Parameter(value = 2)
        var edgeIdValue: Any? = null

        @Parameterized.Parameter(value = 3)
        var vertexPropertyIdValue: Any? = null
        @Test
        fun shouldUseLongIdManagerToCoerceTypes() {
            val graph: Graph = open(longIdManagerConfig)
            val v = graph.addVertex(T.id, vertexIdValue)
            val vp: VertexProperty<*> =
                v.property(VertexProperty.Cardinality.single, "test", "value", T.id, vertexPropertyIdValue)
            val e = v.addEdge("self", v, T.id, edgeIdValue)
            Assert.assertEquals(100L, v.id())
            Assert.assertEquals(200L, e.id())
            Assert.assertEquals(300L, vp.id())
        }

        @Test
        fun shouldUseIntegerIdManagerToCoerceTypes() {
            val graph: Graph = open(integerIdManagerConfig)
            val v = graph.addVertex(T.id, vertexIdValue)
            val vp: VertexProperty<*> =
                v.property(VertexProperty.Cardinality.single, "test", "value", T.id, vertexPropertyIdValue)
            val e = v.addEdge("self", v, T.id, edgeIdValue)
            Assert.assertEquals(100, v.id())
            Assert.assertEquals(200, e.id())
            Assert.assertEquals(300, vp.id())
        }

        companion object {
            private val longIdManagerConfig: Configuration = BaseConfiguration()
            private val integerIdManagerConfig: Configuration = BaseConfiguration()
            @Parameterized.Parameters(name = "{0}")
            fun data(): Iterable<Array<Any>> {
                return Arrays.asList(
                    *arrayOf(
                        arrayOf("coerceLong", 100L, 200L, 300L),
                        arrayOf("coerceInt", 100, 200, 300),
                        arrayOf("coerceDouble", 100.0, 200.0, 300.0),
                        arrayOf("coerceFloat", 100f, 200f, 300f),
                        arrayOf("coerceString", "100", "200", "300"),
                        arrayOf("coerceMixed", 100.0, 200f, "300")
                    )
                )
            }

            @BeforeClass
            fun setup() {
                longIdManagerConfig.addProperty(
                    TinkerCat.GREMLIN_TINKERGRAPH_EDGE_ID_MANAGER,
                    TinkerCat.DefaultIdManager.LONG.name
                )
                longIdManagerConfig.addProperty(
                    TinkerCat.GREMLIN_TINKERGRAPH_VERTEX_ID_MANAGER,
                    TinkerCat.DefaultIdManager.LONG.name
                )
                longIdManagerConfig.addProperty(
                    TinkerCat.GREMLIN_TINKERGRAPH_VERTEX_PROPERTY_ID_MANAGER,
                    TinkerCat.DefaultIdManager.LONG.name
                )
                integerIdManagerConfig.addProperty(
                    TinkerCat.GREMLIN_TINKERGRAPH_EDGE_ID_MANAGER,
                    TinkerCat.DefaultIdManager.INTEGER.name
                )
                integerIdManagerConfig.addProperty(
                    TinkerCat.GREMLIN_TINKERGRAPH_VERTEX_ID_MANAGER,
                    TinkerCat.DefaultIdManager.INTEGER.name
                )
                integerIdManagerConfig.addProperty(
                    TinkerCat.GREMLIN_TINKERGRAPH_VERTEX_PROPERTY_ID_MANAGER,
                    TinkerCat.DefaultIdManager.INTEGER.name
                )
            }
        }
    }

    @RunWith(Parameterized::class)
    class UuidIdManagerTest {
        @Parameterized.Parameter(value = 0)
        var name: String? = null

        @Parameterized.Parameter(value = 1)
        var vertexIdValue: Any? = null

        @Parameterized.Parameter(value = 2)
        var edgeIdValue: Any? = null

        @Parameterized.Parameter(value = 3)
        var vertexPropertyIdValue: Any? = null
        @Test
        fun shouldUseIdManagerToCoerceTypes() {
            val graph: Graph = open(idManagerConfig)
            val v = graph.addVertex(T.id, vertexIdValue)
            val vp: VertexProperty<*> =
                v.property(VertexProperty.Cardinality.single, "test", "value", T.id, vertexPropertyIdValue)
            val e = v.addEdge("self", v, T.id, edgeIdValue)
            Assert.assertEquals(vertexId, v.id())
            Assert.assertEquals(edgeId, e.id())
            Assert.assertEquals(vertexPropertyId, vp.id())
        }

        companion object {
            private val idManagerConfig: Configuration = BaseConfiguration()
            private val vertexId = UUID.fromString("0E939658-ADD2-4598-A722-2FC178E9B741")
            private val edgeId = UUID.fromString("748179AA-E319-8C36-41AE-F3576B73E05C")
            private val vertexPropertyId = UUID.fromString("EC27384C-39A0-923D-9410-271B585683B6")
            @Parameterized.Parameters(name = "{0}")
            fun data(): Iterable<Array<Any>> {
                return Arrays.asList(
                    *arrayOf(
                        arrayOf("coerceUuid", vertexId, edgeId, vertexPropertyId),
                        arrayOf("coerceString", vertexId.toString(), edgeId.toString(), vertexPropertyId.toString()),
                        arrayOf("coerceMixed", vertexId, edgeId, vertexPropertyId.toString())
                    )
                )
            }

            @BeforeClass
            fun setup() {
                idManagerConfig.addProperty(
                    TinkerCat.GREMLIN_TINKERGRAPH_EDGE_ID_MANAGER,
                    TinkerCat.DefaultIdManager.UUID.name
                )
                idManagerConfig.addProperty(
                    TinkerCat.GREMLIN_TINKERGRAPH_VERTEX_ID_MANAGER,
                    TinkerCat.DefaultIdManager.UUID.name
                )
                idManagerConfig.addProperty(
                    TinkerCat.GREMLIN_TINKERGRAPH_VERTEX_PROPERTY_ID_MANAGER,
                    TinkerCat.DefaultIdManager.UUID.name
                )
            }
        }
    }
}