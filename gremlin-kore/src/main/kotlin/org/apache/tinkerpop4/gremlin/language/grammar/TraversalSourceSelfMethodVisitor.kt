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

import org.apache.tinkerpop4.gremlin.process.traversal.Operator

/**
 * A [GraphTraversalSource] self method visitor.
 */
class TraversalSourceSelfMethodVisitor(source: GraphTraversalSource, antlr: GremlinAntlrToJava) :
    GremlinBaseVisitor<GraphTraversalSource?>() {
    private var traversalStrategyVisitor: GremlinBaseVisitor<TraversalStrategy>? = null
    private val source: GraphTraversalSource
    private val antlr: GremlinAntlrToJava

    init {
        this.source = source
        this.antlr = antlr
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalSourceSelfMethod(ctx: TraversalSourceSelfMethodContext?): GraphTraversalSource {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalSourceSelfMethod_withBulk(ctx: TraversalSourceSelfMethod_withBulkContext): GraphTraversalSource {
        val childIndexOfParameterUseBulk = 2
        val useBulk = GenericLiteralVisitor.instance().visitBooleanLiteral(
            ctx.getChild(childIndexOfParameterUseBulk) as BooleanLiteralContext
        ) as Boolean
        return source.withBulk(useBulk)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalSourceSelfMethod_withPath(ctx: TraversalSourceSelfMethod_withPathContext?): GraphTraversalSource {
        return source.withPath()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalSourceSelfMethod_withSack(ctx: TraversalSourceSelfMethod_withSackContext): GraphTraversalSource {
        val childIndexOfParameterInitialValue = 2
        return if (ctx.getChildCount() === 4) {
            source.withSack(
                GenericLiteralVisitor.instance().visitGenericLiteral(
                    ParseTreeContextCastHelper.castChildToGenericLiteral(ctx, childIndexOfParameterInitialValue)
                )
            )
        } else {
            val childIndexOfParameterMergeOperator = 4
            source.withSack(
                GenericLiteralVisitor.instance().visitGenericLiteral(
                    ParseTreeContextCastHelper.castChildToGenericLiteral(ctx, childIndexOfParameterInitialValue)
                ),
                TraversalEnumParser.parseTraversalEnumFromContext(
                    Operator::class.java,
                    ctx.getChild(childIndexOfParameterMergeOperator)
                )
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalSourceSelfMethod_withSideEffect(ctx: TraversalSourceSelfMethod_withSideEffectContext): GraphTraversalSource {
        val childIndexOfParameterKey = 2
        val childIndexOfParameterInitialValue = 4
        val argument1 = GenericLiteralVisitor.instance().visitStringLiteral(
            ctx.getChild(childIndexOfParameterKey) as StringLiteralContext
        ) as String
        val argument2: Object = GenericLiteralVisitor.instance().visitGenericLiteral(
            ctx.getChild(childIndexOfParameterInitialValue) as GenericLiteralContext
        )
        return source.withSideEffect(argument1, argument2)
    }

    @Override
    fun visitTraversalSourceSelfMethod_withStrategies(ctx: TraversalSourceSelfMethod_withStrategiesContext): GraphTraversalSource {
        if (null == traversalStrategyVisitor) traversalStrategyVisitor =
            TraversalStrategyVisitor(antlr.tvisitor as GremlinBaseVisitor)

        // with 4 children withStrategies() was called with a single TraversalStrategy, otherwise multiple were
        // specified.
        return if (ctx.getChildCount() < 5) {
            source.withStrategies(traversalStrategyVisitor.visitTraversalStrategy(ctx.getChild(2) as TraversalStrategyContext))
        } else {
            val vargs: Array<Object> = GenericLiteralVisitor.getTraversalStrategyList(
                ctx.getChild(4) as TraversalStrategyListContext, traversalStrategyVisitor
            )
            val strats: List<TraversalStrategy> =
                ArrayList(Arrays.asList(Arrays.copyOf(vargs, vargs.size, Array<TraversalStrategy>::class.java)))
            strats.add(0, traversalStrategyVisitor.visitTraversalStrategy(ctx.getChild(2) as TraversalStrategyContext))
            source.withStrategies(strats.toArray(arrayOfNulls<TraversalStrategy>(strats.size())))
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalSourceSelfMethod_with(ctx: TraversalSourceSelfMethod_withContext): GraphTraversalSource {
        val childIndexOfParameterKey = 2
        return if (ctx.getChildCount() === 4) {
            val argument1 = GenericLiteralVisitor.instance().visitStringLiteral(
                ctx.getChild(childIndexOfParameterKey) as StringLiteralContext
            ) as String
            source.with(argument1)
        } else {
            val childIndexOfParameterInitialValue = 4
            val argument1 = GenericLiteralVisitor.instance().visitStringLiteral(
                ctx.getChild(childIndexOfParameterKey) as StringLiteralContext
            ) as String
            val argument2: Object = GenericLiteralVisitor.instance().visitGenericLiteral(
                ctx.getChild(childIndexOfParameterInitialValue) as GenericLiteralContext
            )
            source.with(argument1, argument2)
        }
    }
}