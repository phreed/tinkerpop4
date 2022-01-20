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

class TraversalPredicateVisitor private constructor() : GremlinBaseVisitor<P?>() {
    /**
     * Cast ParseTree node child into TraversalPredicateContext
     * @param ctx : ParseTree node
     * @param childIndex : child index
     * @return casted TraversalPredicateContext
     */
    private fun castChildToTraversalPredicate(ctx: ParseTree, childIndex: Int): TraversalPredicateContext {
        return ctx.getChild(childIndex) as TraversalPredicateContext
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalPredicate(ctx: TraversalPredicateContext): P {
        return when (ctx.getChildCount()) {
            1 ->                 // handle simple predicate
                visitChildren(ctx)
            5 ->                 // handle negate
                visit(ctx.getChild(0)).negate()
            6 -> {
                val childIndexOfParameterOperator = 2
                val childIndexOfCaller = 0
                val childIndexOfArgument = 4
                if (ctx.getChild(childIndexOfParameterOperator).getText().equals("or")) {
                    // handle or
                    visit(ctx.getChild(childIndexOfCaller)).or(visit(ctx.getChild(childIndexOfArgument)))
                } else {
                    // handle and
                    visit(ctx.getChild(childIndexOfCaller)).and(visit(ctx.getChild(childIndexOfArgument)))
                }
            }
            else -> throw RuntimeException("unexpected number of children in TraversalPredicateContext " + ctx.getChildCount())
        }
    }

    /**
     * get 1 generic literal argument from the antlr parse tree context,
     * where the arguments has the child index of 2
     */
    private fun getSingleGenericLiteralArgument(ctx: ParseTree): Object {
        val childIndexOfParameterValue = 2
        return GenericLiteralVisitor.instance().visitGenericLiteral(
            ParseTreeContextCastHelper.castChildToGenericLiteral(ctx, childIndexOfParameterValue)
        )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalPredicate_eq(ctx: TraversalPredicate_eqContext): P {
        return P.eq(getSingleGenericLiteralArgument(ctx))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalPredicate_neq(ctx: TraversalPredicate_neqContext): P {
        return P.neq(getSingleGenericLiteralArgument(ctx))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalPredicate_lt(ctx: TraversalPredicate_ltContext): P {
        return P.lt(getSingleGenericLiteralArgument(ctx))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalPredicate_lte(ctx: TraversalPredicate_lteContext): P {
        return P.lte(getSingleGenericLiteralArgument(ctx))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalPredicate_gt(ctx: TraversalPredicate_gtContext): P {
        return P.gt(getSingleGenericLiteralArgument(ctx))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalPredicate_gte(ctx: TraversalPredicate_gteContext): P {
        return P.gte(getSingleGenericLiteralArgument(ctx))
    }

    /**
     * get 2 generic literal arguments from the antlr parse tree context,
     * where the arguments has the child index of 2 and 4
     */
    private fun getDoubleGenericLiteralArgument(ctx: ParseTree): Array<Object> {
        val childIndexOfParameterFirst = 2
        val childIndexOfParameterSecond = 4
        val first: Object = GenericLiteralVisitor.instance().visitGenericLiteral(
            ParseTreeContextCastHelper.castChildToGenericLiteral(ctx, childIndexOfParameterFirst)
        )
        val second: Object = GenericLiteralVisitor.instance().visitGenericLiteral(
            ParseTreeContextCastHelper.castChildToGenericLiteral(ctx, childIndexOfParameterSecond)
        )
        return arrayOf<Object>(first, second)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalPredicate_inside(ctx: TraversalPredicate_insideContext): P {
        val arguments: Array<Object> = getDoubleGenericLiteralArgument(ctx)
        return P.inside(arguments[0], arguments[1])
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalPredicate_outside(ctx: TraversalPredicate_outsideContext): P {
        val arguments: Array<Object> = getDoubleGenericLiteralArgument(ctx)
        return P.outside(arguments[0], arguments[1])
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalPredicate_between(ctx: TraversalPredicate_betweenContext): P {
        val arguments: Array<Object> = getDoubleGenericLiteralArgument(ctx)
        return P.between(arguments[0], arguments[1])
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalPredicate_within(ctx: TraversalPredicate_withinContext): P {
        val childIndexOfParameterValues = 2

        // called with no args which is valid for java/groovy
        if (ctx.getChildCount() === 3) return P.within()
        val arguments: Object = GenericLiteralVisitor.instance().visitGenericLiteralList(
            ParseTreeContextCastHelper.castChildToGenericLiteralList(ctx, childIndexOfParameterValues)
        )
        return if (arguments is Array<Object>) {
            // when generic literal list is consist of a comma separated generic literals
            P.within(arguments as Array<Object?>)
        } else if (arguments is List || arguments is Set) {
            // when generic literal list is consist of a collection of generic literals, E.g. range
            P.within(arguments as Collection)
        } else {
            // when generic literal list is consist of a single generic literal
            P.within(arguments)
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalPredicate_without(ctx: TraversalPredicate_withoutContext): P {
        val childIndexOfParameterValues = 2

        // called with no args which is valid for java/groovy
        if (ctx.getChildCount() === 3) return P.without()
        val arguments: Object = GenericLiteralVisitor.instance().visitGenericLiteralList(
            ParseTreeContextCastHelper.castChildToGenericLiteralList(ctx, childIndexOfParameterValues)
        )
        return if (arguments is Array<Object>) {
            // when generic literal list is consist of a comma separated generic literals
            P.without(arguments as Array<Object?>)
        } else if (arguments is List) {
            // when generic literal list is consist of a collection of generic literals, E.g. range
            P.without(arguments as List)
        } else {
            // when generic literal list is consist of a single generic literal
            P.without(arguments)
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalPredicate_not(ctx: TraversalPredicate_notContext): P {
        val childIndexOfParameterPredicate = 2
        return P.not(visitTraversalPredicate(castChildToTraversalPredicate(ctx, childIndexOfParameterPredicate)))
    }

    @Override
    fun visitTraversalPredicate_containing(ctx: TraversalPredicate_containingContext): P {
        return TextP.containing(GenericLiteralVisitor.getStringLiteral(ctx.stringLiteral()))
    }

    @Override
    fun visitTraversalPredicate_notContaining(ctx: TraversalPredicate_notContainingContext): P {
        return TextP.notContaining(GenericLiteralVisitor.getStringLiteral(ctx.stringLiteral()))
    }

    @Override
    fun visitTraversalPredicate_notEndingWith(ctx: TraversalPredicate_notEndingWithContext): P {
        return TextP.notEndingWith(GenericLiteralVisitor.getStringLiteral(ctx.stringLiteral()))
    }

    @Override
    fun visitTraversalPredicate_endingWith(ctx: TraversalPredicate_endingWithContext): P {
        return TextP.endingWith(GenericLiteralVisitor.getStringLiteral(ctx.stringLiteral()))
    }

    @Override
    fun visitTraversalPredicate_startingWith(ctx: TraversalPredicate_startingWithContext): P {
        return TextP.startingWith(GenericLiteralVisitor.getStringLiteral(ctx.stringLiteral()))
    }

    @Override
    fun visitTraversalPredicate_notStartingWith(ctx: TraversalPredicate_notStartingWithContext): P {
        return TextP.notStartingWith(GenericLiteralVisitor.getStringLiteral(ctx.stringLiteral()))
    }

    companion object {
        private var instance: TraversalPredicateVisitor? = null
        fun instance(): TraversalPredicateVisitor? {
            if (instance == null) {
                instance = TraversalPredicateVisitor()
            }
            return instance
        }

        @Deprecated
        @Deprecated("As of release 3.5.2, replaced by {@link #instance()}.")
        fun getInstance(): TraversalPredicateVisitor? {
            return instance()
        }
    }
}