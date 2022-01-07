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
package org.apache.tinkerpop.gremlin.tinkercat.structure

import org.apache.tinkerpop.shaded.kryo.Kryo
import org.junit.Before
import org.junit.Test
import org.mockito.runners.MockitoJUnitRunner
import java.lang.Exception

/**
 * Unit tests for [TinkerIoRegistryV2d0.TinkerCatGryoSerializer].
 */
@RunWith(MockitoJUnitRunner::class)
class TinkerCatGryoSerializerV2d0Test {
    @Mock
    private val kryo: Kryo? = null

    @Mock
    private val registration: Registration? = null

    @Mock
    private val output: Output? = null

    @Mock
    private val input: Input? = null
    private val graph = TinkerCat.open()
    private val serializer = TinkerIoRegistryV2d0.TinkerCatGryoSerializer()
    @Before
    @Throws(Exception::class)
    fun setUp() {
        Mockito.`when`(kryo.getRegistration(ArgumentMatchers.any<Any>() as Class<*>)).thenReturn(registration)
        Mockito.`when`(input.readBytes(ArgumentMatchers.anyInt())).thenReturn(Arrays.copyOf(GryoMapper.HEADER, 100))
    }

    @Test
    @Throws(Exception::class)
    fun shouldVerifyKryoUsedForWrite() {
        serializer.write(kryo, output, graph)
        Mockito.verify<Any>(kryo, Mockito.atLeastOnce()).getRegistration(ArgumentMatchers.any<Any>() as Class<*>)
    }

    @Test
    @Throws(Exception::class)
    fun shouldVerifyKryoUsedForRead() {
        // Not possible to mock an entire deserialization so just verify the same kryo instances are being used
        try {
            serializer.read(kryo, input, TinkerCat::class.java)
        } catch (ex: RuntimeException) {
            Mockito.verify<Any>(kryo, Mockito.atLeastOnce()).readObject(ArgumentMatchers.any(), ArgumentMatchers.any())
            Mockito.verify<Any>(kryo, Mockito.atLeastOnce()).readClassAndObject(ArgumentMatchers.any())
        }
    }
}