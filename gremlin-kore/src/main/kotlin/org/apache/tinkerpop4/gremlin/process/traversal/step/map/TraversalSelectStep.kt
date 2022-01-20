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
package org.apache.tinkerpop4.gremlin.process.traversal.step.map

import org.apache.tinkerpop4.gremlin.process.traversal.Pop

/**
 * @author Daniel Kuppitz (http://gremlin.guru)
 */
class TraversalSelectStep<S, E>(traversal: Traversal.Admin?, pop: Pop?, keyTraversal: Traversal<S, E>) :
    MapStep<S, E>(traversal), TraversalParent, PathProcessor, ByModulating, Scoping {
    private val pop: Pop?
    private var keyTraversal: Traversal.Admin<S, E>?
    private var selectTraversal: Traversal.Admin<E, E>? = null
    private var keepLabels: Set<String>? = null

    init {
        this.pop = pop
        this.keyTraversal = this.integrateChild(keyTraversal.asAdmin())
    }

    @Override
    protected fun processNextStart(): Traverser.Admin<E> {
        val traverser: Traverser.Admin<S> = this.starts.next()
        val keyIterator: Iterator<E> = TraversalUtil.applyAll(traverser, keyTraversal)
        return if (keyIterator.hasNext()) {
            val key = keyIterator.next()
            try {
                val end: E = getScopeValue(pop, key, traverser)
                val outTraverser: Traverser.Admin<E> =
                    traverser.split(if (null == end) null else TraversalUtil.applyNullable(end, selectTraversal), this)
                if (this.getTraversal().getParent() !is MatchStep) {
                    PathProcessor.processTraverserPathLabels(outTraverser, keepLabels)
                }
                outTraverser
            } catch (nfe: KeyNotFoundException) {
                EmptyTraverser.instance()
            }
        } else {
            EmptyTraverser.instance()
        }
    }

    // can't return scope keys here because they aren't known prior to traversal execution and this method is
    // used at strategy application time. not getting any test failures as a result of returning empty. assuming
    // that strategies don't use Scoping in a way that requires the keys to be known and if they aren't doesn't
    // hose the whole traversal. in the worst case, strategies will hopefully just leave steps alone rather than
    // make their own assumptions that the step is not selecting anything. if that is happening somehow we might
    // need to modify Scoping to better suite this runtime evaluation of the key.
    @get:Override
    val scopeKeys: Set<String>
        get() =// can't return scope keys here because they aren't known prior to traversal execution and this method is
        // used at strategy application time. not getting any test failures as a result of returning empty. assuming
        // that strategies don't use Scoping in a way that requires the keys to be known and if they aren't doesn't
        // hose the whole traversal. in the worst case, strategies will hopefully just leave steps alone rather than
        // make their own assumptions that the step is not selecting anything. if that is happening somehow we might
            // need to modify Scoping to better suite this runtime evaluation of the key.
            Collections.emptySet()

    @Override
    override fun toString(): String {
        return StringFactory.stepString(this, pop, keyTraversal, selectTraversal)
    }

    @Override
    fun clone(): TraversalSelectStep<S, E> {
        val clone = super.clone() as TraversalSelectStep<S, E>
        clone.keyTraversal = keyTraversal.clone()
        if (null != selectTraversal) clone.selectTraversal = selectTraversal.clone()
        return clone
    }

    @Override
    fun setTraversal(parentTraversal: Traversal.Admin<*, *>?) {
        super.setTraversal(parentTraversal)
        this.integrateChild(selectTraversal)
    }

    @Override
    override fun hashCode(): Int {
        var result = super.hashCode() xor keyTraversal.hashCode()
        if (null != selectTraversal) result = result xor selectTraversal.hashCode()
        if (null != pop) result = result xor pop.hashCode()
        return result
    }

    @get:Override
    val localChildren: List<Any>
        get() {
            if (null == selectTraversal && null == keyTraversal) return Collections.emptyList()
            val children: List<Traversal.Admin<*, *>> = ArrayList()
            if (selectTraversal != null) children.add(selectTraversal)
            if (keyTraversal != null) children.add(keyTraversal)
            return children
        }

    @Override
    fun removeLocalChild(traversal: Traversal.Admin<*, *>) {
        if (selectTraversal === traversal) selectTraversal = null
        if (keyTraversal === traversal) keyTraversal = null
    }

    @Override
    fun modulateBy(selectTraversal: Traversal.Admin<*, *>?) {
        this.selectTraversal = this.integrateChild(selectTraversal)
    }

    @get:Override
    val requirements: Set<Any>
        get() = this.getSelfAndChildRequirements(
            TraverserRequirement.OBJECT,
            TraverserRequirement.SIDE_EFFECTS,
            TraverserRequirement.PATH
        )

    fun getPop(): Pop? {
        return pop
    }

    @Override
    fun setKeepLabels(keepLabels: Set<String?>?) {
        this.keepLabels = HashSet(keepLabels)
    }

    @Override
    fun getKeepLabels(): Set<String>? {
        return keepLabels
    }
}