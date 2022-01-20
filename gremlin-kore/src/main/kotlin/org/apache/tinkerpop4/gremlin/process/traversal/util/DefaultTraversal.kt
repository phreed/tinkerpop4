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

import org.apache.tinkerpop4.gremlin.process.computer.traversal.step.map.VertexProgramStep

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class DefaultTraversal<S, E> private constructor(
    graph: Graph,
    traversalStrategies: TraversalStrategies,
    bytecode: Bytecode
) : Traversal.Admin<S, E> {
    private var lastTraverser: Traverser.Admin<E> = EmptyTraverser.instance()
    private var finalEndStep: Step<*, E> = EmptyStep.instance()
    private val stepPosition: StepPosition = StepPosition()

    @kotlin.jvm.Transient
    protected var graph: Graph?

    @kotlin.jvm.Transient
    protected var g: TraversalSource?
    protected var steps: List<Step> = ArrayList()

    // steps will be repeatedly retrieved from this traversal so wrap them once in an immutable list that can be reused
    protected var unmodifiableSteps: List<Step> = Collections.unmodifiableList(steps)
    protected var parent: TraversalParent = EmptyStep.instance()
    protected var sideEffects: TraversalSideEffects = DefaultTraversalSideEffects()
    protected var strategies: TraversalStrategies

    @kotlin.jvm.Transient
    protected var generator: TraverserGenerator? = null
    protected var requirements: Set<TraverserRequirement>? = null

    @get:Override
    var isLocked = false
        protected set

    /**
     * Determines if the traversal has been fully iterated and resources released.
     */
    var isClosed = false
        protected set
    protected var bytecode: Bytecode

    init {
        this.graph = graph
        strategies = traversalStrategies
        this.bytecode = bytecode
        g = null
    }

    constructor(graph: Graph) : this(
        graph,
        TraversalStrategies.GlobalCache.getStrategies(graph.getClass()),
        Bytecode()
    ) {
    }

    constructor(traversalSource: TraversalSource) : this(
        traversalSource.getGraph(),
        traversalSource.getStrategies(),
        traversalSource.getBytecode()
    ) {
        g = traversalSource
    }

    constructor(
        traversalSource: TraversalSource,
        traversal: DefaultTraversal.Admin<S, E>
    ) : this(traversalSource.getGraph(), traversalSource.getStrategies(), traversal.getBytecode()) {
        g = traversalSource
        steps.addAll(traversal.getSteps())
    }

    constructor() : this(
        EmptyGraph.instance(),
        TraversalStrategies.GlobalCache.getStrategies(EmptyGraph::class.java),
        Bytecode()
    ) {
    }

    constructor(bytecode: Bytecode) : this(
        EmptyGraph.instance(), TraversalStrategies.GlobalCache.getStrategies(
            EmptyGraph::class.java
        ), bytecode
    ) {
    }

    fun getBytecode(): Bytecode {
        return bytecode
    }

    @Override
    fun asAdmin(): Traversal.Admin<S, E> {
        return this
    }

    @get:Override
    val traverserGenerator: TraverserGenerator?
        get() {
            if (null == generator) generator =
                if (isRoot()) DefaultTraverserGeneratorFactory.instance().getTraverserGenerator(
                    traverserRequirements
                ) else TraversalHelper.getRootTraversal(this).getTraverserGenerator()
            return generator
        }

    @Override
    @Throws(IllegalStateException::class)
    fun applyStrategies() {
        if (isLocked) throw Traversal.Exceptions.traversalIsLocked()
        TraversalHelper.reIdSteps(stepPosition, this)
        val hasGraph = null != graph

        // we only want to apply strategies on the top-level step or if we got some graphcomputer stuff going on.
        // seems like in that case, the "top-level" of the traversal is really held by the VertexProgramStep which
        // needs to have strategies applied on "pure" copies of the traversal it is holding (i think). it further
        // seems that we need three recursions over the traversal hierarchy to ensure everything "works", where
        // strategy application requires top-level strategies and side-effects pushed into each child and then after
        // application of the strategies we need to call applyStrategies() on all the children to ensure that their
        // steps get reId'd and traverser requirements are set.
        if (isRoot() || getParent() is VertexProgramStep) {

            // prepare the traversal and all its children for strategy application
            TraversalHelper.applyTraversalRecursively({ t ->
                if (hasGraph) t.setGraph(graph)
                t.setStrategies(strategies)
                t.setSideEffects(sideEffects)
            }, this)

            // note that prior to applying strategies to children we used to set side-effects and strategies of all
            // children to that of the parent. under this revised model of strategy application from TINKERPOP-1568
            // it doesn't appear to be necessary to do that (at least from the perspective of the test suite). by,
            // moving side-effect setting after actual recursive strategy application we save a loop and by
            // consequence also fix a problem where strategies might reset something in sideeffects which seems to
            // happen in TranslationStrategy.
            val strategyIterator: Iterator<TraversalStrategy<*>> = strategies.iterator()
            while (strategyIterator.hasNext()) {
                val strategy: TraversalStrategy<*> = strategyIterator.next()
                TraversalHelper.applyTraversalRecursively(strategy::apply, this)
            }

            // don't need to re-apply strategies to "this" - leads to endless recursion in GraphComputer.
            TraversalHelper.applyTraversalRecursively({ t ->
                if (hasGraph) t.setGraph(graph)
                if (!t.isRoot() && t !== this && !t.isLocked()) {
                    t.setSideEffects(sideEffects)
                    t.applyStrategies()
                }
            }, this)
        }
        finalEndStep = endStep

        // finalize requirements
        if (this.isRoot()) {
            resetTraverserRequirements()
        }
        isLocked = true
    }

    private fun resetTraverserRequirements() {
        requirements = null
        traverserRequirements
    }

    @get:Override
    val traverserRequirements: Set<Any>?
        get() {
            if (null == requirements) {
                requirements = EnumSet.noneOf(TraverserRequirement::class.java)
                for (step in getSteps()) {
                    requirements.addAll(step.getRequirements())
                }
                if (!requirements!!.contains(TraverserRequirement.LABELED_PATH) && TraversalHelper.hasLabels(this)) requirements.add(
                    TraverserRequirement.LABELED_PATH
                )
                if (!getSideEffects().keys().isEmpty()) requirements.add(TraverserRequirement.SIDE_EFFECTS)
                if (null != getSideEffects().getSackInitialValue()) requirements.add(TraverserRequirement.SACK)
                if (requirements!!.contains(TraverserRequirement.ONE_BULK)) requirements.remove(TraverserRequirement.BULK)
                requirements = Collections.unmodifiableSet(requirements)
            }
            return requirements
        }

    @Override
    fun getSteps(): List<Step> {
        return unmodifiableSteps
    }

    @Override
    fun nextTraverser(): Traverser.Admin<E> {
        return try {
            if (!isLocked) applyStrategies()
            if (lastTraverser.bulk() > 0L) {
                val temp: Traverser.Admin<E> = lastTraverser
                lastTraverser = EmptyTraverser.instance()
                temp
            } else {
                finalEndStep.next()
            }
        } catch (e: FastNoSuchElementException) {
            throw if (this.isRoot()) NoSuchElementException() else e
        }
    }

    @Override
    operator fun hasNext(): Boolean {
        // if the traversal is closed then resources are released and there is nothing else to iterate
        if (isClosed) return false
        if (!isLocked) applyStrategies()
        val more = lastTraverser.bulk() > 0L || finalEndStep.hasNext()
        if (!more) CloseableIterator.closeIterator(this)
        return more
    }

    @Override
    operator fun next(): E {
        // if the traversal is closed then resources are released and there is nothing else to iterate
        if (isClosed) throw if (parent is EmptyStep) NoSuchElementException() else FastNoSuchElementException.instance()
        return try {
            if (!isLocked) applyStrategies()
            if (lastTraverser.bulk() === 0L) lastTraverser = finalEndStep.next()
            lastTraverser.setBulk(lastTraverser.bulk() - 1L)
            lastTraverser.get()
        } catch (e: FastNoSuchElementException) {
            // No more elements will be produced by this traversal. Close this traversal
            // and release the resources.
            CloseableIterator.closeIterator(this)
            throw if (this.isRoot()) NoSuchElementException() else e
        }
    }

    @Override
    fun reset() {
        steps.forEach(Step::reset)
        finalEndStep.reset()
        lastTraverser = EmptyTraverser.instance()
        isClosed = false
    }

    @Override
    fun addStart(start: Traverser.Admin<S>?) {
        if (!isLocked) applyStrategies()
        if (!steps.isEmpty()) {
            steps[0].addStart(start)

            // match() expects that a traversal that has been iterated can continue to iterate if new starts are
            // added therefore the closed state must be reset.
            isClosed = false
        }
    }

    @Override
    fun addStarts(starts: Iterator<Traverser.Admin<S>?>?) {
        if (!isLocked) applyStrategies()
        if (!steps.isEmpty()) {
            steps[0].addStarts(starts)

            // match() expects that a traversal that has been iterated can continue to iterate if new starts are
            // added therefore the closed state must be reset.
            isClosed = false
        }
    }

    @Override
    override fun toString(): String {
        return StringFactory.traversalString(this)
    }

    @get:Override
    val startStep: Step<S, *>
        get() = if (steps.isEmpty()) EmptyStep.instance() else steps[0]

    @get:Override
    val endStep: Step<*, E>
        get() = if (steps.isEmpty()) EmptyStep.instance() else steps[steps.size() - 1]

    @Override
    fun clone(): DefaultTraversal<S, E> {
        return try {
            val clone = super.clone() as DefaultTraversal<S, E>
            clone.lastTraverser = EmptyTraverser.instance()
            clone.steps = ArrayList()
            clone.unmodifiableSteps = Collections.unmodifiableList(clone.steps)
            clone.sideEffects = sideEffects.clone()
            clone.strategies = strategies
            clone.bytecode = bytecode.clone()
            for (step in steps) {
                val clonedStep: Step<*, *> = step.clone()
                clonedStep.setTraversal(clone)
                val previousStep: Step =
                    if (clone.steps.isEmpty()) EmptyStep.instance() else clone.steps[clone.steps.size() - 1]
                clonedStep.setPreviousStep(previousStep)
                previousStep.setNextStep(clonedStep)
                clone.steps.add(clonedStep)
            }
            clone.finalEndStep = clone.endStep
            clone
        } catch (e: CloneNotSupportedException) {
            throw IllegalStateException(e.getMessage(), e)
        }
    }

    @Override
    fun notifyClose() {
        isClosed = true
    }

    @Override
    fun setSideEffects(sideEffects: TraversalSideEffects) {
        this.sideEffects = sideEffects
    }

    @Override
    fun getSideEffects(): TraversalSideEffects {
        return sideEffects
    }

    @Override
    fun setStrategies(strategies: TraversalStrategies) {
        this.strategies = strategies
    }

    @Override
    fun getStrategies(): TraversalStrategies {
        return strategies
    }

    @Override
    @Throws(IllegalStateException::class)
    fun <S2, E2> addStep(index: Int, step: Step<*, *>): Traversal.Admin<S2, E2> {
        if (isLocked) throw Exceptions.traversalIsLocked()
        step.setId(stepPosition.nextXId())
        steps.add(index, step)
        val previousStep: Step? = if (steps.size() > 0 && index != 0) steps[index - 1] else null
        val nextStep: Step? = if (steps.size() > index + 1) steps[index + 1] else null
        step.setPreviousStep(if (null != previousStep) previousStep else EmptyStep.instance())
        step.setNextStep(if (null != nextStep) nextStep else EmptyStep.instance())
        if (null != previousStep) previousStep.setNextStep(step)
        if (null != nextStep) nextStep.setPreviousStep(step)
        step.setTraversal(this)
        return this as Traversal.Admin<S2, E2>
    }

    @Override
    @Throws(IllegalStateException::class)
    fun <S2, E2> removeStep(index: Int): Traversal.Admin<S2, E2> {
        if (isLocked) throw Exceptions.traversalIsLocked()
        val previousStep: Step? = if (steps.size() > 0 && index != 0) steps[index - 1] else null
        val nextStep: Step? = if (steps.size() > index + 1) steps[index + 1] else null
        //this.steps.get(index).setTraversal(EmptyTraversal.instance());
        steps.remove(index)
        if (null != previousStep) previousStep.setNextStep(if (null == nextStep) EmptyStep.instance() else nextStep)
        if (null != nextStep) nextStep.setPreviousStep(if (null == previousStep) EmptyStep.instance() else previousStep)
        return this as Traversal.Admin<S2, E2>
    }

    @Override
    fun setParent(step: TraversalParent?) {
        parent = if (null == step) EmptyStep.instance() else step
    }

    @Override
    fun getParent(): TraversalParent {
        return parent
    }

    @Override
    fun getGraph(): Optional<Graph> {
        return Optional.ofNullable(graph)
    }

    @get:Override
    val traversalSource: Optional<TraversalSource>
        get() = Optional.ofNullable(g)

    @Override
    fun setGraph(graph: Graph?) {
        this.graph = graph
    }

    @Override
    override fun equals(other: Object?): Boolean {
        return other != null && other.getClass().equals(this.getClass()) && equals(other as Traversal.Admin?)
    }

    @Override
    override fun hashCode(): Int {
        var index = 0
        var result: Int = this.getClass().hashCode()
        for (step in asAdmin().getSteps()) {
            result = result xor Integer.rotateLeft(step.hashCode(), index++)
        }
        return result
    }
}