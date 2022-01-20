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

import org.apache.tinkerpop4.gremlin.process.traversal.P

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class WherePredicateStep<S>(traversal: Traversal.Admin?, startKey: Optional<String?>, predicate: P<String?>) :
    FilterStep<S>(traversal), Scoping, PathProcessor, ByModulating, TraversalParent {
    protected var startKey: String?
    protected var selectKeys: List<String>
    protected var predicate: P<Object>
    protected val scopeKeys: Set<String> = HashSet()
    protected var keepLabels: Set<String>? = null
    protected var traversalRing: TraversalRing<S, *> = TraversalRing()

    init {
        this.startKey = startKey.orElse(null)
        if (null != this.startKey) scopeKeys.add(this.startKey)
        this.predicate = predicate as P
        selectKeys = ArrayList()
        configurePredicates(this.predicate)
    }

    private fun configurePredicates(predicate: P<Object?>) {
        if (predicate is ConnectiveP) (predicate as ConnectiveP<Object?>).getPredicates()
            .forEach { predicate: P<Object?> -> configurePredicates(predicate) } else {
            val selectKey = getSelectKey(predicate)
            selectKeys.add(selectKey)
            scopeKeys.add(selectKey)
        }
    }

    private fun setPredicateValues(
        predicate: P<Object?>,
        traverser: Traverser.Admin<S>,
        selectKeysIterator: Iterator<String>
    ): Boolean {
        return if (predicate is ConnectiveP) {
            for (p in (predicate as ConnectiveP<Object?>).getPredicates()) {
                if (!setPredicateValues(p, traverser, selectKeysIterator)) return false
            }
            true
        } else {
            val product: TraversalProduct = TraversalUtil.produce(
                this.getSafeScopeValue(Pop.last, selectKeysIterator.next(), traverser) as S,
                traversalRing.next()
            )
            if (product.isProductive()) predicate.setValue(product.get())
            product.isProductive()
        }
    }

    fun getPredicate(): Optional<P<*>> {
        return Optional.ofNullable(predicate)
    }

    fun getStartKey(): Optional<String> {
        return Optional.ofNullable(startKey)
    }

    fun getSelectKey(predicate: P<Object?>): String {
        return (if (predicate.getValue() is Collection) (predicate.getValue() as Collection).iterator()
            .next() else predicate.getValue()) as String // hack for within("x"))
    }

    fun removeStartKey() {
        selectKeys.remove(startKey)
        startKey = null
    }

    @Override
    protected fun filter(traverser: Traverser.Admin<S>): Boolean {
        val product: TraversalProduct = if (null == startKey) TraversalUtil.produce(
            traverser,
            traversalRing.next()
        ) else TraversalUtil.produce(this.getSafeScopeValue(Pop.last, startKey, traverser) as S, traversalRing.next())
        val predicateValuesProductive = setPredicateValues(predicate, traverser, selectKeys.iterator())
        traversalRing.reset()
        return product.isProductive() && predicateValuesProductive && predicate.test(product.get())
    }

    @Override
    override fun toString(): String {
        return StringFactory.stepString(this, startKey, predicate, traversalRing)
    }

    @Override
    fun getScopeKeys(): Set<String> {
        return Collections.unmodifiableSet(scopeKeys)
    }

    @Override
    fun clone(): WherePredicateStep<S> {
        val clone = super.clone() as WherePredicateStep<S>
        clone.predicate = predicate.clone()
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
        return super.hashCode() xor traversalRing.hashCode() xor (if (null == startKey) "null".hashCode() else startKey!!.hashCode()) xor predicate.hashCode()
    }

    @get:Override
    val requirements: Set<Any>
        get() = this.getSelfAndChildRequirements(TraverserRequirement.OBJECT, TraverserRequirement.SIDE_EFFECTS)

    @get:Override
    val localChildren: List<Any>
        get() = traversalRing.getTraversals()

    @Override
    protected fun processNextStart(): Traverser.Admin<S> {
        return PathProcessor.processTraverserPathLabels(super.processNextStart(), keepLabels)
    }

    @Override
    fun setKeepLabels(keepLabels: Set<String?>?) {
        this.keepLabels = HashSet(keepLabels)
    }

    @Override
    fun getKeepLabels(): Set<String>? {
        return keepLabels
    }

    @Override
    @Throws(UnsupportedOperationException::class)
    fun modulateBy(traversal: Traversal.Admin<*, *>?) {
        traversalRing.addTraversal(this.integrateChild(traversal))
    }

    @Override
    fun replaceLocalChild(oldTraversal: Traversal.Admin<*, *>?, newTraversal: Traversal.Admin<*, *>?) {
        traversalRing.replaceTraversal(
            oldTraversal as Traversal.Admin?,
            newTraversal as Traversal.Admin?
        )
    }
}