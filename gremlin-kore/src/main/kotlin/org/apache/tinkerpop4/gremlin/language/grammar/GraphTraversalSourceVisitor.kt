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
 * This class implements the [GraphTraversalSource] producing methods of Gremlin grammar.
 */
class GraphTraversalSourceVisitor(traversalSourceName: String, antlr: GremlinAntlrToJava) :
    GremlinBaseVisitor<GraphTraversalSource?>() {
    private val graph: Graph
    private val antlr: GremlinAntlrToJava
    private val traversalSourceName: String
    private val g: GraphTraversalSource?

    /**
     * Constructs the visitor and if the [GremlinAntlrToJava] has a [GraphTraversalSource] assigned to it,
     * the visitor will prefer that "g" rather than creating a new one from the associated [Graph] instance.
     */
    constructor(antlr: GremlinAntlrToJava) : this(TRAVERSAL_ROOT, antlr) {}

    /**
     * Same as [.GraphTraversalSourceVisitor] but allows the traversal source name to be
     * configured to something other than "g".
     */
    init {
        graph = antlr.graph
        this.antlr = antlr
        g = antlr.g
        this.traversalSourceName = traversalSourceName
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalSource(ctx: TraversalSourceContext): GraphTraversalSource {
        return if (ctx.getChildCount() === 1) {
            // handle source method only
            if (null == g) graph.traversal() else g
        } else {
            val childIndexOfSelfMethod = 2
            val source: GraphTraversalSource
            source = if (ctx.getChild(0).getText().equals(traversalSourceName)) {
                // handle single traversal source
                if (null == g) graph.traversal() else g
            } else {
                // handle chained self method
                val childIndexOfTraversalSource = 0
                visitTraversalSource(
                    ctx.getChild(childIndexOfTraversalSource) as TraversalSourceContext
                )
            }
            TraversalSourceSelfMethodVisitor(source, antlr).visitTraversalSourceSelfMethod(
                ctx.getChild(childIndexOfSelfMethod) as TraversalSourceSelfMethodContext
            )
        }
    }

    companion object {
        const val TRAVERSAL_ROOT = "g"
    }
}