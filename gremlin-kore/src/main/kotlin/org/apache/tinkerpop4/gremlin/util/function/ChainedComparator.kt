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
package org.apache.tinkerpop4.gremlin.util.function

import org.apache.tinkerpop4.gremlin.process.traversal.Order

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
@Deprecated
@Deprecated("As of release 3.6.0, not replaced.")
class ChainedComparator<S, C : Comparable?>(
    private val traversers: Boolean,
    comparators: List<Pair<Traversal.Admin<S, C>?, Comparator<C>?>?>
) : Comparator<S>, Serializable, Cloneable {
    private var comparators: List<Pair<Traversal.Admin<S, C>, Comparator<C>>> = ArrayList()
    val isShuffle: Boolean

    init {
        if (comparators.isEmpty()) this.comparators.add(
            Pair(
                IdentityTraversal(),
                Order.asc as Comparator
            )
        ) else this.comparators.addAll(comparators)
        isShuffle = this.comparators[this.comparators.size() - 1].getValue1() as Comparator === Order.shuffle
        if (!isShuffle) this.comparators.removeAll(
            this.comparators.stream().filter { pair -> pair.getValue1() as Comparator === Order.shuffle }
                .collect(Collectors.toList()))
    }

    @Override
    fun compare(objectA: S, objectB: S): Int {
        for (pair in comparators) {
            val comparison: Int = if (traversers) pair.getValue1().compare(
                TraversalUtil.apply(objectA as Traverser.Admin<S>, pair.getValue0()),
                TraversalUtil.apply(objectB as Traverser.Admin<S>, pair.getValue0())
            ) else pair.getValue1()
                .compare(TraversalUtil.apply(objectA, pair.getValue0()), TraversalUtil.apply(objectB, pair.getValue0()))
            if (comparison != 0) return comparison
        }
        return 0
    }

    @Override
    fun clone(): ChainedComparator<S, C> {
        return try {
            val clone = super.clone() as ChainedComparator<S, C>
            clone.comparators = ArrayList()
            for (comparator in comparators) {
                clone.comparators.add(Pair(comparator.getValue0().clone(), comparator.getValue1()))
            }
            clone
        } catch (e: CloneNotSupportedException) {
            throw IllegalStateException(e.getMessage(), e)
        }
    }
}