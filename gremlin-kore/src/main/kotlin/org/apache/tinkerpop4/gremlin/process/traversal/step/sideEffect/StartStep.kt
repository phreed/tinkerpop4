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
package org.apache.tinkerpop4.gremlin.process.traversal.step.sideEffect

import org.apache.tinkerpop4.gremlin.process.traversal.Step

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class StartStep<S>(traversal: Traversal.Admin?, start: Object?) : AbstractStep<S, S>(traversal) {
    protected var start: Object?
    protected var first = true

    init {
        this.start = start
    }

    constructor(traversal: Traversal.Admin?) : this(traversal, null) {}

    fun <T> getStart(): T? {
        return start
    }

    @Override
    override fun toString(): String {
        return StringFactory.stepString(this, start)
    }

    @Override
    protected fun processNextStart(): Traverser.Admin<S> {
        if (first) {
            if (null != start) {
                if (start is Iterator) this.starts.add(
                    this.getTraversal().getTraverserGenerator().generateIterator(start as Iterator<S>?, this, 1L)
                ) else this.starts.add(
                    this.getTraversal().getTraverserGenerator().generate(
                        start as S?, this, 1L
                    )
                )
            }
            first = false
        }
        ///
        val start: Traverser.Admin<S> = this.starts.next()
        if (start.get() is Attachable &&
            this.getTraversal().getGraph().isPresent() &&
            (start.get() !is VertexProperty || null != (start.get() as VertexProperty).element())
        ) start.set((start.get() as Attachable<S>).attach(Attachable.Method.get(this.getTraversal().getGraph().get())))
        return start
    }

    @Override
    fun clone(): StartStep<S> {
        val clone = super.clone() as StartStep<S>
        clone.first = true
        clone.start = null // TODO: is this good?
        return clone
    }

    @Override
    override fun hashCode(): Int {
        var result = super.hashCode()
        if (start is Iterator) {
            val iterator: Iterator? = start
            val list: List = ArrayList()
            while (iterator.hasNext()) {
                val item: Object = iterator.next()
                if (item != null) result = result xor item.hashCode()
                list.add(item)
            }
            start = list.iterator()
        } else if (start != null) {
            result = result xor start.hashCode()
        }
        return result
    }

    companion object {
        fun isVariableStartStep(step: Step<*, *>): Boolean {
            return step.getClass()
                .equals(StartStep::class.java) && null == (step as StartStep<*>).start && (step as StartStep<*>).labels.size() === 1
        }
    }
}