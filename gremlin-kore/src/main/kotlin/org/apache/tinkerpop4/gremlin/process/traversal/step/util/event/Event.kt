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

import org.apache.tinkerpop4.gremlin.process.traversal.Traversal

/**
 * A representation of some action that occurs on a [Graph] for a [Traversal].
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
interface Event {
    /**
     * An `Event` publishes its action to all the event [MutationListener] objects.
     */
    fun fireEvent(eventListeners: Iterator<MutationListener?>?)

    /**
     * Represents an action where an [Edge] is added to the [Graph].
     */
    class EdgeAddedEvent(edge: Edge) : Event {
        private val edge: Edge

        init {
            this.edge = edge
        }

        @Override
        override fun fireEvent(eventListeners: Iterator<MutationListener>) {
            while (eventListeners.hasNext()) {
                eventListeners.next().edgeAdded(edge)
            }
        }
    }

    /**
     * Represents an action where an [Edge] [Property] is added/modified.  If the [Property] is
     * new then the `oldValue` will be `null`.
     */
    class EdgePropertyChangedEvent(edge: Edge?, oldValue: Property?, newValue: Object?) :
        ElementPropertyChangedEvent(edge, oldValue, newValue) {
        @Override
        override fun fire(
            listener: MutationListener,
            element: Element?,
            oldValue: Property?,
            newValue: Object?,
            vararg vertexPropertyKeyValues: Object?
        ) {
            listener.edgePropertyChanged(element as Edge?, oldValue, newValue)
        }
    }

    /**
     * Represents an action where an [Edge] [Property] is removed.
     */
    class EdgePropertyRemovedEvent(element: Edge?, removed: Property?) : ElementPropertyEvent(element, removed, null) {
        @Override
        override fun fire(
            listener: MutationListener,
            element: Element?,
            oldValue: Property?,
            newValue: Object?,
            vararg vertexPropertyKeyValues: Object?
        ) {
            listener.edgePropertyRemoved(element as Edge?, oldValue)
        }
    }

    /**
     * Represents an action where an [Edge] is removed from the [Graph].
     */
    class EdgeRemovedEvent(edge: Edge) : Event {
        private val edge: Edge

        init {
            this.edge = edge
        }

        @Override
        override fun fireEvent(eventListeners: Iterator<MutationListener>) {
            while (eventListeners.hasNext()) {
                eventListeners.next().edgeRemoved(edge)
            }
        }
    }

    /**
     * Represents an action where a [Vertex] is removed from the [Graph].
     */
    class VertexAddedEvent(vertex: Vertex) : Event {
        private val vertex: Vertex

        init {
            this.vertex = vertex
        }

        @Override
        override fun fireEvent(eventListeners: Iterator<MutationListener>) {
            while (eventListeners.hasNext()) {
                eventListeners.next().vertexAdded(vertex)
            }
        }
    }

    /**
     * Represents an action where a [VertexProperty] is modified on a [Vertex].
     */
    class VertexPropertyChangedEvent(
        element: Vertex?,
        oldValue: Property?,
        newValue: Object?,
        vararg vertexPropertyKeyValues: Object?
    ) : ElementPropertyChangedEvent(element, oldValue, newValue, vertexPropertyKeyValues) {
        @Override
        override fun fire(
            listener: MutationListener,
            element: Element?,
            oldValue: Property?,
            newValue: Object?,
            vararg vertexPropertyKeyValues: Object?
        ) {
            listener.vertexPropertyChanged(
                element as Vertex?,
                oldValue as VertexProperty?,
                newValue,
                vertexPropertyKeyValues
            )
        }
    }

    /**
     * Represents an action where a [Property] is modified on a [VertexProperty].
     */
    class VertexPropertyPropertyChangedEvent(element: VertexProperty?, oldValue: Property?, newValue: Object?) :
        ElementPropertyChangedEvent(element, oldValue, newValue) {
        @Override
        override fun fire(
            listener: MutationListener,
            element: Element?,
            oldValue: Property?,
            newValue: Object?,
            vararg vertexPropertyKeyValues: Object?
        ) {
            listener.vertexPropertyPropertyChanged(element as VertexProperty?, oldValue, newValue)
        }
    }

    /**
     * Represents an action where a [Property] is removed from a [VertexProperty].
     */
    class VertexPropertyPropertyRemovedEvent(element: VertexProperty?, removed: Property?) :
        ElementPropertyEvent(element, removed, null) {
        @Override
        override fun fire(
            listener: MutationListener,
            element: Element?,
            oldValue: Property?,
            newValue: Object?,
            vararg vertexPropertyKeyValues: Object?
        ) {
            listener.vertexPropertyPropertyRemoved(element as VertexProperty?, oldValue)
        }
    }

    /**
     * Represents an action where a [Property] is removed from a [Vertex].
     */
    class VertexPropertyRemovedEvent(vertexProperty: VertexProperty) : Event {
        private val vertexProperty: VertexProperty

        init {
            this.vertexProperty = vertexProperty
        }

        @Override
        override fun fireEvent(eventListeners: Iterator<MutationListener>) {
            while (eventListeners.hasNext()) {
                eventListeners.next().vertexPropertyRemoved(vertexProperty)
            }
        }
    }

    /**
     * Represents an action where a [Vertex] is removed from the [Graph].
     */
    class VertexRemovedEvent(vertex: Vertex) : Event {
        private val vertex: Vertex

        init {
            this.vertex = vertex
        }

        @Override
        override fun fireEvent(eventListeners: Iterator<MutationListener>) {
            while (eventListeners.hasNext()) {
                eventListeners.next().vertexRemoved(vertex)
            }
        }
    }

    /**
     * A base class for [Property] mutation events.
     */
    abstract class ElementPropertyChangedEvent(
        element: Element?,
        oldValue: Property?,
        newValue: Object?,
        vararg vertexPropertyKeyValues: Object?
    ) : ElementPropertyEvent(element, oldValue, newValue, *vertexPropertyKeyValues)

    /**
     * A base class for [Property] mutation events.
     */
    abstract class ElementPropertyEvent(
        element: Element?,
        oldValue: Property?,
        newValue: Object?,
        vararg vertexPropertyKeyValues: Object
    ) : Event {
        private val element: Element?
        private val oldValue: Property?
        private val newValue: Object?
        private val vertexPropertyKeyValues: Array<Object>

        init {
            this.element = element
            this.oldValue = oldValue
            this.newValue = newValue
            this.vertexPropertyKeyValues = vertexPropertyKeyValues
        }

        abstract fun fire(
            listener: MutationListener?,
            element: Element?,
            oldValue: Property?,
            newValue: Object?,
            vararg vertexPropertyKeyValues: Object?
        )

        @Override
        override fun fireEvent(eventListeners: Iterator<MutationListener?>) {
            while (eventListeners.hasNext()) {
                fire(eventListeners.next(), element, oldValue, newValue, vertexPropertyKeyValues)
            }
        }
    }
}