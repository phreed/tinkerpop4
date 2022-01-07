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

import org.apache.tinkerpop.gremlin.tinkercat.TinkerCatProvider.Companion.requiresListCardinalityAsDefault
import org.apache.tinkerpop.gremlin.tinkercat.TinkerCatProvider.Companion.requiresPersistence
import org.apache.tinkerpop.gremlin.tinkercat.TinkerCatProvider
import org.apache.tinkerpop.gremlin.LoadGraphWith.GraphData
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerCat
import org.apache.tinkerpop.gremlin.structure.VertexProperty
import org.apache.tinkerpop.gremlin.TestHelper
import org.apache.tinkerpop.gremlin.structure.Graph
import java.util.HashMap

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class TinkerCatUUIDProvider : TinkerCatProvider() {
    override fun getBaseConfiguration(
        graphName: String, test: Class<*>?, testMethodName: String,
        loadGraphWith: GraphData
    ): Map<String, Any> {
        val idManager = TinkerCat.DefaultIdManager.UUID
        val idMaker = idManager.name
        return object : HashMap<String?, Any?>() {
            init {
                put(Graph.GRAPH, TinkerCat::class.java.name)
                put(TinkerCat.GREMLIN_TINKERGRAPH_VERTEX_ID_MANAGER, idMaker)
                put(TinkerCat.GREMLIN_TINKERGRAPH_EDGE_ID_MANAGER, idMaker)
                put(TinkerCat.GREMLIN_TINKERGRAPH_VERTEX_PROPERTY_ID_MANAGER, idMaker)
                if (requiresListCardinalityAsDefault(
                        loadGraphWith,
                        test!!,
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
}