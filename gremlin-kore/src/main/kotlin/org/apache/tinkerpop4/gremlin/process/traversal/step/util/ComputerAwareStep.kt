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

import org.apache.tinkerpop4.gremlin.process.traversal.Traversal

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
abstract class ComputerAwareStep<S, E>(traversal: Traversal.Admin?) : AbstractStep<S, E>(traversal), GraphComputing {
    private var previousIterator: Iterator<Traverser.Admin<E>> = EmptyIterator.instance()
    @Override
    @Throws(NoSuchElementException::class)
    protected fun processNextStart(): Traverser.Admin<E> {
        while (true) {
            if (previousIterator.hasNext()) return previousIterator.next()
            previousIterator =
                if (this.traverserStepIdAndLabelsSetByChild) computerAlgorithm()!! else standardAlgorithm()!!
        }
    }

    @Override
    fun onGraphComputer() {
        this.traverserStepIdAndLabelsSetByChild = true
    }

    @Override
    fun clone(): ComputerAwareStep<S, E> {
        val clone = super.clone() as ComputerAwareStep<S, E>
        clone.previousIterator = EmptyIterator.instance()
        return clone
    }

    @Override
    fun reset() {
        super.reset()
        previousIterator = EmptyIterator.instance()
    }

    @Throws(NoSuchElementException::class)
    protected abstract fun standardAlgorithm(): Iterator<Traverser.Admin<E>?>?
    @Throws(NoSuchElementException::class)
    protected abstract fun computerAlgorithm(): Iterator<Traverser.Admin<E>?>?

    //////
    class EndStep<S>(traversal: Traversal.Admin?) : AbstractStep<S, S>(traversal), GraphComputing {
        @Override
        @Throws(NoSuchElementException::class)
        protected fun processNextStart(): Traverser.Admin<S> {
            val start: Traverser.Admin<S> = this.starts.next()
            if (this.traverserStepIdAndLabelsSetByChild) {
                val step = this.getTraversal().getParent() as ComputerAwareStep<*, *>
                start.setStepId(step.getNextStep().getId())
                start.addLabels(step.getLabels())
            }
            return start
        }

        @Override
        override fun toString(): String {
            return StringFactory.stepString(this)
        }

        @Override
        fun onGraphComputer() {
            this.traverserStepIdAndLabelsSetByChild = true
        }
    }
}