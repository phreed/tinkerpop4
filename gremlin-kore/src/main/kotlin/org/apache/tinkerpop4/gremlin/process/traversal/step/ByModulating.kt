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
package org.apache.tinkerpop4.gremlin.process.traversal.step

import org.apache.tinkerpop4.gremlin.process.traversal.Order

/**
 * A `ByModulating` step is able to take [GraphTraversal.by] calls. All the methods have default
 * implementations except [ByModulating.modulateBy]. In short, given a traversal, what should
 * the `ByModulating` step do with it.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
interface ByModulating {
    @Throws(UnsupportedOperationException::class)
    fun modulateBy(traversal: Traversal.Admin<*, *>?) {
        throw UnsupportedOperationException("The by()-modulating step does not support traversal-based modulation: $this")
    }

    @Throws(UnsupportedOperationException::class)
    fun modulateBy(string: String?) {
        this.modulateBy(ValueTraversal(string))
    }

    @Throws(UnsupportedOperationException::class)
    fun modulateBy(token: T?) {
        this.modulateBy(TokenTraversal(token))
    }

    @Throws(UnsupportedOperationException::class)
    fun modulateBy(function: Function?) {
        if (function is T) this.modulateBy(function as T?) else if (function is Column) this.modulateBy(
            ColumnTraversal(
                function as Column?
            )
        ) else this.modulateBy(__.map(FunctionTraverser(function)).asAdmin())
    }

    @Throws(UnsupportedOperationException::class)
    fun modulateBy() {
        this.modulateBy(IdentityTraversal())
    }

    //////
    fun modulateBy(traversal: Traversal.Admin<*, *>?, comparator: Comparator?) {
        throw UnsupportedOperationException("The by()-modulating step does not support traversal/comparator-based modulation: $this")
    }

    fun modulateBy(key: String?, comparator: Comparator?) {
        this.modulateBy(ValueTraversal(key), comparator)
    }

    fun modulateBy(comparator: Comparator?) {
        this.modulateBy(IdentityTraversal(), comparator)
    }

    fun modulateBy(order: Order?) {
        this.modulateBy(IdentityTraversal(), order)
    }

    fun modulateBy(t: T?, comparator: Comparator?) {
        this.modulateBy(TokenTraversal(t), comparator)
    }

    fun modulateBy(column: Column?, comparator: Comparator?) {
        this.modulateBy(ColumnTraversal(column), comparator)
    }

    fun modulateBy(function: Function?, comparator: Comparator?) {
        if (function is T) this.modulateBy(function as T?, comparator) else if (function is Column) this.modulateBy(
            function as Column?,
            comparator
        ) else this.modulateBy(__.map(FunctionTraverser(function)).asAdmin(), comparator)
    }
}