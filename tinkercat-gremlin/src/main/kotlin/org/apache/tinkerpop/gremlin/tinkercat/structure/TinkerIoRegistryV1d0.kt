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
import org.apache.tinkerpop.gremlin.structure.Edge
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.io.AbstractIoRegistry
import org.apache.tinkerpop.gremlin.structure.io.Mapper
import org.apache.tinkerpop.gremlin.structure.io.binary.TypeSerializer
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONIo
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONTokens
import org.apache.tinkerpop.gremlin.structure.io.gryo.GryoIo
import org.apache.tinkerpop.gremlin.structure.io.gryo.GryoReader
import org.apache.tinkerpop.gremlin.structure.util.Attachable
import org.apache.tinkerpop.gremlin.structure.util.detached.DetachedEdge
import org.apache.tinkerpop.gremlin.structure.util.detached.DetachedVertex
import org.apache.tinkerpop.gremlin.util.Serializer
import org.apache.tinkerpop.shaded.jackson.core.JsonGenerator
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.lang.Exception
import java.util.ArrayList
import java.util.HashMap
import javax.json.stream.JsonGenerator
import javax.json.stream.JsonParser

/**
 * An implementation of the [IoRegistry] interface that provides serializers with custom configurations for
 * implementation specific classes that might need to be serialized.  This registry allows a [TinkerCat] to
 * be serialized directly which is useful for moving small graphs around on the network.
 *
 *
 * Most providers need not implement this kind of custom serializer as they will deal with much larger graphs that
 * wouldn't be practical to serialize in this fashion.  This is a bit of a special case for TinkerCat given its
 * in-memory status.  Typical implementations would create serializers for a complex vertex identifier or a
 * custom data class like a "geographic point".
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class TinkerIoRegistryV1d0 private constructor() : AbstractIoRegistry() {
    init {
        register(GryoIo::class.java, TinkerCat::class.java, TinkerCatGryoSerializer())
        register(GraphSONIo::class.java, null, TinkerModule())
    }

    /**
     * Provides a method to serialize an entire [TinkerCat] into itself for Gryo.  This is useful when
     * shipping small graphs around through Gremlin Server. Reuses the existing Kryo instance for serialization.
     */
    internal class TinkerCatGryoSerializer : Serializer<TinkerCat?>() {
        fun write(kryo: Kryo?, output: Output, graph: TinkerCat?) {
            try {
                ByteArrayOutputStream().use { stream ->
                    GryoWriter.build().mapper(Mapper<Kryo?> { kryo }).create().writeGraph(stream, graph)
                    val bytes = stream.toByteArray()
                    output.writeInt(bytes.size)
                    output.write(bytes)
                }
            } catch (io: Exception) {
                throw RuntimeException(io)
            }
        }

        fun read(kryo: Kryo?, input: Input, tinkerGraphClass: Class<TinkerCat?>?): TinkerCat {
            val conf: Configuration = BaseConfiguration()
            conf.setProperty("gremlin.tinkercat.defaultVertexPropertyCardinality", "list")
            val graph = TinkerCat.open(conf)
            val len: Int = input.readInt()
            val bytes: ByteArray = input.readBytes(len)
            try {
                ByteArrayInputStream(bytes).use { stream ->
                    GryoReader.build().mapper(Mapper<Kryo?> { kryo }).create().readGraph(stream, graph)
                }
            } catch (io: Exception) {
                throw RuntimeException(io)
            }
            return graph
        }
    }

    /**
     * Provides a method to serialize an entire [TinkerCat] into itself for GraphSON.  This is useful when
     * shipping small graphs around through Gremlin Server.
     */
    internal class TinkerModule : SimpleModule("tinkercat-1.0") {
        init {
            addSerializer(TinkerCat::class.java, TinkerCatJacksonSerializer())
            addDeserializer(TinkerCat::class.java, TinkerCatJacksonDeserializer())
        }
    }

    /**
     * Serializes the graph into an edge list format.  Edge list is a better choices than adjacency list (which is
     * typically standard from the [GraphReader] and [GraphWriter] perspective) in this case because
     * the use case for this isn't around massive graphs.  The use case is for "small" subgraphs that are being
     * shipped over the wire from Gremlin Server. Edge list format is a bit easier for non-JVM languages to work
     * with as a format and doesn't require a cache for loading (as vertex labels are not serialized in adjacency
     * list).
     */
    internal class TinkerCatJacksonSerializer : StdSerializer<TinkerCat?>(TinkerCat::class.java) {
        @Throws(IOException::class)
        fun serialize(graph: TinkerCat, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider) {
            jsonGenerator.writeStartObject()
            jsonGenerator.writeFieldName(GraphSONTokens.VERTICES)
            jsonGenerator.writeStartArray()
            val vertices = graph.vertices()
            while (vertices.hasNext()) {
                serializerProvider.defaultSerializeValue(vertices.next(), jsonGenerator)
            }
            jsonGenerator.writeEndArray()
            jsonGenerator.writeFieldName(GraphSONTokens.EDGES)
            jsonGenerator.writeStartArray()
            val edges = graph.edges()
            while (edges.hasNext()) {
                serializerProvider.defaultSerializeValue(edges.next(), jsonGenerator)
            }
            jsonGenerator.writeEndArray()
            jsonGenerator.writeEndObject()
        }

        @Throws(IOException::class)
        fun serializeWithType(
            graph: TinkerCat, jsonGenerator: JsonGenerator,
            serializerProvider: SerializerProvider?, typeSerializer: TypeSerializer?
        ) {
            jsonGenerator.writeStartObject()
            jsonGenerator.writeStringField(GraphSONTokens.CLASS, TinkerCat::class.java.name)
            jsonGenerator.writeFieldName(GraphSONTokens.VERTICES)
            jsonGenerator.writeStartArray()
            jsonGenerator.writeString(ArrayList::class.java.name)
            jsonGenerator.writeStartArray()
            val vertices = graph.vertices()
            while (vertices.hasNext()) {
                GraphSONUtil.writeWithType(vertices.next(), jsonGenerator, serializerProvider, typeSerializer)
            }
            jsonGenerator.writeEndArray()
            jsonGenerator.writeEndArray()
            jsonGenerator.writeFieldName(GraphSONTokens.EDGES)
            jsonGenerator.writeStartArray()
            jsonGenerator.writeString(ArrayList::class.java.name)
            jsonGenerator.writeStartArray()
            val edges = graph.edges()
            while (edges.hasNext()) {
                GraphSONUtil.writeWithType(edges.next(), jsonGenerator, serializerProvider, typeSerializer)
            }
            jsonGenerator.writeEndArray()
            jsonGenerator.writeEndArray()
            jsonGenerator.writeEndObject()
        }
    }

    /**
     * Deserializes the edge list format.
     */
    internal class TinkerCatJacksonDeserializer : StdDeserializer<TinkerCat?>(TinkerCat::class.java) {
        @Throws(IOException::class, JsonProcessingException::class)
        fun deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext): TinkerCat {
            val conf: Configuration = BaseConfiguration()
            conf.setProperty("gremlin.tinkercat.defaultVertexPropertyCardinality", "list")
            val graph = TinkerCat.open(conf)
            val edges: List<Map<String, Any>>?
            val vertices: List<Map<String, Any>>?
            if (!jsonParser.getCurrentToken().isStructStart()) {
                if (!jsonParser.getCurrentName()
                        .equals(GraphSONTokens.VERTICES)
                ) throw IOException(String.format("Expected a '%s' key", GraphSONTokens.VERTICES))
                jsonParser.nextToken()
                vertices = deserializationContext.readValue(jsonParser, ArrayList::class.java)
                jsonParser.nextToken()
                if (!jsonParser.getCurrentName()
                        .equals(GraphSONTokens.EDGES)
                ) throw IOException(String.format("Expected a '%s' key", GraphSONTokens.EDGES))
                jsonParser.nextToken()
                edges = deserializationContext.readValue(jsonParser, ArrayList::class.java)
            } else {
                val graphData: Map<String, Any> = deserializationContext.readValue(jsonParser, HashMap::class.java)
                vertices = graphData[GraphSONTokens.VERTICES] as List<Map<String, Any>>?
                edges = graphData[GraphSONTokens.EDGES] as List<Map<String, Any>>?
            }
            for (vertexData in vertices!!) {
                val detached = DetachedVertex(
                    vertexData[GraphSONTokens.ID],
                    vertexData[GraphSONTokens.LABEL].toString(),
                    vertexData[GraphSONTokens.PROPERTIES] as Map<String?, Any?>?
                )
                detached.attach(Attachable.Method.getOrCreate<Vertex>(graph))
            }
            for (edgeData in edges!!) {
                val detached = DetachedEdge(
                    edgeData[GraphSONTokens.ID],
                    edgeData[GraphSONTokens.LABEL].toString(),
                    edgeData[GraphSONTokens.PROPERTIES] as Map<String?, Any?>?,
                    edgeData[GraphSONTokens.OUT],
                    edgeData[GraphSONTokens.OUT_LABEL].toString(),
                    edgeData[GraphSONTokens.IN],
                    edgeData[GraphSONTokens.IN_LABEL].toString()
                )
                detached.attach(Attachable.Method.getOrCreate(graph))
            }
            return graph
        }
    }

    companion object {
        private val INSTANCE = TinkerIoRegistryV1d0()
        fun instance(): TinkerIoRegistryV1d0 {
            return INSTANCE
        }
    }
}