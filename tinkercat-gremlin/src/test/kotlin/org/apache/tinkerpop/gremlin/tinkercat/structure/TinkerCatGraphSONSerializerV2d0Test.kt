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

import org.apache.tinkerpop.gremlin.process.traversal.Path
import org.apache.tinkerpop.gremlin.process.traversal.step.util.Tree
import org.apache.tinkerpop.gremlin.process.traversal.util.Metrics
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerIoRegistryV2d0.Companion.instance
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerFactory.createModern
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerCat.Companion.open
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerFactory.generateModern
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerCat.addVertex
import kotlin.Throws
import java.io.IOException
import org.apache.tinkerpop.gremlin.structure.io.GraphWriter
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerCat
import java.io.ByteArrayInputStream
import org.apache.tinkerpop.gremlin.structure.io.IoTest
import java.net.InetAddress
import java.time.LocalDateTime
import java.time.Year
import org.apache.tinkerpop.gremlin.process.traversal.util.TraversalMetrics
import org.apache.tinkerpop.gremlin.process.traversal.util.MutableMetrics
import org.apache.tinkerpop.gremlin.structure.*
import org.apache.tinkerpop.gremlin.structure.io.GraphReader
import org.apache.tinkerpop.gremlin.structure.io.Mapper
import org.apache.tinkerpop.gremlin.structure.io.graphson.*
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.time.Duration
import java.util.*

class TinkerCatGraphSONSerializerV2d0Test {
    // As of TinkerPop 3.2.1 default for GraphSON 2.0 means types enabled.
    private val defaultMapperV2d0: Mapper<*> = GraphSONMapper.build()
        .version(GraphSONVersion.V2_0)
        .addCustomModule(GraphSONXModuleV2d0.build().create(false))
        .addRegistry(instance())
        .create()
    private val noTypesMapperV2d0: Mapper<*> = GraphSONMapper.build()
        .version(GraphSONVersion.V2_0)
        .addCustomModule(GraphSONXModuleV2d0.build().create(false))
        .typeInfo(TypeInfo.NO_TYPES)
        .addRegistry(instance())
        .create()

    /**
     * Checks that the graph has been fully ser/deser with types.
     */
    @Test
    @Throws(IOException::class)
    fun shouldDeserializeGraphSONIntoTinkerCatWithPartialTypes() {
        val writer = getWriter(defaultMapperV2d0)
        val reader = getReader(defaultMapperV2d0)
        val baseModern = createModern()
        ByteArrayOutputStream().use { out ->
            writer.writeGraph(out, baseModern)
            val json = out.toString()
            val read = open()
            reader.readGraph(ByteArrayInputStream(json.toByteArray()), read)
            IoTest.assertModernGraph(read, true, false)
        }
    }

    /**
     * Checks that the graph has been fully ser/deser without types.
     */
    @Test
    @Throws(IOException::class)
    fun shouldDeserializeGraphSONIntoTinkerCatWithoutTypes() {
        val writer = getWriter(noTypesMapperV2d0)
        val reader = getReader(noTypesMapperV2d0)
        val baseModern = createModern()
        ByteArrayOutputStream().use { out ->
            writer.writeGraph(out, baseModern)
            val json = out.toString()
            val read = open()
            reader.readGraph(ByteArrayInputStream(json.toByteArray()), read)
            IoTest.assertModernGraph(read, true, false)
        }
    }

    /**
     * Thorough types verification for Vertex ids, Vertex props, Edge ids, Edge props
     */
    @Test
    @Throws(IOException::class)
    fun shouldDeserializeGraphSONIntoTinkerCatKeepingTypes() {
        val writer = getWriter(defaultMapperV2d0)
        val reader = getReader(defaultMapperV2d0)
        val sampleGraph1: Graph = createModern()
        val v1 = sampleGraph1.addVertex(T.id, 100, "name", "kevin", "theUUID", UUID.randomUUID())
        val v2 = sampleGraph1.addVertex(T.id, 101L, "name", "henri", "theUUID", UUID.randomUUID())
        v1.addEdge(
            "hello", v2, T.id, 101L,
            "uuid", UUID.randomUUID()
        )
        ByteArrayOutputStream().use { out ->
            writer.writeObject(out, sampleGraph1)
            val json = out.toString()
            val read = reader.readObject(ByteArrayInputStream(json.toByteArray()), TinkerCat::class.java)
            Assert.assertTrue(approximateGraphsCheck(sampleGraph1, read))
        }
    }

