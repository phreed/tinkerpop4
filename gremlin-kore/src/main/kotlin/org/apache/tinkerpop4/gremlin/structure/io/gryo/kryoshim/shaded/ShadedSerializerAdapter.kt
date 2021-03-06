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

import org.apache.tinkerpop4.gremlin.structure.io.gryo.kryoshim.SerializerShim
import org.apache.tinkerpop.shaded.kryo.Kryo
import org.apache.tinkerpop.shaded.kryo.Serializer
import org.apache.tinkerpop.shaded.kryo.io.Input
import org.apache.tinkerpop.shaded.kryo.io.Output

class ShadedSerializerAdapter<T>(serializer: SerializerShim<T>) : Serializer<T>() {
    private val serializer: SerializerShim<T>

    init {
        this.serializer = serializer
        setImmutable(this.serializer.isImmutable())
    }

    @Override
    fun write(kryo: Kryo?, output: Output?, t: T) {
        /* These adapters could be cached pretty efficiently in instance fields if it were guaranteed that this
         * class was never subject to concurrent use.  That's true of Kryo instances, but it is not clear that
         * it is true of Serializer instances.
         */
        val shadedKryoAdapter = ShadedKryoAdapter(kryo)
        val shadedOutputAdapter = ShadedOutputAdapter(output)
        serializer.write(shadedKryoAdapter, shadedOutputAdapter, t)
    }

    @Override
    fun read(kryo: Kryo?, input: Input?, aClass: Class<T>?): T {
        // Same caching opportunity as in write(...)
        val shadedKryoAdapter = ShadedKryoAdapter(kryo)
        val shadedInputAdapter = ShadedInputAdapter(input)
        return serializer.read(shadedKryoAdapter, shadedInputAdapter, aClass)
    }

    val serializerShim: SerializerShim<T>
        get() = serializer
}