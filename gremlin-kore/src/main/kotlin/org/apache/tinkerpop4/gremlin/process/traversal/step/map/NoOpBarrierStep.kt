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

import org.apache.tinkerpop4.gremlin.process.traversal.Traversal

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class NoOpBarrierStep<S>(traversal: Traversal.Admin?, val maxBarrierSize: Int) : AbstractStep<S, S>(traversal),
    LocalBarrier<S> {
    private var barrier: TraverserSet<S>

    constructor(traversal: Traversal.Admin?) : this(traversal, Integer.MAX_VALUE) {}

    init {
        barrier = traversal.getTraverserSetSupplier().get() as TraverserSet<S>
    }

    @Override
    @Throws(NoSuchElementException::class)
    protected fun processNextStart(): Traverser.Admin<S> {
        if (barrier.isEmpty()) processAllStarts()
        return barrier.remove()
    }

    @get:Override
    val requirements: Set<Any>
        get() = Collections.singleton(TraverserRequirement.BULK)

    @Override
    fun processAllStarts() {
        while ((maxBarrierSize == Integer.MAX_VALUE || barrier.size() < maxBarrierSize) && this.starts.hasNext()) {
            val traverser: Traverser.Admin<S> = this.starts.next()
            traverser.setStepId(
                this.getNextStep().getId()
            ) // when barrier is reloaded, the traversers should be at the next step
            barrier.add(traverser)
        }
    }

    @Override
    fun hasNextBarrier(): Boolean {
        processAllStarts()
        return !barrier.isEmpty()
    }

    @Override
    @Throws(NoSuchElementException::class)
    fun nextBarrier(): TraverserSet<S> {
        processAllStarts()
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
    fun clone(): NoOpBarrierStep<S> {
        val clone = super.clone() as NoOpBarrierStep<S>
        clone.barrier = this.traversal.getTraverserSetSupplier().get() as TraverserSet<S>
        return clone
    }

    @Override
    override fun toString(): String {
        return StringFactory.stepString(this, if (maxBarrierSize == Integer.MAX_VALUE) null else maxBarrierSize)
    }

    @Override
    override fun hashCode(): Int {
        return super.hashCode() xor maxBarrierSize
    }

    @Override
    fun reset() {
        super.reset()
        barrier.clear()
    }
}