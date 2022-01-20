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

/**
 * Base class for serialization of types that don't contain type specific information only {type_code}, {value_flag}
 * and {value}.
 */
abstract class SimpleTypeSerializer<T>(dataType: DataType) : TypeSerializer<T> {
    private val dataType: DataType
    fun getDataType(): DataType {
        return dataType
    }

    init {
        this.dataType = dataType
    }

    @Override
    @Throws(IOException::class)
    fun read(buffer: Buffer, context: GraphBinaryReader?): T? {
        // No {type_info}, just {value_flag}{value}
        return readValue(buffer, context, true)
    }

    @Override
    @Throws(IOException::class)
    fun readValue(buffer: Buffer, context: GraphBinaryReader?, nullable: Boolean): T? {
        if (nullable) {
            val valueFlag: Byte = buffer.readByte()
            if (valueFlag and 1 == 1) {
                return null
            }
        }
        return readValue(buffer, context)
    }

    /**
     * Reads a non-nullable value according to the type format.
     * @param buffer A buffer which reader index has been set to the beginning of the {value}.
     * @param context The binary reader.
     * @return
     * @throws IOException
     */
    @Throws(IOException::class)
    protected abstract fun readValue(buffer: Buffer?, context: GraphBinaryReader?): T
    @Override
    @Throws(IOException::class)
    fun write(value: T, buffer: Buffer?, context: GraphBinaryWriter) {
        writeValue(value, buffer, context, true)
    }

    @Override
    @Throws(IOException::class)
    fun writeValue(value: T?, buffer: Buffer?, context: GraphBinaryWriter, nullable: Boolean) {
        if (value == null) {
            if (!nullable) {
                throw IOException("Unexpected null value when nullable is false")
            }
            context.writeValueFlagNull(buffer)
            return
        }
        if (nullable) {
            context.writeValueFlagNone(buffer)
        }
        writeValue(value, buffer, context)
    }

    /**
     * Writes a non-nullable value into a buffer using the provided allocator.
     * @param value A non-nullable value.
     * @param buffer The buffer allocator to use.
     * @param context The binary writer.
     * @throws IOException
     */
    @Throws(IOException::class)
    protected abstract fun writeValue(value: T, buffer: Buffer?, context: GraphBinaryWriter?)
}