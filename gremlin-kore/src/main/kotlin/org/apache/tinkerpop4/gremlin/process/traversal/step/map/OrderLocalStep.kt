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

import org.apache.tinkerpop4.gremlin.process.traversal.Order

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class OrderLocalStep<S, C : Comparable?>(traversal: Traversal.Admin?) : ScalarMapStep<S, S>(traversal),
    ComparatorHolder<S, C>, ByModulating, TraversalParent, Seedable {
    private var comparators: List<Pair<Traversal.Admin<S, C>, Comparator<C>>> = ArrayList()
    private val random: Random = Random()
    @Override
    fun resetSeed(seed: Long) {
        random.setSeed(seed)
    }

    @Override
    protected fun map(traverser: Traverser.Admin<S>): S {
        val start: S = traverser.get()

        // modulate each traverser object and keep its value to sort on as Pair<traverser-object,List<modulated-object>>
        // as of 3.6.0 this transformation occurs in place and values held in memory so that unproductive by() values
        // can be filtered. without this trade-off for more memory it would be necessary to apply the modulation twice
        // once for the filter and once for the compare.
        if (start is Collection) {
            val original = start as Collection<S>
            val filteredAndModulated: List<Pair<S, List<C>>> = filterAndModulate(original)
            return filteredAndModulated.stream().map(Pair::getValue0).collect(Collectors.toList())
        } else if (start is Map) {
            val filteredAndModulated: List<Pair<S, List<C>>> = filterAndModulate((start as Map).entrySet())
            val sortedMap = LinkedHashMap()
            filteredAndModulated.stream().map(Pair::getValue0).map { entry -> entry }
                .forEach { entry -> sortedMap.put(entry.getKey(), entry.getValue()) }
            return sortedMap
        }
        return start
    }

    /**
     * Take the collection and apply modulators removing traversers that have modulators that aren't productive.
     */
    private fun filterAndModulate(original: Collection<S>): List<Pair<S, List<C>>> {
        if (comparators.isEmpty()) comparators.add(Pair(IdentityTraversal(), Order.asc as Comparator))

        // detect shuffle and optimize by either ignoring other comparators if shuffle is last or
        // ignoring comparators that are shuffles if they are in the middle
        val isShuffle = comparators[comparators.size() - 1].getValue1() as Comparator === Order.shuffle
        val relevantComparators: List<Pair<Traversal.Admin<S, C>, Comparator<C>>> =
            if (isShuffle) comparators else comparators.stream()
                .filter { p -> p.getValue1() as Comparator !== Order.shuffle }
                .collect(Collectors.toList())
        val filteredAndModulated: List<Pair<S, List<C>>> = ArrayList()
        val modulators: List<Traversal.Admin<S, C>> =
            relevantComparators.stream().map(Pair::getValue0).collect(Collectors.toList())
        for (s in original) {
            // filter out unproductive by()
            val modulations: List<C> =
                modulators.stream().map { t -> TraversalUtil.produce(s, t) }.filter(TraversalProduct::isProductive)
                    .map { product -> product.get() }
                    .collect(Collectors.toList())

            // when sizes arent the same it means a by() wasn't productive and it is ignored
            if (modulations.size() === modulators.size()) {
                filteredAndModulated.add(Pair.with(s, modulations))
            }
        }
        if (isShuffle) {
            Collections.shuffle(filteredAndModulated, random)
        } else {
            // sort the filter/modulated local list in place using the index of the modulator/comparators
            Collections.sort(filteredAndModulated) { o1, o2 ->
                val modulated1: List<C> = o1.getValue1()
                val modulated2: List<C> = o2.getValue1()
                for (ix in 0 until modulated1.size()) {
                    val comparison: Int = relevantComparators[ix].getValue1().compare(modulated1[ix], modulated2[ix])
                    if (comparison != 0) return@sort comparison
                }
                0
            }
        }
        return filteredAndModulated
    }

    @Override
    fun addComparator(traversal: Traversal.Admin<S, C>?, comparator: Comparator<C>?) {
        comparators.add(Pair(this.integrateChild(traversal), comparator))
    }

    @Override
    fun modulateBy(traversal: Traversal.Admin<*, *>?) {
        addComparator(traversal as Traversal.Admin<S, C>?, Order.asc as Comparator)
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
        get() = this.getSelfAndChildRequirements(TraverserRequirement.OBJECT)

    @get:Override
    val localChildren: List<Any>
        get() = comparators.stream().map(Pair::getValue0).collect(Collectors.toList())

    @Override
    fun clone(): OrderLocalStep<S, C> {
        val clone = super.clone() as OrderLocalStep<S, C>
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
}