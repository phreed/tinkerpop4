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
 * Traversal terminal method visitor
 */
class TraversalTerminalMethodVisitor(traversal: Traversal<*, *>) : GremlinBaseVisitor<Object?>() {
    protected val traversal: Traversal<*, *>

    init {
        this.traversal = traversal
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalTerminalMethod(ctx: TraversalTerminalMethodContext?): Object {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     *
     * Traversal terminal method explain step
     */
    @Override
    fun visitTraversalTerminalMethod_explain(ctx: TraversalTerminalMethod_explainContext?): Object {
        return traversal.explain()
    }

    /**
     * {@inheritDoc}
     *
     * Traversal terminal method has next step
     */
    @Override
    fun visitTraversalTerminalMethod_hasNext(ctx: TraversalTerminalMethod_hasNextContext?): Object {
        return try {
            traversal.hasNext()
        } finally {
            CloseableIterator.closeIterator(traversal)
        }
    }

    /**
     * {@inheritDoc}
     *
     * Traversal terminal method iterate step
     */
    @Override
    fun visitTraversalTerminalMethod_iterate(ctx: TraversalTerminalMethod_iterateContext?): Object {
        return traversal.iterate()
    }

    /**
     * {@inheritDoc}
     *
     * Traversal terminal method try next step
     */
    @Override
    fun visitTraversalTerminalMethod_tryNext(ctx: TraversalTerminalMethod_tryNextContext?): Object {
        return try {
            traversal.tryNext()
        } finally {
            CloseableIterator.closeIterator(traversal)
        }
    }

    /**
     * {@inheritDoc}
     *
     * Traversal terminal method next step
     */
    @Override
    fun visitTraversalTerminalMethod_next(ctx: TraversalTerminalMethod_nextContext): Object {
        return try {
            if (ctx.getChildCount() === 3) {
                traversal.next()
            } else {
                // the 3rd child is integer value
                val childIndexOfParamaterAmount = 2
                traversal.next(Integer.decode(ctx.getChild(childIndexOfParamaterAmount).getText()))
            }
        } finally {
            CloseableIterator.closeIterator(traversal)
        }
    }

    /**
     * {@inheritDoc}
     *
     * Traversal terminal method to list step
     */
    @Override
    fun visitTraversalTerminalMethod_toList(ctx: TraversalTerminalMethod_toListContext?): Object {
        return traversal.toList()
    }

    /**
     * {@inheritDoc}
     *
     * Traversal terminal method to set step
     */
    @Override
    fun visitTraversalTerminalMethod_toSet(ctx: TraversalTerminalMethod_toSetContext?): Object {
        return traversal.toSet()
    }

    /**
     * {@inheritDoc}
     *
     * Traversal terminal method to bulk set step
     */
    @Override
    fun visitTraversalTerminalMethod_toBulkSet(ctx: TraversalTerminalMethod_toBulkSetContext?): Object {
        return traversal.toBulkSet()
    }
}