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

import org.apache.tinkerpop4.gremlin.process.traversal.Traversal

/**
 * This visitor handles the cases when a new traversal is getting started. It could either be a nested traversal
 * or a root traversal, but in either case should be anonymous.
 */
class TraversalRootVisitor<G : Traversal?>(antlr: GremlinAntlrToJava?, traversal: Traversal?) :
    GremlinBaseVisitor<Traversal?>() {
    private var traversal: Traversal?
    protected val antlr: GremlinAntlrToJava?

    /**
     * Constructor to produce an anonymous traversal.
     */
    constructor(antlr: GremlinAntlrToJava?) : this(antlr, null) {}

    /**
     * Constructor to build on an existing traversal.
     */
    constructor(traversal: Traversal?) : this(null, traversal) {}

    init {
        this.traversal = traversal
        this.antlr = antlr
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitNestedTraversal(ctx: NestedTraversalContext): Traversal {
        return if (ctx.getChild(0) is RootTraversalContext) {
            visitChildren(ctx)
        } else if (ctx.getChild(2) is ChainedParentOfGraphTraversalContext) {
            TraversalRootVisitor<Traversal>(antlr.createAnonymous.get()).visitChainedParentOfGraphTraversal(
                ctx.chainedTraversal().chainedParentOfGraphTraversal()
            )
        } else {
            TraversalMethodVisitor(antlr, antlr.createAnonymous.get()).visitChainedTraversal(ctx.chainedTraversal())
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitRootTraversal(ctx: RootTraversalContext): Traversal {
        // create traversal source
        val childIndexOfTraversalSource = 0
        val source: GraphTraversalSource = antlr.gvisitor.visitTraversalSource(
            ctx.getChild(childIndexOfTraversalSource) as TraversalSourceContext
        )
        // call traversal source spawn method
        val childIndexOfTraversalSourceSpawnMethod = 2
        val traversal: GraphTraversal = TraversalSourceSpawnMethodVisitor(source, this).visitTraversalSourceSpawnMethod(
            ctx.getChild(childIndexOfTraversalSourceSpawnMethod) as TraversalSourceSpawnMethodContext
        )
        return if (ctx.getChildCount() === 5) {
            // handle chained traversal
            val childIndexOfChainedTraversal = 4
            if (ctx.getChild(childIndexOfChainedTraversal) is ChainedParentOfGraphTraversalContext) {
                val traversalRootVisitor: TraversalRootVisitor<*> = TraversalRootVisitor<Any?>(traversal)
                traversalRootVisitor.visitChainedParentOfGraphTraversal(
                    ctx.getChild(childIndexOfChainedTraversal) as ChainedParentOfGraphTraversalContext
                )
            } else {
                val traversalMethodVisitor = TraversalMethodVisitor(antlr, traversal)
                traversalMethodVisitor.visitChainedTraversal(
                    ctx.getChild(childIndexOfChainedTraversal) as ChainedTraversalContext
                )
            }
        } else {
            traversal
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalSelfMethod(ctx: TraversalSelfMethodContext?): Traversal {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalSelfMethod_none(ctx: TraversalSelfMethod_noneContext?): Traversal? {
        traversal = traversal.none()
        return traversal
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitChainedParentOfGraphTraversal(ctx: ChainedParentOfGraphTraversalContext): Traversal {
        return if (ctx.getChildCount() === 1) {
            visitChildren(ctx)
        } else {
            visit(ctx.getChild(0))
            visit(ctx.getChild(2))
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitChainedTraversal(ctx: ChainedTraversalContext): Traversal {
        return if (ctx.getChildCount() === 1) {
            visitChildren(ctx)
        } else {
            visit(ctx.getChild(0))
            visit(ctx.getChild(2))
        }
    }
}