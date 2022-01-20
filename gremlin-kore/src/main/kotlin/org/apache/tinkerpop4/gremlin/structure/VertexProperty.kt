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

import org.apache.tinkerpop4.gremlin.structure.util.empty.EmptyVertexProperty

/**
 * A `VertexProperty` is similar to a [Property] in that it denotes a key/value pair associated with an
 * [Vertex], however it is different in the sense that it also represents an entity that it is an [Element]
 * that can have properties of its own.
 *
 *
 * A property is much like a Java8 `Optional` in that a property can be not present (i.e. empty).
 * The key of a property is always a String and the value of a property is an arbitrary Java object.
 * Each underlying graph engine will typically have constraints on what Java objects are allowed to be used as values.
 *
 * @author Matthias Broecheler (me@matthiasb.com)
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
interface VertexProperty<V> : Property<V>, Element {
    enum class Cardinality {
        single, list, set
    }

    /**
     * Gets the [Vertex] that owns this `VertexProperty`.
     */
    @Override
    fun element(): Vertex

    /**
     * {@inheritDoc}
     */
    @Override
    fun graph(): Graph? {
        return element().graph()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun label(): String? {
        return this.key()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun <U> properties(vararg propertyKeys: String?): Iterator<Property<U>?>?

    /**
     * Common exceptions to use with a property.
     */
    object Exceptions {
        fun userSuppliedIdsNotSupported(): UnsupportedOperationException {
            return UnsupportedOperationException("VertexProperty does not support user supplied identifiers")
        }

        fun userSuppliedIdsOfThisTypeNotSupported(): UnsupportedOperationException {
            return UnsupportedOperationException("VertexProperty does not support user supplied identifiers of this type")
        }

        fun multiPropertiesNotSupported(): UnsupportedOperationException {
            return UnsupportedOperationException("Multiple properties on a vertex is not supported")
        }

        fun identicalMultiPropertiesNotSupported(): UnsupportedOperationException {
            return UnsupportedOperationException("Multiple properties on a vertex is supported, but a single key may not hold the same value more than once")
        }

        fun metaPropertiesNotSupported(): UnsupportedOperationException {
            return UnsupportedOperationException("Properties on a vertex property is not supported")
        }
    }

    companion object {
        const val DEFAULT_LABEL = "vertexProperty"

        /**
         * Constructs an empty `VertexProperty`.
         */
        fun <V> empty(): VertexProperty<V>? {
            return EmptyVertexProperty.instance()
        }
    }
}