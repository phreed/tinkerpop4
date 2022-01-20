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
 * Represents an [Edge] that is disconnected from a [Graph].  "Disconnection" can mean detachment from
 * a [Graph] in the sense that the [Edge] was constructed from a [Graph] instance and this reference
 * was removed or it can mean that the `DetachedEdge` could have been constructed independently of a
 * [Graph] instance in the first place.
 *
 *
 * A `DetachedEdge` only has reference to the properties and in/out vertices that are associated with it at the
 * time of detachment (or construction) and is not traversable or mutable.  Note that the references to the in/out
 * vertices are [DetachedVertex] instances that only have reference to the
 * [org.apache.tinkerpop4.gremlin.structure.Vertex.id] and [org.apache.tinkerpop4.gremlin.structure.Vertex.label].
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class DetachedEdge : DetachedElement<Edge?>, Edge {
    private var outVertex: DetachedVertex? = null
    private var inVertex: DetachedVertex? = null

    private constructor() {}
    protected constructor(edge: Edge, withProperties: Boolean) : super(edge) {
        outVertex = DetachedFactory.detach(edge.outVertex(), false)
        inVertex = DetachedFactory.detach(edge.inVertex(), false)

        // only serialize properties if requested, the graph supports it and there are meta properties present.
        // this prevents unnecessary object creation of a new HashMap of a new HashMap which will just be empty.
        // it will use Collections.emptyMap() by default
        if (withProperties) {
            val propertyIterator: Iterator<Property<Object>> = edge.properties()
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
        id: Object?, label: String?,
        properties: Map<String?, Object?>?,
        outVId: Object?, outVLabel: String?,
        inVId: Object?, inVLabel: String?
    ) : super(id, label) {
        outVertex = DetachedVertex(outVId, outVLabel, Collections.emptyMap())
        inVertex = DetachedVertex(inVId, inVLabel, Collections.emptyMap())
        if (properties != null && !properties.isEmpty()) {
            properties = HashMap()
            properties.entrySet().iterator().forEachRemaining { entry ->
                if (Property::class.java.isAssignableFrom(entry.getValue().getClass())) {
                    properties.put(entry.getKey(), Collections.singletonList(entry.getValue() as Property))
                } else {
                    properties.put(
                        entry.getKey(),
                        Collections.singletonList(DetachedProperty(entry.getKey(), entry.getValue(), this))
                    )
                }
            }
        }
    }

    @Override
    override fun toString(): String {
        return StringFactory.edgeString(this)
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
    fun vertices(direction: Direction?): Iterator<Vertex> {
        return when (direction) {
            OUT -> IteratorUtils.of(outVertex)
            IN -> IteratorUtils.of(inVertex)
            else -> IteratorUtils.of(outVertex, inVertex)
        }
    }

    @Override
    fun remove() {
        throw Edge.Exceptions.edgeRemovalNotSupported()
    }

    @Override
    fun <V> properties(vararg propertyKeys: String?): Iterator<Property<V>>? {
        return super.properties(propertyKeys) as Iterator?
    }

    @Override
    fun internalAddProperty(p: Property) {
        if (null == properties) properties = HashMap()
        this.properties.put(p.key(), Collections.singletonList(p))
    }

    class Builder(private val e: DetachedEdge) {
        fun addProperty(p: Property): Builder {
            e.internalAddProperty(p)
            return this
        }

        fun setId(id: Object): Builder {
            e.id = id
            return this
        }

        fun setLabel(label: String): Builder {
            e.label = label
            return this
        }

        fun setOutV(v: DetachedVertex?): Builder {
            e.outVertex = v
            return this
        }

        fun setInV(v: DetachedVertex?): Builder {
            e.inVertex = v
            return this
        }

        fun create(): DetachedEdge {
            return e
        }
    }

    companion object {
        /**
         * Provides a way to construct an immutable [DetachedEdge].
         */
        fun build(): Builder {
            return Builder(DetachedEdge())
        }
    }
}