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
package org.apache.tinkerpop4.gremlin.structure.util.star

import org.apache.tinkerpop4.gremlin.structure.Edge

/**
 * Provides helper functions for reading vertex and edges from their serialized GraphSON forms.
 */
object StarGraphGraphSONDeserializer {
    /**
     * A helper function for reading vertex edges from a serialized [StarGraph] (i.e. a `Map`) generated by
     * [StarGraphGraphSONSerializerV1d0].
     */
    @Throws(IOException::class)
    fun readStarGraphEdges(
        edgeMaker: Function<Attachable<Edge?>?, Edge?>?,
        starGraph: StarGraph,
        vertexData: Map<String?, Object?>,
        direction: String
    ) {
        for (edgeData in vertexData[direction].entrySet()) {
            for (inner in edgeData.getValue()) {
                val starEdge: StarEdge
                starEdge = if (direction.equals(GraphSONTokens.OUT_E)) starGraph.getStarVertex().addOutEdge(
                    edgeData.getKey(),
                    starGraph.addVertex(T.id, inner[GraphSONTokens.IN]),
                    T.id,
                    inner[GraphSONTokens.ID]
                ) as StarEdge else starGraph.getStarVertex().addInEdge(
                    edgeData.getKey(),
                    starGraph.addVertex(T.id, inner[GraphSONTokens.OUT]),
                    T.id,
                    inner[GraphSONTokens.ID]
                ) as StarEdge
                if (inner.containsKey(GraphSONTokens.PROPERTIES)) {
                    for (epd in inner[GraphSONTokens.PROPERTIES].entrySet()) {
                        starEdge.property(epd.getKey(), epd.getValue())
                    }
                }
                if (edgeMaker != null) edgeMaker.apply(starEdge)
            }
        }
    }

    /**
     * A helper function for reading a serialized [StarGraph] from a `Map` generated by
     * [StarGraphGraphSONSerializerV1d0].
     */
    @Throws(IOException::class)
    fun readStarGraphVertex(vertexData: Map<String?, Object?>): StarGraph {
        val starGraph: StarGraph = StarGraph.open()
        starGraph.addVertex(T.id, vertexData[GraphSONTokens.ID], T.label, vertexData[GraphSONTokens.LABEL])
        if (vertexData.containsKey(GraphSONTokens.PROPERTIES)) {
            for (property in vertexData[GraphSONTokens.PROPERTIES].entrySet()) {
                for (p in property.getValue()) {
                    val vp: StarVertexProperty = starGraph.getStarVertex().property(
                        VertexProperty.Cardinality.list,
                        property.getKey(),
                        p[GraphSONTokens.VALUE],
                        T.id,
                        p[GraphSONTokens.ID]
                    ) as StarVertexProperty
                    if (p.containsKey(GraphSONTokens.PROPERTIES)) {
                        for (epd in p[GraphSONTokens.PROPERTIES].entrySet()) {
                            vp.property(epd.getKey(), epd.getValue())
                        }
                    }
                }
            }
        }
        return starGraph
    }
}