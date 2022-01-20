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
package org.apache.tinkerpop4.gremlin.tinkercat.process.computer

import org.apache.tinkerpop4.gremlin.process.computer.MessageCombiner
import org.apache.tinkerpop4.gremlin.process.computer.MessageScope
import org.apache.tinkerpop4.gremlin.process.computer.Messenger
import org.apache.tinkerpop4.gremlin.process.computer.util.VertexProgramHelper
import org.apache.tinkerpop4.gremlin.process.traversal.Traversal
import org.apache.tinkerpop4.gremlin.process.traversal.step.map.VertexStep
import org.apache.tinkerpop4.gremlin.process.traversal.util.TraversalHelper
import org.apache.tinkerpop4.gremlin.structure.Direction
import org.apache.tinkerpop4.gremlin.structure.Edge
import org.apache.tinkerpop4.gremlin.structure.Vertex
import org.apache.tinkerpop4.gremlin.util.iterator.MultiIterator
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.function.Consumer
import java.util.stream.Stream
import java.util.stream.StreamSupport

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class TinkerMessenger<M>(
    private val vertex: Vertex,
    private val messageBoard: TinkerMessageBoard<M>,
    combiner: Optional<MessageCombiner<M>?>
) : Messenger<M> {
    private val combiner: MessageCombiner<M>?

    init {
        this.combiner = if (combiner.isPresent) combiner.get() else null
    }

    override fun receiveMessages(): Iterator<M> {
        val multiIterator = MultiIterator<M>()
        for (messageScope in messageBoard.receiveMessages.keys) {
//        for (final MessageScope messageScope in this.messageBoard.previousMessageScopes) {
            if (messageScope is MessageScope.Local<*>) {
                val localMessageScope = messageScope as MessageScope.Local<M>
                val incidentTraversal = setVertexStart<Traversal.Admin<Vertex?, Edge?>>(
                    localMessageScope.getIncidentTraversal().get().asAdmin(), vertex
                )
                val direction = getDirection(incidentTraversal)
                val edge = arrayOfNulls<Edge>(1) // simulates storage side-effects available in Gremlin, but not Java streams
                multiIterator.addIterator(StreamSupport.stream(
                    Spliterators.spliteratorUnknownSize(
                        VertexProgramHelper.reverse(
                            incidentTraversal.asAdmin()
                        ), Spliterator.IMMUTABLE or Spliterator.SIZED
                    ), false
                )
                    .filter { it != null }
                    .map { e: Edge? ->
                        edge[0] = e
                        val vv = when {
                            direction == Direction.IN -> e?.vertices(direction)?.next()
                            direction == Direction.OUT -> e?.vertices(direction)?.next()
                            e!!.outVertex() === vertex -> e?.inVertex()
                            else -> e?.outVertex()
                        }
                        messageBoard.receiveMessages[messageScope]!![vv]
                    }
                    .filter { q: Queue<M>? -> null != q }
                    .flatMap<M> { obj -> obj?.stream() }
                    .map { message: M -> localMessageScope.getEdgeFunction().apply(message, edge[0]) }
                    .iterator())
            } else {
                multiIterator.addIterator(
                    Stream.of(vertex)
                        .map { key: Any? -> messageBoard.receiveMessages[messageScope]!![key] }
                        .filter { q: Queue<M>? -> null != q }
                        .flatMap<M> { obj -> obj?.stream() }
                        .iterator())
            }
        }
        return multiIterator
    }

    override fun sendMessage(messageScope: MessageScope, message: M) {
//        this.messageBoard.currentMessageScopes.add(messageScope);
        if (messageScope is MessageScope.Local<*>) {
            addMessage(vertex, message, messageScope)
        } else {
            (messageScope as MessageScope.Global).vertices()
                .forEach(Consumer { v: Vertex -> addMessage(v, message, messageScope) })
        }
    }

    private fun addMessage(vertex: Vertex, message: M, messageScope: MessageScope) {
        messageBoard.sendMessages.compute(messageScope) { _, messages ->
            messages ?: ConcurrentHashMap<Any?, Any?>() as MutableMap<Vertex, Queue<M>>
        }
        messageBoard.sendMessages[messageScope]?.compute(vertex) { _, queue ->
            val newQueue = queue ?: ConcurrentLinkedQueue<Any>() as Queue<M>
            newQueue.add(if (null != combiner && !newQueue.isEmpty()) combiner.combine(newQueue.remove(), message) else message)
            newQueue
        }
    }

    companion object {
        ///////////
        private fun <T : Traversal.Admin<Vertex?, Edge?>?> setVertexStart(
            incidentTraversal: Traversal.Admin<Vertex, Edge>,
            vertex: Vertex
        ): T {
            incidentTraversal.addStart(
                incidentTraversal.traverserGenerator.generate(
                    vertex,
                    incidentTraversal.startStep,
                    1L
                )
            )
            return incidentTraversal as T
        }

        private fun getDirection(incidentTraversal: Traversal.Admin<Vertex?, Edge?>): Direction {
            val step = TraversalHelper.getLastStepOfAssignableClass(
                VertexStep::class.java, incidentTraversal
            ).get()
            return step.direction
        }
    }
}