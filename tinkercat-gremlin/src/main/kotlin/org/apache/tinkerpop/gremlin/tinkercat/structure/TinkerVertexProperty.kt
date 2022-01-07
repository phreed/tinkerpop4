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

import org.apache.tinkerpop.gremlin.structure.Property
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerVertex.graph
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerCat.IdManager.getNextId
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerElement.Companion.elementAlreadyRemoved
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerHelper.removeIndex
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerVertex.properties
import org.apache.tinkerpop.gremlin.structure.VertexProperty
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerCat
import java.lang.IllegalArgumentException
import org.apache.tinkerpop.gremlin.structure.util.ElementHelper
import org.apache.tinkerpop.gremlin.structure.util.StringFactory
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.stream.Collectors

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
open class TinkerVertexProperty<V>(
    id: Any?,
    vertex: TinkerVertex,
    key: String,
    value: V?,
    vararg propertyKeyValues: Any?
) : TinkerElement(
    id!!, key
), VertexProperty<V> {
    protected var properties: MutableMap<String, Property<*>>? = null
    private val vertex: TinkerVertex
    private val key: String
    private val value: V
    private val allowNullPropertyValues: Boolean

    /**
     * This constructor will not validate the ID type against the [Graph].  It will always just use a
     * `Long` for its identifier.  This is useful for constructing a [VertexProperty] for usage
     * with [TinkerCatComputerView].
     */
    constructor(vertex: TinkerVertex, key: String, value: V, vararg propertyKeyValues: Any?) : this(
        (vertex.graph() as TinkerCat).vertexPropertyIdManager.getNextId(
            (vertex.graph() as TinkerCat)
        ), vertex, key, value, *propertyKeyValues
    ) {
    }

    /**
     * Use this constructor to construct [VertexProperty] instances for [TinkerCat] where the `id`
     * can be explicitly set and validated against the expected data type.
     */
    init {
        allowNullPropertyValues = vertex.graph().features().vertex().properties().supportsNullPropertyValues()
        require(!(!allowNullPropertyValues && null == value)) { "value cannot be null as feature supportsNullPropertyValues is false" }
        this.vertex = vertex
        this.key = key
        this.value = value
        ElementHelper.legalPropertyKeyValueArray(*propertyKeyValues)
        ElementHelper.attachProperties(this, *propertyKeyValues)
    }

    override fun key(): String {
        return key
    }

    override fun value(): V {
        return value
    }

    override fun isPresent(): Boolean {
        return true
    }

    override fun toString(): String {
        return StringFactory.propertyString(this)
    }

    override fun id(): Any {
        return id
    }

    override fun equals(`object`: Any?): Boolean {
        return ElementHelper.areEqual(this, `object`)
    }

    override fun keys(): Set<String> {
        return if (null == properties) emptySet() else properties!!.keys
    }

    override fun <U> property(key: String): Property<U> {
        return if (null == properties) Property.empty() else properties!!.getOrDefault(key, Property.empty<U>())
    }

    override fun <U> property(key: String, value: U): Property<U> {
        if (removed) throw elementAlreadyRemoved(
            VertexProperty::class.java, id
        )
        if (!allowNullPropertyValues && null == value) {
            properties<Any?>(key).forEachRemaining { obj: Property<Any?> -> obj.remove() }
            return Property.empty()
        }
        val property: Property<U> = TinkerProperty(this, key, value)
        if (properties == null) properties = HashMap()
        properties!![key] = property
        return property
    }

    override fun element(): Vertex {
        return vertex
    }

    override fun remove() {
        if (null != vertex.properties && vertex.properties!!.containsKey(key)) {
            vertex.properties!![key]!!.remove(this)
            if (vertex.properties!![key]!!.size == 0) {
                vertex.properties!!.remove(key)
                removeIndex(vertex, key, value)
            }
            val delete = AtomicBoolean(true)
            vertex.properties<Any?>(key).forEachRemaining { property: VertexProperty<Any?> ->
                val currentPropertyValue = property.value()
                if (currentPropertyValue != null && currentPropertyValue == value || null == currentPropertyValue && null == value) delete.set(
                    false
                )
            }
            if (delete.get()) removeIndex(vertex, key, value)
            properties = null
            removed = true
        }
    }

    override fun <U> properties(vararg propertyKeys: String): Iterator<Property<U>> {
        if (null == properties) return Collections.emptyIterator()
        return if (propertyKeys.size == 1) {
            val property: Property<U>? = properties!![propertyKeys[0]]
            if (null == property) Collections.emptyIterator() else IteratorUtils.of(property)
        } else properties!!.entries.stream().filter { (key1): Map.Entry<String, Property<*>> ->
            ElementHelper.keyExists(
                key1, *propertyKeys
            )
        }
            .map { (_, value1): Map.Entry<String, Property<*>> -> value1 }
            .collect(Collectors.toList()).iterator()
    }
}