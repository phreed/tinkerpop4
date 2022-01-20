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
package org.apache.tinkerpop4.gremlin.structure.util.reference

import org.apache.tinkerpop4.gremlin.structure.Direction
import org.apache.tinkerpop4.gremlin.structure.Edge
import org.apache.tinkerpop4.gremlin.structure.Element
import org.apache.tinkerpop4.gremlin.structure.Graph
import org.apache.tinkerpop4.gremlin.structure.Vertex
import org.apache.tinkerpop4.gremlin.structure.VertexProperty
import org.apache.tinkerpop4.gremlin.structure.util.StringFactory
import java.util.Collections
import java.util.Iterator

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class ReferenceVertex : ReferenceElement<Vertex?>, Vertex {
    private constructor() {}
    constructor(id: Object?, label: String?) : super(id, label) {}
    constructor(vertex: Vertex?) : super(vertex) {}

    @Override
    fun addEdge(label: String?, inVertex: Vertex?, vararg keyValues: Object?): Edge {
        throw Vertex.Exceptions.edgeAdditionsNotSupported()
    }

    @Override
    fun <V> property(key: String?, value: V, vararg keyValues: Object?): VertexProperty<V> {
        throw Element.Exceptions.propertyAdditionNotSupported()
    }

    @Override
    fun <V> property(
        cardinality: VertexProperty.Cardinality?,
        key: String?,
        value: V,
        vararg keyValues: Object?
    ): VertexProperty<V> {
        throw Element.Exceptions.propertyAdditionNotSupported()
    }

    @Override
    fun edges(direction: Direction?, vararg edgeLabels: String?): Iterator<Edge> {
        return Collections.emptyIterator()
    }

    @Override
    fun vertices(direction: Direction?, vararg edgeLabels: String?): Iterator<Vertex> {
        return Collections.emptyIterator()
    }

    @Override
    fun <V> properties(vararg propertyKeys: String?): Iterator<VertexProperty<V>> {
        return Collections.emptyIterator()
    }

    @Override
    fun remove() {
        throw Vertex.Exceptions.vertexRemovalNotSupported()
    }

    @Override
    override fun toString(): String {
        return StringFactory.vertexString(this)
    }
}