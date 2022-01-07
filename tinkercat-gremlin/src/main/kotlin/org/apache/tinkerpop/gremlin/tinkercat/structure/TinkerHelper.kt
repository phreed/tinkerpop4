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

import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerCat.IdManager.convert
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerCat.IdManager.getNextId
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerEdge.graph
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerCat
import org.apache.tinkerpop.gremlin.structure.util.ElementHelper
import org.apache.tinkerpop.gremlin.process.computer.GraphFilter
import org.apache.tinkerpop.gremlin.process.computer.VertexComputeKey
import org.apache.tinkerpop.gremlin.structure.*
import org.apache.tinkerpop.gremlin.tinkercat.process.computer.TinkerCatComputerView
import java.util.*
import java.util.function.Consumer
import java.util.stream.Stream

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
object TinkerHelper {
    @JvmStatic
    fun addEdge(
        graph: TinkerCat,
        outVertex: TinkerVertex,
        inVertex: TinkerVertex,
        label: String?,
        vararg keyValues: Any?
    ): Edge {
        ElementHelper.validateLabel(label)
        ElementHelper.legalPropertyKeyValueArray(*keyValues)
        var idValue = graph.edgeIdManager.convert(ElementHelper.getIdValue(*keyValues).orElse(null))
        val edge: Edge
        if (null != idValue) {
            if (graph.edges.containsKey(idValue)) throw Graph.Exceptions.edgeWithIdAlreadyExists(idValue)
        } else {
            idValue = graph.edgeIdManager.getNextId(graph)
        }
        edge = TinkerEdge(idValue, outVertex, label, inVertex)
        ElementHelper.attachProperties(edge, *keyValues)
        graph.edges[edge.id()] = edge
        addOutEdge(outVertex, label, edge)
        addInEdge(inVertex, label, edge)
        return edge
    }

    internal fun addOutEdge(vertex: TinkerVertex, label: String?, edge: Edge?) {
        if (null == vertex.outEdges) vertex.outEdges = HashMap()
        var edges = vertex.outEdges[label]
        if (null == edges) {
            edges = HashSet()
            vertex.outEdges[label] = edges
        }
        edges.add(edge)
    }

    internal fun addInEdge(vertex: TinkerVertex, label: String?, edge: Edge?) {
        if (null == vertex.inEdges) vertex.inEdges = HashMap()
        var edges = vertex.inEdges[label]
        if (null == edges) {
            edges = HashSet()
            vertex.inEdges[label] = edges
        }
        edges.add(edge)
    }

    fun queryVertexIndex(graph: TinkerCat, key: String?, value: Any?): List<TinkerVertex> {
        return if (null == graph.vertexIndex) emptyList() else graph.vertexIndex!![key, value]
    }

    fun queryEdgeIndex(graph: TinkerCat, key: String?, value: Any?): List<TinkerEdge> {
        return if (null == graph.edgeIndex) emptyList() else graph.edgeIndex!![key, value]
    }

    @JvmStatic
    fun inComputerMode(graph: TinkerCat): Boolean {
        return null != graph.graphComputerView
    }

    fun createGraphComputerView(
        graph: TinkerCat,
        graphFilter: GraphFilter?,
        computeKeys: Set<VertexComputeKey?>?
    ): TinkerCatComputerView {
        return TinkerCatComputerView(graph, graphFilter!!, computeKeys).also { graph.graphComputerView = it }
    }

    fun getGraphComputerView(graph: TinkerCat): TinkerCatComputerView? {
        return graph.graphComputerView
    }

    fun dropGraphComputerView(graph: TinkerCat) {
        graph.graphComputerView = null
    }

    fun getProperties(vertex: TinkerVertex): Map<String, List<VertexProperty<*>>> {
        return if (null == vertex.properties) emptyMap() else vertex.properties
    }

    fun autoUpdateIndex(edge: TinkerEdge, key: String?, newValue: Any?, oldValue: Any?) {
        val graph = edge.graph() as TinkerCat
        if (graph.edgeIndex != null) graph.edgeIndex!!.autoUpdate(key, newValue, oldValue, edge)
    }

    @JvmStatic
    fun autoUpdateIndex(vertex: TinkerVertex, key: String?, newValue: Any?, oldValue: Any?) {
        val graph = vertex.graph() as TinkerCat
        if (graph.vertexIndex != null) graph.vertexIndex!!.autoUpdate(key, newValue, oldValue, vertex)
    }

    fun removeElementIndex(vertex: TinkerVertex) {
        val graph = vertex.graph() as TinkerCat
        if (graph.vertexIndex != null) graph.vertexIndex!!.removeElement(vertex)
    }

