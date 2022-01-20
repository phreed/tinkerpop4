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
package org.apache.tinkerpop4.gremlin.process.computer

import org.apache.tinkerpop4.gremlin.process.computer.util.VertexProgramHelper

/**
 * A [MessageScope] represents the range of a message. A message can have multiple receivers and message scope
 * allows the underlying [GraphComputer] to apply the message passing algorithm in whichever manner is most
 * efficient. It is best to use [MessageScope.Local] if possible as that provides more room for optimization by
 * providers than [MessageScope.Global].
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Matthias Broecheler (me@matthiasb.com)
 */
abstract class MessageScope {
    /**
     * A Global message is directed at an arbitrary vertex in the graph.
     * The recipient vertex need not be adjacent to the sending vertex.
     * This message scope should be avoided if a [Local] can be used.
     */
    class Global private constructor(vertices: Iterable<Vertex>) : MessageScope() {
        private val vertices: Iterable<Vertex>

        init {
            this.vertices = vertices
        }

        fun vertices(): Iterable<Vertex> {
            return vertices
        }

        @Override
        override fun hashCode(): Int {
            return 4676576
        }

        @Override
        override fun equals(other: Object?): Boolean {
            return other is Global
        }

        companion object {
            private val INSTANCE = of()
            fun of(vertices: Iterable<Vertex>): Global {
                return Global(vertices)
            }

            fun of(vararg vertices: Vertex?): Global {
                return Global(Arrays.asList(vertices))
            }

            fun instance(): Global {
                return INSTANCE
            }
        }
    }

    /**
     * A Local message is directed to an adjacent (or "memory adjacent") vertex.
     * The adjacent vertex set is defined by the provided [Traversal] that dictates how to go from the sending vertex to the receiving vertex.
     * This is the preferred message scope as it can potentially be optimized by the underlying [Messenger] implementation.
     * The preferred optimization is to not distribute a message object to all adjacent vertices.
     * Instead, allow the recipients to read a single message object stored at the "sending" vertex.
     * This is possible via `Traversal.reverse()`. This optimizations greatly reduces the amount of data created in the computation.
     *
     * @param <M> The [VertexProgram] message class
    </M> */
    class Local<M> private constructor(
        incidentTraversal: Supplier<out Traversal<Vertex?, Edge?>?>,
        edgeFunction: BiFunction<M, Edge, M>
    ) : MessageScope() {
        val incidentTraversal: Supplier<out Traversal<Vertex?, Edge?>?>
        val edgeFunction: BiFunction<M, Edge, M>

        private constructor(incidentTraversal: Supplier<out Traversal<Vertex?, Edge?>?>) : this(
            incidentTraversal,
            BiFunction<M, Edge, M> { m: M, e: Edge? -> m }) // the default is an identity function
        {
        }

        init {
            this.incidentTraversal = incidentTraversal
            this.edgeFunction = edgeFunction
        }

        fun getEdgeFunction(): BiFunction<M, Edge, M> {
            return edgeFunction
        }

        fun getIncidentTraversal(): Supplier<out Traversal<Vertex?, Edge?>?> {
            return incidentTraversal
        }

        @Override
        override fun hashCode(): Int {
            return edgeFunction.hashCode() xor incidentTraversal.get().hashCode()
        }

        @Override
        override fun equals(other: Object): Boolean {
            return other is Local<*> &&
                    (other as Local<*>).incidentTraversal.get().equals(
                        incidentTraversal.get()
                    ) && (other as Local<*>).edgeFunction === edgeFunction
        }

        /**
         * A helper class that can be used to generate the reverse traversal of the traversal within a [MessageScope.Local].
         */
        class ReverseTraversalSupplier(private val localMessageScope: Local<*>) : Supplier<Traversal<Vertex?, Edge?>?> {
            fun get(): Traversal<Vertex, Edge> {
                return VertexProgramHelper.reverse(localMessageScope.getIncidentTraversal().get().asAdmin())
            }
        }

        companion object {
            fun <M> of(incidentTraversal: Supplier<out Traversal<Vertex?, Edge?>?>): Local<M> {
                return Local(incidentTraversal)
            }

            fun <M> of(
                incidentTraversal: Supplier<out Traversal<Vertex?, Edge?>?>,
                edgeFunction: BiFunction<M, Edge, M>
            ): Local<M> {
                return Local(incidentTraversal, edgeFunction)
            }
        }
    }
}