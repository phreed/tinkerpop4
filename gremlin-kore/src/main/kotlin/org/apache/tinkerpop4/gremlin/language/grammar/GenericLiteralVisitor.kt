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
package org.apache.tinkerpop4.gremlin.language.grammar

import org.antlr.v4.runtime.tree.ParseTree

/**
 * Visitor class to handle generic literal. All visitor methods return type is Object. It maybe used as a singleton
 * in cases where a [Traversal] object is not expected, otherwise a new instance must be constructed.
 */
class GenericLiteralVisitor : GremlinBaseVisitor<Object?> {
    protected val antlr: GremlinAntlrToJava?
    protected var traversalStrategyVisitor: GremlinBaseVisitor<TraversalStrategy>? = null

    private constructor() {
        antlr = null
    }

    constructor(antlr: GremlinAntlrToJava?) {
        this.antlr = antlr
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitGenericLiteralList(ctx: GenericLiteralListContext?): Object {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitGenericLiteralExpr(ctx: GenericLiteralExprContext): Object {
        val childCount: Int = ctx.getChildCount()
        return when (childCount) {
            0 ->                 // handle empty expression
                arrayOfNulls<Object>(0)
            1 ->                 // handle single generic literal
                visitGenericLiteral(ctx.getChild(0) as GenericLiteralContext)
            else -> {
                // handle multiple generic literal separated by comma
                val genericLiterals: List<Object> = ArrayList()
                var childIndex = 0
                while (childIndex < ctx.getChildCount()) {
                    genericLiterals.add(
                        visitGenericLiteral(
                            ctx.getChild(childIndex) as GenericLiteralContext
                        )
                    )
                    // skip comma
                    childIndex += 2
                }
                genericLiterals.toArray()
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitGenericLiteral(ctx: GenericLiteralContext?): Object {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitGenericLiteralMap(ctx: GenericLiteralMapContext): Object {
        val literalMap: HashMap<Object, Object> = HashMap()
        var childIndex = 1
        while (childIndex < ctx.getChildCount() && ctx.getChildCount() > 3) {
            val key: Object = visitGenericLiteral(ctx.getChild(childIndex) as GenericLiteralContext)
            // skip colon
            childIndex += 2
            val value: Object = visitGenericLiteral(ctx.getChild(childIndex) as GenericLiteralContext)
            literalMap.put(key, value)
            // skip comma
            childIndex += 2
        }
        return literalMap
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitNestedTraversal(ctx: NestedTraversalContext?): Object {
        return antlr.tvisitor.visitNestedTraversal(ctx)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTerminatedTraversal(ctx: TerminatedTraversalContext): Object {
        val traversal: Traversal = antlr.tvisitor.visitRootTraversal(
            ctx.getChild(0) as RootTraversalContext
        )
        return TraversalTerminalMethodVisitor(traversal).visitTraversalTerminalMethod(
            ctx.getChild(2) as TraversalTerminalMethodContext
        )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitIntegerLiteral(ctx: IntegerLiteralContext): Object {
        var integerLiteral: String = ctx.getText().toLowerCase().replace("_", "")
        // handle suffixes for specific types
        val lastCharIndex: Int = integerLiteral.length() - 1
        val suffix: Char = integerLiteral.charAt(lastCharIndex)
        when (suffix) {
            'b' -> {
                integerLiteral = integerLiteral.substring(0, lastCharIndex)
                return Byte.decode(integerLiteral)
            }
            's' -> {
                integerLiteral = integerLiteral.substring(0, lastCharIndex)
                return Short.decode(integerLiteral)
            }
            'i' -> {
                integerLiteral = integerLiteral.substring(0, lastCharIndex)
                return Integer.decode(integerLiteral)
            }
            'l' -> {
                integerLiteral = integerLiteral.substring(0, lastCharIndex)
                return Long.decode(integerLiteral)
            }
            'n' -> {
                integerLiteral = integerLiteral.substring(0, lastCharIndex)
                return BigInteger(integerLiteral)
            }
        }
        return try {
            // try to parse it as integer first
            Integer.decode(integerLiteral)
        } catch (ignoredExpection1: NumberFormatException) {
            try {
                // If range exceeds integer limit, try to parse it as long
                Long.decode(integerLiteral)
            } catch (ignoredExpection2: NumberFormatException) {
                // If range exceeds Long limit, parse it as BigInteger
                // as the literal range is longer than long, the number of character should be much more than 3,
                // so we skip boundary check below.

                // parse sign character
                var startIndex = 0
                val firstChar: Char = integerLiteral.charAt(0)
                val negative = firstChar == '-'
                if (firstChar == '-' || firstChar == '+') {
                    startIndex++
                }

                // parse radix based on format
                var radix = 10
                if (integerLiteral.charAt(startIndex + 1) === 'x') {
                    radix = 16
                    startIndex += 2
                    integerLiteral = integerLiteral.substring(startIndex)
                    if (negative) {
                        integerLiteral = '-' + integerLiteral
                    }
                } else if (integerLiteral.charAt(startIndex) === '0') {
                    radix = 8
                }

                // create big integer
                BigInteger(integerLiteral, radix)
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitFloatLiteral(ctx: FloatLiteralContext): Object {
        val floatLiteral: String = ctx.getText().toLowerCase()

        // check suffix
        val lastCharIndex: Int = floatLiteral.length() - 1
        val lastCharacter: Char = floatLiteral.charAt(lastCharIndex)
        return if (lastCharacter == 'm') {
            // parse M/m or whatever which could be a parse exception
            BigDecimal(floatLiteral.substring(0, lastCharIndex))
        } else if (lastCharacter == 'f') {
            // parse F/f suffix as Float
            Float(ctx.getText())
        } else if (lastCharacter == 'd') {
            // parse D/d suffix as Double
            Double(floatLiteral)
        } else {
            BigDecimal(floatLiteral)
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitBooleanLiteral(ctx: BooleanLiteralContext): Object {
        return Boolean.valueOf(ctx.getText())
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitDateLiteral(ctx: DateLiteralContext): Object {
        return DatetimeHelper.parse(getStringLiteral(ctx.stringLiteral()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitStringLiteral(ctx: StringLiteralContext): Object {
        // Using Java string unescaping because it coincides with the Groovy rules:
        // https://docs.oracle.com/javase/tutorial/java/data/characters.html
        // http://groovy-lang.org/syntax.html#_escaping_special_characters
        if (ctx.gremlinStringConstants() != null) {
            return GremlinStringConstantsVisitor.instance().visitChildren(ctx)
        }
        return if (ctx.NullLiteral() != null) {
            GremlinStringConstantsVisitor.instance().visitChildren(ctx)
        } else StringEscapeUtils.unescapeJava(stripQuotes(ctx.getText()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalToken(ctx: TraversalTokenContext?): Object {
        return TraversalEnumParser.parseTraversalEnumFromContext(T::class.java, ctx)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalCardinality(ctx: TraversalCardinalityContext?): Object {
        return TraversalEnumParser.parseTraversalEnumFromContext(VertexProperty.Cardinality::class.java, ctx)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalDirection(ctx: TraversalDirectionContext?): Object {
        return TraversalEnumParser.parseTraversalEnumFromContext(Direction::class.java, ctx)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalOptionParent(ctx: TraversalOptionParentContext?): Object {
        return TraversalEnumParser.parseTraversalEnumFromContext(Pick::class.java, ctx)
    }

    @Override
    fun visitTraversalStrategy(ctx: TraversalStrategyContext?): Object {
        if (null == traversalStrategyVisitor) traversalStrategyVisitor =
            TraversalStrategyVisitor(antlr.tvisitor as GremlinBaseVisitor)
        return traversalStrategyVisitor.visitTraversalStrategy(ctx)
    }

    /**
     * Groovy range operator syntax is defined in http://groovy-lang.org/operators.html#_range_operator
     * {@inheritDoc}
     */
    @Override
    fun visitGenericLiteralRange(ctx: GenericLiteralRangeContext): Object {
        val childIndexOfParameterStart = 0
        val childIndexOfParameterEnd = 3
        val startContext: ParseTree = ctx.getChild(childIndexOfParameterStart)
        val endContext: ParseTree = ctx.getChild(childIndexOfParameterEnd)
        return if (startContext is IntegerLiteralContext) {
            // handle integer ranges.
            val start: Int = Integer.valueOf(startContext.getText())
            val end: Int = Integer.valueOf(endContext.getText())
            createIntegerRange(start, end, ctx.getText())
        } else {
            // handle string ranges.
            val start = stripQuotes(startContext.getText())
            val end = stripQuotes(endContext.getText())
            createStringRange(start, end, ctx.getText())
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitNullLiteral(ctx: NullLiteralContext?): Object? {
        return null
    }

    /**
     * {@inheritDoc}
     * Generic literal collection returns a list of `Object`
     */
    @Override
    fun visitGenericLiteralCollection(ctx: GenericLiteralCollectionContext): Object {
        val result: List<Object> = ArrayList(ctx.getChildCount() / 2)
        // first child is "[", so start from 2nd child
        var childIndex = 1
        val childCount: Int = ctx.getChildCount()
        if (childCount > 2) {
            while (childIndex < childCount) {
                result.add(visitGenericLiteral(ctx.getChild(childIndex) as GenericLiteralContext))
                // comma is also child, so we need skip it.
                childIndex += 2
            }
        }
        return result
    }

    companion object {
        /**
         * Limit for integer range result count. It is used to avoid OOM in JVM.
         */
        const val TOTAL_INTEGER_RANGE_RESULT_COUNT_LIMIT = 1000000
        private var instance: GenericLiteralVisitor? = null
        fun instance(): GenericLiteralVisitor? {
            if (instance == null) {
                instance = GenericLiteralVisitor()
            }
            return instance
        }

        @Deprecated
        @Deprecated("As of release 3.5.2, replaced by {@link #instance()}.")
        fun getInstance(): GenericLiteralVisitor? {
            return instance()
        }

        /**
         * Parse a string literal context and return the string literal
         */
        fun getStringLiteral(stringLiteral: StringLiteralContext): String {
            return instance()!!.visitStringLiteral(stringLiteral)
        }

        /**
         * Parse a boolean literal context and return the boolean literal
         */
        fun getBooleanLiteral(booleanLiteral: BooleanLiteralContext): Boolean {
            return instance()!!.visitBooleanLiteral(booleanLiteral)
        }

        /**
         * Parse a String literal list context and return a string array
         */
        fun getStringLiteralList(stringLiteralList: StringLiteralListContext?): Array<String?> {
            return if (stringLiteralList == null || stringLiteralList.stringLiteralExpr() == null) {
                arrayOfNulls(0)
            } else stringLiteralList.stringLiteralExpr().stringLiteral()
                .stream()
                .filter(Objects::nonNull)
                .map { stringLiteral -> instance()!!.visitStringLiteral(stringLiteral) }
                .toArray { _Dummy_.__Stt__() }
        }

        /**
         * Parse a generic literal list, and return an object array
         */
        fun getGenericLiteralList(objectLiteralList: GenericLiteralListContext?): Array<Object?> {
            return if (objectLiteralList == null || objectLiteralList.genericLiteralExpr() == null) {
                arrayOfNulls<Object>(0)
            } else objectLiteralList.genericLiteralExpr().genericLiteral()
                .stream()
                .filter(Objects::nonNull)
                .map { genericLiteral -> instance()!!.visitGenericLiteral(genericLiteral) }
                .toArray { _Dummy_.__Stt__() }
        }

        /**
         * Parse a TraversalStrategy literal list context and return a string array
         */
        fun getTraversalStrategyList(
            traversalStrategyListContext: TraversalStrategyListContext?,
            traversalStrategyVisitor: GremlinBaseVisitor<TraversalStrategy?>
        ): Array<TraversalStrategy?> {
            return if (traversalStrategyListContext == null || traversalStrategyListContext.traversalStrategyExpr() == null) {
                arrayOfNulls<TraversalStrategy>(0)
            } else traversalStrategyListContext.traversalStrategyExpr().traversalStrategy()
                .stream()
                .filter(Objects::nonNull)
                .map { tstrat -> traversalStrategyVisitor.visitTraversalStrategy(tstrat) }
                .toArray { _Dummy_.__Stt__() }
        }

        /**
         * Remove single/double quotes around String literal
         *
         * @param quotedString : quoted string
         * @return quotes stripped string
         */
        private fun stripQuotes(quotedString: String): String {
            return quotedString.substring(1, quotedString.length() - 1)
        }

        /**
         * create an integer range from start to end, based on groovy syntax
         * http://groovy-lang.org/operators.html#_range_operator
         *
         * @param start : start of range
         * @param end   : end of range
         * @param range : original range string, for error message
         * @return : return an object which is type of array of object, and each object is a Integer inside the range.
         */
        private fun createIntegerRange(start: Int, end: Int, range: String): Object {
            val results: List<Object> = ArrayList()
            val total_result_count: Int = Math.abs(start - end)

            // validate result count not exceeding limit
            if (total_result_count > TOTAL_INTEGER_RANGE_RESULT_COUNT_LIMIT) {
                throw IllegalArgumentException("Range " + range + " is too wide. Current limit is " + TOTAL_INTEGER_RANGE_RESULT_COUNT_LIMIT + " items")
            }
            if (start <= end) {
                // handle start <= end
                var cur = start
                while (cur <= end) {
                    results.add(cur)
                    cur++
                }
            } else {
                // handle start > end
                var cur = start
                while (cur >= end) {
                    results.add(cur)
                    cur--
                }
            }
            return results
        }

        /**
         * create a string range from start to end, based on groovy syntax
         * http://groovy-lang.org/operators.html#_range_operator
         * The start and end needs to have same length and share same prefix except the last character.
         *
         * @param start : start of range
         * @param end   : end of range
         * @param range : original range string, for error message
         * @return : return an object which is type of array of object, and each object is a String inside the range.
         */
        private fun createStringRange(start: String, end: String, range: String): Object {
            val results: List<Object> = ArrayList()

            // verify lengths of start and end are same.
            if (start.length() !== end.length()) {
                throw IllegalArgumentException("The start and end of Range $range does not have same number of characters")
            }
            if (start.isEmpty()) {
                // return empty result for empty string ranges
                return results
            }

            // verify start and end share same prefix
            val commonPrefix: String = start.substring(0, start.length() - 1)
            if (!end.startsWith(commonPrefix)) {
                throw IllegalArgumentException(
                    "The start and end of Range " + range +
                            " does not share same prefix until the last character"
                )
            }
            val startLastCharacter: Char = start.charAt(start.length() - 1)
            val endLastCharacter: Char = end.charAt(end.length() - 1)
            if (startLastCharacter <= endLastCharacter) {
                // handle start <= end
                var cur = startLastCharacter
                while (cur <= endLastCharacter) {
                    results.add(commonPrefix + cur)
                    cur++
                }
            } else {
                // handle start > end
                var cur = startLastCharacter
                while (cur >= endLastCharacter) {
                    results.add(commonPrefix + cur)
                    cur--
                }
            }
            return results
        }
    }
}