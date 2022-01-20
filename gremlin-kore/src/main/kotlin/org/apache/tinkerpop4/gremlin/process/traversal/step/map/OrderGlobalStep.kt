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

import org.apache.tinkerpop4.gremlin.process.computer.MemoryComputeKey

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class OrderGlobalStep<S, C : Comparable?>(traversal: Traversal.Admin?) : CollectingBarrierStep<S>(traversal),
    ComparatorHolder<S, C>, TraversalParent, ByModulating, Seedable {
    private var comparators: List<Pair<Traversal.Admin<S, C>, Comparator<C>>> = ArrayList()
    private var multiComparator: MultiComparator<C>? = null
    var limit = Long.MAX_VALUE
    private val random: Random = Random()
    @Override
    fun resetSeed(seed: Long) {
        random.setSeed(seed)
    }

    @Override
    fun barrierConsumer(traverserSet: TraverserSet<S>) {
        if (null == multiComparator) multiComparator = createMultiComparator()
        //
        if (multiComparator.isShuffle()) traverserSet.shuffle(random) else traverserSet.sort(multiComparator as Comparator?)
    }

    @Override
    fun processAllStarts() {
        while (this.starts.hasNext()) {
            // only add the traverser if the comparator traversal was productive
            createProjectedTraverser(this.starts.next()).ifPresent(traverserSet::add)
        }
    }

    @Override
    fun addComparator(traversal: Traversal.Admin<S, C>?, comparator: Comparator<C>?) {
        comparators.add(Pair(this.integrateChild(traversal), comparator))
    }

    @Override
    fun modulateBy(traversal: Traversal.Admin<*, *>?) {
        this.modulateBy(traversal, Order.asc)
    }

    @Override
    fun modulateBy(traversal: Traversal.Admin<*, *>?, comparator: Comparator?) {
        addComparator(traversal as Traversal.Admin<S, C>?, comparator)
    }

    @Override
    fun replaceLocalChild(oldTraversal: Traversal.Admin<*, *>?, newTraversal: Traversal.Admin<*, *>?) {
        var i = 0
        for (pair in comparators) {
            val traversal: Traversal.Admin<S, C> = pair.getValue0()
            if (null != traversal && traversal.equals(oldTraversal)) {
                comparators.set(i, Pair.with(this.integrateChild(newTraversal), pair.getValue1()))
                break
            }
            i++
        }
    }

    @Override
    fun getComparators(): List<Pair<Traversal.Admin<S, C>, Comparator<C>>> {
        return if (comparators.isEmpty()) Collections.singletonList(
            Pair(
                IdentityTraversal(),
                Order.asc as Comparator
            )
        ) else Collections.unmodifiableList(
            comparators
        )
    }

    @Override
    override fun toString(): String {
        return StringFactory.stepString(this, comparators)
    }

    @Override
    override fun hashCode(): Int {
        var result = super.hashCode()
        for (i in 0 until comparators.size()) {
            result = result xor comparators[i].hashCode() * (i + 1)
        }
        return result
    }

    @get:Override
    val requirements: Set<Any>
        get() = this.getSelfAndChildRequirements(TraverserRequirement.BULK, TraverserRequirement.OBJECT)

    @get:Override
    val localChildren: List<Any>
        get() = comparators.stream().map(Pair::getValue0).collect(Collectors.toList())

    @Override
    fun clone(): OrderGlobalStep<S, C> {
        val clone = super.clone() as OrderGlobalStep<S, C>
        clone.comparators = ArrayList()
        for (comparator in comparators) {
            clone.comparators.add(Pair(comparator.getValue0().clone(), comparator.getValue1()))
        }
        return clone
    }

    @Override
    fun setTraversal(parentTraversal: Traversal.Admin<*, *>?) {
        super.setTraversal(parentTraversal)
        comparators.stream().map(Pair::getValue0).forEach(super@TraversalParent::integrateChild)
    }

    @get:Override
    val memoryComputeKey: MemoryComputeKey<TraverserSet<S>>
        get() {
            if (null == multiComparator) multiComparator = createMultiComparator()
            return MemoryComputeKey.of(this.getId(), OrderBiOperator<S>(limit, multiComparator, random), false, true)
        }

    private fun createProjectedTraverser(traverser: Traverser.Admin<S>): Optional<ProjectedTraverser<S, Object>> {
        // this was ProjectedTraverser<S, C> but the projection may not be C in the case of a lambda where a
        // Comparable may not be expected but rather an object that can be compared in any way given a lambda.
        // not sure why this is suddenly an issue but Intellij would not let certain tests pass without this
        // adjustment here.
        val projections: List<Object> = ArrayList(comparators.size())
        for (pair in comparators) {
            val product: TraversalProduct = TraversalUtil.produce(traverser, pair.getValue0())
            if (!product.isProductive()) break
            projections.add(product.get())
        }

        // if a traversal wasn't productive then the sizes wont match and it will filter
        return if (projections.size() === comparators.size()) Optional.of(
            ProjectedTraverser(
                traverser,
                projections
            )
        ) else Optional.empty()
    }

    private fun createMultiComparator(): MultiComparator<C> {
        val list: List<Comparator<C>> = ArrayList(comparators.size())
        for (pair in comparators) {
            list.add(pair.getValue1())
        }
        return MultiComparator(list)
    }

    ////////////////
    class OrderBiOperator<S> : BinaryOperator<TraverserSet<S>?>, Serializable {
        private var limit: Long = 0
        private var comparator: MultiComparator? = null
        private var random: Random? = null

        private constructor() {
            // for serializers that need a no-arg constructor
        }

        constructor(limit: Long, multiComparator: MultiComparator?, random: Random?) {
            this.limit = limit
            comparator = multiComparator
            this.random = random
        }

        @Override
        fun apply(setA: TraverserSet<S>, setB: TraverserSet<S>?): TraverserSet<S> {
            setA.addAll(setB)
            if (limit != -1L && setA.bulkSize() > limit) {
                if (comparator.isShuffle()) setA.shuffle(random) else setA.sort(comparator)
                var counter = 0L
                val traversers: Iterator<Traverser.Admin<S>> = setA.iterator()
                while (traversers.hasNext()) {
                    val traverser: Traverser.Admin<S> = traversers.next()
                    if (counter > limit) traversers.remove()
                    counter = counter + traverser.bulk()
                }
            }
            return setA
        }
    }
}