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
package org.apache.tinkerpop.gremlin.tinkercat.process.traversal.step.sideEffect

import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerCat.Companion.open
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerCat
import kotlin.Throws
import org.apache.tinkerpop.gremlin.structure.io.util.CustomId
import org.apache.tinkerpop.gremlin.TestHelper
import org.apache.tinkerpop.gremlin.process.traversal.IO
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.tinkercat.process.traversal.step.sideEffect.TinkerCatIoStepTest
import org.apache.tinkerpop.gremlin.structure.io.util.CustomId.CustomIdIoRegistry
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.lang.Exception
import java.util.*

/**
 * It was hard to test the [IO.registry] configuration as a generic test. Opted to test it as a bit of a
 * standalone test with TinkerCat.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class TinkerCatIoStepTest {
    private var graph: Graph? = null
    private var g: GraphTraversalSource? = null
    @Before
    fun setup() {
        graph = open()
        g = graph.traversal()
    }

    @Test
    @Throws(Exception::class)
    fun shouldWriteReadWithCustomIoRegistryGryo() {
        val uuid = UUID.randomUUID()
        g!!.addV("person").property("name", "stephen").property("custom", CustomId("a", uuid)).iterate()
        val file = TestHelper.generateTempFile(
            TinkerCatIoStepTest::class.java,
            "shouldWriteReadWithCustomIoRegistryGryo",
            ".kryo"
        )
        g!!.io<Any>(file.absolutePath).with(IO.registry, CustomIdIoRegistry::class.java.name).write().iterate()
        val emptyGraph: Graph = open()
        val emptyG = emptyGraph.traversal()
        try {
            emptyG.io<Any>(file.absolutePath).read().iterate()
            Assert.fail("Can't read without a registry")
        } catch (ignored: Exception) {
            // do nothing
        }
        emptyG.io<Any>(file.absolutePath).with(IO.registry, CustomIdIoRegistry.instance()).read().iterate()
        Assert.assertEquals(1, emptyG.V().has("custom", CustomId("a", uuid)).count().next().toInt().toLong())
    }

    @Test
    @Throws(Exception::class)
    fun shouldWriteReadWithCustomIoRegistryGraphSON() {
        val uuid = UUID.randomUUID()
        g!!.addV("person").property("name", "stephen").property("custom", CustomId("a", uuid)).iterate()
        val file = TestHelper.generateTempFile(
            TinkerCatIoStepTest::class.java,
            "shouldWriteReadWithCustomIoRegistryGraphSON",
            ".json"
        )
        g!!.io<Any>(file.absolutePath).with(IO.registry, CustomIdIoRegistry::class.java.name).write().iterate()
        val emptyGraph: Graph = open()
        val emptyG = emptyGraph.traversal()
        try {
            emptyG.io<Any>(file.absolutePath).read().iterate()
            Assert.fail("Can't read without a registry")
        } catch (ignored: Exception) {
            // do nothing
        }
        emptyG.io<Any>(file.absolutePath).with(IO.registry, CustomIdIoRegistry.instance()).read().iterate()
        Assert.assertEquals(1, emptyG.V().has("custom", CustomId("a", uuid)).count().next().toInt().toLong())
    }
}