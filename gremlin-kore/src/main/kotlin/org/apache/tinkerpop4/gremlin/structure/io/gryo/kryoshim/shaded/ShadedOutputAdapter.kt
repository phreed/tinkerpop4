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

import org.apache.tinkerpop4.gremlin.structure.io.gryo.kryoshim.OutputShim
import org.apache.tinkerpop.shaded.kryo.io.Output

class ShadedOutputAdapter(shadedOutput: Output) : OutputShim {
    private val shadedOutput: Output

    init {
        this.shadedOutput = shadedOutput
    }

    @Override
    fun writeByte(b: Byte) {
        shadedOutput.writeByte(b)
    }

    @Override
    fun writeBytes(array: ByteArray?, offset: Int, count: Int) {
        shadedOutput.writeBytes(array, offset, count)
    }

    @Override
    fun writeString(s: String?) {
        shadedOutput.writeString(s)
    }

    @Override
    fun writeLong(l: Long) {
        shadedOutput.writeLong(l)
    }

    @Override
    fun writeInt(i: Int) {
        shadedOutput.writeInt(i)
    }

    @Override
    fun writeDouble(d: Double) {
        shadedOutput.writeDouble(d)
    }

    @Override
    fun writeShort(s: Int) {
        shadedOutput.writeShort(s)
    }

    @Override
    fun writeBoolean(b: Boolean) {
        shadedOutput.writeBoolean(b)
    }

    @Override
    fun flush() {
        shadedOutput.flush()
    }

    fun getShadedOutput(): Output {
        return shadedOutput
    }
}