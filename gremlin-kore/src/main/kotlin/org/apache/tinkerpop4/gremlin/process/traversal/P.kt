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
package org.apache.tinkerpop4.gremlin.process.traversal

import org.apache.tinkerpop4.gremlin.process.traversal.util.AndP
import org.apache.tinkerpop4.gremlin.process.traversal.util.OrP
import java.io.Serializable
import java.util.Arrays
import java.util.Collection
import java.util.function.BiPredicate
import java.util.function.Predicate

/**
 * Predefined `Predicate` values that can be used with
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class P<V>(
    biPredicate: BiPredicate<V, V>,
    /**
     * Gets the current value to be passed to the predicate for testing.
     */
    var value: V
) : Predicate<V>, Serializable, Cloneable {
    protected var biPredicate: BiPredicate<V, V>

    /**
     * Gets the original value used at time of construction of the `P`. This value can change its type
     * in some cases.
     */
    var originalValue: V
        protected set

    init {
        originalValue = value
        this.biPredicate = biPredicate
    }

    fun getBiPredicate(): BiPredicate<V, V> {
        return biPredicate
    }

    @Override
    fun test(testValue: V): Boolean {
        return biPredicate.test(testValue, value)
    }

    @Override
    override fun hashCode(): Int {
        var result: Int = biPredicate.hashCode()
        if (null != originalValue) result = result xor originalValue!!.hashCode()
        return result
    }

    @Override
    override fun equals(other: Object): Boolean {
        return other is P<*> &&
                (other as P<*>).getClass().equals(this.getClass()) &&
                (other as P<*>).getBiPredicate().equals(biPredicate) &&
                ((other as P<*>).originalValue == null && originalValue == null || (other as P<*>).originalValue!!.equals(
                    originalValue
                ))
    }

    @Override
    override fun toString(): String {
        return if (null == originalValue) biPredicate.toString() else biPredicate.toString() + "(" + originalValue + ")"
    }

    @Override
    fun negate(): P<V> {
        return P(biPredicate.negate(), originalValue)
    }

    @Override
    fun and(predicate: Predicate<in V>?): P<V> {
        if (predicate !is P<*>) throw IllegalArgumentException("Only P predicates can be and'd together")
        return AndP(Arrays.asList(this, predicate as P<V>?))
    }

    @Override
    fun or(predicate: Predicate<in V>?): P<V> {
        if (predicate !is P<*>) throw IllegalArgumentException("Only P predicates can be or'd together")
        return OrP(Arrays.asList(this, predicate as P<V>?))
    }

    fun clone(): P<V> {
        return try {
            super.clone() as P<V>
        } catch (e: CloneNotSupportedException) {
            throw IllegalStateException(e.getMessage(), e)
        }
    }

    companion object {
        //////////////// statics
        /**
         * Determines if values are equal.
         *
         * @since 3.0.0-incubating
         */
        fun <V> eq(value: V): P<V?> {
            return P<Any?>(Compare.eq, value)
        }

        /**
         * Determines if values are not equal.
         *
         * @since 3.0.0-incubating
         */
        fun <V> neq(value: V): P<V?> {
            return P<Any?>(Compare.neq, value)
        }

        /**
         * Determines if a value is less than another.
         *
         * @since 3.0.0-incubating
         */
        fun <V> lt(value: V): P<V?> {
            return P<Any?>(Compare.lt, value)
        }

        /**
         * Determines if a value is less than or equal to another.
         *
         * @since 3.0.0-incubating
         */
        fun <V> lte(value: V): P<V?> {
            return P<Any?>(Compare.lte, value)
        }

        /**
         * Determines if a value is greater than another.
         *
         * @since 3.0.0-incubating
         */
        fun <V> gt(value: V): P<V?> {
            return P<Any?>(Compare.gt, value)
        }

        /**
         * Determines if a value is greater than or equal to another.
         *
         * @since 3.0.0-incubating
         */
        fun <V> gte(value: V): P<V?> {
            return P<Any?>(Compare.gte, value)
        }

        /**
         * Determines if a value is within (exclusive) the range of the two specified values.
         *
         * @since 3.0.0-incubating
         */
        fun <V> inside(first: V, second: V): P<V> {
            return AndP<V>(Arrays.asList(P<Any?>(Compare.gt, first), P<Any?>(Compare.lt, second)))
        }

        /**
         * Determines if a value is not within (exclusive) of the range of the two specified values.
         *
         * @since 3.0.0-incubating
         */
        fun <V> outside(first: V, second: V): P<V> {
            return OrP<V>(Arrays.asList(P<Any?>(Compare.lt, first), P<Any?>(Compare.gt, second)))
        }

        /**
         * Determines if a value is within (inclusive) of the range of the two specified values.
         *
         * @since 3.0.0-incubating
         */
        fun <V> between(first: V, second: V): P<V> {
            return AndP<V>(Arrays.asList(P<Any?>(Compare.gte, first), P<Any?>(Compare.lt, second)))
        }

        /**
         * Determines if a value is within the specified list of values. If the array of arguments itself is `null`
         * then the argument is treated as `Object[1]` where that single value is `null`.
         *
         * @since 3.0.0-incubating
         */
        fun <V> within(vararg values: V): P<V> {
            val v: Array<V?> = values ?: arrayOf(null) as Array<V?>
            return within(Arrays.asList(v))
        }

        /**
         * Determines if a value is within the specified list of values. Calling this with `null` is the same as
         * calling [.within] using `null`.
         *
         * @since 3.0.0-incubating
         */
        fun <V> within(value: Collection<V>?): P<V?> {
            return if (null == value) within(null as V?) else P<Any?>(
                Contains.within,
                value
            )
        }

        /**
         * Determines if a value is not within the specified list of values. If the array of arguments itself is `null`
         * then the argument is treated as `Object[1]` where that single value is `null`.
         *
         * @since 3.0.0-incubating
         */
        fun <V> without(vararg values: V): P<V> {
            val v: Array<V?> = values ?: arrayOf(null) as Array<V?>
            return without(Arrays.asList(v))
        }

        /**
         * Determines if a value is not within the specified list of values. Calling this with `null` is the same as
         * calling [.within] using `null`.
         *
         * @since 3.0.0-incubating
         */
        fun <V> without(value: Collection<V>?): P<V?> {
            return if (null == value) without(null as V?) else P<Any?>(
                Contains.without,
                value
            )
        }

        /**
         * Construct an instance of `P` from a `BiPredicate`.
         *
         * @since 3.0.0-incubating
         */
        fun test(biPredicate: BiPredicate, value: Object?): P<*> {
            return P<Any?>(biPredicate, value)
        }

        /**
         * The opposite of the specified `P`.
         *
         * @since 3.0.0-incubating
         */
        fun <V> not(predicate: P<V>): P<V> {
            return predicate.negate()
        }
    }
}