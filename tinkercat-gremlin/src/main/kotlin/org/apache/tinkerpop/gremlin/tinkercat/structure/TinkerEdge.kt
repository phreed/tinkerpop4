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
package org.apache.tinkerpop.gremlin.tinkercat.structure

import org.apache.tinkerpop.gremlin.structure.*
import org.apache.tinkerpop.gremlin.structure.util.ElementHelper
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerCat
import org.apache.tinkerpop.gremlin.structure.util.StringFactory
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils
import java.util.*
import java.util.stream.Collectors

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class TinkerEdge(id: Any?, val outVertex: Vertex, label: String?, val inVertex: Vertex) : TinkerElement(id, label),
    Edge {
    var properties: MutableMap<String, Property<*>>? = null
    private val allowNullPropertyValues: Boolean

    init {
        allowNullPropertyValues = outVertex.graph().features().edge().supportsNullPropertyValues()
        TinkerHelper.autoUpdateIndex(this, T.label.accessor, this.label, null)
    }

    override fun <V> property(key: String, value: V): Property<V> {
        if (removed) throw elementAlreadyRemoved(Edge::class.java, id)
        ElementHelper.validateProperty(key, value)
        if (!allowNullPropertyValues && null == value) {
            properties<Any?>(key).forEachRemaining { obj: Property<Any?> -> obj.remove() }
            return Property.empty()
        }
        val oldProperty: Property<*> = super.property<Any>(key)
        val newProperty: Property<V> = TinkerProperty(this, key, value)
        if (null == properties) properties = HashMap()
        properties!![key] = newProperty
        TinkerHelper.autoUpdateIndex(this, key, value, if (oldProperty.isPresent) oldProperty.value() else null)
        return newProperty
    }

    override fun <V> property(key: String): Property<V> {
        return if (null == properties) Property.empty() else properties!!.getOrDefault(key, Property.empty<V>())
    }

    override fun keys(): Set<String> {
        return if (null == properties) emptySet() else properties!!.keys
    }

    override fun remove() {
        val outVertex = outVertex as TinkerVertex
        val inVertex = inVertex as TinkerVertex
        if (null != outVertex && null != outVertex.outEdges) {
            val edges = outVertex.outEdges[label()]
            edges?.remove(this)
        }
        if (null != inVertex && null != inVertex.inEdges) {
            val edges = inVertex.inEdges[label()]
            edges?.remove(this)
        }
        TinkerHelper.removeElementIndex(this)
        (graph() as TinkerCat).edges.remove(id())
        properties = null
        removed = true
    }

    override fun toString(): String {
        return StringFactory.edgeString(this)
    }

    override fun outVertex(): Vertex {
        return outVertex
    }

    override fun inVertex(): Vertex {
        return inVertex
    }

    override fun vertices(direction: Direction): Iterator<Vertex> {
        return if (removed) Collections.emptyIterator() else when (direction) {
            Direction.OUT -> IteratorUtils.of(
                outVertex
            )
            Direction.IN -> IteratorUtils.of(
                inVertex
            )
            else -> IteratorUtils.of(
                outVertex,
                inVertex
            )
        }
    }

    override fun graph(): Graph {
        return inVertex.graph()
    }

    override fun <V> properties(vararg propertyKeys: String): Iterator<Property<V>> {
        if (null == properties) return Collections.emptyIterator()
        return if (propertyKeys.size == 1) {
            val property: Property<V>? = properties!![propertyKeys[0]]
            if (null == property) Collections.emptyIterator() else IteratorUtils.of(property)
        } else properties!!.entries.stream().filter { (key): Map.Entry<String, Property<*>> ->
            ElementHelper.keyExists(
                key, *propertyKeys
            )
        }
            .map { (_, value): Map.Entry<String, Property<*>> -> value }
            .collect(Collectors.toList()).iterator()
    }
}