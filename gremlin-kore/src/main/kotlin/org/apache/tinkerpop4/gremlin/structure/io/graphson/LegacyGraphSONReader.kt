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
package org.apache.tinkerpop4.gremlin.structure.io.graphson

import org.apache.tinkerpop4.gremlin.structure.Direction

/**
 * A @{link GraphReader} that constructs a graph from a JSON-based representation of a graph and its elements given
 * the "legacy" Blueprints 2.x version of GraphSON.  This implementation is specifically for aiding in migration
 * of graphs from TinkerPop 2.x to TinkerPop 3.x. This reader only reads GraphSON from TinkerPop 2.x that was
 * generated in `GraphSONMode.EXTENDED`.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class LegacyGraphSONReader private constructor(mapper: ObjectMapper, batchSize: Long) : GraphReader {
    private val mapper: ObjectMapper
    private val batchSize: Long

    init {
        this.mapper = mapper
        this.batchSize = batchSize
    }

    @Override
    @Throws(IOException::class)
    fun readGraph(inputStream: InputStream?, graphToWriteTo: Graph) {
        val cache: Map<Object?, Vertex> = HashMap()
        val counter = AtomicLong(0)
        val supportsTx: Boolean = graphToWriteTo.features().graph().supportsTransactions()
        val edgeFeatures: EdgeFeatures = graphToWriteTo.features().edge()
        val vertexFeatures: VertexFeatures = graphToWriteTo.features().vertex()
        val factory: JsonFactory = mapper.getFactory()
        val graphson = LegacyGraphSONUtility(graphToWriteTo, vertexFeatures, edgeFeatures, cache)
        try {
            factory.createParser(inputStream).use { parser ->
                if (parser.nextToken() !== JsonToken.START_OBJECT) throw IOException("Expected data to start with an Object")
                while (parser.nextToken() !== JsonToken.END_OBJECT) {
                    val fieldName = if (parser.getCurrentName() == null) "" else parser.getCurrentName()
                    when (fieldName) {
                        GraphSONTokensTP2.MODE -> {
                            parser.nextToken()
                            val mode: String = parser.getText()
                            if (!mode.equals("EXTENDED")) throw IllegalStateException("The legacy GraphSON must be generated with GraphSONMode.EXTENDED")
                        }
                        GraphSONTokensTP2.VERTICES -> {
                            parser.nextToken()
                            while (parser.nextToken() !== JsonToken.END_ARRAY) {
                                val node: JsonNode = parser.readValueAsTree()
                                graphson.vertexFromJson(node)
                                if (supportsTx && counter.incrementAndGet() % batchSize === 0) graphToWriteTo.tx()
                                    .commit()
                            }
                        }
                        GraphSONTokensTP2.EDGES -> {
                            parser.nextToken()
                            while (parser.nextToken() !== JsonToken.END_ARRAY) {
                                val node: JsonNode = parser.readValueAsTree()
                                val inV: Vertex? =
                                    cache[LegacyGraphSONUtility.getTypedValueFromJsonNode(node.get(GraphSONTokensTP2._IN_V))]
                                val outV: Vertex? =
                                    cache[LegacyGraphSONUtility.getTypedValueFromJsonNode(node.get(GraphSONTokensTP2._OUT_V))]
                                graphson.edgeFromJson(node, outV, inV)
                                if (supportsTx && counter.incrementAndGet() % batchSize === 0) graphToWriteTo.tx()
                                    .commit()
                            }
                        }
                        else -> throw IllegalStateException(
                            String.format(
                                "Unexpected token in GraphSON - %s",
                                fieldName
                            )
                        )
                    }
                }
                if (supportsTx) graphToWriteTo.tx().commit()
            }
        } catch (ex: Exception) {
            throw IOException(ex)
        }
    }

    /**
     * This method is not supported for this reader.
     *
     * @throws UnsupportedOperationException when called.
     */
    @Override
    @Throws(IOException::class)
    fun readVertices(
        inputStream: InputStream?,
        vertexAttachMethod: Function<Attachable<Vertex?>?, Vertex?>?,
        edgeAttachMethod: Function<Attachable<Edge?>?, Edge?>?,
        attachEdgesOfThisDirection: Direction?
    ): Iterator<Vertex> {
        throw Io.Exceptions.readerFormatIsForFullGraphSerializationOnly(this.getClass())
    }

    /**
     * This method is not supported for this reader.
     *
     * @throws UnsupportedOperationException when called.
     */
    @Override
    @Throws(IOException::class)
    fun readEdge(inputStream: InputStream?, edgeAttachMethod: Function<Attachable<Edge?>?, Edge?>?): Edge {
        throw Io.Exceptions.readerFormatIsForFullGraphSerializationOnly(this.getClass())
    }

    /**
     * This method is not supported for this reader.
     *
     * @throws UnsupportedOperationException when called.
     */
    @Override
    @Throws(IOException::class)
    fun readVertex(inputStream: InputStream?, vertexAttachMethod: Function<Attachable<Vertex?>?, Vertex?>?): Vertex {
        throw Io.Exceptions.readerFormatIsForFullGraphSerializationOnly(this.getClass())
    }

    /**
     * This method is not supported for this reader.
     *
     * @throws UnsupportedOperationException when called.
     */
    @Override
    @Throws(IOException::class)
    fun readVertex(
        inputStream: InputStream?, vertexAttachMethod: Function<Attachable<Vertex?>?, Vertex?>?,
        edgeAttachMethod: Function<Attachable<Edge?>?, Edge?>?,
        attachEdgesOfThisDirection: Direction?
    ): Vertex {
        throw Io.Exceptions.readerFormatIsForFullGraphSerializationOnly(this.getClass())
    }

    /**
     * This method is not supported for this reader.
     *
     * @throws UnsupportedOperationException when called.
     */
    @Override
    @Throws(IOException::class)
    fun readVertexProperty(
        inputStream: InputStream?,
        vertexPropertyAttachMethod: Function<Attachable<VertexProperty?>?, VertexProperty?>?
    ): VertexProperty {
        throw Io.Exceptions.readerFormatIsForFullGraphSerializationOnly(this.getClass())
    }

    /**
     * This method is not supported for this reader.
     *
     * @throws UnsupportedOperationException when called.
     */
    @Override
    @Throws(IOException::class)
    fun readProperty(
        inputStream: InputStream?,
        propertyAttachMethod: Function<Attachable<Property?>?, Property?>?
    ): Property {
        throw Io.Exceptions.readerFormatIsForFullGraphSerializationOnly(this.getClass())
    }

    /**
     * This method is not supported for this reader.
     *
     * @throws UnsupportedOperationException when called.
     */
    @Override
    @Throws(IOException::class)
    fun <C> readObject(inputStream: InputStream?, clazz: Class<out C>?): C {
        throw Io.Exceptions.readerFormatIsForFullGraphSerializationOnly(this.getClass())
    }

    class Builder private constructor() {
        private var loadCustomModules = false
        private val customModules: List<SimpleModule> = ArrayList()
        private var batchSize: Long = 10000

        /**
         * Supply a mapper module for serialization/deserialization.
         */
        fun addCustomModule(custom: SimpleModule?): Builder {
            customModules.add(custom)
            return this
        }

        /**
         * Try to load `SimpleModule` instances from the current classpath.  These are loaded in addition to
         * the one supplied to the [.addCustomModule];
         */
        fun loadCustomModules(loadCustomModules: Boolean): Builder {
            this.loadCustomModules = loadCustomModules
            return this
        }

        /**
         * Number of mutations to perform before a commit is executed.
         */
        fun batchSize(batchSize: Long): Builder {
            this.batchSize = batchSize
            return this
        }

        fun create(): LegacyGraphSONReader {
            // not sure why there is specific need for V2 here with "no types" as we don't even need TP3 GraphSON
            // at all. Seems like a standard Jackson ObjectMapper would have worked fine, but rather than change
            // this ancient class at this stage we'll just stick to what is there.
            val builder: GraphSONMapper.Builder = GraphSONMapper.build().version(GraphSONVersion.V2_0)
            customModules.forEach(builder::addCustomModule)
            val mapper: GraphSONMapper = builder.typeInfo(TypeInfo.NO_TYPES)
                .loadCustomModules(loadCustomModules).create()
            return LegacyGraphSONReader(mapper.createMapper(), batchSize)
        }
    }

    internal class LegacyGraphSONUtility(
        g: Graph, vertexFeatures: VertexFeatures,
        edgeFeatures: EdgeFeatures,
        cache: Map<Object?, Vertex>
    ) {
        private val g: Graph
        private val vertexFeatures: VertexFeatures
        private val edgeFeatures: EdgeFeatures
        private val cache: Map<Object?, Vertex>

        init {
            this.g = g
            this.vertexFeatures = vertexFeatures
            this.edgeFeatures = edgeFeatures
            this.cache = cache
        }

        @Throws(IOException::class)
        fun vertexFromJson(json: JsonNode): Vertex {
            val props: Map<String, Object> = readProperties(json)
            val vertexId: Object? = getTypedValueFromJsonNode(json.get(GraphSONTokensTP2._ID))
            val v: Vertex = if (vertexFeatures.willAllowId(vertexId)) g.addVertex(T.id, vertexId) else g.addVertex()
            cache.put(vertexId, v)
            for (entry in props.entrySet()) {
                v.property(g.features().vertex().getCardinality(entry.getKey()), entry.getKey(), entry.getValue())
            }
            return v
        }

        @Throws(IOException::class)
        fun edgeFromJson(json: JsonNode, out: Vertex?, `in`: Vertex?): Edge {
            val props: Map<String, Object> = readProperties(json)
            val edgeId: Object? = getTypedValueFromJsonNode(json.get(GraphSONTokensTP2._ID))
            val nodeLabel: JsonNode = json.get(GraphSONTokensTP2._LABEL)
            val label = if (nodeLabel == null) EMPTY_STRING else nodeLabel.textValue()
            val e: Edge = if (edgeFeatures.willAllowId(edgeId)) out.addEdge(label, `in`, T.id, edgeId) else out.addEdge(
                label,
                `in`
            )
            for (entry in props.entrySet()) {
                e.property(entry.getKey(), entry.getValue())
            }
            return e
        }

        companion object {
            private const val EMPTY_STRING = ""
            fun readProperties(node: JsonNode): Map<String, Object> {
                val map: Map<String, Object> = HashMap()
                val iterator: Iterator<Map.Entry<String, JsonNode>> = node.fields()
                while (iterator.hasNext()) {
                    val entry: Map.Entry<String, JsonNode> = iterator.next()
                    if (!isReservedKey(entry.getKey())) {
                        // it generally shouldn't be as such but graphson containing null values can't be shoved into
                        // element property keys or it will result in error
                        val o: Object? = readProperty(entry.getValue())
                        if (o != null) {
                            map.put(entry.getKey(), o)
                        }
                    }
                }
                return map
            }

            private fun isReservedKey(key: String): Boolean {
                return (key.equals(GraphSONTokensTP2._ID) || key.equals(GraphSONTokensTP2._TYPE) || key.equals(
                    GraphSONTokensTP2._LABEL
                )
                        || key.equals(GraphSONTokensTP2._OUT_V) || key.equals(GraphSONTokensTP2._IN_V))
            }

            private fun readProperty(node: JsonNode): Object? {
                val propertyValue: Object?
                if (node.get(GraphSONTokensTP2.TYPE).textValue().equals(GraphSONTokensTP2.TYPE_UNKNOWN)) {
                    propertyValue = null
                } else if (node.get(GraphSONTokensTP2.TYPE).textValue().equals(GraphSONTokensTP2.TYPE_BOOLEAN)) {
                    propertyValue = node.get(GraphSONTokensTP2.VALUE).booleanValue()
                } else if (node.get(GraphSONTokensTP2.TYPE).textValue().equals(GraphSONTokensTP2.TYPE_FLOAT)) {
                    propertyValue = Float.parseFloat(node.get(GraphSONTokensTP2.VALUE).asText())
                } else if (node.get(GraphSONTokensTP2.TYPE).textValue().equals(GraphSONTokensTP2.TYPE_BYTE)) {
                    propertyValue = Byte.parseByte(node.get(GraphSONTokensTP2.VALUE).asText())
                } else if (node.get(GraphSONTokensTP2.TYPE).textValue().equals(GraphSONTokensTP2.TYPE_SHORT)) {
                    propertyValue = Short.parseShort(node.get(GraphSONTokensTP2.VALUE).asText())
                } else if (node.get(GraphSONTokensTP2.TYPE).textValue().equals(GraphSONTokensTP2.TYPE_DOUBLE)) {
                    propertyValue = node.get(GraphSONTokensTP2.VALUE).doubleValue()
                } else if (node.get(GraphSONTokensTP2.TYPE).textValue().equals(GraphSONTokensTP2.TYPE_INTEGER)) {
                    propertyValue = node.get(GraphSONTokensTP2.VALUE).intValue()
                } else if (node.get(GraphSONTokensTP2.TYPE).textValue().equals(GraphSONTokensTP2.TYPE_LONG)) {
                    propertyValue = node.get(GraphSONTokensTP2.VALUE).longValue()
                } else if (node.get(GraphSONTokensTP2.TYPE).textValue().equals(GraphSONTokensTP2.TYPE_STRING)) {
                    propertyValue = node.get(GraphSONTokensTP2.VALUE).textValue()
                } else if (node.get(GraphSONTokensTP2.TYPE).textValue().equals(GraphSONTokensTP2.TYPE_LIST)) {
                    propertyValue = readProperties(node.get(GraphSONTokensTP2.VALUE).elements())
                } else if (node.get(GraphSONTokensTP2.TYPE).textValue().equals(GraphSONTokensTP2.TYPE_MAP)) {
                    propertyValue = readProperties(node.get(GraphSONTokensTP2.VALUE))
                } else {
                    propertyValue = node.textValue()
                }
                return propertyValue
            }

            private fun readProperties(listOfNodes: Iterator<JsonNode>): List {
                val array: List<Object> = ArrayList()
                while (listOfNodes.hasNext()) {
                    array.add(readProperty(listOfNodes.next()))
                }
                return array
            }

            fun getTypedValueFromJsonNode(node: JsonNode?): Object? {
                var theValue: Object? = null
                if (node != null && !node.isNull()) {
                    theValue = if (node.isBoolean()) {
                        node.booleanValue()
                    } else if (node.isDouble()) {
                        node.doubleValue()
                    } else if (node.isFloatingPointNumber()) {
                        node.floatValue()
                    } else if (node.isInt()) {
                        node.intValue()
                    } else if (node.isLong()) {
                        node.longValue()
                    } else if (node.isTextual()) {
                        node.textValue()
                    } else if (node.isArray()) {
                        // this is an array so just send it back so that it can be
                        // reprocessed to its primitive components
                        node
                    } else if (node.isObject()) {
                        // this is an object so just send it back so that it can be
                        // reprocessed to its primitive components
                        node
                    } else {
                        node.textValue()
                    }
                }
                return theValue
            }
        }
    }

    object GraphSONTokensTP2 {
        const val _ID = "_id"
        const val _LABEL = "_label"
        const val _TYPE = "_type"
        const val _OUT_V = "_outV"
        const val _IN_V = "_inV"
        const val VALUE = "value"
        const val TYPE = "type"
        const val TYPE_LIST = "list"
        const val TYPE_STRING = "string"
        const val TYPE_DOUBLE = "double"
        const val TYPE_INTEGER = "integer"
        const val TYPE_FLOAT = "float"
        const val TYPE_MAP = "map"
        const val TYPE_BOOLEAN = "boolean"
        const val TYPE_LONG = "long"
        const val TYPE_SHORT = "short"
        const val TYPE_BYTE = "byte"
        const val TYPE_UNKNOWN = "unknown"
        const val VERTICES = "vertices"
        const val EDGES = "edges"
        const val MODE = "mode"
    }

    companion object {
        fun build(): Builder {
            return Builder()
        }
    }
}