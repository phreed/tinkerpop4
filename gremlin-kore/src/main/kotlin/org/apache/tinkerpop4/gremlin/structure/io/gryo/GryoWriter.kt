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
package org.apache.tinkerpop4.gremlin.structure.io.gryo

import org.apache.tinkerpop4.gremlin.structure.Direction

/**
 * The [GraphWriter] for the Gremlin Structure serialization format based on Kryo.  The format is meant to be
 * non-lossy in terms of Gremlin Structure to Gremlin Structure migrations (assuming both structure implementations
 * support the same graph features).
 *
 *
 * This implementation is not thread-safe.  Have one `GraphWriter` instance per thread.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class GryoWriter private constructor(gryoMapper: Mapper<Kryo>) : GraphWriter {
    private val kryo: Kryo

    init {
        kryo = gryoMapper.createMapper()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Throws(IOException::class)
    fun writeGraph(outputStream: OutputStream?, g: Graph) {
        writeVertices(outputStream, g.vertices(), Direction.BOTH)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Throws(IOException::class)
    fun writeVertices(outputStream: OutputStream?, vertexIterator: Iterator<Vertex?>, direction: Direction?) {
        kryo.getRegistration(StarGraph::class.java).setSerializer(StarGraphGryoSerializer.with(direction))
        val output = Output(outputStream)
        while (vertexIterator.hasNext()) {
            writeVertexInternal(output, vertexIterator.next())
        }
        output.flush()
        kryo.getRegistration(StarGraph::class.java).setSerializer(StarGraphGryoSerializer.with(Direction.BOTH))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Throws(IOException::class)
    fun writeVertices(outputStream: OutputStream?, vertexIterator: Iterator<Vertex?>) {
        writeVertices(outputStream, vertexIterator, null)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Throws(IOException::class)
    fun writeVertex(outputStream: OutputStream?, v: Vertex?, direction: Direction?) {
        kryo.getRegistration(StarGraph::class.java).setSerializer(StarGraphGryoSerializer.with(direction))
        val output = Output(outputStream)
        writeVertexInternal(output, v)
        output.flush()
        kryo.getRegistration(StarGraph::class.java).setSerializer(StarGraphGryoSerializer.with(Direction.BOTH))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Throws(IOException::class)
    fun writeVertex(outputStream: OutputStream?, v: Vertex?) {
        writeVertex(outputStream, v, null)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Throws(IOException::class)
    fun writeEdge(outputStream: OutputStream?, e: Edge?) {
        val output = Output(outputStream)
        writeHeader(output)
        kryo.writeObject(output, DetachedFactory.detach(e, true))
        output.flush()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Throws(IOException::class)
    fun writeVertexProperty(outputStream: OutputStream?, vp: VertexProperty?) {
        val output = Output(outputStream)
        writeHeader(output)
        kryo.writeObject(output, DetachedFactory.detach(vp, true))
        output.flush()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Throws(IOException::class)
    fun writeProperty(outputStream: OutputStream?, p: Property?) {
        val output = Output(outputStream)
        writeHeader(output)
        kryo.writeObject(output, DetachedFactory.detach(p, true))
        output.flush()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun writeObject(outputStream: OutputStream?, `object`: Object?) {
        val output = Output(outputStream)
        kryo.writeClassAndObject(output, `object`)
        output.flush()
    }

    @Throws(IOException::class)
    fun writeVertexInternal(output: Output, v: Vertex?) {
        writeHeader(output)
        kryo.writeObject(output, StarGraph.of(v))
        kryo.writeClassAndObject(output, VertexTerminator.INSTANCE)
    }

    @Throws(IOException::class)
    fun writeHeader(output: Output) {
        output.writeBytes(GryoMapper.HEADER)
    }

    class Builder private constructor() : WriterBuilder<GryoWriter?> {
        /**
         * Always creates the most current version available.
         */
        private var gryoMapper: Mapper<Kryo> = GryoMapper.build().create()

        /**
         * Supply a mapper [GryoMapper] instance to use as the serializer for the `KryoWriter`.
         */
        fun mapper(gryoMapper: Mapper<Kryo?>): Builder {
            this.gryoMapper = gryoMapper
            return this
        }

        /**
         * Create the `GryoWriter`.
         */
        fun create(): GryoWriter {
            return GryoWriter(gryoMapper)
        }
    }

    companion object {
        fun build(): Builder {
            return Builder()
        }
    }
}