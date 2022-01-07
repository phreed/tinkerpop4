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
package org.apache.tinkerpop.gremlin.tinkercat.process.traversal.strategy.optimization

import org.apache.tinkerpop.gremlin.process.traversal.Step
import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.tinkercat.process.traversal.step.sideEffect.TinkerCatStep.addHasContainer
import org.apache.tinkerpop.gremlin.process.traversal.strategy.AbstractTraversalStrategy
import org.apache.tinkerpop.gremlin.process.traversal.TraversalStrategy.ProviderOptimizationStrategy
import org.apache.tinkerpop.gremlin.process.traversal.step.map.GraphStep
import org.apache.tinkerpop.gremlin.tinkercat.process.traversal.step.sideEffect.TinkerCatStep
import org.apache.tinkerpop.gremlin.process.traversal.step.filter.HasStep
import org.apache.tinkerpop.gremlin.process.traversal.step.map.NoOpBarrierStep
import org.apache.tinkerpop.gremlin.process.traversal.step.util.HasContainer
import org.apache.tinkerpop.gremlin.process.traversal.step.HasContainerHolder
import org.apache.tinkerpop.gremlin.process.traversal.util.TraversalHelper
import org.apache.tinkerpop.gremlin.tinkercat.process.traversal.strategy.optimization.TinkerCatStepStrategy

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class TinkerCatStepStrategy private constructor() : AbstractTraversalStrategy<ProviderOptimizationStrategy?>(),
    ProviderOptimizationStrategy {
    override fun apply(traversal: Traversal.Admin<*, *>) {
        if (TraversalHelper.onGraphComputer(traversal)) return
        for (originalGraphStep in TraversalHelper.getStepsOfClass(
            GraphStep::class.java, traversal
        )) {
            val tinkerGraphStep: TinkerCatStep<*, *> = TinkerCatStep<Any?, Any?>(originalGraphStep)
            TraversalHelper.replaceStep(originalGraphStep, tinkerGraphStep, traversal)
            var currentStep: Step<*, *> = tinkerGraphStep.nextStep
            while (currentStep is HasStep<*> || currentStep is NoOpBarrierStep<*>) {
                if (currentStep is HasStep<*>) {
                    for (hasContainer in (currentStep as HasContainerHolder).hasContainers) {
                        if (!GraphStep.processHasContainerIds(
                                tinkerGraphStep,
                                hasContainer
                            )
                        ) tinkerGraphStep.addHasContainer(
                            hasContainer!!
                        )
                    }
                    TraversalHelper.copyLabels(currentStep, currentStep.previousStep, false)
                    traversal.removeStep<Any, Any>(currentStep)
                }
                currentStep = currentStep.nextStep
            }
        }
    }

    companion object {
        private val INSTANCE = TinkerCatStepStrategy()
        @JvmStatic
        fun instance(): TinkerCatStepStrategy {
            return INSTANCE
        }
    }
}