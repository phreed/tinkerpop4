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
package org.apache.tinkerpop4.gremlin.structure.io.binary.types

import org.apache.tinkerpop4.gremlin.structure.io.binary.DataType

class MetricsSerializer : SimpleTypeSerializer<Metrics?>(DataType.METRICS) {
    @Override
    @Throws(IOException::class)
    protected fun readValue(buffer: Buffer?, context: GraphBinaryReader): Metrics {
        // Consider using a custom implementation, like "DefaultMetrics"
        val result = MutableMetrics(
            context.readValue(buffer, String::class.java, false),
            context.readValue(buffer, String::class.java, false)
        )
        result.setDuration(context.readValue(buffer, Long::class.java, false), TimeUnit.NANOSECONDS)
        val counts: Map<String, Long> = context.readValue(buffer, Map::class.java, false)
        counts.forEach(result::setCount)
        val annotations: Map<String, Object> = context.readValue(buffer, Map::class.java, false)
        annotations.forEach(result::setAnnotation)
        val nestedMetrics: Collection<MutableMetrics> = collectionSerializer.readValue(buffer, context)
        nestedMetrics.forEach(result::addNested)
        return result
    }

    @Override
    @Throws(IOException::class)
    protected fun writeValue(value: Metrics, buffer: Buffer?, context: GraphBinaryWriter) {
        context.writeValue(value.getId(), buffer, false)
        context.writeValue(value.getName(), buffer, false)
        context.writeValue(value.getDuration(TimeUnit.NANOSECONDS), buffer, false)
        context.writeValue(value.getCounts(), buffer, false)
        context.writeValue(value.getAnnotations(), buffer, false)

        // Avoid changing type to List
        collectionSerializer.writeValue(value.getNested(), buffer, context)
    }

    companion object {
        private val collectionSerializer: CollectionSerializer = CollectionSerializer(DataType.LIST)
    }
}