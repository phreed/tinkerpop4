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
package org.apache.tinkerpop4.gremlin.process.traversal.util

import org.apache.tinkerpop4.gremlin.process.traversal.P

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class AndP<V>(predicates: List<P<V>?>) : ConnectiveP<V>(predicates) {
    init {
        for (p in predicates) {
            and(p)
        }
        this.biPredicate = AndBiPredicate(this)
    }

    @Override
    fun and(predicate: Predicate<in V>): P<V> {
        if (predicate !is P) throw IllegalArgumentException("Only P predicates can be and'd together") else if (predicate is AndP<*>) this.predicates.addAll(
            (predicate as AndP<*>).getPredicates()
        ) else this.predicates.add(predicate as P<V>)
        return this
    }

    @Override
    fun negate(): P<V> {
        super.negate()
        return OrP(this.predicates)
    }

    @Override
    override fun toString(): String {
        return "and(" + StringFactory.removeEndBrackets(this.predicates).toString() + ")"
    }

    @Override
    fun clone(): AndP<V> {
        val clone = super.clone() as AndP<V>
        clone.biPredicate = AndBiPredicate(clone)
        return clone
    }

    private inner class AndBiPredicate(private val andP: AndP<V>) : BiPredicate<V, V>, Serializable {
        @Override
        fun test(valueA: V, valueB: V): Boolean {
            for (predicate in andP.predicates) {
                if (!predicate.test(valueA)) return false
            }
            return true
        }
    }
}