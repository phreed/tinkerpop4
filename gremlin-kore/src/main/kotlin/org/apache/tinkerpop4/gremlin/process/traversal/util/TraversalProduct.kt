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
package org.apache.tinkerpop4.gremlin.process.traversal.util

import org.apache.tinkerpop4.gremlin.process.traversal.Traversal

/**
 * This class represents the state of the output of a child [Traversal] where it is either productive or not.
 * It is productive if it has at least one traverser within it, even if that traverser is holding a `null`.
 */
class TraversalProduct {
    private val o: Object?
    val isProductive: Boolean

    private constructor() {
        o = null // null is valid technically but productive=false trumps it
        isProductive = false
    }

    internal constructor(o: Object?) {
        this.o = o
        isProductive = true
    }

    fun get(): Object? {
        return o
    }

    fun ifProductive(c: Consumer<Object?>) {
        if (isProductive) c.accept(o)
    }

    companion object {
        val UNPRODUCTIVE = TraversalProduct()
    }
}