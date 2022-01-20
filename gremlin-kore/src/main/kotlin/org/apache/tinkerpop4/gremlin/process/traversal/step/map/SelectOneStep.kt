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

import org.apache.tinkerpop4.gremlin.process.traversal.Pop

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class SelectOneStep<S, E>(traversal: Traversal.Admin?, pop: Pop?, selectKey: String) : MapStep<S, E>(traversal),
    TraversalParent, Scoping, PathProcessor, ByModulating {
    private val pop: Pop?
    private val selectKey: String
    private var selectTraversal: Traversal.Admin<S, E>? = null
    private var keepLabels: Set<String>? = null

    init {
        this.pop = pop
        this.selectKey = selectKey
    }

    @Override
    @Throws(NoSuchElementException::class)
    protected fun processNextStart(): Traverser.Admin<E> {
        val traverser: Traverser.Admin<S> = this.starts.next()
        return try {
            val o: S = getScopeValue(pop, selectKey, traverser) ?: return traverser.split(null, this)
            val product: TraversalProduct = TraversalUtil.produce(o, selectTraversal)
            if (!product.isProductive()) return EmptyTraverser.instance()
            val outTraverser: Traverser.Admin<E> = traverser.split(product.get() as E, this)
            if (this.getTraversal().getParent() !is MatchStep) PathProcessor.processTraverserPathLabels(
                outTraverser,
                keepLabels
            )
            outTraverser
        } catch (nfe: KeyNotFoundException) {
            EmptyTraverser.instance()
        }
    }

    @Override
    override fun toString(): String {
        return StringFactory.stepString(this, pop, selectKey, selectTraversal)
    }

    @Override
    fun clone(): SelectOneStep<S, E> {
        val clone = super.clone() as SelectOneStep<S, E>
        if (null != selectTraversal) clone.selectTraversal = selectTraversal.clone()
        return clone
    }

    @Override
    fun setTraversal(parentTraversal: Traversal.Admin<*, *>?) {
        super.setTraversal(parentTraversal)
        this.integrateChild(selectTraversal)
    }

    @Override
    override fun hashCode(): Int {
        var result = super.hashCode() xor selectKey.hashCode()
        if (null != selectTraversal) result = result xor selectTraversal.hashCode()
        if (null != pop) result = result xor pop.hashCode()
        return result
    }

    @get:Override
    val localChildren: List<Any>
        get() = if (null == selectTraversal) Collections.emptyList() else Collections.singletonList(selectTraversal)

    @Override
    fun removeLocalChild(traversal: Traversal.Admin<*, *>) {
        if (selectTraversal === traversal) selectTraversal = null
    }

    @Override
    fun modulateBy(selectTraversal: Traversal.Admin<*, *>?) {
        this.selectTraversal = this.integrateChild(selectTraversal)
    }

    @Override
    fun replaceLocalChild(oldTraversal: Traversal.Admin<*, *>?, newTraversal: Traversal.Admin<*, *>?) {
        if (null != selectTraversal && selectTraversal.equals(oldTraversal)) selectTraversal =
            this.integrateChild(newTraversal)
    }

    @get:Override
    val requirements: Set<Any>
        get() = this.getSelfAndChildRequirements(TraverserRequirement.OBJECT, TraverserRequirement.SIDE_EFFECTS)

    @get:Override
    val scopeKeys: Set<String>
        get() = Collections.singleton(selectKey)

    fun getPop(): Pop? {
        return pop
    }

    @Override
    fun setKeepLabels(keepLabels: Set<String?>?) {
        this.keepLabels = HashSet(keepLabels)
    }

    @Override
    fun getKeepLabels(): Set<String>? {
        return keepLabels
    }
}