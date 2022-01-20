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
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class SelectStep<S, E>(traversal: Traversal.Admin?, pop: Pop?, vararg selectKeys: String?) :
    MapStep<S, Map<String?, E>?>(traversal), Scoping, TraversalParent, PathProcessor, ByModulating {
    private var traversalRing: TraversalRing<Object, E> = TraversalRing()
    private val pop: Pop?
    private val selectKeys: List<String>

    @get:Override
    val scopeKeys: Set<String>
    private var keepLabels: Set<String>? = null

    init {
        this.pop = pop
        this.selectKeys = Arrays.asList(selectKeys)
        scopeKeys = Collections.unmodifiableSet(HashSet(this.selectKeys))
        if (this.selectKeys.size() < 2) throw IllegalArgumentException("At least two select keys must be provided: $this")
    }

    @Override
    @Throws(NoSuchElementException::class)
    protected fun processNextStart(): Traverser.Admin<Map<String, E>> {
        val traverser: Traverser.Admin<S> = this.starts.next()
        val bindings: Map<String, E> = LinkedHashMap(selectKeys.size(), 1.0f)
        try {
            for (selectKey in selectKeys) {
                val end: E = this.getScopeValue(pop, selectKey, traverser)
                val product: TraversalProduct = TraversalUtil.produce(end, traversalRing.next())
                if (!product.isProductive()) break
                bindings.put(selectKey, product.get() as E)
            }

            // bindings should be the same size as keys or else there was an uproductive by() in which case we filter
            // with an EmptyTraverser
            if (bindings.size() !== selectKeys.size()) return EmptyTraverser.instance()
        } catch (nfe: KeyNotFoundException) {
            return EmptyTraverser.instance()
        } finally {
            traversalRing.reset()
        }
        return PathProcessor.processTraverserPathLabels(traverser.split(bindings, this), keepLabels)
    }

    @Override
    fun reset() {
        super.reset()
        traversalRing.reset()
    }

    @Override
    override fun toString(): String {
        return StringFactory.stepString(this, pop, selectKeys, traversalRing)
    }

    @Override
    fun clone(): SelectStep<S, E> {
        val clone = super.clone() as SelectStep<S, E>
        clone.traversalRing = traversalRing.clone()
        return clone
    }

    @Override
    fun setTraversal(parentTraversal: Traversal.Admin<*, *>?) {
        super.setTraversal(parentTraversal)
        traversalRing.getTraversals().forEach(this::integrateChild)
    }

    @Override
    override fun hashCode(): Int {
        var result = super.hashCode() xor traversalRing.hashCode() xor selectKeys.hashCode()
        if (null != pop) result = result xor pop.hashCode()
        return result
    }

    @get:Override
    val localChildren: List<Any>
        get() = traversalRing.getTraversals()

    @Override
    fun modulateBy(selectTraversal: Traversal.Admin<*, *>?) {
        traversalRing.addTraversal(this.integrateChild(selectTraversal))
    }

    @Override
    fun replaceLocalChild(oldTraversal: Traversal.Admin<*, *>?, newTraversal: Traversal.Admin<*, *>?) {
        traversalRing.replaceTraversal(
            oldTraversal as Traversal.Admin<Object?, E>?,
            newTraversal as Traversal.Admin<Object?, E>?
        )
    }

    @get:Override
    val requirements: Set<Any>
        get() = this.getSelfAndChildRequirements(TraverserRequirement.OBJECT, TraverserRequirement.SIDE_EFFECTS)
    val byTraversals: Map<String, Any>
        get() {
            val map: Map<String, Traversal.Admin<Object, E>> = HashMap()
            traversalRing.reset()
            for (`as` in selectKeys) {
                map.put(`as`, traversalRing.next())
            }
            return map
        }

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