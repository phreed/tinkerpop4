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
package org.apache.tinkerpop4.gremlin.process.traversal.step.filter

import org.apache.tinkerpop4.gremlin.process.computer.MemoryComputeKey

/**
 * @author Bob Briody (http://bobbriody.com)
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class RangeGlobalStep<S>(traversal: Traversal.Admin?, low: Long, high: Long) : FilterStep<S>(traversal), Ranging,
    Bypassing, Barrier<TraverserSet<S>?> {
    @get:Override
    val lowRange: Long

    @get:Override
    val highRange: Long
    private var counter: AtomicLong = AtomicLong(0L)
    private var bypass = false

    init {
        if (low != -1L && high != -1L && low > high) {
            throw IllegalArgumentException("Not a legal range: [$low, $high]")
        }
        lowRange = low
        highRange = high
    }

    @Override
    protected fun filter(traverser: Traverser.Admin<S>): Boolean {
        if (bypass) return true
        if (highRange != -1L && counter.get() >= highRange) {
            throw FastNoSuchElementException.instance()
        }
        val avail: Long = traverser.bulk()
        if (counter.get() + avail <= lowRange) {
            // Will not surpass the low w/ this traverser. Skip and filter the whole thing.
            counter.getAndAdd(avail)
            return false
        }

        // Skip for the low and trim for the high. Both can happen at once.
        var toSkip: Long = 0
        if (counter.get() < lowRange) {
            toSkip = lowRange - counter.get()
        }
        var toTrim: Long = 0
        if (highRange != -1L && counter.get() + avail >= highRange) {
            toTrim = counter.get() + avail - highRange
        }
        val toEmit = avail - toSkip - toTrim
        counter.getAndAdd(toSkip + toEmit)
        traverser.setBulk(toEmit)
        return true
    }

    @Override
    fun reset() {
        super.reset()
        counter.set(0L)
    }

    @Override
    override fun toString(): String {
        return StringFactory.stepString(this, lowRange, highRange)
    }

    @Override
    fun clone(): RangeGlobalStep<S> {
        val clone = super.clone() as RangeGlobalStep<S>
        clone.counter = AtomicLong(0L)
        return clone
    }

    @Override
    override fun hashCode(): Int {
        return super.hashCode() xor Long.hashCode(lowRange) xor Long.hashCode(highRange)
    }

    @Override
    override fun equals(other: Object?): Boolean {
        if (super.equals(other)) {
            val typedOther = other as RangeGlobalStep<*>
            return typedOther.lowRange == lowRange && typedOther.highRange == highRange
        }
        return false
    }

    @get:Override
    val requirements: Set<Any>
        get() = Collections.singleton(TraverserRequirement.BULK)

    @get:Override
    val memoryComputeKey: MemoryComputeKey<TraverserSet<S>>
        get() = MemoryComputeKey.of(this.getId(), RangeBiOperator<S>(highRange), false, true)

    @Override
    fun setBypass(bypass: Boolean) {
        this.bypass = bypass
    }

    @Override
    fun processAllStarts() {
    }

    @Override
    fun hasNextBarrier(): Boolean {
        return this.starts.hasNext()
    }

    @Override
    @Throws(NoSuchElementException::class)
    fun nextBarrier(): TraverserSet<S> {
        if (!this.starts.hasNext()) throw FastNoSuchElementException.instance()
        val barrier: TraverserSet<S> = this.traversal.getTraverserSetSupplier().get() as TraverserSet<S>
        while (this.starts.hasNext()) {
            barrier.add(this.starts.next())
        }
        return barrier
    }

    @Override
    fun addBarrier(barrier: TraverserSet<S>) {
        IteratorUtils.removeOnNext(barrier.iterator()).forEachRemaining { traverser ->
            traverser.setSideEffects(this.getTraversal().getSideEffects())
            this.addStart(traverser)
        }
    }

    ////////////////
    class RangeBiOperator<S> @JvmOverloads constructor(private val highRange: Long = -1) :
        BinaryOperator<TraverserSet<S>?>, Serializable {
        @Override
        fun apply(mutatingSeed: TraverserSet<S>, set: TraverserSet<S>?): TraverserSet<S> {
            if (highRange == -1L || mutatingSeed.size() < highRange) mutatingSeed.addAll(set)
            return mutatingSeed
        }
    }
}