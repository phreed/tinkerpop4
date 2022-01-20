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
 * @author Bob Briody (http://bobbriody.com)
 */
class ProfileStep<S>(traversal: Traversal.Admin?) : AbstractStep<S, S>(traversal), MemoryComputing<MutableMetrics?> {
    // pseudo GraphComputing but local traversals are "GraphComputing"
    private var metrics: MutableMetrics? = null
    private var onGraphComputer = false

    /**
     * Returns `Optional.empty()` if traversal is not iterated or if not locked after strategy application.
     */
    fun getMetrics(): Optional<MutableMetrics> {
        if (this.traversal.isLocked()) initializeIfNeeded()
        return Optional.ofNullable(metrics)
    }

    @Override
    operator fun next(): Traverser.Admin<S>? {
        var start: Traverser.Admin<S>? = null
        initializeIfNeeded()
        metrics.start()
        return try {
            start = super.next()
            start
        } finally {
            if (start != null) {
                metrics.finish(start.bulk())
                if (onGraphComputer) {
                    this.getTraversal().getSideEffects().add(this.getId(), metrics)
                    metrics = null
                }
            } else {
                metrics.stop()
                if (onGraphComputer) {
                    this.getTraversal().getSideEffects().add(this.getId(), metrics)
                    metrics = null
                }
            }
        }
    }

    @Override
    operator fun hasNext(): Boolean {
        initializeIfNeeded()
        metrics.start()
        val ret: Boolean = super.hasNext()
        metrics.stop()
        return ret
    }

    @Override
    @Throws(NoSuchElementException::class)
    protected fun processNextStart(): Traverser.Admin<S> {
        return this.starts.next()
    }

    private fun initializeIfNeeded() {
        if (null == metrics) {
            onGraphComputer = TraversalHelper.onGraphComputer(this.getTraversal())
            metrics = MutableMetrics(this.getPreviousStep().getId(), this.getPreviousStep().toString())
            val previousStep: Step<*, S> = this.getPreviousStep()

            // give metrics to the step being profiled so that it can add additional data to the metrics like
            // annotations
            if (previousStep is Profiling) (previousStep as Profiling).setMetrics(metrics)
        }
    }

    @get:Override
    val memoryComputeKey: MemoryComputeKey<MutableMetrics>
        get() = MemoryComputeKey.of(this.getId(), ProfileBiOperator.instance(), false, true)

    @Override
    fun clone(): ProfileStep<S> {
        val clone = super.clone() as ProfileStep<S>
        clone.metrics = null
        return clone
    }

    /**
     * Starts the metrics timer.
     */
    fun start() {
        initializeIfNeeded()
        metrics.start()
    }

    /**
     * Stops the metrics timer.
     */
    fun stop() {
        metrics.stop()
    }

    /////
    class ProfileBiOperator : BinaryOperator<MutableMetrics?>, Serializable {
        @Override
        fun apply(metricsA: MutableMetrics, metricsB: MutableMetrics?): MutableMetrics {
            metricsA.aggregate(metricsB)
            return metricsA
        }

        companion object {
            private val INSTANCE = ProfileBiOperator()
            fun instance(): ProfileBiOperator {
                return INSTANCE
            }
        }
    }
}