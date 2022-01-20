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
 * Represents a serializer for a given type.
 */
interface TypeSerializer<T> {
    /**
     * Gets the [DataType] that is represented by the given [T].
     */
    val dataType: DataType?

    /**
     * Reads the type information and value from the buffer and returns an instance of T.
     */
    @Throws(IOException::class)
    fun read(buffer: Buffer?, context: GraphBinaryReader?): T

    /**
     * Reads the value from the buffer (not the type information) and returns an instance of T.
     *
     *
     * Implementors should throw an exception when a complex type doesn't support reading without the type
     * information.
     *
     */
    @Throws(IOException::class)
    fun readValue(buffer: Buffer?, context: GraphBinaryReader?, nullable: Boolean): T

    /**
     * Writes the type code, information and value to a buffer using the provided allocator.
     */
    @Throws(IOException::class)
    fun write(value: T, buffer: Buffer?, context: GraphBinaryWriter?)

    /**
     * Writes the value to a buffer, composed by the value flag and the sequence of bytes.
     */
    @Throws(IOException::class)
    fun writeValue(value: T, buffer: Buffer?, context: GraphBinaryWriter?, nullable: Boolean)
}