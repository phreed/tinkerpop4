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
package org.apache.tinkerpop4.gremlin.process.computer.util

import org.apache.commons.configuration2.Configuration

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class ComputerGraph private constructor(
    private val state: State,
    starVertex: Vertex,
    vertexProgram: Optional<VertexProgram<*>>
) : Graph {
    private enum class State {
        VERTEX_PROGRAM, MAP_REDUCE
    }

    val starVertex: ComputerVertex
    private val computeKeys: Set<String>

    init {
        computeKeys = if (vertexProgram.isPresent()) vertexProgram.get().getVertexComputeKeys().stream()
            .map(VertexComputeKey::getKey).collect(Collectors.toSet()) else Collections.emptySet()
        this.starVertex = ComputerVertex(starVertex)
    }

    @Override
    fun addVertex(vararg keyValues: Object?): Vertex {
        throw UnsupportedOperationException()
    }

    @Override
    @Throws(IllegalArgumentException::class)
    fun <C : GraphComputer?> compute(graphComputerClass: Class<C>?): C {
        throw UnsupportedOperationException()
    }

    @Override
    @Throws(IllegalArgumentException::class)
    fun compute(): GraphComputer {
        throw UnsupportedOperationException()
    }

    @Override
    fun vertices(vararg vertexIds: Object?): Iterator<Vertex> {
        throw UnsupportedOperationException()
    }

    @Override
    fun edges(vararg edgeIds: Object?): Iterator<Edge> {
        throw UnsupportedOperationException()
    }

    @Override
    fun tx(): Transaction {
        return starVertex.graph().tx()
    }

    @Override
    fun variables(): Variables {
        throw UnsupportedOperationException()
    }

    @Override
    fun configuration(): Configuration {
        throw UnsupportedOperationException()
    }

    @Override
    @Throws(Exception::class)
    fun close() {
        throw UnsupportedOperationException()
    }

    inner class ComputerElement(element: Element) : Element, WrappedElement<Element?> {
        private val element: Element

        init {
            this.element = element
        }

        @Override
        fun id(): Object {
            return element.id()
        }

        @Override
        fun label(): String {
            return element.label()
        }

        @Override
        fun graph(): Graph {
            return this@ComputerGraph
        }

        @Override
        fun keys(): Set<String> {
            return element.keys().stream().filter { key -> !computeKeys.contains(key) }.collect(Collectors.toSet())
        }

        @Override
        fun <V> property(key: String?): Property<V> {
            return ComputerProperty<V>(element.property(key))
        }

        @Override
        fun <V> property(key: String?, value: V): Property<V> {
            if (state.equals(State.MAP_REDUCE)) throw GraphComputer.Exceptions.vertexPropertiesCanNotBeUpdatedInMapReduce()
            return ComputerProperty<V>(element.property(key, value))
        }

        @Override
        @Throws(NoSuchElementException::class)
        fun <V> value(key: String?): V {
            return element.value(key)
        }

        @Override
        fun remove() {
            element.remove()
        }

        @Override
        fun <V> properties(vararg propertyKeys: String?): Iterator<Property<V?>?> {
            return IteratorUtils.filter(element.properties(propertyKeys)) { property -> !computeKeys.contains(property.key()) }
        }

        @Override
        fun <V> values(vararg propertyKeys: String?): Iterator<V> {
            return IteratorUtils.map(properties<V>(*propertyKeys)) { property -> property.value() }
        }

        @Override
        override fun hashCode(): Int {
            return element.hashCode()
        }

        @Override
        override fun toString(): String {
            return element.toString()
        }

        @Override
        override fun equals(other: Object?): Boolean {
            return element.equals(other)
        }

        @get:Override
        val baseElement: Element
            get() = element
    }

    ///////////////////////////////////
    inner class ComputerVertex(vertex: Vertex) : ComputerElement(vertex), Vertex, WrappedVertex<Vertex?> {
        @Override
        override fun <V> property(key: String?): VertexProperty<V> {
            return ComputerVertexProperty<V>(baseVertex.property(key))
        }

        @Override
        override fun <V> property(key: String, value: V): VertexProperty<V> {
            if (state.equals(State.MAP_REDUCE)) throw GraphComputer.Exceptions.vertexPropertiesCanNotBeUpdatedInMapReduce()
            if (!computeKeys.contains(key)) throw GraphComputer.Exceptions.providedKeyIsNotAnElementComputeKey(key)
            return ComputerVertexProperty<V>(baseVertex.property(key, value))
        }

        @Override
        fun <V> property(key: String, value: V, vararg keyValues: Object?): VertexProperty<V> {
            if (state.equals(State.MAP_REDUCE)) throw GraphComputer.Exceptions.vertexPropertiesCanNotBeUpdatedInMapReduce()
            if (!computeKeys.contains(key)) throw GraphComputer.Exceptions.providedKeyIsNotAnElementComputeKey(key)
            return ComputerVertexProperty<V>(baseVertex.property(key, value, keyValues))
        }

        @Override
        fun <V> property(
            cardinality: VertexProperty.Cardinality?,
            key: String,
            value: V,
            vararg keyValues: Object?
        ): VertexProperty<V> {
            if (state.equals(State.MAP_REDUCE)) throw GraphComputer.Exceptions.vertexPropertiesCanNotBeUpdatedInMapReduce()
            if (!computeKeys.contains(key)) throw GraphComputer.Exceptions.providedKeyIsNotAnElementComputeKey(key)
            return ComputerVertexProperty<V>(baseVertex.property(cardinality, key, value, keyValues))
        }

        @Override
        fun addEdge(label: String?, inVertex: Vertex?, vararg keyValues: Object?): Edge {
            if (state.equals(State.MAP_REDUCE)) throw GraphComputer.Exceptions.incidentAndAdjacentElementsCanNotBeAccessedInMapReduce()
            return ComputerEdge(baseVertex.addEdge(label, inVertex, keyValues))
        }

        @Override
        fun edges(direction: Direction?, vararg edgeLabels: String?): Iterator<Edge> {
            if (state.equals(State.MAP_REDUCE)) throw GraphComputer.Exceptions.incidentAndAdjacentElementsCanNotBeAccessedInMapReduce()
            return IteratorUtils.map(baseVertex.edges(direction, edgeLabels)) { edge: Edge -> ComputerEdge(edge) }
        }

        @Override
        fun vertices(direction: Direction?, vararg edgeLabels: String?): Iterator<Vertex> {
            if (state.equals(State.MAP_REDUCE)) throw GraphComputer.Exceptions.incidentAndAdjacentElementsCanNotBeAccessedInMapReduce()
            return IteratorUtils.map(
                baseVertex.vertices(
                    direction,
                    edgeLabels
                )
            ) { v -> if (v.equals(starVertex)) starVertex else ComputerAdjacentVertex(v) }
        }

        @Override
        override fun <V> properties(vararg propertyKeys: String?): Iterator<VertexProperty<V>> {
            return IteratorUtils.map(super.properties<Any>(*propertyKeys)) { property ->
                ComputerVertexProperty<V>(
                    property as VertexProperty<V>
                )
            }
        }

        @get:Override
        val baseVertex: Vertex
            get() = this.getBaseElement() as Vertex
    }

    ////////////////////////////
    inner class ComputerEdge(edge: Edge) : ComputerElement(edge), Edge, WrappedEdge<Edge?> {
        @Override
        fun vertices(direction: Direction): Iterator<Vertex> {
            if (direction.equals(Direction.OUT)) return IteratorUtils.of(outVertex())
            return if (direction.equals(Direction.IN)) IteratorUtils.of(inVertex()) else IteratorUtils.of(
                outVertex(),
                inVertex()
            )
        }

        @Override
        fun outVertex(): Vertex {
            return if (baseEdge.outVertex()
                    .equals(starVertex)
            ) starVertex else ComputerAdjacentVertex(baseEdge.outVertex())
        }

        @Override
        fun inVertex(): Vertex {
            return if (baseEdge.inVertex()
                    .equals(starVertex)
            ) starVertex else ComputerAdjacentVertex(baseEdge.inVertex())
        }

        @Override
        override fun <V> properties(vararg propertyKeys: String?): Iterator<Property<V>> {
            return IteratorUtils.map(super.properties<Any>(*propertyKeys)) { property -> ComputerProperty<Any?>(property) }
        }

        @get:Override
        val baseEdge: Edge
            get() = this.getBaseElement() as Edge
    }

    ///////////////////////////
    inner class ComputerVertexProperty<V>(vertexProperty: VertexProperty<V>) : ComputerElement(vertexProperty),
        VertexProperty<V>, WrappedVertexProperty<VertexProperty<V>?> {
        @Override
        fun key(): String {
            return baseVertexProperty.key()
        }

        @Override
        @Throws(NoSuchElementException::class)
        fun value(): V {
            return baseVertexProperty.value()
        }

        @get:Override
        val isPresent: Boolean
            get() = baseVertexProperty.isPresent()

        @Override
        fun element(): Vertex {
            return ComputerVertex(baseVertexProperty.element())
        }

        @Override
        override fun <U> properties(vararg propertyKeys: String?): Iterator<Property<U>> {
            return IteratorUtils.map(super.properties<Any>(*propertyKeys)) { property -> ComputerProperty<Any?>(property) }
        }

        @get:Override
        val baseVertexProperty: VertexProperty<V>
            get() = this.getBaseElement() as VertexProperty<V>
    }

    ///////////////////////////
    inner class ComputerProperty<V>(property: Property<V>) : Property<V>, WrappedProperty<Property<V>?> {
        private val property: Property<V>

        init {
            this.property = property
        }

        @Override
        fun key(): String {
            return property.key()
        }

        @Override
        @Throws(NoSuchElementException::class)
        fun value(): V {
            return property.value()
        }

        @get:Override
        val isPresent: Boolean
            get() = property.isPresent()

        @Override
        fun element(): Element {
            val element: Element = property.element()
            return if (element is Vertex) ComputerVertex(element as Vertex) else if (element is Edge) ComputerEdge(
                element as Edge
            ) else ComputerVertexProperty<Any?>(
                element as VertexProperty
            )
        }

        @Override
        fun remove() {
            property.remove()
        }

        @get:Override
        val baseProperty: Property<V>
            get() = property

        @Override
        override fun toString(): String {
            return property.toString()
        }

        @Override
        override fun hashCode(): Int {
            return property.hashCode()
        }

        @Override
        override fun equals(other: Object?): Boolean {
            return property.equals(other)
        }
    }

    ///////////////////////////
    inner class ComputerAdjacentVertex(adjacentVertex: Vertex) : Vertex, WrappedVertex<Vertex?> {
        private val adjacentVertex: Vertex

        init {
            this.adjacentVertex = adjacentVertex
        }

        @Override
        fun addEdge(label: String?, inVertex: Vertex?, vararg keyValues: Object?): Edge {
            throw GraphComputer.Exceptions.adjacentVertexEdgesAndVerticesCanNotBeReadOrUpdated()
        }

        @Override
        fun <V> property(key: String?, value: V, vararg keyValues: Object?): VertexProperty<V> {
            throw GraphComputer.Exceptions.adjacentVertexPropertiesCanNotBeReadOrUpdated()
        }

        @Override
        fun <V> property(
            cardinality: VertexProperty.Cardinality?,
            key: String?,
            value: V,
            vararg keyValues: Object?
        ): VertexProperty<V> {
            throw GraphComputer.Exceptions.adjacentVertexPropertiesCanNotBeReadOrUpdated()
        }

        @Override
        fun edges(direction: Direction?, vararg edgeLabels: String?): Iterator<Edge> {
            throw GraphComputer.Exceptions.adjacentVertexEdgesAndVerticesCanNotBeReadOrUpdated()
        }

        @Override
        fun vertices(direction: Direction?, vararg edgeLabels: String?): Iterator<Vertex> {
            throw GraphComputer.Exceptions.adjacentVertexEdgesAndVerticesCanNotBeReadOrUpdated()
        }

        @Override
        fun id(): Object {
            return adjacentVertex.id()
        }

        @Override
        fun label(): String {
            throw GraphComputer.Exceptions.adjacentVertexLabelsCanNotBeRead()
        }

        @Override
        fun graph(): Graph {
            return this@ComputerGraph
        }

        @Override
        fun remove() {
        }

        @Override
        fun <V> properties(vararg propertyKeys: String?): Iterator<VertexProperty<V>> {
            throw GraphComputer.Exceptions.adjacentVertexPropertiesCanNotBeReadOrUpdated()
        }

        @Override
        override fun hashCode(): Int {
            return adjacentVertex.hashCode()
        }

        @Override
        override fun toString(): String {
            return adjacentVertex.toString()
        }

        @Override
        override fun equals(other: Object?): Boolean {
            return adjacentVertex.equals(other)
        }

        @get:Override
        val baseVertex: Vertex
            get() = adjacentVertex
    }

    companion object {
        fun vertexProgram(starVertex: Vertex, vertexProgram: VertexProgram?): ComputerVertex {
            return ComputerGraph(State.VERTEX_PROGRAM, starVertex, Optional.of(vertexProgram)).starVertex
        }

        fun mapReduce(starVertex: Vertex): ComputerVertex {
            return ComputerGraph(State.MAP_REDUCE, starVertex, Optional.empty()).starVertex
        }
    }
}