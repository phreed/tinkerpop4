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
 * Represents a serializer for types that can be represented as a single value and that can be read and write
 * in a single operation.
 */
class SingleTypeSerializer<T> private constructor(
    dataType: DataType, readFunc: Function<Buffer, T>,
    writeFunc: BiConsumer<T, Buffer>
) : SimpleTypeSerializer<T>(dataType) {
    private val readFunc: Function<Buffer, T>
    private val writeFunc: BiConsumer<T, Buffer>

    init {
        this.readFunc = readFunc
        this.writeFunc = writeFunc
    }

    @Override
    fun readValue(buffer: Buffer?, context: GraphBinaryReader?): T {
        return readFunc.apply(buffer)
    }

    @Override
    protected fun writeValue(value: T, buffer: Buffer?, context: GraphBinaryWriter?) {
        writeFunc.accept(value, buffer)
    }

    companion object {
        val IntSerializer: SingleTypeSerializer<Integer> =
            SingleTypeSerializer<Any>(DataType.INT, Buffer::readInt, BiConsumer<T, Buffer> { v, b -> b.writeInt(v) })
        val LongSerializer: SingleTypeSerializer<Long> =
            SingleTypeSerializer<Any>(DataType.LONG, Buffer::readLong, BiConsumer<T, Buffer> { v, b -> b.writeLong(v) })
        val DoubleSerializer: SingleTypeSerializer<Double> = SingleTypeSerializer<Any>(
            DataType.DOUBLE,
            Buffer::readDouble,
            BiConsumer<T, Buffer> { v, b -> b.writeDouble(v) })
        val FloatSerializer: SingleTypeSerializer<Float> = SingleTypeSerializer<Any>(
            DataType.FLOAT,
            Buffer::readFloat,
            BiConsumer<T, Buffer> { v, b -> b.writeFloat(v) })
        val ShortSerializer: SingleTypeSerializer<Short> = SingleTypeSerializer<Any>(
            DataType.SHORT,
            Buffer::readShort,
            BiConsumer<T, Buffer> { v, b -> b.writeShort(v) })
        val BooleanSerializer: SingleTypeSerializer<Boolean> = SingleTypeSerializer<Any>(
            DataType.BOOLEAN,
            Buffer::readBoolean,
            BiConsumer<T, Buffer> { v, b -> b.writeBoolean(v) })
        val ByteSerializer: SingleTypeSerializer<Byte> =
            SingleTypeSerializer<Any>(DataType.BYTE, Buffer::readByte, BiConsumer<T, Buffer> { v, b -> b.writeByte(v) })
        val YearSerializer: SingleTypeSerializer<Year> = SingleTypeSerializer<Any>(
            DataType.YEAR,
            Function<Buffer, T> { bb -> Year.of(bb.readInt()) },
            BiConsumer<T, Buffer> { v, b -> b.writeInt(v.getValue()) })
    }
}