/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.tinkerpop4.gremlin.process.traversal.step.branch

import org.apache.tinkerpop4.gremlin.process.traversal.Traversal

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class OptionalStep<S>(traversal: Traversal.Admin?, optionalTraversal: Traversal.Admin<S, S>) :
    AbstractStep<S, S>(traversal), TraversalParent {
    private var optionalTraversal: Traversal.Admin<S, S>

    init {
        this.optionalTraversal = optionalTraversal
    }

    @Override
    @Throws(NoSuchElementException::class)
    protected fun processNextStart(): Traverser.Admin<S> {
        return if (optionalTraversal.hasNext()) optionalTraversal.nextTraverser() else {
            val traverser: Traverser.Admin<S> = this.starts.next()
            optionalTraversal.reset()
            optionalTraversal.addStart(traverser.split())
            if (optionalTraversal.hasNext()) optionalTraversal.nextTraverser() else traverser
        }
    }

    @get:Override
    val localChildren: List<Any>
        get() = Collections.singletonList(optionalTraversal)

    @Override
    fun clone(): OptionalStep<S> {
        val clone = super.clone() as OptionalStep<S>
        clone.optionalTraversal = optionalTraversal.clone()
        return clone
    }

    @Override
    fun setTraversal(parentTraversal: Traversal.Admin<*, *>?) {
        super.setTraversal(parentTraversal)
        integrateChild(optionalTraversal)
    }

    @Override
    override fun toString(): String {
        return StringFactory.stepString(this, optionalTraversal)
    }

    @Override
    override fun hashCode(): Int {
        return super.hashCode() xor optionalTraversal.hashCode()
    }

    @get:Override
    val requirements: Set<Any>
        get() = optionalTraversal.getTraverserRequirements()

    @Override
    fun reset() {
        super.reset()
        optionalTraversal.reset()
    }
}