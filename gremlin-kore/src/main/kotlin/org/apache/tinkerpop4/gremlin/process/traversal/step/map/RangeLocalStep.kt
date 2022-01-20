/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.tinkerpop4.gremlin.process.traversal.step.map

import org.apache.tinkerpop4.gremlin.process.traversal.Traversal

/**
 * @author Daniel Kuppitz (http://gremlin.guru)
 */
class RangeLocalStep<S>(traversal: Traversal.Admin?, low: Long, high: Long) : ScalarMapStep<S, S>(traversal) {
    private val low: Long
    private val high: Long

    init {
        if (low != -1L && high != -1L && low > high) {
            throw IllegalArgumentException("Not a legal range: [$low, $high]")
        }
        this.low = low
        this.high = high
    }

    @Override
    protected fun map(traverser: Traverser.Admin<S>): S {
        val start: S = traverser.get()
        return applyRange(start, low, high)
    }

    @Override
    override fun toString(): String {
        return StringFactory.stepString(this, low, high)
    }

    @Override
    override fun hashCode(): Int {
        return super.hashCode() xor Long.hashCode(high) xor Long.hashCode(low)
    }

    @get:Override
    val requirements: Set<Any>
        get() = Collections.singleton(TraverserRequirement.OBJECT)

    companion object {
        /**
         * Extracts the specified range of elements from a collection.
         * Return type depends on dynamic type of start.
         *
         *  *
         * Map becomes Map (order-preserving)
         *
         *  *
         * Set becomes Set (order-preserving)
         *
         *  *
         * Other Collection types become List
         *
         *
         */
        fun <S> applyRange(start: S, low: Long, high: Long): S {
            if (start is Map) {
                return applyRangeMap(start as Map, low, high)
            } else if (start is Iterable) {
                return applyRangeIterable(start as Iterable, low, high)
            }
            return start
        }

        /**
         * Extracts specified range of elements from a Map.
         */
        private fun applyRangeMap(map: Map, low: Long, high: Long): Map {
            val capacity = (if (high != -1L) high else map.size()) - low
            val result: Map = LinkedHashMap(Math.min(capacity, map.size()) as Int)
            var c = 0L
            for (obj in map.entrySet()) {
                val entry: Map.Entry = obj
                if (c >= low) {
                    if (c < high || high == -1L) {
                        result.put(entry.getKey(), entry.getValue())
                    } else break
                }
                c++
            }
            return result
        }

        /**
         * Extracts specified range of elements from a Collection.
         */
        private fun applyRangeIterable(iterable: Iterable<Object>, low: Long, high: Long): Object {
            // See if we only want a single item.  It is also possible that we will allow more than one item, but that the
            // incoming container is only capable of producing a single item.  In that case, we will still emit a
            // container.  This allows the result type to be predictable based on the step arguments.  It also allows us to
            // avoid creating the result container for the single case.
            val single = if (high != -1L) high - low == 1L else false
            val resultCollection: Collection? =
                if (single) null else if (iterable is Set) LinkedHashSet() else LinkedList()
            var result: Object? = if (single) null else resultCollection
            var c = 0L
            for (item in iterable) {
                if (c >= low) {
                    if (c < high || high == -1L) {
                        if (single) {
                            result = item
                            break
                        } else {
                            resultCollection.add(item)
                        }
                    } else break
                }
                c++
            }
            if (null == result) throw FastNoSuchElementException.instance()
            return result
        }
    }
}