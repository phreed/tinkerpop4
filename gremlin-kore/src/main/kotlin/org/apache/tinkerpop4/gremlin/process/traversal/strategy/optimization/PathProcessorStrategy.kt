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

import org.apache.tinkerpop4.gremlin.process.computer.traversal.step.map.VertexProgramStep

/**
 * `PathProcessStrategy` is an OLAP strategy that does its best to turn non-local children in `where()`
 * and `select()` into local children by inlining components of the non-local child. In this way,
 * `PathProcessStrategy` helps to ensure that more traversals meet the local child constraint imposed on OLAP
 * traversals.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @example <pre>
 * __.select(a).by(x)               // is replaced by select(a).map(x)
 * __.select(a,b).by(x).by(y)       // is replaced by select(a).by(x).as(a).select(b).by(y).as(b).select(a,b)
 * __.where(as(a).out().as(b))      // is replaced by as(xyz).select(a).where(out().as(b)).select(xyz)
 * __.where(as(a).out())            // is replaced by as(xyz).select(a).filter(out()).select(xyz)
</pre> *
 */
class PathProcessorStrategy private constructor() : AbstractTraversalStrategy<OptimizationStrategy?>(),
    OptimizationStrategy {
    @Override
    fun applyPrior(): Set<Class<out OptimizationStrategy?>> {
        return Collections.singleton(MatchPredicateStrategy::class.java)
    }

    @Override
    fun applyPost(): Set<Class<out OptimizationStrategy?>> {
        return Collections.singleton(InlineFilterStrategy::class.java)
    }

    @Override
    fun apply(traversal: Traversal.Admin<*, *>) {
        // using a hidden label marker to denote whether the traversal should not be processed by this strategy
        if ((traversal.getParent() is EmptyStep || traversal.getParent() is VertexProgramStep) &&
            TraversalHelper.hasStepOfAssignableClassRecursively(INVALIDATING_STEP_CLASSES, traversal)
        ) TraversalHelper.applyTraversalRecursively({ t ->
            t.getStartStep().addLabel(
                MARKER
            )
        }, traversal)
        if (traversal.getStartStep().getLabels().contains(MARKER)) {
            traversal.getStartStep().removeLabel(MARKER)
            return
        }

        // process where(as("a").out()...) => select("a").where(out()...)
        val whereTraversalSteps: List<WhereTraversalStep> =
            TraversalHelper.getStepsOfClass(WhereTraversalStep::class.java, traversal)
        for (whereTraversalStep in whereTraversalSteps) {
            val localChild: Traversal.Admin<*, *> = whereTraversalStep.getLocalChildren().get(0)
            if (localChild.getStartStep() is WhereStartStep &&
                !(localChild.getStartStep() as WhereStartStep).getScopeKeys().isEmpty()
            ) {
                var done = false
                while (!done) {
                    done = true
                    val index: Int = TraversalHelper.stepIndex(whereTraversalStep, traversal)
                    if (whereTraversalStep.getPreviousStep() is SelectStep) {
                        done = false
                        traversal.removeStep(index)
                        traversal.addStep(index - 1, whereTraversalStep)
                    }
                }
                val whereStartStep: WhereStartStep<*> = localChild.getStartStep() as WhereStartStep<*>
                var index: Int = TraversalHelper.stepIndex(whereTraversalStep, traversal)
                val selectOneStep: SelectOneStep<*, *> =
                    SelectOneStep(traversal, Pop.last, whereStartStep.getScopeKeys().iterator().next())
                traversal.addStep(index, selectOneStep)
                val generatedLabel = generateLabel()
                if (selectOneStep.getPreviousStep() is EmptyStep) {
                    TraversalHelper.insertBeforeStep(IdentityStep(traversal), selectOneStep, traversal)
                    index++
                }
                selectOneStep.getPreviousStep().addLabel(generatedLabel)
                TraversalHelper.insertAfterStep(
                    SelectOneStep(traversal, Pop.last, generatedLabel),
                    whereTraversalStep,
                    traversal
                )
                whereStartStep.removeScopeKey()
                // process where(as("a").out()) => as('xyz').select("a").filter(out()).select('xyz')
                if (localChild.getEndStep() !is WhereEndStep) {
                    localChild.removeStep(localChild.getStartStep())
                    traversal.addStep(index + 1, TraversalFilterStep(traversal, localChild))
                    traversal.removeStep(whereTraversalStep)
                }
            }
        }

        // process select("a","b").by(...).by(...)
        val selectSteps: List<SelectStep> = TraversalHelper.getStepsOfClass(SelectStep::class.java, traversal)
        for (selectStep in selectSteps) {
            if (selectStep.getPop() !== Pop.all && selectStep.getPop() !== Pop.mixed && // TODO: necessary?
                selectStep.getMaxRequirement().compareTo(PathProcessor.ElementRequirement.ID) > 0
            ) {
                var oneLabel = true
                for (key in selectStep.getScopeKeys()) {
                    if (labelCount(key, TraversalHelper.getRootTraversal(traversal)) > 1) {
                        oneLabel = false
                        break
                    }
                }
                if (!oneLabel) continue
                val index: Int = TraversalHelper.stepIndex(selectStep, traversal)
                val byTraversals: Map<String, Traversal.Admin<Object, Object>> = selectStep.getByTraversals()
                val keys = arrayOfNulls<String>(byTraversals.size())
                var counter = 0
                for (entry in byTraversals.entrySet()) {
                    val selectOneStep = SelectOneStep(traversal, selectStep.getPop(), entry.getKey())
                    val mapStep: TraversalMapStep<*, *> = TraversalMapStep(traversal, entry.getValue().clone())
                    mapStep.addLabel(entry.getKey())
                    traversal.addStep(index + 1, mapStep)
                    traversal.addStep(index + 1, selectOneStep)
                    keys[counter++] = entry.getKey()
                }
                traversal.addStep(index + 1 + byTraversals.size() * 2, SelectStep(traversal, Pop.last, keys))
                traversal.removeStep(index)
            }
        }

        // process select("a").by(...)
        //
        // unfortunately, this strategy needs to know about ProductiveByStrategy. the ordering of strategies
        // doesn't have enough flexibility to handle this situation where ProductiveByStrategy can run prior
        // to this but also after ByModulatorOptimizationStrategy.
        if (!traversal.getStrategies().getStrategy(ProductiveByStrategy::class.java).isPresent()) {
            val selectOneSteps: List<SelectOneStep> =
                TraversalHelper.getStepsOfClass(SelectOneStep::class.java, traversal)
            for (selectOneStep in selectOneSteps) {
                if (selectOneStep.getPop() !== Pop.all && selectOneStep.getPop() !== Pop.mixed && // TODO: necessary?
                    selectOneStep.getMaxRequirement().compareTo(PathProcessor.ElementRequirement.ID) > 0 && labelCount(
                        selectOneStep.getScopeKeys().iterator().next(),
                        TraversalHelper.getRootTraversal(traversal)
                    ) <= 1
                ) {
                    val index: Int = TraversalHelper.stepIndex(selectOneStep, traversal)
                    val localChild: Traversal.Admin<*, *> = selectOneStep.getLocalChildren().get(0)
                    selectOneStep.removeLocalChild(localChild)
                    val mapStep: TraversalMapStep<*, *> = TraversalMapStep(traversal, localChild.clone())
                    traversal.addStep(index + 1, mapStep)
                }
            }
        }
    }

    companion object {
        private val INSTANCE = PathProcessorStrategy()
        private val IS_TESTING: Boolean = Boolean.valueOf(System.getProperty("is.testing", "false"))
        private val MARKER: String = Graph.Hidden.hide("gremlin.pathProcessor")
        private val INVALIDATING_STEP_CLASSES: Set<Class> =
            HashSet(Arrays.asList(PathStep::class.java, TreeStep::class.java, LambdaHolder::class.java))

        fun instance(): PathProcessorStrategy {
            return INSTANCE
        }

        private fun generateLabel(): String {
            return if (IS_TESTING) "xyz" else UUID.randomUUID().toString()
        }

        private fun labelCount(label: String, traversal: Traversal.Admin<*, *>): Int {
            var count = 0
            for (step in traversal.getSteps()) {
                if (step.getLabels().contains(label)) count++
                if (step is TraversalParent) {
                    count =
                        count + (step as TraversalParent).getLocalChildren().stream().map { t -> labelCount(label, t) }
                            .reduce(0) { a, b -> a + b }
                    count =
                        count + (step as TraversalParent).getGlobalChildren().stream().map { t -> labelCount(label, t) }
                            .reduce(0) { a, b -> a + b }
                }
            }
            return count
        }
    }
}