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
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class TraversalMapStep<S, E>(traversal: Traversal.Admin?, mapTraversal: Traversal<S, E>) : MapStep<S, E>(traversal),
    TraversalParent {
    private var mapTraversal: Traversal.Admin<S, E>

    init {
        this.mapTraversal = this.integrateChild(mapTraversal.asAdmin())
    }

    @Override
    @Throws(NoSuchElementException::class)
    protected fun processNextStart(): Traverser.Admin<E> {
        val traverser: Traverser.Admin<S> = this.starts.next()
        val iterator: Iterator<E> = TraversalUtil.applyAll(traverser, mapTraversal)
        return if (iterator.hasNext()) traverser.split(iterator.next(), this) else EmptyTraverser.instance()
    }

    @get:Override
    val localChildren: List<Any>
        get() = Collections.singletonList(mapTraversal)

    @Override
    fun clone(): TraversalMapStep<S, E> {
        val clone = super.clone() as TraversalMapStep<S, E>
        clone.mapTraversal = mapTraversal.clone()
        return clone
    }

    @Override
    fun setTraversal(parentTraversal: Traversal.Admin<*, *>?) {
        super.setTraversal(parentTraversal)
        this.integrateChild(mapTraversal)
    }

    @Override
    override fun toString(): String {
        return StringFactory.stepString(this, mapTraversal)
    }

    @Override
    override fun hashCode(): Int {
        return super.hashCode() xor mapTraversal.hashCode()
    }

    @get:Override
    val requirements: Set<Any>
        get() = this.getSelfAndChildRequirements()
}