    /**
     * Asserts the approximateGraphsChecks function fails when expected. Vertex ids.
     */
    @Test
    @Throws(IOException::class)
    fun shouldLoseTypesWithGraphSONNoTypesForVertexIds() {
        val writer = getWriter(noTypesMapperV2d0)
        val reader = getReader(noTypesMapperV2d0)
        val sampleGraph1 = open()
        generateModern(sampleGraph1)
        sampleGraph1.addVertex(T.id, 100L, "name", "kevin")
        ByteArrayOutputStream().use { out ->
            writer.writeGraph(out, sampleGraph1)
            val json = out.toString()
            val read = open()
            reader.readGraph(ByteArrayInputStream(json.toByteArray()), read)
            // Should fail on deserialized vertex Id.
            Assert.assertFalse(approximateGraphsCheck(sampleGraph1, read))
        }
    }

    /**
     * Asserts the approximateGraphsChecks function fails when expected. Vertex props.
     */
    @Test
    @Throws(IOException::class)
    fun shouldLoseTypesWithGraphSONNoTypesForVertexProps() {
        val writer = getWriter(noTypesMapperV2d0)
        val reader = getReader(noTypesMapperV2d0)
        val sampleGraph1 = open()
        generateModern(sampleGraph1)
        sampleGraph1.addVertex(T.id, 100, "name", "kevin", "uuid", UUID.randomUUID())
        ByteArrayOutputStream().use { out ->
            writer.writeGraph(out, sampleGraph1)
            val json = out.toString()
            val read = open()
            reader.readGraph(ByteArrayInputStream(json.toByteArray()), read)
            // Should fail on deserialized vertex prop.
            Assert.assertFalse(approximateGraphsCheck(sampleGraph1, read))
        }
    }

    /**
     * Asserts the approximateGraphsChecks function fails when expected. Edge ids.
     */
    @Test
    @Throws(IOException::class)
    fun shouldLoseTypesWithGraphSONNoTypesForEdgeIds() {
        val writer = getWriter(noTypesMapperV2d0)
        val reader = getReader(noTypesMapperV2d0)
        val sampleGraph1 = open()
        generateModern(sampleGraph1)
        val v1 = sampleGraph1.addVertex(T.id, 100, "name", "kevin")
        v1.addEdge("hello", sampleGraph1.traversal().V().has("name", "marko").next(), T.id, 101L)
        ByteArrayOutputStream().use { out ->
            writer.writeGraph(out, sampleGraph1)
            val json = out.toString()
            val read = open()
            reader.readGraph(ByteArrayInputStream(json.toByteArray()), read)
            // Should fail on deserialized edge Id.
            Assert.assertFalse(approximateGraphsCheck(sampleGraph1, read))
        }
    }

    /**
     * Asserts the approximateGraphsChecks function fails when expected. Edge props.
     */
    @Test
    @Throws(IOException::class)
    fun shouldLoseTypesWithGraphSONNoTypesForEdgeProps() {
        val writer = getWriter(noTypesMapperV2d0)
        val reader = getReader(noTypesMapperV2d0)
        val sampleGraph1: Graph = createModern()
        val v1 = sampleGraph1.addVertex(T.id, 100, "name", "kevin")
        v1.addEdge(
            "hello", sampleGraph1.traversal().V().has("name", "marko").next(), T.id, 101,
            "uuid", UUID.randomUUID()
        )
        ByteArrayOutputStream().use { out ->
            writer.writeGraph(out, sampleGraph1)
            val json = out.toString()
            val read = open()
            reader.readGraph(ByteArrayInputStream(json.toByteArray()), read)
            // Should fail on deserialized edge prop.
            Assert.assertFalse(approximateGraphsCheck(sampleGraph1, read))
        }
    }

