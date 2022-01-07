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
package org.apache.tinkerpop.gremlin.tinkercat.structure

import org.apache.tinkerpop.gremlin.structure.Element
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Property
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerCat
import java.util.concurrent.ConcurrentHashMap
import java.lang.IllegalArgumentException
import java.util.ArrayList
import java.util.HashSet

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
internal class TinkerIndex<T : Element?>(private val graph: TinkerCat, protected val indexClass: Class<T>) {
    protected var index: MutableMap<String, MutableMap<Any, MutableSet<T>>> = ConcurrentHashMap()
    private val indexedKeys: MutableSet<String> = HashSet()
    protected fun put(key: String, value: Any, element: T) {
        var keyMap = index[key]
        if (null == keyMap) {
            index.putIfAbsent(key, ConcurrentHashMap())
            keyMap = index[key]
        }
        var objects = keyMap!![value]
        if (null == objects) {
            keyMap.putIfAbsent(value, ConcurrentHashMap.newKeySet())
            objects = keyMap[value]
        }
        objects!!.add(element)
    }

    operator fun get(key: String, value: Any?): List<T> {
        val keyMap: Map<Any, MutableSet<T>>? = index[key]
        return if (null == keyMap) {
            emptyList()
        } else {
            val set: Set<T>? =
                keyMap[indexable(value)]
            if (null == set) emptyList() else ArrayList(set)
        }
    }

    fun count(key: String, value: Any?): Long {
        val keyMap: Map<Any, MutableSet<T>>? = index[key]
        return if (null == keyMap) {
            0
        } else {
            val set: Set<T>? =
                keyMap[indexable(value)]
            set?.size?.toLong() ?: 0
        }
    }

    fun remove(key: String, value: Any, element: T) {
        val keyMap = index[key]
        if (null != keyMap) {
            val objects = keyMap[indexable(value)]
            if (null != objects) {
                objects.remove(element)
                if (objects.size == 0) {
                    keyMap.remove(value)
                }
            }
        }
    }

    fun removeElement(element: T) {
        if (indexClass.isAssignableFrom(element.javaClass)) {
            for (map in index.values) {
                for (set in map.values) {
                    set.remove(element)
                }
            }
        }
    }

    fun autoUpdate(key: String, newValue: Any, oldValue: Any, element: T) {
        if (indexedKeys.contains(key)) {
            this.remove(key, oldValue, element)
            put(key, newValue, element)
        }
    }

    fun createKeyIndex(key: String?) {
        if (null == key) throw Graph.Exceptions.argumentCanNotBeNull("key")
        require(!key.isEmpty()) { "The key for the index cannot be an empty string" }
        if (indexedKeys.contains(key)) return
        indexedKeys.add(key)
        (if (Vertex::class.java.isAssignableFrom(indexClass)) graph.vertices.values.parallelStream() else graph.edges.values.parallelStream())
            .map { e: Element -> arrayOf((e as T)!!.property<Any>(key), e) }
            .filter { a: Array<Any> -> (a[0] as Property<*>).isPresent }
            .forEach { a: Array<Any> -> put(key, (a[0] as Property<*>).value(), a[1] as T) }
    }

    fun dropKeyIndex(key: String) {
        if (index.containsKey(key)) index.remove(key)!!.clear()
        indexedKeys.remove(key)
    }

    fun getIndexedKeys(): Set<String> {
        return indexedKeys
    }

    class IndexedNull private constructor() {
        override fun hashCode(): Int {
            return 751912123
        }

        override fun equals(o: Any?): Boolean {
            return o is IndexedNull
        }

        companion object {
            private val inst = IndexedNull()
            fun instance(): IndexedNull {
                return inst
            }
        }
    }

    companion object {
        /**
         * Provides a way for an index to have a `null` value as `ConcurrentHashMap` will not allow a
         * `null` key.
         */
        fun indexable(obj: Any?): Any {
            return obj ?: IndexedNull.instance()
        }
    }
}