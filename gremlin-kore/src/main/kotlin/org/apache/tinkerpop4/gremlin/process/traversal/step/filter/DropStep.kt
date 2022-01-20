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
package org.apache.tinkerpop4.gremlin.process.traversal.step.filter

import org.apache.tinkerpop4.gremlin.process.traversal.Traversal

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class DropStep<S>(traversal: Traversal.Admin?) : FilterStep<S>(traversal), Mutating<Event?> {
    private var callbackRegistry: CallbackRegistry<Event>? = null
    @Override
    protected fun filter(traverser: Traverser.Admin<S>): Boolean {
        val s: S = traverser.get()
        if (s is Element) {
            val toRemove: Element = s as Element
            if (callbackRegistry != null && !callbackRegistry.getCallbacks().isEmpty()) {
                val eventStrategy: EventStrategy =
                    getTraversal().getStrategies().getStrategy(EventStrategy::class.java).get()
                val removeEvent: Event
                if (s is Vertex) removeEvent =
                    VertexRemovedEvent(eventStrategy.detach(s as Vertex)) else if (s is Edge) removeEvent =
                    EdgeRemovedEvent(eventStrategy.detach(s as Edge)) else if (s is VertexProperty) removeEvent =
                    VertexPropertyRemovedEvent(eventStrategy.detach(s as VertexProperty)) else throw IllegalStateException(
                    "The incoming object is not removable: $s"
                )
                callbackRegistry.getCallbacks().forEach { c -> c.accept(removeEvent) }
            }
            toRemove.remove()
        } else if (s is Property) {
            val toRemove: Property = s as Property
            if (callbackRegistry != null && !callbackRegistry.getCallbacks().isEmpty()) {
                val eventStrategy: EventStrategy =
                    getTraversal().getStrategies().getStrategy(EventStrategy::class.java).get()
                val removeEvent: ElementPropertyEvent
                if (toRemove.element() is Edge) removeEvent = EdgePropertyRemovedEvent(
                    eventStrategy.detach(toRemove.element() as Edge),
                    eventStrategy.detach(toRemove)
                ) else if (toRemove.element() is VertexProperty) removeEvent = VertexPropertyPropertyRemovedEvent(
                    eventStrategy.detach(toRemove.element() as VertexProperty),
                    eventStrategy.detach(toRemove)
                ) else throw IllegalStateException(
                    "The incoming object is not removable: $s"
                )
                callbackRegistry.getCallbacks().forEach { c -> c.accept(removeEvent) }
            }
            toRemove.remove()
        } else throw IllegalStateException("The incoming object is not removable: $s")
        return false
    }

    @get:Override
    val mutatingCallbackRegistry: CallbackRegistry<Event>?
        get() {
            if (null == callbackRegistry) callbackRegistry = ListCallbackRegistry()
            return callbackRegistry
        }

    /**
     * This method doesn't do anything as `drop()` doesn't take property mutation arguments.
     */
    @Override
    fun configure(vararg keyValues: Object?) {
        // do nothing
    }

    @get:Override
    val parameters: Parameters
        get() = Parameters.EMPTY
}