    /**
     * Those kinds of types are declared differently in the GraphSON type deserializer, check that all are handled
     * properly.
     */
    @Test
    @Throws(IOException::class)
    fun shouldKeepTypesWhenDeserializingSerializedTinkerCat() {
        val tg = open()
        val v = tg.addVertex("vertexTest")
        val uuidProp = UUID.randomUUID()
        val durationProp = Duration.ofHours(3)
        val longProp = 2L
        val byteBufferProp = ByteBuffer.wrap("testbb".toByteArray())
        val inetAddressProp = InetAddress.getByName("10.10.10.10")

        // One Java util type natively supported by Jackson
        v.property("uuid", uuidProp)
        // One custom time type added by the GraphSON module
        v.property("duration", durationProp)
        // One Java native type not handled by JSON natively
        v.property("long", longProp)
        // One Java util type added by GraphSON
        v.property("bytebuffer", byteBufferProp)
        v.property("inetaddress", inetAddressProp)
        val writer = getWriter(defaultMapperV2d0)
        val reader = getReader(defaultMapperV2d0)
        ByteArrayOutputStream().use { out ->
            writer.writeGraph(out, tg)
            val json = out.toString()
            val read = open()
            reader.readGraph(ByteArrayInputStream(json.toByteArray()), read)
            val vRead = read.traversal().V().hasLabel("vertexTest").next()
            Assert.assertEquals(vRead.property<Any>("uuid").value(), uuidProp)
            Assert.assertEquals(vRead.property<Any>("duration").value(), durationProp)
            Assert.assertEquals(vRead.property<Any>("long").value(), longProp)
            Assert.assertEquals(vRead.property<Any>("bytebuffer").value(), byteBufferProp)
            Assert.assertEquals(vRead.property<Any>("inetaddress").value(), inetAddressProp)
        }
    }

