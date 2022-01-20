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
package org.apache.tinkerpop4.gremlin.process.computer.traversal.strategy.optimization

import org.apache.tinkerpop4.gremlin.process.computer.traversal.step.map.TraversalVertexProgramStep

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class MessagePassingReductionStrategy private constructor() : AbstractTraversalStrategy<OptimizationStrategy?>(),
    OptimizationStrategy {
    @Override
    fun apply(traversal: Traversal.Admin<*, *>) {
        // only process the first traversal step in an OLAP chain
        TraversalHelper.getFirstStepOfAssignableClass(TraversalVertexProgramStep::class.java, traversal)
            .ifPresent { step ->
                val graph: Graph = traversal.getGraph()
                    .orElse(EmptyGraph.instance()) // best guess at what the graph will be as its dynamically determined
                val compiledComputerTraversal: Traversal.Admin<*, *> =
                    step.generateProgram(graph, EmptyMemory.instance()).getTraversal().get().clone()
                if (!compiledComputerTraversal.isLocked()) compiledComputerTraversal.applyStrategies()
                if (!TraversalHelper.hasStepOfAssignableClassRecursively(
                        Arrays.asList(
                            LocalStep::class.java,
                            LambdaHolder::class.java
                        ), compiledComputerTraversal
                    ) &&  // don't do anything with lambdas or locals as this leads to unknown adjacencies
                    !compiledComputerTraversal.getTraverserRequirements()
                        .contains(TraverserRequirement.PATH) &&  // when dynamic detachment is provided in 3.3.0, remove this (TODO)
                    !compiledComputerTraversal.getTraverserRequirements()
                        .contains(TraverserRequirement.LABELED_PATH) &&  // when dynamic detachment is provided in 3.3.0, remove this (TODO)
                    !TraversalHelper.getStepsOfAssignableClass(TraversalParent::class.java, compiledComputerTraversal)
                        .stream // this is a strict precaution that could be loosed with deeper logic on barriers in global children
                            ().filter { parent -> !parent.getGlobalChildren().isEmpty() }.findAny().isPresent()
                ) {
                    val computerTraversal: Traversal.Admin<*, *> = step.computerTraversal.get().clone()
                    // apply the strategies up to this point
                    val strategies: List<TraversalStrategy<*>> = step.getTraversal().getStrategies().toList()
                    if (computerTraversal.getSteps().size() > 1 &&
                        computerTraversal.getStartStep().getNextStep() !is Barrier &&
                        TraversalHelper.hasStepOfAssignableClassRecursively(
                            Arrays.asList(
                                VertexStep::class.java,
                                EdgeVertexStep::class.java
                            ), computerTraversal
                        ) &&
                        TraversalHelper.isLocalStarGraph(computerTraversal)
                    ) {
                        var barrier: Step =
                            TraversalHelper.getFirstStepOfAssignableClass(Barrier::class.java, computerTraversal)
                                .orElse(null) as Step

                        // if the barrier isn't present gotta check for uncapped profile() which can happen if you do
                        // profile("metrics") - see below for more worries
                        if (null == barrier) {
                            val pses: ProfileSideEffectStep = TraversalHelper.getFirstStepOfAssignableClass(
                                ProfileSideEffectStep::class.java, computerTraversal
                            ).orElse(null)
                            if (pses != null) barrier = pses.getPreviousStep()
                        }

                        // if the barrier is a profile() then we'll mess stuff up if we wrap that in a local() as in:
                        //    local(..., ProfileSideEffectStep)
                        // which won't compute right on OLAP (or anything??). By stepping back we cut things off at
                        // just before the ProfileSideEffectStep to go inside the local() so that ProfileSideEffectStep
                        // shows up just after it
                        //
                        // why does this strategy need to know so much about profile!?!
                        if (barrier != null && barrier.getPreviousStep() is ProfileSideEffectStep) barrier =
                            barrier.getPreviousStep().getPreviousStep()
                        if (insertElementId(barrier)) // out().count() -> out().id().count()
                            TraversalHelper.insertBeforeStep(IdStep(computerTraversal), barrier, computerTraversal)
                        if (!endsWithElement(if (null == barrier) computerTraversal.getEndStep() else barrier)) {
                            var newChildTraversal: Traversal.Admin = DefaultGraphTraversal()
                            TraversalHelper.removeToTraversal(
                                if (computerTraversal.getStartStep() is GraphStep) computerTraversal.getStartStep()
                                    .getNextStep() else computerTraversal.getStartStep() as Step,
                                if (null == barrier) EmptyStep.instance() else barrier,
                                newChildTraversal
                            )
                            newChildTraversal = if (newChildTraversal.getSteps()
                                    .size() > 1
                            ) __.local(newChildTraversal) as Traversal.Admin else newChildTraversal
                            if (null == barrier) TraversalHelper.insertTraversal(
                                0,
                                newChildTraversal,
                                computerTraversal
                            ) else TraversalHelper.insertTraversal(
                                barrier.getPreviousStep(),
                                newChildTraversal,
                                computerTraversal
                            )
                        }
                    }
                    step.setComputerTraversal(computerTraversal)
                }
            }
    }

    @Override
    fun applyPrior(): Set<Class<out OptimizationStrategy?>> {
        return PRIORS
    }

    companion object {
        private val INSTANCE = MessagePassingReductionStrategy()
        private val PRIORS: Set<Class<out OptimizationStrategy?>> = HashSet(
            Arrays.asList(
                IncidentToAdjacentStrategy::class.java,
                AdjacentToIncidentStrategy::class.java,
                FilterRankingStrategy::class.java,
                InlineFilterStrategy::class.java
            )
        )

        private fun insertElementId(barrier: Step<*, *>?): Boolean {
            return if (barrier !is Barrier) false else if (!endsWithElement(barrier.getPreviousStep())) false else if (barrier is CountGlobalStep) true else if (barrier is DedupGlobalStep &&
                (barrier as DedupGlobalStep?).getScopeKeys().isEmpty() &&
                (barrier as DedupGlobalStep?).getLocalChildren().isEmpty() &&
                barrier.getNextStep() is CountGlobalStep
            ) true else false
        }

        fun endsWithElement(currentStep: Step<*, *>): Boolean {
            var currentStep: Step<*, *> = currentStep
            while (currentStep !is EmptyStep) {
                if (currentStep is VertexStep) // only inE, in, and out send messages
                    return (currentStep as VertexStep).returnsVertex() || !(currentStep as VertexStep).getDirection()
                        .equals(Direction.OUT) else if (currentStep is EdgeVertexStep) // TODO: add GraphStep but only if its mid-traversal V()/E()
                    return true else if (currentStep is TraversalFlatMapStep || currentStep is TraversalMapStep || currentStep is LocalStep) return endsWithElement(
                    (currentStep as TraversalParent).getLocalChildren().get(0).getEndStep()
                ) else if (!(currentStep is FilterStep || currentStep is SideEffectStep || currentStep is IdentityStep || currentStep is Barrier)) return false
                currentStep = currentStep.getPreviousStep()
            }
            return false
        }

        fun instance(): MessagePassingReductionStrategy {
            return INSTANCE
        }
    }
}