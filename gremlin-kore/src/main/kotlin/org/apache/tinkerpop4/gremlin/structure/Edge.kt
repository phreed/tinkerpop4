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
package org.apache.tinkerpop4.gremlin.structure

import java.util.Iterator

/**
 * An [Edge] links two [Vertex] objects. Along with its [Property] objects, an [Edge] has both
 * a [Direction] and a `label`. The [Direction] determines which [Vertex] is the tail
 * [Vertex] (out [Vertex]) and which [Vertex] is the head [Vertex]
 * (in [Vertex]). The [Edge] `label` determines the type of relationship that exists between the
 * two vertices.
 *
 *
 * Diagrammatically:
 * <pre>
 * outVertex ---label---&gt; inVertex.
</pre> *
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
interface Edge : Element {
    /**
     * Retrieve the vertex (or vertices) associated with this edge as defined by the direction.
     * If the direction is [Direction.BOTH] then the iterator order is: [Direction.OUT] then [Direction.IN].
     *
     * @param direction Get the incoming vertex, outgoing vertex, or both vertices
     * @return An iterator with 1 or 2 vertices
     */
    fun vertices(direction: Direction?): Iterator<Vertex?>

    /**
     * Get the outgoing/tail vertex of this edge.
     *
     * @return the outgoing vertex of the edge
     */
    fun outVertex(): Vertex? {
        return vertices(Direction.OUT).next()
    }

    /**
     * Get the incoming/head vertex of this edge.
     *
     * @return the incoming vertex of the edge
     */
    fun inVertex(): Vertex? {
        return vertices(Direction.IN).next()
    }

    /**
     * Get both the outgoing and incoming vertices of this edge.
     * The first vertex in the iterator is the outgoing vertex.
     * The second vertex in the iterator is the incoming vertex.
     *
     * @return an iterator of the two vertices of this edge
     */
    fun bothVertices(): Iterator<Vertex?>? {
        return vertices(Direction.BOTH)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun <V> properties(vararg propertyKeys: String?): Iterator<Property<V>?>?

    /**
     * Common exceptions to use with an edge.
     */
    object Exceptions : Element.Exceptions() {
        fun userSuppliedIdsNotSupported(): UnsupportedOperationException {
            return UnsupportedOperationException("Edge does not support user supplied identifiers")
        }

        fun userSuppliedIdsOfThisTypeNotSupported(): UnsupportedOperationException {
            return UnsupportedOperationException("Edge does not support user supplied identifiers of this type")
        }

        fun edgeRemovalNotSupported(): IllegalStateException {
            return IllegalStateException("Edge removal are not supported")
        }
    }

    companion object {
        /**
         * The default label to use for an edge.
         * This is typically never used as when an edge is created, an edge label is required to be specified.
         */
        const val DEFAULT_LABEL = "edge"
    }
}