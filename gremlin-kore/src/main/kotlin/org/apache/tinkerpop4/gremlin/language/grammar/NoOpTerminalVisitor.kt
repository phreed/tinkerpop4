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
 * {@inheritDoc}
 *
 * The same as parent visitor [GremlinAntlrToJava] but returns [Bytecode] instead of a [Traversal]
 * or [GraphTraversalSource], and uses an overridden terminal step visitor.
 */
class NoOpTerminalVisitor : GremlinAntlrToJava() {
    /**
     * Returns [Bytecode] of [Traversal] or [GraphTraversalSource], overriding any terminal step
     * operations to prevent them from being executed using the [TraversalTerminalMethodVisitor] to append
     * terminal operations to bytecode.
     *
     * @param ctx - the parse tree
     * @return - bytecode from the traversal or traversal source
     */
    @Override
    fun visitQuery(ctx: QueryContext): Object {
        val childCount: Int = ctx.getChildCount()
        return if (childCount <= 3) {
            val firstChild: ParseTree = ctx.getChild(0)
            if (firstChild is TraversalSourceContext) {
                if (childCount == 1) {
                    // handle traversalSource
                    gvisitor.visitTraversalSource(firstChild as TraversalSourceContext).getBytecode()
                } else {
                    // handle traversalSource DOT transactionPart
                    throw GremlinParserException("Transaction operation is not supported yet")
                }
            } else if (firstChild is EmptyQueryContext) {
                // handle empty query
                ""
            } else {
                if (childCount == 1) {
                    // handle rootTraversal
                    tvisitor.visitRootTraversal(
                        firstChild as RootTraversalContext
                    ).asAdmin().getBytecode()
                } else {
                    // handle rootTraversal DOT traversalTerminalMethod
                    // could not keep all of these methods in one visitor due to the need of the terminal visitor to have a traversal,
                    TerminalMethodToBytecodeVisitor(
                        tvisitor
                            .visitRootTraversal(firstChild as RootTraversalContext)
                    )
                        .visitTraversalTerminalMethod(ctx.getChild(2) as TraversalTerminalMethodContext)
                }
            }
        } else {
            // not clear what valid Gremlin, if any, will trigger this at the moment.
            throw GremlinParserException("Unexpected parse tree for NoOpTerminalVisitor")
        }
    }
}