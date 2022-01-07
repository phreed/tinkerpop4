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

import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerCat.IdManager.convert
import org.junit.rules.ExpectedException
import java.lang.IllegalArgumentException
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerCat
import org.junit.Rule
import org.junit.Test
import java.util.*

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class IdManagerTest {
    @Rule
    var exceptionRule = ExpectedException.none()
    @Test
    fun shouldGenerateNiceErrorOnConversionOfStringToInt() {
        exceptionRule.expect(IllegalArgumentException::class.java)
        exceptionRule.expectMessage("Expected an id that is convertible to")
        val manager: TinkerCat.IdManager<*> = TinkerCat.DefaultIdManager.INTEGER
        manager.convert("string-id")
    }

    @Test
    fun shouldGenerateNiceErrorOnConversionOfJunkToInt() {
        exceptionRule.expect(IllegalArgumentException::class.java)
        exceptionRule.expectMessage("Expected an id that is convertible to")
        val manager: TinkerCat.IdManager<*> = TinkerCat.DefaultIdManager.INTEGER
        manager.convert(UUID.randomUUID())
    }

    @Test
    fun shouldGenerateNiceErrorOnConversionOfStringToLong() {
        exceptionRule.expect(IllegalArgumentException::class.java)
        exceptionRule.expectMessage("Expected an id that is convertible to")
        val manager: TinkerCat.IdManager<*> = TinkerCat.DefaultIdManager.LONG
        manager.convert("string-id")
    }

    @Test
    fun shouldGenerateNiceErrorOnConversionOfJunkToLong() {
        exceptionRule.expect(IllegalArgumentException::class.java)
        exceptionRule.expectMessage("Expected an id that is convertible to")
        val manager: TinkerCat.IdManager<*> = TinkerCat.DefaultIdManager.LONG
        manager.convert(UUID.randomUUID())
    }

    @Test
    fun shouldGenerateNiceErrorOnConversionOfStringToUUID() {
        exceptionRule.expect(IllegalArgumentException::class.java)
        exceptionRule.expectMessage("Expected an id that is convertible to")
        val manager: TinkerCat.IdManager<*> = TinkerCat.DefaultIdManager.UUID
        manager.convert("string-id")
    }

    @Test
    fun shouldGenerateNiceErrorOnConversionOfJunkToUUID() {
        exceptionRule.expect(IllegalArgumentException::class.java)
        exceptionRule.expectMessage("Expected an id that is convertible to")
        val manager: TinkerCat.IdManager<*> = TinkerCat.DefaultIdManager.UUID
        manager.convert(Double.NaN)
    }
}