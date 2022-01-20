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

import org.apache.tinkerpop4.gremlin.process.traversal.Traversal

/**
 * A [Traversal] that always returns a constant value.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class ConstantTraversal<S, E>(private val end: E) : AbstractLambdaTraversal<S, E>() {
    @Override
    operator fun next(): E {
        return end
    }

    @Override
    override fun toString(): String {
        return "(" + Objects.toString(end).toString() + ")"
    }

    @Override
    override fun hashCode(): Int {
        return this.getClass().hashCode() xor Objects.hashCode(end)
    }

    @Override
    override fun equals(other: Object): Boolean {
        return (other is ConstantTraversal<*, *>
                && Objects.equals((other as ConstantTraversal<*, *>).end, end))
    }
}