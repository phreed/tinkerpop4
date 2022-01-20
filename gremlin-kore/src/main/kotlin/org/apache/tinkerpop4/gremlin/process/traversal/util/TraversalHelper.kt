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
package org.apache.tinkerpop4.gremlin.process.traversal.util

import org.apache.tinkerpop4.gremlin.process.computer.traversal.step.map.TraversalVertexProgramStep

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
object TraversalHelper {
    fun isLocalProperties(traversal: Traversal.Admin<*, *>): Boolean {
        for (step in traversal.getSteps()) {
            if (step is RepeatStep) {
                for (global in (step as RepeatStep<*>).getGlobalChildren()) {
                    if (hasStepOfAssignableClass(
                            VertexStep::class.java, global
                        )
                    ) return false
                }
            } else if (step is VertexStep) {
                return false
            } else if (step is EdgeVertexStep) {
                return false
            } else if (step is TraversalParent) {
                for (local in (step as TraversalParent).getLocalChildren()) {
                    if (!isLocalProperties(local)) return false
                }
            }
        }
        return true
    }

    fun isLocalStarGraph(traversal: Traversal.Admin<*, *>): Boolean {
        return 'x' != isLocalStarGraph(traversal, 'v')
    }

    private fun isLocalStarGraph(traversal: Traversal.Admin<*, *>, state: Char): Char {
        var state = state
        if (state == 'u' &&
            (traversal is ValueTraversal ||
                    traversal is TokenTraversal && !(traversal as TokenTraversal).getToken().equals(T.id))
        ) return 'x'
        for (step in traversal.getSteps()) {
            if ((step is PropertiesStep || step is LabelStep || step is PropertyMapStep) && state == 'u') return 'x' else if (step is VertexStep) {
                if (state == 'u') return 'x'
                state = if ((step as VertexStep).returnsVertex()) 'u' else 'e'
            } else if (step is EdgeVertexStep) {
                state = 'u'
            } else if (step is HasContainerHolder && state == 'u') {
                for (hasContainer in (step as HasContainerHolder).getHasContainers()) {
                    if (!hasContainer.getKey().equals(T.id.getAccessor())) return 'x'
                }
            } else if (step is TraversalParent) {
                val currState = state
                val states: Set<Character> = HashSet()
                for (local in (step as TraversalParent).getLocalChildren()) {
                    val s = isLocalStarGraph(local, currState)
                    if ('x' == s) return 'x'
                    states.add(s)
                }
                if (step !is ByModulating) {
                    if (states.contains('u')) state = 'u' else if (states.contains('e')) state = 'e'
                }
                states.clear()
                if (step is SelectStep || step is SelectOneStep) {
                    states.add('u')
                }
                for (local in (step as TraversalParent).getGlobalChildren()) {
                    val s = isLocalStarGraph(local, currState)
                    if ('x' == s) return 'x'
                    states.add(s)
                }
                if (states.contains('u')) state = 'u' else if (states.contains('e')) state = 'e'
                if (state != currState && (step is RepeatStep || step is MatchStep)) return 'x'
            }
        }
        return state
    }

    /**
     * Insert a step before a specified step instance.
     *
     * @param insertStep the step to insert
     * @param afterStep  the step to insert the new step before
     * @param traversal  the traversal on which the action should occur
     */
    fun <S, E> insertBeforeStep(insertStep: Step<S, E>?, afterStep: Step<E, *>?, traversal: Traversal.Admin<*, *>) {
        traversal.addStep(stepIndex<Any, Any>(afterStep, traversal), insertStep)
    }

    /**
     * Insert a step after a specified step instance.
     *
     * @param insertStep the step to insert
     * @param beforeStep the step to insert the new step after
     * @param traversal  the traversal on which the action should occur
     */
    fun <S, E> insertAfterStep(insertStep: Step<S, E>?, beforeStep: Step<*, S>?, traversal: Traversal.Admin<*, *>) {
        traversal.addStep(stepIndex<Any, Any>(beforeStep, traversal) + 1, insertStep)
    }

