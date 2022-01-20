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
package org.apache.tinkerpop4.gremlin.process.traversal.step.util.event

import org.apache.tinkerpop4.gremlin.structure.Edge
import org.apache.tinkerpop4.gremlin.structure.Graph
import org.apache.tinkerpop4.gremlin.structure.Property
import org.apache.tinkerpop4.gremlin.structure.Vertex
import org.apache.tinkerpop4.gremlin.structure.VertexProperty

/**
 * An example listener that writes a message to the console for each event that fires from the graph.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class ConsoleMutationListener(graph: Graph) : MutationListener {
    private val graph: Graph

    init {
        this.graph = graph
    }

    @Override
    fun vertexAdded(vertex: Vertex) {
        System.out.println(
            "Vertex [" + vertex.toString().toString() + "] added to graph [" + graph.toString().toString() + "]"
        )
    }

    @Override
    fun vertexRemoved(vertex: Vertex) {
        System.out.println(
            "Vertex [" + vertex.toString().toString() + "] removed from graph [" + graph.toString().toString() + "]"
        )
    }

    @Override
    fun vertexPropertyRemoved(vertexProperty: VertexProperty) {
        System.out.println(
            "Vertex Property [" + vertexProperty.toString().toString() + "] removed from graph [" + graph.toString()
                .toString() + "]"
        )
    }

    @Override
    fun edgeAdded(edge: Edge) {
        System.out.println(
            "Edge [" + edge.toString().toString() + "] added to graph [" + graph.toString().toString() + "]"
        )
    }

    @Override
    fun edgeRemoved(edge: Edge) {
        System.out.println(
            "Edge [" + edge.toString().toString() + "] removed from graph [" + graph.toString().toString() + "]"
        )
    }

    @Override
    fun edgePropertyRemoved(element: Edge, removedValue: Property) {
        System.out.println(
            "Edge [" + element.toString()
                .toString() + "] property with value of [" + removedValue.toString() + "] removed in graph [" + graph.toString()
                .toString() + "]"
        )
    }

    @Override
    fun edgePropertyChanged(element: Edge, oldValue: Property, setValue: Object) {
        System.out.println(
            "Edge [" + element.toString()
                .toString() + "] property change value from [" + oldValue.toString() + "] to [" + setValue.toString() + "] in graph [" + graph.toString()
                .toString() + "]"
        )
    }

    @Override
    fun vertexPropertyPropertyChanged(element: VertexProperty, oldValue: Property, setValue: Object) {
        System.out.println(
            "VertexProperty [" + element.toString()
                .toString() + "] property change value from [" + oldValue.toString() + "] to [" + setValue.toString() + "] in graph [" + graph.toString()
                .toString() + "]"
        )
    }

    @Override
    fun vertexPropertyPropertyRemoved(element: VertexProperty, oldValue: Property) {
        System.out.println(
            "VertexProperty [" + element.toString()
                .toString() + "] property with value of [" + oldValue.toString() + "] removed in graph [" + graph.toString()
                .toString() + "]"
        )
    }

    @Override
    fun vertexPropertyChanged(
        element: Vertex,
        oldValue: VertexProperty,
        setValue: Object,
        vararg vertexPropertyKeyValues: Object?
    ) {
        System.out.println(
            "Vertex [" + element.toString()
                .toString() + "] property [" + oldValue.toString() + "] change to [" + setValue.toString() + "] in graph [" + graph.toString()
                .toString() + "]"
        )
    }

    @Override
    override fun toString(): String {
        return MutationListener::class.java.getSimpleName() + "[" + graph + "]"
    }
}