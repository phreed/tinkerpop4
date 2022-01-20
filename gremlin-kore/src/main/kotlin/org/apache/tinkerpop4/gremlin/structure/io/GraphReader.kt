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

import org.apache.tinkerpop4.gremlin.process.computer.GraphFilter

/**
 * Functions for reading a graph and its graph elements from a different serialization format. Implementations of
 * this class do not need to explicitly guarantee that an object read with one method must have its format
 * equivalent to another. In other words the input to [.readVertex]} need not also
 * be readable by [.readObject]. In other words, implementations are free
 * to optimize as is possible for a specific serialization method.
 *
 *
 * That said, it is however important that the complementary "write" operation in [GraphWriter] be capable of
 * writing output compatible to its reader.  In other words, the output of
 * [GraphWriter.writeObject] should always be readable by
 * [.readObject] and the output of [GraphWriter.writeGraph]
 * should always be readable by [.readGraph].
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
interface GraphReader {
    /**
     * Reads an entire graph from an [InputStream].  This method is mean to load an empty [Graph].
     * It is up to individual implementations to manage transactions, but it is not required or enforced.  Consult
     * the documentation of an implementation to understand the approach it takes.
     *
     * @param inputStream    a stream containing an entire graph of vertices and edges as defined by the accompanying
     * [GraphWriter.writeGraph].
     * @param graphToWriteTo the graph to write to when reading from the stream.
     */
    @Throws(IOException::class)
    fun readGraph(inputStream: InputStream?, graphToWriteTo: Graph?)

    /**
     * Reads a single vertex from an [InputStream]. This method will filter the read the read vertex by the provided
     * [GraphFilter]. If the graph filter will filter the vertex itself, then the returned [Optional] is empty.
     *
     * @param inputStream a stream containing at least a single vertex as defined by the accompanying
     * [GraphWriter.writeVertex].
     * @param graphFilter The [GraphFilter] to filter the vertex and its associated edges by.
     * @return the vertex with filtered edges or [Optional.empty]  if the vertex itself was filtered.
     * @throws IOException
     */
    @Throws(IOException::class)
    fun readVertex(inputStream: InputStream?, graphFilter: GraphFilter?): Optional<Vertex?>? {
        throw UnsupportedOperationException(
            this.getClass()
                .getCanonicalName() + " currently does not support " + GraphFilter::class.java.getSimpleName() + " deserialization filtering"
        )
    }

    /**
     * Reads a single vertex from an [InputStream].  This method will read vertex properties but not edges.
     * It is expected that the user will manager their own transaction context with respect to this method (i.e.
     * implementations should not commit the transaction for the user).
     *
     * @param inputStream        a stream containing at least a single vertex as defined by the accompanying
     * [GraphWriter.writeVertex].
     * @param vertexAttachMethod a function that creates re-attaches a [Vertex] to a [Host] object.
     */
    @Throws(IOException::class)
    fun readVertex(inputStream: InputStream?, vertexAttachMethod: Function<Attachable<Vertex?>?, Vertex?>?): Vertex?

    /**
     * Reads a single vertex from an [InputStream].  This method will read vertex properties as well as edges
     * given the direction supplied as an argument.  It is expected that the user will manager their own transaction
     * context with respect to this method (i.e. implementations should not commit the transaction for the user).
     *
     * @param inputStream                a stream containing at least one [Vertex] as defined by the accompanying
     * [GraphWriter.writeVertices] method.
     * @param vertexAttachMethod         a function that creates re-attaches a [Vertex] to a [Host] object.
     * @param edgeAttachMethod           a function that creates re-attaches a [Edge] to a [Host] object.
     * @param attachEdgesOfThisDirection only edges of this direction are passed to the `edgeMaker`.
     */
    @Throws(IOException::class)
    fun readVertex(
        inputStream: InputStream?,
        vertexAttachMethod: Function<Attachable<Vertex?>?, Vertex?>?,
        edgeAttachMethod: Function<Attachable<Edge?>?, Edge?>?,
        attachEdgesOfThisDirection: Direction?
    ): Vertex?

    /**
     * Reads a set of one or more vertices from an [InputStream] which were written by
     * [GraphWriter.writeVertices].  This method will read vertex properties as well as
     * edges given the direction supplied as an argument. It is expected that the user will manager their own
     * transaction context with respect to this method (i.e. implementations should not commit the transaction for
     * the user).
     *
     * @param inputStream                a stream containing at least one [Vertex] as defined by the accompanying
     * [GraphWriter.writeVertices] or
     * [GraphWriter.writeVertices] methods.
     * @param vertexAttachMethod         a function that creates re-attaches a [Vertex] to a [Host] object.
     * @param edgeAttachMethod           a function that creates re-attaches a [Edge] to a [Host] object.
     * @param attachEdgesOfThisDirection only edges of this direction are passed to the `edgeMaker`.
     */
    @Throws(IOException::class)
    fun readVertices(
        inputStream: InputStream?,
        vertexAttachMethod: Function<Attachable<Vertex?>?, Vertex?>?,
        edgeAttachMethod: Function<Attachable<Edge?>?, Edge?>?,
        attachEdgesOfThisDirection: Direction?
    ): Iterator<Vertex?>?

    /**
     * Reads a single edge from an [InputStream]. It is expected that the user will manager their own
     * transaction context with respect to this method (i.e. implementations should not commit the transaction for
     * the user).
     *
     * @param inputStream      a stream containing at least one [Edge] as defined by the accompanying
     * [GraphWriter.writeEdge] method.
     * @param edgeAttachMethod a function that creates re-attaches a [Edge] to a [Host] object.
     */
    @Throws(IOException::class)
    fun readEdge(inputStream: InputStream?, edgeAttachMethod: Function<Attachable<Edge?>?, Edge?>?): Edge?

    /**
     * Reads a single vertex property from an [InputStream].  It is expected that the user will manager their own
     * transaction context with respect to this method (i.e. implementations should not commit the transaction for
     * the user).
     *
     * @param inputStream                a stream containing at least one [VertexProperty] as written by the accompanying
     * [GraphWriter.writeVertexProperty] method.
     * @param vertexPropertyAttachMethod a function that creates re-attaches a [VertexProperty] to a
     * [Host] object.
     * @return the value returned by the attach method.
     */
    @Throws(IOException::class)
    fun readVertexProperty(
        inputStream: InputStream?,
        vertexPropertyAttachMethod: Function<Attachable<VertexProperty?>?, VertexProperty?>?
    ): VertexProperty?

    /**
     * Reads a single property from an [InputStream].  It is expected that the user will manager their own
     * transaction context with respect to this method (i.e. implementations should not commit the transaction for
     * the user).
     *
     * @param inputStream          a stream containing at least one [Property] as written by the accompanying
     * [GraphWriter.writeProperty] method.
     * @param propertyAttachMethod a function that creates re-attaches a [Property] to a [Host] object.
     * @return the value returned by the attach method.
     */
    @Throws(IOException::class)
    fun readProperty(
        inputStream: InputStream?,
        propertyAttachMethod: Function<Attachable<Property?>?, Property?>?
    ): Property?

    /**
     * Reads an arbitrary object using the registered serializers.
     *
     * @param inputStream a stream containing an object.
     * @param clazz       the class expected to be in the stream - may or may not be used by the underlying implementation.
     */
    @Throws(IOException::class)
    fun <C> readObject(inputStream: InputStream?, clazz: Class<out C>?): C

    /**
     * Largely a marker interface for builder classes that construct a [GraphReader].
     */
    interface ReaderBuilder<T : GraphReader?> {
        /**
         * Creates the [GraphReader] implementation given options provided to the [ReaderBuilder]
         * implementation.
         */
        fun create(): T
    }
}