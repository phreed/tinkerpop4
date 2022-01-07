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
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerCat.features
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerHelper.inComputerMode
import org.apache.tinkerpop.gremlin.tinkercat.process.computer.TinkerCatComputerView.getProperty
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerElement.Companion.elementAlreadyRemoved
import org.apache.tinkerpop.gremlin.tinkercat.process.computer.TinkerCatComputerView.addProperty
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerCat.IdManager.convert
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerCat.IdManager.getNextId
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerHelper.autoUpdateIndex
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerHelper.addEdge
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerHelper.removeElementIndex
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerHelper.getEdges
import org.apache.tinkerpop.gremlin.tinkercat.process.computer.TinkerCatComputerView.legalEdge
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerHelper.getVertices
import org.apache.tinkerpop.gremlin.tinkercat.process.computer.TinkerCatComputerView.getProperties
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerCat
import org.apache.tinkerpop.gremlin.structure.util.ElementHelper
import org.apache.tinkerpop.gremlin.structure.util.StringFactory
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils
import java.util.*
import java.util.stream.Collectors

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class TinkerVertex protected constructor(id: Any?, label: String?, private val graph: TinkerCat) : TinkerElement(
    id!!, label!!
), Vertex {
    var properties: MutableMap<String, MutableList<VertexProperty<*>>>? = null
    protected var outEdges: Map<String, Set<Edge>>? = null
    protected var inEdges: Map<String, Set<Edge>>? = null
    private val allowNullPropertyValues: Boolean

    init {
        allowNullPropertyValues = graph.features().vertex().supportsNullPropertyValues()
    }

    override fun graph(): Graph {
        return graph
    }

    override fun <V> property(key: String): VertexProperty<V> {
        if (removed) return VertexProperty.empty()
        return if (inComputerMode(graph)) {
            val list: List<VertexProperty<*>> = graph.graphComputerView!!.getProperty(this, key)
            if (list.size == 0) VertexProperty.empty() else if (list.size == 1) list[0] else throw Vertex.Exceptions.multiplePropertiesExistForProvidedKey(
                key
            )
        } else {
            if (properties != null && properties!!.containsKey(key)) {
                val list: List<VertexProperty<*>> = properties!![key] as List<*>
                if (list.size > 1) throw Vertex.Exceptions.multiplePropertiesExistForProvidedKey(
                    key
                ) else list[0]
            } else VertexProperty.empty()
        }
    }

    override fun <V> property(
        cardinality: VertexProperty.Cardinality,
        key: String,
        value: V,
        vararg keyValues: Any
    ): VertexProperty<V> {
        if (removed) throw elementAlreadyRemoved(
            Vertex::class.java, id
        )
        ElementHelper.legalPropertyKeyValueArray(*keyValues)
        ElementHelper.validateProperty(key, value)

        // if we don't allow null property values and the value is null then the key can be removed but only if the
        // cardinality is single. if it is list/set then we can just ignore the null.
        if (!allowNullPropertyValues && null == value) {
            val card = cardinality ?: graph.features().vertex().getCardinality(key)
            if (VertexProperty.Cardinality.single == card) properties<Any?>(key).forEachRemaining { obj: VertexProperty<Any?> -> obj.remove() }
            return VertexProperty.empty()
        }
        val optionalId = ElementHelper.getIdValue(*keyValues)
        val optionalVertexProperty = ElementHelper.stageVertexProperty(this, cardinality, key, value, *keyValues)
        if (optionalVertexProperty.isPresent) return optionalVertexProperty.get()
        return if (inComputerMode(graph)) {
            val vertexProperty = graph.graphComputerView!!.addProperty(this, key, value) as VertexProperty<V>
            ElementHelper.attachProperties(vertexProperty, *keyValues)
            vertexProperty
        } else {
            val idValue =
                if (optionalId.isPresent) graph.vertexPropertyIdManager.convert(optionalId.get()) else graph.vertexPropertyIdManager.getNextId(
                    graph
                )
            val vertexProperty: VertexProperty<V> = TinkerVertexProperty(idValue, this, key, value)
            if (null == properties) properties = HashMap()
            val list = properties!!.getOrDefault(key, ArrayList())
            list.add(vertexProperty)
            properties!![key] = list
            autoUpdateIndex(this, key, value, null)
            ElementHelper.attachProperties(vertexProperty, *keyValues)
            vertexProperty
        }
    }

    override fun keys(): Set<String> {
        if (null == properties) return emptySet()
        return if (inComputerMode((graph() as TinkerCat))) super@Vertex.keys() else properties!!.keys
    }

    override fun addEdge(label: String, vertex: Vertex, vararg keyValues: Any): Edge {
        if (null == vertex) throw Graph.Exceptions.argumentCanNotBeNull("vertex")
        if (removed) throw elementAlreadyRemoved(
            Vertex::class.java, id
        )
        return addEdge(graph, this, (vertex as TinkerVertex), label, *keyValues)
    }

    override fun remove() {
        val edges: MutableList<Edge> = ArrayList()
        edges(Direction.BOTH).forEachRemaining { e: Edge -> edges.add(e) }
        edges.stream().filter { edge: Edge -> !(edge as TinkerEdge).removed }
            .forEach { obj: Edge -> obj.remove() }
        properties = null
        removeElementIndex(this)
        graph.vertices.remove(id)
        removed = true
    }

    override fun toString(): String {
        return StringFactory.vertexString(this)
    }

    override fun edges(direction: Direction, vararg edgeLabels: String): Iterator<Edge> {
        val edgeIterator: Iterator<Edge> = getEdges(this, direction, *edgeLabels)
        return if (inComputerMode(graph)) IteratorUtils.filter(edgeIterator) { edge: Edge? ->
            graph.graphComputerView!!.legalEdge(
                this,
                edge!!
            )
        } else edgeIterator
    }

    override fun vertices(direction: Direction, vararg edgeLabels: String): Iterator<Vertex> {
        return if (inComputerMode(graph)) if (direction == Direction.BOTH) IteratorUtils.concat(
            IteratorUtils.map(edges(Direction.OUT, *edgeLabels)) { obj: Edge -> obj.inVertex() },
            IteratorUtils.map(
                edges(
                    Direction.IN,
                    *edgeLabels
                )
            ) { obj: Edge -> obj.outVertex() }) else IteratorUtils.map(
            edges(direction, *edgeLabels)
        ) { edge: Edge -> edge.vertices(direction.opposite()).next() } else getVertices(this, direction, *edgeLabels)
    }

    override fun <V> properties(vararg propertyKeys: String): Iterator<VertexProperty<V>> {
        if (removed) return Collections.emptyIterator()
        return if (inComputerMode((graph() as TinkerCat))) (graph() as TinkerCat).graphComputerView!!.getProperties(this@TinkerVertex)
            .stream().filter { p: Property<*> -> ElementHelper.keyExists(p.key(), *propertyKeys) }
            .iterator() as Iterator<*> else {
            if (null == properties) return Collections.emptyIterator()
            if (propertyKeys.size == 1) {
                val properties = properties!!.getOrDefault(propertyKeys[0], emptyList())
                if (properties.size == 1) {
                    IteratorUtils.of<VertexProperty<V>>(properties[0])
                } else if (properties.isEmpty()) {
                    Collections.emptyIterator()
                } else {
                    ArrayList(properties).iterator()
                }
            } else properties!!.entries.stream().filter { (key): Map.Entry<String, List<VertexProperty<*>>> ->
                ElementHelper.keyExists(
                    key, *propertyKeys
                )
            }
                .flatMap { (_, value): Map.Entry<String, List<VertexProperty<*>>> -> value.stream() }
                .collect(Collectors.toList()).iterator()
        }
    }
}