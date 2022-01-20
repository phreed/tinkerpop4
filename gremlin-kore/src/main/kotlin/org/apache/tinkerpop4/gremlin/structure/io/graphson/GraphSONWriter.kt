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
 * A @{link GraphWriter} that writes a graph and its elements to a JSON-based representation. This implementation
 * only supports JSON data types and is therefore lossy with respect to data types (e.g. a float will become a double).
 * Further note that serialized `Map` objects do not support complex types for keys.  [Edge] and
 * [Vertex] objects are serialized to `Map` instances. If an
 * [Element] is used as a key, it is coerced to its identifier.  Other complex
 * objects are converted via [Object.toString] unless a mapper serializer is supplied.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class GraphSONWriter private constructor(builder: Builder) : GraphWriter {
    private val mapper: ObjectMapper
    private val wrapAdjacencyList: Boolean

    init {
        mapper = builder.mapper.createMapper()
        wrapAdjacencyList = builder.wrapAdjacencyList
    }

    /**
     * Writes a [Graph] to stream in an adjacency list format where vertices are written with edges from both
     * directions.  Under this serialization model, edges are grouped by label.
     *
     * @param outputStream the stream to write to.
     * @param g the graph to write to stream.
     */
    @Override
    @Throws(IOException::class)
    fun writeGraph(outputStream: OutputStream?, g: Graph) {
        writeVertices(outputStream, g.vertices(), Direction.BOTH)
    }

    /**
     * Writes a single [Vertex] to stream where edges only from the specified direction are written.
     * Under this serialization model, edges are grouped by label.
     *
     * @param direction the direction of edges to write or null if no edges are to be written.
     */
    @Override
    @Throws(IOException::class)
    fun writeVertex(outputStream: OutputStream?, v: Vertex?, direction: Direction?) {
        mapper.writeValue(outputStream, DirectionalStarGraph(StarGraph.of(v), direction))
    }

    /**
     * Writes a single [Vertex] with no edges serialized.
     *
     * @param outputStream the stream to write to.
     * @param v            the vertex to write.
     */
    @Override
    @Throws(IOException::class)
    fun writeVertex(outputStream: OutputStream?, v: Vertex?) {
        writeVertex(outputStream, v, null)
    }

    /**
     * Writes a list of vertices in adjacency list format where vertices are written with edges from both
     * directions.  Under this serialization model, edges are grouped by label.
     *
     * @param outputStream the stream to write to.
     * @param vertexIterator    a traversal that returns a list of vertices.
     * @param direction    if direction is null then no edges are written.
     */
    @Override
    @Throws(IOException::class)
    fun writeVertices(outputStream: OutputStream?, vertexIterator: Iterator<Vertex?>, direction: Direction?) {
        val writer = BufferedWriter(OutputStreamWriter(outputStream))
        ByteArrayOutputStream().use { baos ->
            if (wrapAdjacencyList) writer.write("{\"" + GraphSONTokens.VERTICES.toString() + "\":[")
            while (vertexIterator.hasNext()) {
                writeVertex(baos, vertexIterator.next(), direction)
                writer.write(String(baos.toByteArray()))
                if (wrapAdjacencyList) {
                    if (vertexIterator.hasNext()) writer.write(",")
                } else {
                    writer.newLine()
                }
                baos.reset()
            }
            if (wrapAdjacencyList) writer.write("]}")
        }
        writer.flush()
    }

    /**
     * Writes a list of vertices without edges.
     *
     * @param outputStream the stream to write to.
     * @param vertexIterator    a iterator that returns a list of vertices.
     */
    @Override
    @Throws(IOException::class)
    fun writeVertices(outputStream: OutputStream?, vertexIterator: Iterator<Vertex?>) {
        writeVertices(outputStream, vertexIterator, null)
    }

    /**
     * Writes an [Edge] object to the stream.  Note that this format is different from the format of an
     * [Edge] when serialized with a [Vertex] as done with
     * [.writeVertex] or
     * [.writeVertices] in that the edge label is part of the object and
     * vertex labels are included with their identifiers.
     *
     * @param outputStream the stream to write to.
     * @param e the edge to write.
     */
    @Override
    @Throws(IOException::class)
    fun writeEdge(outputStream: OutputStream?, e: Edge?) {
        mapper.writeValue(outputStream, e)
    }

    /**
     * Write a [VertexProperty] object to the stream.
     *
     * @param outputStream the stream to write to.
     * @param vp the vertex property to write.
     */
    @Override
    @Throws(IOException::class)
    fun writeVertexProperty(outputStream: OutputStream?, vp: VertexProperty?) {
        mapper.writeValue(outputStream, vp)
    }

    /**
     * Write a [Property] object to the stream.
     *
     * @param outputStream the stream to write to.
     * @param p the property to write.
     */
    @Override
    @Throws(IOException::class)
    fun writeProperty(outputStream: OutputStream?, p: Property?) {
        mapper.writeValue(outputStream, p)
    }

    /**
     * Writes an arbitrary object to the stream.  Note that Gremlin Server uses this method when serializing output,
     * thus the format of the GraphSON for a [Vertex] will be somewhat different from the format supplied
     * when using [.writeVertex]. For example, edges will never be included.
     *
     * @param outputStream the stream to write to
     * @param object the object to write which will use the standard serializer set
     */
    @Override
    @Throws(IOException::class)
    fun writeObject(outputStream: OutputStream?, `object`: Object?) {
        mapper.writeValue(outputStream, `object`)
    }

    class Builder private constructor() : WriterBuilder<GraphSONWriter?> {
        private var mapper: Mapper<ObjectMapper> = GraphSONMapper.build().create()
        private var wrapAdjacencyList = false

        /**
         * Override all of the builder options with this mapper.  If this value is set to something other than
         * null then that value will be used to construct the writer.
         */
        fun mapper(mapper: Mapper<ObjectMapper?>): Builder {
            this.mapper = mapper
            return this
        }

        /**
         * Wraps the output of [.writeGraph], [.writeVertices]
         * and [.writeVertices] in a JSON object.  By default, this value
         * is `false` which means that the output is such that there is one JSON object (vertex) per line.
         * When `true` the line breaks are not written and instead a valid JSON object is formed where the
         * vertices are part of a JSON array in a key called "vertices".
         *
         *
         * By setting this value to `true`, the generated JSON is no longer "splittable" by line and thus not
         * suitable for OLAP processing.  Furthermore, reading this format of the JSON with
         * [GraphSONReader.readGraph] or
         * [GraphSONReader.readVertices] requires that the
         * entire JSON object be read into memory, so it is best saved for "small" graphs.
         */
        fun wrapAdjacencyList(wrapAdjacencyListInObject: Boolean): Builder {
            wrapAdjacencyList = wrapAdjacencyListInObject
            return this
        }

        fun create(): GraphSONWriter {
            return GraphSONWriter(this)
        }
    }

    companion object {
        fun build(): Builder {
            return Builder()
        }
    }
}