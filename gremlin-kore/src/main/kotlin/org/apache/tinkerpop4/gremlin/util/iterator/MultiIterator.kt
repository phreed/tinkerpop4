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

import org.apache.tinkerpop4.gremlin.process.traversal.util.FastNoSuchElementException
import org.apache.tinkerpop4.gremlin.structure.util.CloseableIterator
import java.io.Serializable
import java.util.ArrayList
import java.util.Iterator
import java.util.List

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class MultiIterator<T> : Iterator<T>, Serializable, AutoCloseable {
    private val iterators: List<Iterator<T>> = ArrayList()
    private var current = 0
    fun addIterator(iterator: Iterator<T>?) {
        iterators.add(iterator)
    }

    @Override
    override fun hasNext(): Boolean {
        if (current >= iterators.size()) return false
        var currentIterator = iterators[current]
        while (true) {
            currentIterator = if (currentIterator.hasNext()) {
                return true
            } else {
                current++
                if (current >= iterators.size()) break
                iterators[current]
            }
        }
        return false
    }

    @Override
    fun remove() {
        iterators[current].remove()
    }

    @Override
    override fun next(): T {
        if (iterators.isEmpty()) throw FastNoSuchElementException.instance()
        var currentIterator = iterators[current]
        while (true) {
            currentIterator = if (currentIterator.hasNext()) {
                return currentIterator.next()
            } else {
                current++
                if (current >= iterators.size()) break
                iterators[current]
            }
        }
        throw FastNoSuchElementException.instance()
    }

    fun clear() {
        iterators.clear()
        current = 0
    }

    /**
     * Close the underlying iterators if auto-closeable. Note that when Exception is thrown from any iterator
     * in the for loop on closing, remaining iterators possibly left unclosed.
     */
    @Override
    fun close() {
        for (iterator in iterators) {
            CloseableIterator.closeIterator(iterator)
        }
    }
}