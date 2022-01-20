/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.tinkerpop4.gremlin.process.traversal.strategy.optimization

import org.apache.tinkerpop4.gremlin.process.traversal.Scope

/**
 * `RepeatUnrollStrategy` is an OLTP-only strategy that unrolls any [RepeatStep] if it uses a constant
 * number of loops (`times(x)`) and doesn't emit intermittent elements. If any of the following 3 steps appears
 * within the repeat-traversal, the strategy will not be applied:
 *
 *
 *
 *  * [DedupGlobalStep]
 *  * [LoopsStep]
 *  * [LambdaHolder]
 *
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @example <pre>
 * __.repeat(out()).times(2)   // is replaced by __.out().barrier(2500).out().barrier(2500)
</pre> *
 */
class RepeatUnrollStrategy private constructor() : AbstractTraversalStrategy<OptimizationStrategy?>(),
    OptimizationStrategy {
    @Override
    fun apply(traversal: Traversal.Admin<*, *>) {
        if (TraversalHelper.onGraphComputer(traversal)) return
        val lazyBarrierStrategyInstalled: Boolean =
            TraversalHelper.getRootTraversal(traversal).getStrategies().getStrategy(
                LazyBarrierStrategy::class.java
            ).isPresent()
        for (i in 0 until traversal.getSteps().size()) {
            if (traversal.getSteps().get(i) is RepeatStep) {
                val repeatStep: RepeatStep<*> = traversal.getSteps().get(i) as RepeatStep
                if (null == repeatStep.getEmitTraversal() && null != repeatStep.getRepeatTraversal() &&
                    repeatStep.getUntilTraversal() is LoopTraversal && (repeatStep.getUntilTraversal() as LoopTraversal).getMaxLoops() > 0 &&
                    !TraversalHelper.hasStepOfAssignableClassRecursively(
                        Scope.global,
                        DedupGlobalStep::class.java,
                        repeatStep.getRepeatTraversal()
                    ) &&
                    !TraversalHelper.hasStepOfAssignableClassRecursively(
                        INVALIDATING_STEPS,
                        repeatStep.getRepeatTraversal()
                    )
                ) {
                    val repeatTraversal: Traversal.Admin<*, *> = repeatStep.getGlobalChildren().get(0)
                    repeatTraversal.removeStep(repeatTraversal.getSteps().size() - 1) // removes the RepeatEndStep
                    val repeatLength: Int = repeatTraversal.getSteps().size()
                    var insertIndex: Int = i
                    val loops = (repeatStep.getUntilTraversal() as LoopTraversal).getMaxLoops() as Int
                    for (j in 0 until loops) {
                        TraversalHelper.insertTraversal(insertIndex, repeatTraversal.clone(), traversal)
                        insertIndex = insertIndex + repeatLength

                        // the addition of barriers is determined by the existence of LazyBarrierStrategy
                        if (lazyBarrierStrategyInstalled) {
                            // only add a final NoOpBarrier is subsequent step is not a barrier
                            // Don't add a barrier if this step is a barrier (prevents nested repeat adding the barrier multiple times)
                            val step: Step = traversal.getSteps().get(insertIndex)
                            if ((j != loops - 1 || step.getNextStep() !is Barrier) && step !is NoOpBarrierStep) {
                                traversal.addStep(++insertIndex, NoOpBarrierStep(traversal, MAX_BARRIER_SIZE))
                            }
                        }
                    }

                    // label last step if repeat() was labeled
                    if (!repeatStep.getLabels().isEmpty()) TraversalHelper.copyLabels(
                        repeatStep,
                        traversal.getSteps().get(insertIndex),
                        false
                    )

                    // remove the RepeatStep
                    traversal.removeStep(i)
                }
            }
        }
    }

    companion object {
        private val INSTANCE = RepeatUnrollStrategy()
        protected const val MAX_BARRIER_SIZE = 2500
        private val INVALIDATING_STEPS: Set<Class> =
            HashSet(Arrays.asList(LambdaHolder::class.java, LoopsStep::class.java))

        fun instance(): RepeatUnrollStrategy {
            return INSTANCE
        }
    }
}