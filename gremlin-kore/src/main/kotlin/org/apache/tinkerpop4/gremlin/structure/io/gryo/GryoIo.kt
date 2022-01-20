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

import org.apache.tinkerpop4.gremlin.structure.Graph

/**
 * Constructs Gryo IO implementations given a [Graph] and [IoRegistry]. Implementers of the [Graph]
 * interfaces should see the [GryoMapper] for information on the expectations for the [IoRegistry].
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class GryoIo private constructor(builder: Builder) : Io<GryoReader.Builder?, GryoWriter.Builder?, GryoMapper.Builder?> {
    private val graph: Graph?
    private val onMapper: Optional<Consumer<Mapper.Builder>>
    private val version: GryoVersion

    init {
        graph = builder.graph
        onMapper = Optional.ofNullable(builder.onMapper)
        version = builder.version
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun reader(): GryoReader.Builder {
        return GryoReader.build().mapper(mapper().create())
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun writer(): GryoWriter.Builder {
        return GryoWriter.build().mapper(mapper().create())
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun mapper(): GryoMapper.Builder {
        val builder: GryoMapper.Builder = GryoMapper.build().version(version)
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

    class Builder internal constructor(version: GryoVersion) : Io.Builder<GryoIo?> {
        private var graph: Graph? = null
        private var onMapper: Consumer<Mapper.Builder>? = null
        private val version: GryoVersion

        init {
            this.version = version
        }

        @Override
        fun onMapper(onMapper: Consumer<Mapper.Builder?>): Io.Builder<out Io?> {
            this.onMapper = onMapper
            return this
        }

        @Override
        fun graph(g: Graph?): Io.Builder<GryoIo> {
            graph = g
            return this
        }

        @Override
        fun <V> requiresVersion(version: V): Boolean {
            return this.version === version
        }

        @Override
        fun create(): GryoIo {
            if (null == graph) throw IllegalArgumentException("The graph argument was not specified")
            return GryoIo(this)
        }
    }

    companion object {
        /**
         * Create a new builder using the default version of Gryo - v3.
         */
        fun build(): Io.Builder<GryoIo> {
            return build(GryoVersion.V3_0)
        }

        /**
         * Create a new builder using the specified version of Gryo.
         */
        fun build(version: GryoVersion): Io.Builder<GryoIo> {
            return Builder(version)
        }
    }
}