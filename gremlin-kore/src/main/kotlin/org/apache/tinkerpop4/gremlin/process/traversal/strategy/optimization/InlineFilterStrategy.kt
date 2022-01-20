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

import org.apache.tinkerpop4.gremlin.process.computer.traversal.strategy.optimization.GraphFilterStrategy

/**
 * This strategy analyzes filter-steps with child traversals that themselves are pure filters. If the child traversals
 * are pure filters then the wrapping parent filter is not needed and thus, the children can be "inlined." Normalizing
 * pure filters with inlining reduces the number of variations of a filter that a graph provider may need to reason
 * about when writing their own strategies. As a result, this strategy helps increase the likelihood that a provider's
 * filtering optimization will succeed at re-writing the traversal.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @example <pre>
 * __.outE().hasLabel(eq("knows").or(eq("created"))).inV()       // is replaced by __.outE("knows", "created").inV()
 * __.filter(has("name","marko"))                                // is replaced by __.has("name","marko")
 * __.and(has("name"),has("age"))                                // is replaced by __.has("name").has("age")
 * __.and(filter(has("name","marko").has("age")),hasNot("blah")) // is replaced by __.has("name","marko").has("age").hasNot("blah")
 * __.match(as('a').has(key,value),...)                          // is replaced by __.as('a').has(key,value).match(...)
</pre> *
 */
