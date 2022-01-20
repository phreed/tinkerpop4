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

import org.apache.tinkerpop4.gremlin.process.traversal.Step

/**
 * `MatchWhereStrategy` will fold any post-`where()` step that maintains a traversal constraint into
 * `match()`. [MatchStep] is intelligent with traversal constraint applications and thus, can more
 * efficiently use the constraint of [WhereTraversalStep] or [WherePredicateStep].
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @example <pre>
 * __.match(a,b).where(c)            // is replaced by __.match(a,b,c)
 * __.match(a,b).select().where(c)   // is replaced by __.match(a,b,c).select()
</pre> *
 */
class MatchPredicateStrategy private constructor() : AbstractTraversalStrategy<OptimizationStrategy?>(),
    OptimizationStrategy {
    @Override
    fun apply(traversal: Traversal.Admin<*, *>) {
        if (!TraversalHelper.hasStepOfClass(MatchStep::class.java, traversal)) return
        TraversalHelper.getStepsOfClass(MatchStep::class.java, traversal).forEach { matchStep ->
            // match().select().where() --> match(where()).select()
            // match().select().dedup() --> match(dedup()).select()
            var nextStep: Step<*, *> = matchStep.getNextStep()
            while (nextStep is WherePredicateStep ||
                nextStep is WhereTraversalStep ||
                nextStep is DedupGlobalStep && !(nextStep as DedupGlobalStep).getScopeKeys()
                    .isEmpty() && (nextStep as DedupGlobalStep).getLocalChildren().isEmpty() ||
                nextStep is SelectStep && (nextStep as SelectStep).getLocalChildren().isEmpty() ||
                nextStep is SelectOneStep && (nextStep as SelectOneStep).getLocalChildren().isEmpty()
            ) {
                nextStep = if (nextStep is WherePredicateStep || nextStep is WhereTraversalStep) {
                    traversal.removeStep(nextStep)
                    matchStep.addGlobalChild(
                        if (traversal is GraphTraversal) DefaultGraphTraversal().addStep(nextStep) else DefaultTraversal().addStep(
                            nextStep
                        )
                    )
                    matchStep.getNextStep()
                } else if (nextStep is DedupGlobalStep && !(nextStep as DedupGlobalStep).getScopeKeys()
                        .isEmpty() && (nextStep as DedupGlobalStep).getLocalChildren()
                        .isEmpty() && !TraversalHelper.onGraphComputer(traversal)
                ) {
                    traversal.removeStep(nextStep)
                    matchStep.setDedupLabels((nextStep as DedupGlobalStep<*>).getScopeKeys())
                    matchStep.getNextStep()
                } else if (nextStep.getLabels().isEmpty()) {
                    nextStep.getNextStep()
                } else break
            }
        }
    }

    @Override
    fun applyPrior(): Set<Class<out OptimizationStrategy?>> {
        return PRIORS
    }

    @Override
    fun applyPost(): Set<Class<out OptimizationStrategy?>> {
        return POSTS
    }

    companion object {
        private val INSTANCE = MatchPredicateStrategy()
        private val PRIORS: Set<Class<out OptimizationStrategy?>> =
            Collections.singleton(IdentityRemovalStrategy::class.java)
        private val POSTS: Set<Class<out OptimizationStrategy?>> =
            Collections.singleton(FilterRankingStrategy::class.java)

        fun instance(): MatchPredicateStrategy {
            return INSTANCE
        }
    }
}