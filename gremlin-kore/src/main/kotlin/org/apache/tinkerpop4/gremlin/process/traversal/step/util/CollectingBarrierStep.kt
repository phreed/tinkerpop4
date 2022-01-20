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
package org.apache.tinkerpop4.gremlin.process.traversal.step.util

import org.apache.tinkerpop4.gremlin.process.computer.MemoryComputeKey

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
abstract class CollectingBarrierStep<S>(traversal: Traversal.Admin?, maxBarrierSize: Int) :
    AbstractStep<S, S>(traversal), Barrier<TraverserSet<S>?> {
    protected var traverserSet: TraverserSet<S>
    private val maxBarrierSize: Int
    private var barrierConsumed = false

    constructor(traversal: Traversal.Admin?) : this(traversal, Integer.MAX_VALUE) {}

    init {
        traverserSet = traversal.getTraverserSetSupplier().get() as TraverserSet<S>
        this.maxBarrierSize = maxBarrierSize
    }

    abstract fun barrierConsumer(traverserSet: TraverserSet<S>?)

    @get:Override
    val requirements: Set<Any>
        get() = Collections.singleton(TraverserRequirement.BULK)

    @Override
    fun processAllStarts() {
        if (this.starts.hasNext()) {
            if (Integer.MAX_VALUE === maxBarrierSize) {
                this.starts.forEachRemaining(traverserSet::add)
            } else {
                while (this.starts.hasNext() && traverserSet.size() < maxBarrierSize) {
                    traverserSet.add(this.starts.next())
                }
            }
        }
    }

    @Override
    fun hasNextBarrier(): Boolean {
        processAllStarts()
        return !traverserSet.isEmpty()
    }

    @Override
    @Throws(NoSuchElementException::class)
    fun nextBarrier(): TraverserSet<S> {
        processAllStarts()
        return if (traverserSet.isEmpty()) throw FastNoSuchElementException.instance() else {
            val temp: TraverserSet<S> = this.traversal.getTraverserSetSupplier().get() as TraverserSet<S>
            IteratorUtils.removeOnNext(traverserSet.iterator()).forEachRemaining { t ->
                DetachedFactory.detach(t, true) // this should be dynamic
                temp.add(t)
            }
            temp
        }
    }

    @Override
    fun addBarrier(barrier: TraverserSet<S>) {
        barrier.forEach { traverser -> traverser.setSideEffects(this.getTraversal().getSideEffects()) }
        traverserSet.addAll(barrier)
        barrierConsumed = false
    }

    @Override
    fun processNextStart(): Traverser.Admin<S> {
        if (traverserSet.isEmpty() && this.starts.hasNext()) {
            processAllStarts()
            barrierConsumed = false
        }
        //
        if (!barrierConsumed) {
            barrierConsumer(traverserSet)
            barrierConsumed = true
        }
        return ProjectedTraverser.tryUnwrap(traverserSet.remove())
    }

    @Override
    fun clone(): CollectingBarrierStep<S> {
        val clone = super.clone() as CollectingBarrierStep<S>
        clone.traverserSet = this.traversal.getTraverserSetSupplier().get() as TraverserSet<S>
        clone.barrierConsumed = false
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
        traverserSet.clear()
    }

    @get:Override
    val memoryComputeKey: MemoryComputeKey<TraverserSet<S>>
        get() = MemoryComputeKey.of(this.getId(), Operator.addAll as BinaryOperator, false, true)
}