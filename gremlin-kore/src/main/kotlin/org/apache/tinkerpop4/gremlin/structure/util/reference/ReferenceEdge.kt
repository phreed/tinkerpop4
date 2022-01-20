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
import org.apache.tinkerpop4.gremlin.structure.Property
import org.apache.tinkerpop4.gremlin.structure.Vertex
import org.apache.tinkerpop4.gremlin.structure.util.StringFactory
import org.apache.tinkerpop4.gremlin.util.iterator.IteratorUtils
import java.util.Collections
import java.util.Iterator

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class ReferenceEdge : ReferenceElement<Edge?>, Edge {
    private var inVertex: ReferenceVertex? = null
    private var outVertex: ReferenceVertex? = null

    private constructor() {}
    constructor(edge: Edge) : super(edge) {
        inVertex = ReferenceVertex(edge.inVertex())
        outVertex = ReferenceVertex(edge.outVertex())
    }

    constructor(id: Object?, label: String?, inVertex: ReferenceVertex?, outVertex: ReferenceVertex?) : super(
        id,
        label
    ) {
        this.inVertex = inVertex
        this.outVertex = outVertex
    }

    @Override
    fun <V> property(key: String?, value: V): Property<V> {
        throw Element.Exceptions.propertyAdditionNotSupported()
    }

    @Override
    fun remove() {
        throw Edge.Exceptions.edgeRemovalNotSupported()
    }

    @Override
    fun vertices(direction: Direction): Iterator<Vertex> {
        return if (direction.equals(Direction.OUT)) IteratorUtils.of(outVertex) else if (direction.equals(Direction.IN)) IteratorUtils.of(
            inVertex
        ) else IteratorUtils.of(
            outVertex,
            inVertex
        )
    }

    @Override
    fun inVertex(): Vertex? {
        return inVertex
    }

    @Override
    fun outVertex(): Vertex? {
        return outVertex
    }

    @Override
    fun <V> properties(vararg propertyKeys: String?): Iterator<Property<V>> {
        return Collections.emptyIterator()
    }

    @Override
    override fun toString(): String {
        return StringFactory.edgeString(this)
    }
}