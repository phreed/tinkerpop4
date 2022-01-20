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
package org.apache.tinkerpop4.gremlin.process.traversal.step

import org.apache.tinkerpop4.gremlin.process.traversal.Step
import org.apache.tinkerpop4.gremlin.process.traversal.Traversal
import org.apache.tinkerpop4.gremlin.process.traversal.dsl.graph.Anon
import org.apache.tinkerpop4.gremlin.process.traversal.lambda.ColumnTraversal
import org.apache.tinkerpop4.gremlin.process.traversal.lambda.FunctionTraverser
import org.apache.tinkerpop4.gremlin.process.traversal.lambda.IdentityTraversal
import org.apache.tinkerpop4.gremlin.process.traversal.lambda.TokenTraversal
import org.apache.tinkerpop4.gremlin.process.traversal.lambda.ValueTraversal
import org.apache.tinkerpop4.gremlin.process.traversal.step.map.GroupStep
import org.apache.tinkerpop4.gremlin.process.traversal.step.map.LambdaMapStep
import org.apache.tinkerpop4.gremlin.process.traversal.step.sideEffect.GroupSideEffectStep
import org.apache.tinkerpop4.gremlin.process.traversal.step.util.ProfileStep
import java.util.List
import java.util.Map

/**
 * An interface for common functionality of [GroupStep] and [GroupSideEffectStep].
 */
interface Grouping<S, K, V> {
    /**
     * Determines if the provided traversal is equal to the key traversal that the `Grouping` has.
     */
    val keyTraversal: Traversal.Admin<S, K>?

    /**
     * Determines if the provided traversal is equal to the value traversal that the `Grouping` has.
     */
    val valueTraversal: Traversal.Admin<S, V>?

    /**
     * Checks if there is a non-local [Barrier] in the value traversal.
     */
    fun hasBarrierInValueTraversal(): Boolean {
        return null != determineBarrierStep(valueTraversal)
    }

    /**
     * Determines the first non-local [Barrier] step in the provided traversal. This method is used by [GroupStep]
     * and [GroupSideEffectStep] to ultimately determine the reducing bi-operator.
     *
     * @param traversal The traversal to inspect.
     * @return The first non-local barrier step or `null` if no such step was found.
     */
    fun determineBarrierStep(traversal: Traversal.Admin<S, V>): Barrier? {
        val steps: List<Step> = traversal.getSteps()
        for (ix in 0 until steps.size()) {
            val step: Step = steps[ix]
            if (step is Barrier && step !is LocalBarrier) {
                val b: Barrier = step as Barrier

                // when profile() is enabled the step needs to be wrapped up with the barrier so that the timer on
                // the ProfileStep is properly triggered
                return if (ix < steps.size() - 1 && steps[ix + 1] is ProfileStep) ProfiledBarrier(
                    b,
                    steps[ix + 1] as ProfileStep
                ) else b
            }
        }
        return null
    }

    fun convertValueTraversal(valueTraversal: Traversal.Admin<S, V>): Traversal.Admin<S, V>? {
        return if (valueTraversal is ValueTraversal ||
            valueTraversal is TokenTraversal ||
            valueTraversal is IdentityTraversal ||
            valueTraversal is ColumnTraversal || valueTraversal.getStartStep() is LambdaMapStep && (valueTraversal.getStartStep() as LambdaMapStep).getMapFunction() is FunctionTraverser
        ) {
            __.map(valueTraversal).fold() as Traversal.Admin<S, V>
        } else valueTraversal
    }

    /**
     * When there is a [Barrier] detected by [.determineBarrierStep] it is processed
     * in full up to that point and then the remainder of the traversal that follows it is completed.
     */
    fun doFinalReduction(map: Map<K, Object?>, valueTraversal: Traversal.Admin<S, V>): Map<K, V?>? {
        val barrierStep: Barrier? = determineBarrierStep(valueTraversal)
        if (barrierStep != null) {
            for (key in map.keySet()) {
                valueTraversal.reset()
                barrierStep.addBarrier(map[key])
                if (valueTraversal.hasNext()) map.put(key, valueTraversal.next())
            }
        }
        return map
    }
}