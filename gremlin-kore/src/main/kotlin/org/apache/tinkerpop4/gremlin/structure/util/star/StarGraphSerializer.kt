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

import java.util.HashMap

/**
 * Kryo serializer for [StarGraph].  Implements an internal versioning capability for backward compatibility.
 * The single byte at the front of the serialization stream denotes the version.  That version can be used to choose
 * the correct deserialization mechanism.  The limitation is that this versioning won't help with backward
 * compatibility for custom serializers from providers.  Providers should be encouraged to write their serializers
 * with backward compatibility in mind.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class StarGraphSerializer(edgeDirectionToSerialize: Direction?, graphFilter: GraphFilter) : SerializerShim<StarGraph?> {
    private val edgeDirectionToSerialize: Direction?
    private val graphFilter: GraphFilter

    init {
        this.edgeDirectionToSerialize = edgeDirectionToSerialize
        this.graphFilter = graphFilter
    }

    @Override
    fun <O : OutputShim?> write(kryo: KryoShim<*, O>, output: O, starGraph: StarGraph) {
        output.writeByte(VERSION_1)
        kryo.writeObjectOrNull(output, starGraph.edgeProperties, HashMap::class.java)
        kryo.writeObjectOrNull(output, starGraph.metaProperties, HashMap::class.java)
        kryo.writeClassAndObject(output, starGraph.starVertex.id)
        kryo.writeObject(output, starGraph.starVertex.label)
        writeEdges(kryo, output, starGraph, Direction.IN)
        writeEdges(kryo, output, starGraph, Direction.OUT)
        kryo.writeObject(output, null != starGraph.starVertex.vertexProperties)
        if (null != starGraph.starVertex.vertexProperties) {
            kryo.writeObject(output, starGraph.starVertex.vertexProperties.size())
            for (vertexProperties in starGraph.starVertex.vertexProperties.entrySet()) {
                kryo.writeObject(output, vertexProperties.getKey())
                kryo.writeObject(output, vertexProperties.getValue().size())
                for (vertexProperty in vertexProperties.getValue()) {
                    kryo.writeClassAndObject(output, vertexProperty.id())
                    kryo.writeClassAndObject(output, vertexProperty.value())
                }
            }
        }
    }

    /**
     * If the returned [StarGraph] is null, that means that the [GraphFilter] filtered the vertex.
     */
    @Override
    fun <I : InputShim?> read(kryo: KryoShim<I, *>, input: I, clazz: Class<StarGraph?>?): StarGraph {
        val starGraph: StarGraph = StarGraph.open()
        input.readByte() // version field ignored for now - for future use with backward compatibility
        starGraph.edgeProperties = kryo.readObjectOrNull(input, HashMap::class.java)
        starGraph.metaProperties = kryo.readObjectOrNull(input, HashMap::class.java)
        starGraph.addVertex(T.id, kryo.readClassAndObject(input), T.label, kryo.readObject(input, String::class.java))
        readEdges(kryo, input, starGraph, Direction.IN)
        readEdges(kryo, input, starGraph, Direction.OUT)
        if (kryo.readObject(input, Boolean::class.java)) {
            val numberOfUniqueKeys: Int = kryo.readObject(input, Integer::class.java)
            for (i in 0 until numberOfUniqueKeys) {
                val vertexPropertyKey: String = kryo.readObject(input, String::class.java)
                val numberOfVertexPropertiesWithKey: Int = kryo.readObject(input, Integer::class.java)
                for (j in 0 until numberOfVertexPropertiesWithKey) {
                    val id: Object = kryo.readClassAndObject(input)
                    val value: Object = kryo.readClassAndObject(input)
                    starGraph.starVertex.property(VertexProperty.Cardinality.list, vertexPropertyKey, value, T.id, id)
                }
            }
        }
        return if (graphFilter.hasFilter()) starGraph.applyGraphFilter(graphFilter).orElse(null) else starGraph
    }

    private fun <O : OutputShim?> writeEdges(
        kryo: KryoShim<*, O>,
        output: O,
        starGraph: StarGraph,
        direction: Direction
    ) {
        // only write edges if there are some AND if the user requested them to be serialized AND if they match
        // the direction being serialized by the format
        val starEdges: Map<String, List<Edge>> =
            if (direction.equals(Direction.OUT)) starGraph.starVertex.outEdges else starGraph.starVertex.inEdges
        val writeEdges =
            null != starEdges && edgeDirectionToSerialize != null && (edgeDirectionToSerialize === direction || edgeDirectionToSerialize === Direction.BOTH)
        kryo.writeObject(output, writeEdges)
        if (writeEdges) {
            kryo.writeObject(output, starEdges.size())
            for (edges in starEdges.entrySet()) {
                kryo.writeObject(output, edges.getKey())
                kryo.writeObject(output, edges.getValue().size())
                for (edge in edges.getValue()) {
                    kryo.writeClassAndObject(output, edge.id())
                    kryo.writeClassAndObject(
                        output,
                        if (direction.equals(Direction.OUT)) edge.inVertex().id() else edge.outVertex().id()
                    )
                }
            }
        }
    }

    private fun <I : InputShim?> readEdges(kryo: KryoShim<I, *>, input: I, starGraph: StarGraph, direction: Direction) {
        if (kryo.readObject(input, Boolean::class.java)) {
            val numberOfUniqueLabels: Int = kryo.readObject(input, Integer::class.java)
            for (i in 0 until numberOfUniqueLabels) {
                val edgeLabel: String = kryo.readObject(input, String::class.java)
                val numberOfEdgesWithLabel: Int = kryo.readObject(input, Integer::class.java)
                for (j in 0 until numberOfEdgesWithLabel) {
                    val edgeId: Object = kryo.readClassAndObject(input)
                    val adjacentVertexId: Object = kryo.readClassAndObject(input)
                    if (graphFilter.checkEdgeLegality(direction, edgeLabel).positive()) {
                        if (direction.equals(Direction.OUT)) starGraph.starVertex.addOutEdge(
                            edgeLabel,
                            starGraph.addVertex(T.id, adjacentVertexId),
                            T.id,
                            edgeId
                        ) else starGraph.starVertex.addInEdge(
                            edgeLabel,
                            starGraph.addVertex(T.id, adjacentVertexId),
                            T.id,
                            edgeId
                        )
                    } else if (null != starGraph.edgeProperties) {
                        starGraph.edgeProperties.remove(edgeId)
                    }
                }
            }
        }
    }

    companion object {
        private const val VERSION_1 = Byte.MIN_VALUE
    }
}