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

import org.apache.tinkerpop4.gremlin.structure.io.binary.types.CustomTypeSerializer

/**
 * Writes a value to a buffer using the [TypeSerializer] instances configured in the
 * [TypeSerializerRegistry].
 *
 *
 *
 * This class exposes two different methods to write a value to a buffer:
 * [GraphBinaryWriter.write] and
 * [GraphBinaryWriter.writeValue]:
 *
 *  * `write()` method writes the binary representation of the
 * `{type_code}{type_info}{value_flag}{value}` components.
 *  * `writeValue()` method writes the `{value_flag}{value}` when a value is nullable and
 * only `{value}` when a value is not nullable.
 *
 *
 *
 */
class GraphBinaryWriter @JvmOverloads constructor(registry: TypeSerializerRegistry = TypeSerializerRegistry.INSTANCE) {
    private val registry: TypeSerializerRegistry

    init {
        this.registry = registry
    }

    /**
     * Writes a value without including type information.
     */
    @Throws(IOException::class)
    fun <T> writeValue(value: T?, buffer: Buffer, nullable: Boolean) {
        if (value == null) {
            if (!nullable) {
                throw IOException("Unexpected null value when nullable is false")
            }
            writeValueFlagNull(buffer)
            return
        }
        val objectClass: Class<*> = value.getClass()
        val serializer: TypeSerializer<T> = registry.getSerializer(objectClass) as TypeSerializer<T>
        serializer.writeValue(value, buffer, this, nullable)
    }

    /**
     * Writes an object in fully-qualified format, containing {type_code}{type_info}{value_flag}{value}.
     */
    @Throws(IOException::class)
    fun <T> write(value: T?, buffer: Buffer) {
        if (value == null) {
            // return Object of type "unspecified object null" with the value flag set to null.
            buffer.writeBytes(unspecifiedNullBytes)
            return
        }
        val objectClass: Class<*> = value.getClass()
        val serializer: TypeSerializer<T> = registry.getSerializer(objectClass) as TypeSerializer<T>
        if (serializer is CustomTypeSerializer) {
            // It's a custom type
            val customTypeSerializer: CustomTypeSerializer = serializer as CustomTypeSerializer
            buffer.writeBytes(customTypeCodeBytes)
            writeValue(customTypeSerializer.getTypeName(), buffer, false)
            customTypeSerializer.write(value, buffer, this)
            return
        }
        if (serializer is TransformSerializer) {
            // For historical reasons, there are types that need to be transformed into another type
            // before serialization, e.g., Map.Entry
            val transformSerializer: TransformSerializer<T> = serializer as TransformSerializer<T>
            write<T>(transformSerializer.transform(value), buffer)
            return
        }

        // Try to serialize the value before creating a new composite buffer
        buffer.writeBytes(serializer.getDataType().getDataTypeBuffer())
        serializer.write(value, buffer, this)
    }

    /**
     * Represents a null value of a specific type, useful when the parent type contains a type parameter that must be
     * specified.
     *
     * Note that for simple types, the provided information will be `null`.
     */
    @Throws(IOException::class)
    fun <T> writeFullyQualifiedNull(objectClass: Class<T>?, buffer: Buffer?, information: Object?) {
        val serializer: TypeSerializer<T> = registry.getSerializer(objectClass)
        serializer.write(null, buffer, this)
    }

    /**
     * Writes a single byte representing the null value_flag.
     */
    fun writeValueFlagNull(buffer: Buffer) {
        buffer.writeByte(VALUE_FLAG_NULL)
    }

    /**
     * Writes a single byte with value 0, representing an unset value_flag.
     */
    fun writeValueFlagNone(buffer: Buffer) {
        buffer.writeByte(VALUE_FLAG_NONE)
    }

    companion object {
        private const val VALUE_FLAG_NULL: Byte = 1
        private const val VALUE_FLAG_NONE: Byte = 0
        const val VERSION_BYTE = 0x81.toByte()
        private val unspecifiedNullBytes = byteArrayOf(DataType.UNSPECIFIED_NULL.getCodeByte(), 0x01)
        private val customTypeCodeBytes = byteArrayOf(DataType.CUSTOM.getCodeByte())
    }
}