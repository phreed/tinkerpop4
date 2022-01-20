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

import org.apache.tinkerpop4.gremlin.structure.Element

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class ReferenceVertexProperty<V> : ReferenceElement<VertexProperty<V>?>, VertexProperty<V> {
    private var vertex: ReferenceVertex? = null
    private var value: V? = null

    private constructor() {}
    constructor(vertexProperty: VertexProperty<V>) : super(vertexProperty) {
        vertex = if (null == vertexProperty.element()) null else ReferenceFactory.detach(vertexProperty.element())
        value = vertexProperty.value()
    }

    constructor(id: Object, label: String, value: V) {
        id = id
        label = label
        this.value = value
        vertex = null
    }

    @Override
    override fun toString(): String {
        return StringFactory.propertyString(this)
    }

    @Override
    fun key(): String {
        return this.label
    }

    @Override
    fun label(): String {
        return this.label
    }

    @Override
    @Throws(NoSuchElementException::class)
    fun value(): V {
        return value
    }

    @get:Override
    val isPresent: Boolean
        get() = true

    @Override
    fun element(): Vertex? {
        return vertex
    }

    @Override
    fun <U> property(key: String?, value: U): Property<U> {
        throw Element.Exceptions.propertyAdditionNotSupported()
    }

    @Override
    fun remove() {
        throw Property.Exceptions.propertyRemovalNotSupported()
    }

    @Override
    fun <U> properties(vararg propertyKeys: String?): Iterator<Property<U>> {
        return Collections.emptyIterator()
    }
}