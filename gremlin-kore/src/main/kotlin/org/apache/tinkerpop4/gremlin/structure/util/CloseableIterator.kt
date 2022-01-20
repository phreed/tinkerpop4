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
package org.apache.tinkerpop4.gremlin.structure.util

import org.apache.tinkerpop4.gremlin.structure.Graph

/**
 * An extension of `Iterator` that implements `Closeable` which allows a [Graph] implementation
 * that hold open resources to provide the user the option to release those resources.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
interface CloseableIterator<T> : Iterator<T>, Closeable {
    @Override
    fun close() {
        // do nothing by default
    }

    companion object {
        /**
         * Wraps an existing `Iterator` in a `CloseableIterator`. If the `Iterator` is already of that
         * type then it will simply be returned as-is.
         */
        fun <T> asCloseable(iterator: Iterator<T>?): CloseableIterator<T>? {
            return if (iterator is CloseableIterator<*>) iterator as CloseableIterator<T>? else DefaultCloseableIterator<T>(
                iterator
            )
        }

        fun <T> closeIterator(iterator: Iterator<T>) {
            if (iterator is AutoCloseable) {
                try {
                    (iterator as AutoCloseable).close()
                } catch (e: Exception) {
                    throw RuntimeException(e)
                }
            }
        }
    }
}