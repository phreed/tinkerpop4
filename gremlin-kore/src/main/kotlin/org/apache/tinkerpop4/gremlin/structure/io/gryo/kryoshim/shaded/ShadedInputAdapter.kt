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
package org.apache.tinkerpop4.gremlin.structure.io.gryo.kryoshim.shaded

import org.apache.tinkerpop4.gremlin.structure.io.gryo.kryoshim.InputShim
import org.apache.tinkerpop.shaded.kryo.io.Input

class ShadedInputAdapter(shadedInput: Input) : InputShim {
    private val shadedInput: Input

    init {
        this.shadedInput = shadedInput
    }

    fun getShadedInput(): Input {
        return shadedInput
    }

    @Override
    fun readShort(): Short {
        return shadedInput.readShort()
    }

    @Override
    fun readByte(): Byte {
        return shadedInput.readByte()
    }

    @Override
    fun readBytes(size: Int): ByteArray {
        return shadedInput.readBytes(size)
    }

    @Override
    fun readString(): String {
        return shadedInput.readString()
    }

    @Override
    fun readLong(): Long {
        return shadedInput.readLong()
    }

    @Override
    fun readInt(): Int {
        return shadedInput.readInt()
    }

    @Override
    fun readDouble(): Double {
        return shadedInput.readDouble()
    }

    @Override
    fun readBoolean(): Boolean {
        return shadedInput.readBoolean()
    }
}