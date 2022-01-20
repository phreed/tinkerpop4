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
package org.apache.tinkerpop4.gremlin.util

import java.math.BigDecimal

/**
 * @author Daniel Kuppitz (http://gremlin.guru)
 */
class NumberHelper private constructor(
    add: BiFunction<Number, Number, Number>,
    sub: BiFunction<Number, Number, Number>,
    mul: BiFunction<Number, Number, Number>,
    div: BiFunction<Number, Number, Number>,
    min: BiFunction<Number, Number, Number>,
    max: BiFunction<Number, Number, Number>,
    cmp: BiFunction<Number, Number, Integer>
) {
    val add: BiFunction<Number, Number, Number>
    val sub: BiFunction<Number, Number, Number>
    val mul: BiFunction<Number, Number, Number>
    val div: BiFunction<Number, Number, Number>
    val min: BiFunction<Number, Number, Number>
    val max: BiFunction<Number, Number, Number>
    val cmp: BiFunction<Number, Number, Integer>

    init {
        this.add = add
        this.sub = sub
        this.mul = mul
        this.div = div
        this.min = min
        this.max = max
        this.cmp = cmp
    }

    companion object {
        val BYTE_NUMBER_HELPER = NumberHelper(
            BiFunction<Number, Number, Number> { a, b -> (a.byteValue() + b.byteValue()) },
            BiFunction<Number, Number, Number> { a, b -> (a.byteValue() - b.byteValue()) },
            BiFunction<Number, Number, Number> { a, b -> (a.byteValue() * b.byteValue()) },
            BiFunction<Number, Number, Number> { a, b -> (a.byteValue() / b.byteValue()) }, label@
            BiFunction<Number, Number, Number> { a, b ->
                if (isNumber(a)) {
                    if (isNumber(b)) {
                        val x: Byte = a.byteValue()
                        val y: Byte = b.byteValue()
                        return@label if (x <= y) x else y
                    }
                    return@label a.byteValue()
                }
                b.byteValue()
            }, label@
            BiFunction<Number, Number, Number> { a, b ->
                if (isNumber(a)) {
                    if (isNumber(b)) {
                        val x: Byte = a.byteValue()
                        val y: Byte = b.byteValue()
                        return@label if (x >= y) x else y
                    }
                    return@label a.byteValue()
                }
                b.byteValue()
            },
            BiFunction<Number, Number, Integer> { a, b -> Byte.compare(a.byteValue(), b.byteValue()) })
        val SHORT_NUMBER_HELPER = NumberHelper(
            BiFunction<Number, Number, Number> { a, b -> (a.shortValue() + b.shortValue()) },
            BiFunction<Number, Number, Number> { a, b -> (a.shortValue() - b.shortValue()) },
            BiFunction<Number, Number, Number> { a, b -> (a.shortValue() * b.shortValue()) },
            BiFunction<Number, Number, Number> { a, b -> (a.shortValue() / b.shortValue()) }, label@
            BiFunction<Number, Number, Number> { a, b ->
                if (isNumber(a)) {
                    if (isNumber(b)) {
                        val x: Short = a.shortValue()
                        val y: Short = b.shortValue()
                        return@label if (x <= y) x else y
                    }
                    return@label a.shortValue()
                }
                b.shortValue()
            }, label@
            BiFunction<Number, Number, Number> { a, b ->
                if (isNumber(a)) {
                    if (isNumber(b)) {
                        val x: Short = a.shortValue()
                        val y: Short = b.shortValue()
                        return@label if (x >= y) x else y
                    }
                    return@label a.shortValue()
                }
                b.shortValue()
            },
            BiFunction<Number, Number, Integer> { a, b -> Short.compare(a.shortValue(), b.shortValue()) })
        val INTEGER_NUMBER_HELPER = NumberHelper(
            BiFunction<Number, Number, Number> { a, b -> a.intValue() + b.intValue() },
            BiFunction<Number, Number, Number> { a, b -> a.intValue() - b.intValue() },
            BiFunction<Number, Number, Number> { a, b -> a.intValue() * b.intValue() },
            BiFunction<Number, Number, Number> { a, b -> a.intValue() / b.intValue() }, label@
            BiFunction<Number, Number, Number> { a, b ->
                if (isNumber(a)) {
                    if (isNumber(b)) {
                        val x: Int = a.intValue()
                        val y: Int = b.intValue()
                        return@label if (x <= y) x else y
                    }
                    return@label a.intValue()
                }
                b.intValue()
            }, label@
            BiFunction<Number, Number, Number> { a, b ->
                if (isNumber(a)) {
                    if (isNumber(b)) {
                        val x: Int = a.intValue()
                        val y: Int = b.intValue()
                        return@label if (x >= y) x else y
                    }
                    return@label a.intValue()
                }
                b.intValue()
            },
            BiFunction<Number, Number, Integer> { a, b -> Integer.compare(a.intValue(), b.intValue()) })
        val LONG_NUMBER_HELPER = NumberHelper(
            BiFunction<Number, Number, Number> { a, b -> a.longValue() + b.longValue() },
            BiFunction<Number, Number, Number> { a, b -> a.longValue() - b.longValue() },
            BiFunction<Number, Number, Number> { a, b -> a.longValue() * b.longValue() },
            BiFunction<Number, Number, Number> { a, b -> a.longValue() / b.longValue() }, label@
            BiFunction<Number, Number, Number> { a, b ->
                if (isNumber(a)) {
                    if (isNumber(b)) {
                        val x: Long = a.longValue()
                        val y: Long = b.longValue()
                        return@label if (x <= y) x else y
                    }
                    return@label a.longValue()
                }
                b.longValue()
            }, label@
            BiFunction<Number, Number, Number> { a, b ->
                if (isNumber(a)) {
                    if (isNumber(b)) {
                        val x: Long = a.longValue()
                        val y: Long = b.longValue()
                        return@label if (x >= y) x else y
                    }
                    return@label a.longValue()
                }
                b.longValue()
            },
            BiFunction<Number, Number, Integer> { a, b -> Long.compare(a.longValue(), b.longValue()) })
        val BIG_INTEGER_NUMBER_HELPER = NumberHelper(
            BiFunction<Number, Number, Number> { a, b -> bigIntegerValue(a).add(bigIntegerValue(b)) },
            BiFunction<Number, Number, Number> { a, b -> bigIntegerValue(a).subtract(bigIntegerValue(b)) },
            BiFunction<Number, Number, Number> { a, b -> bigIntegerValue(a).multiply(bigIntegerValue(b)) },
            BiFunction<Number, Number, Number> { a, b -> bigIntegerValue(a).divide(bigIntegerValue(b)) }, label@
            BiFunction<Number, Number, Number> { a, b ->
                if (isNumber(a)) {
                    if (isNumber(b)) {
                        val x: BigInteger? = bigIntegerValue(a)
                        val y: BigInteger? = bigIntegerValue(b)
                        return@label if (x.compareTo(y) <= 0) x else y
                    }
                    return@label bigIntegerValue(a)
                }
                bigIntegerValue(b)
            }, label@
            BiFunction<Number, Number, Number> { a, b ->
                if (isNumber(a)) {
                    if (isNumber(b)) {
                        val x: BigInteger? = bigIntegerValue(a)
                        val y: BigInteger? = bigIntegerValue(b)
                        return@label if (x.compareTo(y) >= 0) x else y
                    }
                    return@label bigIntegerValue(a)
                }
                bigIntegerValue(b)
            },
            BiFunction<Number, Number, Integer> { a, b -> bigIntegerValue(a).compareTo(bigIntegerValue(b)) })
        val FLOAT_NUMBER_HELPER = NumberHelper(
            BiFunction<Number, Number, Number> { a, b -> a.floatValue() + b.floatValue() },
            BiFunction<Number, Number, Number> { a, b -> a.floatValue() - b.floatValue() },
            BiFunction<Number, Number, Number> { a, b -> a.floatValue() * b.floatValue() },
            BiFunction<Number, Number, Number> { a, b -> a.floatValue() / b.floatValue() }, label@
            BiFunction<Number, Number, Number> { a, b ->
                if (isNumber(a)) {
                    if (isNumber(b)) {
                        val x: Float = a.floatValue()
                        val y: Float = b.floatValue()
                        return@label if (x <= y) x else y
                    }
                    return@label a.floatValue()
                }
                b.floatValue()
            }, label@
            BiFunction<Number, Number, Number> { a, b ->
                if (isNumber(a)) {
                    if (isNumber(b)) {
                        val x: Float = a.floatValue()
                        val y: Float = b.floatValue()
                        return@label if (x >= y) x else y
                    }
                    return@label a.floatValue()
                }
                b.floatValue()
            },
            BiFunction<Number, Number, Integer> { a, b -> Float.compare(a.floatValue(), b.floatValue()) })
        val DOUBLE_NUMBER_HELPER = NumberHelper(
            BiFunction<Number, Number, Number> { a, b -> a.doubleValue() + b.doubleValue() },
            BiFunction<Number, Number, Number> { a, b -> a.doubleValue() - b.doubleValue() },
            BiFunction<Number, Number, Number> { a, b -> a.doubleValue() * b.doubleValue() },
            BiFunction<Number, Number, Number> { a, b -> a.doubleValue() / b.doubleValue() }, label@
            BiFunction<Number, Number, Number> { a, b ->
                if (isNumber(a)) {
                    if (isNumber(b)) {
                        val x: Double = a.doubleValue()
                        val y: Double = b.doubleValue()
                        return@label if (x <= y) x else y
                    }
                    return@label a.doubleValue()
                }
                b.doubleValue()
            }, label@
            BiFunction<Number, Number, Number> { a, b ->
                if (isNumber(a)) {
                    if (isNumber(b)) {
                        val x: Double = a.doubleValue()
                        val y: Double = b.doubleValue()
                        return@label if (x >= y) x else y
                    }
                    return@label a.doubleValue()
                }
                b.doubleValue()
            },
            BiFunction<Number, Number, Integer> { a, b -> Double.compare(a.doubleValue(), b.doubleValue()) })
        val BIG_DECIMAL_NUMBER_HELPER = NumberHelper(
            BiFunction<Number, Number, Number> { a, b -> bigDecimalValue(a).add(bigDecimalValue(b)) },
            BiFunction<Number, Number, Number> { a, b -> bigDecimalValue(a).subtract(bigDecimalValue(b)) },
            BiFunction<Number, Number, Number> { a, b -> bigDecimalValue(a).multiply(bigDecimalValue(b)) }, label@
            BiFunction<Number, Number, Number> { a, b ->
                val ba: BigDecimal? = bigDecimalValue(a)
                val bb: BigDecimal? = bigDecimalValue(b)
                try {
                    return@label ba.divide(bb)
                } catch (ignored: ArithmeticException) {
                    // set a default precision
                    val precision: Int = Math.max(ba.precision(), bb.precision()) + 10
                    var result: BigDecimal = ba.divide(bb, MathContext(precision))
                    val scale: Int = Math.max(Math.max(ba.scale(), bb.scale()), 10)
                    if (result.scale() > scale) result = result.setScale(scale, BigDecimal.ROUND_HALF_UP)
                    return@label result
                }
            }, label@
            BiFunction<Number, Number, Number> { a, b ->
                if (isNumber(a)) {
                    if (isNumber(b)) {
                        val x: BigDecimal? = bigDecimalValue(a)
                        val y: BigDecimal? = bigDecimalValue(b)
                        return@label if (x.compareTo(y) <= 0) x else y
                    }
                    return@label bigDecimalValue(a)
                }
                bigDecimalValue(b)
            }, label@
            BiFunction<Number, Number, Number> { a, b ->
                if (isNumber(a)) {
                    if (isNumber(b)) {
                        val x: BigDecimal? = bigDecimalValue(a)
                        val y: BigDecimal? = bigDecimalValue(b)
                        return@label if (x.compareTo(y) >= 0) x else y
                    }
                    return@label bigDecimalValue(a)
                }
                bigDecimalValue(b)
            },
            BiFunction<Number, Number, Integer> { a, b -> bigDecimalValue(a).compareTo(bigDecimalValue(b)) })

        fun getHighestCommonNumberClass(vararg numbers: Number?): Class<out Number?> {
            return getHighestCommonNumberClass(false, *numbers)
        }

        fun getHighestCommonNumberClass(forceFloatingPoint: Boolean, vararg numbers: Number): Class<out Number?> {
            var bits = 8
            var fp = forceFloatingPoint
            for (number in numbers) {
                if (!isNumber(number)) continue
                val clazz: Class<out Number?> = number.getClass()
                if (clazz.equals(Byte::class.java)) continue
                if (clazz.equals(Short::class.java)) {
                    bits = if (bits < 16) 16 else bits
                } else if (clazz.equals(Integer::class.java)) {
                    bits = if (bits < 32) 32 else bits
                } else if (clazz.equals(Long::class.java)) {
                    bits = if (bits < 64) 64 else bits
                } else if (clazz.equals(BigInteger::class.java)) {
                    bits = if (bits < 128) 128 else bits
                } else if (clazz.equals(Float::class.java)) {
                    bits = if (bits < 32) 32 else bits
                    fp = true
                } else if (clazz.equals(Double::class.java)) {
                    bits = if (bits < 64) 64 else bits
                    fp = true
                } else  /*if (clazz.equals(BigDecimal.class))*/ {
                    bits = if (bits < 128) 128 else bits
                    fp = true
                    break // maxed out, no need to check remaining numbers
                }
            }
            return determineNumberClass(bits, fp)
        }

        /**
         * Adds two numbers returning the highest common number class between them.
         *
         * <pre>
         * a = 1, b = 1 -> 2
         * a = null, b = 1 -> null
         * a = 1, b = null -> 1
         * a = null, b = null -> null
        </pre> *
         *
         * @param a should be thought of as the seed to be modified by `b`
         * @param b the modifier to {code a}
         */
        fun add(a: Number?, b: Number?): Number? {
            if (null == a || null == b) return a
            val clazz: Class<out Number?> = getHighestCommonNumberClass(a, b)
            return getHelper(clazz).add.apply(a, b)
        }

        /**
         * Subtracts two numbers returning the highest common number class between them.
         *
         * <pre>
         * a = 1, b = 1 -> 0
         * a = null, b = 1 -> null
         * a = 1, b = null -> 1
         * a = null, b = null -> null
        </pre> *
         *
         * @param a should be thought of as the seed to be modified by `b`
         * @param b the modifier to {code a}
         */
        fun sub(a: Number?, b: Number?): Number? {
            if (null == a || null == b) return a
            val clazz: Class<out Number?> = getHighestCommonNumberClass(a, b)
            return getHelper(clazz).sub.apply(a, b)
        }

        /**
         * Multiplies two numbers returning the highest common number class between them.
         *
         * <pre>
         * a = 1, b = 2 -> 2
         * a = null, b = 1 -> null
         * a = 1, b = null -> 1
         * a = null, b = null -> null
        </pre> *
         *
         * @param a should be thought of as the seed to be modified by `b`
         * @param b the modifier to {code a}
         */
        fun mul(a: Number?, b: Number?): Number? {
            if (null == a || null == b) return a
            val clazz: Class<out Number?> = getHighestCommonNumberClass(a, b)
            return getHelper(clazz).mul.apply(a, b)
        }

        /**
         * Divides two numbers returning the highest common number class between them calling
         * [.div] with a `false`.
         */
        fun div(a: Number?, b: Number?): Number? {
            return if (null == a || null == b) a else div(a, b, false)
        }

        /**
         * Divides two numbers returning the highest common number class between them.
         *
         * <pre>
         * a = 4, b = 2 -> 2
         * a = null, b = 1 -> null
         * a = 1, b = null -> 1
         * a = null, b = null -> null
        </pre> *
         *
         * @param a should be thought of as the seed to be modified by `b`
         * @param b the modifier to {code a}
         * @param forceFloatingPoint when set to `true` ensures that the return value is the highest common floating number class
         */
        fun div(a: Number?, b: Number?, forceFloatingPoint: Boolean): Number? {
            if (null == a || null == b) return null
            val clazz: Class<out Number?> = getHighestCommonNumberClass(forceFloatingPoint, a, b)
            return getHelper(clazz).div.apply(a, b)
        }

        /**
         * Gets the smaller number of the two provided returning the highest common number class between them.
         *
         * <pre>
         * a = 4, b = 2 -> 2
         * a = null, b = 1 -> 1
         * a = 1, b = null -> 1
         * a = null, b = null -> null
        </pre> *
         */
        fun min(a: Number?, b: Number?): Number? {
            if (null == a && null == b) return null
            val clazz: Class<out Number?> = getHighestCommonNumberClass(a, b)
            return getHelper(clazz).min.apply(a, b)
        }

        /**
         * Gets the smaller number of the two provided returning the highest common number class between them.
         *
         * <pre>
         * a = 4, b = 2 -> 2
         * a = null, b = 1 -> 1
         * a = 1, b = null -> 1
         * a = null, b = null -> null
        </pre> *
         */
        fun min(a: Comparable?, b: Comparable?): Comparable? {
            if (null == a || null == b) {
                return if (null == a && null == b) null else if (null == a) b else a
            }
            if (a is Number && b is Number && !a.equals(Double.NaN) && !b.equals(Double.NaN)) {
                val an = a as Number
                val bn = b as Number
                val clazz: Class<out Number?> = getHighestCommonNumberClass(an, bn)
                return getHelper(clazz).min.apply(an, bn)
            }
            return if (isNonValue(a)) b else if (isNonValue(b)) a else if (a.compareTo(b) < 0) a else b
        }

        /**
         * Gets the larger number of the two provided returning the highest common number class between them.
         *
         * <pre>
         * a = 4, b = 2 -> 4
         * a = null, b = 1 -> 1
         * a = 1, b = null -> 1
         * a = null, b = null -> null
        </pre> *
         */
        fun max(a: Number?, b: Number?): Number? {
            if (null == a && null == b) return null
            val clazz: Class<out Number?> = getHighestCommonNumberClass(a, b)
            return getHelper(clazz).max.apply(a, b)
        }

        /**
         * Gets the larger number of the two provided returning the highest common number class between them.
         *
         * <pre>
         * a = 4, b = 2 -> 4
         * a = null, b = 1 -> 1
         * a = 1, b = null -> 1
         * a = null, b = null -> null
        </pre> *
         */
        fun max(a: Comparable?, b: Comparable?): Comparable? {
            if (null == a || null == b) {
                return if (null == a && null == b) null else if (null == a) b else a
            }
            if (a is Number && b is Number && !a.equals(Double.NaN) && !b.equals(Double.NaN)) {
                val an = a as Number
                val bn = b as Number
                val clazz: Class<out Number?> = getHighestCommonNumberClass(an, bn)
                return getHelper(clazz).max.apply(an, bn)
            }
            return if (isNonValue(a)) b else if (isNonValue(b)) a else if (a.compareTo(b) > 0) a else b
        }

        /**
         * Compares two numbers.
         *
         * <pre>
         * a = 4, b = 2 -> 1
         * a = 2, b = 4 -> -1
         * a = null, b = 1 -> -1
         * a = 1, b = null -> 1
         * a = null, b = null -> 0
        </pre> *
         */
        fun compare(a: Number?, b: Number?): Integer {
            if (null == a || null == b) {
                return if (a === b) 0 else if (null == a) -1 else 1
            }
            val clazz: Class<out Number?> = getHighestCommonNumberClass(a, b)
            return getHelper(clazz).cmp.apply(a, b)
        }

        private fun getHelper(clazz: Class<out Number?>): NumberHelper {
            if (clazz.equals(Byte::class.java)) {
                return BYTE_NUMBER_HELPER
            }
            if (clazz.equals(Short::class.java)) {
                return SHORT_NUMBER_HELPER
            }
            if (clazz.equals(Integer::class.java)) {
                return INTEGER_NUMBER_HELPER
            }
            if (clazz.equals(Long::class.java)) {
                return LONG_NUMBER_HELPER
            }
            if (clazz.equals(BigInteger::class.java)) {
                return BIG_INTEGER_NUMBER_HELPER
            }
            if (clazz.equals(Float::class.java)) {
                return FLOAT_NUMBER_HELPER
            }
            if (clazz.equals(Double::class.java)) {
                return DOUBLE_NUMBER_HELPER
            }
            if (clazz.equals(BigDecimal::class.java)) {
                return BIG_DECIMAL_NUMBER_HELPER
            }
            throw IllegalArgumentException("Unsupported numeric type: $clazz")
        }

        private fun bigIntegerValue(number: Number?): BigInteger? {
            if (number == null) return null
            return if (number is BigInteger) number as BigInteger? else BigInteger.valueOf(number.longValue())
        }

        private fun bigDecimalValue(number: Number?): BigDecimal? {
            if (number == null) return null
            if (number is BigDecimal) return number as BigDecimal?
            if (number is BigInteger) return BigDecimal(number as BigInteger?)
            return if (number is Double || number is Float) BigDecimal.valueOf(number.doubleValue()) else BigDecimal.valueOf(
                number.longValue()
            )
        }

        private fun determineNumberClass(bits: Int, floatingPoint: Boolean): Class<out Number?> {
            return if (floatingPoint) {
                if (bits <= 32) return Float::class.java
                if (bits <= 64) Double::class.java else BigDecimal::class.java
            } else {
                if (bits <= 8) return Byte::class.java
                if (bits <= 16) return Short::class.java
                if (bits <= 32) return Integer::class.java
                if (bits <= 64) Long::class.java else BigInteger::class.java
            }
        }

        private fun isNumber(number: Number?): Boolean {
            return number != null && !number.equals(Double.NaN)
        }

        private fun isNonValue(value: Object): Boolean {
            return value is Double && !isNumber(value as Double)
        }
    }
}