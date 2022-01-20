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

import org.apache.tinkerpop4.gremlin.process.traversal.Compare

/**
 * This strategy optimizes any occurrence of [CountGlobalStep] followed by an [IsStep]. The idea is to limit
 * the number of incoming elements in a way that it's enough for the [IsStep] to decide whether it evaluates
 * `true` or `false`. If the traversal already contains a user supplied limit, the strategy won't
 * modify it.
 *
 * @author Daniel Kuppitz (http://gremlin.guru)
 * @example <pre>
 * __.outE().count().is(0)      // is replaced by __.not(outE())
 * __.outE().count().is(lt(3))  // is replaced by __.outE().limit(3).count().is(lt(3))
 * __.outE().count().is(gt(3))  // is replaced by __.outE().limit(4).count().is(gt(3))
</pre> *
 */
class CountStrategy private constructor() : AbstractTraversalStrategy<OptimizationStrategy?>(), OptimizationStrategy {
    @Override
    fun apply(traversal: Traversal.Admin<*, *>) {
        val parent: TraversalParent = traversal.getParent()
        var size: Int = traversal.getSteps().size()
        var prev: Step? = null
        var i = 0
        while (i < size) {
            val curr: Step = traversal.getSteps().get(i)
            if (i < size - 1 && doStrategy(curr)) {
                val isStep: IsStep = traversal.getSteps().get(i + 1) as IsStep
                val isStepPredicate: P = isStep.getPredicate()
                var highRange: Long? = null
                var useNotStep = false
                var dismissCountIs = false
                for (p in if (isStepPredicate is ConnectiveP) (isStepPredicate as ConnectiveP<*>).getPredicates() else Collections.singletonList(
                    isStepPredicate
                )) {
                    val value: Object = p.getValue()
                    val predicate: BiPredicate = p.getBiPredicate()
                    if (value is Number) {
                        val highRangeOffset = if (INCREASED_OFFSET_SCALAR_PREDICATES.contains(predicate)) 1L else 0L
                        val highRangeCandidate = Math.ceil((value as Number).doubleValue()) as Long + highRangeOffset
                        val update = highRange == null || highRangeCandidate > highRange
                        if (update) {
                            if (parent is EmptyStep) {
                                useNotStep = false
                            } else {
                                if (parent is RepeatStep) {
                                    val repeatStep: RepeatStep = parent as RepeatStep
                                    useNotStep = (Objects.equals(traversal, repeatStep.getUntilTraversal())
                                            || Objects.equals(traversal, repeatStep.getEmitTraversal()))
                                    dismissCountIs = useNotStep
                                } else {
                                    useNotStep = parent is FilterStep || parent is SideEffectStep
                                    dismissCountIs = useNotStep
                                }
                            }
                            highRange = highRangeCandidate
                            useNotStep = useNotStep and (curr.getLabels().isEmpty() && isStep.getLabels().isEmpty()
                                    && isStep.getNextStep() is EmptyStep
                                    && (highRange <= 1L && predicate.equals(Compare.lt)
                                    || highRange == 1L && (predicate.equals(Compare.eq) || predicate.equals(Compare.lte))))
                            dismissCountIs =
                                dismissCountIs and (curr.getLabels().isEmpty() && isStep.getLabels().isEmpty()
                                        && isStep.getNextStep() is EmptyStep
                                        && highRange == 1L && (predicate.equals(Compare.gt) || predicate.equals(Compare.gte)))
                        }
                    } else {
                        val highRangeOffset = RANGE_PREDICATES[predicate]
                        if (value is Collection && highRangeOffset != null) {
                            val high: Object = Collections.max(value as Collection)
                            if (high is Number) {
                                val highRangeCandidate: Long = (high as Number).longValue() + highRangeOffset
                                val update = highRange == null || highRangeCandidate > highRange
                                if (update) highRange = highRangeCandidate
                            }
                        }
                    }
                }
                if (highRange != null) {
                    if (useNotStep || dismissCountIs) {
                        traversal.asAdmin().removeStep(isStep) // IsStep
                        traversal.asAdmin().removeStep(curr) // CountStep
                        size -= 2
                        if (!dismissCountIs) {
                            var p: TraversalParent
                            if (traversal.getParent().also { p = it } is FilterStep && p !is ConnectiveStep) {
                                val filterStep: Step<*, *> = parent.asStep()
                                val parentTraversal: Traversal.Admin = filterStep.getTraversal()
                                val notStep: Step = NotStep(
                                    parentTraversal,
                                    if (traversal.getSteps().isEmpty()) __.identity() else traversal
                                )
                                filterStep.getLabels().forEach(notStep::addLabel)
                                TraversalHelper.replaceStep(filterStep, notStep, parentTraversal)
                            } else {
                                val inner: Traversal.Admin
                                if (prev != null) {
                                    inner = __.start().asAdmin()
                                    while (true) {
                                        val pp: Step = prev.getPreviousStep()
                                        inner.addStep(0, prev)
                                        if (pp is EmptyStep || pp is GraphStep ||
                                            !(prev is FilterStep || prev is SideEffectStep)
                                        ) break
                                        traversal.removeStep(prev)
                                        prev = pp
                                        size--
                                    }
                                } else {
                                    inner = __.identity().asAdmin()
                                }
                                if (prev != null) TraversalHelper.replaceStep(
                                    prev,
                                    NotStep(traversal, inner),
                                    traversal
                                ) else traversal.asAdmin().addStep(NotStep(traversal, inner))
                            }
                        } else if (size == 0) {
                            val parentStep: Step = traversal.getParent().asStep()
                            if (parentStep !is EmptyStep) {
                                val parentTraversal: Traversal.Admin = parentStep.getTraversal()
                                //parentTraversal.removeStep(parentStep); // this leads to IndexOutOfBoundsExceptions
                                TraversalHelper.replaceStep(parentStep, IdentityStep(parentTraversal), parentTraversal)
                            }
                        }
                    } else {
                        TraversalHelper.insertBeforeStep(RangeGlobalStep(traversal, 0L, highRange), curr, traversal)
                    }
                    i++
                }
            }
            prev = curr
            i++
        }
    }

    private fun doStrategy(step: Step): Boolean {
        if (step !is CountGlobalStep ||
            step.getNextStep() !is IsStep ||
            step.getPreviousStep() is RangeGlobalStep
        ) // if a RangeStep was provided, assume that the user knows what he's doing
            return false
        val parent: Step = step.getTraversal().getParent().asStep()
        return (parent is FilterStep || parent.getLabels()
            .isEmpty()) &&  // if the parent is labeled, then the count matters
                !(parent.getNextStep() is MatchEndStep &&  // if this is in a pattern match, then don't do it.
                        (parent.getNextStep() as MatchEndStep).getMatchKey().isPresent())
    }

    companion object {
        private val RANGE_PREDICATES: Map<BiPredicate, Long> = object : HashMap<BiPredicate?, Long?>() {
            init {
                put(Contains.within, 1L)
                put(Contains.without, 0L)
            }
        }
        private val INCREASED_OFFSET_SCALAR_PREDICATES: Set<Compare> =
            EnumSet.of(Compare.eq, Compare.neq, Compare.lte, Compare.gt)
        private val INSTANCE = CountStrategy()
        fun instance(): CountStrategy {
            return INSTANCE
        }
    }
}