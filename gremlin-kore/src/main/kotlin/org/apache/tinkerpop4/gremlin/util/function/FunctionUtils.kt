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

import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Supplier

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
object FunctionUtils {
    fun <T, U> wrapFunction(functionThatThrows: ThrowingFunction<T, U>): Function<T, U> {
        return label@ Function<T, U> { a ->
            try {
                return@label functionThatThrows.apply(a)
            } catch (e: RuntimeException) {
                throw e
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
    }

    fun <T> wrapConsumer(consumerThatThrows: ThrowingConsumer<T>): Consumer<T> {
        return Consumer<T> { a ->
            try {
                consumerThatThrows.accept(a)
            } catch (e: RuntimeException) {
                throw e
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
    }

    fun <T, U> wrapBiConsumer(consumerThatThrows: ThrowingBiConsumer<T, U>): BiConsumer<T, U> {
        return BiConsumer<T, U> { a, b ->
            try {
                consumerThatThrows.accept(a, b)
            } catch (e: RuntimeException) {
                throw e
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
    }

    fun <T> wrapSupplier(supplierThatThrows: ThrowingSupplier<T>): Supplier<T> {
        return label@ Supplier<T> {
            try {
                return@label supplierThatThrows.get()
            } catch (e: RuntimeException) {
                throw e
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
    }
}