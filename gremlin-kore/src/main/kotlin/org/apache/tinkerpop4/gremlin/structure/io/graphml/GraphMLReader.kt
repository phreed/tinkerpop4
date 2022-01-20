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
package org.apache.tinkerpop4.gremlin.structure.io.graphml

import org.apache.tinkerpop4.gremlin.structure.Direction

/**
 * `GraphMLReader` writes the data from a GraphML stream to a graph.  Note that this format is lossy, in the
 * sense that data types and features of Gremlin Structure not supported by GraphML are not serialized.  This format
 * is meant for external export of a graph to tools outside of Gremlin Structure graphs.  Note that GraphML does not
 * support the notion of multi-properties or properties on properties.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Alex Averbuch (alex.averbuch@gmail.com)
 * @author Joshua Shinavier (http://fortytwo.net)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class GraphMLReader private constructor(builder: Builder) : GraphReader {
    private val edgeLabelKey: String
    private val vertexLabelKey: String
    private val batchSize: Long
    private val strict: Boolean
    private val inputFactory: XMLInputFactory?

    init {
        edgeLabelKey = builder.edgeLabelKey
        batchSize = builder.batchSize
        vertexLabelKey = builder.vertexLabelKey
        strict = builder.strict
        inputFactory = builder.inputFactory
    }

    @Override
    @Throws(IOException::class)
    fun readGraph(graphInputStream: InputStream?, graphToWriteTo: Graph) {
        val cache: Map<Object?, Vertex> = HashMap()
        val counter = AtomicLong(0)
        val supportsTx: Boolean = graphToWriteTo.features().graph().supportsTransactions()
        val edgeFeatures: EdgeFeatures = graphToWriteTo.features().edge()
        val vertexFeatures: VertexFeatures = graphToWriteTo.features().vertex()
        try {
            val reader: XMLStreamReader = inputFactory.createXMLStreamReader(graphInputStream)
            val keyIdMap: Map<String, String> = HashMap()
            val keyTypesMaps: Map<String, String> = HashMap()

            // Buffered Vertex Data
            var vertexId: String? = null
            var vertexLabel: String? = null
            var vertexProps: Map<String?, Object?>? = null
            var isInVertex = false

            // Buffered Edge Data
            var edgeId: String? = null
            var edgeLabel: String? = null
            var edgeInVertex: Vertex? = null
            var edgeOutVertex: Vertex? = null
            var edgeProps: Map<String?, Object?>? = null
            var isInEdge = false
            while (reader.hasNext()) {
                val eventType: Integer = reader.next()
                if (eventType.equals(XMLEvent.START_ELEMENT)) {
                    val elementName: String = reader.getName().getLocalPart()
                    when (elementName) {
                        GraphMLTokens.KEY -> {
                            val id: String = reader.getAttributeValue(null, GraphMLTokens.ID)
                            val attributeName: String = reader.getAttributeValue(null, GraphMLTokens.ATTR_NAME)
                            val attributeType: String = reader.getAttributeValue(null, GraphMLTokens.ATTR_TYPE)
                            keyIdMap.put(id, attributeName)
                            keyTypesMaps.put(id, attributeType)
                        }
                        GraphMLTokens.NODE -> {
                            vertexId = reader.getAttributeValue(null, GraphMLTokens.ID)
                            isInVertex = true
                            vertexProps = HashMap()
                        }
                        GraphMLTokens.EDGE -> {
                            edgeId = reader.getAttributeValue(null, GraphMLTokens.ID)
                            val vertexIdOut: String = reader.getAttributeValue(null, GraphMLTokens.SOURCE)
                            val vertexIdIn: String = reader.getAttributeValue(null, GraphMLTokens.TARGET)

                            // graphml allows edges and vertices to be mixed in terms of how they are positioned
                            // in the xml therefore it is possible that an edge is created prior to its definition
                            // as a vertex.
                            edgeOutVertex = findOrCreate(vertexIdOut, graphToWriteTo, vertexFeatures, cache, false)
                            edgeInVertex = findOrCreate(vertexIdIn, graphToWriteTo, vertexFeatures, cache, false)
                            if (supportsTx && counter.incrementAndGet() % batchSize === 0) graphToWriteTo.tx().commit()
                            isInEdge = true
                            edgeProps = HashMap()
                        }
                        GraphMLTokens.DATA -> {
                            val key: String = reader.getAttributeValue(null, GraphMLTokens.KEY)
                            val dataAttributeName = keyIdMap[key]
                            if (dataAttributeName != null) {
                                val value: String = reader.getElementText()
                                if (isInVertex) {
                                    if (key.equals(vertexLabelKey)) vertexLabel = value else {
                                        try {
                                            vertexProps.put(dataAttributeName, typeCastValue(key, value, keyTypesMaps))
                                        } catch (nfe: NumberFormatException) {
                                            if (strict) throw nfe
                                        }
                                    }
                                } else if (isInEdge) {
                                    if (key.equals(edgeLabelKey)) edgeLabel = value else {
                                        try {
                                            edgeProps.put(dataAttributeName, typeCastValue(key, value, keyTypesMaps))
                                        } catch (nfe: NumberFormatException) {
                                            if (strict) throw nfe
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (eventType.equals(XMLEvent.END_ELEMENT)) {
                    val elementName: String = reader.getName().getLocalPart()
                    if (elementName.equals(GraphMLTokens.NODE)) {
                        val currentVertexId = vertexId
                        val currentVertexLabel: String = Optional.ofNullable(vertexLabel).orElse(Vertex.DEFAULT_LABEL)
                        val propsAsArray: Array<Object> =
                            vertexProps.entrySet().stream().flatMap { e -> Stream.of(e.getKey(), e.getValue()) }
                                .toArray()
                        findOrCreate(
                            currentVertexId, graphToWriteTo, vertexFeatures, cache,
                            true, ElementHelper.upsert(propsAsArray, T.label, currentVertexLabel)
                        )
                        if (supportsTx && counter.incrementAndGet() % batchSize === 0) graphToWriteTo.tx().commit()
                        vertexId = null
                        vertexLabel = null
                        vertexProps = null
                        isInVertex = false
                    } else if (elementName.equals(GraphMLTokens.EDGE)) {
                        val propsAsArray: Array<Object> =
                            edgeProps.entrySet().stream().flatMap { e -> Stream.of(e.getKey(), e.getValue()) }
                                .toArray()
                        val propsReady: Array<Object> =
                            if (null != edgeId && edgeFeatures.willAllowId(edgeId)) ElementHelper.upsert(
                                propsAsArray,
                                T.id,
                                edgeId
                            ) else propsAsArray
                        edgeOutVertex.addEdge(edgeLabel ?: Edge.DEFAULT_LABEL, edgeInVertex, propsReady)
                        if (supportsTx && counter.incrementAndGet() % batchSize === 0) graphToWriteTo.tx().commit()
                        edgeId = null
                        edgeLabel = null
                        edgeOutVertex = null
                        edgeInVertex = null
                        edgeProps = null
                        isInEdge = false
                    }
                }
            }
            if (supportsTx) graphToWriteTo.tx().commit()
        } catch (xse: XMLStreamException) {
            // rollback whatever portion failed
            if (supportsTx && counter.incrementAndGet() % batchSize === 0) graphToWriteTo.tx().rollback()
            throw IOException(xse)
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

    /**
     * Allows configuration and construction of the GraphMLReader instance.
     */
    class Builder private constructor() : ReaderBuilder<GraphMLReader?> {
        private var edgeLabelKey: String = GraphMLTokens.LABEL_E
        private var vertexLabelKey: String = GraphMLTokens.LABEL_V
        private var strict = true
        private var batchSize: Long = 10000
        private var inputFactory: XMLInputFactory? = null

        /**
         * When set to true, exceptions will be thrown if a property value cannot be coerced to the expected data
         * type. If set to false, then the reader will continue with the import but ignore the failed property key.
         * By default this value is "true".
         */
        fun strict(strict: Boolean): Builder {
            this.strict = strict
            return this
        }

        /**
         * The key to use as the edge label.
         */
        fun edgeLabelKey(edgeLabelKey: String): Builder {
            this.edgeLabelKey = edgeLabelKey
            return this
        }

        /**
         * the key to use as the vertex label.
         */
        fun vertexLabelKey(vertexLabelKey: String): Builder {
            this.vertexLabelKey = vertexLabelKey
            return this
        }

        /**
         * Number of mutations to perform before a commit is executed.
         */
        fun batchSize(batchSize: Long): Builder {
            this.batchSize = batchSize
            return this
        }

        /**
         * A custom `XMLInputFactory`. If this value is not set then a default one is constructed. The default
         * will be configured to disable DTDs and support of external entities to prevent
         * [XXE](https://cheatsheetseries.owasp.org/cheatsheets/XML_External_Entity_Prevention_Cheat_Sheet.html#xmlinputfactory-a-stax-parser)
         * style attacks.
         */
        fun xmlInputFactory(inputFactory: XMLInputFactory?): Builder {
            this.inputFactory = inputFactory
            return this
        }

        fun create(): GraphMLReader {
            if (inputFactory == null) {
                inputFactory = XMLInputFactory.newInstance()

                // prevent XXE
                // https://owasp.org/www-community/vulnerabilities/XML_External_Entity_(XXE)_Processing
                inputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false)
                inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false)
            }
            return GraphMLReader(this)
        }
    }

    companion object {
        private fun findOrCreate(
            id: Object?, graphToWriteTo: Graph,
            features: VertexFeatures,
            cache: Map<Object?, Vertex>, asVertex: Boolean, vararg args: Object
        ): Vertex? {
            return if (cache.containsKey(id)) {
                // if the request to findOrCreate come from a vertex then AND the vertex was already created, that means
                // that the vertex was created by an edge that arrived first in the stream (allowable via GraphML
                // specification).  as the edge only carries the vertex id and not its properties, the properties
                // of the vertex need to be attached at this point.
                if (asVertex) {
                    val v: Vertex? = cache[id]
                    ElementHelper.attachProperties(v, args)
                    v
                } else {
                    cache[id]
                }
            } else {
                val argsReady: Array<Object> =
                    if (features.willAllowId(id)) ElementHelper.upsert(args, T.id, id) else args
                val v: Vertex = graphToWriteTo.addVertex(argsReady)
                cache.put(id, v)
                v
            }
        }

        private fun typeCastValue(key: String, value: String, keyTypes: Map<String, String>): Object {
            val type = keyTypes[key]
            return if (null == type || type.equals(GraphMLTokens.STRING)) value else if (type.equals(GraphMLTokens.FLOAT)) Float.valueOf(
                value
            ) else if (type.equals(GraphMLTokens.INT)) Integer.valueOf(value) else if (type.equals(GraphMLTokens.DOUBLE)) Double.valueOf(
                value
            ) else if (type.equals(GraphMLTokens.BOOLEAN)) Boolean.valueOf(value) else if (type.equals(GraphMLTokens.LONG)) Long.valueOf(
                value
            ) else value
        }

        fun build(): Builder {
            return Builder()
        }
    }
}