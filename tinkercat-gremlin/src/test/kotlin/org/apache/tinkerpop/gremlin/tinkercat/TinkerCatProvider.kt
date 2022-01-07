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
package org.apache.tinkerpop.gremlin.tinkercat

import org.apache.commons.configuration2.Configuration
import org.apache.tinkerpop.gremlin.AbstractGraphProvider
import org.apache.tinkerpop.gremlin.LoadGraphWith.GraphData
import org.apache.tinkerpop.gremlin.tinkercat.TinkerCatProvider
import org.apache.tinkerpop.gremlin.structure.VertexProperty
import org.apache.tinkerpop.gremlin.TestHelper
import kotlin.Throws
import org.apache.tinkerpop.gremlin.structure.io.IoEdgeTest
import org.apache.tinkerpop.gremlin.structure.io.IoVertexTest
import org.apache.tinkerpop.gremlin.LoadGraphWith
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.GraphTest
import java.lang.IllegalStateException
import org.apache.tinkerpop.gremlin.structure.util.star.StarGraphTest
import org.apache.tinkerpop.gremlin.structure.util.detached.DetachedGraphTest
import org.apache.tinkerpop.gremlin.tinkercat.structure.*
import java.io.File
import java.lang.Exception
import java.util.HashMap
import java.util.HashSet

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
open class TinkerCatProvider : AbstractGraphProvider() {
    override fun getBaseConfiguration(
        graphName: String, test: Class<*>, testMethodName: String,
        loadGraphWith: GraphData
    ): Map<String, Any> {
        val idManager = selectIdMakerFromGraphData(loadGraphWith)
        val idMaker = (if (idManager == TinkerCat.DefaultIdManager.ANY) selectIdMakerFromTest(
            test,
            testMethodName
        ) else idManager).name
        return object : HashMap<String?, Any?>() {
            init {
                put(Graph.GRAPH, TinkerCat::class.java.name)
                put(TinkerCat.GREMLIN_TINKERGRAPH_VERTEX_ID_MANAGER, idMaker)
                put(TinkerCat.GREMLIN_TINKERGRAPH_EDGE_ID_MANAGER, idMaker)
                put(TinkerCat.GREMLIN_TINKERGRAPH_VERTEX_PROPERTY_ID_MANAGER, idMaker)
                if (requiresListCardinalityAsDefault(
                        loadGraphWith,
                        test,
                        testMethodName
                    )
                ) put(
                    TinkerCat.GREMLIN_TINKERGRAPH_DEFAULT_VERTEX_PROPERTY_CARDINALITY,
                    VertexProperty.Cardinality.list.name
                )
                if (requiresPersistence(test, testMethodName)) {
                    put(TinkerCat.GREMLIN_TINKERGRAPH_GRAPH_FORMAT, "gryo")
                    put(
                        TinkerCat.GREMLIN_TINKERGRAPH_GRAPH_LOCATION,
                        TestHelper.makeTestDataFile(test, "temp", "$testMethodName.kryo")
                    )
                }
            }
        }
    }

    @Throws(Exception::class)
    override fun clear(graph: Graph, configuration: Configuration) {
        if (graph != null) graph.close()

        // in the even the graph is persisted we need to clean up
        val graphLocation = if (null != configuration) configuration.getString(
            TinkerCat.GREMLIN_TINKERGRAPH_GRAPH_LOCATION,
            null
        ) else null
        if (graphLocation != null) {
            val f = File(graphLocation)
            f.delete()
        }
    }

    override fun getImplementations(): Set<Class<*>> {
        return IMPLEMENTATION
    }