    /**
     * Replace a step with a new step.
     *
     * @param removeStep the step to remove
     * @param insertStep the step to insert
     * @param traversal  the traversal on which the action will occur
     */
    fun <S, E> replaceStep(removeStep: Step<S, E>?, insertStep: Step<S, E>?, traversal: Traversal.Admin<*, *>) {
        var i: Int
        traversal.removeStep(stepIndex<Any, Any>(removeStep, traversal).also { i = it })
        traversal.addStep(i, insertStep)
    }

    fun <S, E> insertTraversal(
        previousStep: Step<*, S>?,
        insertTraversal: Traversal.Admin<S, E>?,
        traversal: Traversal.Admin<*, *>
    ): Step<*, E> {
        return insertTraversal<Any, Any>(stepIndex<Any, Any>(previousStep, traversal), insertTraversal, traversal)
    }

    fun <S, E> insertTraversal(
        insertIndex: Int,
        insertTraversal: Traversal.Admin<S, E>,
        traversal: Traversal.Admin<*, *>
    ): Step<*, E> {
        return if (0 == traversal.getSteps().size()) {
            var currentStep: Step? = EmptyStep.instance()
            for (insertStep in insertTraversal.getSteps()) {
                currentStep = insertStep
                traversal.addStep(insertStep)
            }
            currentStep
        } else {
            var currentStep: Step = traversal.getSteps().get(insertIndex)
            for (insertStep in insertTraversal.getSteps()) {
                insertAfterStep(insertStep, currentStep, traversal)
                currentStep = insertStep
            }
            currentStep
        }
    }

    fun <S, E> removeToTraversal(startStep: Step<S, *>, endStep: Step<*, E>, newTraversal: Traversal.Admin<S, E>) {
        val originalTraversal: Traversal.Admin<*, *> = startStep.getTraversal()
        var currentStep: Step<*, *> = startStep
        while (currentStep !== endStep && currentStep !is EmptyStep) {
            val temp: Step<*, *> = currentStep.getNextStep()
            originalTraversal.removeStep(currentStep)
            newTraversal.addStep(currentStep)
            currentStep = temp
        }
    }

    /**
     * Gets the index of a particular step in the [Traversal].
     *
     * @param step      the step to retrieve the index for
     * @param traversal the traversal to perform the action on
     * @return the index of the step or -1 if the step is not present
     */
    fun <S, E> stepIndex(step: Step<S, E>?, traversal: Traversal.Admin<*, *>): Int {
        var i = 0
        for (s in traversal.getSteps()) {
            if (s.equals(step, true)) return i
            i++
        }
        return -1
    }

    fun <S> getStepsOfClass(stepClass: Class<S>?, traversal: Traversal.Admin<*, *>): List<S> {
        val steps: List<S> = ArrayList()
        for (step in traversal.getSteps()) {
            if (step.getClass().equals(stepClass)) steps.add(step as S)
        }
        return steps
    }

    fun <S> getStepsOfAssignableClass(stepClass: Class<S>, traversal: Traversal.Admin<*, *>): List<S> {
        val steps: List<S> = ArrayList()
        for (step in traversal.getSteps()) {
            if (stepClass.isAssignableFrom(step.getClass())) steps.add(step as S)
        }
        return steps
    }

    fun <S> getLastStepOfAssignableClass(stepClass: Class<S>, traversal: Traversal.Admin<*, *>): Optional<S> {
        val steps: List<S> = getStepsOfAssignableClass<Any>(stepClass, traversal)
        return if (steps.size() === 0) Optional.empty() else Optional.of(steps[steps.size() - 1])
    }

    fun <S> getFirstStepOfAssignableClass(stepClass: Class<S>, traversal: Traversal.Admin<*, *>): Optional<S> {
        for (step in traversal.getSteps()) {
            if (stepClass.isAssignableFrom(step.getClass())) return Optional.of(step as S)
        }
        return Optional.empty()
    }

    fun <S> getStepsOfAssignableClassRecursively(stepClass: Class<S>, traversal: Traversal.Admin<*, *>): List<S> {
        return getStepsOfAssignableClassRecursively<Any>(null, stepClass, traversal)
    }

