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
package org.apache.tinkerpop4.gremlin.process.traversal.step.branch

import org.apache.tinkerpop4.gremlin.process.traversal.Step

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class RepeatStep<S>(traversal: Traversal.Admin?) : ComputerAwareStep<S, S>(traversal), TraversalParent {
    private var repeatTraversal: Traversal.Admin<S, S>? = null
    private var untilTraversal: Traversal.Admin<S, *>? = null
    private var emitTraversal: Traversal.Admin<S, *>? = null
    private var loopName: String? = null
    var untilFirst = false
    var emitFirst = false

    @get:Override
    val requirements: Set<Any>
        get() {
            val requirements: Set<TraverserRequirement> = this.getSelfAndChildRequirements(TraverserRequirement.BULK)
            if (requirements.contains(TraverserRequirement.SINGLE_LOOP)) requirements.add(TraverserRequirement.NESTED_LOOP)
            requirements.add(TraverserRequirement.SINGLE_LOOP)
            return requirements
        }

    @SuppressWarnings("unchecked")
    fun setRepeatTraversal(repeatTraversal: Traversal.Admin<S, S>) {
        if (null != this.repeatTraversal) throw IllegalStateException("The repeat()-step already has its loop section declared: $this")
        this.repeatTraversal = repeatTraversal // .clone();
        this.repeatTraversal.addStep(RepeatEndStep<Any?>(this.repeatTraversal))
        this.integrateChild(this.repeatTraversal)
    }

    fun setLoopName(loopName: String?) {
        this.loopName = loopName
    }

    fun setUntilTraversal(untilTraversal: Traversal.Admin<S, *>?) {
        if (null != this.untilTraversal) throw IllegalStateException("The repeat()-step already has its until()-modulator declared: $this")
        if (null == repeatTraversal) untilFirst = true
        this.untilTraversal = this.integrateChild(untilTraversal)
    }

    fun getUntilTraversal(): Traversal.Admin<S, *> {
        return untilTraversal
    }

    fun setEmitTraversal(emitTraversal: Traversal.Admin<S, *>?) {
        if (null != this.emitTraversal) throw IllegalStateException("The repeat()-step already has its emit()-modulator declared: $this")
        if (null == repeatTraversal) emitFirst = true
        this.emitTraversal = this.integrateChild(emitTraversal)
    }

    fun getEmitTraversal(): Traversal.Admin<S, *> {
        return emitTraversal
    }

    fun getRepeatTraversal(): Traversal.Admin<S, S> {
        return repeatTraversal
    }

    val globalChildren: List<Any>
        get() = if (null == repeatTraversal) Collections.emptyList() else Collections.singletonList(repeatTraversal)
    val localChildren: List<Any>
        get() {
            val list: List<Traversal.Admin<S, *>> = ArrayList(2)
            if (null != untilTraversal) list.add(untilTraversal)
            if (null != emitTraversal) list.add(emitTraversal)
            return list
        }

    fun doUntil(traverser: Traverser.Admin<S>?, utilFirst: Boolean): Boolean {
        return utilFirst == untilFirst && null != untilTraversal && TraversalUtil.test(traverser, untilTraversal)
    }

    fun doEmit(traverser: Traverser.Admin<S>?, emitFirst: Boolean): Boolean {
        return emitFirst == this.emitFirst && null != emitTraversal && TraversalUtil.test(traverser, emitTraversal)
    }

    @Override
    override fun toString(): String {
        return if (untilFirst && emitFirst) StringFactory.stepString(
            this,
            untilString(),
            emitString(),
            repeatTraversal
        ) else if (emitFirst) StringFactory.stepString(
            this,
            emitString(),
            repeatTraversal,
            untilString()
        ) else if (untilFirst) StringFactory.stepString(
            this,
            untilString(),
            repeatTraversal,
            emitString()
        ) else StringFactory.stepString(this, repeatTraversal, untilString(), emitString())
    }

    @Override
    fun reset() {
        super.reset()
        if (null != emitTraversal) emitTraversal.reset()
        if (null != untilTraversal) untilTraversal.reset()
        if (null != repeatTraversal) repeatTraversal.reset()
    }

    private fun untilString(): String {
        return if (null == untilTraversal) "until(false)" else "until(" + untilTraversal + ')'
    }

    private fun emitString(): String {
        return if (null == emitTraversal) "emit(false)" else "emit(" + emitTraversal + ')'
    }

    /////////////////////////
    @Override
    fun clone(): RepeatStep<S> {
        val clone = super.clone() as RepeatStep<S>
        clone.repeatTraversal = repeatTraversal.clone()
        if (null != untilTraversal) clone.untilTraversal = untilTraversal.clone()
        if (null != emitTraversal) clone.emitTraversal = emitTraversal.clone()
        return clone
    }

    @Override
    fun setTraversal(parentTraversal: Traversal.Admin<*, *>?) {
        super.setTraversal(parentTraversal)
        this.integrateChild(repeatTraversal)
        this.integrateChild(untilTraversal)
        this.integrateChild(emitTraversal)
    }

    @Override
    override fun hashCode(): Int {
        var result = super.hashCode() xor repeatTraversal.hashCode()
        result = result xor Boolean.hashCode(untilFirst)
        result = result xor (Boolean.hashCode(emitFirst) shl 1)
        if (loopName != null) result = result xor loopName!!.hashCode()
        if (untilTraversal != null) result = result xor untilTraversal.hashCode()
        if (emitTraversal != null) result = result xor emitTraversal.hashCode()
        return result
    }

    @Override
    @Throws(NoSuchElementException::class)
    protected fun standardAlgorithm(): Iterator<Traverser.Admin<S>> {
        if (null == repeatTraversal) throw IllegalStateException("The repeat()-traversal was not defined: $this")
        while (true) {
            if (repeatTraversal.getEndStep().hasNext()) {
                return repeatTraversal.getEndStep()
            } else {
                val start: Traverser.Admin<S> = this.starts.next()
                start.initialiseLoops(this.getId(), loopName)
                if (doUntil(start, true)) {
                    start.resetLoops()
                    return IteratorUtils.of(start)
                }
                repeatTraversal.addStart(start)
                if (doEmit(start, true)) {
                    val emitSplit: Traverser.Admin<S> = start.split()
                    emitSplit.resetLoops()
                    return IteratorUtils.of(emitSplit)
                }
            }
        }
    }

    @Override
    @Throws(NoSuchElementException::class)
    protected fun computerAlgorithm(): Iterator<Traverser.Admin<S>> {
        if (null == repeatTraversal) throw IllegalStateException("The repeat()-traversal was not defined: $this")
        val start: Traverser.Admin<S> = this.starts.next()
        return if (doUntil(start, true)) {
            start.setStepId(this.getNextStep().getId())
            start.addLabels(this.labels)
            IteratorUtils.of(start)
        } else {
            start.setStepId(repeatTraversal.getStartStep().getId())
            start.initialiseLoops(start.getStepId(), loopName)
            if (doEmit(start, true)) {
                val emitSplit: Traverser.Admin<S> = start.split()
                emitSplit.resetLoops()
                emitSplit.setStepId(this.getNextStep().getId())
                IteratorUtils.of(start, emitSplit)
            } else {
                IteratorUtils.of(start)
            }
        }
    }

    ///////////////////////////////////
    class RepeatEndStep<S>(traversal: Traversal.Admin?) : ComputerAwareStep<S, S>(traversal) {
        @Override
        @Throws(NoSuchElementException::class)
        protected fun standardAlgorithm(): Iterator<Traverser.Admin<S>> {
            val repeatStep = this.getTraversal().getParent() as RepeatStep<S>
            while (true) {
                val start: Traverser.Admin<S> = this.starts.next()
                start.incrLoops()
                if (repeatStep.doUntil(start, false)) {
                    start.resetLoops()
                    return IteratorUtils.of(start)
                } else {
                    if (!repeatStep.untilFirst && !repeatStep.emitFirst) repeatStep.repeatTraversal.addStart(start) else repeatStep.addStart(
                        start
                    )
                    if (repeatStep.doEmit(start, false)) {
                        val emitSplit: Traverser.Admin<S> = start.split()
                        emitSplit.resetLoops()
                        return IteratorUtils.of(emitSplit)
                    }
                }
            }
        }

        @Override
        @Throws(NoSuchElementException::class)
        protected fun computerAlgorithm(): Iterator<Traverser.Admin<S>> {
            val repeatStep = this.getTraversal().getParent() as RepeatStep<S>
            val start: Traverser.Admin<S> = this.starts.next()
            start.incrLoops()
            return if (repeatStep.doUntil(start, false)) {
                start.resetLoops()
                start.setStepId(repeatStep.getNextStep().getId())
                start.addLabels(repeatStep.labels)
                IteratorUtils.of(start)
            } else {
                start.setStepId(repeatStep.getId())
                if (repeatStep.doEmit(start, false)) {
                    val emitSplit: Traverser.Admin<S> = start.split()
                    emitSplit.resetLoops()
                    emitSplit.setStepId(repeatStep.getNextStep().getId())
                    emitSplit.addLabels(repeatStep.labels)
                    return IteratorUtils.of(start, emitSplit)
                }
                IteratorUtils.of(start)
            }
        }
    }

    companion object {
        /////////////////////////
        fun <A, B, C : Traversal<A, B>?> addRepeatToTraversal(traversal: C, repeatTraversal: Traversal.Admin<B, B>): C {
            val step: Step<*, B> = traversal.asAdmin().getEndStep()
            if (step is RepeatStep<*> && null == (step as RepeatStep<*>).repeatTraversal) {
                (step as RepeatStep<B>).setRepeatTraversal(repeatTraversal)
            } else {
                val repeatStep: RepeatStep<B> = RepeatStep(traversal.asAdmin())
                repeatStep.setRepeatTraversal(repeatTraversal)
                traversal.asAdmin().addStep(repeatStep)
            }
            return traversal
        }

        fun <A, B, C : Traversal<A, B>?> addRepeatToTraversal(
            traversal: C,
            loopName: String?,
            repeatTraversal: Traversal.Admin<B, B>
        ): C {
            addRepeatToTraversal(traversal, repeatTraversal)
            val step: Step<*, B> = traversal.asAdmin().getEndStep()
            (step as RepeatStep<*>).loopName = loopName
            return traversal
        }

        fun <A, B, C : Traversal<A, B>?> addUntilToTraversal(traversal: C, untilPredicate: Traversal.Admin<B, *>): C {
            val step: Step<*, B> = traversal.asAdmin().getEndStep()
            if (step is RepeatStep<*> && null == (step as RepeatStep<*>).untilTraversal) {
                (step as RepeatStep<B>).setUntilTraversal(untilPredicate)
            } else {
                val repeatStep: RepeatStep<B> = RepeatStep(traversal.asAdmin())
                repeatStep.setUntilTraversal(untilPredicate)
                traversal.asAdmin().addStep(repeatStep)
            }
            return traversal
        }

        fun <A, B, C : Traversal<A, B>?> addEmitToTraversal(traversal: C, emitPredicate: Traversal.Admin<B, *>): C {
            val step: Step<*, B> = traversal.asAdmin().getEndStep()
            if (step is RepeatStep<*> && null == (step as RepeatStep<*>).emitTraversal) {
                (step as RepeatStep<B>).setEmitTraversal(emitPredicate)
            } else {
                val repeatStep: RepeatStep<B> = RepeatStep(traversal.asAdmin())
                repeatStep.setEmitTraversal(emitPredicate)
                traversal.asAdmin().addStep(repeatStep)
            }
            return traversal
        }
    }
}