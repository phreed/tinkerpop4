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
package org.apache.tinkerpop4.gremlin.tinkercat

import org.apache.commons.configuration2.Configuration
import org.apache.tinkerpop4.gremlin.AbstractGraphProvider
import org.apache.tinkerpop4.gremlin.LoadGraphWith.GraphData
import org.apache.tinkerpop4.gremlin.tinkercat.TinkerCatProvider
import org.apache.tinkerpop4.gremlin.structure.VertexProperty
import org.apache.tinkerpop4.gremlin.TestHelper
import kotlin.Throws
import org.apache.tinkerpop4.gremlin.structure.io.IoEdgeTest
import org.apache.tinkerpop4.gremlin.structure.io.IoVertexTest
import org.apache.tinkerpop4.gremlin.LoadGraphWith
import org.apache.tinkerpop4.gremlin.structure.Graph
import org.apache.tinkerpop4.gremlin.structure.GraphTest
import java.lang.IllegalStateException
import org.apache.tinkerpop4.gremlin.structure.util.star.StarGraphTest
import org.apache.tinkerpop4.gremlin.structure.util.detached.DetachedGraphTest
import org.apache.tinkerpop4.gremlin.tinkercat.structure.*
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
    ): MutableMap<String, Any> {
        val idManager = selectIdMakerFromGraphData(loadGraphWith)
        val idMaker = (if (idManager == TinkerCat.DefaultIdManager.ANY) selectIdMakerFromTest(
            test,
            testMethodName
        ) else idManager).name

        val configMap = mutableMapOf(
            Graph.GRAPH to TinkerCat::class.java.name,
            TinkerCat.GREMLIN_TINKERGRAPH_VERTEX_ID_MANAGER to idMaker,
            TinkerCat.GREMLIN_TINKERGRAPH_EDGE_ID_MANAGER to idMaker,
            TinkerCat.GREMLIN_TINKERGRAPH_VERTEX_PROPERTY_ID_MANAGER to idMaker)

        if (requiresListCardinalityAsDefault(loadGraphWith,test,testMethodName)) {
            configMap[TinkerCat.GREMLIN_TINKERGRAPH_DEFAULT_VERTEX_PROPERTY_CARDINALITY] = VertexProperty.Cardinality.list.name
        }
        if (requiresPersistence(test, testMethodName)) {
            configMap[TinkerCat.GREMLIN_TINKERGRAPH_GRAPH_FORMAT] = "gryo"
            configMap[TinkerCat.GREMLIN_TINKERGRAPH_GRAPH_LOCATION] = TestHelper.makeTestDataFile(test, "temp", "$testMethodName.kryo")
        }
        return configMap as MutableMap<String,Any>
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
    private fun selectIdMakerFromTest(test: Class<*>, testMethodName: String): TinkerCat.DefaultIdManager {
        when (test) {
            is GraphTest -> {
                val testsThatNeedLongIdManager = setOf(
                    "shouldIterateVerticesWithNumericIdSupportUsingDoubleRepresentation",
                    "shouldIterateVerticesWithNumericIdSupportUsingDoubleRepresentations",
                    "shouldIterateVerticesWithNumericIdSupportUsingIntegerRepresentation",
                    "shouldIterateVerticesWithNumericIdSupportUsingIntegerRepresentations",
                    "shouldIterateVerticesWithNumericIdSupportUsingFloatRepresentation",
                    "shouldIterateVerticesWithNumericIdSupportUsingFloatRepresentations",
                    "shouldIterateVerticesWithNumericIdSupportUsingStringRepresentation",
                    "shouldIterateVerticesWithNumericIdSupportUsingStringRepresentations",
                    "shouldIterateEdgesWithNumericIdSupportUsingDoubleRepresentation",
                    "shouldIterateEdgesWithNumericIdSupportUsingDoubleRepresentations",
                    "shouldIterateEdgesWithNumericIdSupportUsingIntegerRepresentation",
                    "shouldIterateEdgesWithNumericIdSupportUsingIntegerRepresentations",
                    "shouldIterateEdgesWithNumericIdSupportUsingFloatRepresentation",
                    "shouldIterateEdgesWithNumericIdSupportUsingFloatRepresentations",
                    "shouldIterateEdgesWithNumericIdSupportUsingStringRepresentation",
                    "shouldIterateEdgesWithNumericIdSupportUsingStringRepresentations",
                )
                val testsThatNeedUuidIdManager = setOf(
                    "shouldIterateVerticesWithUuidIdSupportUsingStringRepresentation",
                    "shouldIterateVerticesWithUuidIdSupportUsingStringRepresentations",
                    "shouldIterateEdgesWithUuidIdSupportUsingStringRepresentation",
                    "shouldIterateEdgesWithUuidIdSupportUsingStringRepresentations",
                )
                if (testsThatNeedLongIdManager.contains(testMethodName)) return TinkerCat.DefaultIdManager.LONG
                else if (testsThatNeedUuidIdManager.contains(testMethodName)) return TinkerCat.DefaultIdManager.UUID
            }
            is IoEdgeTest -> {
                val testsThatNeedLongIdManager = setOf(
                    "shouldReadWriteEdge[graphson-v1]",
                    "shouldReadWriteDetachedEdgeAsReference[graphson-v1]",
                    "shouldReadWriteDetachedEdge[graphson-v1]",
                    "shouldReadWriteEdge[graphson-v2]",
                    "shouldReadWriteDetachedEdgeAsReference[graphson-v2]",
                    "shouldReadWriteDetachedEdge[graphson-v2]",
                )
                if (testsThatNeedLongIdManager.contains(testMethodName)) return TinkerCat.DefaultIdManager.LONG
            }

            is IoVertexTest -> {
                val testsThatNeedLongIdManager = setOf(
                    "shouldReadWriteVertexWithBOTHEdges[graphson-v1]",
                    "shouldReadWriteVertexWithINEdges[graphson-v1]",
                    "shouldReadWriteVertexWithOUTEdges[graphson-v1]",
                    "shouldReadWriteVertexNoEdges[graphson-v1]",
                    "shouldReadWriteDetachedVertexNoEdges[graphson-v1]",
                    "shouldReadWriteDetachedVertexAsReferenceNoEdges[graphson-v1]",
                    "shouldReadWriteVertexMultiPropsNoEdges[graphson-v1]",
                    "shouldReadWriteVertexWithBOTHEdges[graphson-v2]",
                    "shouldReadWriteVertexWithINEdges[graphson-v2]",
                    "shouldReadWriteVertexWithOUTEdges[graphson-v2]",
                    "shouldReadWriteVertexNoEdges[graphson-v2]",
                    "shouldReadWriteDetachedVertexNoEdges[graphson-v2]",
                    "shouldReadWriteDetachedVertexAsReferenceNoEdges[graphson-v2]",
                    "shouldReadWriteVertexMultiPropsNoEdges[graphson-v2]",
                )
                if (testsThatNeedLongIdManager.contains(testMethodName)) return TinkerCat.DefaultIdManager.LONG
            }
        }
        return TinkerCat.DefaultIdManager.ANY
    }

    /**
     * Test that load with specific graph data can be configured with a specific id manager as the data type to
     * be used in the test for that graph is known.
     */
    private fun selectIdMakerFromGraphData(loadGraphWith: GraphData?): TinkerCat.DefaultIdManager {
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
        private val IMPLEMENTATION = setOf(
            TinkerEdge::class.java,
            TinkerElement::class.java,
            TinkerCat::class.java,
            TinkerCatVariables::class.java,
            TinkerProperty::class.java,
            TinkerVertex::class.java,
            TinkerVertexProperty::class.java,
        )

        /**
         * Determines if a test requires TinkerCat persistence to be configured with graph location and format.
         */
        fun requiresPersistence(test: Class<*>, testMethodName: String): Boolean {
            return test == GraphTest::class.java && testMethodName == "shouldPersistDataOnClose"
        }

        /**
         * Determines if a test requires a different cardinality as the default or not.
         */
        fun requiresListCardinalityAsDefault(
            loadGraphWith: GraphData,
            test: Class<*>, testMethodName: String
        ): Boolean {
            return when (testMethodName) {
                "shouldAttachWithCreateMethod" -> (loadGraphWith == GraphData.CREW || test is StarGraphTest)
                "testAttachableCreateMethod" -> test is DetachedGraphTest
                else -> false
            }
        }
    }
}