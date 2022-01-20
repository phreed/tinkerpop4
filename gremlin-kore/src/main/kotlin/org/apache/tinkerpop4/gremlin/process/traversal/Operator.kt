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
import java.util.Collection
import java.util.Map
import java.util.function.BinaryOperator

/**
 * A set of [BinaryOperator] instances that handle common operations for traversal steps.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
enum class Operator : BinaryOperator<Object?> {
    /**
     * An addition function.
     *
     * @see NumberHelper.add
     * @since 3.0.0-incubating
     */
    sum {
        fun apply(a: Object?, b: Object?): Object {
            return NumberHelper.add(a as Number?, b as Number?)
        }
    },

    /**
     * A subtraction function.
     *
     * @see NumberHelper.sub
     * @since 3.0.0-incubating
     */
    minus {
        fun apply(a: Object?, b: Object?): Object {
            return NumberHelper.sub(a as Number?, b as Number?)
        }
    },

    /**
     * A multiplication function.
     *
     * @see NumberHelper.mul
     * @since 3.0.0-incubating
     */
    mult {
        fun apply(a: Object?, b: Object?): Object {
            return NumberHelper.mul(a as Number?, b as Number?)
        }
    },

    /**
     * A division function.
     *
     * @see NumberHelper.div
     * @since 3.0.0-incubating
     */
    div {
        fun apply(a: Object?, b: Object?): Object {
            return NumberHelper.div(a as Number?, b as Number?)
        }
    },

    /**
     * Selects the smaller of the values.
     *
     * @see NumberHelper.min
     * @since 3.0.0-incubating
     */
    min {
        fun apply(a: Object?, b: Object?): Object {
            return NumberHelper.min(a as Comparable?, b as Comparable?)
        }
    },

    /**
     * Selects the larger of the values.
     *
     * @see NumberHelper.max
     * @since 3.0.0-incubating
     */
    max {
        fun apply(a: Object?, b: Object?): Object {
            return NumberHelper.max(a as Comparable?, b as Comparable?)
        }
    },

    /**
     * The new incoming value (i.e. the second value to the function) is returned unchanged result in the assignment
     * of that value to the object of the `Operator`.
     *
     * @since 3.1.0-incubating
     */
    assign {
        fun apply(a: Object?, b: Object): Object {
            return b
        }
    },

    /**
     * Applies "and" to boolean values.
     *
     * <pre>
     * a = true, b = null -> true
     * a = false, b = null -> false
     * a = null, b = true -> true
     * a = null, b = false -> false
     * a = null, b = null -> null
    </pre> *
     *
     * @since 3.2.0-incubating
     */
    and {
        fun apply(a: Object?, b: Object?): Object? {
            return if (null == a || null == b) {
                if (null == a && null == b) null else if (null == b) a else b
            } else a && b
        }
    },

    /**
     * Applies "or" to boolean values.
     *
     * <pre>
     * a = true, b = null -> true
     * a = false, b = null -> false
     * a = null, b = true -> true
     * a = null, b = false -> false
     * a = null, b = null -> null
    </pre> *
     *
     * @since 3.2.0-incubating
     */
    or {
        fun apply(a: Object?, b: Object?): Object? {
            return if (null == a || null == b) {
                if (null == a && null == b) null else if (null == b) a else b
            } else a || b
        }
    },

    /**
     * Takes all objects in the second `Collection` and adds them to the first. If the first is `null`,
     * then the second `Collection` is returned and if the second is `null` then the first is returned.
     * If both are `null` then `null` is returned. Arguments must be of type `Map` or
     * `Collection`.
     *
     *
     * The semantics described above for `Collection` are the same when applied to a `Map`.
     *
     * @since 3.2.0-incubating
     */
    addAll {
        fun apply(a: Object?, b: Object?): Object? {
            if (null == a || null == b) {
                return if (null == a && null == b) null else if (null == b) a else b
            }
            if (a is Map && b is Map) (a as Map<*, *>).putAll(b as Map?) else if (a is Collection && a is Collection) (a as Collection<*>).addAll(
                b as Collection?
            ) else throw IllegalArgumentException(
                String.format(
                    "Objects must be both of Map or Collection: a=%s b=%s",
                    a.getClass().getSimpleName(), b.getClass().getSimpleName()
                )
            )
            return a
        }
    },

    /**
     * Sums and adds long values.
     *
     * @since 3.2.0-incubating
     */
    sumLong {
        fun apply(a: Object, b: Object): Object {
            return a as Long + b as Long
        }
    }
}