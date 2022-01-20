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
package org.apache.tinkerpop4.gremlin.process.traversal.step.map

import org.apache.tinkerpop4.gremlin.util.NumberHelper

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Daniel Kuppitz (http://gremlin.guru)
 */
class MeanGlobalStep<S : Number?, E : Number?>(traversal: Traversal.Admin?) : ReducingBarrierStep<S, E>(traversal) {
    init {
        this.setReducingBiOperator(MeanGlobalBiOperator.INSTANCE)
    }

    /**
     * Advances the starts until a non-null value is found or simply returns `null`. In this way, an all
     * `null` stream will result in `null`.
     */
    @Override
    protected fun generateSeedFromStarts(): E? {
        var e: E? = null
        while (starts.hasNext() && null == e) {
            e = projectTraverser(this.starts.next())
        }
        return e
    }

    @Override
    fun processAllStarts() {
        if (this.starts.hasNext()) super.processAllStarts()
    }

    @Override
    fun projectTraverser(traverser: Traverser.Admin<S>): E? {
        return if (null == traverser.get()) null else MeanNumber(traverser.get(), traverser.bulk()) as E
    }

    @get:Override
    val requirements: Set<Any>
        get() = REQUIREMENTS

    @Override
    fun generateFinalResult(meanNumber: E?): E? {
        // if the meanNumber is null it means the whole stream was null
        return if (null == meanNumber) null as E? else (meanNumber as MeanNumber).final as E
    }

    /////
    class MeanGlobalBiOperator<S : Number?> : BinaryOperator<S>, Serializable {
        @Override
        fun apply(mutatingSeed: S?, number: S?): S? {
            if (null == mutatingSeed || null == number) return mutatingSeed
            return if (mutatingSeed is MeanNumber) {
                if (number is MeanNumber) (mutatingSeed as MeanNumber).add(number as MeanNumber) as S else (mutatingSeed as MeanNumber).add(
                    number,
                    1L
                ) as S
            } else {
                if (number is MeanNumber) (number as MeanNumber).add(mutatingSeed, 1L) as S else MeanNumber(
                    number,
                    1L
                ).add(
                    mutatingSeed,
                    1L
                ) as S
            }
        }

        companion object {
            val INSTANCE: MeanGlobalBiOperator<*> = MeanGlobalBiOperator<Any?>()
        }
    }

    class MeanNumber @JvmOverloads constructor(number: Number? = 0, private var count: Long = 0) : Number(),
        Comparable<Number?> {
        private var sum: Number

        init {
            sum = mul(number, count)
        }

        fun add(amount: Number?, count: Long): MeanNumber {
            this.count += count
            sum = NumberHelper.add(sum, mul(amount, count))
            return this
        }

        fun add(other: MeanNumber): MeanNumber {
            count += other.count
            sum = NumberHelper.add(sum, other.sum)
            return this
        }

        @Override
        fun intValue(): Int {
            return div(sum, count).intValue()
        }

        @Override
        fun longValue(): Long {
            return div(sum, count).longValue()
        }

        @Override
        fun floatValue(): Float {
            return div(sum, count, true).floatValue()
        }

        @Override
        fun doubleValue(): Double {
            return div(sum, count, true).doubleValue()
        }

        @Override
        override fun toString(): String {
            return final.toString()
        }

        @Override
        operator fun compareTo(number: Number): Int {
            // TODO: NumberHelper should provide a compareTo() implementation
            return Double.valueOf(doubleValue()).compareTo(number.doubleValue())
        }

        @Override
        override fun equals(`object`: Object): Boolean {
            return `object` is Number && Double.valueOf(doubleValue()).equals((`object` as Number).doubleValue())
        }

        @Override
        override fun hashCode(): Int {
            return Double.valueOf(doubleValue()).hashCode()
        }

        val final: Number
            get() = div(sum, count, true)
    }

    companion object {
        private val REQUIREMENTS: Set<TraverserRequirement> =
            EnumSet.of(TraverserRequirement.OBJECT, TraverserRequirement.BULK)
    }
}