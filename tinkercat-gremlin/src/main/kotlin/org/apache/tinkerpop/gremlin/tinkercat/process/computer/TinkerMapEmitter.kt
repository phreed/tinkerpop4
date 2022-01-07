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
package org.apache.tinkerpop.gremlin.tinkercat.process.computer

import org.apache.tinkerpop.gremlin.process.computer.KeyValue
import org.apache.tinkerpop.gremlin.process.computer.MapReduce.MapEmitter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import org.apache.tinkerpop.gremlin.process.computer.MapReduce
import java.util.*
import java.util.function.Consumer
import java.util.function.Function

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class TinkerMapEmitter<K, V>(private val doReduce: Boolean) : MapEmitter<K, V> {
    var reduceMap: MutableMap<K, Queue<V>>? = null
    var mapQueue: Queue<KeyValue<K, V>>? = null

    init {
        if (doReduce) reduceMap = ConcurrentHashMap() else mapQueue = ConcurrentLinkedQueue()
    }

    override fun emit(key: K, value: V) {
        if (doReduce) reduceMap!!.computeIfAbsent(key, Function<K, Queue<V?>> { k: K -> ConcurrentLinkedQueue() })
            .add(value) else mapQueue!!.add(
            KeyValue(key, value)
        )
    }

    protected fun complete(mapReduce: MapReduce<K, V, *, *, *>) {
        if (!doReduce && mapReduce.mapKeySort.isPresent) {
            val comparator = mapReduce.mapKeySort.get()
            val list: List<KeyValue<K, V>> = ArrayList(
                mapQueue
            )
            Collections.sort(list, Comparator.comparing(
                { obj: KeyValue<K, V> -> obj.key }, comparator
            )
            )
            mapQueue!!.clear()
            mapQueue!!.addAll(list)
        } else if (mapReduce.mapKeySort.isPresent) {
            val comparator = mapReduce.mapKeySort.get()
            val list: MutableList<Map.Entry<K, Queue<V>>> = ArrayList()
            list.addAll(reduceMap!!.entries)
            Collections.sort(list, Comparator.comparing(
                Function<Map.Entry<K, Queue<V>>, K> { (key, value) -> java.util.Map.Entry.key }, comparator
            )
            )
            reduceMap = LinkedHashMap()
            list.forEach(Consumer { (key, value): Map.Entry<K, Queue<V>> -> reduceMap[key] = value })
        }
    }
}