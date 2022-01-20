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

import org.apache.tinkerpop4.gremlin.process.traversal.Traversal

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class LocalStep<S, E>(traversal: Traversal.Admin?, localTraversal: Traversal.Admin<S, E>?) :
    AbstractStep<S, E>(traversal), TraversalParent {
    private var localTraversal: Traversal.Admin<S, E>
    private var first = true

    init {
        this.localTraversal = this.integrateChild(localTraversal)
    }

    @get:Override
    val localChildren: List<Any>
        get() = Collections.singletonList(localTraversal)

    @get:Override
    val requirements: Set<Any>
        get() = localTraversal.getTraverserRequirements()

    @Override
    @Throws(NoSuchElementException::class)
    protected fun processNextStart(): Traverser.Admin<E> {
        if (first) {
            first = false
            localTraversal.addStart(this.starts.next())
        }
        while (true) {
            if (localTraversal.hasNext()) return localTraversal.nextTraverser() else if (this.starts.hasNext()) {
                localTraversal.reset()
                localTraversal.addStart(this.starts.next())
            } else {
                throw FastNoSuchElementException.instance()
            }
        }
    }

    @Override
    fun reset() {
        super.reset()
        first = true
        localTraversal.reset()
    }

    @Override
    fun clone(): LocalStep<S, E> {
        val clone = super.clone() as LocalStep<S, E>
        clone.localTraversal = localTraversal.clone()
        clone.first = true
        return clone
    }

    @Override
    fun setTraversal(parentTraversal: Traversal.Admin<*, *>?) {
        super.setTraversal(parentTraversal)
        this.integrateChild(localTraversal)
    }

    @Override
    override fun toString(): String {
        return StringFactory.stepString(this, localTraversal)
    }

    @Override
    override fun hashCode(): Int {
        return super.hashCode() xor localTraversal.hashCode()
    }
}