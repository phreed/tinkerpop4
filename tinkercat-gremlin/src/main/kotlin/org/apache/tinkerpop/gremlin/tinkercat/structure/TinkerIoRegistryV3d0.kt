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
import org.apache.tinkerpop.gremlin.structure.io.Mapper
import org.apache.tinkerpop.gremlin.structure.util.Attachable
import org.apache.tinkerpop.shaded.jackson.core.JsonGenerator
import java.io.ByteArrayOutputStream
import java.lang.Exception
import java.util.HashMap

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
class TinkerIoRegistryV3d0 private constructor() : AbstractIoRegistry() {
    init {
        register(GryoIo::class.java, TinkerCat::class.java, TinkerCatGryoSerializer())
        register(GraphSONIo::class.java, null, TinkerModuleV2d0())
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
     * Provides a method to serialize an entire [TinkerCat] into itself for GraphSON. This is useful when
     * shipping small graphs around through Gremlin Server.
     */
    internal class TinkerModuleV2d0 : TinkerPopJacksonModule("tinkercat-2.0") {
        init {
            addSerializer(TinkerCat::class.java, TinkerCatJacksonSerializer())
            addDeserializer(TinkerCat::class.java, TinkerCatJacksonDeserializer())
        }

        val typeDefinitions: Map<Class<*>, String>
            get() = object : HashMap<Class<*>?, String?>() {
                init {
                    put(TinkerCat::class.java, "graph")
                }
            }
        val typeNamespace: String
            get() = "tinker"
    }

    /**
     * Serializes the graph into an edge list format.  Edge list is a better choices than adjacency list (which is
     * typically standard from the [GraphReader] and [GraphWriter] perspective) in this case because
     * the use case for this isn't around massive graphs.  The use case is for "small" subgraphs that are being
     * shipped over the wire from Gremlin Server. Edge list format is a bit easier for non-JVM languages to work
     * with as a format and doesn't require a cache for loading (as vertex labels are not serialized in adjacency
     * list).
     */
    internal class TinkerCatJacksonSerializer : StdScalarSerializer<TinkerCat?>(TinkerCat::class.java) {
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
            while (jsonParser.nextToken() !== JsonToken.END_OBJECT) {
                if (jsonParser.getCurrentName().equals("vertices")) {
                    while (jsonParser.nextToken() !== JsonToken.END_ARRAY) {
                        if (jsonParser.currentToken() === JsonToken.START_OBJECT) {
                            val v: DetachedVertex =
                                deserializationContext.readValue(jsonParser, Vertex::class.java) as DetachedVertex
                            v.attach(Attachable.Method.getOrCreate<Vertex>(graph))
                        }
                    }
                } else if (jsonParser.getCurrentName().equals("edges")) {
                    while (jsonParser.nextToken() !== JsonToken.END_ARRAY) {
                        if (jsonParser.currentToken() === JsonToken.START_OBJECT) {
                            val e: DetachedEdge =
                                deserializationContext.readValue(jsonParser, Edge::class.java) as DetachedEdge
                            e.attach(Attachable.Method.getOrCreate<Edge>(graph))
                        }
                    }
                }
            }
            return graph
        }
    }

    companion object {
        private val INSTANCE = TinkerIoRegistryV3d0()
        @JvmStatic
        fun instance(): TinkerIoRegistryV3d0 {
            return INSTANCE
        }
    }
}