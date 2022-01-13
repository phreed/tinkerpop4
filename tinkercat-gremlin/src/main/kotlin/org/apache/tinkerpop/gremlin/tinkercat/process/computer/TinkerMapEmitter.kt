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
        if (doReduce) reduceMap!!.computeIfAbsent(key) { ConcurrentLinkedQueue() }
            .add(value) else mapQueue!!.add(
            KeyValue(key, value)
        )
    }

    fun complete(mapReduce: MapReduce<K, V, *, *, *>) {
        if (!doReduce && mapReduce.mapKeySort.isPresent) {
            val comparator = mapReduce.mapKeySort.get()
            val list = mutableListOf<KeyValue<K,V>>()
                list.addAll(mapQueue!!)
            list.sortWith { e0, e1 ->
                comparator.compare(e0.key, e1.key)
            }
            mapQueue!!.clear()
            mapQueue!!.addAll(list)
        } else if (mapReduce.mapKeySort.isPresent) {
            val comparator = mapReduce.mapKeySort.get()
            val newReduceMap = this.reduceMap?.toSortedMap { e0, e1 ->
                comparator.compare(e0, e1)
            }
            this.reduceMap = newReduceMap
        }
    }
}