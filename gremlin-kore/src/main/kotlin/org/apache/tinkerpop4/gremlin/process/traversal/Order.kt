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

import java.util.Comparator
import java.util.Random
import org.apache.tinkerpop4.gremlin.util.NumberHelper

/**
 * Provides `Comparator` instances for ordering traversers.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
enum class Order : Comparator<Object?> {
    /**
     * Order in a random fashion. While this enum implements `Comparator`, the `compare(a,b)` method is not
     * supported as a direct call. This change to the implementation of `compare(a,b)` occurred at 3.5.0 but
     * this implementation was never used directly within the TinkerPop code base.
     *
     * @since 3.0.0-incubating
     */
    shuffle {
        @Override
        override fun compare(first: Object?, second: Object?): Int {
            throw UnsupportedOperationException("Order.shuffle should not be used as an actual Comparator - it is a marker only")
        }

        @Override
        override fun reversed(): Order {
            return shuffle
        }
    },

    /**
     * Order in ascending fashion.
     *
     * @since 3.3.4
     */
    asc {
        private val ascendingComparator: Comparator<Comparable> =
            Comparator.nullsFirst(Comparator.< Comparable > naturalOrder < Comparable ? > ())

        @Override
        override fun compare(first: Object, second: Object): Int {
            // need to convert enum to string representations for comparison or else you can get cast exceptions.
            // this typically happens when sorting local on the keys of maps that contain T
            val f: Object = if (first is Enum<*>) (first as Enum<*>).name() else first
            val s: Object = if (second is Enum<*>) (second as Enum<*>).name() else second
            return if (f is Number && s is Number) NumberHelper.compare(
                f as Number,
                s as Number
            ) else ascendingComparator.compare(f as Comparable, s as Comparable)
        }

        @Override
        override fun reversed(): Order {
            return desc
        }
    },

    /**
     * Order in descending fashion.
     *
     * @since 3.3.4
     */
    desc {
        private val descendingComparator: Comparator<Comparable> =
            Comparator.nullsLast(Comparator.< Comparable > reverseOrder < Comparable ? > ())

        @Override
        override fun compare(first: Object, second: Object): Int {
            // need to convert enum to string representations for comparison or else you can get cast exceptions.
            // this typically happens when sorting local on the keys of maps that contain T
            val f: Object = if (first is Enum<*>) (first as Enum<*>).name() else first
            val s: Object = if (second is Enum<*>) (second as Enum<*>).name() else second
            return if (f is Number && s is Number) NumberHelper.compare(
                s as Number,
                f as Number
            ) else descendingComparator.compare(f as Comparable, s as Comparable)
        }

        @Override
        override fun reversed(): Order {
            return asc
        }
    };

    /**
     * {@inheritDoc}
     */
    abstract fun compare(first: Object?, second: Object?): Int

    /**
     * Produce the opposite representation of the current `Order` enum.
     */
    @Override
    abstract fun reversed(): Order?

    companion object {
        private val RANDOM: Random = Random()
    }
}