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
package org.apache.tinkerpop4.gremlin.process.traversal.step.sideEffect

import org.apache.tinkerpop4.gremlin.process.traversal.Path

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class TreeSideEffectStep<S>(traversal: Traversal.Admin?, @get:Override val sideEffectKey: String) :
    SideEffectStep<S>(traversal), SideEffectCapable<Tree?, Tree?>, TraversalParent, ByModulating, PathProcessor {
    private var traversalRing: TraversalRing<Object, Object>
    private var keepLabels: Set<String>? = null

    init {
        traversalRing = TraversalRing()
        this.getTraversal().getSideEffects()
            .registerIfAbsent(sideEffectKey, TreeSupplier.instance() as Supplier, TreeStep.TreeBiOperator.instance())
    }

    @Override
    protected fun sideEffect(traverser: Traverser.Admin<S>) {
        val root = Tree()
        var depth: Tree = root
        val path: Path = traverser.path()
        for (i in 0 until path.size()) {
            val `object`: Object = TraversalUtil.applyNullable(path.< Object > get < Object ? > i, traversalRing.next())
            if (!depth.containsKey(`object`)) depth.put(`object`, Tree())
            depth = depth.get(`object`) as Tree
        }
        traversalRing.reset()
        this.getTraversal().getSideEffects().add(sideEffectKey, root)
    }

    @Override
    protected fun processNextStart(): Traverser.Admin<S> {
        return PathProcessor.processTraverserPathLabels(super.processNextStart(), keepLabels)
    }

    @Override
    fun reset() {
        super.reset()
        traversalRing.reset()
    }

    @Override
    override fun toString(): String {
        return StringFactory.stepString(this, sideEffectKey, traversalRing)
    }

    @Override
    fun clone(): TreeSideEffectStep<S> {
        val clone = super.clone() as TreeSideEffectStep<S>
        clone.traversalRing = traversalRing.clone()
        return clone
    }

    @Override
    fun setTraversal(parentTraversal: Traversal.Admin<*, *>?) {
        super.setTraversal(parentTraversal)
        traversalRing.getTraversals().forEach(this::integrateChild)
    }

    @Override
    override fun hashCode(): Int {
        return super.hashCode() xor sideEffectKey.hashCode() xor traversalRing.hashCode()
    }

    @get:Override
    val localChildren: List<Any>
        get() = traversalRing.getTraversals()

    @Override
    fun modulateBy(treeTraversal: Traversal.Admin<*, *>?) {
        traversalRing.addTraversal(this.integrateChild(treeTraversal))
    }

    @Override
    fun replaceLocalChild(oldTraversal: Traversal.Admin<*, *>?, newTraversal: Traversal.Admin<*, *>?) {
        traversalRing.replaceTraversal(
            oldTraversal as Traversal.Admin<Object?, Object?>?,
            newTraversal as Traversal.Admin<Object?, Object?>?
        )
    }

    @get:Override
    val requirements: Set<Any>
        get() = this.getSelfAndChildRequirements(TraverserRequirement.PATH, TraverserRequirement.SIDE_EFFECTS)

    @Override
    fun setKeepLabels(keepLabels: Set<String?>?) {
        this.keepLabels = HashSet(keepLabels)
    }

    @Override
    fun getKeepLabels(): Set<String>? {
        return keepLabels
    }
}