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

import org.apache.tinkerpop4.gremlin.structure.io.binary.GraphBinaryReader

class MapEntrySerializer : SimpleTypeSerializer<Map.Entry?>(null), TransformSerializer<Map.Entry?> {
    @Override
    @Throws(IOException::class)
    protected fun readValue(buffer: Buffer?, context: GraphBinaryReader?): Map.Entry {
        throw IOException("A map entry should not be read individually")
    }

    @Override
    @Throws(IOException::class)
    protected fun writeValue(value: Map.Entry?, buffer: Buffer?, context: GraphBinaryWriter?) {
        throw IOException("A map entry should not be written individually")
    }

    @Override
    fun transform(value: Map.Entry): Object {
        val map: Map = HashMap()
        map.put(value.getKey(), value.getValue())
        return map
    }
}