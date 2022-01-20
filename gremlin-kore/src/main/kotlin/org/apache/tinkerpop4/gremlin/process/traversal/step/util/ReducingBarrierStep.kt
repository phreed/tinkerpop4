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
abstract class ReducingBarrierStep<S, E>(traversal: Traversal.Admin?) : AbstractStep<S, E>(traversal), Barrier<E>,
    Generating<E, E> {
    /**
     * If the `seedSupplier` is `null` then the default behavior is to generate the seed from the starts.
     * This supplier must be callable as a constant and not rely on state from the class. Prefer overriding
     * [.generateSeedFromStarts] otherwise.
     */
    protected var seedSupplier: Supplier<E>? = null
    protected var reducingBiOperator: BinaryOperator<E>? = null
    protected var hasProcessedOnce = false
    private var seed = NON_EMITTING_SEED as E
    fun setSeedSupplier(seedSupplier: Supplier<E>) {
        this.seedSupplier = seedSupplier
    }

    /**
     * Gets the provided seed supplier or provides [.generateSeedFromStarts].
     */
    fun getSeedSupplier(): Supplier<E> {
        return Optional.ofNullable(seedSupplier).orElse { generateSeedFromStarts() }
    }

    /**
     * If the `seedSupplier` is `null` then this method is called.
     */
    protected fun generateSeedFromStarts(): E? {
        return if (starts.hasNext()) projectTraverser(this.starts.next()) else null
    }

    abstract fun projectTraverser(traverser: Traverser.Admin<S>?): E
    fun setReducingBiOperator(reducingBiOperator: BinaryOperator<E>) {
        this.reducingBiOperator = reducingBiOperator
    }

    val biOperator: BinaryOperator<E>
        get() = reducingBiOperator

    fun reset() {
        super.reset()
        hasProcessedOnce = false
        seed = NON_EMITTING_SEED
    }

    @Override
    fun done() {
        hasProcessedOnce = true
        seed = NON_EMITTING_SEED
    }

    @Override
    fun processAllStarts() {
        if (hasProcessedOnce && !this.starts.hasNext()) return
        hasProcessedOnce = true
        if (seed === NON_EMITTING_SEED) {
            seed = getSeedSupplier().get()
        }
        while (this.starts.hasNext()) seed = reducingBiOperator.apply(seed, projectTraverser(this.starts.next()))
    }

    @Override
    fun hasNextBarrier(): Boolean {
        processAllStarts()
        return NON_EMITTING_SEED !== seed
    }

    @Override
    fun nextBarrier(): E {
        return if (!hasNextBarrier()) throw FastNoSuchElementException.instance() else {
            val temp = seed
            seed = NON_EMITTING_SEED
            temp
        }
    }

    @Override
    fun addBarrier(barrier: E) {
        seed = if (NON_EMITTING_SEED === seed) barrier else reducingBiOperator.apply(seed, barrier)
    }

    @Override
    fun processNextStart(): Traverser.Admin<E> {
        processAllStarts()
        if (seed === NON_EMITTING_SEED) throw FastNoSuchElementException.instance()
        val traverser: Traverser.Admin<E> = this.getTraversal().getTraverserGenerator().generate(
            this.generateFinalResult(
                seed
            ), this as Step<E, E>, 1L
        )
        seed = NON_EMITTING_SEED
        return traverser
    }

    @Override
    fun clone(): ReducingBarrierStep<S, E> {
        val clone = super.clone() as ReducingBarrierStep<S, E>
        clone.hasProcessedOnce = false
        clone.seed = NON_EMITTING_SEED
        return clone
    }

    @get:Override
    val memoryComputeKey: MemoryComputeKey<E>
        get() = MemoryComputeKey.of(this.getId(), biOperator, false, true)

    /**
     * A class that represents a value that is not be to be emitted which helps with flow control internal to the class
     * and is serializable in Gryo for use in OLAP.
     */
    object NonEmittingSeed : Serializable {
        val INSTANCE: NonEmittingSeed = NonEmittingSeed()
    }

    companion object {
        /**
         * A seed value not to be emitted from the `ReducingBarrierStep` helping with flow control within this step.
         */
        val NON_EMITTING_SEED: Object = NonEmittingSeed.INSTANCE
    }
}