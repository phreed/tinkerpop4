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
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
interface TriFunction<A, B, C, R> {
    /**
     * Applies this function to the given arguments.
     *
     * @param a the first argument to the function
     * @param b the second argument to the function
     * @param c the third argument to the function
     * @return the function result
     */
    fun apply(a: A, b: B, c: C): R

    /**
     * Returns a composed function that first applies this function to its input, and then applies the after function
     * to the result. If evaluation of either function throws an exception, it is relayed to the caller of the composed
     * function.
     *
     * @param after the function to apply after this function is applied
     * @param <V>   the type of the output of the `after` function, and of the composed function
     * @return a composed function that first applies this function and then applies the `after` function.
     * @throws NullPointerException if `after` is null
    </V> */
    fun <V> andThen(after: Function<in R, out V>): TriFunction<A, B, C, V>? {
        Objects.requireNonNull(after)
        return TriFunction<A, B, C, V> { a: A, b: B, c: C -> after.apply(apply(a, b, c)) }
    }
}