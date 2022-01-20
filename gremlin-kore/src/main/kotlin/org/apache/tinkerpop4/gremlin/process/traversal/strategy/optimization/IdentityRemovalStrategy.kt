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
package org.apache.tinkerpop4.gremlin.process.traversal.strategy.optimization

import org.apache.tinkerpop4.gremlin.process.traversal.Traversal

/**
 * `IdentityRemovalStrategy` looks for [IdentityStep] instances and removes them.
 * If the identity step is labeled, its labels are added to the previous step.
 * If the identity step is labeled and it's the first step in the traversal, it stays.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @example <pre>
 * __.out().identity().count()            // is replaced by __.out().count()
 * __.in().identity().as("a")             // is replaced by __.in().as("a")
 * __.identity().as("a").out()            // is replaced by __.identity().as("a").out()
</pre> *
 */
class IdentityRemovalStrategy private constructor() : AbstractTraversalStrategy<OptimizationStrategy?>(),
    OptimizationStrategy {
    @Override
    fun apply(traversal: Traversal.Admin<*, *>) {
        // if there is just one step we would keep the step whether it was identity() or not.
        if (traversal.getSteps().size() <= 1) return
        for (identityStep in TraversalHelper.getStepsOfClass(IdentityStep::class.java, traversal)) {
            // with no labels on the identity() it can just be dropped. if there are labels then they should be
            // moved to the previous step. if there is no previous step then this is a start of a labelled traversal
            // and is kept
            if (identityStep.getLabels().isEmpty() || identityStep.getPreviousStep() !is EmptyStep) {

                // for branch()/union() type steps an EndStep gets added which would lead to something like:
                // [UnionStep([[VertexStep(OUT,vertex), EndStep], [EndStep], [VertexStep(OUT,vertex), EndStep]])]
                // if the identity() was removed. seems to make sense to account for that case so that the traversal
                // gets to be:
                // [UnionStep([[VertexStep(OUT,vertex), EndStep], [IdentityStep, EndStep], [VertexStep(OUT,vertex), EndStep]])]
                // EndStep seems to just behave like a identity() in the above case, but perhaps it is more consistent
                // to keep the identity() placeholder rather than a step that doesn't actually exist
                if (!(identityStep.getNextStep() is EndStep && traversal.getSteps().size() === 2)) {
                    TraversalHelper.copyLabels(identityStep, identityStep.getPreviousStep(), false)
                    traversal.removeStep(identityStep)
                }
            }
        }
    }

    companion object {
        private val INSTANCE = IdentityRemovalStrategy()
        fun instance(): IdentityRemovalStrategy {
            return INSTANCE
        }
    }
}