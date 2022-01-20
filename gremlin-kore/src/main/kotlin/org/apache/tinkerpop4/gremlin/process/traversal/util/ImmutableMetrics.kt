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
package org.apache.tinkerpop4.gremlin.process.traversal.util

import java.io.Serializable

/**
 * A [Metrics] implementation that cannot be modified.
 *
 * @author Bob Briody (http://bobbriody.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class ImmutableMetrics protected constructor() : Metrics, Serializable {
    var id: String? = null
        protected set

    @get:Override
    var name: String? = null
        protected set
    protected var counts: Map<String, AtomicLong> = ConcurrentHashMap()
    protected var durationNs = 0L
    protected val annotations: Map<String, Object> = Collections.synchronizedMap(LinkedHashMap())
    protected val nested: Map<String, ImmutableMetrics> = LinkedHashMap()
    @Override
    fun getDuration(unit: TimeUnit): Long {
        return unit.convert(durationNs, SOURCE_UNIT)
    }

    @Override
    fun getCount(key: String): Long? {
        return if (!counts.containsKey(key)) {
            null
        } else counts[key].get()
    }

    @Override
    fun getCounts(): Map<String, Long> {
        val ret: Map<String, Long> = HashMap()
        for (count in counts.entrySet()) {
            ret.put(count.getKey(), count.getValue().get())
        }
        return ret
    }

    @Override
    fun getNested(): Collection<ImmutableMetrics> {
        return nested.values()
    }

    @Override
    fun getNested(metricsId: String): ImmutableMetrics? {
        return nested[metricsId]
    }

    @Override
    fun getAnnotations(): Map<String, Object> {
        return annotations
    }

    @Override
    fun getAnnotation(key: String): Object? {
        return annotations[key]
    }

    @Override
    override fun toString(): String {
        return "ImmutableMetrics{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", counts=" + counts +
                ", durationNs=" + durationNs +
                '}'
    }

    companion object {
        val SOURCE_UNIT: TimeUnit = TimeUnit.NANOSECONDS
    }
}