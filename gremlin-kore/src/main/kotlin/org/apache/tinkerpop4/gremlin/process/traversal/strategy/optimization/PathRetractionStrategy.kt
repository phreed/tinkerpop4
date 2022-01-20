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
 * @author Ted Wilmes (http://twilmes.org)
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class PathRetractionStrategy private constructor(private val standardBarrierSize: Int) :
    AbstractTraversalStrategy<OptimizationStrategy?>(), OptimizationStrategy {
    @Override
    fun apply(traversal: Traversal.Admin<*, *>) {
        if (traversal.isRoot() && isNotApplicable(traversal)) {
            TraversalHelper.applyTraversalRecursively({ t -> t.getEndStep().addLabel(MARKER) }, traversal)
        }
        if (traversal.getEndStep().getLabels().contains(MARKER)) {
            traversal.getEndStep().removeLabel(MARKER)
            return
        }
        val lazyBarrierStrategyInstalled: Boolean =
            traversal.getStrategies().getStrategy(LazyBarrierStrategy::class.java).isPresent()
        val onGraphComputer: Boolean = TraversalHelper.onGraphComputer(traversal)
        val foundLabels: Set<String> = HashSet()
        val keepLabels: Set<String> = HashSet()
        val steps: List<Step> = traversal.getSteps()
        for (i in steps.size() - 1 downTo 0) {
            val currentStep: Step = steps[i]
            // maintain our list of labels to keep, repeatedly adding labels that were found during
            // the last iteration
            keepLabels.addAll(foundLabels)
            val labels: Set<String> = PathUtil.getReferencedLabels(currentStep)
            for (label in labels) {
                if (foundLabels.contains(label)) keepLabels.add(label) else foundLabels.add(label)
            }
            // add the keep labels to the path processor
            if (currentStep is PathProcessor) {
                val pathProcessor: PathProcessor = currentStep as PathProcessor
                // in versions prior to 3.5.0 we use to only keep labels if match() was last
                // or was followed by dedup() or select().where(), but the pattern matching
                // wasn't too smart and thus produced inconsistent/unexpected outputs. trying
                // to make gremlin be a bit less surprising for users and ultimately this seemed
                // like too much magic. finally, we really shouldn't rely have strategies relying
                // to heavily on one another for results to be consistent. a traversal should
                // produce the same results irrespective of zero, one or more strategies being
                // applied. so, for now this change adds back a bit of overhead in managing some
                // labels but produces results users expect.
                if (currentStep is MatchStep) {
                    pathProcessor.setKeepLabels((currentStep as MatchStep).getMatchStartLabels())
                    pathProcessor.getKeepLabels().addAll((currentStep as MatchStep).getMatchEndLabels())
                } else {
                    if (pathProcessor.getKeepLabels() == null) pathProcessor.setKeepLabels(keepLabels) else pathProcessor.getKeepLabels()
                        .addAll(HashSet(keepLabels))
                }
                if (currentStep.getTraversal().getParent() is MatchStep) {
                    pathProcessor.setKeepLabels(
                        (currentStep.getTraversal().getParent().asStep() as MatchStep).getMatchStartLabels()
                    )
                    pathProcessor.getKeepLabels()
                        .addAll((currentStep.getTraversal().getParent().asStep() as MatchStep).getMatchEndLabels())
                }

                // LazyBarrierStrategy should control all barrier() additions. OLTP barrier optimization that will try
                // and bulk traversers after a path processor step to thin the stream
                if (lazyBarrierStrategyInstalled && !onGraphComputer &&
                    currentStep !is MatchStep &&
                    currentStep !is Barrier &&
                    currentStep.getNextStep() !is Barrier &&
                    currentStep.getTraversal().getParent() !is MatchStep &&
                    currentStep.getNextStep() !is NoneStep &&
                    currentStep.getNextStep() !is EmptyStep
                ) TraversalHelper.insertAfterStep(
                    NoOpBarrierStep(traversal, standardBarrierSize),
                    currentStep,
                    traversal
                )
            }
        }
        keepLabels.addAll(foundLabels)

        // build a list of parent traversals and their required labels
        var parent: Step<*, *> = traversal.getParent().asStep()
        val parentKeeperPairs: List<Pair<Step, Set<String>>> = ArrayList()
        while (!parent.equals(EmptyStep.instance())) {
            val parentKeepLabels: Set<String> = HashSet(PathUtil.getReferencedLabels(parent))
            parentKeepLabels.addAll(PathUtil.getReferencedLabelsAfterStep(parent))
            parentKeeperPairs.add(Pair(parent, parentKeepLabels))
            parent = parent.getTraversal().getParent().asStep()
        }

        // reverse the parent traversal list so that labels are kept from the top down
        Collections.reverse(parentKeeperPairs)
        var hasRepeat = false
        val keeperTrail: Set<String> = HashSet()
        for (pair in parentKeeperPairs) {
            var step: Step = pair.getValue0()
            val levelLabels: Set<String> = pair.getValue1()
            if (step is RepeatStep) {
                hasRepeat = true
            }

            // if parent step is a TraversalParent itself and it has more than 1 child traversal
            // propagate labels to children
            if (step is TraversalParent) {
                val children: List<Traversal.Admin<Object, Object>> = ArrayList()
                children.addAll((step as TraversalParent).getGlobalChildren())
                children.addAll((step as TraversalParent).getLocalChildren())
                // if this is the only child traversal, do not re-push labels
                if (children.size() > 1) applyToChildren(keepLabels, children)
            }

            // propagate requirements of keep labels back through the traversal's previous steps
            // to ensure that the label is not dropped before it reaches the step(s) that require it
            step = step.getPreviousStep()
            while (!step.equals(EmptyStep.instance())) {
                if (step is PathProcessor) {
                    addLabels(step as PathProcessor, keepLabels)
                }
                if (step is TraversalParent) {
                    val children: List<Traversal.Admin<Object, Object>> = ArrayList()
                    children.addAll((step as TraversalParent).getGlobalChildren())
                    children.addAll((step as TraversalParent).getLocalChildren())
                    applyToChildren(keepLabels, children)
                }
                step = step.getPreviousStep()
            }

            // propagate keep labels forwards if future steps require a particular nested label
            while (!step.equals(EmptyStep.instance())) {
                if (step is PathProcessor) {
                    val referencedLabels: Set<String> = PathUtil.getReferencedLabelsAfterStep(step)
                    for (ref in referencedLabels) {
                        if (levelLabels.contains(ref)) {
                            if ((step as PathProcessor).getKeepLabels() == null) {
                                val newKeepLabels: HashSet<String> = HashSet()
                                newKeepLabels.add(ref)
                                (step as PathProcessor).setKeepLabels(newKeepLabels)
                            } else {
                                (step as PathProcessor).getKeepLabels().addAll(Collections.singleton(ref))
                            }
                        }
                    }
                }
                step = step.getNextStep()
            }
            keeperTrail.addAll(levelLabels)
        }
        for (currentStep in traversal.getSteps()) {
            // go back through current level and add all keepers
            // if there is one more RepeatSteps in this traversal's lineage, preserve keep labels
            if (currentStep is PathProcessor) {
                (currentStep as PathProcessor).getKeepLabels().addAll(keeperTrail)
                if (hasRepeat) (currentStep as PathProcessor).getKeepLabels().addAll(keepLabels)
            }
        }
    }

    private fun applyToChildren(keepLabels: Set<String>, children: List<Traversal.Admin<Object, Object>>) {
        for (child in children) {
            TraversalHelper.applyTraversalRecursively({ trav -> addLabels(trav, keepLabels) }, child)
        }
    }

    private fun addLabels(traversal: Traversal.Admin, keepLabels: Set<String>) {
        for (s in traversal.getSteps()) {
            if (s is PathProcessor) addLabels(s as PathProcessor, keepLabels)
        }
    }

    private fun addLabels(s: PathProcessor, keepLabels: Set<String>) {
        val labelsCopy: Set<String> = HashSet(keepLabels)
        if (null == s.getKeepLabels()) s.setKeepLabels(labelsCopy) else s.getKeepLabels().addAll(labelsCopy)
    }

    @Override
    fun applyPrior(): Set<Class<out OptimizationStrategy?>> {
        return PRIORS
    }

    companion object {
        var MAX_BARRIER_SIZE: Integer = 2500
        private val INSTANCE = PathRetractionStrategy(MAX_BARRIER_SIZE)

        // these strategies do strong rewrites involving path labeling and thus, should run prior to PathRetractionStrategy
        private val PRIORS: Set<Class<out OptimizationStrategy?>> = HashSet(
            Arrays.asList(
                RepeatUnrollStrategy::class.java,
                MatchPredicateStrategy::class.java,
                PathProcessorStrategy::class.java
            )
        )
        private val MARKER: String = Graph.Hidden.hide("gremlin.pathRetraction")
        fun instance(): PathRetractionStrategy {
            return INSTANCE
        }

        /**
         * Determines if the strategy should be applied or not. It returns `true` and is "not applicable" when the
         * following conditions are met:
         *
         *  * If there are lambdas as you can't introspect to know what path information the lambdas are using
         *  * If a PATH requirement step is being used (in the future, we can do PATH requirement lookhead to be more intelligent about its usage)
         *  * If a VertexProgramStep is present with LABELED_PATH requirements
         *
         */
        private fun isNotApplicable(traversal: Traversal.Admin<*, *>): Boolean {
            return TraversalHelper.anyStepRecursively({ step ->
                step is LambdaHolder ||
                        step.getRequirements().contains(TraverserRequirement.PATH) ||
                        step is VertexProgramStep && step.getRequirements().contains(TraverserRequirement.LABELED_PATH)
            }, traversal)
        }
    }
}