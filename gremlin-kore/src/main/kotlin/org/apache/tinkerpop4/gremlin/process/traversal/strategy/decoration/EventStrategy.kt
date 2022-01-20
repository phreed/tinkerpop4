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
package org.apache.tinkerpop4.gremlin.process.traversal.strategy.decoration

import org.apache.tinkerpop4.gremlin.process.traversal.Traversal

/**
 * A strategy that raises events when [Mutating] steps are encountered and successfully executed.
 *
 *
 * Note that this implementation requires a [Graph] on the [Traversal] instance.  If that is not present
 * an `IllegalStateException` will be thrown. Finally, this strategy is meant for use on the JVM only and has
 * no analogous implementation in other Gremlin Language Variants.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class EventStrategy private constructor(builder: Builder) : AbstractTraversalStrategy<DecorationStrategy?>(),
    DecorationStrategy {
    private val eventQueue: EventQueue
    val detachment: Detachment

    init {
        eventQueue = builder.eventQueue
        eventQueue.setListeners(builder.listeners)
        detachment = builder.detachment
    }

    /**
     * Applies the appropriate detach operation to elements that will be raised in mutation events.
     */
    fun <R> detach(attached: R): R {
        return detachment.detach(attached)
    }

    @Override
    fun apply(traversal: Traversal.Admin<*, *>?) {
        val callback = EventStrategyCallback(eventQueue)
        TraversalHelper.getStepsOfAssignableClass(Mutating::class.java, traversal)
            .forEach { s -> s.getMutatingCallbackRegistry().addCallback(callback) }
    }

    inner class EventStrategyCallback(private val eventQueue: EventQueue) : EventCallback<Event?>, Serializable {
        @Override
        fun accept(event: Event?) {
            eventQueue.addEvent(event)
        }
    }

    class Builder internal constructor() {
        private val listeners: List<MutationListener> = ArrayList()
        private var eventQueue: EventQueue = DefaultEventQueue()
        private var detachment = Detachment.DETACHED_WITH_PROPERTIES
        fun addListener(listener: MutationListener?): Builder {
            listeners.add(listener)
            return this
        }

        fun eventQueue(eventQueue: EventQueue): Builder {
            this.eventQueue = eventQueue
            return this
        }

        /**
         * Configures the method of detachment for element provided in mutation callback events. The default is
         * [Detachment.DETACHED_WITH_PROPERTIES].
         */
        fun detach(detachment: Detachment): Builder {
            this.detachment = detachment
            return this
        }

        fun create(): EventStrategy {
            return EventStrategy(this)
        }
    }

    /**
     * A common interface for detachment.
     */
    interface Detacher {
        fun detach(`object`: Object?): Object?
    }

    /**
     * Options for detaching elements from the graph during eventing.
     */
    enum class Detachment : Detacher {
        /**
         * Does not detach the element from the graph. It should be noted that if this option is used with
         * transactional graphs new transactions may be opened if these elements are accessed after a `commit()`
         * is called.
         */
        NONE {
            @Override
            override fun detach(`object`: Object): Object {
                return `object`
            }
        },

        /**
         * Uses [DetachedFactory] to detach and includes properties of elements that have them.
         */
        DETACHED_WITH_PROPERTIES {
            @Override
            override fun detach(`object`: Object?): Object {
                return DetachedFactory.detach(`object`, true)
            }
        },

        /**
         * Uses [DetachedFactory] to detach and does not include properties of elements that have them.
         */
        DETACHED_NO_PROPERTIES {
            @Override
            override fun detach(`object`: Object?): Object {
                return DetachedFactory.detach(`object`, false)
            }
        },

        /**
         * Uses [ReferenceFactory] to detach which only includes id and label of elements.
         */
        REFERENCE {
            @Override
            override fun detach(`object`: Object?): Object {
                return ReferenceFactory.detach(`object`)
            }
        }
    }

    /**
     * Gathers messages from callbacks and fires them to listeners.  When the event is sent to the listener is
     * up to the implementation of this interface.
     */
    interface EventQueue {
        /**
         * Provide listeners to the queue that were given to the [EventStrategy] on construction.
         */
        fun setListeners(listeners: List<MutationListener?>?)

        /**
         * Add an event to the event queue.
         */
        fun addEvent(evt: Event?)
    }

    /**
     * Immediately notifies all listeners as events arrive.
     */
    class DefaultEventQueue : EventQueue {
        private var listeners: List<MutationListener> = Collections.emptyList()
        @Override
        override fun setListeners(listeners: List<MutationListener>) {
            this.listeners = listeners
        }

        @Override
        override fun addEvent(evt: Event) {
            evt.fireEvent(listeners.iterator())
        }
    }

    /**
     * Stores events in a queue that builds up until the transaction is committed which then fires them in the order
     * they were received.
     */
    class TransactionalEventQueue(graph: Graph) : EventQueue {
        private val eventQueue: ThreadLocal<Deque<Event>> = object : ThreadLocal<Deque<Event?>?>() {
            protected fun initialValue(): Deque<Event> {
                return ArrayDeque()
            }
        }
        private var listeners: List<MutationListener> = Collections.emptyList()

        init {
            if (!graph.features().graph().supportsTransactions()) throw IllegalStateException(
                String.format(
                    "%s requires the graph to support transactions",
                    EventStrategy::class.java.getName()
                )
            )

            // since this is a transactional graph events are enqueued so the events should be fired/reset only after
            // transaction is committed/rolled back as tied to a graph transaction
            graph.tx().addTransactionListener { status ->
                if (status === Transaction.Status.COMMIT) fireEventQueue() else if (status === Transaction.Status.ROLLBACK) resetEventQueue() else throw RuntimeException(
                    String.format("The %s is not aware of this status: %s", EventQueue::class.java.getName(), status)
                )
            }
        }

        override fun addEvent(evt: Event?) {
            eventQueue.get().add(evt)
        }

        @Override
        override fun setListeners(listeners: List<MutationListener>) {
            this.listeners = listeners
        }

        private fun resetEventQueue() {
            eventQueue.set(ArrayDeque())
        }

        private fun fireEventQueue() {
            val deque: Deque<Event> = eventQueue.get()
            var event: Event = deque.pollFirst()
            while (event != null) {
                event.fireEvent(listeners.iterator())
                event = deque.pollFirst()
            }
        }
    }

    companion object {
        fun build(): Builder {
            return Builder()
        }
    }
}