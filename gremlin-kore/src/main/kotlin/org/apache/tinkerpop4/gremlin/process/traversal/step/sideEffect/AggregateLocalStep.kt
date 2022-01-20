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

import org.apache.tinkerpop4.gremlin.process.traversal.Operator

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class AggregateLocalStep<S>(traversal: Traversal.Admin?, @get:Override val sideEffectKey: String) :
    SideEffectStep<S>(traversal), SideEffectCapable<Collection?, Collection?>, TraversalParent, ByModulating {
    private var storeTraversal: Traversal.Admin<S, Object>? = null

    init {
        this.getTraversal().getSideEffects()
            .registerIfAbsent(sideEffectKey, BulkSetSupplier.instance() as Supplier, Operator.addAll)
    }

    @Override
    protected fun sideEffect(traverser: Traverser.Admin<S>) {
        val bulkSet: BulkSet<Object> = BulkSet()
        TraversalUtil.produce(traverser, storeTraversal).ifProductive { p -> bulkSet.add(p, traverser.bulk()) }
        this.getTraversal().getSideEffects().add(sideEffectKey, bulkSet)
    }

    @Override
    override fun toString(): String {
        return StringFactory.stepString(this, sideEffectKey, storeTraversal)
    }

    @get:Override
    val localChildren: List<Any>
        get() = if (null == storeTraversal) Collections.emptyList() else Collections.singletonList(storeTraversal)

    @Override
    fun modulateBy(storeTraversal: Traversal.Admin<*, *>?) {
        this.storeTraversal = this.integrateChild(storeTraversal)
    }

    @Override
    fun replaceLocalChild(oldTraversal: Traversal.Admin<*, *>?, newTraversal: Traversal.Admin<*, *>?) {
        if (null != storeTraversal && storeTraversal.equals(oldTraversal)) storeTraversal =
            this.integrateChild(newTraversal)
    }

    @get:Override
    val requirements: Set<Any>
        get() = this.getSelfAndChildRequirements(TraverserRequirement.SIDE_EFFECTS, TraverserRequirement.BULK)

    @Override
    fun clone(): AggregateLocalStep<S> {
        val clone = super.clone() as AggregateLocalStep<S>
        if (null != storeTraversal) clone.storeTraversal = storeTraversal.clone()
        return clone
    }

    @Override
    fun setTraversal(parentTraversal: Traversal.Admin<*, *>?) {
        super.setTraversal(parentTraversal)
        this.integrateChild(storeTraversal)
    }

    @Override
    override fun hashCode(): Int {
        var result = super.hashCode() xor sideEffectKey.hashCode()
        if (storeTraversal != null) result = result xor storeTraversal.hashCode()
        return result
    }
}