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
package org.apache.tinkerpop4.gremlin.process.traversal.lambda

import org.apache.tinkerpop4.gremlin.process.traversal.Bytecode

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
abstract class AbstractLambdaTraversal<S, E> : Traversal.Admin<S, E> {
    protected var bypassTraversal: Traversal.Admin<S, E>? = null
    fun setBypassTraversal(bypassTraversal: Traversal.Admin<S, E>) {
        this.bypassTraversal = bypassTraversal
    }

    fun getBypassTraversal(): Traversal.Admin<S, E> {
        return bypassTraversal
    }

    @get:Override
    val steps: List<Any>
        get() = if (null == bypassTraversal) Collections.emptyList() else bypassTraversal.getSteps()

    @get:Override
    val bytecode: Bytecode
        get() = if (null == bypassTraversal) Bytecode() else bypassTraversal.getBytecode()

    @Override
    fun reset() {
        if (null != bypassTraversal) bypassTraversal.reset()
    }

    @Override
    @Throws(IllegalStateException::class)
    fun <S2, E2> addStep(index: Int, step: Step<*, *>?): Traversal.Admin<S2, E2> {
        return if (null == bypassTraversal) this as Traversal.Admin<S2, E2> else bypassTraversal.addStep(index, step)
    }

    @Override
    @Throws(IllegalStateException::class)
    fun <S2, E2> removeStep(index: Int): Traversal.Admin<S2, E2> {
        return if (null == bypassTraversal) this as Traversal.Admin<S2, E2> else bypassTraversal.removeStep(index)
    }

    @Override
    @Throws(IllegalStateException::class)
    fun applyStrategies() {
        if (null != bypassTraversal) bypassTraversal.applyStrategies()
    }

    @get:Override
    val traverserGenerator: TraverserGenerator
        get() = if (null == bypassTraversal) B_O_TraverserGenerator.instance() else bypassTraversal.getTraverserGenerator()

    @get:Override
    @set:Override
    var sideEffects: TraversalSideEffects
        get() = if (null == bypassTraversal) EmptyTraversalSideEffects.instance() else bypassTraversal.getSideEffects()
        set(sideEffects) {
            if (null != bypassTraversal) bypassTraversal.setSideEffects(sideEffects)
        }

    @get:Override
    @set:Override
    var strategies: TraversalStrategies
        get() = if (null == bypassTraversal) EmptyTraversalStrategies.instance() else bypassTraversal.getStrategies()
        set(strategies) {
            if (null != bypassTraversal) bypassTraversal.setStrategies(strategies)
        }

    @get:Override
    @set:Override
    var parent: TraversalParent
        get() = if (null == bypassTraversal) EmptyStep.instance() else bypassTraversal.getParent()
        set(step) {
            if (null != bypassTraversal) {
                bypassTraversal.setParent(step)
                step.integrateChild(bypassTraversal)
            }
        }

    @Override
    fun clone(): Traversal.Admin<S, E> {
        return try {
            val clone = super.clone() as AbstractLambdaTraversal<S, E>
            if (null != bypassTraversal) clone.bypassTraversal = bypassTraversal.clone()
            clone
        } catch (e: CloneNotSupportedException) {
            throw IllegalStateException(e.getMessage(), e)
        }
    }

    @Override
    operator fun next(): E {
        if (null != bypassTraversal) return bypassTraversal.next()
        throw UnsupportedOperationException(
            "The " + this.getClass().getSimpleName().toString() + " can only be used as a predicate traversal"
        )
    }

    @Override
    fun nextTraverser(): Traverser.Admin<E> {
        if (null != bypassTraversal) return bypassTraversal.nextTraverser()
        throw UnsupportedOperationException(
            "The " + this.getClass().getSimpleName().toString() + " can only be used as a predicate traversal"
        )
    }

    @Override
    operator fun hasNext(): Boolean {
        return null == bypassTraversal || bypassTraversal.hasNext()
    }

    @Override
    fun addStart(start: Traverser.Admin<S>?) {
        if (null != bypassTraversal) bypassTraversal.addStart(start)
    }

    @get:Override
    val isLocked: Boolean
        get() = null == bypassTraversal || bypassTraversal.isLocked()

    /**
     * Implementations of this class can never be a root-level traversal as they are specialized implementations
     * intended to be child traversals by design.
     */
    @get:Override
    val isRoot: Boolean
        get() = false

    @get:Override
    @set:Override
    var graph: Optional<Graph>
        get() = if (null == bypassTraversal) Optional.empty() else bypassTraversal.getGraph()
        set(graph) {
            if (null != bypassTraversal) bypassTraversal.setGraph(graph)
        }

    @get:Override
    val traverserRequirements: Set<Any>
        get() = if (null == bypassTraversal) REQUIREMENTS else bypassTraversal.getTraverserRequirements()

    @Override
    override fun hashCode(): Int {
        return if (null == bypassTraversal) this.getClass().hashCode() else bypassTraversal.hashCode()
    }

    @Override
    override fun equals(`object`: Object): Boolean {
        return this.getClass().equals(`object`.getClass()) && this.hashCode() == `object`.hashCode()
    }

    companion object {
        private val REQUIREMENTS: Set<TraverserRequirement> = Collections.singleton(TraverserRequirement.OBJECT)
    }
}