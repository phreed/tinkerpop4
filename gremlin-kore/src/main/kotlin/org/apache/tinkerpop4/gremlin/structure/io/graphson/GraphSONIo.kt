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

import org.apache.tinkerpop4.gremlin.structure.Graph

/**
 * Constructs GraphSON IO implementations given a [Graph] and [IoRegistry]. Implementers of the [Graph]
 * interfaces should see the [GraphSONMapper] for information on the expectations for the [IoRegistry].
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class GraphSONIo private constructor(builder: Builder) :
    Io<GraphSONReader.Builder?, GraphSONWriter.Builder?, GraphSONMapper.Builder?> {
    private val graph: Graph?
    private val onMapper: Optional<Consumer<Mapper.Builder>>
    private val version: GraphSONVersion

    init {
        graph = builder.graph
        onMapper = Optional.ofNullable(builder.onMapper)
        version = builder.version
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun reader(): GraphSONReader.Builder {
        return GraphSONReader.build().mapper(mapper().create())
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun writer(): GraphSONWriter.Builder {
        return GraphSONWriter.build().mapper(mapper().create())
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun mapper(): GraphSONMapper.Builder {
        val builder: GraphSONMapper.Builder = GraphSONMapper.build().version(version)
        onMapper.ifPresent { c -> c.accept(builder) }
        return builder
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Throws(IOException::class)
    fun writeGraph(file: String?) {
        FileOutputStream(file).use { out -> writer().create().writeGraph(out, graph) }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Throws(IOException::class)
    fun readGraph(file: String?) {
        FileInputStream(file).use { `in` -> reader().create().readGraph(`in`, graph) }
    }

    class Builder internal constructor(version: GraphSONVersion) : Io.Builder<GraphSONIo?> {
        private var graph: Graph? = null
        private var onMapper: Consumer<Mapper.Builder>? = null
        private val version: GraphSONVersion

        init {
            this.version = version
        }

        @Override
        fun onMapper(onMapper: Consumer<Mapper.Builder?>): Io.Builder<out Io?> {
            this.onMapper = onMapper
            return this
        }

        @Override
        fun graph(g: Graph?): Io.Builder<GraphSONIo> {
            graph = g
            return this
        }

        @Override
        fun <V> requiresVersion(version: V): Boolean {
            return this.version === version
        }

        @Override
        fun create(): GraphSONIo {
            if (null == graph) throw IllegalArgumentException("The graph argument was not specified")
            return GraphSONIo(this)
        }
    }

    companion object {
        /**
         * Create a new builder using the default version of GraphSON - v3.
         */
        fun build(): Io.Builder<GraphSONIo> {
            return build(GraphSONVersion.V3_0)
        }

        /**
         * Create a new builder using the specified version of GraphSON.
         */
        fun build(version: GraphSONVersion): Io.Builder<GraphSONIo> {
            return Builder(version)
        }
    }
}