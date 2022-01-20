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
package org.apache.tinkerpop4.gremlin.process.traversal.strategy.decoration

import org.apache.tinkerpop4.gremlin.process.traversal.Step

/**
 * ConnectiveStrategy rewrites the binary conjunction form of `a.and().b` into a [AndStep] of
 * `and(a,b)` (likewise for [OrStep]).
 *
 *
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Daniel Kuppitz (http://gremlin.guru)
 * @example <pre>
 * __.has("name","stephen").or().where(__.out("knows").has("name","stephen"))   // is replaced by __.or(__.has("name","stephen"), __.where(__.out("knows").has("name","stephen")))
 * __.out("a").out("b").and().out("c").or().out("d")                            // is replaced by __.or(__.and(__.out("a").out("b"), __.out("c")), __.out("d"))
 * __.as("a").out().as("b").and().as("c").in().as("d")                          // is replaced by __.and(__.as("a").out().as("b"), __.as("c").in().as("d"))
</pre> *
 */
class ConnectiveStrategy private constructor() : AbstractTraversalStrategy<DecorationStrategy?>(), DecorationStrategy {
    @Override
    fun apply(traversal: Traversal.Admin<*, *>) {
        if (TraversalHelper.hasStepOfAssignableClass(ConnectiveStep::class.java, traversal)) {
            processConnectiveMarker(traversal)
        }
    }

    companion object {
        private val INSTANCE = ConnectiveStrategy()
        private fun legalCurrentStep(step: Step<*, *>): Boolean {
            return !(step is EmptyStep || step is ProfileSideEffectStep || step is HasNextStep ||
                    step is EndStep || step is StartStep && !StartStep.isVariableStartStep(step) ||
                    GraphStep.isStartStep(step))
        }

        private fun processConnectiveMarker(traversal: Traversal.Admin<*, *>) {
            processConjunctionMarker(OrStep::class.java, traversal)
            processConjunctionMarker(AndStep::class.java, traversal)
        }

        private fun processConjunctionMarker(
            markerClass: Class<out ConnectiveStep?>,
            traversal: Traversal.Admin<*, *>
        ) {
            val steps: List<Step> = traversal.getSteps()
            var i = 0
            while (i < steps.size()) {
                val step: Step = steps[i]
                if (step.getClass().equals(markerClass)) {
                    val currentStep: ConnectiveStep<*> = step as ConnectiveStep
                    if (currentStep.getLocalChildren().isEmpty()) {
                        var connectiveTraversal: Traversal.Admin<*, *>
                        currentStep.addLocalChild(__.start().asAdmin().also { connectiveTraversal = it })
                        var j = i - 1
                        while (j >= 0) {
                            val previousStep: Step = steps[j]
                            if (legalCurrentStep(previousStep)) {
                                connectiveTraversal.addStep(0, previousStep)
                                traversal.removeStep(previousStep)
                            } else break
                            i--
                            j--
                        }
                        i++
                        currentStep.addLocalChild(
                            connectiveTraversal(
                                connectiveTraversal,
                                currentStep
                            ).also { connectiveTraversal = it })
                        currentStep.getLabels().forEach(currentStep::removeLabel)
                        while (i < steps.size()) {
                            val nextStep: Step = steps[i]
                            if (legalCurrentStep(nextStep)) {
                                if (nextStep.getClass().equals(markerClass) &&
                                    (nextStep as ConnectiveStep).getLocalChildren().isEmpty()
                                ) {
                                    val nextConnectiveStep: ConnectiveStep<*> = nextStep as ConnectiveStep<*>
                                    currentStep.addLocalChild(
                                        connectiveTraversal(
                                            connectiveTraversal,
                                            nextConnectiveStep
                                        ).also { connectiveTraversal = it })
                                } else {
                                    connectiveTraversal.addStep(nextStep)
                                }
                                traversal.removeStep(nextStep)
                            } else break
                        }
                        if (currentStep is OrStep) {
                            currentStep.getLocalChildren().forEach { t ->
                                processConjunctionMarker(
                                    AndStep::class.java, t
                                )
                            }
                        }
                    }
                }
                i++
            }
        }

        private fun connectiveTraversal(
            connectiveTraversal: Traversal.Admin<*, *>,
            connectiveStep: ConnectiveStep
        ): Traversal.Admin<*, *> {
            val traversal: Traversal.Admin<*, *> = __.start().asAdmin()
            val conjunctionLabels: Set<String> = connectiveStep.getLabels()
            if (!conjunctionLabels.isEmpty()) {
                val startStep: StartStep<*> = StartStep(connectiveTraversal)
                conjunctionLabels.forEach(startStep::addLabel)
                traversal.addStep(startStep)
            }
            return traversal
        }

        fun instance(): ConnectiveStrategy {
            return INSTANCE
        }
    }
}