    fun <S> getStepsOfAssignableClassRecursively(
        scope: Scope?,
        stepClass: Class<S>,
        traversal: Traversal.Admin<*, *>
    ): List<S> {
        val list: List<S> = ArrayList()
        for (step in traversal.getSteps()) {
            if (stepClass.isAssignableFrom(step.getClass())) list.add(step as S)
            if (step is TraversalParent) {
                if (null == scope || Scope.local.equals(scope)) {
                    for (localChild in (step as TraversalParent).getLocalChildren()) {
                        list.addAll(getStepsOfAssignableClassRecursively<Any>(stepClass, localChild))
                    }
                }
                if (null == scope || Scope.global.equals(scope)) {
                    for (globalChild in (step as TraversalParent).getGlobalChildren()) {
                        list.addAll(getStepsOfAssignableClassRecursively<Any>(stepClass, globalChild))
                    }
                }
            }
        }
        return list
    }

    fun isGlobalChild(traversal: Traversal.Admin<*, *>): Boolean {
        var traversal: Traversal.Admin<*, *> = traversal
        while (!traversal.isRoot()) {
            if (traversal.getParent().getLocalChildren().contains(traversal)) return false
            traversal = traversal.getParent().asStep().getTraversal()
        }
        return true
    }

    /**
     * Determine if the traversal has a step of a particular class.
     *
     * @param stepClass the step class to look for
     * @param traversal the traversal to perform the action on
     * @return `true` if the class is found and `false` otherwise
     */
    fun hasStepOfClass(stepClass: Class?, traversal: Traversal.Admin<*, *>): Boolean {
        for (step in traversal.getSteps()) {
            if (step.getClass().equals(stepClass)) {
                return true
            }
        }
        return false
    }

    /**
     * Determine if the traversal has a step of an assignable class.
     *
     * @param superClass the step super class to look for
     * @param traversal  the traversal to perform the action on
     * @return `true` if the class is found and `false` otherwise
     */
    fun hasStepOfAssignableClass(superClass: Class, traversal: Traversal.Admin<*, *>): Boolean {
        for (step in traversal.getSteps()) {
            if (superClass.isAssignableFrom(step.getClass())) {
                return true
            }
        }
        return false
    }

    /**
     * Determine if the traversal has a step of an assignable class in the current [Traversal] and its
     * local and global child traversals.
     *
     * @param stepClass the step class to look for
     * @param traversal the traversal in which to look for the given step class
     * @return `true` if any step in the given traversal (and its child traversals) is an instance of the
     * given `stepClass`, otherwise `false`.
     */
    fun hasStepOfAssignableClassRecursively(stepClass: Class?, traversal: Traversal.Admin<*, *>?): Boolean {
        return hasStepOfAssignableClassRecursively(null, stepClass, traversal)
    }

    /**
     * Determine if the traversal has a step of an assignable class in the current [Traversal] and its
     * [Scope] child traversals.
     *
     * @param scope     the child traversal scope to check
     * @param stepClass the step class to look for
     * @param traversal the traversal in which to look for the given step class
     * @return `true` if any step in the given traversal (and its child traversals) is an instance of the
     * given `stepClass`, otherwise `false`.
     */
    fun hasStepOfAssignableClassRecursively(
        scope: Scope?,
        stepClass: Class,
        traversal: Traversal.Admin<*, *>
    ): Boolean {
        for (step in traversal.getSteps()) {
            if (stepClass.isAssignableFrom(step.getClass())) {
                return true
            }
            if (step is TraversalParent) {
                if (null == scope || Scope.local.equals(scope)) {
                    for (localChild in (step as TraversalParent).getLocalChildren()) {
                        if (hasStepOfAssignableClassRecursively(stepClass, localChild)) return true
                    }
                }
                if (null == scope || Scope.global.equals(scope)) {
                    for (globalChild in (step as TraversalParent).getGlobalChildren()) {
                        if (hasStepOfAssignableClassRecursively(stepClass, globalChild)) return true
                    }
                }
            }
        }
        return false
    }

    /**
     * Determine if the traversal has any of the supplied steps of an assignable class in the current [Traversal]
     * and its global or local child traversals.
     *
     * @param stepClasses the step classes to look for
     * @param traversal   the traversal in which to look for the given step classes
     * @return `true` if any step in the given traversal (and its child traversals) is an instance of a class
     * provided in `stepClasses`, otherwise `false`.
     */
    fun hasStepOfAssignableClassRecursively(
        stepClasses: Collection<Class?>?,
        traversal: Traversal.Admin<*, *>?
    ): Boolean {
        return hasStepOfAssignableClassRecursively(null, stepClasses, traversal)
    }

