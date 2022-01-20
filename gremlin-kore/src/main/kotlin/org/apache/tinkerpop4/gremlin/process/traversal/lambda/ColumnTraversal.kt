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
package org.apache.tinkerpop4.gremlin.process.traversal.lambda

import org.apache.tinkerpop4.gremlin.process.traversal.Traverser

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class ColumnTraversal(column: Column) : AbstractLambdaTraversal() {
    private var selection: Object? = null
    private val column: Column

    init {
        this.column = column
    }

    @Override
    operator fun next(): Object? {
        return selection
    }

    @Override
    fun addStart(start: Traverser.Admin) {
        selection = column.apply(start.get())
    }

    @Override
    override fun toString(): String {
        return column.toString()
    }

    fun getColumn(): Column {
        return column
    }

    @Override
    override fun hashCode(): Int {
        return this.getClass().hashCode() xor column.hashCode()
    }

    @Override
    override fun equals(other: Object): Boolean {
        return (other is ColumnTraversal
                && Objects.equals((other as ColumnTraversal).column, column))
    }
}