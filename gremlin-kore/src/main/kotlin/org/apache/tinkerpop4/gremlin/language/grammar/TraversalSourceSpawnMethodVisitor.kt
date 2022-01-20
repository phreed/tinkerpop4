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
 * Use a [GraphTraversalSource] as the source and returns a [GraphTraversal] object.
 */
class TraversalSourceSpawnMethodVisitor(
    traversalSource: GraphTraversalSource,
    anonymousVisitor: GremlinBaseVisitor<Traversal?>
) : GremlinBaseVisitor<GraphTraversal?>() {
    protected var traversalSource: GraphTraversalSource
    protected var graphTraversal: GraphTraversal? = null
    protected val anonymousVisitor: GremlinBaseVisitor<Traversal>

    init {
        this.traversalSource = traversalSource
        this.anonymousVisitor = anonymousVisitor
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalSourceSpawnMethod(ctx: TraversalSourceSpawnMethodContext?): GraphTraversal {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalSourceSpawnMethod_addE(ctx: TraversalSourceSpawnMethod_addEContext): GraphTraversal {
        return if (ctx.stringLiteral() != null) {
            traversalSource.addE(GenericLiteralVisitor.getStringLiteral(ctx.stringLiteral()))
        } else if (ctx.nestedTraversal() != null) {
            traversalSource.addE(anonymousVisitor.visitNestedTraversal(ctx.nestedTraversal()))
        } else {
            throw IllegalArgumentException("addE with empty arguments is not valid.")
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalSourceSpawnMethod_addV(ctx: TraversalSourceSpawnMethod_addVContext): GraphTraversal {
        return if (ctx.stringLiteral() != null) {
            traversalSource.addV(GenericLiteralVisitor.getStringLiteral(ctx.stringLiteral()))
        } else if (ctx.nestedTraversal() != null) {
            traversalSource.addV(anonymousVisitor.visitNestedTraversal(ctx.nestedTraversal()))
        } else {
            traversalSource.addV()
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalSourceSpawnMethod_E(ctx: TraversalSourceSpawnMethod_EContext): GraphTraversal {
        return if (ctx.genericLiteralList().getChildCount() > 0) {
            traversalSource.E(GenericLiteralVisitor.getGenericLiteralList(ctx.genericLiteralList()))
        } else {
            traversalSource.E()
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalSourceSpawnMethod_V(ctx: TraversalSourceSpawnMethod_VContext): GraphTraversal {
        return if (ctx.genericLiteralList().getChildCount() > 0) {
            traversalSource.V(GenericLiteralVisitor.getGenericLiteralList(ctx.genericLiteralList()))
        } else {
            traversalSource.V()
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalSourceSpawnMethod_inject(ctx: TraversalSourceSpawnMethod_injectContext): GraphTraversal {
        return traversalSource.inject(GenericLiteralVisitor.getGenericLiteralList(ctx.genericLiteralList()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalSourceSpawnMethod_io(ctx: TraversalSourceSpawnMethod_ioContext): GraphTraversal? {
        if (ctx.getChildCount() > 2) {
            graphTraversal = traversalSource.io(GenericLiteralVisitor.getStringLiteral(ctx.stringLiteral()))
        }
        return graphTraversal
    }
}