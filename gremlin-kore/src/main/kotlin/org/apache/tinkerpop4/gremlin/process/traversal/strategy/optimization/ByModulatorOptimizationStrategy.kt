/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.tinkerpop4.gremlin.process.traversal.strategy.optimization

import org.apache.tinkerpop4.gremlin.process.traversal.Step

/**
 * This strategy looks for standard traversals in by-modulators and replaces them with more optimized traversals
 * (e.g. `TokenTraversal`) if possible.
 *
 *
 *
 * @author Daniel Kuppitz (http://gremlin.guru)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 * @example <pre>
 * __.path().by(id())                        // is replaced by __.path().by(id)
 * __.dedup().by(label())                    // is replaced by __.dedup().by(label)
 * __.order().by(values("name"))             // is replaced by __.order().by("name")
 * __.group().by().by(values("name").fold()) // is replaced by __.group().by("name")
</pre> *
 */
class ByModulatorOptimizationStrategy private constructor() : AbstractTraversalStrategy<OptimizationStrategy?>(),
    OptimizationStrategy {
    private fun optimizeByModulatingTraversal(step: TraversalParent, traversal: Traversal.Admin<*, *>?) {
        if (traversal == null) return
        val steps: List<Step> = traversal.asAdmin().getSteps()
        if (steps.size() === 1) {
            val singleStep: Step = steps[0]
            optimizeForStep(step, traversal, singleStep)
        }
    }

    private fun optimizeForStep(step: TraversalParent, traversal: Traversal.Admin<*, *>, singleStep: Step) {
        if (singleStep is PropertiesStep) {
            val ps: PropertiesStep = singleStep as PropertiesStep
            if (ps.getReturnType().equals(PropertyType.VALUE) && ps.getPropertyKeys().length === 1) {
                step.replaceLocalChild(traversal, ValueTraversal(ps.getPropertyKeys().get(0)))
            }
        } else if (singleStep is IdStep) {
            step.replaceLocalChild(traversal, TokenTraversal(T.id))
        } else if (singleStep is LabelStep) {
            step.replaceLocalChild(traversal, TokenTraversal(T.label))
        } else if (singleStep is PropertyKeyStep) {
            step.replaceLocalChild(traversal, TokenTraversal(T.key))
        } else if (singleStep is PropertyValueStep) {
            step.replaceLocalChild(traversal, TokenTraversal(T.value))
        } else if (singleStep is IdentityStep) {
            step.replaceLocalChild(traversal, IdentityTraversal())
        }
    }

    @Override
    fun apply(traversal: Traversal.Admin<*, *>) {
        val step: Step = traversal.getParent().asStep()
        if (step is ByModulating && step is TraversalParent) {
            val byModulatingStep: TraversalParent = step as TraversalParent
            if (step is Grouping) {
                val grouping: Grouping = step as Grouping
                optimizeByModulatingTraversal(byModulatingStep, grouping.getKeyTraversal())

                // the value by() needs different handling because by(Traversal) only equals by(String) or by(T)
                // if the traversal does a fold().
                val currentValueTraversal: Traversal.Admin<*, *> = grouping.getValueTraversal()
                val stepsInCurrentValueTraversal: List<Step> = currentValueTraversal.getSteps()
                if (stepsInCurrentValueTraversal.size() === 1 && stepsInCurrentValueTraversal[0] is IdentityStep) optimizeForStep(
                    byModulatingStep,
                    currentValueTraversal,
                    stepsInCurrentValueTraversal[0]
                ) else if (stepsInCurrentValueTraversal.size() === 2 && stepsInCurrentValueTraversal[1] is FoldStep) optimizeForStep(
                    byModulatingStep,
                    currentValueTraversal,
                    stepsInCurrentValueTraversal[0]
                )
            } else {
                for (byModulatingTraversal in byModulatingStep.getLocalChildren()) {
                    optimizeByModulatingTraversal(byModulatingStep, byModulatingTraversal)
                }
            }
        }
    }

    @Override
    fun applyPrior(): Set<Class<out OptimizationStrategy?>> {
        return PRIORS
    }

    companion object {
        private val INSTANCE = ByModulatorOptimizationStrategy()

        // when PathProcessorStrategy is present for withComputer() you need to ensure it always executes first because
        // it does some manipulation to select().by(t) in some cases to turn it to select().map(t) in which case this
        // strategy has nothing to do. if it were to occur first then that optimization wouldn't work as expected.
        private val PRIORS: Set<Class<out OptimizationStrategy?>> = HashSet(
            Arrays.asList(
                PathProcessorStrategy::class.java, IdentityRemovalStrategy::class.java
            )
        )

        fun instance(): ByModulatorOptimizationStrategy {
            return INSTANCE
        }
    }
}