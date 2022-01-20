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
package org.apache.tinkerpop4.gremlin.process.traversal.traverser.util

import org.apache.tinkerpop4.gremlin.process.traversal.Traverser
import org.apache.tinkerpop4.gremlin.process.traversal.util.FastNoSuchElementException
import org.apache.tinkerpop4.gremlin.util.iterator.IteratorUtils
import java.io.Serializable
import java.util.AbstractSet
import java.util.ArrayList
import java.util.Collections
import java.util.Comparator
import java.util.Iterator
import java.util.LinkedHashMap
import java.util.List
import java.util.Map
import java.util.Queue
import java.util.Random
import java.util.Set
import java.util.Spliterator

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class TraverserSet<S> : AbstractSet<Traverser.Admin<S>?>, Set<Traverser.Admin<S>?>, Queue<Traverser.Admin<S>?>,
    Serializable {
    private val map: Map<Traverser.Admin<S>, Traverser.Admin<S>> = Collections.synchronizedMap(LinkedHashMap())

    constructor() {}
    constructor(traverser: Traverser.Admin<S>?) {
        if (traverser != null) map.put(traverser, traverser)
    }

    @Override
    override fun iterator(): Iterator<Traverser.Admin<S>> {
        return map.values().iterator()
    }

    operator fun get(traverser: Traverser.Admin<S>?): Traverser.Admin<S>? {
        return map[traverser]
    }

    @Override
    fun size(): Int {
        return map.size()
    }

    fun bulkSize(): Long {
        var bulk = 0L
        for (traverser in map.values()) {
            bulk = bulk + traverser.bulk()
        }
        return bulk
    }

    @Override
    override fun isEmpty(): Boolean {
        return map.isEmpty()
    }

    @Override
    override fun contains(traverser: Object?): Boolean {
        return map.containsKey(traverser)
    }

    @Override
    fun add(traverser: Traverser.Admin<S>?): Boolean {
        val existing: Traverser.Admin<S>? = map[traverser]
        return if (null == existing) {
            map.put(traverser, traverser)
            true
        } else {
            existing.merge(traverser)
            false
        }
    }

    @Override
    fun offer(traverser: Traverser.Admin<S>?): Boolean {
        return add(traverser)
    }

    @Override
    fun remove(): Traverser.Admin<S> {  // pop, exception if empty
        val iterator: Iterator<Traverser.Admin<S>> = map.values().iterator()
        if (!iterator.hasNext()) throw FastNoSuchElementException.instance()
        val next: Traverser.Admin<S> = iterator.next()
        iterator.remove()
        return next
    }

    @Override
    fun poll(): Traverser.Admin<S>? {  // pop, null if empty
        return if (map.isEmpty()) null else this.remove()
    }

    @Override
    fun element(): Traverser.Admin<S> { // peek, exception if empty
        return iterator().next()
    }

    @Override
    fun peek(): Traverser.Admin<S>? { // peek, null if empty
        return if (map.isEmpty()) null else iterator().next()
    }

    @Override
    fun remove(traverser: Object?): Boolean {
        return map.remove(traverser) != null
    }

    @Override
    fun clear() {
        map.clear()
    }

    @Override
    fun spliterator(): Spliterator<Traverser.Admin<S>> {
        return map.values().spliterator()
    }

    @Override
    override fun toString(): String {
        return map.values().toString()
    }

    fun sort(comparator: Comparator<Traverser<S>?>?) {
        val list: List<Traverser.Admin<S>> = ArrayList(map.size())
        IteratorUtils.removeOnNext(map.values().iterator()).forEachRemaining(list::add)
        Collections.sort(list, comparator)
        map.clear()
        list.forEach { traverser -> map.put(traverser, traverser) }
    }

    fun shuffle(random: Random?) {
        val list: List<Traverser.Admin<S>> = ArrayList(map.size())
        IteratorUtils.removeOnNext(map.values().iterator()).forEachRemaining(list::add)
        Collections.shuffle(list, random)
        map.clear()
        list.forEach { traverser -> map.put(traverser, traverser) }
    }
}