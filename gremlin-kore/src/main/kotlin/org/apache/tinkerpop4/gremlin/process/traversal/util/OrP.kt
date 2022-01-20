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
class OrP<V>(predicates: List<P<V>?>) : ConnectiveP<V>(predicates) {
    init {
        for (p in predicates) {
            or(p)
        }
        this.biPredicate = OrBiPredicate(this)
    }

    @Override
    fun or(predicate: Predicate<in V>): P<V> {
        if (predicate !is P) throw IllegalArgumentException("Only P predicates can be or'd together") else if (predicate is OrP<*>) this.predicates.addAll(
            (predicate as OrP<*>).getPredicates()
        ) else this.predicates.add(predicate as P<V>)
        return this
    }

    @Override
    fun negate(): P<V> {
        super.negate()
        return AndP(this.predicates)
    }

    @Override
    override fun toString(): String {
        return "or(" + StringFactory.removeEndBrackets(this.predicates).toString() + ")"
    }

    @Override
    fun clone(): OrP<V> {
        val clone = super.clone() as OrP<V>
        clone.biPredicate = OrBiPredicate(clone)
        return clone
    }

    private inner class OrBiPredicate(private val orP: OrP<V>) : BiPredicate<V, V>, Serializable {
        @Override
        fun test(valueA: V, valueB: V): Boolean {
            for (predicate in orP.predicates) {
                if (predicate.test(valueA)) return true
            }
            return false
        }
    }
}