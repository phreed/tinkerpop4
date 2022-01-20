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

import org.apache.tinkerpop4.gremlin.structure.Direction

/**
 * Represents a [Vertex] that is disconnected from a [Graph].  "Disconnection" can mean detachment from
 * a [Graph] in the sense that a [Vertex] was constructed from a [Graph] instance and this reference
 * was removed or it can mean that the `DetachedVertex` could have been constructed independently of a
 * [Graph] instance in the first place.
 *
 *
 * A `DetachedVertex` only has reference to the properties that are associated with it at the time of detachment
 * (or construction) and is not traversable or mutable.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class DetachedVertex : DetachedElement<Vertex?>, Vertex {
    private constructor() {}
    protected constructor(vertex: Vertex, withProperties: Boolean) : super(vertex) {

        // only serialize properties if requested, and there are meta properties present. this prevents unnecessary
        // object creation of a new HashMap of a new HashMap which will just be empty.  it will use
        // Collections.emptyMap() by default
        if (withProperties) {
            val propertyIterator: Iterator<VertexProperty<Object>> = vertex.properties()
            if (propertyIterator.hasNext()) {
                this.properties = HashMap()
                propertyIterator.forEachRemaining { property ->
                    val list: List<Property> = this.properties.getOrDefault(property.key(), ArrayList())
                    list.add(DetachedFactory.detach(property, true))
                    this.properties.put(property.key(), list)
                }
            }
        }
    }

    constructor(id: Object?, label: String?, properties: Map<String?, Object?>?) : super(id, label) {
        if (properties != null && !properties.isEmpty()) {
            properties = HashMap()
            properties.entrySet().iterator().forEachRemaining { entry ->
                properties.put(entry.getKey(), IteratorUtils.< Property > list < Property ? > IteratorUtils.map(
                    (entry.getValue() as List<Object?>).iterator()
                ) { m ->
                    if (VertexProperty::class.java.isAssignableFrom(m.getClass())) m as VertexProperty else DetachedVertexProperty(
                        (m as Map).get(
                            ID
                        ),
                        entry.getKey(),
                        (m as Map).get(VALUE),
                        (m as Map).getOrDefault(PROPERTIES, HashMap()) as Map<String?, Object?>?,
                        this
                    )
                })
            }
        }
    }

    @Override
    fun <V> property(key: String?, value: V): VertexProperty<V> {
        throw Element.Exceptions.propertyAdditionNotSupported()
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
    fun <V> property(key: String?): VertexProperty<V> {
        return if (null != this.properties && this.properties.containsKey(key)) {
            val list: List<VertexProperty> = this.properties.get(key) as List
            if (list.size() > 1) throw Vertex.Exceptions.multiplePropertiesExistForProvidedKey(key) else list[0]
        } else VertexProperty.< V > empty < V ? > ()
    }

    @Override
    fun addEdge(label: String?, inVertex: Vertex?, vararg keyValues: Object?): Edge {
        throw Vertex.Exceptions.edgeAdditionsNotSupported()
    }

    @Override
    override fun toString(): String {
        return StringFactory.vertexString(this)
    }

    @Override
    fun <V> properties(vararg propertyKeys: String?): Iterator<VertexProperty<V>>? {
        return super.properties(propertyKeys) as Iterator?
    }

    @Override
    fun edges(direction: Direction?, vararg edgeLabels: String?): Iterator<Edge> {
        return Collections.emptyIterator()
    }

    @Override
    fun vertices(direction: Direction?, vararg labels: String?): Iterator<Vertex> {
        return Collections.emptyIterator()
    }

    @Override
    fun remove() {
        throw Vertex.Exceptions.vertexRemovalNotSupported()
    }

    @Override
    fun internalAddProperty(p: Property) {
        if (null == properties) properties = HashMap()
        if (!properties.containsKey(p.key())) properties.put(p.key(), ArrayList())
        this.properties.get(p.key()).add(p)
    }

    class Builder(private val v: DetachedVertex) {
        fun addProperty(vp: DetachedVertexProperty): Builder {
            v.internalAddProperty(vp)
            vp.internalSetVertex(v)
            return this
        }

        fun setId(id: Object): Builder {
            v.id = id
            return this
        }

        fun setLabel(label: String): Builder {
            v.label = label
            return this
        }

        fun create(): DetachedVertex {
            return v
        }
    }

    companion object {
        private const val ID = "id"
        private const val VALUE = "value"
        private const val PROPERTIES = "properties"

        /**
         * Provides a way to construct an immutable [DetachedVertex].
         */
        fun build(): Builder {
            return Builder(DetachedVertex())
        }
    }
}