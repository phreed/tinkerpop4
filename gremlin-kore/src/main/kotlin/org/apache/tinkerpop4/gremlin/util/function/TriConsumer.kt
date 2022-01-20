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
package org.apache.tinkerpop4.gremlin.util.function

import java.util.Objects

/**
 * Represents an operation that accepts two input arguments and returns no result. This is the tri-arity
 * specialization of `Consumer`. Unlike most other functional interfaces, `TriConsumer`
 * is expected to operate via side-effects.
 *
 *
 * This is a functional interface whose functional method is [.accept].
 *
 * @param <A> the type of the first argument to the operation
 * @param <B> the type of the second argument to the operation
 * @param <C> the type of the third argument to the operation
 * @author Stephen Mallette (http://stephen.genoprime.com)
</C></B></A> */
interface TriConsumer<A, B, C> {
    /**
     * Performs this operation on the given arguments.
     *
     * @param a the first argument to the operation
     * @param b the second argument to the operation
     * @param c the third argument to the operation
     */
    fun accept(a: A, b: B, c: C)

    /**
     * Returns a composed @{link TriConsumer} that performs, in sequence, this operation followed by the `after`
     * operation. If performing either operation throws an exception, it is relayed to the caller of the composed
     * operation. If performing this operation throws an exception, the after operation will not be performed.
     *
     * @param after the operation to perform after this operation
     * @return a composed [TriConsumer] that performs in sequence this operation followed by the `after`
     * operation
     * @throws NullPointerException if `after` is null
     */
    fun andThen(after: TriConsumer<in A, in B, in C>): TriConsumer<A, B, C>? {
        Objects.requireNonNull(after)
        return TriConsumer<A, B, C> { a: A, b: B, c: C ->
            accept(a, b, c)
            after.accept(a, b, c)
        }
    }
}