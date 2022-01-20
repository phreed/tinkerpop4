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
 * `FilterRankingStrategy` reorders filter- and order-steps according to their rank. Step ranks are defined within
 * the strategy and indicate when it is reasonable for a step to move in front of another. It will also do its best to
 * push step labels as far "right" as possible in order to keep traversers as small and bulkable as possible prior to
 * the absolute need for path-labeling.
 *
 * @author Daniel Kuppitz (http://gremlin.guru)
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @example <pre>
 * __.order().dedup()                        // is replaced by __.dedup().order()
 * __.dedup().filter(out()).has("value", 0)  // is replaced by __.has("value", 0).filter(out()).dedup()
</pre> *
 */
class FilterRankingStrategy private constructor() : AbstractTraversalStrategy<OptimizationStrategy?>(),
    OptimizationStrategy {
    @Override
    fun apply(traversal: Traversal.Admin<*, *>) {
        var modified = true
        while (modified) {
            modified = false
            val steps: List<Step> = traversal.getSteps()
            for (i in 0 until steps.size() - 1) {
                val step: Step<*, *> = steps[i]
                val nextStep: Step<*, *> = step.getNextStep()
                if (!usesLabels(nextStep, step.getLabels())) {
                    val nextRank = getStepRank(nextStep)
                    if (nextRank != 0) {
                        if (!step.getLabels().isEmpty()) {
                            TraversalHelper.copyLabels(step, nextStep, true)
                            modified = true
                        }
                        if (getStepRank(step) > nextRank) {
                            traversal.removeStep(nextStep)
                            traversal.addStep(i, nextStep)
                            modified = true
                        }
                    }
                }
            }
        }
    }

    @Override
    fun applyPrior(): Set<Class<out OptimizationStrategy?>> {
        return PRIORS
    }

    companion object {
        private val INSTANCE = FilterRankingStrategy()
        private val PRIORS: Set<Class<out OptimizationStrategy?>> =
            Collections.singleton(IdentityRemovalStrategy::class.java)

        /**
         * Ranks the given step. Steps with lower ranks can be moved in front of steps with higher ranks. 0 means that
         * the step has no rank and thus is not exchangeable with its neighbors.
         *
         * @param step the step to get a ranking for
         * @return The rank of the given step.
         */
        private fun getStepRank(step: Step): Int {
            val rank: Int
            rank =
                if (!(step is FilterStep || step is OrderGlobalStep)) return 0 else if (step is IsStep || step is ClassFilterStep) 1 else if (step is HasStep) 2 else if (step is WherePredicateStep && (step as WherePredicateStep).getLocalChildren()
                        .isEmpty()
                ) 3 else if (step is TraversalFilterStep || step is NotStep) 4 else if (step is WhereTraversalStep) 5 else if (step is OrStep) 6 else if (step is AndStep) 7 else if (step is WherePredicateStep) // has by()-modulation
                    8 else if (step is DedupGlobalStep) 9 else if (step is OrderGlobalStep) 10 else return 0
            ////////////
            return if (step is TraversalParent) getMaxStepRank(
                step as TraversalParent,
                rank
            ) else rank
        }

        private fun getMaxStepRank(parent: TraversalParent, startRank: Int): Int {
            var maxStepRank = startRank
            // no filter steps are global parents (yet)
            for (traversal in parent.getLocalChildren()) {
                for (step in traversal.getSteps()) {
                    val stepRank = getStepRank(step)
                    if (stepRank > maxStepRank) maxStepRank = stepRank
                }
            }
            return maxStepRank
        }

        private fun usesLabels(step: Step<*, *>, labels: Set<String>): Boolean {
            if (step is LambdaHolder) return true
            if (step is Scoping) {
                val scopes: Set<String> = (step as Scoping).getScopeKeys()
                for (label in labels) {
                    if (scopes.contains(label)) return true
                }
            }
            if (step is TraversalParent) {
                if (TraversalHelper.anyStepRecursively(
                        { s -> usesLabels(s, labels) },
                        step as TraversalParent
                    )
                ) return true
            }
            return false
        }

        fun instance(): FilterRankingStrategy {
            return INSTANCE
        }
    }
}