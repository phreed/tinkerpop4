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

import org.apache.tinkerpop4.gremlin.structure.Property

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class DetachedVertexProperty<V> : DetachedElement<VertexProperty<V>?>, VertexProperty<V> {
    protected var value: V? = null

    @kotlin.jvm.Transient
    protected var vertex: DetachedVertex? = null

    private constructor() {}
    protected constructor(vertexProperty: VertexProperty<V>, withProperties: Boolean) : super(vertexProperty) {
        value = vertexProperty.value()
        vertex = DetachedFactory.detach(vertexProperty.element(), false)

        // only serialize properties if requested, the graph supports it and there are meta properties present.
        // this prevents unnecessary object creation of a new HashMap which will just be empty.  it will use
        // Collections.emptyMap() by default
        if (withProperties && vertexProperty.graph().features().vertex().supportsMetaProperties()) {
            val propertyIterator: Iterator<Property<Object>> = vertexProperty.properties()
            if (propertyIterator.hasNext()) {
                this.properties = HashMap()
                propertyIterator.forEachRemaining { property ->
                    this.properties.put(
                        property.key(),
                        Collections.singletonList(DetachedFactory.detach(property))
                    )
                }
            }
        }
    }

    constructor(
        id: Object?, label: String?, value: V,
        properties: Map<String?, Object?>?,
        vertex: Vertex?
    ) : super(id, label) {
        this.value = value
        this.vertex = DetachedFactory.detach(vertex, true)
        if (null != properties && !properties.isEmpty()) {
            properties = HashMap()
            properties.entrySet().iterator().forEachRemaining { entry ->
                properties.put(
                    entry.getKey(),
                    Collections.singletonList(DetachedProperty(entry.getKey(), entry.getValue(), this))
                )
            }
        }
    }

    /**
     * This constructor is used by GraphSON when deserializing and the [Host] is not known.
     */
    constructor(
        id: Object?, label: String?, value: V,
        properties: Map<String?, Object?>?
    ) : super(id, label) {
        this.value = value
        if (null != properties && !properties.isEmpty()) {
            properties = HashMap()
            properties.entrySet().iterator().forEachRemaining { entry ->
                properties.put(
                    entry.getKey(),
                    Collections.singletonList(DetachedProperty(entry.getKey(), entry.getValue(), this))
                )
            }
        }
    }

    @get:Override
    val isPresent: Boolean
        get() = true

    @Override
    fun key(): String {
        return this.label
    }

    @Override
    fun value(): V {
        return value
    }

    @Override
    fun element(): Vertex? {
        return vertex
    }

    @Override
    fun remove() {
        throw Property.Exceptions.propertyRemovalNotSupported()
    }

    @Override
    override fun toString(): String {
        return StringFactory.propertyString(this)
    }

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
    override fun equals(`object`: Object?): Boolean {
        return ElementHelper.areEqual(this, `object`)
    }

    @Override
    fun <U> properties(vararg propertyKeys: String?): Iterator<Property<U>>? {
        return super.properties(propertyKeys) as Iterator?
    }

    @Override
    fun internalAddProperty(p: Property) {
        if (null == properties) properties = HashMap()
        this.properties.put(p.key(), Collections.singletonList(p))
    }

    fun internalSetVertex(vertex: DetachedVertex?) {
        this.vertex = vertex
    }

    class Builder(private val vp: DetachedVertexProperty<*>) {
        fun setV(v: DetachedVertex?): Builder {
            vp.internalSetVertex(v)
            return this
        }

        fun addProperty(p: Property?): Builder {
            vp.internalAddProperty(p)
            return this
        }

        fun setId(id: Object): Builder {
            vp.id = id
            return this
        }

        fun setLabel(label: String): Builder {
            vp.label = label
            return this
        }

        fun setValue(value: Object?): Builder {
            vp.value = value
            return this
        }

        fun create(): DetachedVertexProperty<*> {
            return vp
        }
    }

    companion object {
        /**
         * Provides a way to construct an immutable [DetachedEdge].
         */
        fun build(): Builder {
            return Builder(DetachedVertexProperty<Any?>())
        }
    }
}