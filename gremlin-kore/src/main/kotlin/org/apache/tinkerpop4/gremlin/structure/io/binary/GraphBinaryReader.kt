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
package org.apache.tinkerpop4.gremlin.structure.io.binary

import org.apache.tinkerpop4.gremlin.structure.io.Buffer

/**
 * Reads a value from a buffer using the [TypeSerializer] instances configured in the
 * [TypeSerializerRegistry].
 *
 *
 *
 * This class exposes two different methods to read a value from a buffer: [GraphBinaryReader.read]
 * and [GraphBinaryReader.readValue]:
 *
 *  * `read()` method expects a value in fully-qualified format, composed of
 * `{type_code}{type_info}{value_flag}{value}`.
 *  * `readValue()` method expects a `{value_flag}{value}` when a value is nullable and
 * only `{value}` when a value is not nullable.
 *
 *
 *
 *
 *
 *
 * The [GraphBinaryReader] should be used to read a nested known type from a [TypeSerializer].
 * For example, if a POINT type is composed by two doubles representing the position in the x and y axes, a
 * [TypeSerializer] for POINT type should use the provided [GraphBinaryReader] instance to read those
 * two double values. As x and y values are expected to be provided as non-nullable doubles, the method
 * `readValue()` should be used: `readValue(buffer, Double.class, false)`
 *
 */
class GraphBinaryReader @JvmOverloads constructor(registry: TypeSerializerRegistry = TypeSerializerRegistry.INSTANCE) {
    private val registry: TypeSerializerRegistry

    init {
        this.registry = registry
    }

    /**
     * Reads a value for an specific type.
     *
     *
     * When the value is nullable, the reader expects the `{value_flag}{value}` to be contained in the
     * buffer.
     *
     *
     * When the value is not nullable, the reader expects only the `{value}` to be contained in the
     * buffer.
     */
    @Throws(IOException::class)
    fun <T> readValue(buffer: Buffer?, type: Class<T>?, nullable: Boolean): T {
        if (buffer == null) {
            throw IllegalArgumentException("input cannot be null.")
        } else if (type == null) {
            throw IllegalArgumentException("type cannot be null.")
        }
        val serializer: TypeSerializer<T> = registry.getSerializer(type)
        return serializer.readValue(buffer, this, nullable)
    }

    /**
     * Reads the type code, information and value of a given buffer with fully-qualified format.
     */
    @Throws(IOException::class)
    fun <T> read(buffer: Buffer): T? {
        // Fully-qualified format: {type_code}{type_info}{value_flag}{value}
        val type: DataType = DataType.get(Byte.toUnsignedInt(buffer.readByte()))
        if (type === DataType.UNSPECIFIED_NULL) {
            // There is no TypeSerializer for unspecified null object
            // Read the value_flag - (folding the buffer.readByte() into the assert does not advance the index so
            // assign to a var first and then do equality on that - learned that the hard way
            val check: Byte = buffer.readByte()
            assert(check.toInt() == 1)

            // Just return null
            return null
        }
        val serializer: TypeSerializer<T>
        serializer = if (type !== DataType.CUSTOM) {
            registry.getSerializer(type)
        } else {
            val customTypeName: String = readValue(buffer, String::class.java, false)
            registry.getSerializerForCustomType(customTypeName)
        }
        return serializer.read(buffer, this)
    }
}