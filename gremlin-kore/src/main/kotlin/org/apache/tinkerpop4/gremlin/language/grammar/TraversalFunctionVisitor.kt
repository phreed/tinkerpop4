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

import org.apache.tinkerpop4.gremlin.structure.Column

/**
 * Traversal Function parser parses Function enums.
 */
class TraversalFunctionVisitor private constructor() : GremlinBaseVisitor<Function?>() {
    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalFunction(ctx: TraversalFunctionContext?): Function {
        return this.visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalToken(ctx: TraversalTokenContext?): Function {
        return TraversalEnumParser.parseTraversalEnumFromContext(T::class.java, ctx)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalColumn(ctx: TraversalColumnContext?): Function {
        return TraversalEnumParser.parseTraversalEnumFromContext(Column::class.java, ctx)
    }

    companion object {
        private var instance: TraversalFunctionVisitor? = null
        fun instance(): TraversalFunctionVisitor? {
            if (instance == null) {
                instance = TraversalFunctionVisitor()
            }
            return instance
        }

        @Deprecated
        @Deprecated("As of release 3.5.2, replaced by {@link #instance()}.")
        fun getInstance(): TraversalFunctionVisitor? {
            return instance()
        }
    }
}