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
package org.apache.tinkerpop4.gremlin.process.traversal.step.util

import java.io.Serializable

/**
 * BulkSet is a weighted set (i.e. a multi-set). Objects are added along with a bulk counter the denotes how many times the object was added to the set.
 * Given that count-based compression (vs. enumeration) can yield large sets, methods exist that are long-based (2^64).
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class BulkSet<S> : AbstractSet<S>(), Set<S>, Serializable {
    private val map: Map<S, Long> = LinkedHashMap()
    @Override
    fun size(): Int {
        return longSize().toInt()
    }

    fun uniqueSize(): Int {
        return map.size()
    }

    fun longSize(): Long {
        return map.values().stream().collect(Collectors.summingLong(Long::longValue))
    }

    @Override
    override fun isEmpty(): Boolean {
        return map.isEmpty()
    }

    @Override
    override fun contains(s: Object?): Boolean {
        return map.containsKey(s)
    }

    @Override
    fun add(s: S): Boolean {
        return this.add(s, 1L)
    }

    @Override
    fun addAll(collection: Collection<S>): Boolean {
        if (collection is BulkSet<*>) {
            (collection as BulkSet<S>).map.forEach(this::add)
        } else {
            collection.iterator().forEachRemaining(this::add)
        }
        return true
    }

    fun forEach(consumer: BiConsumer<S, Long?>?) {
        map.forEach(consumer)
    }

    fun asBulk(): Map<S, Long> {
        return Collections.unmodifiableMap(map)
    }

    fun add(s: S, bulk: Long): Boolean {
        val current = map[s]
        return if (current != null) {
            map.put(s, current + bulk)
            false
        } else {
            map.put(s, bulk)
            true
        }
    }

    operator fun get(s: S): Long {
        val bulk = map[s]
        return bulk ?: 0
    }

    @Override
    fun remove(s: Object?): Boolean {
        return map.remove(s) != null
    }

    @Override
    fun clear() {
        map.clear()
    }

    @Override
    fun spliterator(): Spliterator<S> {
        return toList().spliterator()
    }

    @Override
    fun removeAll(collection: Collection<*>): Boolean {
        Objects.requireNonNull(collection)
        var modified = false
        for (`object` in collection) {
            if (null != map.remove(`object`)) modified = true
        }
        return modified
    }

    @Override
    override fun hashCode(): Int {
        return map.hashCode()
    }

    @Override
    override fun equals(`object`: Object): Boolean {
        return `object` is BulkSet<*> && map.equals((`object` as BulkSet<*>).map)
    }

    @Override
    override fun toString(): String {
        return map.toString()
    }

    private fun toList(): List<S> {
        val list: List<S> = ArrayList()
        map.forEach { k, v ->
            for (i in 0 until v) {
                list.add(k)
            }
        }
        return list
    }

    @Override
    override fun iterator(): Iterator<S> {
        return object : Iterator<S>() {
            val entryIterator: Iterator<Map.Entry<S, Long>> = map.entrySet().iterator()
            var lastObject: S? = null
            var lastCount = 0L
            override fun hasNext(): Boolean {
                return lastCount > 0L || entryIterator.hasNext()
            }

            @Override
            override fun next(): S {
                if (lastCount > 0L) {
                    lastCount--
                    return lastObject
                }
                val entry = entryIterator.next()
                return if (entry.getValue() === 1) {
                    entry.getKey()
                } else {
                    lastObject = entry.getKey()
                    lastCount = entry.getValue() - 1
                    lastObject
                }
            }
        }
    }
}