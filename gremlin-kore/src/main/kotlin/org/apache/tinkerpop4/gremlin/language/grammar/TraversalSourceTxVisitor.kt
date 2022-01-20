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

import org.apache.tinkerpop4.gremlin.process.traversal.dsl.graph.GraphTraversalSource

/**
 * Handles transactions via calls to `tx()`.
 */
class TraversalSourceTxVisitor(source: GraphTraversalSource, antlr: GremlinAntlrToJava) : GremlinBaseVisitor<Void?>() {
    private val source: GraphTraversalSource
    private val antlr: GremlinAntlrToJava

    init {
        this.source = source
        this.antlr = antlr
    }

    @Override
    fun visitTransactionPart(ctx: TransactionPartContext): Void? {
        // position 4 holds the tx command
        val cmd: String = ctx.getChild(4).getText()
        when (cmd) {
            "begin" -> {}
            "commit" -> source.tx().commit()
            "rollback" -> source.tx().rollback()
            else -> notImplemented(ctx)
        }
        return null
    }
}