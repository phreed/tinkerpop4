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
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class TraversalFlatMapStep<S, E>(traversal: Traversal.Admin?, flatMapTraversal: Traversal<S, E>) :
    FlatMapStep<S, E>(traversal), TraversalParent {
    private var flatMapTraversal: Traversal.Admin<S, E>

    init {
        this.flatMapTraversal = this.integrateChild(flatMapTraversal.asAdmin())
    }

    @Override
    protected fun flatMap(traverser: Traverser.Admin<S>?): Iterator<E> {
        return TraversalUtil.applyAll(traverser, flatMapTraversal)
    }

    @get:Override
    val localChildren: List<Any>
        get() = Collections.singletonList(flatMapTraversal)

    @Override
    fun clone(): TraversalFlatMapStep<S, E> {
        val clone = super.clone() as TraversalFlatMapStep<S, E>
        clone.flatMapTraversal = flatMapTraversal.clone()
        return clone
    }

    @Override
    fun setTraversal(parentTraversal: Traversal.Admin<*, *>?) {
        super.setTraversal(parentTraversal)
        this.integrateChild(flatMapTraversal)
    }

    @Override
    override fun toString(): String {
        return StringFactory.stepString(this, flatMapTraversal)
    }

    @Override
    override fun hashCode(): Int {
        return super.hashCode() xor flatMapTraversal.hashCode()
    }

    @get:Override
    val requirements: Set<Any>
        get() = this.getSelfAndChildRequirements()
}