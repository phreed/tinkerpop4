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

import org.apache.tinkerpop4.gremlin.process.traversal.Traversal

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class SackValueStep<S, A, B>(traversal: Traversal.Admin?, sackFunction: BiFunction<A, B, A>) :
    AbstractStep<S, S>(traversal), TraversalParent, ByModulating, LambdaHolder {
    private var sackTraversal: Traversal.Admin<S, B>? = null
    private val sackFunction: BiFunction<A, B, A>

    init {
        this.sackFunction = sackFunction
    }

    @Override
    fun modulateBy(sackTraversal: Traversal.Admin<*, *>?) {
        this.sackTraversal = this.integrateChild(sackTraversal)
    }

    @Override
    fun replaceLocalChild(oldTraversal: Traversal.Admin<*, *>?, newTraversal: Traversal.Admin<*, *>?) {
        if (null != sackTraversal && sackTraversal.equals(oldTraversal)) sackTraversal =
            this.integrateChild(newTraversal)
    }

    @get:Override
    val localChildren: List<Any>
        get() = if (null == sackTraversal) Collections.emptyList() else Collections.singletonList(sackTraversal)

    @Override
    protected fun processNextStart(): Traverser.Admin<S> {
        val traverser: Traverser.Admin<S> = this.starts.next()
        return if (null == sackTraversal) {
            traverser.sack(sackFunction.apply(traverser.sack(), traverser.get() as B))
            traverser
        } else {
            val product: TraversalProduct = TraversalUtil.produce(traverser, sackTraversal)
            if (!product.isProductive()) return EmptyTraverser.instance()
            traverser.sack(sackFunction.apply(traverser.sack(), product.get() as B))
            traverser
        }
    }

    @Override
    override fun toString(): String {
        return StringFactory.stepString(this, sackFunction, sackTraversal)
    }

    @Override
    override fun hashCode(): Int {
        return super.hashCode() xor sackFunction.hashCode() xor if (null == sackTraversal) "null".hashCode() else sackTraversal.hashCode()
    }

    fun getSackFunction(): BiFunction<A, B, A> {
        return sackFunction
    }

    @get:Override
    val requirements: Set<Any>
        get() = getSelfAndChildRequirements(TraverserRequirement.SACK)

    @Override
    fun clone(): SackValueStep<S, A, B> {
        val clone = super.clone() as SackValueStep<S, A, B>
        if (null != sackTraversal) clone.sackTraversal = sackTraversal.clone()
        return clone
    }

    @Override
    fun setTraversal(parentTraversal: Traversal.Admin<*, *>?) {
        super.setTraversal(parentTraversal)
        this.integrateChild(sackTraversal)
    }
}