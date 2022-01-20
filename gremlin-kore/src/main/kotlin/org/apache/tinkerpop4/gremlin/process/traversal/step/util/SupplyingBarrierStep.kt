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
abstract class SupplyingBarrierStep<S, E>(traversal: Traversal.Admin?) : AbstractStep<S, E>(traversal),
    Barrier<Boolean?> {
    private var done = false
    protected abstract fun supply(): E
    @Override
    fun addStarts(starts: Iterator<Traverser.Admin<S>?>) {
        if (starts.hasNext()) {
            done = false
            super.addStarts(starts)
        }
    }

    @Override
    fun addStart(start: Traverser.Admin<S>?) {
        done = false
        super.addStart(start)
    }

    @Override
    fun reset() {
        super.reset()
        done = false
    }

    @Override
    fun processNextStart(): Traverser.Admin<E> {
        if (done) throw FastNoSuchElementException.instance()
        processAllStarts()
        done = true
        return this.getTraversal().asAdmin().getTraverserGenerator().generate(supply(), this as Step<E, E>, 1L)
    }

    @Override
    fun clone(): SupplyingBarrierStep<S, E> {
        val clone = super.clone() as SupplyingBarrierStep<S, E>
        clone.done = false
        return clone
    }

    fun processAllStarts() {
        while (this.starts.hasNext()) this.starts.next()
    }

    @Override
    fun hasNextBarrier(): Boolean {
        return !done
    }

    @Override
    @Throws(NoSuchElementException::class)
    fun nextBarrier(): Boolean {
        processAllStarts()
        done = true
        return true
    }

    @Override
    fun addBarrier(barrier: Boolean?) {
        done = false
    }

    @Override
    fun done() {
        done = true
    }

    @get:Override
    val memoryComputeKey: MemoryComputeKey<Boolean>
        get() = MemoryComputeKey.of(this.getId(), Operator.and as BinaryOperator, false, true)
}