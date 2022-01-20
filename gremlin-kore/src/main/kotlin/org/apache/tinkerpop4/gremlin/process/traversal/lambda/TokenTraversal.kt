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
class TokenTraversal<S, E>(t: T) : AbstractLambdaTraversal<S, E>() {
    private var e: E? = null
    private val t: T

    init {
        this.t = t
    }

    @Override
    operator fun next(): E {
        return e
    }

    @Override
    fun addStart(start: Traverser.Admin<S>) {
        val s: S = start.get()
        if (s is Element) e = t.apply(start.get() as Element) else if (s is Property) {
            // T.apply() doesn't work on Property because the inheritance hierarchy doesn't make it an Element. have
            // to special case it here. only T.key/value make any sense for it.
            if (t === T.key) e = (s as Property).key() else if (t === T.value) e =
                (s as Property<E>).value() else throw IllegalStateException(
                String.format(
                    "TokenTraversal support of Property does not allow selection by %s",
                    t
                )
            )
        } else throw IllegalStateException(
            String.format(
                "TokenTraversal support of %s does not allow selection by %s",
                s.getClass().getName(),
                t
            )
        )
    }

    @Override
    override fun toString(): String {
        return t.toString()
    }

    val token: T
        get() = t

    @Override
    override fun hashCode(): Int {
        return this.getClass().hashCode() xor t.hashCode()
    }

    @Override
    override fun equals(other: Object): Boolean {
        return (other is TokenTraversal<*, *>
                && Objects.equals((other as TokenTraversal<*, *>).t, t))
    }
}