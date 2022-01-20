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
 * @author Matt Frantz (http://github.com/mhfrantz)
 */
class TailGlobalStep<S>(traversal: Traversal.Admin?, private val limit: Long) : AbstractStep<S, S>(traversal),
    Bypassing, Barrier<TraverserSet<S>?> {
    private var tail: Deque<Traverser.Admin<S>>
    private var tailBulk = 0L
    private var bypass = false

    init {
        tail = ArrayDeque(limit.toInt())
    }

    fun setBypass(bypass: Boolean) {
        this.bypass = bypass
    }

    @Override
    fun processNextStart(): Traverser.Admin<S> {
        return if (bypass) {
            // If we are bypassing this step, let everything through.
            this.starts.next()
        } else {
            // Pull everything available before we start delivering from the tail buffer.
            if (this.starts.hasNext()) {
                this.starts.forEachRemaining { start: Traverser.Admin<S> -> addTail(start) }
            }
            // Pull the oldest traverser from the tail buffer.
            val oldest: Traverser.Admin<S> = tail.pop()
            // Trim any excess from the oldest traverser.
            val excess = tailBulk - limit
            if (excess > 0) {
                oldest.setBulk(oldest.bulk() - excess)
                // Account for the loss of excess in the tail buffer
                tailBulk -= excess
            }
            // Account for the loss of bulk in the tail buffer as we emit the oldest traverser.
            tailBulk -= oldest.bulk()
            oldest
        }
    }

    @Override
    fun reset() {
        super.reset()
        tail.clear()
        tailBulk = 0L
    }

    @Override
    override fun toString(): String {
        return StringFactory.stepString(this, limit)
    }

    @Override
    fun clone(): TailGlobalStep<S> {
        val clone = super.clone() as TailGlobalStep<S>
        clone.tail = ArrayDeque(limit.toInt())
        clone.tailBulk = 0L
        return clone
    }

    @Override
    override fun hashCode(): Int {
        return super.hashCode() xor Long.hashCode(limit)
    }

    @get:Override
    val requirements: Set<Any>
        get() = Collections.singleton(TraverserRequirement.BULK)

    private fun addTail(start: Traverser.Admin<S>) {
        // Calculate the tail bulk including this new start.
        tailBulk += start.bulk()
        // Evict from the tail buffer until we have enough room.
        while (!tail.isEmpty()) {
            val oldest: Traverser.Admin<S> = tail.getFirst()
            val bulk: Long = oldest.bulk()
            if (tailBulk - bulk < limit) break
            tail.pop()
            tailBulk -= bulk
        }
        tail.add(start)
    }

    @get:Override
    val memoryComputeKey: MemoryComputeKey<TraverserSet<S>>
        get() = MemoryComputeKey.of(this.getId(), RangeBiOperator(limit), false, true)

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
}