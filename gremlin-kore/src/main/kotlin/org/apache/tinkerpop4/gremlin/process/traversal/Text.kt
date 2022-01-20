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

import java.util.function.BiPredicate

/**
 * [Text] is a [java.util.function.BiPredicate] that determines whether the first string starts with, starts
 * not with, ends with, ends not with, contains or does not contain the second string argument.
 *
 * @author Daniel Kuppitz (http://gremlin.guru)
 * @since 3.4.0
 */
enum class Text : BiPredicate<String?, String?> {
    /**
     * Evaluates if the first string starts with the second.
     *
     * @since 3.4.0
     */
    startingWith {
        @Override
        fun test(value: String, prefix: String?): Boolean {
            return value.startsWith(prefix)
        }

        /**
         * The negative of `startsWith` is [.notStartingWith].
         */
        @Override
        override fun negate(): Text {
            return notStartingWith
        }
    },

    /**
     * Evaluates if the first string does not start with the second.
     *
     * @since 3.4.0
     */
    notStartingWith {
        @Override
        fun test(value: String?, prefix: String?): Boolean {
            return !startingWith.test(value, prefix)
        }

        /**
         * The negative of `startsNotWith` is [.startingWith].
         */
        @Override
        override fun negate(): Text {
            return startingWith
        }
    },

    /**
     * Evaluates if the first string ends with the second.
     *
     * @since 3.4.0
     */
    endingWith {
        @Override
        fun test(value: String, suffix: String?): Boolean {
            return value.endsWith(suffix)
        }

        /**
         * The negative of `endsWith` is [.notEndingWith].
         */
        @Override
        override fun negate(): Text {
            return notEndingWith
        }
    },

    /**
     * Evaluates if the first string does not end with the second.
     *
     * @since 3.4.0
     */
    notEndingWith {
        @Override
        fun test(value: String?, prefix: String?): Boolean {
            return !endingWith.test(value, prefix)
        }

        /**
         * The negative of `endsNotWith` is [.endingWith].
         */
        @Override
        override fun negate(): Text {
            return endingWith
        }
    },

    /**
     * Evaluates if the first string contains the second.
     *
     * @since 3.4.0
     */
    containing {
        @Override
        fun test(value: String, search: String?): Boolean {
            return value.contains(search)
        }

        /**
         * The negative of `contains` is [.notContaining].
         */
        @Override
        override fun negate(): Text {
            return notContaining
        }
    },

    /**
     * Evaluates if the first string does not contain the second.
     *
     * @since 3.4.0
     */
    notContaining {
        @Override
        fun test(value: String?, search: String?): Boolean {
            return !containing.test(value, search)
        }

        /**
         * The negative of `absent` is [.containing].
         */
        @Override
        override fun negate(): Text {
            return containing
        }
    };

    /**
     * Produce the opposite representation of the current `Text` enum.
     */
    @Override
    abstract fun negate(): Text?
}