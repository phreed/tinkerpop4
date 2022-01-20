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
package org.apache.tinkerpop4.gremlin.process.traversal.strategy.optimization

import org.apache.tinkerpop4.gremlin.process.traversal.Step

/**
 * This strategy looks for [RangeGlobalStep]s that can be moved further left in the traversal and thus be applied
 * earlier. It will also try to merge multiple [RangeGlobalStep]s into one.
 * If the logical consequence of one or multiple [RangeGlobalStep]s is an empty result, the strategy will remove
 * as many steps as possible and add a [NoneStep] instead.
 *
 * @author Daniel Kuppitz (http://gremlin.guru)
 * @example <pre>
 * __.out().valueMap().limit(5)                          // becomes __.out().limit(5).valueMap()
 * __.outE().range(2, 10).valueMap().limit(5)            // becomes __.outE().range(2, 7).valueMap()
 * __.outE().limit(5).valueMap().range(2, -1)            // becomes __.outE().range(2, 5).valueMap()
 * __.outE().limit(5).valueMap().range(5, 10)            // becomes __.outE().none()
 * __.outE().limit(5).valueMap().range(5, 10).cap("a")   // becomes __.outE().none().cap("a")
</pre> *
 */
class EarlyLimitStrategy private constructor() : AbstractTraversalStrategy<OptimizationStrategy?>(),
    OptimizationStrategy {
    @SuppressWarnings("unchecked")
    @Override
    fun apply(traversal: Traversal.Admin<*, *>) {
        val steps: List<Step> = traversal.getSteps()
        var insertAfter: Step? = null
        var merge = false
        var i = 0
        var j: Int = steps.size()
        while (i < j) {
            val step: Step = steps[i]
            if (step is RangeGlobalStep) {
                if (insertAfter != null) {
                    // RangeStep was found, move it to the earliest possible step or merge it with a
                    // previous RangeStep; keep the RangeStep's labels at its preceding step
                    TraversalHelper.copyLabels(step, step.getPreviousStep(), true)
                    insertAfter = moveRangeStep(step as RangeGlobalStep, insertAfter, traversal, merge)
                    if (insertAfter is NoneStep) {
                        // any step besides a SideEffectCapStep after a NoneStep would be pointless
                        val noneStepIndex: Int = TraversalHelper.stepIndex(insertAfter, traversal)
                        i = j - 2
                        while (i > noneStepIndex) {
                            if (steps[i] !is SideEffectCapStep && steps[i] !is ProfileSideEffectStep) {
                                traversal.removeStep(i)
                            }
                            i--
                        }
                        break
                    }
                    j = steps.size()
                }
            } else if (!(step is MapStep || step is SideEffectStep)) {
                // remember the last step that can be used to move any RangeStep to
                // any RangeStep can be moved in front of all its preceding map- and sideEffect-steps
                insertAfter = step
                merge = true
            } else if (step is SideEffectCapable) {
                // if there's any SideEffectCapable step along the way, RangeSteps cannot be merged as this could
                // change the final traversal's internal memory
                merge = false
            }
            i++
        }
    }

    @SuppressWarnings("unchecked")
    private fun moveRangeStep(
        step: RangeGlobalStep, insertAfter: Step, traversal: Traversal.Admin<*, *>,
        merge: Boolean
    ): Step {
        val rangeStep: Step
        var remove = true
        if (insertAfter is RangeGlobalStep) {
            // there's a previous RangeStep which might affect the effective range of the current RangeStep
            // recompute this step's low and high; if the result is still a valid range, create a new RangeStep,
            // otherwise a NoneStep
            val other: RangeGlobalStep = insertAfter as RangeGlobalStep
            val low: Long = other.getLowRange() + step.getLowRange()
            if (other.getHighRange() === -1L) {
                rangeStep = RangeGlobalStep(traversal, low, other.getLowRange() + step.getHighRange())
            } else if (step.getHighRange() === -1L) {
                val high: Long = other.getHighRange() - other.getLowRange() - step.getLowRange() + low
                if (low < high) {
                    rangeStep = RangeGlobalStep(traversal, low, high)
                } else {
                    rangeStep = NoneStep(traversal)
                }
            } else {
                val high: Long = Math.min(other.getLowRange() + step.getHighRange(), other.getHighRange())
                rangeStep = if (high > low) RangeGlobalStep(traversal, low, high) else NoneStep(traversal)
            }
            remove = merge
            TraversalHelper.replaceStep(if (merge) insertAfter else step, rangeStep, traversal)
        } else if (!step.getPreviousStep().equals(insertAfter, true)) {
            // move the RangeStep behind the earliest possible map- or sideEffect-step
            rangeStep = step.clone()
            TraversalHelper.insertAfterStep(rangeStep, insertAfter, traversal)
        } else {
            // no change if the earliest possible step to insert the RangeStep after is
            // already the current step's previous step
            return step
        }
        if (remove) traversal.removeStep(step)
        return rangeStep
    }

    companion object {
        private val INSTANCE = EarlyLimitStrategy()
        fun instance(): EarlyLimitStrategy {
            return INSTANCE
        }
    }
}