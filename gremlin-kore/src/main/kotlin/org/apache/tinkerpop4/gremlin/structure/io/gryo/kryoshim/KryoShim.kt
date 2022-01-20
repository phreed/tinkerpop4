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
package org.apache.tinkerpop4.gremlin.structure.io.gryo.kryoshim

import org.apache.tinkerpop.shaded.kryo.Kryo

/**
 * A minimal [Kryo]-like abstraction.  See that class for method documentation.
 *
 * @param <I> this interface's complementary InputShim
 * @param <O> this interface's complementary OutputShim
</O></I> */
interface KryoShim<I : InputShim?, O : OutputShim?> {
    fun <T> readObject(input: I, type: Class<T>?): T
    fun readClassAndObject(input: I): Object?
    fun writeObject(output: O, `object`: Object?)
    fun writeClassAndObject(output: O, `object`: Object?)
    fun <T> readObjectOrNull(input: I, type: Class<T>?): T
    fun writeObjectOrNull(output: O, `object`: Object?, type: Class?)
}