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

import org.apache.tinkerpop4.gremlin.structure.util.Host
import java.util.Iterator

/**
 * A [Vertex] maintains pointers to both a set of incoming and outgoing [Edge] objects. The outgoing edges
 * are those edges for  which the [Vertex] is the tail. The incoming edges are those edges for which the
 * [Vertex] is the head.
 *
 *
 * Diagrammatically:
 * <pre>
 * ---inEdges---&gt; vertex ---outEdges---&gt;.
</pre> *
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
interface Vertex : Element, Host {
    /**
     * Add an outgoing edge to the vertex with provided label and edge properties as key/value pairs.
     * These key/values must be provided in an even number where the odd numbered arguments are [String]
     * property keys and the even numbered arguments are the related property values.
     *
     * @param label     The label of the edge
     * @param inVertex  The vertex to receive an incoming edge from the current vertex
     * @param keyValues The key/value pairs to turn into edge properties
     * @return the newly created edge
     */
    fun addEdge(label: String?, inVertex: Vertex?, vararg keyValues: Object?): Edge?

    /**
     * Get the [VertexProperty] for the provided key. If the property does not exist, return
     * [VertexProperty.empty]. If there are more than one vertex properties for the provided
     * key, then throw [Vertex.Exceptions.multiplePropertiesExistForProvidedKey].
     *
     * @param key the key of the vertex property to get
     * @param <V> the expected type of the vertex property value
     * @return the retrieved vertex property
    </V> */
    @Override
    fun <V> property(key: String): VertexProperty<V>? {
        val iterator: Iterator<VertexProperty<V>> = properties<Any>(key)!!
        return if (iterator.hasNext()) {
            val property: VertexProperty<V> = iterator.next()
            if (iterator.hasNext()) throw Exceptions.multiplePropertiesExistForProvidedKey(key) else property
        } else {
            VertexProperty.< V > empty < V ? > ()
        }
    }

    /**
     * Set the provided key to the provided value using [VertexProperty.Cardinality.single].
     *
     * @param key   the key of the vertex property
     * @param value The value of the vertex property
     * @param <V>   the type of the value of the vertex property
     * @return the newly created vertex property
    </V> */
    @Override
    fun <V> property(key: String?, value: V): VertexProperty<V>? {
        return this.property(key, value, *EMPTY_ARGS)
    }

    /**
     * Set the provided key to the provided value using default [VertexProperty.Cardinality] for that key.
     * The default cardinality can be vendor defined and is usually tied to the graph schema.
     * The default implementation of this method determines the cardinality
     * `graph().features().vertex().getCardinality(key)`. The provided key/values are the properties of the
     * newly created [VertexProperty]. These key/values must be provided in an even number where the odd
     * numbered arguments are [String].
     *
     * @param key       the key of the vertex property
     * @param value     The value of the vertex property
     * @param keyValues the key/value pairs to turn into vertex property properties
     * @param <V>       the type of the value of the vertex property
     * @return the newly created vertex property
    </V> */
    fun <V> property(key: String?, value: V, vararg keyValues: Object?): VertexProperty<V>? {
        return this.property(graph().features().vertex().getCardinality(key), key, value, keyValues)
    }

    /**
     * Create a new vertex property. If the cardinality is [VertexProperty.Cardinality.single], then set the key
     * to the value. If the cardinality is [VertexProperty.Cardinality.list], then add a new value to the key.
     * If the cardinality is [VertexProperty.Cardinality.set], then only add a new value if that value doesn't
     * already exist for the key. If the value already exists for the key, add the provided key value vertex property
     * properties to it.
     *
     * @param cardinality the desired cardinality of the property key
     * @param key         the key of the vertex property
     * @param value       The value of the vertex property
     * @param keyValues   the key/value pairs to turn into vertex property properties
     * @param <V>         the type of the value of the vertex property
     * @return the newly created vertex property
    </V> */
    fun <V> property(
        cardinality: VertexProperty.Cardinality?,
        key: String?,
        value: V,
        vararg keyValues: Object?
    ): VertexProperty<V>?

    /**
     * Gets an [Iterator] of incident edges.
     *
     * @param direction  The incident direction of the edges to retrieve off this vertex
     * @param edgeLabels The labels of the edges to retrieve. If no labels are provided, then get all edges.
     * @return An iterator of edges meeting the provided specification
     */
    fun edges(direction: Direction?, vararg edgeLabels: String?): Iterator<Edge?>?

    /**
     * Gets an [Iterator] of adjacent vertices.
     *
     * @param direction  The adjacency direction of the vertices to retrieve off this vertex
     * @param edgeLabels The labels of the edges associated with the vertices to retrieve. If no labels are provided,
     * then get all edges.
     * @return An iterator of vertices meeting the provided specification
     */
    fun vertices(direction: Direction?, vararg edgeLabels: String?): Iterator<Vertex?>?

    /**
     * {@inheritDoc}
     */
    @Override
    fun <V> properties(vararg propertyKeys: String?): Iterator<VertexProperty<V>?>?

    /**
     * Common exceptions to use with a vertex.
     */
    object Exceptions {
        fun userSuppliedIdsNotSupported(): UnsupportedOperationException {
            return UnsupportedOperationException("Vertex does not support user supplied identifiers")
        }

        fun userSuppliedIdsOfThisTypeNotSupported(): UnsupportedOperationException {
            return UnsupportedOperationException("Vertex does not support user supplied identifiers of this type")
        }

        fun vertexRemovalNotSupported(): IllegalStateException {
            return IllegalStateException("Vertex removal are not supported")
        }

        fun edgeAdditionsNotSupported(): IllegalStateException {
            return IllegalStateException("Edge additions not supported")
        }

        fun multiplePropertiesExistForProvidedKey(propertyKey: String): IllegalStateException {
            return IllegalStateException("Multiple properties exist for the provided key, use Vertex.properties($propertyKey)")
        }
    }

    companion object {
        /**
         * The default label to use for a vertex.
         */
        const val DEFAULT_LABEL = "vertex"
        val EMPTY_ARGS: Array<Object?> = arrayOfNulls<Object>(0)
    }
}