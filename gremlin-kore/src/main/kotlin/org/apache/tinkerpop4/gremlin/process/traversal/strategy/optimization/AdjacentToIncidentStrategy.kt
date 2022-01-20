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
 * This strategy looks for vertex- and value-emitting steps followed by a [CountGlobalStep] and replaces the
 * pattern with an edge- or property-emitting step followed by a [CountGlobalStep]. Furthermore, if a vertex-
 * or value-emitting step is the last step in a `.has(traversal)`, `.and(traversal, ...)` or
 * `.or(traversal, ...)` child traversal, it is replaced by an appropriate edge- or property-emitting step.
 * Performing this replacement removes situations where the more expensive trip to an adjacent graph element (e.g.
 * the vertex on the other side of an edge) can be satisfied by trips to incident graph elements (e.g. just the edge
 * itself).
 *
 * @author Daniel Kuppitz (http://gremlin.guru)
 * @example <pre>
 * __.out().count()            // is replaced by __.outE().count()
 * __.in().limit(3).count()    // is replaced by __.inE().limit(3).count()
 * __.values("name").count()   // is replaced by __.properties("name").count()
 * __.where(__.out())          // is replaced by __.where(__.outE())
 * __.where(__.values())       // is replaced by __.where(__.properties())
 * __.and(__.in(), __.out())   // is replaced by __.and(__.inE(), __.outE())
</pre> *
 */
class AdjacentToIncidentStrategy private constructor() : AbstractTraversalStrategy<OptimizationStrategy?>(),
    OptimizationStrategy {
    @Override
    fun apply(traversal: Traversal.Admin<*, *>) {
        val steps: List<Step> = traversal.getSteps()
        val size: Int = steps.size() - 1
        var prev: Step? = null
        for (i in 0..size) {
            val curr: Step = steps[i]
            if (i == size && isOptimizable(curr)) {
                val parent: TraversalParent = curr.getTraversal().getParent()
                if (parent is NotStep || parent is TraversalFilterStep || parent is WhereTraversalStep || parent is ConnectiveStep) {
                    optimizeStep(traversal, curr)
                }
            } else if (isOptimizable(prev)) {
                if (curr is CountGlobalStep) {
                    optimizeStep(traversal, prev)
                }
            }
            if (curr !is RangeGlobalStep) {
                prev = curr
            }
        }
    }

    @Override
    fun applyPrior(): Set<Class<out OptimizationStrategy?>> {
        return PRIORS
    }

    companion object {
        private val INSTANCE = AdjacentToIncidentStrategy()
        private val PRIORS: Set<Class<out OptimizationStrategy?>> =
            HashSet(Arrays.asList(IdentityRemovalStrategy::class.java, IncidentToAdjacentStrategy::class.java))

        /**
         * Checks whether a given step is optimizable or not.
         *
         * @param step the step to check
         * @return `true` if the step is optimizable, otherwise `false`
         */
        private fun isOptimizable(step: Step?): Boolean {
            return (step is VertexStep && (step as VertexStep?).returnsVertex() ||
                    step is PropertiesStep && PropertyType.VALUE.equals((step as PropertiesStep?).getReturnType())) && (step.getTraversal()
                .getEndStep().getLabels().isEmpty() || step.getNextStep() is CountGlobalStep)
        }

        /**
         * Optimizes the given step if possible. Basically this method converts `.out()` to `.outE()`
         * and `.values()` to `.properties()`.
         *
         * @param traversal the traversal that holds the given step
         * @param step      the step to replace
         */
        private fun optimizeStep(traversal: Traversal.Admin, step: Step?) {
            val newStep: Step
            if (step is VertexStep) {
                val vs: VertexStep? = step as VertexStep?
                newStep = VertexStep(traversal, Edge::class.java, vs.getDirection(), vs.getEdgeLabels())
            } else if (step is PropertiesStep) {
                val ps: PropertiesStep? = step as PropertiesStep?
                newStep = PropertiesStep(traversal, PropertyType.PROPERTY, ps.getPropertyKeys())
            } else {
                return
            }
            TraversalHelper.replaceStep(step, newStep, traversal)
        }

        fun instance(): AdjacentToIncidentStrategy {
            return INSTANCE
        }
    }
}