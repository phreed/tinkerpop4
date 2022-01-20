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

import org.apache.tinkerpop4.gremlin.process.computer.MemoryComputeKey

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class DedupGlobalStep<S>(traversal: Traversal.Admin?, vararg dedupLabels: String?) : FilterStep<S>(traversal),
    TraversalParent, Scoping, GraphComputing, Barrier<Map<Object?, Traverser.Admin<S>?>?>, ByModulating, PathProcessor {
    private var dedupTraversal: Traversal.Admin<S, Object>? = null
    private var duplicateSet: Set<Object> = HashSet()
    private var onGraphComputer = false
    private val dedupLabels: Set<String>?
    private var keepLabels: Set<String>? = null
    private var executingAtMaster = false
    private var barrier: Map<Object, Traverser.Admin<S>>? = null
    private var barrierIterator: Iterator<Map.Entry<Object, Traverser.Admin<S>>>? = null

    init {
        this.dedupLabels =
            if (dedupLabels.size == 0) null else Collections.unmodifiableSet(HashSet(Arrays.asList(dedupLabels)))
    }

    @Override
    protected fun filter(traverser: Traverser.Admin<S>): Boolean {
        if (onGraphComputer && !executingAtMaster) return true
        traverser.setBulk(1L)
        return if (null == dedupLabels) {
            val product: TraversalProduct = TraversalUtil.produce(traverser, dedupTraversal)
            product.isProductive() && duplicateSet.add(product.get())
        } else {
            val objects: List<Object> = ArrayList(dedupLabels.size())
            for (label in dedupLabels) {
                val product: TraversalProduct =
                    TraversalUtil.produce(this.getSafeScopeValue(Pop.last, label, traverser) as S, dedupTraversal)
                if (!product.isProductive()) break
                objects.add(product.get())
            }

            // the object sizes must be equal or else it means a by() wasn't productive and that path will be filtered
            objects.size() === dedupLabels.size() && duplicateSet.add(objects)
        }
    }

    @Override
    fun atMaster(atMaster: Boolean) {
        executingAtMaster = atMaster
    }

    @get:Override
    val maxRequirement: ElementRequirement
        get() = if (null == dedupLabels) ElementRequirement.ID else super@PathProcessor.getMaxRequirement()

    @Override
    protected fun processNextStart(): Traverser.Admin<S> {
        if (null != barrier) {
            barrierIterator = barrier.entrySet().iterator()
            barrier = null
        }
        while (barrierIterator != null && barrierIterator!!.hasNext()) {
            if (null == barrierIterator) barrierIterator = barrier.entrySet().iterator()
            val entry: Map.Entry<Object, Traverser.Admin<S>> = barrierIterator!!.next()
            if (duplicateSet.add(entry.getKey())) return PathProcessor.processTraverserPathLabels(
                entry.getValue(),
                keepLabels
            )
        }
        return PathProcessor.processTraverserPathLabels(super.processNextStart(), keepLabels)
    }

    @get:Override
    val localChildren: List<Any>
        get() = if (null == dedupTraversal) Collections.emptyList() else Collections.singletonList(dedupTraversal)

    @Override
    fun modulateBy(dedupTraversal: Traversal.Admin<*, *>?) {
        this.dedupTraversal = this.integrateChild(dedupTraversal)
    }

    @Override
    fun replaceLocalChild(oldTraversal: Traversal.Admin<*, *>?, newTraversal: Traversal.Admin<*, *>?) {
        if (null != dedupTraversal && dedupTraversal.equals(oldTraversal)) dedupTraversal =
            this.integrateChild(newTraversal)
    }

    @Override
    fun clone(): DedupGlobalStep<S> {
        val clone = super.clone() as DedupGlobalStep<S>
        clone.duplicateSet = HashSet()
        if (null != dedupTraversal) clone.dedupTraversal = dedupTraversal.clone()
        return clone
    }

    @Override
    fun setTraversal(parentTraversal: Traversal.Admin<*, *>?) {
        super.setTraversal(parentTraversal)
        this.integrateChild(dedupTraversal)
    }

    @Override
    override fun hashCode(): Int {
        var result = super.hashCode()
        if (dedupTraversal != null) result = result xor dedupTraversal.hashCode()
        if (dedupLabels != null) result = result xor dedupLabels.hashCode()
        return result
    }

    @Override
    fun reset() {
        super.reset()
        duplicateSet.clear()
        barrier = null
        barrierIterator = null
    }

    @Override
    override fun toString(): String {
        return StringFactory.stepString(this, dedupLabels, dedupTraversal)
    }

    @get:Override
    val requirements: Set<Any>
        get() = if (dedupLabels == null) this.getSelfAndChildRequirements(TraverserRequirement.BULK) else this.getSelfAndChildRequirements(
            TraverserRequirement.LABELED_PATH,
            TraverserRequirement.BULK
        )

    @Override
    fun onGraphComputer() {
        onGraphComputer = true
    }

    @get:Override
    val scopeKeys: Set<String>
        get() = dedupLabels ?: Collections.emptySet()

    @Override
    fun processAllStarts() {
    }

    @Override
    fun hasNextBarrier(): Boolean {
        return null != barrier || this.starts.hasNext()
    }

    @Override
    @Throws(NoSuchElementException::class)
    fun nextBarrier(): Map<Object?, Traverser.Admin<S>> {
        val map: Map<Object?, Traverser.Admin<S>> = if (null != barrier) barrier!! else HashMap()
        while (this.starts.hasNext()) {
            val traverser: Traverser.Admin<S> = this.starts.next()
            val `object`: Object?
            var productive: Boolean
            if (null != dedupLabels) {
                `object` = ArrayList(dedupLabels.size())
                for (label in dedupLabels) {
                    val product: TraversalProduct =
                        TraversalUtil.produce(this.getSafeScopeValue(Pop.last, label, traverser) as S, dedupTraversal)
                    if (!product.isProductive()) break
                    (`object` as List?).add(product.get())
                }
                productive = (`object` as List?)!!.size() === this.dedupLabels.size()
            } else {
                val product: TraversalProduct = TraversalUtil.produce(traverser, dedupTraversal)
                productive = product.isProductive()
                `object` = if (productive) product.get() else null
            }
            if (productive) {
                if (!map.containsKey(`object`)) {
                    traverser.setBulk(1L)

                    // DetachedProperty and DetachedVertexProperty both have a transient for the Host element. that causes
                    // trouble for olap which ends up requiring the Host later. can't change the transient without some
                    // consequences: (1) we break gryo formatting and io tests start failing (2) storing the element with
                    // the property has the potential to bloat detached Element instances as it basically stores that data
                    // twice. Not sure if it's smart to change that at least in 3.4.x and not without some considerable
                    // thought as to what might be major changes. To work around the problem we will detach properties as
                    // references so that the parent element goes with it. Also, given TINKERPOP-2318 property comparisons
                    // have changed in such a way that allows this to work properly
                    if (onGraphComputer) {
                        if (traverser.get() is Property) traverser.set(ReferenceFactory.detach(traverser.get())) else traverser.set(
                            DetachedFactory.detach(traverser.get(), true)
                        )
                    } else {
                        traverser.set(traverser.get())
                    }
                    map.put(`object`, traverser)
                }
            }
        }
        barrier = null
        barrierIterator = null
        return map
    }

    @Override
    fun addBarrier(barrier: Map<Object?, Traverser.Admin<S>?>?) {
        if (null == this.barrier) this.barrier = HashMap(barrier) else this.barrier.putAll(barrier)
    }

    @get:Override
    val memoryComputeKey: MemoryComputeKey<Map<Object, Traverser.Admin<S>>>
        get() = MemoryComputeKey.of(this.getId(), Operator.addAll as BinaryOperator, false, true)

    @Override
    fun setKeepLabels(keepLabels: Set<String?>?) {
        this.keepLabels = HashSet(keepLabels)
    }

    @Override
    fun getKeepLabels(): Set<String>? {
        return keepLabels
    }
}