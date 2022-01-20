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
import org.apache.tinkerpop4.gremlin.structure.io.binary.GraphBinaryReader
import org.apache.tinkerpop4.gremlin.structure.io.binary.GraphBinaryWriter
import org.apache.tinkerpop4.gremlin.structure.io.Buffer
import java.sql.Timestamp
import java.util.Date
import java.util.function.Function

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class DateSerializer<T : Date?> private constructor(type: DataType, reader: Function<Long, T>) :
    SimpleTypeSerializer<T>(type) {
    private val reader: Function<Long, T>

    init {
        this.reader = reader
    }

    @Override
    protected fun readValue(buffer: Buffer, context: GraphBinaryReader?): T {
        return reader.apply(buffer.readLong())
    }

    @Override
    protected fun writeValue(value: T, buffer: Buffer, context: GraphBinaryWriter?) {
        buffer.writeLong(value.getTime())
    }

    companion object {
        val DateSerializer: DateSerializer<Date> = DateSerializer<Date>(DataType.DATE, Function<Long, T> { Date() })
        val TimestampSerializer: DateSerializer<Timestamp> =
            DateSerializer<Date>(DataType.TIMESTAMP, Function<Long, T> { Timestamp() })
    }
}