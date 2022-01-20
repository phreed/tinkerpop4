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

import org.apache.tinkerpop4.gremlin.process.traversal.Step

/**
 * `LazyBarrierStrategy` is an OLTP-only strategy that automatically inserts a [NoOpBarrierStep] after every
 * [FlatMapStep] if neither path-tracking nor partial path-tracking is required, and the next step is not the
 * traversal's last step or a [Barrier]. [NoOpBarrierStep]s allow traversers to be bulked, thus this strategy
 * is meant to reduce memory requirements and improve the overall query performance.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @example <pre>
 * __.out().bothE().count()      // is replaced by __.out().barrier(2500).bothE().count()
 * __.both().both().valueMap()   // is replaced by __.both().barrier(2500).both().barrier(2500).valueMap()
</pre> *
 */
class LazyBarrierStrategy private constructor() : AbstractTraversalStrategy<OptimizationStrategy?>(),
    OptimizationStrategy {
    @Override
    fun apply(traversal: Traversal.Admin<*, *>) {
        // drop() is a problem for bulked edge/meta properties because of Property equality changes in TINKERPOP-2318
        // which made it so that a Property is equal if the key/value is equal. as a result, they bulk together which
        // is fine for almost all cases except when you wish to drop the property.
        if (TraversalHelper.onGraphComputer(traversal) ||
            traversal.getTraverserRequirements().contains(TraverserRequirement.PATH) ||
            TraversalHelper.hasStepOfAssignableClass(DropStep::class.java, traversal)
        ) return
        var foundFlatMap = false
        var labeledPath = false
        for (i in 0 until traversal.getSteps().size()) {
            val step: Step<*, *> = traversal.getSteps().get(i)
            if (step.getLabels().contains(BARRIER_PLACEHOLDER)) {
                TraversalHelper.insertAfterStep(NoOpBarrierStep(traversal, MAX_BARRIER_SIZE), step, traversal)
                step.removeLabel(BARRIER_PLACEHOLDER)
                if (step.getLabels().contains(BARRIER_COPY_LABELS)) {
                    step.removeLabel(BARRIER_COPY_LABELS)
                    TraversalHelper.copyLabels(step, step.getNextStep(), true)
                }
            }
            if (step is PathProcessor) {
                val keepLabels: Set<String> = (step as PathProcessor).getKeepLabels()
                if (null != keepLabels && keepLabels.isEmpty()) // if no more path data, then start barrier'ing again
                    labeledPath = false
            }
            if (step is FlatMapStep &&
                !(step is VertexStep && (step as VertexStep).returnsEdge()) ||
                step is GraphStep &&
                (i > 0 || (step as GraphStep).getIds().length >= BIG_START_SIZE ||
                        (step as GraphStep).getIds().length === 0 && step.getNextStep() !is HasStep)
            ) {

                // NoneStep, EmptyStep signify the end of the traversal where no barriers are really going to be
                // helpful after that. ProfileSideEffectStep means the traversal had profile() called on it and if
                // we don't account for that a barrier will inject at the end of the traversal where it wouldn't
                // be otherwise. LazyBarrierStrategy executes before the finalization strategy of ProfileStrategy
                // so additionally injected ProfileSideEffectStep instances should not have effect here.
                if (foundFlatMap && !labeledPath &&
                    step.getNextStep() !is Barrier &&
                    step.getNextStep() !is NoneStep &&
                    step.getNextStep() !is EmptyStep &&
                    step.getNextStep() !is ProfileSideEffectStep
                ) {
                    val noOpBarrierStep: Step = NoOpBarrierStep(traversal, MAX_BARRIER_SIZE)
                    TraversalHelper.copyLabels(step, noOpBarrierStep, true)
                    TraversalHelper.insertAfterStep(noOpBarrierStep, step, traversal)
                } else foundFlatMap = true
            }
            if (!step.getLabels().isEmpty()) labeledPath = true
        }
    }

    @Override
    fun applyPrior(): Set<Class<out OptimizationStrategy?>> {
        return PRIORS
    }

    companion object {
        val BARRIER_PLACEHOLDER: String = Graph.Hidden.hide("gremlin.lazyBarrier.position")
        val BARRIER_COPY_LABELS: String = Graph.Hidden.hide("gremlin.lazyBarrier.copyLabels")
        private val INSTANCE = LazyBarrierStrategy()
        private val PRIORS: Set<Class<out OptimizationStrategy?>> = HashSet(
            Arrays.asList(
                CountStrategy::class.java,
                PathRetractionStrategy::class.java,
                IncidentToAdjacentStrategy::class.java,
                AdjacentToIncidentStrategy::class.java,
                FilterRankingStrategy::class.java,
                InlineFilterStrategy::class.java,
                MatchPredicateStrategy::class.java,
                EarlyLimitStrategy::class.java,
                RepeatUnrollStrategy::class.java
            )
        )
        private const val BIG_START_SIZE = 5
        protected const val MAX_BARRIER_SIZE = 2500
        fun instance(): LazyBarrierStrategy {
            return INSTANCE
        }
    }
}