    /**
     * Determine if the traversal has any of the supplied steps of an assignable class in the current [Traversal]
     * and its [Scope] child traversals.
     *
     * @param scope       whether to check global or local children (null for both).
     * @param stepClasses the step classes to look for
     * @param traversal   the traversal in which to look for the given step classes
     * @return `true` if any step in the given traversal (and its child traversals) is an instance of a class
     * provided in `stepClasses`, otherwise `false`.
     */
    fun hasStepOfAssignableClassRecursively(
        scope: Scope?,
        stepClasses: Collection<Class?>,
        traversal: Traversal.Admin<*, *>
    ): Boolean {
        if (stepClasses.size() === 1) return hasStepOfAssignableClassRecursively(
            stepClasses.iterator().next(),
            traversal
        )
        for (step in traversal.getSteps()) {
            if (IteratorUtils.anyMatch(stepClasses.iterator()) { stepClass -> stepClass.isAssignableFrom(step.getClass()) }) {
                return true
            }
            if (step is TraversalParent) {
                if (null == scope || Scope.local.equals(scope)) {
                    for (localChild in (step as TraversalParent).getLocalChildren()) {
                        if (hasStepOfAssignableClassRecursively(stepClasses, localChild)) return true
                    }
                }
                if (null == scope || Scope.global.equals(scope)) {
                    for (globalChild in (step as TraversalParent).getGlobalChildren()) {
                        if (hasStepOfAssignableClassRecursively(stepClasses, globalChild)) return true
                    }
                }
            }
        }
        return false
    }

    /**
     * Determine if any step in [Traversal] or its children match the step given the provided [Predicate].
     *
     * @param predicate the match function
     * @param traversal the traversal to perform the action on
     * @return `true` if there is a match and `false` otherwise
     */
    fun anyStepRecursively(predicate: Predicate<Step?>, traversal: Traversal.Admin<*, *>): Boolean {
        for (step in traversal.getSteps()) {
            if (predicate.test(step)) {
                return true
            }
            if (step is TraversalParent && anyStepRecursively(predicate, step as TraversalParent)) {
                return true
            }
        }
        return false
    }

    /**
     * Determine if any child step of a [TraversalParent] match the step given the provided [Predicate].
     *
     * @param predicate the match function
     * @param step      the step to perform the action on
     * @return `true` if there is a match and `false` otherwise
     */
    fun anyStepRecursively(predicate: Predicate<Step?>?, step: TraversalParent): Boolean {
        for (localChild in step.getLocalChildren()) {
            if (anyStepRecursively(predicate, localChild)) return true
        }
        for (globalChild in step.getGlobalChildren()) {
            if (anyStepRecursively(predicate, globalChild)) return true
        }
        return false
    }

    /**
     * Apply the provider [Consumer] function to the provided [Traversal] and all of its children.
     *
     * @param consumer  the function to apply to the each traversal in the tree
     * @param traversal the root traversal to start application
     */
    fun applyTraversalRecursively(consumer: Consumer<Traversal.Admin<*, *>?>, traversal: Traversal.Admin<*, *>) {
        consumer.accept(traversal)

        // we get accused of concurrentmodification if we try a for(Iterable)
        val steps: List<Step> = traversal.getSteps()
        for (ix in 0 until steps.size()) {
            val step: Step = steps[ix]
            if (step is TraversalParent) {
                for (local in (step as TraversalParent).getLocalChildren()) {
                    applyTraversalRecursively(consumer, local)
                }
                for (global in (step as TraversalParent).getGlobalChildren()) {
                    applyTraversalRecursively(consumer, global)
                }
            }
        }
    }

    fun <S> addToCollection(collection: Collection<S>, s: S, bulk: Long) {
        if (collection is BulkSet) {
            (collection as BulkSet<S>).add(s, bulk)
        } else if (collection is Set) {
            collection.add(s)
        } else {
            for (i in 0 until bulk) {
                collection.add(s)
            }
        }
    }

    /**
     * Returns the name of *step* truncated to *maxLength*. An ellipses is appended when the name exceeds
     * *maxLength*.
     *
     * @param step
     * @param maxLength Includes the 3 "..." characters that will be appended when the length of the name exceeds
     * maxLength.
     * @return short step name.
     */
    fun getShortName(step: Step, maxLength: Int): String {
        val name: String = step.toString()
        return if (name.length() > maxLength) name.substring(0, maxLength - 3) + "..." else name
    }

