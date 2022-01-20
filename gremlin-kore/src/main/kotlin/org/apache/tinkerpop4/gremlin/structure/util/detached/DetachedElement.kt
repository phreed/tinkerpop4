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
package org.apache.tinkerpop4.gremlin.structure.util.detached

import org.apache.tinkerpop4.gremlin.structure.Element
import org.apache.tinkerpop4.gremlin.structure.Graph
import org.apache.tinkerpop4.gremlin.structure.Property
import org.apache.tinkerpop4.gremlin.structure.Vertex
import org.apache.tinkerpop4.gremlin.structure.util.Attachable
import org.apache.tinkerpop4.gremlin.structure.util.ElementHelper
import org.apache.tinkerpop4.gremlin.structure.util.empty.EmptyGraph
import java.io.Serializable
import java.util.Collections
import java.util.Iterator
import java.util.List
import java.util.Map

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
abstract class DetachedElement<E> : Element, Serializable, Attachable<E> {
    protected var id: Object? = null
    protected var label: String? = null
    protected var properties: Map<String, List<Property>>? = null

    protected constructor() {}
    protected constructor(element: Element) {
        id = element.id()
        try {
            label = element.label()
        } catch (e: UnsupportedOperationException) {   // ghetto.
            label = Vertex.DEFAULT_LABEL
        }
    }

    protected constructor(id: Object?, label: String?) {
        this.id = id
        this.label = label
    }

    @Override
    fun graph(): Graph {
        return EmptyGraph.instance()
    }

    @Override
    fun id(): Object? {
        return id
    }

    @Override
    fun label(): String? {
        return label
    }

    @Override
    fun <V> property(key: String?, value: V): Property<V> {
        throw Element.Exceptions.propertyAdditionNotSupported()
    }

    @Override
    fun <V> property(key: String): Property<V> {
        return if (null != properties && properties!!.containsKey(key)) properties!![key]!![0] else Property.empty()
    }

    @Override
    override fun hashCode(): Int {
        return ElementHelper.hashCode(this)
    }

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
    override fun equals(`object`: Object?): Boolean {
        return ElementHelper.areEqual(this, `object`)
    }

    @Override
    fun <V> properties(vararg propertyKeys: String?): Iterator<Property<V?>?> {
        return if (null == properties) Collections.emptyIterator() else (properties.entrySet().stream()
            .filter { entry -> ElementHelper.keyExists(entry.getKey(), propertyKeys) }
            .flatMap { entry -> entry.getValue().stream() }.iterator() as Iterator)
    }

    fun get(): E {
        return this as E
    }

    abstract fun internalAddProperty(p: Property?)
}