class InlineFilterStrategy private constructor() : AbstractTraversalStrategy<OptimizationStrategy?>(),
    OptimizationStrategy {
    @Override
    fun apply(traversal: Traversal.Admin<*, *>) {
        var changed = true // recursively walk child traversals trying to inline them into the current traversal line.
        while (changed) {
            changed = false
            val filterStepIterator: Iterator<FilterStep> =
                TraversalHelper.getStepsOfAssignableClass(FilterStep::class.java, traversal).iterator()
            while (!changed && filterStepIterator.hasNext()) {
                val step: FilterStep<*> = filterStepIterator.next()
                changed = step is HasStep && processHasStep(
                    step as HasStep,
                    traversal
                ) || step is TraversalFilterStep && processTraversalFilterStep(
                    step as TraversalFilterStep,
                    traversal
                ) || step is OrStep && processOrStep(step as OrStep, traversal) || step is AndStep && processAndStep(
                    step as AndStep,
                    traversal
                )
            }
            if (!changed && traversal.isRoot()) {
                val matchStepIterator: Iterator<MatchStep> =
                    TraversalHelper.getStepsOfClass(MatchStep::class.java, traversal).iterator()
                while (!changed && matchStepIterator.hasNext()) {
                    if (processMatchStep(matchStepIterator.next(), traversal)) changed = true
                }
            }
        }
    }

    @Override
    fun applyPost(): Set<Class<out OptimizationStrategy?>> {
        return POSTS
    }

    @Override
    fun applyPrior(): Set<Class<out OptimizationStrategy?>> {
        return PRIORS
    }

    companion object {
        private val INSTANCE = InlineFilterStrategy()
        private val POSTS: Set<Class<out OptimizationStrategy?>> = HashSet(
            Arrays.asList(
                GraphFilterStrategy::class.java,
                AdjacentToIncidentStrategy::class.java,
                PathRetractionStrategy::class.java
            )
        )
        private val PRIORS: Set<Class<out OptimizationStrategy?>> = HashSet(
            Arrays.asList(
                FilterRankingStrategy::class.java,
                IdentityRemovalStrategy::class.java,
                MatchPredicateStrategy::class.java
            )
        )

        ////////////////////////////
        ///////////////////////////
        private fun processHasStep(step: HasStep<*>, traversal: Traversal.Admin<*, *>): Boolean {
            return if (step.getPreviousStep() is HasStep) {
                val previousStep: HasStep<*> = step.getPreviousStep() as HasStep<*>
                for (hasContainer in step.getHasContainers()) {
                    previousStep.addHasContainer(hasContainer)
                }
                TraversalHelper.copyLabels(step, previousStep, false)
                traversal.removeStep(step)
                true
            } else if (step.getPreviousStep() is VertexStep
                && (step.getPreviousStep() as VertexStep).returnsEdge()
                && 0 == (step.getPreviousStep() as VertexStep).getEdgeLabels().length
            ) {
                val previousStep: VertexStep<Edge> = step.getPreviousStep() as VertexStep<Edge>
                val edgeLabels: List<String> = ArrayList()
                for (hasContainer in ArrayList(step.getHasContainers())) {
                    if (hasContainer.getKey().equals(T.label.getAccessor())) {
                        if (hasContainer.getBiPredicate() === Compare.eq &&
                            hasContainer.getValue() is String &&
                            edgeLabels.isEmpty()
                        ) {
                            edgeLabels.add(hasContainer.getValue() as String)
                            step.removeHasContainer(hasContainer)
                        } else if (hasContainer.getBiPredicate() === Contains.within &&
                            hasContainer.getValue() is Collection &&
                            (hasContainer.getValue() as Collection).containsAll(edgeLabels)
                        ) {
                            edgeLabels.addAll(hasContainer.getValue() as Collection<String?>)
                            step.removeHasContainer(hasContainer)
                        } else if (hasContainer.getPredicate() is OrP && edgeLabels.isEmpty()) {
                            var removeContainer = true
                            val orps: List<P<*>> = (hasContainer.getPredicate() as OrP).getPredicates()
                            val newEdges: List<String> = ArrayList()
                            for (i in 0 until orps.size()) {
                                if (orps[i].getBiPredicate() === Compare.eq && orps[i].getValue() is String) newEdges.add(
                                    orps[i].getValue() as String
                                ) else {
                                    removeContainer = false
                                    break
                                }
                            }
                            if (removeContainer) {
                                edgeLabels.addAll(newEdges)
                                step.removeHasContainer(hasContainer)
                            }
                        }
                    }
                }
                if (!edgeLabels.isEmpty()) {
                    val newVertexStep: VertexStep<Edge> = VertexStep(
                        traversal,
                        Edge::class.java,
                        previousStep.getDirection(),
                        edgeLabels.toArray(arrayOfNulls<String>(edgeLabels.size()))
                    )
                    TraversalHelper.replaceStep(previousStep, newVertexStep, traversal)
                    TraversalHelper.copyLabels(previousStep, newVertexStep, false)
                    if (step.getHasContainers().isEmpty()) {
                        TraversalHelper.copyLabels(step, newVertexStep, false)
                        traversal.removeStep(step)
                    }
                    return true
                }
                false
            } else false
        }

        private fun processTraversalFilterStep(
            step: TraversalFilterStep<*>,
            traversal: Traversal.Admin<*, *>
        ): Boolean {
            val childTraversal: Traversal.Admin<*, *> = step.getLocalChildren().get(0)
            if (TraversalHelper.hasAllStepsOfClass(childTraversal, FilterStep::class.java) &&
                !TraversalHelper.hasStepOfClass(
                    childTraversal,
                    DropStep::class.java,
                    RangeGlobalStep::class.java,
                    DedupGlobalStep::class.java,
                    LambdaHolder::class.java
                )
            ) {
                val finalStep: Step<*, *> = childTraversal.getEndStep()
                TraversalHelper.insertTraversal(step as Step, childTraversal, traversal)
                TraversalHelper.copyLabels(step, finalStep, false)
                traversal.removeStep(step)
                return true
            }
            return false
        }

        private fun processOrStep(step: OrStep<*>, traversal: Traversal.Admin<*, *>): Boolean {
            var process = true
            var key: String? = null
            var predicate: P? = null
            val labels: List<String> = ArrayList()
            for (childTraversal in step.getLocalChildren()) {
                instance().apply(childTraversal) // todo: this may be a bad idea, but I can't seem to find a test case to break it
                for (childStep in childTraversal.getSteps()) {
                    if (childStep is HasStep) {
                        var p: P? = null
                        for (hasContainer in (childStep as HasStep<*>).getHasContainers()) {
                            if (null == key) key = hasContainer.getKey() else if (!hasContainer.getKey().equals(key)) {
                                process = false
                                break
                            }
                            p = if (null == p) hasContainer.getPredicate() else p.and(hasContainer.getPredicate())
                        }
                        if (process) {
                            predicate = if (null == predicate) p else predicate.or(p)
                        }
                        labels.addAll(childStep.getLabels())
                    } else {
                        process = false
                        break
                    }
                }
                if (!process) break
            }
            if (process) {
                val hasStep = HasStep(traversal, HasContainer(key, predicate))
                TraversalHelper.replaceStep(step, hasStep, traversal)
                TraversalHelper.copyLabels(step, hasStep, false)
                for (label in labels) {
                    hasStep.addLabel(label)
                }
                return true
            }
            return false
        }

        private fun processAndStep(step: AndStep<*>, traversal: Traversal.Admin<*, *>): Boolean {
            var process = true
            for (childTraversal in step.getLocalChildren()) {
                if (!TraversalHelper.hasAllStepsOfClass(childTraversal, FilterStep::class.java) ||
                    TraversalHelper.hasStepOfClass(
                        childTraversal,
                        DropStep::class.java,
                        RangeGlobalStep::class.java,
                        DedupGlobalStep::class.java,
                        LambdaHolder::class.java
                    )
                ) {
                    process = false
                    break
                }
            }
            if (process) {
                val childTraversals: List<Traversal.Admin<*, *>> = step.getLocalChildren()
                var finalStep: Step<*, *>? = null
                for (i in childTraversals.size() - 1 downTo 0) {
                    val childTraversal: Traversal.Admin<*, *> = childTraversals[i]
                    if (null == finalStep) finalStep = childTraversal.getEndStep()
                    TraversalHelper.insertTraversal(step as Step, childTraversals[i], traversal)
                }
                if (null != finalStep) TraversalHelper.copyLabels(step, finalStep, false)
                traversal.removeStep(step)
                return true
            }
            return false
        }

        private fun processMatchStep(step: MatchStep<*, *>, traversal: Traversal.Admin<*, *>): Boolean {
            if (step.getPreviousStep() is EmptyStep) return false
            var changed = false
            val startLabel: String = MatchStep.Helper.computeStartLabel(step.getGlobalChildren())
            for (matchTraversal in ArrayList(step.getGlobalChildren())) {
                if (TraversalHelper.hasAllStepsOfClass(
                        matchTraversal,
                        HasStep::class.java,
                        MatchStartStep::class.java,
                        MatchEndStep::class.java
                    ) &&
                    matchTraversal.getStartStep() is MatchStartStep &&
                    startLabel.equals((matchTraversal.getStartStep() as MatchStartStep).getSelectKey().orElse(null))
                ) {
                    changed = true
                    step.removeGlobalChild(matchTraversal)
                    val endLabel: String = (matchTraversal.getEndStep() as MatchEndStep).getMatchKey()
                        .orElse(null) // why would this exist? but just in case
                    matchTraversal.removeStep(0) // remove MatchStartStep
                    matchTraversal.removeStep(matchTraversal.getSteps().size() - 1) // remove MatchEndStep
                    matchTraversal.getEndStep().addLabel(startLabel)
                    if (null != endLabel) matchTraversal.getEndStep().addLabel(endLabel)
                    TraversalHelper.insertTraversal(step.getPreviousStep() as Step, matchTraversal, traversal)
                }
            }
            if (step.getGlobalChildren().isEmpty()) traversal.removeStep(step)
            return changed
        }

        fun instance(): InlineFilterStrategy {
            return INSTANCE
        }
    }
}