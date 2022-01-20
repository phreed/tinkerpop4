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
package org.apache.tinkerpop4.gremlin.process.traversal.strategy.finalization

import org.apache.tinkerpop4.gremlin.process.computer.traversal.step.map.VertexProgramStep

/**
 * @author Bob Briody (http://bobbriody.com)
 */
class ProfileStrategy private constructor() : AbstractTraversalStrategy<FinalizationStrategy?>(), FinalizationStrategy {
    @Override
    fun apply(traversal: Traversal.Admin<*, *>) {
        if (!traversal.getEndStep().getLabels().contains(MARKER) &&
            (traversal.isRoot() || traversal.getParent() is VertexProgramStep) &&
            TraversalHelper.hasStepOfAssignableClassRecursively(ProfileSideEffectStep::class.java, traversal)
        ) TraversalHelper.applyTraversalRecursively({ t -> t.getEndStep().addLabel(MARKER) }, traversal)
        if (traversal.getEndStep().getLabels().contains(MARKER)) {
            traversal.getEndStep().removeLabel(MARKER)
            // Add .profile() step after every pre-existing step.
            val steps: List<Step> = traversal.getSteps()
            val numSteps: Int = steps.size()
            for (i in 0 until numSteps) {
                // Do not inject profiling after ProfileSideEffectStep as this will be the last step on the root traversal.
                if (steps[i * 2] is ProfileSideEffectStep) break
                // Create and inject ProfileStep
                val profileStepToAdd = ProfileStep(traversal)
                traversal.addStep(i * 2 + 1, profileStepToAdd)
                val stepToBeProfiled: Step = traversal.getSteps().get(i * 2)
                if (stepToBeProfiled is ProfilingAware) {
                    (stepToBeProfiled as ProfilingAware).prepareForProfiling()
                }
            }
        }
    }

    companion object {
        private val INSTANCE = ProfileStrategy()
        private val MARKER: String = Graph.Hidden.hide("gremlin.profile")
        fun instance(): ProfileStrategy {
            return INSTANCE
        }
    }
}