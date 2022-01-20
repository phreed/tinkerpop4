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
package org.apache.tinkerpop4.gremlin.process.traversal.util

import org.apache.tinkerpop4.gremlin.process.traversal.Bytecode

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class EmptyTraversal<S, E> protected constructor() : Traversal.Admin<S, E> {
    val bytecode: Bytecode
        get() = Bytecode()

    @Override
    fun asAdmin(): Traversal.Admin<S, E> {
        return this
    }

    @Override
    operator fun hasNext(): Boolean {
        return false
    }

    @Override
    operator fun next(): E {
        throw FastNoSuchElementException.instance()
    }

    @get:Override
    @set:Override
    var sideEffects: TraversalSideEffects
        get() = SIDE_EFFECTS
        set(sideEffects) {}

    @Override
    fun applyStrategies() {
    }

    @Override
    fun addStarts(starts: Iterator<Traverser.Admin<S>?>?) {
    }

    @Override
    fun addStart(start: Traverser.Admin<S>?) {
    }

    @Override
    fun <E2> addStep(step: Step<*, E2>?): Traversal.Admin<S, E2> {
        return instance<Any, Any>()
    }

    @get:Override
    val steps: List<Any>
        get() = Collections.emptyList()

    @Override
    fun clone(): EmptyTraversal<S, E> {
        return instance()
    }

    @get:Override
    val isLocked: Boolean
        get() = true

    @get:Override
    val traverserGenerator: TraverserGenerator?
        get() = null

    @get:Override
    @set:Override
    var strategies: TraversalStrategies
        get() = STRATEGIES
        set(traversalStrategies) {}

    @get:Override
    @set:Override
    var parent: TraversalParent
        get() = EmptyStep.instance()
        set(step) {}

    @Override
    @Throws(IllegalStateException::class)
    fun <S2, E2> addStep(index: Int, step: Step<*, *>?): Traversal.Admin<S2, E2> {
        return this as Traversal.Admin
    }

    @Override
    @Throws(IllegalStateException::class)
    fun <S2, E2> removeStep(index: Int): Traversal.Admin<S2, E2> {
        return this as Traversal.Admin
    }

    @Override
    override fun equals(`object`: Object?): Boolean {
        return `object` is EmptyTraversal<*, *>
    }

    @Override
    override fun hashCode(): Int {
        return -343564565
    }

    @get:Override
    val traverserRequirements: Set<Any>
        get() = Collections.emptySet()

    @get:Override
    @set:Override
    var graph: Optional<Graph>
        get() = Optional.empty()
        set(graph) {}

    companion object {
        private val INSTANCE: EmptyTraversal<*, *> = EmptyTraversal<Any?, Any?>()
        private val SIDE_EFFECTS: TraversalSideEffects = EmptyTraversalSideEffects.instance()
        private val STRATEGIES: TraversalStrategies = EmptyTraversalStrategies.instance()
        fun <A, B> instance(): EmptyTraversal<A, B> {
            return INSTANCE
        }
    }
}