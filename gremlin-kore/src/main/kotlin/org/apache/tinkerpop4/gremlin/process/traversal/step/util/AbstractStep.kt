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
package org.apache.tinkerpop4.gremlin.process.traversal.step.util

import org.apache.tinkerpop4.gremlin.process.traversal.Step

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
abstract class AbstractStep<S, E>(traversal: Traversal.Admin) : Step<S, E> {
    protected var labels: Set<String> = LinkedHashSet()
    protected var id: String = Traverser.Admin.HALT
    protected var traversal: Traversal.Admin
    protected var starts: ExpandableStepIterator<S>
    protected var nextEnd: Traverser.Admin<E> = EmptyTraverser.instance()
    var isTraverserStepIdAndLabelsSetByChild = false
        protected set
    protected var previousStep: Step<*, S> = EmptyStep.instance()
    protected var nextStep: Step<E, *> = EmptyStep.instance()

    init {
        this.traversal = traversal
        starts = ExpandableStepIterator(this, traversal.getTraverserSetSupplier().get() as TraverserSet<S>)
    }

    @Override
    fun setId(id: String) {
        Objects.requireNonNull(id)
        this.id = id
    }

    @Override
    fun getId(): String {
        return id
    }

    @Override
    fun addLabel(label: String?) {
        labels.add(label)
    }

    @Override
    fun removeLabel(label: String?) {
        labels.remove(label)
    }

    @Override
    fun getLabels(): Set<String> {
        return Collections.unmodifiableSet(labels)
    }

    @Override
    fun reset() {
        starts.clear()
        nextEnd = EmptyTraverser.instance()
    }

    @Override
    fun addStarts(starts: Iterator<Traverser.Admin<S>?>?) {
        this.starts.add(starts)
    }

    @Override
    fun addStart(start: Traverser.Admin<S>?) {
        starts.add(start)
    }

    @Override
    fun hasStarts(): Boolean {
        return starts.hasNext()
    }

    @Override
    fun setPreviousStep(step: Step<*, S>) {
        previousStep = step
    }

    @Override
    fun getPreviousStep(): Step<*, S> {
        return previousStep
    }

    @Override
    fun setNextStep(step: Step<E, *>) {
        nextStep = step
    }

    @Override
    fun getNextStep(): Step<E, *> {
        return nextStep
    }

    @Override
    operator fun next(): Traverser.Admin<E> {
        if (EmptyTraverser.instance() !== nextEnd) {
            return try {
                prepareTraversalForNextStep(nextEnd)
            } finally {
                nextEnd = EmptyTraverser.instance()
            }
        } else {
            while (true) {
                if (Thread.interrupted()) throw TraversalInterruptedException()
                val traverser: Traverser.Admin<E> = processNextStart()
                if (traverser.bulk() > 0) return prepareTraversalForNextStep(traverser)
            }
        }
    }

    @Override
    operator fun hasNext(): Boolean {
        if (EmptyTraverser.instance() !== nextEnd) return true else {
            try {
                while (true) {
                    if (Thread.interrupted()) throw TraversalInterruptedException()
                    nextEnd = processNextStart()
                    if (nextEnd.bulk() > 0) return true else nextEnd = EmptyTraverser.instance()
                }
            } catch (e: NoSuchElementException) {
                return false
            }
        }
    }

    @Override
    fun <A, B> getTraversal(): Traversal.Admin<A, B> {
        return traversal
    }

    @Override
    fun setTraversal(traversal: Traversal.Admin<*, *>) {
        this.traversal = traversal
    }

    @Throws(NoSuchElementException::class)
    protected abstract fun processNextStart(): Traverser.Admin<E>?

    @Override
    override fun toString(): String {
        return StringFactory.stepString(this)
    }

    @Override
    @SuppressWarnings("CloneDoesntDeclareCloneNotSupportedException")
    fun clone(): AbstractStep<S, E> {
        return try {
            val clone = super.clone() as AbstractStep<S, E>
            clone.starts = ExpandableStepIterator(clone, traversal.getTraverserSetSupplier().get() as TraverserSet<S>)
            clone.previousStep = EmptyStep.instance()
            clone.nextStep = EmptyStep.instance()
            clone.nextEnd = EmptyTraverser.instance()
            clone.traversal = EmptyTraversal.instance()
            clone.labels = LinkedHashSet(labels)
            clone.reset()
            clone
        } catch (e: CloneNotSupportedException) {
            throw IllegalStateException(e.getMessage(), e)
        }
    }

    @Override
    override fun equals(other: Object?): Boolean {
        return other != null && other.getClass().equals(this.getClass()) && hashCode() == other.hashCode()
    }

    @Override
    override fun hashCode(): Int {
        var result: Int = this.getClass().hashCode()
        for (label in getLabels()) {
            result = result xor label.hashCode()
        }
        return result
    }

    fun getStarts(): ExpandableStepIterator<S> {
        return starts
    }

    protected fun prepareTraversalForNextStep(traverser: Traverser.Admin<E>): Traverser.Admin<E> {
        if (!isTraverserStepIdAndLabelsSetByChild) {
            traverser.setStepId(nextStep.getId())
            traverser.addLabels(labels)
        }
        return traverser
    }
}