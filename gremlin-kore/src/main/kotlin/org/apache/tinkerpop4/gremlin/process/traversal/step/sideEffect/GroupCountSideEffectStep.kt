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

import org.apache.tinkerpop4.gremlin.process.traversal.Traversal

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class GroupCountSideEffectStep<S, E>(traversal: Traversal.Admin?, @get:Override val sideEffectKey: String) :
    SideEffectStep<S>(traversal), SideEffectCapable<Map<E, Long?>?, Map<E, Long?>?>, TraversalParent, ByModulating {
    private var keyTraversal: Traversal.Admin<S, E>? = null

    init {
        this.getTraversal().asAdmin().getSideEffects()
            .registerIfAbsent(sideEffectKey, HashMapSupplier.instance(), GroupCountStep.GroupCountBiOperator.instance())
    }

    @Override
    protected fun sideEffect(traverser: Traverser.Admin<S>) {
        val map: Map<E, Long> = HashMap(1)
        map.put(TraversalUtil.applyNullable(traverser, keyTraversal), traverser.bulk())
        this.getTraversal().getSideEffects().add(sideEffectKey, map)
    }

    @Override
    override fun toString(): String {
        return StringFactory.stepString(this, sideEffectKey, keyTraversal)
    }

    @Override
    fun addLocalChild(groupTraversal: Traversal.Admin<*, *>?) {
        keyTraversal = this.integrateChild(groupTraversal)
    }

    @get:Override
    val localChildren: List<Any>
        get() = if (null == keyTraversal) Collections.emptyList() else Collections.singletonList(keyTraversal)

    @get:Override
    val requirements: Set<Any>
        get() = this.getSelfAndChildRequirements(TraverserRequirement.BULK, TraverserRequirement.SIDE_EFFECTS)

    @Override
    fun clone(): GroupCountSideEffectStep<S, E> {
        val clone = super.clone() as GroupCountSideEffectStep<S, E>
        if (null != keyTraversal) clone.keyTraversal = keyTraversal.clone()
        return clone
    }

    @Override
    fun setTraversal(parentTraversal: Traversal.Admin<*, *>?) {
        super.setTraversal(parentTraversal)
        this.integrateChild(keyTraversal)
    }

    @Override
    override fun hashCode(): Int {
        var result = super.hashCode() xor sideEffectKey.hashCode()
        if (keyTraversal != null) result = result xor keyTraversal.hashCode()
        return result
    }

    @Override
    @Throws(UnsupportedOperationException::class)
    fun modulateBy(keyTraversal: Traversal.Admin<*, *>?) {
        this.keyTraversal = this.integrateChild(keyTraversal)
    }

    @Override
    fun replaceLocalChild(oldTraversal: Traversal.Admin<*, *>?, newTraversal: Traversal.Admin<*, *>?) {
        if (null != keyTraversal && keyTraversal.equals(oldTraversal)) keyTraversal = this.integrateChild(newTraversal)
    }
}