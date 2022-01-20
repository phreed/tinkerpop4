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
class AggregateGlobalStep<S>(traversal: Traversal.Admin?, @get:Override val sideEffectKey: String) :
    AbstractStep<S, S>(traversal), SideEffectCapable<Collection?, Collection?>, TraversalParent, ByModulating,
    LocalBarrier<S> {
    private var aggregateTraversal: Traversal.Admin<S, Object>? = null
    private var barrier: TraverserSet<S>

    init {
        barrier = traversal.getTraverserSetSupplier().get() as TraverserSet<S>
        this.getTraversal().getSideEffects()
            .registerIfAbsent(sideEffectKey, BulkSetSupplier.instance() as Supplier, Operator.addAll)
    }

    @Override
    override fun toString(): String {
        return StringFactory.stepString(this, sideEffectKey, aggregateTraversal)
    }

    @Override
    fun modulateBy(aggregateTraversal: Traversal.Admin<*, *>?) {
        this.aggregateTraversal = this.integrateChild(aggregateTraversal)
    }

    @Override
    fun replaceLocalChild(oldTraversal: Traversal.Admin<*, *>?, newTraversal: Traversal.Admin<*, *>?) {
        if (null != aggregateTraversal && aggregateTraversal.equals(oldTraversal)) aggregateTraversal =
            this.integrateChild(newTraversal)
    }

    @get:Override
    val localChildren: List<Any>
        get() = if (null == aggregateTraversal) Collections.emptyList() else Collections.singletonList(
            aggregateTraversal
        )

    @get:Override
    val requirements: Set<Any>
        get() = this.getSelfAndChildRequirements(TraverserRequirement.BULK, TraverserRequirement.SIDE_EFFECTS)

    @Override
    fun clone(): AggregateGlobalStep<S> {
        val clone = super.clone() as AggregateGlobalStep<S>
        clone.barrier = this.traversal.getTraverserSetSupplier().get() as TraverserSet<S>
        if (null != aggregateTraversal) clone.aggregateTraversal = aggregateTraversal.clone()
        return clone
    }

    @Override
    fun setTraversal(parentTraversal: Traversal.Admin<*, *>?) {
        super.setTraversal(parentTraversal)
        this.integrateChild(aggregateTraversal)
    }

    @Override
    override fun hashCode(): Int {
        var result = super.hashCode() xor sideEffectKey.hashCode()
        if (aggregateTraversal != null) result = result xor aggregateTraversal.hashCode()
        return result
    }

    @Override
    protected fun processNextStart(): Traverser.Admin<S> {
        if (barrier.isEmpty()) {
            processAllStarts()
        }
        return barrier.remove()
    }

    @Override
    fun processAllStarts() {
        if (this.starts.hasNext()) {
            val bulkSet: BulkSet<Object> = BulkSet()
            while (this.starts.hasNext()) {
                val traverser: Traverser.Admin<S> = this.starts.next()
                TraversalUtil.produce(traverser, aggregateTraversal)
                    .ifProductive { p -> bulkSet.add(p, traverser.bulk()) }
                traverser.setStepId(this.getNextStep().getId())

                // when barrier is reloaded, the traversers should be at the next step
                barrier.add(traverser)
            }
            this.getTraversal().getSideEffects().add(sideEffectKey, bulkSet)
        }
    }

    @Override
    fun hasNextBarrier(): Boolean {
        if (barrier.isEmpty()) {
            processAllStarts()
        }
        return !barrier.isEmpty()
    }

    @Override
    @Throws(NoSuchElementException::class)
    fun nextBarrier(): TraverserSet<S> {
        if (barrier.isEmpty()) {
            processAllStarts()
        }
        return if (barrier.isEmpty()) throw FastNoSuchElementException.instance() else {
            val temp: TraverserSet<S> = barrier
            barrier = this.traversal.getTraverserSetSupplier().get() as TraverserSet<S>
            temp
        }
    }

    @Override
    fun addBarrier(barrier: TraverserSet<S>?) {
        this.barrier.addAll(barrier)
    }

    @Override
    fun reset() {
        super.reset()
        barrier.clear()
    }
}