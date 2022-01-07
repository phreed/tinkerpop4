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
import org.apache.tinkerpop.gremlin.process.computer.MapReduce.ReduceEmitter
import java.util.concurrent.ConcurrentLinkedQueue
import org.apache.tinkerpop.gremlin.process.computer.MapReduce
import java.util.*

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class TinkerReduceEmitter<OK, OV> : ReduceEmitter<OK, OV> {
    protected var reduceQueue: Queue<KeyValue<OK, OV>> = ConcurrentLinkedQueue()
    override fun emit(key: OK, value: OV) {
        reduceQueue.add(KeyValue(key, value))
    }

    protected fun complete(mapReduce: MapReduce<*, *, OK, OV, *>) {
        if (mapReduce.reduceKeySort.isPresent) {
            val comparator = mapReduce.reduceKeySort.get()
            val list: List<KeyValue<OK, OV>> = ArrayList(
                reduceQueue
            )
            Collections.sort(list, Comparator.comparing(
                { obj: KeyValue<OK, OV> -> obj.key }, comparator
            )
            )
            reduceQueue.clear()
            reduceQueue.addAll(list)
        }
    }
}