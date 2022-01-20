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

import org.apache.tinkerpop4.gremlin.util.NumberHelper

/**
 * `Compare` is a `BiPredicate` that determines whether the first argument is `==`, `!=`,
 * `>`, `>=`, `<`, `<=` to the second argument.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 * @author Matt Frantz (http://github.com/mhfrantz)
 * @author Daniel Kuppitz (http://gemlin.guru)
 */
enum class Compare : BiPredicate<Object?, Object?> {
    /**
     * Evaluates if the first object is equal to the second. If both are of type [Number], [NumberHelper]
     * will be used for the comparison, thus enabling the comparison of only values, ignoring the number types.
     *
     * @since 3.0.0-incubating
     */
    eq {
        @Override
        fun test(first: Object?, second: Object?): Boolean {
            return if (null == first) null == second else if (bothAreNumber(
                    first,
                    second
                )
            ) NumberHelper.compare(first as Number?, second as Number?) === 0 else first.equals(second)
        }

        /**
         * The negative of `eq` is [.neq].
         */
        @Override
        override fun negate(): Compare {
            return neq
        }
    },

    /**
     * Evaluates if the first object is not equal to the second. If both are of type [Number], [NumberHelper]
     * will be used for the comparison, thus enabling the comparison of only values, ignoring the number types.
     *
     * @since 3.0.0-incubating
     */
    neq {
        @Override
        fun test(first: Object?, second: Object?): Boolean {
            return !eq.test(first, second)
        }

        /**
         * The negative of `neq` is [.eq]
         */
        @Override
        override fun negate(): Compare {
            return eq
        }
    },

    /**
     * Evaluates if the first object is greater than the second. If both are of type [Number], [NumberHelper]
     * will be used for the comparison, thus enabling the comparison of only values, ignoring the number types.
     *
     * @since 3.0.0-incubating
     */
    gt {
        @Override
        fun test(first: Object?, second: Object?): Boolean {
            if (null != first && null != second) {
                if (bothAreNumber(first, second)) {
                    return NumberHelper.compare(first as Number?, second as Number?) > 0
                }
                if (bothAreComparableAndOfSameType(first, second)) {
                    return (first as Comparable).compareTo(second) > 0
                }
                throwException(first, second)
            }
            return false
        }

        /**
         * The negative of `gt` is [.lte].
         */
        @Override
        override fun negate(): Compare {
            return lte
        }
    },

    /**
     * Evaluates if the first object is greater-equal to the second. If both are of type [Number], [NumberHelper]
     * will be used for the comparison, thus enabling the comparison of only values, ignoring the number types.
     *
     * @since 3.0.0-incubating
     */
    gte {
        @Override
        fun test(first: Object?, second: Object?): Boolean {
            return if (null == first) null == second else null != second && !lt.test(first, second)
        }

        /**
         * The negative of `gte` is [.lt].
         */
        @Override
        override fun negate(): Compare {
            return lt
        }
    },

    /**
     * Evaluates if the first object is less than the second. If both are of type [Number], [NumberHelper]
     * will be used for the comparison, thus enabling the comparison of only values, ignoring the number types.
     *
     * @since 3.0.0-incubating
     */
    lt {
        @Override
        fun test(first: Object?, second: Object?): Boolean {
            if (null != first && null != second) {
                if (bothAreNumber(first, second)) {
                    return NumberHelper.compare(first as Number?, second as Number?) < 0
                }
                if (bothAreComparableAndOfSameType(first, second)) {
                    return (first as Comparable).compareTo(second) < 0
                }
                throwException(first, second)
            }
            return false
        }

        /**
         * The negative of `lt` is [.gte].
         */
        @Override
        override fun negate(): Compare {
            return gte
        }
    },

    /**
     * Evaluates if the first object is less-equal to the second. If both are of type [Number], [NumberHelper]
     * will be used for the comparison, thus enabling the comparison of only values, ignoring the number types.
     *
     * @since 3.0.0-incubating
     */
    lte {
        @Override
        fun test(first: Object?, second: Object?): Boolean {
            return if (null == first) null == second else null != second && !gt.test(first, second)
        }

        /**
         * The negative of `lte` is [.gt].
         */
        @Override
        override fun negate(): Compare {
            return gt
        }
    };

    /**
     * Produce the opposite representation of the current `Compare` enum.
     */
    @Override
    abstract fun negate(): Compare?

    companion object {
        private fun bothAreNumber(first: Object, second: Object): Boolean {
            return first is Number && second is Number
        }

        private fun bothAreComparableAndOfSameType(first: Object, second: Object): Boolean {
            return (first is Comparable && second is Comparable
                    && (first.getClass().isInstance(second) || second.getClass().isInstance(first)))
        }

        private fun throwException(first: Object, second: Object) {
            throw IllegalArgumentException(
                String.format(
                    "Cannot compare '%s' (%s) and '%s' (%s) as both need to be an instance of Number or Comparable (and of the same type)",
                    first,
                    first.getClass().getSimpleName(),
                    second,
                    second.getClass().getSimpleName()
                )
            )
        }
    }
}