    @Test
    fun deserializersTestsVertex() {
        val tg = open()
        val v = tg.addVertex("vertexTest")
        v.property("born", LocalDateTime.of(1971, 1, 2, 20, 50))
        v.property("dead", LocalDateTime.of(1971, 1, 7, 20, 50))
        val writer = getWriter(defaultMapperV2d0)
        val reader = getReader(defaultMapperV2d0)
        try {
            ByteArrayOutputStream().use { out ->
                writer.writeObject(out, v)
                val json = out.toString()

                // Object works, because there's a type in the payload now
                // Vertex.class would work as well
                // Anything else would not because we check the type in param here with what's in the JSON, for safety.
                val vRead = reader.readObject(ByteArrayInputStream(json.toByteArray()), Any::class.java) as Vertex
                Assert.assertEquals(v, vRead)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Assert.fail("Should not have thrown exception: " + e.message)
        }
    }

    @Test
    fun deserializersTestsEdge() {
        val tg = open()
        val v = tg.addVertex("vertexTest")
        val v2 = tg.addVertex("vertexTest")
        val ed = v.addEdge("knows", v2, "time", LocalDateTime.now())
        val writer = getWriter(defaultMapperV2d0)
        val reader = getReader(defaultMapperV2d0)
        try {
            ByteArrayOutputStream().use { out ->
                writer.writeObject(out, ed)
                val json = out.toString()

                // Object works, because there's a type in the payload now
                // Edge.class would work as well
                // Anything else would not because we check the type in param here with what's in the JSON, for safety.
                val eRead = reader.readObject(ByteArrayInputStream(json.toByteArray()), Any::class.java) as Edge
                Assert.assertEquals(ed, eRead)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Assert.fail("Should not have thrown exception: " + e.message)
        }
    }

    @Test
    fun deserializersTestsTinkerCat() {
        val tg = open()
        val v = tg.addVertex("vertexTest")
        val v2 = tg.addVertex("vertexTest")
        v.addEdge("knows", v2)
        val writer = getWriter(defaultMapperV2d0)
        val reader = getReader(defaultMapperV2d0)
        try {
            ByteArrayOutputStream().use { out ->
                writer.writeObject(out, tg)
                val json = out.toString()
                val gRead = reader.readObject(ByteArrayInputStream(json.toByteArray()), Any::class.java) as Graph
                Assert.assertTrue(approximateGraphsCheck(tg, gRead))
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Assert.fail("Should not have thrown exception: " + e.message)
        }
    }

    @Test
    fun deserializersTestsProperty() {
        val tg = open()
        val v = tg.addVertex("vertexTest")
        val v2 = tg.addVertex("vertexTest")
        val ed = v.addEdge("knows", v2)
        val writer = getWriter(defaultMapperV2d0)
        val reader = getReader(defaultMapperV2d0)
        val prop: Property<*> = ed.property("since", Year.parse("1993"))
        try {
            ByteArrayOutputStream().use { out ->
                writer.writeObject(out, prop)
                val json = out.toString()
                val pRead = reader.readObject(ByteArrayInputStream(json.toByteArray()), Any::class.java) as Property<*>
                //can't use equals here, because pRead is detached, its parent element has not been intentionally
                //serialized and "equals()" checks that.
                Assert.assertTrue(prop.key() == pRead.key() && prop.value() == pRead.value())
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Assert.fail("Should not have thrown exception: " + e.message)
        }
    }

    @Test
    fun deserializersTestsVertexProperty() {
        val tg = open()
        val v = tg.addVertex("vertexTest")
        val writer = getWriter(defaultMapperV2d0)
        val reader = getReader(defaultMapperV2d0)
        val prop: VertexProperty<*> = v.property("born", LocalDateTime.of(1971, 1, 2, 20, 50))
        try {
            ByteArrayOutputStream().use { out ->
                writer.writeObject(out, prop)
                val json = out.toString()
                val vPropRead =
                    reader.readObject(ByteArrayInputStream(json.toByteArray()), Any::class.java) as VertexProperty<*>
                //only classes and ids are checked, that's ok, full vertex property ser/de
                //is checked elsewhere.
                Assert.assertEquals(prop, vPropRead)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Assert.fail("Should not have thrown exception: " + e.message)
        }
    }

    @Test
    fun deserializersTestsPath() {
        val tg = createModern()
        val writer = getWriter(defaultMapperV2d0)
        val reader = getReader(defaultMapperV2d0)
        val p = tg.traversal().V(1).`as`("a").has("name").`as`("b").out("knows").out("created").`as`("c")
            .has("name", "ripple").values<Any>("name").`as`("d").identity().`as`("e").path().next()
        try {
            ByteArrayOutputStream().use { out ->
                writer.writeObject(out, p)
                val json = out.toString()
                val pathRead = reader.readObject(ByteArrayInputStream(json.toByteArray()), Any::class.java) as Path
                for (i in p.objects().indices) {
                    val o = p.objects()[i]
                    val oRead = pathRead.objects()[i]
                    Assert.assertEquals(o, oRead)
                }
                for (i in p.labels().indices) {
                    val o = p.labels()[i]
                    val oRead = pathRead.labels()[i]
                    Assert.assertEquals(o, oRead)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Assert.fail("Should not have thrown exception: " + e.message)
        }
    }

    @Test
    fun deserializersTestsMetrics() {
        val tg = createModern()
        val writer = getWriter(defaultMapperV2d0)
        val reader = getReader(defaultMapperV2d0)
        val tm = tg.traversal().V(1).`as`("a").has("name").`as`("b").out("knows").out("created").`as`("c")
            .has("name", "ripple").values<Any>("name").`as`("d").identity().`as`("e").profile().next()
        val m = MutableMetrics(tm.getMetrics(0))
        // making sure nested metrics are included in serde
        m.addNested(MutableMetrics(tm.getMetrics(1)))
        try {
            ByteArrayOutputStream().use { out ->
                writer.writeObject(out, m)
                val json = out.toString()
                val metricsRead =
                    reader.readObject(ByteArrayInputStream(json.toByteArray()), Any::class.java) as Metrics
                // toString should be enough to compare Metrics
                Assert.assertTrue(m.toString() == metricsRead.toString())
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Assert.fail("Should not have thrown exception: " + e.message)
        }
    }

    @Test
    fun deserializersTestsTraversalMetrics() {
        val tg = createModern()
        val writer = getWriter(defaultMapperV2d0)
        val reader = getReader(defaultMapperV2d0)
        val tm = tg.traversal().V(1).`as`("a").has("name").`as`("b").out("knows").out("created").`as`("c")
            .has("name", "ripple").values<Any>("name").`as`("d").identity().`as`("e").profile().next()
        try {
            ByteArrayOutputStream().use { out ->
                writer.writeObject(out, tm)
                val json = out.toString()
                val traversalMetricsRead =
                    reader.readObject(ByteArrayInputStream(json.toByteArray()), Any::class.java) as TraversalMetrics
                // toString should be enough to compare TraversalMetrics
                Assert.assertTrue(tm.toString() == traversalMetricsRead.toString())
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Assert.fail("Should not have thrown exception: " + e.message)
        }
    }

    @Test
    @Ignore("https://issues.apache.org/jira/browse/TINKERPOP-1509")
    fun deserializersTestsTree() {
        val tg = createModern()
        val writer = getWriter(defaultMapperV2d0)
        val reader = getReader(defaultMapperV2d0)
        val t = tg.traversal().V().out().out().tree().next()
        try {
            ByteArrayOutputStream().use { out ->
                writer.writeObject(out, t)
                val json = out.toString()
                val treeRead = reader.readObject(ByteArrayInputStream(json.toByteArray()), Any::class.java) as Tree<*>
                //Map's equals should check each component of the tree recursively
                //on each it will call "equals()" which for Vertices will compare ids, which
                //is ok. Complete vertex deser is checked elsewhere.
                Assert.assertEquals(t, treeRead)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Assert.fail("Should not have thrown exception: " + e.message)
        }
    }

    private fun getWriter(paramMapper: Mapper<*>): GraphWriter {
        return GraphSONWriter.build().mapper(paramMapper).create()
    }

    private fun getReader(paramMapper: Mapper<*>): GraphReader {
        return GraphSONReader.build().mapper(paramMapper).create()
    }

    /**
     * Checks sequentially vertices and edges of both graphs. Will check sequentially Vertex IDs, Vertex Properties IDs
     * and values and classes. Then same for edges. To use when serializing a Graph and deserializing the supposedly
     * same Graph.
     */
    private fun approximateGraphsCheck(g1: Graph, g2: Graph): Boolean {
        val itV = g1.vertices()
        val itVRead = g2.vertices()
        while (itV.hasNext()) {
            val v = itV.next()
            val vRead = itVRead.next()

            // Will only check IDs but that's 'good' enough.
            if (v != vRead) {
                return false
            }
            val itVP: Iterator<*> = v.properties<Any>()
            val itVPRead: Iterator<*> = vRead.properties<Any>()
            while (itVP.hasNext()) {
                val vp = itVP.next() as VertexProperty<*>
                val vpRead = itVPRead.next() as VertexProperty<*>
                if (vp.value() != vpRead.value()
                    || vp != vpRead
                ) {
                    return false
                }
            }
        }
        val itE = g1.edges()
        val itERead = g2.edges()
        while (itE.hasNext()) {
            val e = itE.next()
            val eRead = itERead.next()
            // Will only check IDs but that's good enough.
            if (e != eRead) {
                return false
            }
            val itEP: Iterator<*> = e.properties<Any>()
            val itEPRead: Iterator<*> = eRead.properties<Any>()
            while (itEP.hasNext()) {
                val ep = itEP.next() as Property<*>
                val epRead = itEPRead.next() as Property<*>
                if (ep.value() != epRead.value()
                    || ep != epRead
                ) {
                    return false
                }
            }
        }
        return true
    }
}