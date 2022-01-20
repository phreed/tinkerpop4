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

import org.apache.tinkerpop4.gremlin.process.computer.traversal.step.map.ComputerResultStep

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class ComputerVerificationStrategy private constructor() :
    AbstractTraversalStrategy<TraversalStrategy.VerificationStrategy?>(), TraversalStrategy.VerificationStrategy {
    @Override
    fun apply(traversal: Traversal.Admin<*, *>) {
        if (!TraversalHelper.onGraphComputer(traversal)) return
        if (traversal.getParent() is TraversalVertexProgramStep) {
            if (TraversalHelper.getStepsOfAssignableClassRecursively(GraphStep::class.java, traversal)
                    .size() > 1
            ) throw VerificationException(
                "Mid-traversal V()/E() is currently not supported on GraphComputer",
                traversal
            )
            if (TraversalHelper.hasStepOfAssignableClassRecursively(
                    ProfileStep::class.java,
                    traversal
                ) && TraversalHelper.getStepsOfAssignableClass(
                    VertexProgramStep::class.java, TraversalHelper.getRootTraversal(traversal)
                ).size() > 1
            ) throw VerificationException(
                "Profiling a multi-VertexProgramStep traversal is currently not supported on GraphComputer",
                traversal
            )
        }

        // this is a problem because sideEffect.merge() is transient on the OLAP reduction
        if (TraversalHelper.getRootTraversal(traversal).getTraverserRequirements()
                .contains(TraverserRequirement.ONE_BULK)
        ) throw VerificationException(
            "One bulk is currently not supported on GraphComputer: $traversal", traversal
        )

        // you can not traverse past the local star graph with localChildren (e.g. by()-modulators).
        if (!TraversalHelper.isGlobalChild(traversal) && !TraversalHelper.isLocalStarGraph(traversal)) throw VerificationException(
            "Local traversals may not traverse past the local star-graph on GraphComputer: $traversal", traversal
        )
        for (step in traversal.getSteps()) {
            if (step is PathProcessor && (step as PathProcessor).getMaxRequirement() !== PathProcessor.ElementRequirement.ID) throw VerificationException(
                "It is not possible to access more than a path element's id on GraphComputer: " + step + " requires " + (step as PathProcessor).getMaxRequirement(),
                traversal
            )
            if (UNSUPPORTED_STEPS.stream().filter { c -> c.isAssignableFrom(step.getClass()) }
                    .findFirst().isPresent()) throw VerificationException(
                "The following step is currently not supported on GraphComputer: $step",
                traversal
            )
        }
        var nextParentStep: Step<*, *> = traversal.getParent().asStep()
        while (nextParentStep !is EmptyStep) {
            if (nextParentStep is PathProcessor && (nextParentStep as PathProcessor).getMaxRequirement() !== PathProcessor.ElementRequirement.ID) throw VerificationException(
                "The following path processor step requires more than the element id on GraphComputer: " + nextParentStep + " requires " + (nextParentStep as PathProcessor).getMaxRequirement(),
                traversal
            )
            nextParentStep = nextParentStep.getNextStep()
        }
    }

    companion object {
        private val INSTANCE = ComputerVerificationStrategy()
        private val UNSUPPORTED_STEPS: Set<Class<*>> = HashSet(
            Arrays.asList(
                InjectStep::class.java,
                Mutating::class.java,
                SubgraphStep::class.java,
                ComputerResultStep::class.java,
                IoStep::class.java
            )
        )

        fun instance(): ComputerVerificationStrategy {
            return INSTANCE
        }
    }
}