    fun reIdSteps(stepPosition: StepPosition, traversal: Traversal.Admin<*, *>) {
        stepPosition.x = 0
        stepPosition.y = -1
        stepPosition.z = -1
        stepPosition.parentId = null
        var current: Traversal.Admin<*, *> = traversal
        while (current !is EmptyTraversal) {
            stepPosition.y++
            val parent: TraversalParent = current.getParent()
            if (null == stepPosition.parentId && parent !is EmptyStep) stepPosition.parentId = parent.asStep().getId()
            if (-1 == stepPosition.z) {
                val globalChildrenSize: Int = parent.getGlobalChildren().size()
                for (i in 0 until globalChildrenSize) {
                    if (parent.getGlobalChildren().get(i) === current) {
                        stepPosition.z = i
                    }
                }
                for (i in 0 until parent.getLocalChildren().size()) {
                    val currentLocalChild: Traversal = parent.getLocalChildren().get(i)
                    if (currentLocalChild === current ||
                        currentLocalChild is AbstractLambdaTraversal && (currentLocalChild as AbstractLambdaTraversal).getBypassTraversal() === current
                    ) {
                        stepPosition.z = i + globalChildrenSize
                    }
                }
            }
            current = parent.asStep().getTraversal()
        }
        if (-1 == stepPosition.z) stepPosition.z = 0
        if (null == stepPosition.parentId) stepPosition.parentId = ""
        for (step in traversal.getSteps()) {
            step.setId(stepPosition.nextXId())
        }
    }

    fun getRootTraversal(traversal: Traversal.Admin<*, *>): Traversal.Admin<*, *> {
        var traversal: Traversal.Admin<*, *> = traversal
        while (traversal.getParent() !is EmptyStep) {
            traversal = traversal.getParent().asStep().getTraversal()
        }
        return traversal
    }

    fun hasLabels(traversal: Traversal.Admin<*, *>): Boolean {
        for (step in traversal.getSteps()) {
            for (label in step.getLabels()) {
                if (!Graph.Hidden.isHidden(label)) return true
            }
            if (step is TraversalParent) {
                for (local in (step as TraversalParent).getLocalChildren()) {
                    if (hasLabels(local)) return true
                }
                for (global in (step as TraversalParent).getGlobalChildren()) {
                    if (hasLabels(global)) return true
                }
            }
        }
        return false
    }

    fun getLabels(traversal: Traversal.Admin<*, *>): Set<String> {
        return getLabels(HashSet(), traversal)
    }

    private fun getLabels(labels: Set<String>, traversal: Traversal.Admin<*, *>): Set<String> {
        for (step in traversal.getSteps()) {
            labels.addAll(step.getLabels())
            if (step is TraversalParent) {
                for (local in (step as TraversalParent).getLocalChildren()) {
                    getLabels(labels, local)
                }
                for (global in (step as TraversalParent).getGlobalChildren()) {
                    getLabels(labels, global)
                }
            }
        }
        return labels
    }

    fun getVariableLocations(traversal: Traversal.Admin<*, *>): Set<Scoping.Variable> {
        return getVariableLocations(
            EnumSet.noneOf(
                Scoping.Variable::class.java
            ), traversal
        )
    }

