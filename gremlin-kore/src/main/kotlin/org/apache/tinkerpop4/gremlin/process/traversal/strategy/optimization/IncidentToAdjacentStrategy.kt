/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.tinkerpop4.gremlin.process.traversal.strategy.optimization

import org.apache.tinkerpop4.gremlin.process.computer.traversal.step.map.VertexProgramStep

/**
 * This strategy looks for `.outE().inV()`, `.inE().outV()` and `.bothE().otherV()`
 * and replaces these step sequences with `.out()`, `.in()` or `.both()` respectively.
 * The strategy won't modify the traversal if:
 *
 *
 *
 *  * the edge step is labeled
 *  * the traversal contains a `path` step
 *  * the traversal contains a lambda step
 *
 *
 *
 *
 * By re-writing the traversal in this fashion, the traversal eliminates unnecessary steps and becomes more normalized.
 *
 * @author Daniel Kuppitz (http://gremlin.guru)
 * @example <pre>
 * __.outE().inV()         // is replaced by __.out()
 * __.inE().outV()         // is replaced by __.in()
 * __.bothE().otherV()     // is replaced by __.both()
 * __.bothE().bothV()      // will not be modified
 * __.outE().inV().path()  // will not be modified
 * __.outE().inV().tree()  // will not be modified
</pre> *
 */
class IncidentToAdjacentStrategy private constructor() : AbstractTraversalStrategy<OptimizationStrategy?>(),
    OptimizationStrategy {
    @Override
    fun apply(traversal: Traversal.Admin<*, *>) {
        // using a hidden label marker to denote whether the traversal should not be processed by this strategy
        if ((traversal.isRoot() || traversal.getParent() is VertexProgramStep) &&
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
        ////////////////////////////////////////////////////////////////////////////
        val stepsToReplace: Collection<Pair<VertexStep, Step>> = ArrayList()
        var prev: Step? = null
        for (curr in traversal.getSteps()) {
            if (curr is TraversalParent) {
                (curr as TraversalParent).getLocalChildren()
                    .forEach { traversal: Traversal.Admin<*, *> -> apply(traversal) }
                (curr as TraversalParent).getGlobalChildren()
                    .forEach { traversal: Traversal.Admin<*, *> -> apply(traversal) }
            }
            if (isOptimizable(prev, curr)) {
                stepsToReplace.add(Pair.with(prev as VertexStep?, curr))
            }
            prev = curr
        }
        if (!stepsToReplace.isEmpty()) {
            for (pair in stepsToReplace) {
                optimizeSteps(traversal, pair.getValue0(), pair.getValue1())
            }
        }
    }

    @Override
    fun applyPrior(): Set<Class<out OptimizationStrategy?>> {
        return Collections.singleton(IdentityRemovalStrategy::class.java)
    }

    @Override
    fun applyPost(): Set<Class<out OptimizationStrategy?>> {
        return Collections.singleton(PathRetractionStrategy::class.java)
    }

    companion object {
        private val INSTANCE = IncidentToAdjacentStrategy()
        private val MARKER: String = Graph.Hidden.hide("gremlin.incidentToAdjacent")
        private val INVALIDATING_STEP_CLASSES: Set<Class> = HashSet(
            Arrays.asList(
                PathStep::class.java,
                PathFilterStep::class.java,
                TreeStep::class.java,
                TreeSideEffectStep::class.java,
                LambdaHolder::class.java
            )
        )

        /**
         * Checks whether a given step is optimizable or not.
         *
         * @param step1 an edge-emitting step
         * @param step2 a vertex-emitting step
         * @return `true` if step1 is not labeled and emits edges and step2 emits vertices,
         * otherwise `false`
         */
        private fun isOptimizable(step1: Step?, step2: Step): Boolean {
            if (step1 is VertexStep && (step1 as VertexStep?).returnsEdge() && step1.getLabels().isEmpty()) {
                val step1Dir: Direction = (step1 as VertexStep?).getDirection()
                return if (step1Dir.equals(Direction.BOTH)) {
                    step2 is EdgeOtherVertexStep
                } else step2 is EdgeOtherVertexStep || step2 is EdgeVertexStep &&
                        (step2 as EdgeVertexStep).getDirection().equals(step1Dir.opposite())
            }
            return false
        }

        /**
         * Optimizes the given edge-emitting step and the vertex-emitting step by replacing them with a single
         * vertex-emitting step.
         *
         * @param traversal the traversal that holds the given steps
         * @param step1     the edge-emitting step to replace
         * @param step2     the vertex-emitting step to replace
         */
        private fun optimizeSteps(traversal: Traversal.Admin, step1: VertexStep, step2: Step) {
            val newStep: Step = VertexStep(traversal, Vertex::class.java, step1.getDirection(), step1.getEdgeLabels())
            for (label in step2.getLabels()) {
                newStep.addLabel(label)
            }
            TraversalHelper.replaceStep(step1, newStep, traversal)
            traversal.removeStep(step2)
        }

        fun instance(): IncidentToAdjacentStrategy {
            return INSTANCE
        }
    }
}