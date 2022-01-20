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

import org.apache.tinkerpop4.gremlin.structure.Direction

/**
 * An interface that provides methods for detached properties and elements to be re-attached to the [Graph].
 * There are two general ways in which they can be attached: [Method.get] or [Method.create].
 * A [Method.get] will find the property/element at the host location and return it.
 * A [Method.create] will create the property/element at the host location and return it.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
interface Attachable<V> {
    /**
     * Get the raw object trying to be attached.
     *
     * @return the raw object to attach
     */
    fun get(): V

    /**
     * Provide a way to attach an [Attachable] implementation to a host.  Note that the context of the host
     * is not defined by way of the attachment method itself that is supplied as an argument.  It is up to the
     * implementer to supply that context.
     *
     * @param method a [Function] that takes an [Attachable] and returns the "re-attached" object
     * @return the return value of the `method`
     * @throws IllegalStateException if the [Attachable] is not a "graph" object (i.e. host or
     * attachable don't work together)
     */
    @Throws(IllegalStateException::class)
    fun attach(method: Function<Attachable<V>?, V>): V {
        return method.apply(this)
    }

    /**
     * A collection of general methods of attachment. Note that more efficient methods of attachment might exist
     * if the user knows the source data being attached and the features of the graph that the data is being
     * attached to.
     */
    object Method {
        operator fun <V> get(hostVertexOrGraph: Host): Function<Attachable<V>, V> {
            return if (hostVertexOrGraph is EmptyGraph) Function<Attachable<V>, V> { obj: Attachable<*> -> obj.get() } else label@ Function<Attachable<V>, V> { attachable: Attachable<V> ->
                val base: Object = attachable.get()
                if (base is Vertex) {
                    val optional: Optional<Vertex> = if (hostVertexOrGraph is Graph) getVertex(
                        attachable as Attachable<Vertex>,
                        hostVertexOrGraph as Graph
                    ) else getVertex(attachable as Attachable<Vertex>, hostVertexOrGraph as Vertex)
                    return@label optional.orElseThrow {
                        if (hostVertexOrGraph is Graph) Exceptions.canNotGetAttachableFromHostGraph(
                            attachable,
                            hostVertexOrGraph as Graph
                        ) else Exceptions.canNotGetAttachableFromHostVertex(attachable, hostVertexOrGraph as Vertex)
                    }
                } else if (base is Edge) {
                    val optional: Optional<Edge> = if (hostVertexOrGraph is Graph) getEdge(
                        attachable as Attachable<Edge>,
                        hostVertexOrGraph as Graph
                    ) else getEdge(attachable as Attachable<Edge>, hostVertexOrGraph as Vertex)
                    return@label optional.orElseThrow {
                        if (hostVertexOrGraph is Graph) Exceptions.canNotGetAttachableFromHostGraph(
                            attachable,
                            hostVertexOrGraph as Graph
                        ) else Exceptions.canNotGetAttachableFromHostVertex(attachable, hostVertexOrGraph as Vertex)
                    }
                } else if (base is VertexProperty) {
                    val optional: Optional<VertexProperty> = if (hostVertexOrGraph is Graph) getVertexProperty(
                        attachable as Attachable<VertexProperty>,
                        hostVertexOrGraph as Graph
                    ) else getVertexProperty(attachable as Attachable<VertexProperty>, hostVertexOrGraph as Vertex)
                    return@label optional.orElseThrow {
                        if (hostVertexOrGraph is Graph) Exceptions.canNotGetAttachableFromHostGraph(
                            attachable,
                            hostVertexOrGraph as Graph
                        ) else Exceptions.canNotGetAttachableFromHostVertex(attachable, hostVertexOrGraph as Vertex)
                    }
                } else if (base is Property) {
                    val optional: Optional<Property> = if (hostVertexOrGraph is Graph) getProperty(
                        attachable as Attachable<Property>,
                        hostVertexOrGraph as Graph
                    ) else getProperty(attachable as Attachable<Property>, hostVertexOrGraph as Vertex)
                    return@label optional.orElseThrow {
                        if (hostVertexOrGraph is Graph) Exceptions.canNotGetAttachableFromHostGraph(
                            attachable,
                            hostVertexOrGraph as Graph
                        ) else Exceptions.canNotGetAttachableFromHostVertex(attachable, hostVertexOrGraph as Vertex)
                    }
                } else throw Exceptions.providedAttachableMustContainAGraphObject(attachable)
            }
        }

        fun <V> getOrCreate(hostVertexOrGraph: Host?): Function<Attachable<V>, V> {
            return label@ Function<Attachable<V>, V> { attachable: Attachable<V> ->
                val base: Object = attachable.get()
                if (base is Vertex) {
                    return@label (if (hostVertexOrGraph is Graph) getVertex(
                        attachable as Attachable<Vertex>,
                        hostVertexOrGraph as Graph?
                    ) else getVertex(attachable as Attachable<Vertex>, hostVertexOrGraph as Vertex?))
                        .orElseGet {
                            if (hostVertexOrGraph is Graph) createVertex(
                                attachable as Attachable<Vertex>,
                                hostVertexOrGraph as Graph?
                            ) else createVertex(attachable as Attachable<Vertex>, hostVertexOrGraph as Vertex?)
                        }
                } else if (base is Edge) {
                    return@label (if (hostVertexOrGraph is Graph) getEdge(
                        attachable as Attachable<Edge>,
                        hostVertexOrGraph as Graph?
                    ) else getEdge(attachable as Attachable<Edge>, hostVertexOrGraph as Vertex?))
                        .orElseGet {
                            if (hostVertexOrGraph is Graph) createEdge(
                                attachable as Attachable<Edge>,
                                hostVertexOrGraph as Graph?
                            ) else createEdge(attachable as Attachable<Edge>, hostVertexOrGraph as Vertex?)
                        }
                } else if (base is VertexProperty) {
                    return@label (if (hostVertexOrGraph is Graph) getVertexProperty(
                        attachable as Attachable<VertexProperty>,
                        hostVertexOrGraph as Graph?
                    ) else getVertexProperty(attachable as Attachable<VertexProperty>, hostVertexOrGraph as Vertex?))
                        .orElseGet {
                            if (hostVertexOrGraph is Graph) createVertexProperty(
                                attachable as Attachable<VertexProperty>,
                                hostVertexOrGraph as Graph?
                            ) else createVertexProperty(
                                attachable as Attachable<VertexProperty>,
                                hostVertexOrGraph as Vertex?
                            )
                        }
                } else if (base is Property) {
                    return@label (if (hostVertexOrGraph is Graph) getProperty(
                        attachable as Attachable<Property>,
                        hostVertexOrGraph as Graph?
                    ) else getProperty(attachable as Attachable<Property>, hostVertexOrGraph as Vertex?))
                        .orElseGet {
                            if (hostVertexOrGraph is Graph) createProperty(
                                attachable as Attachable<Property>,
                                hostVertexOrGraph as Graph?
                            ) else createProperty(attachable as Attachable<Property>, hostVertexOrGraph as Vertex?)
                        }
                } else throw Exceptions.providedAttachableMustContainAGraphObject(attachable)
            }
        }

        fun <V> create(hostVertexOrGraph: Host?): Function<Attachable<V>, V> {
            return label@ Function<Attachable<V>, V> { attachable: Attachable<V> ->
                val base: Object = attachable.get()
                if (base is Vertex) {
                    return@label if (hostVertexOrGraph is Graph) createVertex(
                        attachable as Attachable<Vertex>,
                        hostVertexOrGraph as Graph?
                    ) else createVertex(attachable as Attachable<Vertex>, hostVertexOrGraph as Vertex?)
                } else if (base is Edge) {
                    return@label if (hostVertexOrGraph is Graph) createEdge(
                        attachable as Attachable<Edge>,
                        hostVertexOrGraph as Graph?
                    ) else createEdge(attachable as Attachable<Edge>, hostVertexOrGraph as Vertex?)
                } else if (base is VertexProperty) {
                    return@label if (hostVertexOrGraph is Graph) createVertexProperty(
                        attachable as Attachable<VertexProperty>,
                        hostVertexOrGraph as Graph?
                    ) as V else createVertexProperty(
                        attachable as Attachable<VertexProperty>,
                        hostVertexOrGraph as Vertex?
                    ) as V
                } else if (base is Property) {
                    return@label if (hostVertexOrGraph is Graph) createProperty(
                        attachable as Attachable<Property>,
                        hostVertexOrGraph as Graph?
                    ) as V else createProperty(attachable as Attachable<Property>, hostVertexOrGraph as Vertex?) as V
                } else throw Exceptions.providedAttachableMustContainAGraphObject(attachable)
            }
        }

        ///////////////////
        ///// GET HELPER METHODS
        fun getVertex(attachableVertex: Attachable<Vertex>, hostGraph: Graph): Optional<Vertex> {
            val vertexIterator: Iterator<Vertex> = hostGraph.vertices(attachableVertex.get().id())
            return if (vertexIterator.hasNext()) Optional.of(vertexIterator.next()) else Optional.empty()
        }

        fun getVertex(attachableVertex: Attachable<Vertex?>, hostVertex: Vertex?): Optional<Vertex> {
            return if (ElementHelper.areEqual(
                    attachableVertex.get(),
                    hostVertex
                )
            ) Optional.of(hostVertex) else Optional.empty()
        }

        fun getEdge(attachableEdge: Attachable<Edge>, hostGraph: Graph): Optional<Edge> {
            val edgeIterator: Iterator<Edge> = hostGraph.edges(attachableEdge.get().id())
            return if (edgeIterator.hasNext()) Optional.of(edgeIterator.next()) else Optional.empty()
        }

        fun getEdge(attachableEdge: Attachable<Edge>, hostVertex: Vertex): Optional<Edge> {
            val baseEdge: Edge = attachableEdge.get()
            val edgeIterator: Iterator<Edge> = hostVertex.edges(Direction.OUT, attachableEdge.get().label())
            while (edgeIterator.hasNext()) {
                val edge: Edge = edgeIterator.next()
                if (ElementHelper.areEqual(edge, baseEdge)) return Optional.of(edge)
            }
            return Optional.empty()
        }

        fun getVertexProperty(
            attachableVertexProperty: Attachable<VertexProperty>,
            hostGraph: Graph
        ): Optional<VertexProperty> {
            val baseVertexProperty: VertexProperty = attachableVertexProperty.get()
            val vertexIterator: Iterator<Vertex> = hostGraph.vertices(baseVertexProperty.element().id())
            if (vertexIterator.hasNext()) {
                val vertexPropertyIterator: Iterator<VertexProperty<Object>> =
                    vertexIterator.next().properties(baseVertexProperty.key())
                while (vertexPropertyIterator.hasNext()) {
                    val vertexProperty: VertexProperty = vertexPropertyIterator.next()
                    if (ElementHelper.areEqual(vertexProperty, baseVertexProperty)) return Optional.of(vertexProperty)
                }
            }
            return Optional.empty()
        }

        fun getVertexProperty(
            attachableVertexProperty: Attachable<VertexProperty>,
            hostVertex: Vertex
        ): Optional<VertexProperty> {
            val baseVertexProperty: VertexProperty = attachableVertexProperty.get()
            val vertexPropertyIterator: Iterator<VertexProperty<Object>> =
                hostVertex.properties(baseVertexProperty.key())
            while (vertexPropertyIterator.hasNext()) {
                val vertexProperty: VertexProperty = vertexPropertyIterator.next()
                if (ElementHelper.areEqual(vertexProperty, baseVertexProperty)) return Optional.of(vertexProperty)
            }
            return Optional.empty()
        }

        fun getProperty(attachableProperty: Attachable<Property>, hostGraph: Graph): Optional<Property> {
            val baseProperty: Property = attachableProperty.get()
            val propertyElement: Element = attachableProperty.get().element()
            return if (propertyElement is Vertex) {
                getVertexProperty(
                    attachableProperty as Attachable<*>,
                    hostGraph
                ) as Optional
            } else if (propertyElement is Edge) {
                val edgeIterator: Iterator<Edge> = hostGraph.edges(propertyElement.id())
                while (edgeIterator.hasNext()) {
                    val property: Property = edgeIterator.next().property(baseProperty.key())
                    if (property.isPresent() && property.value().equals(baseProperty.value())) return Optional.of(
                        property
                    )
                }
                Optional.empty()
            } else { // vertex property
                val vertexIterator: Iterator<Vertex> =
                    hostGraph.vertices((propertyElement as VertexProperty).element().id())
                if (vertexIterator.hasNext()) {
                    val vertexPropertyIterator: Iterator<VertexProperty<Object>> = vertexIterator.next().properties()
                    while (vertexPropertyIterator.hasNext()) {
                        val vertexProperty: VertexProperty = vertexPropertyIterator.next()
                        if (ElementHelper.areEqual(vertexProperty, baseProperty.element())) {
                            val property: Property = vertexProperty.property(baseProperty.key())
                            return if (property.isPresent() && property.value()
                                    .equals(baseProperty.value())
                            ) Optional.of(property) else Optional.empty()
                        }
                    }
                }
                Optional.empty()
            }
        }

        fun getProperty(attachableProperty: Attachable<Property>, hostVertex: Vertex): Optional<Property> {
            val baseProperty: Property = attachableProperty.get()
            val propertyElement: Element = attachableProperty.get().element()
            return if (propertyElement is Vertex) {
                getVertexProperty(
                    attachableProperty as Attachable<*>,
                    hostVertex
                ) as Optional
            } else if (propertyElement is Edge) {
                val edgeIterator: Iterator<Edge> = hostVertex.edges(Direction.OUT)
                while (edgeIterator.hasNext()) {
                    val edge: Edge = edgeIterator.next()
                    if (ElementHelper.areEqual(edge, propertyElement)) {
                        val property: Property = edge.property(baseProperty.key())
                        if (ElementHelper.areEqual(baseProperty, property)) return Optional.of(property)
                    }
                }
                Optional.empty()
            } else { // vertex property
                val vertexPropertyIterator: Iterator<VertexProperty<Object>> = hostVertex.properties()
                while (vertexPropertyIterator.hasNext()) {
                    val vertexProperty: VertexProperty = vertexPropertyIterator.next()
                    if (ElementHelper.areEqual(vertexProperty, baseProperty.element())) {
                        val property: Property = vertexProperty.property(baseProperty.key())
                        return if (property.isPresent() && property.value()
                                .equals(baseProperty.value())
                        ) Optional.of(property) else Optional.empty()
                    }
                }
                Optional.empty()
            }
        }

        ///// CREATE HELPER METHODS
        fun createVertex(attachableVertex: Attachable<Vertex>, hostGraph: Graph): Vertex {
            val baseVertex: Vertex = attachableVertex.get()
            val vertex: Vertex = if (hostGraph.features().vertex().willAllowId(baseVertex.id())) hostGraph.addVertex(
                T.id,
                baseVertex.id(),
                T.label,
                baseVertex.label()
            ) else hostGraph.addVertex(T.label, baseVertex.label())
            baseVertex.properties().forEachRemaining { vp ->
                val vertexProperty: VertexProperty = if (hostGraph.features().vertex().properties()
                        .willAllowId(vp.id())
                ) vertex.property(
                    hostGraph.features().vertex().getCardinality(vp.key()),
                    vp.key(),
                    vp.value(),
                    T.id,
                    vp.id()
                ) else vertex.property(hostGraph.features().vertex().getCardinality(vp.key()), vp.key(), vp.value())
                vp.properties().forEachRemaining { p -> vertexProperty.property(p.key(), p.value()) }
            }
            return vertex
        }

        fun createVertex(attachableVertex: Attachable<Vertex?>?, hostVertex: Vertex?): Vertex {
            throw IllegalStateException("It is not possible to create a vertex at a host vertex")
        }

        fun createEdge(attachableEdge: Attachable<Edge>, hostGraph: Graph): Edge {
            val baseEdge: Edge = attachableEdge.get()
            var vertices: Iterator<Vertex> = hostGraph.vertices(baseEdge.outVertex().id())
            val outV: Vertex = if (vertices.hasNext()) vertices.next() else if (hostGraph.features().vertex()
                    .willAllowId(baseEdge.outVertex().id())
            ) hostGraph.addVertex(T.id, baseEdge.outVertex().id()) else hostGraph.addVertex()
            vertices = hostGraph.vertices(baseEdge.inVertex().id())
            val inV: Vertex = if (vertices.hasNext()) vertices.next() else if (hostGraph.features().vertex()
                    .willAllowId(baseEdge.inVertex().id())
            ) hostGraph.addVertex(T.id, baseEdge.inVertex().id()) else hostGraph.addVertex()
            if (ElementHelper.areEqual(outV, inV)) {
                val itty: Iterator<Edge> = outV.edges(Direction.OUT, baseEdge.label())
                while (itty.hasNext()) {
                    val e: Edge = itty.next()
                    if (ElementHelper.areEqual(baseEdge, e)) return e
                }
            }
            val e: Edge = if (hostGraph.features().edge().willAllowId(baseEdge.id())) outV.addEdge(
                baseEdge.label(),
                inV,
                T.id,
                baseEdge.id()
            ) else outV.addEdge(baseEdge.label(), inV)
            baseEdge.properties().forEachRemaining { p -> e.property(p.key(), p.value()) }
            return e
        }

        fun createEdge(attachableEdge: Attachable<Edge?>?, hostVertex: Vertex): Edge {
            return createEdge(attachableEdge, hostVertex.graph()) // TODO (make local to vertex)
        }

        fun createVertexProperty(
            attachableVertexProperty: Attachable<VertexProperty>,
            hostGraph: Graph
        ): VertexProperty {
            val baseVertexProperty: VertexProperty<Object> = attachableVertexProperty.get()
            val vertexIterator: Iterator<Vertex> = hostGraph.vertices(baseVertexProperty.element().id())
            if (vertexIterator.hasNext()) {
                val vertexProperty: VertexProperty = if (hostGraph.features().vertex().properties()
                        .willAllowId(baseVertexProperty.id())
                ) vertexIterator.next().property(
                    hostGraph.features().vertex().getCardinality(baseVertexProperty.key()),
                    baseVertexProperty.key(),
                    baseVertexProperty.value(),
                    T.id,
                    baseVertexProperty.id()
                ) else vertexIterator.next().property(
                    hostGraph.features().vertex().getCardinality(baseVertexProperty.key()),
                    baseVertexProperty.key(),
                    baseVertexProperty.value()
                )
                baseVertexProperty.properties().forEachRemaining { p -> vertexProperty.property(p.key(), p.value()) }
                return vertexProperty
            }
            throw IllegalStateException("Could not find vertex to create the attachable vertex property on")
        }

        fun createVertexProperty(
            attachableVertexProperty: Attachable<VertexProperty>,
            hostVertex: Vertex
        ): VertexProperty {
            val baseVertexProperty: VertexProperty<Object> = attachableVertexProperty.get()
            val vertexProperty: VertexProperty = if (hostVertex.graph().features().vertex().properties()
                    .willAllowId(baseVertexProperty.id())
            ) hostVertex.property(
                hostVertex.graph().features().vertex().getCardinality(baseVertexProperty.key()),
                baseVertexProperty.key(),
                baseVertexProperty.value(),
                T.id,
                baseVertexProperty.id()
            ) else hostVertex.property(
                hostVertex.graph().features().vertex().getCardinality(baseVertexProperty.key()),
                baseVertexProperty.key(),
                baseVertexProperty.value()
            )
            baseVertexProperty.properties().forEachRemaining { p -> vertexProperty.property(p.key(), p.value()) }
            return vertexProperty
        }

        fun createProperty(attachableProperty: Attachable<Property>, hostGraph: Graph): Property {
            val baseProperty: Property = attachableProperty.get()
            val baseElement: Element = baseProperty.element()
            return if (baseElement is Vertex) {
                createVertexProperty(
                    attachableProperty as Attachable<*>,
                    hostGraph
                )
            } else if (baseElement is Edge) {
                val edgeIterator: Iterator<Edge> = hostGraph.edges(baseElement.id())
                if (edgeIterator.hasNext()) return edgeIterator.next()
                    .property(baseProperty.key(), baseProperty.value())
                throw IllegalStateException("Could not find edge to create the attachable property on")
            } else { // vertex property
                val vertexIterator: Iterator<Vertex> =
                    hostGraph.vertices((baseElement as VertexProperty).element().id())
                if (vertexIterator.hasNext()) {
                    val vertex: Vertex = vertexIterator.next()
                    val vertexPropertyIterator: Iterator<VertexProperty<Object>> =
                        vertex.properties((baseElement as VertexProperty).key())
                    while (vertexPropertyIterator.hasNext()) {
                        val vp: VertexProperty<Object> = vertexPropertyIterator.next()
                        if (ElementHelper.areEqual(vp, baseElement)) return vp.property(
                            baseProperty.key(),
                            baseProperty.value()
                        )
                    }
                }
                throw IllegalStateException("Could not find vertex property to create the attachable property on")
            }
        }

        fun createProperty(attachableProperty: Attachable<Property>, hostVertex: Vertex): Property {
            val baseProperty: Property = attachableProperty.get()
            val baseElement: Element = baseProperty.element()
            return if (baseElement is Vertex) {
                createVertexProperty(
                    attachableProperty as Attachable<*>,
                    hostVertex
                )
            } else if (baseElement is Edge) {
                val edgeIterator: Iterator<Edge> = hostVertex.edges(Direction.OUT)
                if (edgeIterator.hasNext()) return edgeIterator.next()
                    .property(baseProperty.key(), baseProperty.value())
                throw IllegalStateException("Could not find edge to create the property on")
            } else { // vertex property
                val vertexPropertyIterator: Iterator<VertexProperty<Object>> =
                    hostVertex.properties((baseElement as VertexProperty).key())
                while (vertexPropertyIterator.hasNext()) {
                    val vp: VertexProperty<Object> = vertexPropertyIterator.next()
                    if (ElementHelper.areEqual(vp, baseElement)) return vp.property(
                        baseProperty.key(),
                        baseProperty.value()
                    )
                }
                throw IllegalStateException("Could not find vertex property to create the attachable property on")
            }
        }
    }

    object Exceptions {
        fun canNotGetAttachableFromHostVertex(attachable: Attachable<*>, hostVertex: Vertex): IllegalStateException {
            return IllegalStateException("Can not get the attachable from the host vertex: $attachable-/->$hostVertex")
        }

        fun canNotGetAttachableFromHostGraph(attachable: Attachable<*>, hostGraph: Graph): IllegalStateException {
            return IllegalStateException("Can not get the attachable from the host vertex: $attachable-/->$hostGraph")
        }

        fun providedAttachableMustContainAGraphObject(attachable: Attachable<*>): IllegalArgumentException {
            return IllegalArgumentException("The provided attachable must contain a graph object: $attachable")
        }
    }
}