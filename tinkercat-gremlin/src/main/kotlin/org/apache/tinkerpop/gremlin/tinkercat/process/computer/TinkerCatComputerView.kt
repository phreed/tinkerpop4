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
package org.apache.tinkerpop.gremlin.tinkercat.process.computer

import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerCat
import org.apache.tinkerpop.gremlin.process.computer.GraphFilter
import org.apache.tinkerpop.gremlin.process.computer.VertexComputeKey
import java.util.concurrent.ConcurrentHashMap
import org.apache.tinkerpop.gremlin.structure.util.ElementHelper
import org.apache.tinkerpop.gremlin.process.computer.GraphComputer
import org.apache.tinkerpop.gremlin.process.computer.GraphComputer.ResultGraph
import org.apache.tinkerpop.gremlin.process.computer.GraphComputer.Persist
import org.apache.tinkerpop.gremlin.structure.*
import org.apache.tinkerpop.gremlin.structure.util.empty.EmptyGraph
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerHelper
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerVertex
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerVertexProperty
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet
import java.util.function.BiFunction
import java.util.function.Consumer
import java.util.function.Function

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
open class TinkerCatComputerView(
    private val graph: TinkerCat,
    graphFilter: GraphFilter,
    computeKeys: Set<VertexComputeKey>
) {
    protected val computeKeys: MutableMap<String, VertexComputeKey>
    private val computeProperties: MutableMap<Element, MutableMap<String, MutableList<VertexProperty<*>>>>
    private val legalVertices: MutableSet<Any> = HashSet()
    private val legalEdges: MutableMap<Any, Set<Any>> = HashMap()
    private val graphFilter: GraphFilter

    init {
        this.computeKeys = HashMap()
        computeKeys.forEach(Consumer { key: VertexComputeKey -> this.computeKeys[key.key] = key })
        computeProperties = ConcurrentHashMap()
        this.graphFilter = graphFilter
        if (this.graphFilter.hasFilter()) {
            graph.vertices().forEachRemaining { vertex: Vertex ->
                var legalVertex = false
                if (this.graphFilter.hasVertexFilter() && this.graphFilter.legalVertex(vertex)) {
                    legalVertices.add(vertex.id())
                    legalVertex = true
                }
                if ((legalVertex || !this.graphFilter.hasVertexFilter()) && this.graphFilter.hasEdgeFilter()) {
                    val edges: MutableSet<Any> = HashSet()
                    legalEdges[vertex.id()] = edges
                    this.graphFilter.legalEdges(vertex).forEachRemaining { edge: Edge -> edges.add(edge.id()) }
                }
            }
        }
    }

    fun <V> addProperty(vertex: TinkerVertex, key: String, value: V): Property<V> {
        ElementHelper.validateProperty(key, value)
        return if (isComputeKey(key)) {
            val property: TinkerVertexProperty<V> = object :
                TinkerVertexProperty<V>(
                    vertex,
                    key,
                    value
                ) {
                override fun remove() {
                    removeProperty(vertex, key, this)
                }
            }
            addValue(vertex, key, property)
            property
        } else {
            throw GraphComputer.Exceptions.providedKeyIsNotAnElementComputeKey(key)
        }
    }

    fun getProperty(vertex: TinkerVertex, key: String): List<VertexProperty<*>> {
        // if the vertex property is already on the vertex, use that.
        val vertexProperty = this.getValue(vertex, key)
        return vertexProperty.ifEmpty {
            TinkerHelper.getProperties(vertex).getOrDefault(key, emptyList())
        }
        //return isComputeKey(key) ? this.getValue(vertex, key) : (List) TinkerHelper.getProperties(vertex).getOrDefault(key, Collections.emptyList());
    }

    fun getProperties(vertex: TinkerVertex?): List<Property<*>> {
        val list: MutableList<Property<*>> = ArrayList()
        for (properties in vertex?.let { TinkerHelper.getProperties(it).values }!!) {
            list.addAll(properties!!)
        }
        for (properties in computeProperties.getOrDefault(vertex, emptyMap<String, List<VertexProperty<*>>>()).values) {
            list.addAll(properties)
        }
        return list
    }

    fun removeProperty(vertex: TinkerVertex, key: String, property: VertexProperty<*>) {
        if (isComputeKey(key)) {
            removeValue(vertex, key, property)
        } else {
            throw GraphComputer.Exceptions.providedKeyIsNotAnElementComputeKey(key)
        }
    }

    fun legalVertex(vertex: Vertex): Boolean {
        return !graphFilter.hasVertexFilter() || legalVertices.contains(vertex.id())
    }

    fun legalEdge(vertex: Vertex, edge: Edge): Boolean {
        return !graphFilter.hasEdgeFilter() || legalEdges[vertex.id()]!!.contains(edge.id())
    }

    fun complete() {
        // remove all transient properties from the vertices
        for (computeKey in computeKeys.values) {
            if (computeKey.isTransient) {
                for (properties in computeProperties.values) {
                    properties.remove(computeKey.key)
                }
            }
        }
    }

    //////////////////////
    fun processResultGraphPersist(
        resultGraph: ResultGraph,
        persist: Persist
    ): Graph {
        return if (Persist.NOTHING == persist) {
            if (ResultGraph.ORIGINAL == resultGraph) graph else EmptyGraph.instance()
        } else if (Persist.VERTEX_PROPERTIES == persist) {
            if (ResultGraph.ORIGINAL == resultGraph) {
                addPropertiesToOriginalGraph()
                graph
            } else {
                val newGraph = TinkerCat.open()
                graph.vertices().forEachRemaining { vertex: Vertex ->
                    val newVertex = newGraph.addVertex(T.id, vertex.id(), T.label, vertex.label())
                    vertex.properties<Any?>().forEachRemaining { vertexProperty: VertexProperty<Any?> ->
                        val newVertexProperty: VertexProperty<*> = newVertex.property(
                            VertexProperty.Cardinality.list,
                            vertexProperty.key(),
                            vertexProperty.value(),
                            T.id,
                            vertexProperty.id()
                        )
                        vertexProperty.properties<Any?>().forEachRemaining { property: Property<Any?> ->
                            newVertexProperty.property(
                                property.key(),
                                property.value()
                            )
                        }
                    }
                }
                newGraph
            }
        } else {  // Persist.EDGES
            if (ResultGraph.ORIGINAL == resultGraph) {
                addPropertiesToOriginalGraph()
                graph
            } else {
                val newGraph = TinkerCat.open()
                graph.vertices().forEachRemaining { vertex: Vertex ->
                    val newVertex = newGraph.addVertex(T.id, vertex.id(), T.label, vertex.label())
                    vertex.properties<Any?>().forEachRemaining { vertexProperty: VertexProperty<Any?> ->
                        val newVertexProperty: VertexProperty<*> = newVertex.property(
                            VertexProperty.Cardinality.list,
                            vertexProperty.key(),
                            vertexProperty.value(),
                            T.id,
                            vertexProperty.id()
                        )
                        vertexProperty.properties<Any?>().forEachRemaining { property: Property<Any?> ->
                            newVertexProperty.property(
                                property.key(),
                                property.value()
                            )
                        }
                    }
                }
                graph.edges().forEachRemaining { edge: Edge ->
                    val outVertex = newGraph.vertices(edge.outVertex().id()).next()
                    val inVertex = newGraph.vertices(edge.inVertex().id()).next()
                    val newEdge = outVertex.addEdge(edge.label(), inVertex, T.id, edge.id())
                    edge.properties<Any>().forEachRemaining { property: Property<Any> ->
                        newEdge.property(
                            property.key(),
                            property.value()
                        )
                    }
                }
                newGraph
            }
        }
    }

    private fun addPropertiesToOriginalGraph() {
        TinkerHelper.dropGraphComputerView(graph)
        computeProperties.forEach { (element: Element, properties: Map<String, MutableList<VertexProperty<*>>>) ->
            properties.forEach { (key: String?, vertexProperties: List<VertexProperty<*>>) ->
                vertexProperties.forEach(
                    Consumer { vertexProperty: VertexProperty<*> ->
                        val newVertexProperty: VertexProperty<*> = (element as Vertex).property(
                            VertexProperty.Cardinality.list,
                            vertexProperty.key(),
                            vertexProperty.value(),
                            T.id,
                            vertexProperty.id()
                        )
                        vertexProperty.properties<Any?>().forEachRemaining { property: Property<Any?> ->
                            newVertexProperty.property(
                                property.key(),
                                property.value()
                            )
                        }
                    })
            }
        }
        computeProperties.clear()
    }

    //////////////////////
    private fun isComputeKey(key: String): Boolean {
        return computeKeys.containsKey(key)
    }

    private fun addValue(vertex: Vertex, key: String, property: VertexProperty<*>) {
        val elementProperties = computeProperties.computeIfAbsent(
            vertex
        ) {
            HashMap<String, MutableList<VertexProperty<*>>>()
        }
        elementProperties.compute(key, BiFunction { _: String?, v: MutableList<VertexProperty<*>>? ->
            val nv = v ?: mutableListOf()
            nv.add(property)
            nv
        })
    }

    private fun removeValue(vertex: Vertex, key: String, property: VertexProperty<*>) {
        computeProperties.getOrDefault(
            vertex,
            emptyMap()
        )[key]!!.remove(property)
    }

    private fun getValue(vertex: Vertex, key: String): List<VertexProperty<*>> {
        return computeProperties.getOrDefault(vertex, emptyMap<String, List<VertexProperty<*>>>())[key] ?: emptyList()
    }
}