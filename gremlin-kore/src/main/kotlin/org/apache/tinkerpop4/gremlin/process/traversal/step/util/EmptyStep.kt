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
class EmptyStep<S, E> private constructor() : Step<S, E>, TraversalParent {
    @Override
    fun addStarts(starts: Iterator<Traverser.Admin<S>?>?) {
    }

    @Override
    fun hasStarts(): Boolean {
        return false
    }

    @Override
    fun addStart(start: Traverser.Admin<S>?) {
    }

    @Override
    fun reset() {
    }

    @get:Override
    @set:Override
    var previousStep: Step<*, S>
        get() = INSTANCE
        set(step) {}

    @get:Override
    @set:Override
    var nextStep: Step<E, *>
        get() = INSTANCE
        set(step) {}

    @Override
    fun <A, B> getTraversal(): Traversal.Admin<A, B> {
        return EmptyTraversal.instance()
    }

    @Override
    fun setTraversal(traversal: Traversal.Admin<*, *>?) {
    }

    @Override
    @SuppressWarnings("CloneDoesntCallSuperClone")
    fun clone(): EmptyStep<S, E> {
        return INSTANCE
    }

    @get:Override
    val labels: Set<String>
        get() = Collections.emptySet()

    @Override
    fun addLabel(label: String?) {
    }

    @Override
    fun removeLabel(label: String?) {
    }

    @get:Override
    @set:Override
    var id: String?
        get() = Traverser.Admin.HALT
        set(id) {}

    @Override
    operator fun hasNext(): Boolean {
        return false
    }

    @Override
    operator fun next(): Traverser.Admin<E> {
        throw FastNoSuchElementException.instance()
    }

    @Override
    override fun hashCode(): Int {
        return -1691648095
    }

    @Override
    override fun equals(`object`: Object?): Boolean {
        return `object` is EmptyStep<*, *>
    }

    @get:Override
    val requirements: Set<Any>
        get() = Collections.emptySet()

    companion object {
        private val INSTANCE: EmptyStep<*, *> = EmptyStep<Any, Any>()
        fun <S, E> instance(): EmptyStep<S, E> {
            return INSTANCE
        }
    }
}