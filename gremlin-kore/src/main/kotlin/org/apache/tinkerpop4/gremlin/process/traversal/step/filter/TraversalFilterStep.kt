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
package org.apache.tinkerpop4.gremlin.process.traversal.step.filter

import org.apache.tinkerpop4.gremlin.process.traversal.Traversal

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class TraversalFilterStep<S>(traversal: Traversal.Admin?, filterTraversal: Traversal<S, *>) : FilterStep<S>(traversal),
    TraversalParent, Configuring {
    private val parameters: Parameters = Parameters()
    private var filterTraversal: Traversal.Admin<S, *>

    init {
        this.filterTraversal = this.integrateChild(filterTraversal.asAdmin())
    }

    fun getFilterTraversal(): Traversal.Admin<S, *> {
        return filterTraversal
    }

    @Override
    fun getParameters(): Parameters {
        return parameters
    }

    @Override
    fun configure(vararg keyValues: Object?) {
        parameters.set(null, keyValues)
    }

    @Override
    protected fun filter(traverser: Traverser.Admin<S>?): Boolean {
        return TraversalUtil.test(traverser, filterTraversal)
    }

    @get:Override
    val localChildren: List<Any>
        get() = Collections.singletonList(filterTraversal)

    @Override
    fun clone(): TraversalFilterStep<S> {
        val clone = super.clone() as TraversalFilterStep<S>
        clone.filterTraversal = filterTraversal.clone()
        return clone
    }

    @Override
    fun setTraversal(parentTraversal: Traversal.Admin<*, *>?) {
        super.setTraversal(parentTraversal)
        integrateChild(filterTraversal)
    }

    @Override
    override fun toString(): String {
        return StringFactory.stepString(this, filterTraversal)
    }

    @Override
    override fun hashCode(): Int {
        return super.hashCode() xor filterTraversal.hashCode()
    }

    @get:Override
    val requirements: Set<Any>
        get() = this.getSelfAndChildRequirements()
}