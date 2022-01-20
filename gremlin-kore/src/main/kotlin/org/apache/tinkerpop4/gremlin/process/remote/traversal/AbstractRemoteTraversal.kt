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
package org.apache.tinkerpop4.gremlin.process.remote.traversal

import org.apache.tinkerpop4.gremlin.process.remote.traversal.step.map.RemoteStep

/**
 * This is a stub implementation for [RemoteTraversal] and requires that the [.nextTraverser] method
 * is implemented from [Traversal.Admin]. It is this method that gets called from [RemoteStep] when
 * the [Traversal] is iterated.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
abstract class AbstractRemoteTraversal<S, E> : RemoteTraversal<S, E> {
    /**
     * Note that internally `#nextTraverser()` is called from within a loop (specifically in
     * [AbstractStep.next] that breaks properly when a `NoSuchElementException` is thrown. In
     * other words the "results" should be iterated to force that failure.
     */
    @Override
    abstract fun nextTraverser(): Traverser.Admin<E>?

    @get:Override
    @set:Override
    var sideEffects: TraversalSideEffects
        get() {
            throw UnsupportedOperationException("Remote traversals do not support this method")
        }
        set(sideEffects) {
            throw UnsupportedOperationException("Remote traversals do not support this method")
        }

    @get:Override
    val bytecode: Bytecode
        get() {
            throw UnsupportedOperationException("Remote traversals do not support this method")
        }

    @get:Override
    val steps: List<Any>
        get() {
            throw UnsupportedOperationException("Remote traversals do not support this method")
        }

    @Override
    @Throws(IllegalStateException::class)
    fun <S2, E2> addStep(index: Int, step: Step<*, *>?): Admin<S2, E2> {
        throw UnsupportedOperationException("Remote traversals do not support this method")
    }

    @Override
    @Throws(IllegalStateException::class)
    fun <S2, E2> removeStep(index: Int): Admin<S2, E2> {
        throw UnsupportedOperationException("Remote traversals do not support this method")
    }

    @Override
    @Throws(IllegalStateException::class)
    fun applyStrategies() {
        throw UnsupportedOperationException("Remote traversals do not support this method")
    }

    @get:Override
    val traverserGenerator: TraverserGenerator
        get() {
            throw UnsupportedOperationException("Remote traversals do not support this method")
        }

    @get:Override
    val traverserRequirements: Set<Any>
        get() {
            throw UnsupportedOperationException("Remote traversals do not support this method")
        }

    @get:Override
    @set:Override
    var strategies: TraversalStrategies
        get() {
            throw UnsupportedOperationException("Remote traversals do not support this method")
        }
        set(strategies) {
            throw UnsupportedOperationException("Remote traversals do not support this method")
        }

    @get:Override
    @set:Override
    var parent: TraversalParent
        get() {
            throw UnsupportedOperationException("Remote traversals do not support this method")
        }
        set(step) {
            throw UnsupportedOperationException("Remote traversals do not support this method")
        }

    @Override
    fun clone(): Admin<S, E> {
        throw UnsupportedOperationException("Remote traversals do not support this method")
    }

    @get:Override
    val isLocked: Boolean
        get() {
            throw UnsupportedOperationException("Remote traversals do not support this method")
        }

    @get:Override
    @set:Override
    var graph: Optional<Graph>
        get() {
            throw UnsupportedOperationException("Remote traversals do not support this method")
        }
        set(graph) {
            throw UnsupportedOperationException("Remote traversals do not support this method")
        }
}