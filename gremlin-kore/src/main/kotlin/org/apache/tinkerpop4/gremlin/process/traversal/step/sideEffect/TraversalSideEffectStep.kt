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
class TraversalSideEffectStep<S>(traversal: Traversal.Admin?, sideEffectTraversal: Traversal<S, *>) :
    SideEffectStep<S>(traversal), TraversalParent {
    private var sideEffectTraversal: Traversal.Admin<S, *>

    init {
        this.sideEffectTraversal = this.integrateChild(sideEffectTraversal.asAdmin())
    }

    @Override
    protected fun sideEffect(traverser: Traverser.Admin<S>?) {
        val iterator: Iterator<*> = TraversalUtil.applyAll(traverser, sideEffectTraversal)
        while (iterator.hasNext()) iterator.next()
    }

    @get:Override
    val localChildren: List<Any>
        get() = Collections.singletonList(sideEffectTraversal)

    @Override
    fun clone(): TraversalSideEffectStep<S> {
        val clone = super.clone() as TraversalSideEffectStep<S>
        clone.sideEffectTraversal = sideEffectTraversal.clone()
        return clone
    }

    @Override
    fun setTraversal(parentTraversal: Traversal.Admin<*, *>?) {
        super.setTraversal(parentTraversal)
        this.integrateChild(sideEffectTraversal)
    }

    @Override
    override fun toString(): String {
        return StringFactory.stepString(this, sideEffectTraversal)
    }

    @Override
    override fun hashCode(): Int {
        return super.hashCode() xor sideEffectTraversal.hashCode()
    }

    @get:Override
    val requirements: Set<Any>
        get() = this.getSelfAndChildRequirements()
}