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

import org.apache.tinkerpop4.gremlin.structure.Graph

/**
 * Constructs GraphML IO implementations given a [Graph] and [IoRegistry]. Implementers of the [Graph]
 * interfaces do not have to register any special serializers to the [IoRegistry] as GraphML does not support
 * such things.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class GraphMLIo private constructor(builder: Builder) :
    Io<GraphMLReader.Builder?, GraphMLWriter.Builder?, GraphMLMapper.Builder?> {
    private val graph: Graph?
    private val onMapper: Optional<Consumer<Mapper.Builder>>

    init {
        graph = builder.graph
        onMapper = Optional.ofNullable(builder.onMapper)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun reader(): GraphMLReader.Builder {
        return GraphMLReader.build()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun writer(): GraphMLWriter.Builder {
        return GraphMLWriter.build()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun mapper(): GraphMLMapper.Builder {
        val builder: GraphMLMapper.Builder = GraphMLMapper.build()
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

    class Builder : Io.Builder<GraphMLIo?> {
        private var graph: Graph? = null
        private var onMapper: Consumer<Mapper.Builder>? = null
        @Override
        fun onMapper(onMapper: Consumer<Mapper.Builder?>): Io.Builder<out Io?> {
            this.onMapper = onMapper
            return this
        }

        @Override
        fun graph(g: Graph?): Io.Builder<GraphMLIo> {
            graph = g
            return this
        }

        /**
         * GraphML does not have a version and therefore always returns false. This default return also makes sense
         * because GraphML does not use custom [IoRegistry] instances anyway which is the primary reason for
         * calling this method by a graph provider.
         */
        @Override
        fun <V> requiresVersion(version: V): Boolean {
            return false
        }

        @Override
        fun create(): GraphMLIo {
            if (null == graph) throw IllegalArgumentException("The graph argument was not specified")
            return GraphMLIo(this)
        }
    }

    companion object {
        fun build(): Io.Builder<GraphMLIo> {
            return Builder()
        }
    }
}