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

import java.io.IOException

/**
 * GraphMLWriter writes a Graph to a GraphML OutputStream. Note that this format is lossy, in the sense that data
 * types and features of Gremlin Structure not supported by GraphML are not serialized.  This format is meant for
 * external export of a graph to tools outside of Gremlin Structure graphs.  Note that GraphML does not support
 * the notion of multi-properties or properties on properties and will throw an exception when writing a
 * graph elements that have such things.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Joshua Shinavier (http://fortytwo.net)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class GraphMLWriter private constructor(
    normalize: Boolean, vertexKeyTypes: Map<String, String>?,
    edgeKeyTypes: Map<String, String>?, xmlSchemaLocation: String?,
    edgeLabelKey: String, vertexLabelKey: String
) : GraphWriter {
    private val inputFactory: XMLOutputFactory = XMLOutputFactory.newInstance()
    private val normalize = false
    private val vertexKeyTypes: Optional<Map<String, String>>
    private val edgeKeyTypes: Optional<Map<String, String>>
    private val xmlSchemaLocation: Optional<String>
    private val edgeLabelKey: String
    private val vertexLabelKey: String
    private var intersection: Collection<String>? = Collections.emptySet()

    init {
        this.normalize = normalize
        this.vertexKeyTypes = Optional.ofNullable(vertexKeyTypes)
        this.edgeKeyTypes = Optional.ofNullable(edgeKeyTypes)
        this.xmlSchemaLocation = Optional.ofNullable(xmlSchemaLocation)
        this.edgeLabelKey = edgeLabelKey
        this.vertexLabelKey = vertexLabelKey
    }

    /**
     * This method is not supported for this writer.
     *
     * @throws UnsupportedOperationException when called.
     */
    @Override
    @Throws(IOException::class)
    fun writeVertex(outputStream: OutputStream?, v: Vertex?, direction: Direction?) {
        throw Io.Exceptions.writerFormatIsForFullGraphSerializationOnly(this.getClass())
    }

    /**
     * This method is not supported for this writer.
     *
     * @throws UnsupportedOperationException when called.
     */
    @Override
    @Throws(IOException::class)
    fun writeVertex(outputStream: OutputStream?, v: Vertex?) {
        throw Io.Exceptions.writerFormatIsForFullGraphSerializationOnly(this.getClass())
    }

    /**
     * This method is not supported for this writer.
     *
     * @throws UnsupportedOperationException when called.
     */
    @Override
    @Throws(IOException::class)
    fun writeEdge(outputStream: OutputStream?, e: Edge?) {
        throw Io.Exceptions.writerFormatIsForFullGraphSerializationOnly(this.getClass())
    }

    /**
     * This method is not supported for this writer.
     *
     * @throws UnsupportedOperationException when called.
     */
    @Override
    @Throws(IOException::class)
    fun writeVertices(outputStream: OutputStream?, vertexIterator: Iterator<Vertex?>?, direction: Direction?) {
        throw Io.Exceptions.writerFormatIsForFullGraphSerializationOnly(this.getClass())
    }

    /**
     * This method is not supported for this writer.
     *
     * @throws UnsupportedOperationException when called.
     */
    @Override
    @Throws(IOException::class)
    fun writeVertices(outputStream: OutputStream?, vertexIterator: Iterator<Vertex?>?) {
        throw Io.Exceptions.writerFormatIsForFullGraphSerializationOnly(this.getClass())
    }

    /**
     * This method is not supported for this writer.
     *
     * @throws UnsupportedOperationException when called.
     */
    @Override
    @Throws(IOException::class)
    fun writeVertexProperty(outputStream: OutputStream?, vp: VertexProperty?) {
        throw Io.Exceptions.writerFormatIsForFullGraphSerializationOnly(this.getClass())
    }

    /**
     * This method is not supported for this writer.
     *
     * @throws UnsupportedOperationException when called.
     */
    @Override
    @Throws(IOException::class)
    fun writeProperty(outputStream: OutputStream?, p: Property?) {
        throw Io.Exceptions.writerFormatIsForFullGraphSerializationOnly(this.getClass())
    }

    /**
     * This method is not supported for this writer.
     *
     * @throws UnsupportedOperationException when called.
     */
    @Override
    @Throws(IOException::class)
    fun writeObject(outputStream: OutputStream?, `object`: Object?) {
        throw Io.Exceptions.writerFormatIsForFullGraphSerializationOnly(this.getClass())
    }

    /**
     * Write the data in a Graph to a GraphML OutputStream.
     *
     * @param outputStream the GraphML OutputStream to write the Graph data to
     * @throws IOException thrown if there is an error generating the GraphML data
     */
    @Override
    @Throws(IOException::class)
    fun writeGraph(outputStream: OutputStream, g: Graph) {
        val identifiedVertexKeyTypes: Map<String, String> = vertexKeyTypes.orElseGet { determineVertexTypes(g) }
        val identifiedEdgeKeyTypes: Map<String, String> = edgeKeyTypes.orElseGet { determineEdgeTypes(g) }
        if (identifiedEdgeKeyTypes.containsKey(edgeLabelKey)) throw IllegalStateException(
            String.format(
                "The edgeLabelKey value of[%s] conflicts with the name of an existing property key to be included in the GraphML",
                edgeLabelKey
            )
        )
        if (identifiedVertexKeyTypes.containsKey(vertexLabelKey)) throw IllegalStateException(
            String.format(
                "The vertexLabelKey value of[%s] conflicts with the name of an existing property key to be included in the GraphML",
                vertexLabelKey
            )
        )
        identifiedEdgeKeyTypes.put(edgeLabelKey, GraphMLTokens.STRING)
        identifiedVertexKeyTypes.put(vertexLabelKey, GraphMLTokens.STRING)
        try {
            val writer: XMLStreamWriter
            writer = configureWriter(outputStream)
            writer.writeStartDocument()
            writer.writeStartElement(GraphMLTokens.GRAPHML)
            writeXmlNsAndSchema(writer)
            writeTypes(identifiedVertexKeyTypes, identifiedEdgeKeyTypes, writer)
            writer.writeStartElement(GraphMLTokens.GRAPH)
            writer.writeAttribute(GraphMLTokens.ID, GraphMLTokens.G)
            writer.writeAttribute(GraphMLTokens.EDGEDEFAULT, GraphMLTokens.DIRECTED)
            writeVertices(writer, g)
            writeEdges(writer, g)
            writer.writeEndElement() // graph
            writer.writeEndElement() // graphml
            writer.writeEndDocument()
            writer.flush()
            writer.close()
        } catch (xse: XMLStreamException) {
            throw IOException(xse)
        }
    }

    @Throws(XMLStreamException::class)
    private fun configureWriter(outputStream: OutputStream): XMLStreamWriter {
        val utf8Writer: XMLStreamWriter = inputFactory.createXMLStreamWriter(outputStream, "UTF8")
        return if (normalize) {
            val writer: XMLStreamWriter = IndentingXMLStreamWriter(utf8Writer)
            (writer as GraphMLWriterHelper.IndentingXMLStreamWriter).setIndentStep("    ")
            writer
        } else utf8Writer
    }

    @Throws(XMLStreamException::class)
    private fun writeTypes(
        identifiedVertexKeyTypes: Map<String, String>,
        identifiedEdgeKeyTypes: Map<String, String>,
        writer: XMLStreamWriter
    ) {
        // <key id="weight" for="edge" attr.name="weight" attr.type="float"/>
        val vertexKeySet = getKeysAndNormalizeIfRequired(identifiedVertexKeyTypes)
        val edgeKeySet = getKeysAndNormalizeIfRequired(identifiedEdgeKeyTypes)
        // in case vertex and edge may have the same attribute name, the key id in graphml have to be different
        intersection = CollectionUtils.intersection(vertexKeySet, edgeKeySet)
        for (key in vertexKeySet) {
            writer.writeStartElement(GraphMLTokens.KEY)
            if (intersection!!.contains(key)) {
                writer.writeAttribute(GraphMLTokens.ID, key.concat(GraphMLTokens.VERTEX_SUFFIX))
            } else {
                writer.writeAttribute(GraphMLTokens.ID, key)
            }
            writer.writeAttribute(GraphMLTokens.FOR, GraphMLTokens.NODE)
            writer.writeAttribute(GraphMLTokens.ATTR_NAME, key)
            writer.writeAttribute(GraphMLTokens.ATTR_TYPE, identifiedVertexKeyTypes[key])
            writer.writeEndElement()
        }
        for (key in edgeKeySet) {
            writer.writeStartElement(GraphMLTokens.KEY)
            if (intersection!!.contains(key)) {
                writer.writeAttribute(GraphMLTokens.ID, key.concat(GraphMLTokens.EDGE_SUFFIX))
            } else {
                writer.writeAttribute(GraphMLTokens.ID, key)
            }
            writer.writeAttribute(GraphMLTokens.FOR, GraphMLTokens.EDGE)
            writer.writeAttribute(GraphMLTokens.ATTR_NAME, key)
            writer.writeAttribute(GraphMLTokens.ATTR_TYPE, identifiedEdgeKeyTypes[key])
            writer.writeEndElement()
        }
    }

    @Throws(XMLStreamException::class)
    private fun writeEdges(writer: XMLStreamWriter, graph: Graph) {
        val iterator: Iterator<Edge> = graph.edges()
        try {
            if (normalize) {
                val edges: List<Edge> = IteratorUtils.list(iterator)
                Collections.sort(edges, Comparators.ELEMENT_COMPARATOR)
                for (edge in edges) {
                    writer.writeStartElement(GraphMLTokens.EDGE)
                    writer.writeAttribute(GraphMLTokens.ID, edge.id().toString())
                    writer.writeAttribute(GraphMLTokens.SOURCE, edge.outVertex().id().toString())
                    writer.writeAttribute(GraphMLTokens.TARGET, edge.inVertex().id().toString())
                    writer.writeStartElement(GraphMLTokens.DATA)
                    writer.writeAttribute(GraphMLTokens.KEY, edgeLabelKey)
                    writer.writeCharacters(edge.label())
                    writer.writeEndElement()
                    val keys: List<String> = ArrayList(edge.keys())
                    Collections.sort(keys)
                    for (key in keys) {
                        writer.writeStartElement(GraphMLTokens.DATA)
                        if (intersection != null && intersection!!.contains(key)) {
                            writer.writeAttribute(GraphMLTokens.KEY, key + GraphMLTokens.EDGE_SUFFIX)
                        } else {
                            writer.writeAttribute(GraphMLTokens.KEY, key)
                        }
                        // technically there can't be a null here as gremlin structure forbids that occurrence even if Graph
                        // implementations support it, but out to empty string just in case.
                        writer.writeCharacters(edge.property(key).orElse("").toString())
                        writer.writeEndElement()
                    }
                    writer.writeEndElement()
                }
            } else {
                while (iterator.hasNext()) {
                    val edge: Edge = iterator.next()
                    writer.writeStartElement(GraphMLTokens.EDGE)
                    writer.writeAttribute(GraphMLTokens.ID, edge.id().toString())
                    writer.writeAttribute(GraphMLTokens.SOURCE, edge.outVertex().id().toString())
                    writer.writeAttribute(GraphMLTokens.TARGET, edge.inVertex().id().toString())
                    writer.writeStartElement(GraphMLTokens.DATA)
                    writer.writeAttribute(GraphMLTokens.KEY, edgeLabelKey)
                    writer.writeCharacters(edge.label())
                    writer.writeEndElement()
                    for (key in edge.keys()) {
                        writer.writeStartElement(GraphMLTokens.DATA)
                        if (intersection != null && intersection!!.contains(key)) {
                            writer.writeAttribute(GraphMLTokens.KEY, key + GraphMLTokens.EDGE_SUFFIX)
                        } else {
                            writer.writeAttribute(GraphMLTokens.KEY, key)
                        }
                        // technically there can't be a null here as gremlin structure forbids that occurrence even if Graph
                        // implementations support it, but out to empty string just in case.
                        writer.writeCharacters(edge.property(key).orElse("").toString())
                        writer.writeEndElement()
                    }
                    writer.writeEndElement()
                }
            }
        } finally {
            CloseableIterator.closeIterator(iterator)
        }
    }

    @Throws(XMLStreamException::class)
    private fun writeVertices(writer: XMLStreamWriter, graph: Graph) {
        val vertices: Iterable<Vertex> = getVerticesAndNormalizeIfRequired(graph)
        for (vertex in vertices) {
            writer.writeStartElement(GraphMLTokens.NODE)
            writer.writeAttribute(GraphMLTokens.ID, vertex.id().toString())
            val keys = getElementKeysAndNormalizeIfRequired(vertex)
            writer.writeStartElement(GraphMLTokens.DATA)
            writer.writeAttribute(GraphMLTokens.KEY, vertexLabelKey)
            writer.writeCharacters(vertex.label())
            writer.writeEndElement()
            for (key in keys) {
                writer.writeStartElement(GraphMLTokens.DATA)
                if (intersection != null && intersection!!.contains(key)) {
                    writer.writeAttribute(GraphMLTokens.KEY, key.concat(GraphMLTokens.VERTEX_SUFFIX))
                } else {
                    writer.writeAttribute(GraphMLTokens.KEY, key)
                }
                val currentValue: VertexProperty<Object> = getCheckedVertexProperty(vertex, key)

                // technically there can't be a null here as gremlin structure forbids that occurrence even if Graph
                // implementations support it, but out to empty string just in case.
                writer.writeCharacters(currentValue.orElse("").toString())
                writer.writeEndElement()
            }
            writer.writeEndElement()
        }
    }

    private fun getElementKeysAndNormalizeIfRequired(element: Element): Collection<String> {
        val keys: Collection<String>
        if (normalize) {
            keys = ArrayList()
            keys.addAll(element.keys())
            Collections.sort(keys as List<String?>)
        } else keys = element.keys()
        return keys
    }

    private fun getVerticesAndNormalizeIfRequired(graph: Graph): Iterable<Vertex> {
        val iterator: Iterator<Vertex> = graph.vertices()
        return try {
            val vertices: Iterable<Vertex>
            if (normalize) {
                vertices = ArrayList()
                while (iterator.hasNext()) {
                    (vertices as Collection<Vertex?>).add(iterator.next())
                }
                Collections.sort(vertices as List<Vertex?>, Comparators.ELEMENT_COMPARATOR)
            } else {
                vertices = IteratorUtils.list(iterator)
            }
            vertices
        } finally {
            CloseableIterator.closeIterator(iterator)
        }
    }

    private fun getKeysAndNormalizeIfRequired(identifiedKeyTypes: Map<String, String>): Collection<String> {
        val keyset: Collection<String>
        if (normalize) {
            keyset = ArrayList()
            keyset.addAll(identifiedKeyTypes.keySet())
            Collections.sort(keyset as List<String?>)
        } else keyset = identifiedKeyTypes.keySet()
        return keyset
    }

    @Throws(XMLStreamException::class)
    private fun writeXmlNsAndSchema(writer: XMLStreamWriter) {
        writer.writeAttribute(GraphMLTokens.XMLNS, GraphMLTokens.GRAPHML_XMLNS)

        //XML Schema instance namespace definition (xsi)
        writer.writeAttribute(
            XMLConstants.XMLNS_ATTRIBUTE + ':' + GraphMLTokens.XML_SCHEMA_NAMESPACE_TAG,
            XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI
        )
        //XML Schema location
        writer.writeAttribute(
            GraphMLTokens.XML_SCHEMA_NAMESPACE_TAG + ':' + GraphMLTokens.XML_SCHEMA_LOCATION_ATTRIBUTE,
            GraphMLTokens.GRAPHML_XMLNS + ' ' + xmlSchemaLocation.orElse(GraphMLTokens.DEFAULT_GRAPHML_SCHEMA_LOCATION)
        )
    }

    class Builder : WriterBuilder<GraphMLWriter?> {
        private var normalize = false
        private var vertexKeyTypes: Map<String, String>? = null
        private var edgeKeyTypes: Map<String, String>? = null
        private var xmlSchemaLocation: String? = null
        private var edgeLabelKey: String = GraphMLTokens.LABEL_E
        private var vertexLabelKey: String = GraphMLTokens.LABEL_V

        /**
         * Normalized output is deterministic with respect to the order of elements and properties in the resulting
         * XML document, and is compatible with line diff-based tools such as Git. Note: normalized output is
         * sideEffects-intensive and is not appropriate for very large graphs.
         *
         * @param normalize whether to normalize the output.
         */
        fun normalize(normalize: Boolean): Builder {
            this.normalize = normalize
            return this
        }

        /**
         * Map of the data types of the vertex keys.  It is best to specify this Map if possible as the only
         * other way to attain it is to iterate all vertices to find all the property keys.
         */
        fun vertexKeyTypes(vertexKeyTypes: Map<String, String>?): Builder {
            this.vertexKeyTypes = vertexKeyTypes
            return this
        }

        /**
         * Map of the data types of the edge keys.  It is best to specify this Map if possible as the only
         * other way to attain it is to iterate all vertices to find all the property keys.
         */
        fun edgeKeyTypes(edgeKeyTypes: Map<String, String>?): Builder {
            this.edgeKeyTypes = edgeKeyTypes
            return this
        }

        /**
         * Location of the GraphML schema which is defaulted to
         * [org.apache.tinkerpop4.gremlin.structure.io.graphml.GraphMLTokens.DEFAULT_GRAPHML_SCHEMA_LOCATION].
         */
        fun xmlSchemaLocation(xmlSchemaLocation: String?): Builder {
            this.xmlSchemaLocation = xmlSchemaLocation
            return this
        }

        /**
         * Set the name of the edge label in the GraphML. This value is defaulted to [GraphMLTokens.LABEL_E].
         * The value of [org.apache.tinkerpop4.gremlin.structure.Edge.label] is written as a data element on the edge
         * and the appropriate key element is added to define it in the GraphML.  It is important that when reading
         * this GraphML back in with the reader that this label key is set appropriately to properly read the edge
         * labels.
         *
         * @param edgeLabelKey if the label of an edge will be handled by the data property.
         */
        fun edgeLabelKey(edgeLabelKey: String): Builder {
            this.edgeLabelKey = edgeLabelKey
            return this
        }

        /**
         * Set the name of the vertex label in the GraphML. This value is defaulted to [GraphMLTokens.LABEL_V].
         * The value of [org.apache.tinkerpop4.gremlin.structure.Vertex.label] is written as a data element on the
         * vertex and the appropriate key element is added to define it in the GraphML.  It is important that when
         * reading this GraphML back in with the reader that this label key is set appropriately to properly read the
         * vertex labels.
         *
         * @param vertexLabelKey if the label of an vertex will be handled by the data property.
         */
        fun vertexLabelKey(vertexLabelKey: String): Builder {
            this.vertexLabelKey = vertexLabelKey
            return this
        }

        fun create(): GraphMLWriter {
            return GraphMLWriter(
                normalize,
                vertexKeyTypes,
                edgeKeyTypes,
                xmlSchemaLocation,
                edgeLabelKey,
                vertexLabelKey
            )
        }
    }

    companion object {
        private fun determineVertexTypes(graph: Graph): Map<String?, String> {
            val vertexKeyTypes: Map<String?, String> = HashMap()
            val vertices: Iterator<Vertex> = graph.vertices()
            try {
                while (vertices.hasNext()) {
                    val vertex: Vertex = vertices.next()
                    for (key in vertex.keys()) {
                        if (!vertexKeyTypes.containsKey(key)) {
                            val currentValue: VertexProperty<Object> = getCheckedVertexProperty(vertex, key)
                            vertexKeyTypes.put(key, getStringType(currentValue.value()))
                        }
                    }
                }
            } finally {
                CloseableIterator.closeIterator(vertices)
            }
            return vertexKeyTypes
        }

        private fun getCheckedVertexProperty(vertex: Vertex, key: String): VertexProperty<Object> {
            val properties: Iterator<VertexProperty<Object>> = vertex.properties(key)
            val currentValue: VertexProperty<Object> = properties.next()
            if (properties.hasNext()) throw IllegalStateException("Multiple properties exists for the provided key: [%s] and multi-properties are not directly supported by GraphML format")
            return currentValue
        }

        private fun determineEdgeTypes(graph: Graph): Map<String?, String> {
            val edgeKeyTypes: Map<String?, String> = HashMap()
            val edges: Iterator<Edge> = graph.edges()
            try {
                while (edges.hasNext()) {
                    val edge: Edge = edges.next()
                    for (key in edge.keys()) {
                        if (!edgeKeyTypes.containsKey(key)) edgeKeyTypes.put(
                            key,
                            getStringType(edge.property(key).value())
                        )
                    }
                }
            } finally {
                CloseableIterator.closeIterator(edges)
            }
            return edgeKeyTypes
        }

        private fun getStringType(`object`: Object): String {
            return if (`object` is String) GraphMLTokens.STRING else if (`object` is Integer) GraphMLTokens.INT else if (`object` is Long) GraphMLTokens.LONG else if (`object` is Float) GraphMLTokens.FLOAT else if (`object` is Double) GraphMLTokens.DOUBLE else if (`object` is Boolean) GraphMLTokens.BOOLEAN else GraphMLTokens.STRING
        }

        fun build(): Builder {
            return Builder()
        }
    }
}