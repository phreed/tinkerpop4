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
package org.apache.tinkerpop4.gremlin.structure.util

import org.apache.tinkerpop4.gremlin.structure.Edge
import org.apache.tinkerpop4.gremlin.structure.Element
import org.apache.tinkerpop4.gremlin.structure.Property
import org.apache.tinkerpop4.gremlin.structure.Vertex
import java.util.Comparator
import java.util.Map

/**
 * A collection of commonly used [Comparator] instances.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
object Comparators {
    /**
     * Sorts [Element] objects  by the `toString()` value of [Element.id] using
     * [String.CASE_INSENSITIVE_ORDER].
     */
    val ELEMENT_COMPARATOR: Comparator<Element> =
        Comparator.comparing({ e -> e.id().toString() }, String.CASE_INSENSITIVE_ORDER)

    /**
     * Sorts [Vertex] objects  by the `toString()` value of [Vertex.id] using
     * [String.CASE_INSENSITIVE_ORDER].
     */
    val VERTEX_COMPARATOR: Comparator<Vertex> =
        Comparator.comparing({ e -> e.id().toString() }, String.CASE_INSENSITIVE_ORDER)

    /**
     * Sorts [Edge] objects  by the `toString()` value of [Edge.id] using
     * [String.CASE_INSENSITIVE_ORDER].
     */
    val EDGE_COMPARATOR: Comparator<Edge> =
        Comparator.comparing({ e -> e.id().toString() }, String.CASE_INSENSITIVE_ORDER)

    /**
     * Sorts [Property] objects  by the value of [Property.key] using
     * [String.CASE_INSENSITIVE_ORDER].
     */
    val PROPERTY_COMPARATOR: Comparator<Property> = Comparator.comparing(Property::key, String.CASE_INSENSITIVE_ORDER)
}