    fun removeElementIndex(edge: TinkerEdge) {
        val graph = edge.graph() as TinkerCat
        if (graph.edgeIndex != null) graph.edgeIndex!!.removeElement(edge)
    }

    fun removeIndex(vertex: TinkerVertex, key: String?, value: Any?) {
        val graph = vertex.graph() as TinkerCat
        if (graph.vertexIndex != null) graph.vertexIndex!!.remove(key, value, vertex)
    }

    @JvmStatic
    fun removeIndex(edge: TinkerEdge, key: String?, value: Any?) {
        val graph = edge.graph() as TinkerCat
        if (graph.edgeIndex != null) graph.edgeIndex!!.remove(key, value, edge)
    }

    fun getEdges(vertex: TinkerVertex, direction: Direction, vararg edgeLabels: String?): Iterator<TinkerEdge> {
        val edges: MutableList<Edge> = ArrayList()
        if (direction == Direction.OUT || direction == Direction.BOTH) {
            if (vertex.outEdges != null) {
                if (edgeLabels.size == 0) vertex.outEdges.values.forEach(Consumer { c: Set<Edge>? ->
                    edges.addAll(
                        c!!
                    )
                }) else if (edgeLabels.size == 1) edges.addAll(
                    vertex.outEdges.getOrDefault(
                        edgeLabels[0],
                        emptySet()
                    )
                ) else Stream.of(*edgeLabels).map { key: String? -> vertex.outEdges[key] }
                    .filter { obj: Set<Edge>? -> Objects.nonNull(obj) }
                    .forEach { c: Set<Edge>? ->
                        edges.addAll(
                            c!!
                        )
                    }
            }
        }
        if (direction == Direction.IN || direction == Direction.BOTH) {
            if (vertex.inEdges != null) {
                if (edgeLabels.size == 0) vertex.inEdges.values.forEach(Consumer { c: Set<Edge>? ->
                    edges.addAll(
                        c!!
                    )
                }) else if (edgeLabels.size == 1) edges.addAll(
                    vertex.inEdges.getOrDefault(
                        edgeLabels[0],
                        emptySet()
                    )
                ) else Stream.of(*edgeLabels).map { key: String? -> vertex.inEdges[key] }
                    .filter { obj: Set<Edge>? -> Objects.nonNull(obj) }
                    .forEach { c: Set<Edge>? ->
                        edges.addAll(
                            c!!
                        )
                    }
            }
        }
        return edges.iterator()
    }

    fun getVertices(vertex: TinkerVertex, direction: Direction, vararg edgeLabels: String?): Iterator<TinkerVertex> {
        val vertices: MutableList<Vertex> = ArrayList()
        if (direction == Direction.OUT || direction == Direction.BOTH) {
            if (vertex.outEdges != null) {
                if (edgeLabels.size == 0) vertex.outEdges.values.forEach(Consumer { set: Set<Edge> ->
                    set.forEach(
                        Consumer { edge: Edge -> vertices.add((edge as TinkerEdge).inVertex) })
                }) else if (edgeLabels.size == 1) vertex.outEdges.getOrDefault(edgeLabels[0], emptySet()).forEach(
                    Consumer { edge: Edge -> vertices.add((edge as TinkerEdge).inVertex) }) else Stream.of(*edgeLabels)
                    .map { key: String? -> vertex.outEdges[key] }
                    .filter { obj: Set<Edge>? -> Objects.nonNull(obj) }
                    .flatMap { obj: Set<Edge> -> obj.stream() }
                    .forEach { edge: Edge -> vertices.add((edge as TinkerEdge).inVertex) }
            }
        }
        if (direction == Direction.IN || direction == Direction.BOTH) {
            if (vertex.inEdges != null) {
                if (edgeLabels.size == 0) vertex.inEdges.values.forEach(Consumer { set: Set<Edge> ->
                    set.forEach(
                        Consumer { edge: Edge -> vertices.add((edge as TinkerEdge).outVertex) })
                }) else if (edgeLabels.size == 1) vertex.inEdges.getOrDefault(edgeLabels[0], emptySet()).forEach(
                    Consumer { edge: Edge -> vertices.add((edge as TinkerEdge).outVertex) }) else Stream.of(*edgeLabels)
                    .map { key: String? -> vertex.inEdges[key] }
                    .filter { obj: Set<Edge>? -> Objects.nonNull(obj) }
                    .flatMap { obj: Set<Edge> -> obj.stream() }
                    .forEach { edge: Edge -> vertices.add((edge as TinkerEdge).outVertex) }
            }
        }
        return vertices.iterator()
    }

    fun getVertices(graph: TinkerCat): Map<Any, Vertex> {
        return graph.vertices
    }

    fun getEdges(graph: TinkerCat): Map<Any, Edge> {
        return graph.edges
    }
}