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
abstract class ConnectiveP<V>(predicates: List<P<V>?>) : P<V>(null, null) {
    protected var predicates: List<P<V>> = ArrayList()

    init {
        if (predicates.size() < 2) throw IllegalArgumentException(
            "The provided " + this.getClass().getSimpleName()
                .toString() + " array must have at least two arguments: " + predicates.size()
        )
    }

    fun getPredicates(): List<P<V>> {
        return Collections.unmodifiableList(predicates)
    }

    @Override
    fun negate(): P<V> {
        val negated: List<P<V>> = ArrayList()
        for (predicate in predicates) {
            negated.add(predicate.negate())
        }
        predicates = negated
        return this
    }

    protected fun negate(p: ConnectiveP<V>): P<V> {
        val negated: List<P<V>> = ArrayList()
        for (predicate in predicates) {
            negated.add(predicate.negate())
        }
        p.predicates = negated
        return p
    }

    @Override
    override fun hashCode(): Int {
        var result = 0
        var i = 0
        for (p in predicates) {
            result = result xor Integer.rotateLeft(p.hashCode(), i++)
        }
        return result
    }

    @Override
    override fun equals(other: Object?): Boolean {
        if (other != null && other.getClass().equals(this.getClass())) {
            val otherPredicates: List<P<V>> = (other as ConnectiveP<V>).predicates
            if (predicates.size() === otherPredicates.size()) {
                for (i in 0 until predicates.size()) {
                    if (!predicates[i].equals(otherPredicates[i])) {
                        return false
                    }
                }
                return true
            }
        }
        return false
    }

    @Override
    fun clone(): ConnectiveP<V> {
        val clone = super.clone() as ConnectiveP<V>
        clone.predicates = ArrayList()
        for (p in predicates) {
            clone.predicates.add(p.clone())
        }
        return clone
    }

    @Override
    fun and(predicate: Predicate<in V>?): P<V> {
        if (predicate !is P) throw IllegalArgumentException("Only P predicates can be and'd together")
        return AndP(Arrays.asList(this, predicate as P<V>?))
    }

    @Override
    fun or(predicate: Predicate<in V>?): P<V> {
        if (predicate !is P) throw IllegalArgumentException("Only P predicates can be or'd together")
        return OrP(Arrays.asList(this, predicate as P<V>?))
    }
}