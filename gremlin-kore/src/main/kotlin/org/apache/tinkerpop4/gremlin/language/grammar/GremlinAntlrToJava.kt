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
 * This is the entry point for converting the Gremlin Antlr grammar into Java. It is bound to a [Graph] instance
 * as that instance may spawn specific [Traversal] or [TraversalSource] types. A new instance should be
 * created for each parse execution.
 */
class GremlinAntlrToJava protected constructor(
    traversalSourceName: String?, graph: Graph?,
    createAnonymous: Supplier<GraphTraversal<*, *>?>,
    g: GraphTraversalSource?
) : GremlinBaseVisitor<Object?>() {
    /**
     * The [Graph] instance to which this instance is bound.
     */
    val graph: Graph?

    /**
     * The "g" from which to start the traversal.
     */
    val g: GraphTraversalSource?

    /**
     * A [GremlinBaseVisitor] that processes [TraversalSource] methods.
     */
    val gvisitor: GremlinBaseVisitor<GraphTraversalSource>

    /**
     * A [GremlinBaseVisitor] that processes [Traversal] methods and is meant to construct traversals
     * anonymously.
     */
    val tvisitor: GremlinBaseVisitor<GraphTraversal>

    /**
     * A [GremlinBaseVisitor] that is meant to construct a list of traversals anonymously.
     */
    val tListVisitor: GremlinBaseVisitor<Array<Traversal>>

    /**
     * Handles transactions.
     */
    val txVisitor: GremlinBaseVisitor<Void>

    /**
     * Creates a [GraphTraversal] implementation that is meant to be anonymous. This provides a way to change the
     * type of implementation that will be used as anonymous traversals. By default, it uses [__] which generates
     * a [DefaultGraphTraversal]
     */
    val createAnonymous: Supplier<GraphTraversal<*, *>>

    /**
     * Constructs a new instance and is bound to an [EmptyGraph]. This form of construction is helpful for
     * generating [Bytecode] or for various forms of testing. [Traversal] instances constructed from this
     * form will not be capable of iterating. Assumes that "g" is the name of the [GraphTraversalSource].
     */
    constructor() : this(EmptyGraph.instance()) {}

    /**
     * Constructs a new instance that is bound to the specified [Graph] instance. Assumes that "g" is the name
     * of the [GraphTraversalSource].
     */
    constructor(graph: Graph?) : this(graph, Anon::start) {}

    /**
     * Constructs a new instance that is bound to the specified [GraphTraversalSource] and thus spawns the
     * [Traversal] from this "g" rather than from a fresh one constructed from the [Graph] instance.
     */
    constructor(g: GraphTraversalSource?) : this(g, Anon::start) {}

    /**
     * Constructs a new instance that is bound to the specified [Graph] instance with an override to using
     * [__] for constructing anonymous [Traversal] instances. Assumes that "g" is the name of the
     * [GraphTraversalSource].
     */
    protected constructor(graph: Graph?, createAnonymous: Supplier<GraphTraversal<*, *>?>) : this(
        GraphTraversalSourceVisitor.TRAVERSAL_ROOT,
        graph,
        createAnonymous
    ) {
    }

    /**
     * Constructs a new instance that is bound to the specified [GraphTraversalSource] and thus spawns the
     * [Traversal] from this "g" rather than from a fresh one constructed from the [Graph] instance.
     */
    protected constructor(g: GraphTraversalSource, createAnonymous: Supplier<GraphTraversal<*, *>?>) : this(
        GraphTraversalSourceVisitor.TRAVERSAL_ROOT,
        g.getGraph(),
        createAnonymous,
        g
    ) {
    }

    /**
     * Constructs a new instance that is bound to the specified [Graph] instance with an override to using
     * [__] for constructing anonymous [Traversal] instances.
     *
     * @param traversalSourceName The name of the traversal source which will be "g" if not specified.
     */
    protected constructor(
        traversalSourceName: String?, graph: Graph?,
        createAnonymous: Supplier<GraphTraversal<*, *>?>
    ) : this(traversalSourceName, graph, createAnonymous, null) {
    }

    /**
     * Constructs a new instance that is bound to the specified [Graph] instance with an override to using
     * [__] for constructing anonymous [Traversal] instances. If the [GraphTraversalSource] is
     * provided then the [Traversal] will spawn from it as opposed to a fresh one from the [Graph] instance.
     *
     * @param traversalSourceName The name of the traversal source which will be "g" if not specified.
     */
    init {
        this.g = g
        this.graph = graph
        gvisitor = GraphTraversalSourceVisitor(
            traversalSourceName ?: GraphTraversalSourceVisitor.TRAVERSAL_ROOT, this
        )
        tvisitor = TraversalRootVisitor(this)
        tListVisitor = NestedTraversalSourceListVisitor(this)
        this.createAnonymous = createAnonymous
        txVisitor = TraversalSourceTxVisitor(g, this)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitQuery(ctx: QueryContext): Object {
        val childCount: Int = ctx.getChildCount()
        return if (childCount <= 3) {
            val firstChild: ParseTree = ctx.getChild(0)
            if (firstChild is TraversalSourceContext) {
                if (childCount == 1) {
                    // handle traversalSource
                    gvisitor.visitTraversalSource(firstChild as TraversalSourceContext)
                } else {
                    // handle traversalSource DOT transactionPart
                    // third child is the tx info
                    txVisitor.visitTransactionPart(ctx.getChild(2) as TransactionPartContext)
                }
            } else if (firstChild is EmptyQueryContext) {
                // handle empty query
                ""
            } else {
                if (childCount == 1) {
                    // handle rootTraversal
                    tvisitor.visitRootTraversal(
                        firstChild as RootTraversalContext
                    )
                } else {
                    // handle rootTraversal DOT traversalTerminalMethod
                    TraversalTerminalMethodVisitor(
                        tvisitor.visitRootTraversal(
                            firstChild as RootTraversalContext
                        )
                    ).visitTraversalTerminalMethod(
                        ctx.getChild(2) as TraversalTerminalMethodContext
                    )
                }
            }
        } else {
            // handle toString
            String.valueOf(visitChildren(ctx))
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitQueryList(ctx: QueryListContext?): Object {
        return visitChildren(ctx)
    }

    /**
     * Override the aggregate result behavior. If the next result is `null`, return the current result. This is
     * used to handle child EOF, which is the last child of the `QueryList` context. If the next Result is not
     * `null`, return the next result. This is used to handle multiple queries, and return only the last query
     * result logic.
     */
    @Override
    protected fun aggregateResult(result: Object, nextResult: Object?): Object {
        return if (nextResult == null) {
            result
        } else {
            nextResult
        }
    }
}