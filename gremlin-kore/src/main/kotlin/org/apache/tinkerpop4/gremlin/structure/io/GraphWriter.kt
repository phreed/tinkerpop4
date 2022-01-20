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
package org.apache.tinkerpop4.gremlin.structure.io

import org.apache.tinkerpop4.gremlin.process.traversal.Traversal

/**
 * Functions for writing a graph and its elements to a serialized format. Implementations of this class do not need
 * to explicitly guarantee that an object written with one method must have its format equivalent to another. In other
 * words, calling [.writeVertex]} need not have equivalent output to
 * [.writeObject].  Nor does the representation of an [Edge] within the output of
 * [.writeVertex] need to match the representation of that same
 * [Edge] when provided to [.writeEdge]. In other words, implementations are free
 * to optimize as is possible for a specific serialization method.
 *
 *
 * That said, it is however important that the complementary "read" operation in [GraphReader] be capable of
 * reading the output of the writer.  In other words, the output of [.writeObject]
 * should always be readable by [GraphReader.readObject] and the output of
 * [.writeGraph] should always be readable by
 * [GraphReader.readGraph].
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
interface GraphWriter {
    /**
     * Write the entire graph to a stream.
     *
     * @param outputStream the stream to write to.
     * @param g the graph to write to stream.
     */
    @Throws(IOException::class)
    fun writeGraph(outputStream: OutputStream?, g: Graph?)

    /**
     * Write a vertex to a stream with its associated edges.  Only write edges as defined by the requested direction.
     *
     * @param outputStream the stream to write to.
     * @param v            the vertex to write.
     * @param direction    the direction of edges to write or null if no edges are to be written.
     */
    @Throws(IOException::class)
    fun writeVertex(outputStream: OutputStream?, v: Vertex?, direction: Direction?)

    /**
     * Write a vertex to a stream without writing its edges.
     *
     * @param outputStream the stream to write to.
     * @param v            the vertex to write.
     */
    @Throws(IOException::class)
    fun writeVertex(outputStream: OutputStream?, v: Vertex?)

    /**
     * Write a list of vertices from a [Traversal] to a stream with its associated edges.  Only write edges as
     * defined by the requested direction.
     *
     * @param outputStream the stream to write to.
     * @param vertexIterator a traversal that returns a list of vertices.
     * @param direction the direction of edges to write or null if no edges are to be written.
     */
    @Throws(IOException::class)
    fun writeVertices(outputStream: OutputStream?, vertexIterator: Iterator<Vertex?>, direction: Direction?) {
        while (vertexIterator.hasNext()) {
            writeVertex(outputStream, vertexIterator.next(), direction)
        }
    }

    /**
     * Write a vertex to a stream without writing its edges.
     *
     * @param outputStream the stream to write to.
     * @param vertexIterator a iterator that returns a list of vertices.
     */
    @Throws(IOException::class)
    fun writeVertices(outputStream: OutputStream?, vertexIterator: Iterator<Vertex?>) {
        while (vertexIterator.hasNext()) {
            writeVertex(outputStream, vertexIterator.next())
        }
    }

    /**
     * Write an edge to a stream.
     *
     * @param outputStream the stream to write to.
     * @param e the edge to write.
     */
    @Throws(IOException::class)
    fun writeEdge(outputStream: OutputStream?, e: Edge?)

    /**
     * Write a vertex property to a stream.
     *
     * @param outputStream the stream to write to.
     * @param vp the vertex property to write.
     */
    @Throws(IOException::class)
    fun writeVertexProperty(outputStream: OutputStream?, vp: VertexProperty?)

    /**
     * Write a property to a stream.
     *
     * @param outputStream the stream to write to.
     * @param p the property to write.
     */
    @Throws(IOException::class)
    fun writeProperty(outputStream: OutputStream?, p: Property?)

    /**
     * Writes an arbitrary object to the stream.
     *
     * @param outputStream the stream to write to.
     * @param object the object to write which will use the standard serializer set.
     */
    @Throws(IOException::class)
    fun writeObject(outputStream: OutputStream?, `object`: Object?)

    /**
     * Largely a marker interface for builder classes that construct a [GraphWriter].
     */
    interface WriterBuilder<T : GraphWriter?> {
        /**
         * Creates the [GraphWriter] implementation given options provided to the [WriterBuilder]
         * implementation.
         */
        fun create(): T
    }
}