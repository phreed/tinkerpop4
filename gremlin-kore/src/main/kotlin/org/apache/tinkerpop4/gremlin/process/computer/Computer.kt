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
package org.apache.tinkerpop4.gremlin.process.computer

import org.apache.tinkerpop4.gremlin.process.traversal.Traversal

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class Computer : Function<Graph?, GraphComputer?>, Serializable, Cloneable {
    private var graphComputerClass: Class<out GraphComputer?> = GraphComputer::class.java
    private var configuration: Map<String, Object> = HashMap()
    var workers = -1
        private set
    private var persist: Persist? = null
    private var resultGraph: ResultGraph? = null
    private var vertices: Traversal<Vertex, Vertex>? = null
    private var edges: Traversal<Vertex, Edge>? = null

    private constructor(graphComputerClass: Class<out GraphComputer?>) {
        this.graphComputerClass = graphComputerClass
    }

    private constructor() {}

    fun graphComputer(graphComputerClass: Class<out GraphComputer?>): Computer {
        val clone = clone()
        clone.graphComputerClass = graphComputerClass
        return clone
    }

    fun configure(key: String?, value: Object?): Computer {
        val clone = clone()
        clone.configuration.put(key, value)
        return clone
    }

    fun configure(configurations: Map<String?, Object?>?): Computer {
        val clone = clone()
        clone.configuration.putAll(configurations)
        return clone
    }

    fun workers(workers: Int): Computer {
        val clone = clone()
        clone.workers = workers
        return clone
    }

    fun persist(persist: Persist?): Computer {
        val clone = clone()
        clone.persist = persist
        return clone
    }

    fun result(resultGraph: ResultGraph?): Computer {
        val clone = clone()
        clone.resultGraph = resultGraph
        return clone
    }

    fun vertices(vertexFilter: Traversal<Vertex?, Vertex?>): Computer {
        val clone = clone()
        clone.vertices = vertexFilter
        return clone
    }

    fun edges(edgeFilter: Traversal<Vertex?, Edge?>): Computer {
        val clone = clone()
        clone.edges = edgeFilter
        return clone
    }

    fun apply(graph: Graph): GraphComputer {
        var computer: GraphComputer =
            if (graphComputerClass.equals(GraphComputer::class.java)) graph.compute() else graph.compute(
                graphComputerClass
            )
        for (entry in configuration.entrySet()) {
            computer = computer.configure(entry.getKey(), entry.getValue())
        }
        if (-1 != workers) computer = computer.workers(workers)
        if (null != persist) computer = computer.persist(persist)
        if (null != resultGraph) computer = computer.result(resultGraph)
        if (null != vertices) computer = computer.vertices(vertices)
        if (null != edges) computer.edges(edges)
        return computer
    }

    @Override
    override fun toString(): String {
        return graphComputerClass.getSimpleName().toLowerCase()
    }

    @Override
    fun clone(): Computer {
        return try {
            val clone = super.clone() as Computer
            clone.configuration = HashMap(configuration)
            if (null != vertices) clone.vertices = vertices.asAdmin().clone()
            if (null != edges) clone.edges = edges.asAdmin().clone()
            clone
        } catch (e: CloneNotSupportedException) {
            throw IllegalStateException(e.getMessage())
        }
    }

    /////////////////
    /////////////////
    fun getGraphComputerClass(): Class<out GraphComputer?> {
        return graphComputerClass
    }

    fun getConfiguration(): Map<String, Object> {
        return configuration
    }

    fun getVertices(): Traversal<Vertex, Vertex> {
        return vertices
    }

    fun getEdges(): Traversal<Vertex, Edge> {
        return edges
    }

    fun getPersist(): Persist? {
        return persist
    }

    fun getResultGraph(): ResultGraph? {
        return resultGraph
    }

    companion object {
        fun compute(): Computer {
            return Computer(GraphComputer::class.java)
        }

        fun compute(graphComputerClass: Class<out GraphComputer?>): Computer {
            return Computer(graphComputerClass)
        }
    }
}