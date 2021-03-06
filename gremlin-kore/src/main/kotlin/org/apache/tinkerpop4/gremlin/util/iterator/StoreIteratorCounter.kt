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
package org.apache.tinkerpop4.gremlin.util.iterator

import org.apache.tinkerpop4.gremlin.process.traversal.Traversal

/**
 * Utility class which can be used by providers to keep a count of the number of
 * open iterators to the underlying storage. Please note that, by default, this does
 * not maintain the count unless explicitly plugged-in by the provider implementation.
 *
 *
 * As an example on how to plugin-in the counter in the provider implementation
 * check TinkerGraphIterator.
 *
 * @see Traversal.close
 */
class StoreIteratorCounter {
    private val openIteratorCount: AtomicLong = AtomicLong(0)
    fun reset() {
        openIteratorCount.set(0)
    }

    fun getOpenIteratorCount(): Long {
        return openIteratorCount.get()
    }

    fun increment() {
        openIteratorCount.incrementAndGet()
    }

    fun decrement() {
        openIteratorCount.decrementAndGet()
    }

    companion object {
        val INSTANCE = StoreIteratorCounter()
    }
}