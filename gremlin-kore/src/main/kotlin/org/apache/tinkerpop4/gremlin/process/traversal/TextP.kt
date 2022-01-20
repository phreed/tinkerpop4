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
 * Predefined `Predicate` values that can be used as `String` filters.
 *
 * @author Daniel Kuppitz (http://gremlin.guru)
 */
class TextP @SuppressWarnings("WeakerAccess") constructor(biPredicate: BiPredicate<String?, String?>?, value: String?) :
    P<String?>(biPredicate, value) {
    @Override
    override fun equals(other: Object?): Boolean {
        return other is TextP && super.equals(other)
    }

    @Override
    override fun toString(): String {
        return if (null == this.originalValue) this.biPredicate.toString() else this.biPredicate.toString() + "(" + this.originalValue + ")"
    }

    @Override
    fun negate(): TextP {
        return TextP(this.biPredicate.negate(), this.originalValue)
    }

    fun clone(): TextP {
        return super.clone() as TextP
    }

    companion object {
        //////////////// statics
        /**
         * Determines if String does start with the given value.
         *
         * @since 3.4.0
         */
        fun startingWith(value: String?): TextP {
            return TextP(Text.startingWith, value)
        }

        /**
         * Determines if String does not start with the given value.
         *
         * @since 3.4.0
         */
        fun notStartingWith(value: String?): TextP {
            return TextP(Text.notStartingWith, value)
        }

        /**
         * Determines if String does start with the given value.
         *
         * @since 3.4.0
         */
        fun endingWith(value: String?): TextP {
            return TextP(Text.endingWith, value)
        }

        /**
         * Determines if String does not start with the given value.
         *
         * @since 3.4.0
         */
        fun notEndingWith(value: String?): TextP {
            return TextP(Text.notEndingWith, value)
        }

        /**
         * Determines if String does contain the given value.
         *
         * @since 3.4.0
         */
        fun containing(value: String?): TextP {
            return TextP(Text.containing, value)
        }

        /**
         * Determines if String does not contain the given value.
         *
         * @since 3.4.0
         */
        fun notContaining(value: String?): TextP {
            return TextP(Text.notContaining, value)
        }
    }
}