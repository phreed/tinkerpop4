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
 * This class implements Gremlin grammar's nested-traversal-list methods that returns a [Traversal] `[]`
 * to the callers.
 */
class NestedTraversalSourceListVisitor(context: GremlinAntlrToJava) : GremlinBaseVisitor<Array<Traversal?>?>() {
    protected val context: GremlinAntlrToJava

    init {
        this.context = context
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitNestedTraversalList(ctx: NestedTraversalListContext): Array<Traversal?> {
        return if (ctx.children == null) {
            arrayOfNulls<Traversal>(0)
        } else this.visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitNestedTraversalExpr(ctx: NestedTraversalExprContext): Array<Traversal?> {
        val childCount: Int = ctx.getChildCount()

        // handle arbitrary number of traversals that are separated by comma
        val results: Array<Traversal?> = arrayOfNulls<Traversal>((childCount + 1) / 2)
        var childIndex = 0
        while (childIndex < ctx.getChildCount()) {
            results[childIndex / 2] = context.tvisitor.visitNestedTraversal(
                ctx.getChild(childIndex) as NestedTraversalContext
            )
            // skip comma child
            childIndex += 2
        }
        return results
    }
}