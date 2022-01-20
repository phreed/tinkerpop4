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
package org.apache.tinkerpop4.gremlin.process.computer.traversal.step.map

import org.apache.tinkerpop4.gremlin.process.computer.ComputerResult

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class ComputerResultStep<S>(traversal: Traversal.Admin?) : AbstractStep<ComputerResult?, S>(traversal) {
    private val attachElements: Boolean = Boolean.valueOf(System.getProperty("is.testing", "false"))
    private var currentIterator: Iterator<Traverser.Admin<S>> = EmptyIterator.instance()
    fun attach(iterator: Iterator<Traverser.Admin<S>?>?, graph: Graph?): Iterator<Traverser.Admin<S>> {
        return IteratorUtils.map(iterator) { traverser ->
            traverser.setSideEffects(this.getTraversal().getSideEffects()) // necessary to ensure no NPE
            if (attachElements && traverser.get() is Attachable && traverser.get() !is Property) traverser.set(
                (traverser.get() as Attachable<Element?>).attach(
                    Attachable.Method.get(graph)
                ) as S
            )
            traverser
        }
    }

    @Override
    @Throws(NoSuchElementException::class)
    protected fun processNextStart(): Traverser.Admin<S> {
        while (true) {
            if (currentIterator.hasNext()) return currentIterator.next() else {
                val result: ComputerResult = this.starts.next().get()
                currentIterator = attach(
                    if (result.memory()
                            .exists(TraversalVertexProgram.HALTED_TRAVERSERS)
                    ) result.memory().< TraverserSet < S > > get<TraverserSet<S>>(TraversalVertexProgram.HALTED_TRAVERSERS).iterator() else EmptyIterator.instance(),
                    result.graph()
                )
            }
        }
    }

    @Override
    override fun toString(): String {
        return StringFactory.stepString(this)
    }

    @get:Override
    val requirements: Set<Any>
        get() = EnumSet.of(TraverserRequirement.OBJECT)

    @Override
    fun clone(): ComputerResultStep<S> {
        val clone = super.clone() as ComputerResultStep<S>
        clone.currentIterator = EmptyIterator.instance()
        return clone
    }
}