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
package org.apache.tinkerpop4.gremlin.process.traversal.strategy.verification

import org.apache.tinkerpop4.gremlin.process.computer.traversal.step.VertexComputing

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class StandardVerificationStrategy private constructor() :
    AbstractTraversalStrategy<TraversalStrategy.VerificationStrategy?>(), TraversalStrategy.VerificationStrategy {
    @Override
    fun apply(traversal: Traversal.Admin<*, *>) {
        if (!traversal.getStrategies().getStrategy(ComputerFinalizationStrategy::class.java).isPresent() &&
            !traversal.getStrategies().getStrategy(ComputerVerificationStrategy::class.java).isPresent()
        ) {
            if (!TraversalHelper.getStepsOfAssignableClass(VertexComputing::class.java, traversal)
                    .isEmpty()
            ) throw VerificationException(
                "VertexComputing steps must be executed with a GraphComputer: " + TraversalHelper.getStepsOfAssignableClass(
                    VertexComputing::class.java, traversal
                ), traversal
            )
        }
        for (step in traversal.getSteps()) {
            for (label in HashSet(step.getLabels())) {
                if (Graph.Hidden.isHidden(label)) step.removeLabel(label)
            }
            if (step is ReducingBarrierStep && step.getTraversal().getParent() is RepeatStep && step.getTraversal()
                    .getParent().getGlobalChildren().get(0).getSteps().contains(step)
            ) throw VerificationException(
                "The parent of a reducing barrier can not be repeat()-step: $step", traversal
            )
        }

        // The ProfileSideEffectStep must be one of the following
        // (1) the last step
        // (2) 2nd last step when accompanied by the cap step or none step (i.e. iterate() to nothing)
        // (3) 3rd to last when the traversal ends with a RequirementsStep.
        val endStep: Step<*, *> = traversal.asAdmin().getEndStep()
        if (TraversalHelper.hasStepOfClass(ProfileSideEffectStep::class.java, traversal) &&
            !(endStep is ProfileSideEffectStep ||
                    endStep is SideEffectCapStep && endStep.getPreviousStep() is ProfileSideEffectStep ||
                    endStep is NoneStep && endStep.getPreviousStep() is SideEffectCapStep && endStep.getPreviousStep()
                .getPreviousStep() is ProfileSideEffectStep ||
                    endStep is RequirementsStep && endStep.getPreviousStep() is SideEffectCapStep && endStep.getPreviousStep()
                .getPreviousStep() is ProfileSideEffectStep ||
                    endStep is RequirementsStep && endStep.getPreviousStep() is NoneStep && endStep.getPreviousStep()
                .getPreviousStep() is SideEffectCapStep && endStep.getPreviousStep().getPreviousStep()
                .getPreviousStep() is ProfileSideEffectStep)
        ) {
            throw VerificationException(
                "When specified, the profile()-Step must be the last step or followed only by the cap()-step.",
                traversal
            )
        }
        if (TraversalHelper.getStepsOfClass(ProfileSideEffectStep::class.java, traversal).size() > 1) {
            throw VerificationException("The profile()-Step cannot be specified multiple times.", traversal)
        }
    }

    @Override
    fun applyPrior(): Set<Class<out VerificationStrategy?>> {
        return Collections.singleton(ComputerVerificationStrategy::class.java)
    }

    companion object {
        private val INSTANCE = StandardVerificationStrategy()
        fun instance(): StandardVerificationStrategy {
            return INSTANCE
        }
    }
}