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

import org.apache.tinkerpop4.gremlin.structure.io.gryo.kryoshim.KryoShim
import org.apache.tinkerpop.shaded.kryo.Kryo

class ShadedKryoAdapter(shadedKryo: Kryo) : KryoShim<ShadedInputAdapter?, ShadedOutputAdapter?> {
    private val shadedKryo: Kryo

    init {
        this.shadedKryo = shadedKryo
    }

    @Override
    fun <T> readObject(input: ShadedInputAdapter, type: Class<T>?): T {
        return shadedKryo.readObject(input.getShadedInput(), type)
    }

    @Override
    fun readClassAndObject(input: ShadedInputAdapter): Object {
        return shadedKryo.readClassAndObject(input.getShadedInput())
    }

    @Override
    fun writeObject(output: ShadedOutputAdapter, `object`: Object?) {
        shadedKryo.writeObject(output.getShadedOutput(), `object`)
    }

    @Override
    fun writeClassAndObject(output: ShadedOutputAdapter, `object`: Object?) {
        shadedKryo.writeClassAndObject(output.getShadedOutput(), `object`)
    }

    @Override
    fun <T> readObjectOrNull(input: ShadedInputAdapter, type: Class<T>?): T {
        return shadedKryo.readObjectOrNull(input.getShadedInput(), type)
    }

    @Override
    fun writeObjectOrNull(output: ShadedOutputAdapter, `object`: Object?, type: Class?) {
        shadedKryo.writeObjectOrNull(output.getShadedOutput(), `object`, type)
    }
}