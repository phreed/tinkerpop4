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

import java.util.function.Function

/**
 * A collection of (T)okens which allows for more concise Traversal definitions.
 * T implements [Function] can be used to map an element to its token value.
 * For example, `T.id.apply(element)`.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
enum class T : Function<Element?, Object?> {
    /**
     * Label (representing Element.label())
     *
     * @since 3.0.0-incubating
     */
    label {
        @Override
        override fun getAccessor(): String {
            return LABEL
        }

        @Override
        override fun apply(element: Element): String {
            return element.label()
        }
    },

    /**
     * Id (representing Element.id())
     *
     * @since 3.0.0-incubating
     */
    id {
        @Override
        override fun getAccessor(): String {
            return ID
        }

        @Override
        override fun apply(element: Element): Object {
            return element.id()
        }
    },

    /**
     * Key (representing Property.key())
     *
     * @since 3.0.0-incubating
     */
    key {
        @Override
        override fun getAccessor(): String {
            return KEY
        }

        @Override
        override fun apply(element: Element): String {
            return (element as VertexProperty).key()
        }
    },

    /**
     * Value (representing Property.value())
     *
     * @since 3.0.0-incubating
     */
    {
        @Override
        fun getAccessor(): String {
            return VALUE
        }

        @Override
        fun apply(element: Element): Object {
            return (element as VertexProperty).value()
        }
    }

    abstract val accessor: String?
    @Override
    abstract fun apply(element: Element?): Object?

    companion object {
        private val LABEL: String = Graph.Hidden.hide("label")
        private val ID: String = Graph.Hidden.hide("id")
        private val KEY: String = Graph.Hidden.hide("key")
        private val VALUE: String = Graph.Hidden.hide("value")
        fun fromString(accessor: String): T {
            return if (accessor.equals(LABEL)) label else if (accessor.equals(ID)) id else if (accessor.equals(
                    KEY
                )
            ) key else if (accessor.equals(VALUE)) T.value else throw IllegalArgumentException("The following token string is unknown: $accessor")
        }
    }
}