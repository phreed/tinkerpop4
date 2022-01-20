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

class CharSerializer : SimpleTypeSerializer<Character?>(DataType.CHAR) {
    @Override
    @Throws(IOException::class)
    protected fun readValue(buffer: Buffer, context: GraphBinaryReader?): Character {
        val firstByte: Int = buffer.readByte() and 0xff
        var byteLength = 1
        // A byte with the first byte ON (10000000) signals that more bytes are needed to represent the UTF-8 char
        if (firstByte and 0x80 > 0) {
            if (firstByte and 0xf0 == 0xf0) { // 0xf0 = 11110000
                byteLength = 4
            } else if (firstByte and 0xe0 == 0xe0) { //11100000
                byteLength = 3
            } else if (firstByte and 0xc0 == 0xc0) { //11000000
                byteLength = 2
            }
        }
        val byteArray: ByteArray
        if (byteLength == 1) {
            byteArray = byteArrayOf(firstByte.toByte())
        } else {
            byteArray = ByteArray(byteLength)
            byteArray[0] = firstByte.toByte()
            buffer.readBytes(byteArray, 1, byteLength - 1)
        }
        return String(byteArray, StandardCharsets.UTF_8).charAt(0)
    }

    @Override
    @Throws(IOException::class)
    protected fun writeValue(value: Character?, buffer: Buffer, context: GraphBinaryWriter?) {
        val stringValue: String = Character.toString(value)
        buffer.writeBytes(stringValue.getBytes(StandardCharsets.UTF_8))
    }
}