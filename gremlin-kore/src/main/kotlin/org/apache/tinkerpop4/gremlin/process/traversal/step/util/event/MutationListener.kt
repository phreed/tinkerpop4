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

import org.apache.tinkerpop4.gremlin.process.traversal.strategy.decoration.EventStrategy
import org.apache.tinkerpop4.gremlin.structure.Edge
import org.apache.tinkerpop4.gremlin.structure.Element
import org.apache.tinkerpop4.gremlin.structure.Property
import org.apache.tinkerpop4.gremlin.structure.Vertex
import org.apache.tinkerpop4.gremlin.structure.VertexProperty

/**
 * Interface for a listener to [EventStrategy] change events. Implementations of this interface should be added
 * to the list of listeners on the addListener method on the [EventStrategy].
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
interface MutationListener {
    /**
     * Raised when a new [Vertex] is added.
     *
     * @param vertex the [Vertex] that was added
     */
    fun vertexAdded(vertex: Vertex?)

    /**
     * Raised after a [Vertex] was removed from the graph.
     *
     * @param vertex the [Vertex] that was removed
     */
    fun vertexRemoved(vertex: Vertex?)

    /**
     * Raised after the property of a [Vertex] changed.
     *
     * @param element  the [Vertex] that changed
     * @param setValue the new value of the property
     */
    fun vertexPropertyChanged(
        element: Vertex?,
        oldValue: VertexProperty?,
        setValue: Object?,
        vararg vertexPropertyKeyValues: Object?
    )

    /**
     * Raised after a [VertexProperty] was removed from the graph.
     *
     * @param vertexProperty the [VertexProperty] that was removed
     */
    fun vertexPropertyRemoved(vertexProperty: VertexProperty?)

    /**
     * Raised after a new [Edge] is added.
     *
     * @param edge the [Edge] that was added
     */
    fun edgeAdded(edge: Edge?)

    /**
     * Raised after an [Edge] was removed from the graph.
     *
     * @param edge  the [Edge] that was removed.
     */
    fun edgeRemoved(edge: Edge?)

    /**
     * Raised after the property of a [Edge] changed.
     *
     * @param element  the [Edge] that changed
     * @param setValue the new value of the property
     */
    fun edgePropertyChanged(element: Edge?, oldValue: Property?, setValue: Object?)

    /**
     * Raised after an [Property] property was removed from an [Edge].
     *
     * @param property  the [Property] that was removed
     */
    fun edgePropertyRemoved(element: Edge?, property: Property?)

    /**
     * Raised after the property of a [VertexProperty] changed.
     *
     * @param element  the [VertexProperty] that changed
     * @param setValue the new value of the property
     */
    fun vertexPropertyPropertyChanged(element: VertexProperty?, oldValue: Property?, setValue: Object?)

    /**
     * Raised after an [Property] property was removed from a [VertexProperty].
     *
     * @param property  the [Property] that removed
     */
    fun vertexPropertyPropertyRemoved(element: VertexProperty?, property: Property?)
}