    private fun getVariableLocations(
        variables: Set<Scoping.Variable>,
        traversal: Traversal.Admin<*, *>
    ): Set<Scoping.Variable> {
        if (variables.size() === 2) return variables // has both START and END so no need to compute further
        val startStep: Step<*, *> = traversal.getStartStep()
        if (StartStep.isVariableStartStep(startStep)) variables.add(Scoping.Variable.START) else if (startStep is WherePredicateStep) {
            if ((startStep as WherePredicateStep).getStartKey().isPresent()) variables.add(Scoping.Variable.START)
        } else if (startStep is WhereStartStep) {
            if (!(startStep as WhereStartStep).getScopeKeys().isEmpty()) variables.add(Scoping.Variable.START)
        } else if (startStep is MatchStartStep) {
            if ((startStep as MatchStartStep).getSelectKey().isPresent()) variables.add(Scoping.Variable.START)
        } else if (startStep is MatchStep) {
            for (global in (startStep as MatchStep<*, *>).getGlobalChildren()) {
                getVariableLocations(variables, global)
            }
        } else if (startStep is ConnectiveStep || startStep is NotStep || startStep is WhereTraversalStep) {
            for (local in (startStep as TraversalParent).getLocalChildren()) {
                getVariableLocations(variables, local)
            }
        }
        ///
        val endStep: Step<*, *> = traversal.getEndStep()
        if (endStep is WherePredicateStep) {
            if ((endStep as WherePredicateStep).getStartKey().isPresent()) variables.add(Scoping.Variable.END)
        } else if (endStep is WhereEndStep) {
            if (!(endStep as WhereEndStep).getScopeKeys().isEmpty()) variables.add(Scoping.Variable.END)
        } else if (endStep is MatchEndStep) {
            if ((endStep as MatchEndStep).getMatchKey().isPresent()) variables.add(Scoping.Variable.END)
        } else if (!endStep.getLabels().isEmpty()) variables.add(Scoping.Variable.END)
        ///
        return variables
    }

    fun onGraphComputer(traversal: Traversal.Admin<*, *>): Boolean {
        var traversal: Traversal.Admin<*, *> = traversal
        while (!traversal.isRoot()) {
            if (traversal.getParent() is TraversalVertexProgramStep) return true
            traversal = traversal.getParent().asStep().getTraversal()
        }
        return false
    }

    fun removeAllSteps(traversal: Traversal.Admin<*, *>) {
        val size: Int = traversal.getSteps().size()
        for (i in 0 until size) {
            traversal.removeStep(0)
        }
    }

    fun copyLabels(fromStep: Step<*, *>, toStep: Step<*, *>, moveLabels: Boolean) {
        if (!fromStep.getLabels().isEmpty()) {
            for (label in if (moveLabels) LinkedHashSet(fromStep.getLabels()) else fromStep.getLabels()) {
                toStep.addLabel(label)
                if (moveLabels) fromStep.removeLabel(label)
            }
        }
    }

    fun hasAllStepsOfClass(traversal: Traversal.Admin<*, *>, vararg classesToCheck: Class<*>?): Boolean {
        for (step in traversal.getSteps()) {
            var foundInstance = false
            for (classToCheck in classesToCheck) {
                if (classToCheck.isInstance(step)) {
                    foundInstance = true
                    break
                }
            }
            if (!foundInstance) return false
        }
        return true
    }

    fun hasStepOfClass(traversal: Traversal.Admin<*, *>, vararg classesToCheck: Class<*>?): Boolean {
        for (step in traversal.getSteps()) {
            for (classToCheck in classesToCheck) {
                if (classToCheck.isInstance(step)) return true
            }
        }
        return false
    }

    @Deprecated
    @Deprecated("As of release 3.5.2, not replaced as strategies are not applied in this fashion after 3.5.0")
    fun applySingleLevelStrategies(
        parentTraversal: Traversal.Admin<*, *>,
        childTraversal: Traversal.Admin<*, *>,
        stopAfterStrategy: Class<out TraversalStrategy?>?
    ) {
        childTraversal.setStrategies(parentTraversal.getStrategies())
        childTraversal.setSideEffects(parentTraversal.getSideEffects())
        parentTraversal.getGraph().ifPresent(childTraversal::setGraph)
        for (strategy in parentTraversal.getStrategies()) {
            strategy.apply(childTraversal)
            if (null != stopAfterStrategy && stopAfterStrategy.isInstance(strategy)) break
        }
    }

    /**
     * Used to left-fold a [HasContainer] to a [HasContainerHolder] if it exists. Else, append a [HasStep].
     *
     * @param traversal    the traversal to fold or append.
     * @param hasContainer the container to add left or append.
     * @param <T>          the traversal type
     * @return the has container folded or appended traversal
    </T> */
    fun <T : Traversal.Admin<*, *>?> addHasContainer(traversal: T, hasContainer: HasContainer?): T {
        return if (traversal.getEndStep() is HasContainerHolder) {
            (traversal.getEndStep() as HasContainerHolder).addHasContainer(hasContainer)
            traversal
        } else traversal.addStep(HasStep(traversal, hasContainer)) as T
    }
}