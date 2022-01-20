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
package org.apache.tinkerpop4.gremlin.process.traversal.step.map

import org.apache.tinkerpop4.gremlin.process.traversal.Traversal

/**
 * @author Daniel Kuppitz (http://gremlin.guru)
 */
class CoalesceStep<S, E> @SafeVarargs constructor(
    traversal: Traversal.Admin?,
    vararg coalesceTraversals: Traversal.Admin<S, E>?
) : FlatMapStep<S, E>(traversal), TraversalParent {
    private var coalesceTraversals: List<Traversal.Admin<S, E>>

    init {
        this.coalesceTraversals = Arrays.asList(coalesceTraversals)
        for (conjunctionTraversal in this.coalesceTraversals) {
            this.integrateChild(conjunctionTraversal)
        }
    }

    @Override
    protected fun flatMap(traverser: Traverser.Admin<S>): Iterator<E> {
        val innerTraverser: Traverser.Admin<S> = traverser.clone().asAdmin()
        innerTraverser.setBulk(1L)
        for (coalesceTraversal in coalesceTraversals) {
            coalesceTraversal.reset()
            coalesceTraversal.addStart(innerTraverser.split())
            if (coalesceTraversal.hasNext()) return coalesceTraversal
        }
        return EmptyIterator.instance()
    }

    @get:Override
    val requirements: Set<Any>
        get() = this.getSelfAndChildRequirements()

    @get:Override
    val localChildren: List<Any>
        get() = Collections.unmodifiableList(coalesceTraversals)

    @Override
    fun clone(): CoalesceStep<S, E> {
        val clone = super.clone() as CoalesceStep<S, E>
        clone.coalesceTraversals = ArrayList()
        for (conjunctionTraversal in coalesceTraversals) {
            clone.coalesceTraversals.add(conjunctionTraversal.clone())
        }
        return clone
    }

    @Override
    fun setTraversal(parentTraversal: Traversal.Admin<*, *>?) {
        super.setTraversal(parentTraversal)
        for (conjunctionTraversal in coalesceTraversals) {
            this.integrateChild(conjunctionTraversal)
        }
    }

    @Override
    override fun toString(): String {
        return StringFactory.stepString(this, coalesceTraversals)
    }

    @Override
    override fun hashCode(): Int {
        var result = super.hashCode()
        var i = 0
        for (traversal in coalesceTraversals) {
            result = result xor Integer.rotateLeft(traversal.hashCode(), i++)
        }
        return result
    }
}