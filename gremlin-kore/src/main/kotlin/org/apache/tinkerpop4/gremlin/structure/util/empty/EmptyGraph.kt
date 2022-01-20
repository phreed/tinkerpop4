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
package org.apache.tinkerpop4.gremlin.structure.util.empty

import org.apache.commons.configuration2.Configuration

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
@GraphFactoryClass(EmptyGraphFactory::class)
class EmptyGraph private constructor() : Graph {
    private val features = EmptyGraphFeatures()
    @Override
    fun features(): Features {
        return features
    }

    @Override
    fun addVertex(vararg keyValues: Object?): Vertex {
        throw Exceptions.vertexAdditionsNotSupported()
    }

    @Override
    fun <C : GraphComputer?> compute(graphComputerClass: Class<C>?): C {
        throw Exceptions.graphComputerNotSupported()
    }

    @Override
    fun compute(): GraphComputer {
        throw Exceptions.graphComputerNotSupported()
    }

    @Override
    fun tx(): Transaction {
        throw Exceptions.transactionsNotSupported()
    }

    @Override
    fun variables(): Variables {
        throw Exceptions.variablesNotSupported()
    }

    @Override
    fun configuration(): Configuration {
        throw IllegalStateException(MESSAGE)
    }

    @Override
    @Throws(Exception::class)
    fun close() {
        throw IllegalStateException(MESSAGE)
    }

    @Override
    fun vertices(vararg vertexIds: Object?): Iterator<Vertex> {
        return Collections.emptyIterator()
    }

    @Override
    fun edges(vararg edgeIds: Object?): Iterator<Edge> {
        return Collections.emptyIterator()
    }

    @Override
    override fun toString(): String {
        return StringFactory.graphString(this, "empty")
    }

    /**
     * Features defined such that they support immutability but allow all other possibilities.
     */
    class EmptyGraphFeatures : Graph.Features {
        private val graphFeatures: GraphFeatures = EmptyGraphGraphFeatures()
        private val vertexFeatures: VertexFeatures = EmptyGraphVertexFeatures()
        private val edgeFeatures: EdgeFeatures = EmptyGraphEdgeFeatures()
        private val edgePropertyFeatures: EdgePropertyFeatures = EmptyGraphEdgePropertyFeatures()
        private val vertexPropertyFeatures: VertexPropertyFeatures = EmptyGraphVertexPropertyFeatures()
        @Override
        fun graph(): GraphFeatures {
            return graphFeatures
        }

        @Override
        fun vertex(): VertexFeatures {
            return vertexFeatures
        }

        @Override
        fun edge(): EdgeFeatures {
            return edgeFeatures
        }

        /**
         * Graph features defined such that they support immutability but allow all other possibilities.
         */
        inner class EmptyGraphGraphFeatures : GraphFeatures {
            @Override
            fun supportsPersistence(): Boolean {
                return false
            }

            @Override
            fun supportsTransactions(): Boolean {
                return false
            }

            @Override
            fun supportsThreadedTransactions(): Boolean {
                return false
            }

            @Override
            fun variables(): VariableFeatures? {
                return null
            }

            @Override
            fun supportsComputer(): Boolean {
                return false
            }
        }

        /**
         * Vertex features defined such that they support immutability but allow all other possibilities.
         */
        inner class EmptyGraphVertexFeatures : EmptyGraphElementFeatures(), VertexFeatures {
            @Override
            fun getCardinality(key: String?): VertexProperty.Cardinality {
                // probably not much hurt here in returning list...it's an "empty graph"
                return VertexProperty.Cardinality.list
            }

            @Override
            fun supportsAddVertices(): Boolean {
                return false
            }

            @Override
            fun supportsRemoveVertices(): Boolean {
                return false
            }

            @Override
            fun properties(): VertexPropertyFeatures {
                return vertexPropertyFeatures
            }
        }

        /**
         * Edge features defined such that they support immutability but allow all other possibilities.
         */
        inner class EmptyGraphEdgeFeatures : EmptyGraphElementFeatures(), EdgeFeatures {
            @Override
            fun supportsAddEdges(): Boolean {
                return false
            }

            @Override
            fun supportsRemoveEdges(): Boolean {
                return false
            }

            @Override
            fun properties(): EdgePropertyFeatures {
                return edgePropertyFeatures
            }
        }

        /**
         * Vertex Property features defined such that they support immutability but allow all other possibilities.
         */
        inner class EmptyGraphVertexPropertyFeatures : VertexPropertyFeatures {
            @Override
            fun supportsRemoveProperty(): Boolean {
                return false
            }
        }

        /**
         * Edge property features defined such that they support immutability but allow all other possibilities.
         */
        inner class EmptyGraphEdgePropertyFeatures : EdgePropertyFeatures

        /**
         * Vertex features defined such that they support immutability but allow all other possibilities.
         */
        abstract inner class EmptyGraphElementFeatures : ElementFeatures {
            @Override
            fun supportsAddProperty(): Boolean {
                return false
            }

            @Override
            fun supportsRemoveProperty(): Boolean {
                return false
            }
        }
    }

    /**
     * [EmptyGraph] doesn't have a standard `open()` method because it is a singleton. Use this factory
     * to provide as a [GraphFactoryClass] for [EmptyGraph] so that [GraphFactory] can instantiate
     * it in a generalized way. End users should not generally use this method of instantiation.
     */
    object EmptyGraphFactory {
        fun open(conf: Configuration?): Graph {
            return instance()
        }
    }

    companion object {
        private const val MESSAGE = "The graph is immutable and empty"
        private val INSTANCE = EmptyGraph()
        fun instance(): Graph {
            return INSTANCE
        }
    }
}