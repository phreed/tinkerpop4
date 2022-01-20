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

import org.apache.tinkerpop4.gremlin.structure.Direction

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class StarGraphGraphSONSerializerV1d0(private val normalize: Boolean) : StdSerializer<DirectionalStarGraph?>(
    DirectionalStarGraph::class.java
) {
    @Override
    @Throws(IOException::class, JsonGenerationException::class)
    fun serialize(
        starGraph: DirectionalStarGraph, jsonGenerator: JsonGenerator,
        serializerProvider: SerializerProvider
    ) {
        ser(starGraph, jsonGenerator, serializerProvider, null)
    }

    @Override
    @Throws(IOException::class, JsonProcessingException::class)
    fun serializeWithType(
        starGraph: DirectionalStarGraph, jsonGenerator: JsonGenerator,
        serializerProvider: SerializerProvider,
        typeSerializer: TypeSerializer?
    ) {
        ser(starGraph, jsonGenerator, serializerProvider, typeSerializer)
    }

    @Throws(IOException::class, JsonProcessingException::class)
    private fun ser(
        directionalStarGraph: DirectionalStarGraph, jsonGenerator: JsonGenerator,
        serializerProvider: SerializerProvider,
        typeSerializer: TypeSerializer?
    ) {
        val starGraph: StarGraph = directionalStarGraph.getStarGraphToSerialize()
        jsonGenerator.writeStartObject()
        if (typeSerializer != null) jsonGenerator.writeStringField(GraphSONTokens.CLASS, HashMap::class.java.getName())
        GraphSONUtil.writeWithType(
            GraphSONTokens.ID,
            starGraph.starVertex.id,
            jsonGenerator,
            serializerProvider,
            typeSerializer
        )
        jsonGenerator.writeStringField(GraphSONTokens.LABEL, starGraph.starVertex.label)
        if (directionalStarGraph.getDirection() != null) writeEdges(
            directionalStarGraph,
            jsonGenerator,
            serializerProvider,
            typeSerializer,
            Direction.IN
        )
        if (directionalStarGraph.getDirection() != null) writeEdges(
            directionalStarGraph,
            jsonGenerator,
            serializerProvider,
            typeSerializer,
            Direction.OUT
        )
        if (starGraph.starVertex.vertexProperties != null && !starGraph.starVertex.vertexProperties.isEmpty()) {
            jsonGenerator.writeObjectFieldStart(GraphSONTokens.PROPERTIES)
            if (typeSerializer != null) jsonGenerator.writeStringField(
                GraphSONTokens.CLASS,
                HashMap::class.java.getName()
            )
            val keys: Set<String> =
                if (normalize) TreeSet(starGraph.starVertex.vertexProperties.keySet()) else starGraph.starVertex.vertexProperties.keySet()
            for (k in keys) {
                val vp: List<VertexProperty> = starGraph.starVertex.vertexProperties.get(k)
                jsonGenerator.writeArrayFieldStart(k)
                if (typeSerializer != null) {
                    jsonGenerator.writeString(ArrayList::class.java.getName())
                    jsonGenerator.writeStartArray()
                }
                val vertexProperties: List<VertexProperty> =
                    if (normalize) sort<Any>(vp, Comparators.PROPERTY_COMPARATOR) else vp
                for (property in vertexProperties) {
                    jsonGenerator.writeStartObject()
                    if (typeSerializer != null) jsonGenerator.writeStringField(
                        GraphSONTokens.CLASS,
                        HashMap::class.java.getName()
                    )
                    GraphSONUtil.writeWithType(
                        GraphSONTokens.ID,
                        property.id(),
                        jsonGenerator,
                        serializerProvider,
                        typeSerializer
                    )
                    GraphSONUtil.writeWithType(
                        GraphSONTokens.VALUE,
                        property.value(),
                        jsonGenerator,
                        serializerProvider,
                        typeSerializer
                    )
                    val metaProperties: Iterator<Property> =
                        if (normalize) IteratorUtils.list(property.properties(), Comparators.PROPERTY_COMPARATOR)
                            .iterator() else property.properties()
                    if (metaProperties.hasNext()) {
                        jsonGenerator.writeObjectFieldStart(GraphSONTokens.PROPERTIES)
                        if (typeSerializer != null) jsonGenerator.writeStringField(
                            GraphSONTokens.CLASS,
                            HashMap::class.java.getName()
                        )
                        while (metaProperties.hasNext()) {
                            val meta: Property<Object> = metaProperties.next()
                            GraphSONUtil.writeWithType(
                                meta.key(),
                                meta.value(),
                                jsonGenerator,
                                serializerProvider,
                                typeSerializer
                            )
                        }
                        jsonGenerator.writeEndObject()
                    }
                    jsonGenerator.writeEndObject()
                }
                jsonGenerator.writeEndArray()
                if (typeSerializer != null) jsonGenerator.writeEndArray()
            }
            jsonGenerator.writeEndObject()
        }
        // For some reason, this wasn't closed.
        jsonGenerator.writeEndObject()
    }

    @Throws(IOException::class, JsonProcessingException::class)
    private fun writeEdges(
        directionalStarGraph: DirectionalStarGraph, jsonGenerator: JsonGenerator,
        serializerProvider: SerializerProvider,
        typeSerializer: TypeSerializer?,
        direction: Direction
    ) {
        // only write edges if there are some AND if the user requested them to be serialized AND if they match
        // the direction being serialized by the format
        val starGraph: StarGraph = directionalStarGraph.getStarGraphToSerialize()
        val edgeDirectionToSerialize: Direction = directionalStarGraph.getDirection()
        val starEdges: Map<String, List<Edge>> =
            if (direction.equals(Direction.OUT)) starGraph.starVertex.outEdges else starGraph.starVertex.inEdges
        val writeEdges =
            null != starEdges && edgeDirectionToSerialize != null && (edgeDirectionToSerialize === direction || edgeDirectionToSerialize === Direction.BOTH)
        if (writeEdges) {
            jsonGenerator.writeObjectFieldStart(if (direction === Direction.IN) GraphSONTokens.IN_E else GraphSONTokens.OUT_E)
            if (typeSerializer != null) jsonGenerator.writeStringField(
                GraphSONTokens.CLASS,
                HashMap::class.java.getName()
            )
            val keys: Set<String> = if (normalize) TreeSet(starEdges.keySet()) else starEdges.keySet()
            for (k in keys) {
                val edges: List<Edge> = starEdges[k]!!
                jsonGenerator.writeArrayFieldStart(k)
                if (typeSerializer != null) {
                    jsonGenerator.writeString(ArrayList::class.java.getName())
                    jsonGenerator.writeStartArray()
                }
                val edgesToWrite: List<Edge> = if (normalize) sort<Any>(edges, Comparators.EDGE_COMPARATOR) else edges
                for (edge in edgesToWrite) {
                    jsonGenerator.writeStartObject()
                    if (typeSerializer != null) jsonGenerator.writeStringField(
                        GraphSONTokens.CLASS,
                        HashMap::class.java.getName()
                    )
                    GraphSONUtil.writeWithType(
                        GraphSONTokens.ID,
                        edge.id(),
                        jsonGenerator,
                        serializerProvider,
                        typeSerializer
                    )
                    GraphSONUtil.writeWithType(
                        if (direction.equals(Direction.OUT)) GraphSONTokens.IN else GraphSONTokens.OUT,
                        if (direction.equals(Direction.OUT)) edge.inVertex().id() else edge.outVertex().id(),
                        jsonGenerator, serializerProvider, typeSerializer
                    )
                    val edgeProperties: Iterator<Property<Object>> =
                        if (normalize) IteratorUtils.list(edge.properties(), Comparators.PROPERTY_COMPARATOR)
                            .iterator() else edge.properties()
                    if (edgeProperties.hasNext()) {
                        jsonGenerator.writeObjectFieldStart(GraphSONTokens.PROPERTIES)
                        if (typeSerializer != null) jsonGenerator.writeStringField(
                            GraphSONTokens.CLASS,
                            HashMap::class.java.getName()
                        )
                        while (edgeProperties.hasNext()) {
                            val meta: Property<Object> = edgeProperties.next()
                            GraphSONUtil.writeWithType(
                                meta.key(),
                                meta.value(),
                                jsonGenerator,
                                serializerProvider,
                                typeSerializer
                            )
                        }
                        jsonGenerator.writeEndObject()
                    }
                    jsonGenerator.writeEndObject()
                }
                jsonGenerator.writeEndArray()
                if (typeSerializer != null) jsonGenerator.writeEndArray()
            }
            jsonGenerator.writeEndObject()
        }
    }

    companion object {
        fun <S> sort(listToSort: List<S>, comparator: Comparator?): List<S> {
            Collections.sort(listToSort, comparator)
            return listToSort
        }
    }
}