    /**
     * Some tests require special configuration for TinkerCat to properly configure the id manager.
     */
    protected fun selectIdMakerFromTest(test: Class<*>, testMethodName: String): TinkerCat.DefaultIdManager {
        if (test == GraphTest::class.java) {
            val testsThatNeedLongIdManager: Set<String> = object : HashSet<String?>() {
                init {
                    add("shouldIterateVerticesWithNumericIdSupportUsingDoubleRepresentation")
                    add("shouldIterateVerticesWithNumericIdSupportUsingDoubleRepresentations")
                    add("shouldIterateVerticesWithNumericIdSupportUsingIntegerRepresentation")
                    add("shouldIterateVerticesWithNumericIdSupportUsingIntegerRepresentations")
                    add("shouldIterateVerticesWithNumericIdSupportUsingFloatRepresentation")
                    add("shouldIterateVerticesWithNumericIdSupportUsingFloatRepresentations")
                    add("shouldIterateVerticesWithNumericIdSupportUsingStringRepresentation")
                    add("shouldIterateVerticesWithNumericIdSupportUsingStringRepresentations")
                    add("shouldIterateEdgesWithNumericIdSupportUsingDoubleRepresentation")
                    add("shouldIterateEdgesWithNumericIdSupportUsingDoubleRepresentations")
                    add("shouldIterateEdgesWithNumericIdSupportUsingIntegerRepresentation")
                    add("shouldIterateEdgesWithNumericIdSupportUsingIntegerRepresentations")
                    add("shouldIterateEdgesWithNumericIdSupportUsingFloatRepresentation")
                    add("shouldIterateEdgesWithNumericIdSupportUsingFloatRepresentations")
                    add("shouldIterateEdgesWithNumericIdSupportUsingStringRepresentation")
                    add("shouldIterateEdgesWithNumericIdSupportUsingStringRepresentations")
                }
            }
            val testsThatNeedUuidIdManager: Set<String> = object : HashSet<String?>() {
                init {
                    add("shouldIterateVerticesWithUuidIdSupportUsingStringRepresentation")
                    add("shouldIterateVerticesWithUuidIdSupportUsingStringRepresentations")
                    add("shouldIterateEdgesWithUuidIdSupportUsingStringRepresentation")
                    add("shouldIterateEdgesWithUuidIdSupportUsingStringRepresentations")
                }
            }
            if (testsThatNeedLongIdManager.contains(testMethodName)) return TinkerCat.DefaultIdManager.LONG else if (testsThatNeedUuidIdManager.contains(
                    testMethodName
                )
            ) return TinkerCat.DefaultIdManager.UUID
        } else if (test == IoEdgeTest::class.java) {
            val testsThatNeedLongIdManager: Set<String> = object : HashSet<String?>() {
                init {
                    add("shouldReadWriteEdge[graphson-v1]")
                    add("shouldReadWriteDetachedEdgeAsReference[graphson-v1]")
                    add("shouldReadWriteDetachedEdge[graphson-v1]")
                    add("shouldReadWriteEdge[graphson-v2]")
                    add("shouldReadWriteDetachedEdgeAsReference[graphson-v2]")
                    add("shouldReadWriteDetachedEdge[graphson-v2]")
                }
            }
            if (testsThatNeedLongIdManager.contains(testMethodName)) return TinkerCat.DefaultIdManager.LONG
        } else if (test == IoVertexTest::class.java) {
            val testsThatNeedLongIdManager: Set<String> = object : HashSet<String?>() {
                init {
                    add("shouldReadWriteVertexWithBOTHEdges[graphson-v1]")
                    add("shouldReadWriteVertexWithINEdges[graphson-v1]")
                    add("shouldReadWriteVertexWithOUTEdges[graphson-v1]")
                    add("shouldReadWriteVertexNoEdges[graphson-v1]")
                    add("shouldReadWriteDetachedVertexNoEdges[graphson-v1]")
                    add("shouldReadWriteDetachedVertexAsReferenceNoEdges[graphson-v1]")
                    add("shouldReadWriteVertexMultiPropsNoEdges[graphson-v1]")
                    add("shouldReadWriteVertexWithBOTHEdges[graphson-v2]")
                    add("shouldReadWriteVertexWithINEdges[graphson-v2]")
                    add("shouldReadWriteVertexWithOUTEdges[graphson-v2]")
                    add("shouldReadWriteVertexNoEdges[graphson-v2]")
                    add("shouldReadWriteDetachedVertexNoEdges[graphson-v2]")
                    add("shouldReadWriteDetachedVertexAsReferenceNoEdges[graphson-v2]")
                    add("shouldReadWriteVertexMultiPropsNoEdges[graphson-v2]")
                }
            }
            if (testsThatNeedLongIdManager.contains(testMethodName)) return TinkerCat.DefaultIdManager.LONG
        }
        return TinkerCat.DefaultIdManager.ANY
    }

    /**
     * Test that load with specific graph data can be configured with a specific id manager as the data type to
     * be used in the test for that graph is known.
     */
    protected fun selectIdMakerFromGraphData(loadGraphWith: GraphData?): TinkerCat.DefaultIdManager {
        if (null == loadGraphWith) return TinkerCat.DefaultIdManager.ANY
        return if (loadGraphWith == GraphData.CLASSIC) TinkerCat.DefaultIdManager.INTEGER else if (loadGraphWith == GraphData.MODERN) TinkerCat.DefaultIdManager.INTEGER else if (loadGraphWith == GraphData.CREW) TinkerCat.DefaultIdManager.INTEGER else if (loadGraphWith == GraphData.GRATEFUL) TinkerCat.DefaultIdManager.INTEGER else if (loadGraphWith == GraphData.SINK) TinkerCat.DefaultIdManager.INTEGER else throw IllegalStateException(
            String.format(
                "Need to define a new %s for %s",
                TinkerCat.IdManager::class.java.name,
                loadGraphWith.name
            )
        )
    }

    companion object {
        private val IMPLEMENTATION: Set<Class<*>> = object : HashSet<Class<*>?>() {
            init {
                add(TinkerEdge::class.java)
                add(TinkerElement::class.java)
                add(TinkerCat::class.java)
                add(TinkerCatVariables::class.java)
                add(TinkerProperty::class.java)
                add(TinkerVertex::class.java)
                add(TinkerVertexProperty::class.java)
            }
        }

        /**
         * Determines if a test requires TinkerCat persistence to be configured with graph location and format.
         */
        @JvmStatic
        protected fun requiresPersistence(test: Class<*>, testMethodName: String): Boolean {
            return test == GraphTest::class.java && testMethodName == "shouldPersistDataOnClose"
        }

        /**
         * Determines if a test requires a different cardinality as the default or not.
         */
        @JvmStatic
        protected fun requiresListCardinalityAsDefault(
            loadGraphWith: GraphData,
            test: Class<*>, testMethodName: String
        ): Boolean {
            return (loadGraphWith == GraphData.CREW || test == StarGraphTest::class.java && testMethodName == "shouldAttachWithCreateMethod"
                    || test == DetachedGraphTest::class.java && testMethodName == "testAttachableCreateMethod")
        }
    }
}