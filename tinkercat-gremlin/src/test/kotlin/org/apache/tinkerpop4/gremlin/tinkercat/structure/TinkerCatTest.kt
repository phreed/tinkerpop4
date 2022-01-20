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
package org.apache.tinkerpop4.gremlin.tinkercat.structure

import org.apache.commons.configuration2.BaseConfiguration
import org.apache.commons.configuration2.Configuration
import org.apache.tinkerpop4.gremlin.process.computer.Computer
import org.apache.tinkerpop4.gremlin.process.traversal.P
import org.apache.tinkerpop4.gremlin.process.traversal.Traversal
import org.apache.tinkerpop4.gremlin.process.traversal.dsl.graph.__
import org.apache.tinkerpop4.gremlin.structure.Edge
import org.apache.tinkerpop4.gremlin.structure.Graph
import org.apache.tinkerpop4.gremlin.structure.Vertex
import org.apache.tinkerpop4.gremlin.structure.io.GraphReader
import org.apache.tinkerpop4.gremlin.structure.io.Mapper
import org.apache.tinkerpop4.gremlin.structure.io.graphson.TypeInfo
import org.apache.tinkerpop4.gremlin.util.iterator.IteratorUtils
import org.apache.tinkerpop.shaded.jackson.databind.ObjectMapper
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.hamcrest.core.StringContains
import org.junit.Assert
import org.junit.Test
import java.awt.Color
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.Exception
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import java.util.function.Supplier

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class TinkerCatTest {
    @Test
    fun shouldManageIndices() {
        val g = TinkerCat.open()
        var keys = g.getIndexedKeys(
            Vertex::class.java
        )
        Assert.assertEquals(0, keys.size.toLong())
        keys = g.getIndexedKeys(Edge::class.java)
        Assert.assertEquals(0, keys.size.toLong())
        g.createIndex("name1", Vertex::class.java)
        g.createIndex("name2", Vertex::class.java)
        g.createIndex("oid1", Edge::class.java)
        g.createIndex("oid2", Edge::class.java)

        // add the same one twice to check idempotency
        g.createIndex("name1", Vertex::class.java)
        keys = g.getIndexedKeys(Vertex::class.java)
        Assert.assertEquals(2, keys.size.toLong())
        for (k in keys) {
            Assert.assertTrue(k == "name1" || k == "name2")
        }
        keys = g.getIndexedKeys(Edge::class.java)
        Assert.assertEquals(2, keys.size.toLong())
        for (k in keys) {
            Assert.assertTrue(k == "oid1" || k == "oid2")
        }
        g.dropIndex("name2", Vertex::class.java)
        keys = g.getIndexedKeys(Vertex::class.java)
        Assert.assertEquals(1, keys.size.toLong())
        Assert.assertEquals("name1", keys.iterator().next())
        g.dropIndex("name1", Vertex::class.java)
        keys = g.getIndexedKeys(Vertex::class.java)
        Assert.assertEquals(0, keys.size.toLong())
        g.dropIndex("oid1", Edge::class.java)
        keys = g.getIndexedKeys(Edge::class.java)
        Assert.assertEquals(1, keys.size.toLong())
        Assert.assertEquals("oid2", keys.iterator().next())
        g.dropIndex("oid2", Edge::class.java)
        keys = g.getIndexedKeys(Edge::class.java)
        Assert.assertEquals(0, keys.size.toLong())
        g.dropIndex("better-not-error-index-key-does-not-exist", Vertex::class.java)
        g.dropIndex("better-not-error-index-key-does-not-exist", Edge::class.java)
    }

    @Test(expected = IllegalArgumentException::class)
    fun shouldNotCreateVertexIndexWithNullKey() {
        val g = TinkerCat.open()
        g.createIndex(null, Vertex::class.java)
    }

    @Test(expected = IllegalArgumentException::class)
    fun shouldNotCreateEdgeIndexWithNullKey() {
        val g = TinkerCat.open()
        g.createIndex(null, Edge::class.java)
    }

    @Test(expected = IllegalArgumentException::class)
    fun shouldNotCreateVertexIndexWithEmptyKey() {
        val g = TinkerCat.open()
        g.createIndex("", Vertex::class.java)
    }

    @Test(expected = IllegalArgumentException::class)
    fun shouldNotCreateEdgeIndexWithEmptyKey() {
        val g = TinkerCat.open()
        g.createIndex("", Edge::class.java)
    }

    @Test
    fun shouldUpdateVertexIndicesInNewGraph() {
        val g = TinkerCat.open()
        g.createIndex("name", Vertex::class.java)
        g.addVertex("name", "marko", "age", 29)
        g.addVertex("name", "stephen", "age", 35)

        // a tricky way to evaluate if indices are actually being used is to pass a fake BiPredicate to has()
        // to get into the Pipeline and evaluate what's going through it.  in this case, we know that at index
        // is used because only "stephen" ages should pass through the pipeline due to the inclusion of the
        // key index lookup on "name".  If there's an age of something other than 35 in the pipeline being evaluated
        // then something is wrong.
        Assert.assertEquals(
            1, g.traversal().V().has("age", P.test(
                BiPredicate<*, *> { t: Any?, u: Any? ->
                    Assert.assertEquals(35, t)
                    true
                }, 35
            )
            ).has("name", "stephen").count().next()
        )
    }

    @Test
    fun shouldRemoveAVertexFromAnIndex() {
        val g = TinkerCat.open()
        g.createIndex("name", Vertex::class.java)
        g.addVertex("name", "marko", "age", 29)
        g.addVertex("name", "stephen", "age", 35)
        val v = g.addVertex("name", "stephen", "age", 35)

        // a tricky way to evaluate if indices are actually being used is to pass a fake BiPredicate to has()
        // to get into the Pipeline and evaluate what's going through it.  in this case, we know that at index
        // is used because only "stephen" ages should pass through the pipeline due to the inclusion of the
        // key index lookup on "name".  If there's an age of something other than 35 in the pipeline being evaluated
        // then something is wrong.
        Assert.assertEquals(
            2, g.traversal().V().has("age", P.test(
                BiPredicate<*, *> { t: Any?, u: Any? ->
                    Assert.assertEquals(35, t)
                    true
                }, 35
            )
            ).has("name", "stephen").count().next()
        )
        v.remove()
        Assert.assertEquals(
            1, g.traversal().V().has("age", P.test(
                BiPredicate<*, *> { t: Any?, u: Any? ->
                    Assert.assertEquals(35, t)
                    true
                }, 35
            )
            ).has("name", "stephen").count().next()
        )
    }

    @Test
    fun shouldUpdateVertexIndicesInExistingGraph() {
        val g = TinkerCat.open()
        g.addVertex("name", "marko", "age", 29)
        g.addVertex("name", "stephen", "age", 35)

        // a tricky way to evaluate if indices are actually being used is to pass a fake BiPredicate to has()
        // to get into the Pipeline and evaluate what's going through it.  in this case, we know that at index
        // is not used because "stephen" and "marko" ages both pass through the pipeline.
        Assert.assertEquals(
            1, g.traversal().V().has("age", P.test(
                BiPredicate<*, *> { t: Any, u: Any? ->
                    Assert.assertTrue(t == 35 || t == 29)
                    true
                }, 35
            )
            ).has("name", "stephen").count().next()
        )
        g.createIndex("name", Vertex::class.java)

        // another spy into the pipeline for index check.  in this case, we know that at index
        // is used because only "stephen" ages should pass through the pipeline due to the inclusion of the
        // key index lookup on "name".  If there's an age of something other than 35 in the pipeline being evaluated
        // then something is wrong.
        Assert.assertEquals(
            1, g.traversal().V().has("age", P.test(
                BiPredicate<*, *> { t: Any?, u: Any? ->
                    Assert.assertEquals(35, t)
                    true
                }, 35
            )
            ).has("name", "stephen").count().next()
        )
    }

    @Test
    fun shouldUpdateEdgeIndicesInNewGraph() {
        val g = TinkerCat.open()
        g.createIndex("oid", Edge::class.java)
        val v = g.addVertex()
        v.addEdge("friend", v, "oid", "1", "weight", 0.5f)
        v.addEdge("friend", v, "oid", "2", "weight", 0.6f)

        // a tricky way to evaluate if indices are actually being used is to pass a fake BiPredicate to has()
        // to get into the Pipeline and evaluate what's going through it.  in this case, we know that at index
        // is used because only oid 1 should pass through the pipeline due to the inclusion of the
        // key index lookup on "oid".  If there's an weight of something other than 0.5f in the pipeline being
        // evaluated then something is wrong.
        Assert.assertEquals(
            1, g.traversal().E().has("weight", P.test(
                BiPredicate<*, *> { t: Any?, u: Any? ->
                    Assert.assertEquals(0.5f, t)
                    true
                }, 0.5
            )
            ).has("oid", "1").count().next()
        )
    }

    @Test
    fun shouldRemoveEdgeFromAnIndex() {
        val g = TinkerCat.open()
        g.createIndex("oid", Edge::class.java)
        val v = g.addVertex()
        v.addEdge("friend", v, "oid", "1", "weight", 0.5f)
        val e = v.addEdge("friend", v, "oid", "1", "weight", 0.5f)
        v.addEdge("friend", v, "oid", "2", "weight", 0.6f)

        // a tricky way to evaluate if indices are actually being used is to pass a fake BiPredicate to has()
        // to get into the Pipeline and evaluate what's going through it.  in this case, we know that at index
        // is used because only oid 1 should pass through the pipeline due to the inclusion of the
        // key index lookup on "oid".  If there's an weight of something other than 0.5f in the pipeline being
        // evaluated then something is wrong.
        Assert.assertEquals(
            2, g.traversal().E().has("weight", P.test(
                BiPredicate<*, *> { t: Any?, u: Any? ->
                    Assert.assertEquals(0.5f, t)
                    true
                }, 0.5
            )
            ).has("oid", "1").count().next()
        )
        e.remove()
        Assert.assertEquals(
            1, g.traversal().E().has("weight", P.test(
                BiPredicate<*, *> { t: Any?, u: Any? ->
                    Assert.assertEquals(0.5f, t)
                    true
                }, 0.5
            )
            ).has("oid", "1").count().next()
        )
    }

    @Test
    fun shouldUpdateEdgeIndicesInExistingGraph() {
        val g = TinkerCat.open()
        val v = g.addVertex()
        v.addEdge("friend", v, "oid", "1", "weight", 0.5f)
        v.addEdge("friend", v, "oid", "2", "weight", 0.6f)

        // a tricky way to evaluate if indices are actually being used is to pass a fake BiPredicate to has()
        // to get into the Pipeline and evaluate what's going through it.  in this case, we know that at index
        // is not used because "1" and "2" weights both pass through the pipeline.
        Assert.assertEquals(
            1, g.traversal().E().has("weight", P.test(
                BiPredicate<*, *> { t: Any, u: Any? ->
                    Assert.assertTrue(t == 0.5f || t == 0.6f)
                    true
                }, 0.5
            )
            ).has("oid", "1").count().next()
        )
        g.createIndex("oid", Edge::class.java)

        // another spy into the pipeline for index check.  in this case, we know that at index
        // is used because only oid 1 should pass through the pipeline due to the inclusion of the
        // key index lookup on "oid".  If there's an weight of something other than 0.5f in the pipeline being
        // evaluated then something is wrong.
        Assert.assertEquals(
            1, g.traversal().E().has("weight", P.test(
                BiPredicate<*, *> { t: Any?, u: Any? ->
                    Assert.assertEquals(0.5f, t)
                    true
                }, 0.5
            )
            ).has("oid", "1").count().next()
        )
    }

    @Test
    @Throws(Exception::class)
    fun shouldSerializeTinkerCatToGryo() {
        val graph = TinkerFactory.createModern()
        ByteArrayOutputStream().use { out ->
            graph.io<GryoIo>(IoCore.gryo()).writer().create().writeObject(out, graph)
            val b = out.toByteArray()
            ByteArrayInputStream(b).use { inputStream ->
                val target: TinkerCat = graph.io<GryoIo>(IoCore.gryo()).reader().create()
                    .readObject<TinkerCat>(inputStream, TinkerCat::class.java)
                IoTest.assertModernGraph(target, true, false)
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun shouldSerializeTinkerCatWithMultiPropertiesToGryo() {
        val graph = TinkerFactory.createTheCrew()
        ByteArrayOutputStream().use { out ->
            graph.io<GryoIo>(IoCore.gryo()).writer().create().writeObject(out, graph)
            val b = out.toByteArray()
            ByteArrayInputStream(b).use { inputStream ->
                val target: TinkerCat = graph.io<GryoIo>(IoCore.gryo()).reader().create()
                    .readObject<TinkerCat>(inputStream, TinkerCat::class.java)
                IoTest.assertCrewGraph(target, false)
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun shouldSerializeTinkerCatToGraphSON() {
        val graph = TinkerFactory.createModern()
        ByteArrayOutputStream().use { out ->
            graph.io<GraphSONIo>(IoCore.graphson()).writer().create().writeObject(out, graph)
            ByteArrayInputStream(out.toByteArray()).use { inputStream ->
                val target: TinkerCat = graph.io<GraphSONIo>(IoCore.graphson()).reader().create()
                    .readObject<TinkerCat>(inputStream, TinkerCat::class.java)
                IoTest.assertModernGraph(target, true, false)
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun shouldSerializeTinkerCatWithMultiPropertiesToGraphSON() {
        val graph = TinkerFactory.createTheCrew()
        ByteArrayOutputStream().use { out ->
            graph.io<GraphSONIo>(IoCore.graphson()).writer().create().writeObject(out, graph)
            ByteArrayInputStream(out.toByteArray()).use { inputStream ->
                val target: TinkerCat = graph.io<GraphSONIo>(IoCore.graphson()).reader().create()
                    .readObject<TinkerCat>(inputStream, TinkerCat::class.java)
                IoTest.assertCrewGraph(target, false)
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun shouldSerializeTinkerCatToGraphSONWithTypes() {
        val graph = TinkerFactory.createModern()
        val mapper: Mapper<ObjectMapper> =
            graph.io<GraphSONIo>(IoCore.graphson()).mapper().typeInfo(TypeInfo.PARTIAL_TYPES).create()
        ByteArrayOutputStream().use { out ->
            val writer: GraphWriter = GraphSONWriter.build().mapper(mapper).create()
            writer.writeObject(out, graph)
            ByteArrayInputStream(out.toByteArray()).use { inputStream ->
                val reader: GraphReader = GraphSONReader.build().mapper(mapper).create()
                val target = reader.readObject(inputStream, TinkerCat::class.java)
                IoTest.assertModernGraph(target, true, false)
            }
        }
    }

    @Test(expected = IllegalStateException::class)
    fun shouldRequireGraphLocationIfFormatIsSet() {
        val conf: Configuration = BaseConfiguration()
        conf.setProperty(TinkerCat.GREMLIN_TINKERGRAPH_GRAPH_FORMAT, "graphml")
        TinkerCat.open(conf)
    }

    @Test(expected = IllegalStateException::class)
    fun shouldNotModifyAVertexThatWasRemoved() {
        val graph = TinkerCat.open()
        val v = graph.addVertex()
        v.property("name", "stephen")
        Assert.assertEquals("stephen", v.value("name"))
        v.remove()
        v.property("status", 1)
    }

    @Test(expected = IllegalStateException::class)
    fun shouldNotAddEdgeToAVertexThatWasRemoved() {
        val graph = TinkerCat.open()
        val v = graph.addVertex()
        v.property("name", "stephen")
        Assert.assertEquals("stephen", v.value("name"))
        v.remove()
        v.addEdge("self", v)
    }

    @Test(expected = IllegalStateException::class)
    fun shouldNotReadValueOfPropertyOnVertexThatWasRemoved() {
        val graph = TinkerCat.open()
        val v = graph.addVertex()
        v.property("name", "stephen")
        Assert.assertEquals("stephen", v.value("name"))
        v.remove()
        v.value<Any>("name")
    }

    @Test(expected = IllegalStateException::class)
    fun shouldRequireGraphFormatIfLocationIsSet() {
        val conf: Configuration = BaseConfiguration()
        conf.setProperty(
            TinkerCat.GREMLIN_TINKERGRAPH_GRAPH_LOCATION,
            TestHelper.makeTestDataDirectory(TinkerCatTest::class.java)
        )
        TinkerCat.open(conf)
    }

    @Test
    fun shouldPersistToGraphML() {
        val graphLocation: String = TestHelper.makeTestDataFile(TinkerCatTest::class.java, "shouldPersistToGraphML.xml")
        val f = File(graphLocation)
        if (f.exists() && f.isFile) f.delete()
        val conf: Configuration = BaseConfiguration()
        conf.setProperty(TinkerCat.GREMLIN_TINKERGRAPH_GRAPH_FORMAT, "graphml")
        conf.setProperty(TinkerCat.GREMLIN_TINKERGRAPH_GRAPH_LOCATION, graphLocation)
        val graph = TinkerCat.open(conf)
        TinkerFactory.generateModern(graph)
        graph.close()
        val reloadedGraph = TinkerCat.open(conf)
        IoTest.assertModernGraph(reloadedGraph, true, true)
        reloadedGraph.close()
    }

    @Test
    fun shouldPersistToGraphSON() {
        val graphLocation: String =
            TestHelper.makeTestDataFile(TinkerCatTest::class.java, "shouldPersistToGraphSON.json")
        val f = File(graphLocation)
        if (f.exists() && f.isFile) f.delete()
        val conf: Configuration = BaseConfiguration()
        conf.setProperty(TinkerCat.GREMLIN_TINKERGRAPH_GRAPH_FORMAT, "graphson")
        conf.setProperty(TinkerCat.GREMLIN_TINKERGRAPH_GRAPH_LOCATION, graphLocation)
        val graph = TinkerCat.open(conf)
        TinkerFactory.generateModern(graph)
        graph.close()
        val reloadedGraph = TinkerCat.open(conf)
        IoTest.assertModernGraph(reloadedGraph, true, false)
        reloadedGraph.close()
    }

    @Test
    fun shouldPersistToGryo() {
        val graphLocation: String = TestHelper.makeTestDataFile(TinkerCatTest::class.java, "shouldPersistToGryo.kryo")
        val f = File(graphLocation)
        if (f.exists() && f.isFile) f.delete()
        val conf: Configuration = BaseConfiguration()
        conf.setProperty(TinkerCat.GREMLIN_TINKERGRAPH_GRAPH_FORMAT, "gryo")
        conf.setProperty(TinkerCat.GREMLIN_TINKERGRAPH_GRAPH_LOCATION, graphLocation)
        val graph = TinkerCat.open(conf)
        TinkerFactory.generateModern(graph)
        graph.close()
        val reloadedGraph = TinkerCat.open(conf)
        IoTest.assertModernGraph(reloadedGraph, true, false)
        reloadedGraph.close()
    }

    @Test
    fun shouldPersistToGryoAndHandleMultiProperties() {
        val graphLocation: String =
            TestHelper.makeTestDataFile(TinkerCatTest::class.java, "shouldPersistToGryoMulti.kryo")
        val f = File(graphLocation)
        if (f.exists() && f.isFile) f.delete()
        val conf: Configuration = BaseConfiguration()
        conf.setProperty(TinkerCat.GREMLIN_TINKERGRAPH_GRAPH_FORMAT, "gryo")
        conf.setProperty(TinkerCat.GREMLIN_TINKERGRAPH_GRAPH_LOCATION, graphLocation)
        val graph = TinkerCat.open(conf)
        TinkerFactory.generateTheCrew(graph)
        graph.close()
        conf.setProperty(
            TinkerCat.GREMLIN_TINKERGRAPH_DEFAULT_VERTEX_PROPERTY_CARDINALITY,
            VertexProperty.Cardinality.list.toString()
        )
        val reloadedGraph = TinkerCat.open(conf)
        IoTest.assertCrewGraph(reloadedGraph, false)
        reloadedGraph.close()
    }

    @Test
    fun shouldPersistWithRelativePath() {
        val graphLocation: String = (TestHelper.convertToRelative(
            TinkerCatTest::class.java,
            TestHelper.makeTestDataPath(TinkerCatTest::class.java)
        )
                + "shouldPersistToGryoRelative.kryo")
        val f = File(graphLocation)
        if (f.exists() && f.isFile) f.delete()
        val conf: Configuration = BaseConfiguration()
        conf.setProperty(TinkerCat.GREMLIN_TINKERGRAPH_GRAPH_FORMAT, "gryo")
        conf.setProperty(TinkerCat.GREMLIN_TINKERGRAPH_GRAPH_LOCATION, graphLocation)
        val graph = TinkerCat.open(conf)
        TinkerFactory.generateModern(graph)
        graph.close()
        val reloadedGraph = TinkerCat.open(conf)
        IoTest.assertModernGraph(reloadedGraph, true, false)
        reloadedGraph.close()
    }

    @Test
    fun shouldPersistToAnyGraphFormat() {
        val graphLocation: String =
            TestHelper.makeTestDataFile(TinkerCatTest::class.java, "shouldPersistToAnyGraphFormat.dat")
        val f = File(graphLocation)
        if (f.exists() && f.isFile) f.delete()
        val conf: Configuration = BaseConfiguration()
        conf.setProperty(TinkerCat.GREMLIN_TINKERGRAPH_GRAPH_FORMAT, TestIoBuilder::class.java.name)
        conf.setProperty(TinkerCat.GREMLIN_TINKERGRAPH_GRAPH_LOCATION, graphLocation)
        val graph = TinkerCat.open(conf)
        TinkerFactory.generateModern(graph)

        //Test write graph
        graph.close()
        Assert.assertEquals(TestIoBuilder.calledOnMapper.toLong(), 1)
        Assert.assertEquals(TestIoBuilder.calledGraph.toLong(), 1)
        Assert.assertEquals(TestIoBuilder.calledCreate.toLong(), 1)
        try {
            BufferedOutputStream(FileOutputStream(f)).use { os -> os.write("dummy string".toByteArray()) }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        //Test read graph
        val readGraph = TinkerCat.open(conf)
        Assert.assertEquals(TestIoBuilder.calledOnMapper.toLong(), 1)
        Assert.assertEquals(TestIoBuilder.calledGraph.toLong(), 1)
        Assert.assertEquals(TestIoBuilder.calledCreate.toLong(), 1)
    }

    @Test
    @Throws(Exception::class)
    fun shouldSerializeWithColorClassResolverToTinkerCat() {
        val colors: MutableMap<String, Color> = HashMap()
        colors["red"] = Color.RED
        colors["green"] = Color.GREEN
        val colorList: ArrayList<Color> = ArrayList<Color>(Arrays.asList<Color>(Color.RED, Color.GREEN))
        val classResolver: Supplier<ClassResolver> = CustomClassResolverSupplier()
        val mapper: GryoMapper =
            GryoMapper.build().version(GryoVersion.V3_0).addRegistry(TinkerIoRegistryV3d0.instance())
                .classResolver(classResolver).create()
        val kryo: Kryo = mapper.createMapper()
        ByteArrayOutputStream().use { stream ->
            val out = Output(stream)
            kryo.writeObject(out, colorList)
            out.flush()
            val b = stream.toByteArray()
            ByteArrayInputStream(b).use { inputStream ->
                val input = Input(inputStream)
                val m: List<*> = kryo.readObject(input, ArrayList::class.java)
                val readX = m[0] as TinkerCat
                Assert.assertEquals(104, IteratorUtils.count(readX.vertices()))
                Assert.assertEquals(102, IteratorUtils.count(readX.edges()))
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun shouldSerializeWithColorClassResolverToTinkerCatUsingDeprecatedTinkerIoRegistry() {
        val colors: MutableMap<String, Color> = HashMap()
        colors["red"] = Color.RED
        colors["green"] = Color.GREEN
        val colorList: ArrayList<Color> = ArrayList<Color>(Arrays.asList<Color>(Color.RED, Color.GREEN))
        val classResolver: Supplier<ClassResolver> = CustomClassResolverSupplier()
        val mapper: GryoMapper =
            GryoMapper.build().version(GryoVersion.V3_0).addRegistry(TinkerIoRegistryV3d0.instance())
                .classResolver(classResolver).create()
        val kryo: Kryo = mapper.createMapper()
        ByteArrayOutputStream().use { stream ->
            val out = Output(stream)
            kryo.writeObject(out, colorList)
            out.flush()
            val b = stream.toByteArray()
            ByteArrayInputStream(b).use { inputStream ->
                val input = Input(inputStream)
                val m: List<*> = kryo.readObject(input, ArrayList::class.java)
                val readX = m[0] as TinkerCat
                Assert.assertEquals(104, IteratorUtils.count(readX.vertices()))
                Assert.assertEquals(102, IteratorUtils.count(readX.edges()))
            }
        }
    }

    @Test
    fun shouldCloneTinkergraph() {
        val original = TinkerCat.open()
        val clone = TinkerCat.open()
        val marko = original.addVertex("name", "marko", "age", 29)
        val stephen = original.addVertex("name", "stephen", "age", 35)
        marko.addEdge("knows", stephen)
        GraphHelper.cloneElements(original, clone)
        val michael = clone.addVertex("name", "michael")
        michael.addEdge("likes", marko)
        michael.addEdge("likes", stephen)
        clone.traversal().V().property("newProperty", "someValue").toList()
        clone.traversal().E().property("newProperty", "someValue").toList()
        Assert.assertEquals("original graph should be unchanged", 2, original.traversal().V().count().next())
        Assert.assertEquals("original graph should be unchanged", 1, original.traversal().E().count().next())
        Assert.assertEquals(
            "original graph should be unchanged",
            0,
            original.traversal().V().has("newProperty").count().next()
        )
        Assert.assertEquals("cloned graph should contain new elements", 3, clone.traversal().V().count().next())
        Assert.assertEquals("cloned graph should contain new elements", 3, clone.traversal().E().count().next())
        Assert.assertEquals(
            "cloned graph should contain new property",
            3,
            clone.traversal().V().has("newProperty").count().next()
        )
        Assert.assertEquals(
            "cloned graph should contain new property",
            3,
            clone.traversal().E().has("newProperty").count().next()
        )
        Assert.assertNotSame(
            "cloned elements should reference to different objects",
            original.traversal().V().has("name", "stephen").next(),
            clone.traversal().V().has("name", "stephen").next()
        )
    }

    /**
     * This isn't a TinkerCat specific test, but TinkerCat is probably best suited for the testing of this
     * particular problem originally noted in TINKERPOP-1992.
     */
    @Test
    fun shouldProperlyTimeReducingBarrierForProfile() {
        val g: GraphTraversalSource = TinkerFactory.createModern().traversal()
        var m: TraversalMetrics = g.V().group<Any, Any>().by().by(__.bothE().count()).profile().next()
        for (i in m.getMetrics(1).getNested()) {
            MatcherAssert.assertThat(i.getDuration(TimeUnit.NANOSECONDS), Matchers.greaterThan(0L))
        }
        m = g.withComputer().V().group<Any, Any>().by().by(__.bothE().count()).profile().next()
        for (i in m.getMetrics(1).getNested()) {
            MatcherAssert.assertThat(i.getDuration(TimeUnit.NANOSECONDS), Matchers.greaterThan(0L))
        }
    }

    /**
     * Just validating that property folding works nicely given TINKERPOP-2112
     */
    @Test
    fun shouldFoldPropertyStepForTokens() {
        val g: GraphTraversalSource = TinkerCat.open().traversal()
        g.addV("person").property(VertexProperty.Cardinality.single, "k", "v").property(T.id, "id")
            .property(VertexProperty.Cardinality.list, "l", 1).property("x", "y")
            .property(VertexProperty.Cardinality.list, "l", 2).property("m", "m", "mm", "mm").property("y", "z")
            .iterate()
        MatcherAssert.assertThat(g.V("id").hasNext(), Matchers.`is`(true))
    }

    @Test
    fun shouldOptionalUsingWithComputer() {
        // not all systems will have 3+ available processors (e.g. travis)
        Assume.assumeThat<Int>(Runtime.getRuntime().availableProcessors(), Matchers.greaterThan(2))

        // didn't add this as a general test as it basically was only failing under a specific condition for
        // TinkerCatComputer - see more here: https://issues.apache.org/jira/browse/TINKERPOP-1619
        val g: GraphTraversalSource = TinkerFactory.createModern().traversal()
        val expected: List<Edge> = g.E(7, 7, 8, 9).order().by(T.id).toList()
        Assert.assertEquals(
            expected,
            g.withComputer(Computer.compute().workers(3)).V(1, 2).optional<Edge>(__.bothE().dedup()).order().by(T.id)
                .toList()
        )
        Assert.assertEquals(
            expected,
            g.withComputer(Computer.compute().workers(4)).V(1, 2).optional<Edge>(__.bothE().dedup()).order().by(T.id)
                .toList()
        )
    }

    @Test
    fun shouldReservedKeyVerify() {
        val reserved: Set<String> = HashSet<String>(Arrays.asList<String>("something", "id", "label"))
        val g: GraphTraversalSource = TinkerCat.open().traversal().withStrategies(
            ReservedKeysVerificationStrategy.build().reservedKeys(reserved).throwException().create()
        )
        g.addV("person").property(T.id, 123).iterate()
        try {
            g.addV("person").property("id", 123).iterate()
            Assert.fail("Verification exception expected")
        } catch (ve: IllegalStateException) {
            MatcherAssert.assertThat(
                ve.message,
                StringContains.containsString("that is setting a property key to a reserved word")
            )
        }
        try {
            g.addV("person").property("something", 123).iterate()
            Assert.fail("Verification exception expected")
        } catch (ve: IllegalStateException) {
            MatcherAssert.assertThat(
                ve.message,
                StringContains.containsString("that is setting a property key to a reserved word")
            )
        }
    }

    @Test
    fun shouldProvideClearErrorWhenTryingToMutateT() {
        val g: GraphTraversalSource = TinkerCat.open().traversal()
        g.addV("person").property(T.id, 100).iterate()
        try {
            g.V(100).property(T.label, "software").iterate()
            Assert.fail("Should have thrown an error")
        } catch (ise: IllegalStateException) {
            Assert.assertEquals("T.label is immutable on existing elements", ise.message)
        }
        try {
            g.V(100).property(T.id, 101).iterate()
            Assert.fail("Should have thrown an error")
        } catch (ise: IllegalStateException) {
            Assert.assertEquals("T.id is immutable on existing elements", ise.message)
        }
        try {
            g.V(100).property("name", "marko").property(T.label, "software").iterate()
            Assert.fail("Should have thrown an error")
        } catch (ise: IllegalStateException) {
            Assert.assertEquals("T.label is immutable on existing elements", ise.message)
        }
        try {
            g.V(100).property(T.id, 101).property("name", "marko").iterate()
            Assert.fail("Should have thrown an error")
        } catch (ise: IllegalStateException) {
            Assert.assertEquals("T.id is immutable on existing elements", ise.message)
        }
    }

    @Test
    fun shouldProvideClearErrorWhenTryingToMutateEdgeWithCardinality() {
        val g: GraphTraversalSource = TinkerFactory.createModern().traversal()
        try {
            g.E().property(VertexProperty.Cardinality.single, "k", 100).iterate()
            Assert.fail("Should have thrown an error")
        } catch (ise: IllegalStateException) {
            Assert.assertEquals(
                "Property cardinality can only be set for a Vertex but the traversal encountered TinkerEdge for key: k",
                ise.message
            )
        }
        try {
            g.E().property(VertexProperty.Cardinality.list, "k", 100).iterate()
            Assert.fail("Should have thrown an error")
        } catch (ise: IllegalStateException) {
            Assert.assertEquals(
                "Property cardinality can only be set for a Vertex but the traversal encountered TinkerEdge for key: k",
                ise.message
            )
        }
        try {
            g.addE("link").to(__.V<Any>(1)).from(__.V<Any>(1)).property(VertexProperty.Cardinality.list, "k", 100)
                .iterate()
            Assert.fail("Should have thrown an error")
        } catch (ise: IllegalStateException) {
            Assert.assertEquals(
                "Multi-property cardinality of [list] can only be set for a Vertex but is being used for addE() with key: k",
                ise.message
            )
        }
    }

    @Test
    fun shouldProvideClearErrorWhenPuttingFromToInWrongSpot() {
        val g: GraphTraversalSource = TinkerFactory.createModern().traversal()
        try {
            g.addE("link").property(VertexProperty.Cardinality.single, "k", 100).out().to(__.V<Any>(1))
                .from(__.V<Any>(1)).iterate()
            Assert.fail("Should have thrown an error")
        } catch (ise: IllegalArgumentException) {
            Assert.assertEquals("The to() step cannot follow VertexStep", ise.message)
        }
        try {
            g.addE("link").property("k", 100).out().from(__.V<Any>(1)).to(__.V<Any>(1)).iterate()
            Assert.fail("Should have thrown an error")
        } catch (ise: IllegalArgumentException) {
            Assert.assertEquals("The from() step cannot follow VertexStep", ise.message)
        }
    }

    @Test
    fun shouldProvideClearErrorWhenFromOrToDoesNotResolveToVertex() {
        val g: GraphTraversalSource = TinkerFactory.createModern().traversal()
        try {
            g.addE("link").property(VertexProperty.Cardinality.single, "k", 100).to(__.V<Any>(1)).iterate()
            Assert.fail("Should have thrown an error")
        } catch (ise: IllegalStateException) {
            Assert.assertEquals("addE(link) could not find a Vertex for from() - encountered: null", ise.message)
        }
        try {
            g.addE("link").property(VertexProperty.Cardinality.single, "k", 100).from(__.V<Any>(1)).iterate()
            Assert.fail("Should have thrown an error")
        } catch (ise: IllegalStateException) {
            Assert.assertEquals("addE(link) could not find a Vertex for to() - encountered: null", ise.message)
        }
        try {
            g.addE("link").property("k", 100).from(__.V<Any>(1)).iterate()
            Assert.fail("Should have thrown an error")
        } catch (ise: IllegalStateException) {
            Assert.assertEquals("addE(link) could not find a Vertex for to() - encountered: null", ise.message)
        }
        try {
            g.V(1).values<Any>("name").`as`("a").addE("link").property(VertexProperty.Cardinality.single, "k", 100)
                .from("a").iterate()
            Assert.fail("Should have thrown an error")
        } catch (ise: IllegalStateException) {
            Assert.assertEquals("addE(link) could not find a Vertex for to() - encountered: String", ise.message)
        }
        try {
            g.V(1).values<Any>("name").`as`("a").addE("link").property(VertexProperty.Cardinality.single, "k", 100)
                .to("a").iterate()
            Assert.fail("Should have thrown an error")
        } catch (ise: IllegalStateException) {
            Assert.assertEquals("addE(link) could not find a Vertex for to() - encountered: String", ise.message)
        }
        try {
            g.V(1).`as`("v").values<Any>("name").`as`("a").addE("link")
                .property(VertexProperty.Cardinality.single, "k", 100).to("v").from("a").iterate()
            Assert.fail("Should have thrown an error")
        } catch (ise: IllegalStateException) {
            Assert.assertEquals("addE(link) could not find a Vertex for from() - encountered: String", ise.message)
        }
    }

    @Test
    fun shouldWorkWithoutIdentityStrategy() {
        val graph: Graph = TinkerFactory.createModern()
        val g: GraphTraversalSource = AnonymousTraversalSource.traversal().withEmbedded(graph).withoutStrategies(
            IdentityRemovalStrategy::class.java
        )
        val result: List<Map<String, Any>> =
            g.V().match<Any>(__.`as`<Any>("a").out("knows").values<Any>("name").`as`("b")).identity().toList()
        Assert.assertEquals(2, result.size.toLong())
        result.stream().forEach { m: Map<String, Any> ->
            Assert.assertEquals(2, m.size.toLong())
            MatcherAssert.assertThat(m.containsKey("a"), Matchers.`is`(true))
            MatcherAssert.assertThat(m.containsKey("b"), Matchers.`is`(true))
        }
    }

    @Test
    fun shouldApplyStrategiesRecursivelyWithGraph() {
        val graph: Graph = TinkerCat.open()
        val g: GraphTraversalSource = AnonymousTraversalSource.traversal().withEmbedded(graph)
            .withStrategies(object : ProviderOptimizationStrategy {
                override fun apply(traversal: Traversal.Admin<*, *>) {
                    val graph = traversal.graph.get()
                    graph.addVertex("person")
                }
            })

        // adds one person by way of the strategy
        g.inject<Int>(0).iterate()
        Assert.assertEquals(
            1,
            AnonymousTraversalSource.traversal().withEmbedded(graph).V().hasLabel("person").count().next().toInt()
                .toLong()
        )

        // adds two persons by way of the strategy one for the parent and one for the child
        g.inject<Int>(0).sideEffect(__.addV<Any>()).iterate()
        Assert.assertEquals(
            3,
            AnonymousTraversalSource.traversal().withEmbedded(graph).V().hasLabel("person").count().next().toInt()
                .toLong()
        )
    }

    @Test
    fun shouldAllowHeterogeneousIdsWithAnyManager() {
        val anyManagerConfig: Configuration = BaseConfiguration()
        anyManagerConfig.addProperty(TinkerCat.GREMLIN_TINKERGRAPH_EDGE_ID_MANAGER, TinkerCat.DefaultIdManager.ANY.name)
        anyManagerConfig.addProperty(
            TinkerCat.GREMLIN_TINKERGRAPH_VERTEX_ID_MANAGER,
            TinkerCat.DefaultIdManager.ANY.name
        )
        anyManagerConfig.addProperty(
            TinkerCat.GREMLIN_TINKERGRAPH_VERTEX_PROPERTY_ID_MANAGER,
            TinkerCat.DefaultIdManager.ANY.name
        )
        val graph: Graph = TinkerCat.open(anyManagerConfig)
        val g: GraphTraversalSource = AnonymousTraversalSource.traversal().withEmbedded(graph)
        val uuid = UUID.fromString("0E939658-ADD2-4598-A722-2FC178E9B741")
        g.addV("person").property(T.id, 100).addV("person").property(T.id, "1000").addV("person").property(T.id, "1001")
            .addV("person").property(T.id, uuid).iterate()
        Assert.assertEquals(3, g.V(100, "1000", uuid).count().next().toInt().toLong())
    }

    /**
     * Coerces a `Color` to a [TinkerCat] during serialization.  Demonstrates how custom serializers
     * can be developed that can coerce one value to another during serialization.
     */
    class ColorToTinkerCatSerializer : Serializer<Color?>() {
        fun write(kryo: Kryo?, output: Output, color: Color) {
            val graph = TinkerCat.open()
            val v = graph.addVertex(T.id, 1, T.label, "color", "name", color.toString())
            val vRed = graph.addVertex(T.id, 2, T.label, "primary", "name", "red")
            val vGreen = graph.addVertex(T.id, 3, T.label, "primary", "name", "green")
            val vBlue = graph.addVertex(T.id, 4, T.label, "primary", "name", "blue")
            v.addEdge("hasComponent", vRed, "amount", color.red)
            v.addEdge("hasComponent", vGreen, "amount", color.green)
            v.addEdge("hasComponent", vBlue, "amount", color.blue)

            // make some junk so the graph is kinda big
            generate(graph)
            try {
                ByteArrayOutputStream().use { stream ->
                    GryoWriter.build().mapper(Mapper<Kryo?> { kryo }).create().writeGraph(stream, graph)
                    val bytes = stream.toByteArray()
                    output.writeInt(bytes.size)
                    output.write(bytes)
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        fun read(kryo: Kryo?, input: Input?, colorClass: Class<Color?>?): Color {
            throw UnsupportedOperationException("IoX writes to DetachedVertex and can't be read back in as IoX")
        }

        companion object {
            private fun generate(graph: Graph) {
                val size = 100
                val ids: MutableList<Any> = ArrayList()
                val v = graph.addVertex("sin", 0.0f, "cos", 1.0f, "ii", 0f)
                ids.add(v.id())
                val g: GraphTraversalSource = graph.traversal()
                val rand = Random()
                for (ii in 1 until size) {
                    val t = graph.addVertex(
                        "ii",
                        ii,
                        "sin",
                        Math.sin((ii / 5.0f).toDouble()),
                        "cos",
                        Math.cos((ii / 5.0f).toDouble())
                    )
                    val u: Vertex = g.V(ids[rand.nextInt(ids.size)]).next()
                    t.addEdge("linked", u)
                    ids.add(u.id())
                    ids.add(v.id())
                }
            }
        }
    }

    class CustomClassResolverSupplier : Supplier<ClassResolver> {
        override fun get(): ClassResolver {
            return CustomClassResolver()
        }
    }

    class CustomClassResolver : GryoClassResolverV1d0() {
        private val colorToGraphSerializer = ColorToTinkerCatSerializer()
        override fun getRegistration(clazz: Class<*>?): Registration {
            return if (Color::class.java.isAssignableFrom(clazz)) {
                val registration: Registration = super.getRegistration(TinkerCat::class.java)
                Registration(registration.getType(), colorToGraphSerializer, registration.getId())
            } else {
                super.getRegistration(clazz)
            }
        }
    }

    class TestIoBuilder     //Looks awkward to reset static vars inside a constructor, but makes sense from testing perspective
        : Io.Builder<Any?> {
        override fun onMapper(onMapper: Consumer<*>?): Io.Builder<out Io<*, *, *>> {
            calledOnMapper++
            return this
        }

        override fun graph(graph: Graph): Io.Builder<out Io<*, *, *>> {
            calledGraph++
            return this
        }

        override fun create(): Io<*, *, *> {
            calledCreate++
            return Mockito.mock<Io<*, *, *>>(Io::class.java)
        }

        override fun requiresVersion(version: Any): Boolean {
            return false
        }

        companion object {
            var calledGraph = 0
            var calledCreate = 0
            var calledOnMapper = 0
        }
    }
}