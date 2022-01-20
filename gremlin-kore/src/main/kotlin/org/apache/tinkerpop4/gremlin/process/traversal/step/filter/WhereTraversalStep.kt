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
package org.apache.tinkerpop4.gremlin.process.traversal.step.filter

import org.apache.tinkerpop4.gremlin.process.traversal.Pop

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class WhereTraversalStep<S>(traversal: Traversal.Admin?, whereTraversal: Traversal<*, *>) : FilterStep<S>(traversal),
    TraversalParent, Scoping, PathProcessor {
    protected var whereTraversal: Traversal.Admin<*, *>
    protected val scopeKeys: Set<String> = HashSet()
    protected var keepLabels: Set<String>? = null

    init {
        this.whereTraversal = whereTraversal.asAdmin()
        configureStartAndEndSteps(this.whereTraversal)
        if (scopeKeys.isEmpty()) throw IllegalArgumentException("A where()-traversal must have at least a start or end label (i.e. variable): $whereTraversal")
        this.whereTraversal = this.integrateChild(this.whereTraversal)
    }

    private fun configureStartAndEndSteps(whereTraversal: Traversal.Admin<*, *>) {
        ConnectiveStrategy.instance().apply(whereTraversal)
        //// START STEP to WhereStartStep
        val startStep: Step<*, *> = whereTraversal.getStartStep()
        if (startStep is ConnectiveStep || startStep is NotStep) {       // for conjunction- and not-steps
            (startStep as TraversalParent).getLocalChildren()
                .forEach { whereTraversal: Traversal.Admin<*, *> -> configureStartAndEndSteps(whereTraversal) }
        } else if (StartStep.isVariableStartStep(startStep)) {  // as("a").out()... traversals
            val label: String = startStep.getLabels().iterator().next()
            scopeKeys.add(label)
            TraversalHelper.replaceStep(startStep, WhereStartStep<Any?>(whereTraversal, label), whereTraversal)
        } else if (!whereTraversal.getEndStep().getLabels()
                .isEmpty()
        ) {                    // ...out().as("a") traversals
            TraversalHelper.insertBeforeStep(
                WhereStartStep<Any?>(whereTraversal, null),
                startStep as Step,
                whereTraversal
            )
        }
        //// END STEP to WhereEndStep
        val endStep: Step<*, *> = whereTraversal.getEndStep()
        if (!endStep.getLabels().isEmpty()) {
            if (endStep.getLabels()
                    .size() > 1
            ) throw IllegalArgumentException("The end step of a where()-traversal can only have one label: $endStep")
            val label: String = endStep.getLabels().iterator().next()
            scopeKeys.add(label)
            endStep.removeLabel(label)
            whereTraversal.addStep(WhereEndStep(whereTraversal, label))
        }
    }

    @get:Override
    val maxRequirement: ElementRequirement
        get() = if (TraversalHelper.getVariableLocations(whereTraversal)
                .contains(Scoping.Variable.START)
        ) super@PathProcessor.getMaxRequirement() else ElementRequirement.ID

    @Override
    protected fun processNextStart(): Traverser.Admin<S> {
        return PathProcessor.processTraverserPathLabels(super.processNextStart(), keepLabels)
    }

    @Override
    protected fun filter(traverser: Traverser.Admin<S>?): Boolean {
        return TraversalUtil.test(traverser as Traverser.Admin?, whereTraversal)
    }

    @get:Override
    val localChildren: List<Any>
        get() = if (null == whereTraversal) Collections.emptyList() else Collections.singletonList(whereTraversal)

    @Override
    override fun toString(): String {
        return StringFactory.stepString(this, whereTraversal)
    }

    @Override
    fun getScopeKeys(): Set<String> {
        return Collections.unmodifiableSet(scopeKeys)
    }

    @Override
    fun clone(): WhereTraversalStep<S> {
        val clone = super.clone() as WhereTraversalStep<S>
        clone.whereTraversal = whereTraversal.clone()
        return clone
    }

    @Override
    fun setTraversal(parentTraversal: Traversal.Admin<*, *>?) {
        super.setTraversal(parentTraversal)
        integrateChild(whereTraversal)
    }

    @Override
    override fun hashCode(): Int {
        return super.hashCode() xor whereTraversal.hashCode()
    }

    @get:Override
    val requirements: Set<Any>
        get() = this.getSelfAndChildRequirements(TraverserRequirement.OBJECT, TraverserRequirement.SIDE_EFFECTS)

    @Override
    fun setKeepLabels(keepLabels: Set<String?>?) {
        this.keepLabels = HashSet(keepLabels)
    }

    @Override
    fun getKeepLabels(): Set<String>? {
        return keepLabels
    }

    //////////////////////////////
    class WhereStartStep<S>(traversal: Traversal.Admin?, private var selectKey: String?) :
        ScalarMapStep<S, Object?>(traversal), Scoping {
        @Override
        protected fun map(traverser: Traverser.Admin<S>): Object {
            if (this.getTraversal().getEndStep() is WhereEndStep) (this.getTraversal()
                .getEndStep() as WhereEndStep).processStartTraverser(traverser) else if (this.getTraversal()
                    .getEndStep() is ProfileStep && this.getTraversal().getEndStep().getPreviousStep() is WhereEndStep
            ) // TOTAL SUCKY HACK!
                (this.getTraversal().getEndStep().getPreviousStep() as WhereEndStep).processStartTraverser(traverser)
            return if (null == selectKey) traverser.get() else this.getSafeScopeValue(Pop.last, selectKey, traverser)
        }

        @Override
        override fun toString(): String {
            return StringFactory.stepString(this, selectKey)
        }

        @Override
        override fun hashCode(): Int {
            return super.hashCode() xor if (null == selectKey) "null".hashCode() else selectKey!!.hashCode()
        }

        fun removeScopeKey() {
            selectKey = null
        }

        @Override
        fun getScopeKeys(): Set<String> {
            return if (null == selectKey) Collections.emptySet() else Collections.singleton(selectKey)
        }
    }

    class WhereEndStep(traversal: Traversal.Admin?, private val matchKey: String?) : FilterStep<Object?>(traversal),
        Scoping {
        private var matchValue: Object? = null
        fun processStartTraverser(traverser: Traverser.Admin?) {
            if (null != matchKey) matchValue = this.getSafeScopeValue(Pop.last, matchKey, traverser)
        }

        @Override
        protected fun filter(traverser: Traverser.Admin<Object?>): Boolean {
            return null == matchKey || traverser.get().equals(matchValue)
        }

        @Override
        override fun toString(): String {
            return StringFactory.stepString(this, matchKey)
        }

        @Override
        override fun hashCode(): Int {
            return super.hashCode() xor if (null == matchKey) "null".hashCode() else matchKey.hashCode()
        }

        @Override
        fun getScopeKeys(): Set<String> {
            return if (null == matchKey) Collections.emptySet() else Collections.singleton(matchKey)
        }
    } //////////////////////////////
}