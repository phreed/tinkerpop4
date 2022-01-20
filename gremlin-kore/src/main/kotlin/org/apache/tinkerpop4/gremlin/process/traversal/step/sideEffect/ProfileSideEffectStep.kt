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
import org.apache.tinkerpop4.gremlin.process.traversal.Traversal
import org.apache.tinkerpop4.gremlin.process.traversal.Traverser
import org.apache.tinkerpop4.gremlin.process.traversal.step.GraphComputing
import org.apache.tinkerpop4.gremlin.process.traversal.step.SideEffectCapable
import org.apache.tinkerpop4.gremlin.process.traversal.util.DefaultTraversalMetrics
import org.apache.tinkerpop4.gremlin.structure.Graph
import org.apache.tinkerpop4.gremlin.util.function.DefaultTraversalMetricsSupplier
import java.util.function.Supplier

/**
 * @author Bob Briody (http://bobbriody.com)
 */
class ProfileSideEffectStep<S>(traversal: Traversal.Admin?, @get:Override val sideEffectKey: String) :
    SideEffectStep<S>(traversal), SideEffectCapable<DefaultTraversalMetrics?, DefaultTraversalMetrics?>,
    GraphComputing {
    private var onGraphComputer = false

    init {
        this.getTraversal().getSideEffects()
            .registerIfAbsent(sideEffectKey, DefaultTraversalMetricsSupplier.instance() as Supplier, Operator.assign)
    }

    @Override
    protected fun sideEffect(traverser: Traverser.Admin<S>?) {
    }

    @Override
    operator fun next(): Traverser.Admin<S>? {
        var start: Traverser.Admin<S>? = null
        return try {
            start = super.next()
            start
        } finally {
            if (!onGraphComputer && start == null) {
                val m: DefaultTraversalMetrics = traversalMetricsFromSideEffects
                if (!m.isFinalized()) m.setMetrics(this.getTraversal(), false)
            }
        }
    }

    @Override
    operator fun hasNext(): Boolean {
        val start: Boolean = super.hasNext()
        if (!onGraphComputer && !start) {
            val m: DefaultTraversalMetrics = traversalMetricsFromSideEffects
            if (!m.isFinalized()) m.setMetrics(this.getTraversal(), false)
        }
        return start
    }

    private val traversalMetricsFromSideEffects: DefaultTraversalMetrics
        private get() = this.getTraversal().getSideEffects().get(sideEffectKey) as DefaultTraversalMetrics

    @Override
    fun generateFinalResult(tm: DefaultTraversalMetrics): DefaultTraversalMetrics {
        if (onGraphComputer && !tm.isFinalized()) tm.setMetrics(this.getTraversal(), true)
        return tm
    }

    @Override
    fun onGraphComputer() {
        onGraphComputer = true
    }

    companion object {
        val DEFAULT_METRICS_KEY: String = Graph.Hidden.hide("metrics")
    }
}