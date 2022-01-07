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

import org.apache.commons.io.FileUtils
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerFactory.createClassic
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerFactory.createModern
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerFactory.createTheCrew
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerFactory.createKitchenSink
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerCat.Companion.open
import kotlin.Throws
import java.io.IOException
import org.apache.tinkerpop.gremlin.TestHelper
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerCatTest
import java.io.FileOutputStream
import org.apache.tinkerpop.gremlin.structure.io.gryo.GryoWriter
import org.apache.tinkerpop.gremlin.structure.io.gryo.GryoMapper
import org.apache.tinkerpop.gremlin.structure.io.gryo.GryoVersion
import org.apache.tinkerpop.gremlin.structure.io.graphml.GraphMLWriter
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONWriter
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONMapper
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONVersion
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerCat
import org.apache.tinkerpop.gremlin.algorithm.generator.DistributionGenerator
import org.apache.tinkerpop.gremlin.algorithm.generator.PowerLawDistribution
import org.apache.tinkerpop.gremlin.structure.io.gryo.GryoReader
import org.apache.tinkerpop.gremlin.AbstractGremlinTest
import org.apache.tinkerpop.gremlin.process.traversal.Traverser
import org.apache.tinkerpop.gremlin.structure.Edge
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.io.GraphReader
import org.apache.tinkerpop.gremlin.structure.io.graphson.TypeInfo
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.OutputStream
import java.lang.RuntimeException
import java.util.stream.IntStream

/**
 * Less of a test of functionality and more of a tool to help generate data files for TinkerPop.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class IoDataGenerationTest {
    private var tempPath: File? = null
    @Before
    @Throws(IOException::class)
    fun before() {
        tempPath = TestHelper.makeTestDataPath(TinkerCatTest::class.java, "tinkerpop-io")
        FileUtils.deleteDirectory(tempPath)
        if (!tempPath.mkdirs()) throw IOException(String.format("Could not create %s", tempPath))
    }

    /**
     * No assertions.  Just write out the graph for convenience.
     */
    @Test
    @Throws(IOException::class)
    fun shouldWriteClassicGraphAsGryoV1d0() {
        val os: OutputStream = FileOutputStream(File(tempPath, "tinkerpop-classic-v1d0.kryo"))
        GryoWriter.build().mapper(GryoMapper.build().version(GryoVersion.V1_0).create()).create()
            .writeGraph(os, createClassic())
        os.close()
    }

    /**
     * No assertions.  Just write out the graph for convenience.
     */
    @Test
    @Throws(IOException::class)
    fun shouldWriteModernGraphAsGryoV1d0() {
        val os: OutputStream = FileOutputStream(File(tempPath, "tinkerpop-modern-v1d0.kryo"))
        GryoWriter.build().mapper(GryoMapper.build().version(GryoVersion.V1_0).create()).create()
            .writeGraph(os, createModern())
        os.close()
    }

    /**
     * No assertions.  Just write out the graph for convenience.
     */
    @Test
    @Throws(IOException::class)
    fun shouldWriteCrewGraphAsGryoV1d0() {
        val os: OutputStream = FileOutputStream(File(tempPath, "tinkerpop-crew-v1d0.kryo"))
        GryoWriter.build().mapper(GryoMapper.build().version(GryoVersion.V1_0).create()).create()
            .writeGraph(os, createTheCrew())
        os.close()
    }

    /**
     * No assertions.  Just write out the graph for convenience.
     */
    @Test
    @Throws(IOException::class)
    fun shouldWriteSinkGraphAsGryoV1d0() {
        val os: OutputStream = FileOutputStream(File(tempPath, "tinkerpop-sink-v1d0.kryo"))
        GryoWriter.build().mapper(GryoMapper.build().version(GryoVersion.V1_0).create()).create()
            .writeGraph(os, createKitchenSink())
        os.close()
    }

    /**
     * No assertions.  Just write out the graph for convenience.
     */
    @Test
    @Throws(IOException::class)
    fun shouldWriteClassicGraphAsGryoV3d0() {
        val os: OutputStream = FileOutputStream(File(tempPath, "tinkerpop-classic-v3d0.kryo"))
        GryoWriter.build().mapper(GryoMapper.build().version(GryoVersion.V3_0).create()).create()
            .writeGraph(os, createClassic())
        os.close()
    }

    /**
     * No assertions.  Just write out the graph for convenience.
     */
    @Test
    @Throws(IOException::class)
    fun shouldWriteModernGraphAsGryoV3d0() {
        val os: OutputStream = FileOutputStream(File(tempPath, "tinkerpop-modern-v3d0.kryo"))
        GryoWriter.build().mapper(GryoMapper.build().version(GryoVersion.V3_0).create()).create()
            .writeGraph(os, createModern())
        os.close()
    }

    /**
     * No assertions.  Just write out the graph for convenience.
     */
    @Test
    @Throws(IOException::class)
    fun shouldWriteCrewGraphAsGryoV3d0() {
        val os: OutputStream = FileOutputStream(File(tempPath, "tinkerpop-crew-v3d0.kryo"))
        GryoWriter.build().mapper(GryoMapper.build().version(GryoVersion.V3_0).create()).create()
            .writeGraph(os, createTheCrew())
        os.close()
    }

    /**
     * No assertions.  Just write out the graph for convenience.
     */
    @Test
    @Throws(IOException::class)
    fun shouldWriteSinkGraphAsGryoV3d0() {
        val os: OutputStream = FileOutputStream(File(tempPath, "tinkerpop-sink-v3d0.kryo"))
        GryoWriter.build().mapper(GryoMapper.build().version(GryoVersion.V3_0).create()).create()
            .writeGraph(os, createKitchenSink())
        os.close()
    }

    /**
     * No assertions.  Just write out the graph for convenience.
     */
    @Test
    @Throws(IOException::class)
    fun shouldWriteDEFAULTClassicGraphAsGryoV3d0() {
        val os: OutputStream = FileOutputStream(File(tempPath, "tinkerpop-classic.kryo"))
        GryoWriter.build().mapper(GryoMapper.build().version(GryoVersion.V3_0).create()).create()
            .writeGraph(os, createClassic())
        os.close()
    }

    /**
     * No assertions.  Just write out the graph for convenience.
     */
    @Test
    @Throws(IOException::class)
    fun shouldWriteDEFAULTModernGraphAsGryoV3d0() {
        val os: OutputStream = FileOutputStream(File(tempPath, "tinkerpop-modern.kryo"))
        GryoWriter.build().mapper(GryoMapper.build().version(GryoVersion.V3_0).create()).create()
            .writeGraph(os, createModern())
        os.close()
    }

    /**
     * No assertions.  Just write out the graph for convenience.
     */
    @Test
    @Throws(IOException::class)
    fun shouldWriteDEFAULTCrewGraphAsGryoV3d0() {
        val os: OutputStream = FileOutputStream(File(tempPath, "tinkerpop-crew.kryo"))
        GryoWriter.build().mapper(GryoMapper.build().version(GryoVersion.V3_0).create()).create()
            .writeGraph(os, createTheCrew())
        os.close()
    }

    /**
     * No assertions.  Just write out the graph for convenience.
     */
    @Test
    @Throws(IOException::class)
    fun shouldWriteDEFAULTSinkGraphAsGryoV3d0() {
        val os: OutputStream = FileOutputStream(File(tempPath, "tinkerpop-sink.kryo"))
        GryoWriter.build().mapper(GryoMapper.build().version(GryoVersion.V3_0).create()).create()
            .writeGraph(os, createKitchenSink())
        os.close()
    }

    /**
     * No assertions.  Just write out the graph for convenience.
     */
    @Test
    @Throws(IOException::class)
    fun shouldWriteClassicGraphAsGraphML() {
        FileOutputStream(File(tempPath, "tinkerpop-classic.xml")).use { os ->
            GraphMLWriter.build().create().writeGraph(os, createClassic())
        }
    }

    /**
     * No assertions.  Just write out the graph for convenience.
     */
    @Test
    @Throws(IOException::class)
    fun shouldWriteModernGraphAsGraphML() {
        FileOutputStream(File(tempPath, "tinkerpop-modern.xml")).use { os ->
            GraphMLWriter.build().create().writeGraph(os, createModern())
        }
    }

    /**
     * No assertions.  Just write out the graph for convenience.
     */
    @Test
    @Throws(IOException::class)
    fun shouldWriteClassicGraphAsGraphSONV1d0NoTypes() {
        val os: OutputStream = FileOutputStream(File(tempPath, "tinkerpop-classic-v1d0.json"))
        GraphSONWriter.build()
            .mapper(GraphSONMapper.build().version(GraphSONVersion.V1_0).typeInfo(TypeInfo.NO_TYPES).create()).create()
            .writeGraph(os, createClassic())
        os.close()
    }

    /**
     * No assertions.  Just write out the graph for convenience.
     */
    @Test
    @Throws(IOException::class)
    fun shouldWriteModernGraphAsGraphSONV1d0NoTypes() {
        val os: OutputStream = FileOutputStream(File(tempPath, "tinkerpop-modern-v1d0.json"))
        GraphSONWriter.build()
            .mapper(GraphSONMapper.build().version(GraphSONVersion.V1_0).typeInfo(TypeInfo.NO_TYPES).create()).create()
            .writeGraph(os, createModern())
        os.close()
    }

    /**
     * No assertions.  Just write out the graph for convenience.
     */
    @Test
    @Throws(IOException::class)
    fun shouldWriteCrewGraphAsGraphSONV1d0NoTypes() {
        val os: OutputStream = FileOutputStream(File(tempPath, "tinkerpop-crew-v1d0.json"))
        GraphSONWriter.build()
            .mapper(GraphSONMapper.build().version(GraphSONVersion.V1_0).typeInfo(TypeInfo.NO_TYPES).create()).create()
            .writeGraph(os, createTheCrew())
        os.close()
    }

    /**
     * No assertions. Just write out the graph for convenience
     */
    @Test
    @Throws(IOException::class)
    fun shouldWriteKitchenSinkAsGraphSONNoTypes() {
        val os: OutputStream = FileOutputStream(File(tempPath, "tinkerpop-sink-v1d0.json"))
        GraphSONWriter.build()
            .mapper(GraphSONMapper.build().version(GraphSONVersion.V1_0).typeInfo(TypeInfo.NO_TYPES).create()).create()
            .writeGraph(os, createKitchenSink())
        os.close()
    }

    /**
     * No assertions.  Just write out the graph for convenience.
     */
    @Test
    @Throws(IOException::class)
    fun shouldWriteClassicGraphNormalizedAsGraphSONV1d0() {
        val os: OutputStream = FileOutputStream(File(tempPath, "tinkerpop-classic-normalized-v1d0.json"))
        GraphSONWriter.build().mapper(
            GraphSONMapper.build().normalize(true).version(GraphSONVersion.V1_0).typeInfo(
                TypeInfo.NO_TYPES
            ).create()
        ).create().writeGraph(os, createClassic())
        os.close()
    }

    /**
     * No assertions.  Just write out the graph for convenience.
     */
    @Test
    @Throws(IOException::class)
    fun shouldWriteModernGraphNormalizedAsGraphSONV1d0() {
        val os: OutputStream = FileOutputStream(File(tempPath, "tinkerpop-modern-normalized-v1d0.json"))
        GraphSONWriter.build().mapper(
            GraphSONMapper.build().normalize(true).version(GraphSONVersion.V1_0).typeInfo(
                TypeInfo.NO_TYPES
            ).create()
        ).create().writeGraph(os, createModern())
        os.close()
    }

    /**
     * No assertions.  Just write out the graph for convenience.
     */
    @Test
    @Throws(IOException::class)
    fun shouldWriteClassicGraphAsGraphSONV1d0WithTypes() {
        val os: OutputStream = FileOutputStream(File(tempPath, "tinkerpop-classic-typed-v1d0.json"))
        GraphSONWriter.build()
            .mapper(GraphSONMapper.build().version(GraphSONVersion.V1_0).typeInfo(TypeInfo.PARTIAL_TYPES).create())
            .create().writeGraph(os, createClassic())
        os.close()
    }

    /**
     * No assertions.  Just write out the graph for convenience.
     */
    @Test
    @Throws(IOException::class)
    fun shouldWriteModernGraphAsGraphSONV1d0WithTypes() {
        val os: OutputStream = FileOutputStream(File(tempPath, "tinkerpop-modern-typed-v1d0.json"))
        GraphSONWriter.build()
            .mapper(GraphSONMapper.build().version(GraphSONVersion.V1_0).typeInfo(TypeInfo.PARTIAL_TYPES).create())
            .create().writeGraph(os, createModern())
        os.close()
    }

    /**
     * No assertions.  Just write out the graph for convenience.
     */
    @Test
    @Throws(IOException::class)
    fun shouldWriteCrewGraphAsGraphSONV1d0WithTypes() {
        val os: OutputStream = FileOutputStream(File(tempPath, "tinkerpop-crew-typed-v1d0.json"))
        GraphSONWriter.build()
            .mapper(GraphSONMapper.build().version(GraphSONVersion.V1_0).typeInfo(TypeInfo.PARTIAL_TYPES).create())
            .create().writeGraph(os, createTheCrew())
        os.close()
    }

    /**
     * No assertions. Just write out the graph for convenience
     */
    @Test
    @Throws(IOException::class)
    fun shouldWriteKitchenSinkAsGraphSONWithTypes() {
        val os: OutputStream = FileOutputStream(File(tempPath, "tinkerpop-sink-typed-v1d0.json"))
        GraphSONWriter.build()
            .mapper(GraphSONMapper.build().version(GraphSONVersion.V1_0).typeInfo(TypeInfo.PARTIAL_TYPES).create())
            .create().writeGraph(os, createKitchenSink())
        os.close()
    }

    /**
     * No assertions.  Just write out the graph for convenience.
     */
    @Test
    @Throws(IOException::class)
    fun shouldWriteClassicGraphAsGraphSONV2d0NoTypes() {
        val os: OutputStream = FileOutputStream(File(tempPath, "tinkerpop-classic-v2d0.json"))
        GraphSONWriter.build()
            .mapper(GraphSONMapper.build().version(GraphSONVersion.V2_0).typeInfo(TypeInfo.NO_TYPES).create()).create()
            .writeGraph(os, createClassic())
        os.close()
    }

    /**
     * No assertions.  Just write out the graph for convenience.
     */
    @Test
    @Throws(IOException::class)
    fun shouldWriteModernGraphAsGraphSOV2d0NNoTypes() {
        val os: OutputStream = FileOutputStream(File(tempPath, "tinkerpop-modern-v2d0.json"))
        GraphSONWriter.build()
            .mapper(GraphSONMapper.build().version(GraphSONVersion.V2_0).typeInfo(TypeInfo.NO_TYPES).create()).create()
            .writeGraph(os, createModern())
        os.close()
    }

    /**
     * No assertions.  Just write out the graph for convenience.
     */
    @Test
    @Throws(IOException::class)
    fun shouldWriteCrewGraphAsGraphSONV2d0NoTypes() {
        val os: OutputStream = FileOutputStream(File(tempPath, "tinkerpop-crew-v2d0.json"))
        GraphSONWriter.build()
            .mapper(GraphSONMapper.build().version(GraphSONVersion.V2_0).typeInfo(TypeInfo.NO_TYPES).create()).create()
            .writeGraph(os, createTheCrew())
        os.close()
    }

    /**
     * No assertions. Just write out the graph for convenience
     */
    @Test
    @Throws(IOException::class)
    fun shouldWriteKitchenSinkAsGraphSONV2d0NoTypes() {
        val os: OutputStream = FileOutputStream(File(tempPath, "tinkerpop-sink-v2d0.json"))
        GraphSONWriter.build()
            .mapper(GraphSONMapper.build().version(GraphSONVersion.V2_0).typeInfo(TypeInfo.NO_TYPES).create()).create()
            .writeGraph(os, createKitchenSink())
        os.close()
    }

    /**
     * No assertions.  Just write out the graph for convenience.
     */
    @Test
    @Throws(IOException::class)
    fun shouldWriteClassicGraphNormalizedAsGraphSONV2d0() {
        val os: OutputStream = FileOutputStream(File(tempPath, "tinkerpop-classic-normalized-v2d0.json"))
        GraphSONWriter.build().mapper(
            GraphSONMapper.build().version(GraphSONVersion.V2_0).typeInfo(TypeInfo.NO_TYPES).normalize(true).create()
        ).create()
            .writeGraph(os, createClassic())
        os.close()
    }

    /**
     * No assertions.  Just write out the graph for convenience.
     */
    @Test
    @Throws(IOException::class)
    fun shouldWriteModernGraphNormalizedAsGraphSONV2d0() {
        val os: OutputStream = FileOutputStream(File(tempPath, "tinkerpop-modern-normalized-v2d0.json"))
        GraphSONWriter.build().mapper(
            GraphSONMapper.build().version(GraphSONVersion.V2_0).typeInfo(TypeInfo.NO_TYPES).normalize(true).create()
        ).create()
            .writeGraph(os, createModern())
        os.close()
    }

    /**
     * No assertions.  Just write out the graph for convenience.
     */
    @Test
    @Throws(IOException::class)
    fun shouldWriteClassicGraphAsGraphSONV2d0WithTypes() {
        val os: OutputStream = FileOutputStream(File(tempPath, "tinkerpop-classic-typed-v2d0.json"))
        GraphSONWriter.build()
            .mapper(GraphSONMapper.build().version(GraphSONVersion.V2_0).typeInfo(TypeInfo.PARTIAL_TYPES).create())
            .create()
            .writeGraph(os, createClassic())
        os.close()
    }

    /**
     * No assertions.  Just write out the graph for convenience.
     */
    @Test
    @Throws(IOException::class)
    fun shouldWriteModernGraphAsGraphSONV2d0WithTypes() {
        val os: OutputStream = FileOutputStream(File(tempPath, "tinkerpop-modern-typed-v2d0.json"))
        GraphSONWriter.build()
            .mapper(GraphSONMapper.build().version(GraphSONVersion.V2_0).typeInfo(TypeInfo.PARTIAL_TYPES).create())
            .create()
            .writeGraph(os, createModern())
        os.close()
    }

    /**
     * No assertions.  Just write out the graph for convenience.
     */
    @Test
    @Throws(IOException::class)
    fun shouldWriteCrewGraphAsGraphSONV2d0WithTypes() {
        val os: OutputStream = FileOutputStream(File(tempPath, "tinkerpop-crew-typed-v2d0.json"))
        GraphSONWriter.build()
            .mapper(GraphSONMapper.build().version(GraphSONVersion.V2_0).typeInfo(TypeInfo.PARTIAL_TYPES).create())
            .create()
            .writeGraph(os, createTheCrew())
        os.close()
    }

    /**
     * No assertions. Just write out the graph for convenience
     */
    @Test
    @Throws(IOException::class)
    fun shouldWriteKitchenSinkAsGraphSONV2d0WithTypes() {
        val os: OutputStream = FileOutputStream(File(tempPath, "tinkerpop-sink-typed-v2d0.json"))
        GraphSONWriter.build()
            .mapper(GraphSONMapper.build().version(GraphSONVersion.V2_0).typeInfo(TypeInfo.PARTIAL_TYPES).create())
            .create()
            .writeGraph(os, createKitchenSink())
        os.close()
    }

    /**
     * No assertions.  Just write out the graph for convenience.
     */
    @Test
    @Throws(IOException::class)
    fun shouldWriteClassicGraphAsGraphSONV3d0() {
        val os: OutputStream = FileOutputStream(File(tempPath, "tinkerpop-classic-v3d0.json"))
        GraphSONWriter.build().mapper(GraphSONMapper.build().version(GraphSONVersion.V3_0).create()).create()
            .writeGraph(os, createClassic())
        os.close()
    }

    /**
     * No assertions.  Just write out the graph for convenience.
     */
    @Test
    @Throws(IOException::class)
    fun shouldWriteModernGraphAsGraphSONV3d0() {
        val os: OutputStream = FileOutputStream(File(tempPath, "tinkerpop-modern-v3d0.json"))
        GraphSONWriter.build().mapper(GraphSONMapper.build().version(GraphSONVersion.V3_0).create()).create()
            .writeGraph(os, createModern())
        os.close()
    }

    /**
     * No assertions.  Just write out the graph for convenience.
     */
    @Test
    @Throws(IOException::class)
    fun shouldWriteCrewGraphAsGraphSONV3d0() {
        val os: OutputStream = FileOutputStream(File(tempPath, "tinkerpop-crew-v3d0.json"))
        GraphSONWriter.build().mapper(GraphSONMapper.build().version(GraphSONVersion.V3_0).create()).create()
            .writeGraph(os, createTheCrew())
        os.close()
    }

    /**
     * No assertions.  Just write out the graph for convenience.
     */
    @Test
    @Throws(IOException::class)
    fun shouldWriteSinkGraphAsGraphSONV3d0() {
        val os: OutputStream = FileOutputStream(File(tempPath, "tinkerpop-sink-v3d0.json"))
        GraphSONWriter.build().mapper(GraphSONMapper.build().version(GraphSONVersion.V3_0).create()).create()
            .writeGraph(os, createKitchenSink())
        os.close()
    }

    /**
     * No assertions.  Just write out the graph for convenience.
     */
    @Test
    @Throws(IOException::class)
    fun shouldWriteDEFAULTClassicGraphAsGraphSONV3d0() {
        val os: OutputStream = FileOutputStream(File(tempPath, "tinkerpop-classic.json"))
        GraphSONWriter.build().mapper(GraphSONMapper.build().version(GraphSONVersion.V3_0).create()).create()
            .writeGraph(os, createClassic())
        os.close()
    }

    /**
     * No assertions.  Just write out the graph for convenience.
     */
    @Test
    @Throws(IOException::class)
    fun shouldWriteDEFAULTModernGraphAsGraphSONV3d0() {
        val os: OutputStream = FileOutputStream(File(tempPath, "tinkerpop-modern.json"))
        GraphSONWriter.build().mapper(GraphSONMapper.build().version(GraphSONVersion.V3_0).create()).create()
            .writeGraph(os, createModern())
        os.close()
    }

    /**
     * No assertions.  Just write out the graph for convenience.
     */
    @Test
    @Throws(IOException::class)
    fun shouldWriteDEFAULTCrewGraphAsGraphSONV3d0() {
        val os: OutputStream = FileOutputStream(File(tempPath, "tinkerpop-crew.json"))
        GraphSONWriter.build().mapper(GraphSONMapper.build().version(GraphSONVersion.V3_0).create()).create()
            .writeGraph(os, createTheCrew())
        os.close()
    }

    /**
     * No assertions.  Just write out the graph for convenience.
     */
    @Test
    @Throws(IOException::class)
    fun shouldWriteDEFAULTSinkGraphAsGraphSONV3d0() {
        val os: OutputStream = FileOutputStream(File(tempPath, "tinkerpop-sink.json"))
        GraphSONWriter.build().mapper(GraphSONMapper.build().version(GraphSONVersion.V3_0).create()).create()
            .writeGraph(os, createKitchenSink())
        os.close()
    }

    /**
     * No assertions.  Just write out the graph for convenience.
     */
    @Test
    @Throws(IOException::class)
    fun shouldWriteClassicGraphNormalizedAsGraphSONV3d0() {
        val os: OutputStream = FileOutputStream(File(tempPath, "tinkerpop-classic-normalized-v3d0.json"))
        GraphSONWriter.build().mapper(GraphSONMapper.build().version(GraphSONVersion.V3_0).normalize(true).create())
            .create()
            .writeGraph(os, createClassic())
        os.close()
    }

    /**
     * No assertions.  Just write out the graph for convenience.
     */
    @Test
    @Throws(IOException::class)
    fun shouldWriteModernGraphNormalizedAsGraphSONV3d0() {
        val os: OutputStream = FileOutputStream(File(tempPath, "tinkerpop-modern-normalized-v3d0.json"))
        GraphSONWriter.build().mapper(GraphSONMapper.build().version(GraphSONVersion.V3_0).normalize(true).create())
            .create()
            .writeGraph(os, createModern())
        os.close()
    }

    @Test
    @Throws(IOException::class)
    fun shouldWriteSampleForGremlinServer() {
        val g: Graph = open()
        IntStream.range(0, 10000).forEach { i: Int -> g.addVertex("oid", i) }
        DistributionGenerator.build(g)
            .label("knows")
            .seedGenerator { 987654321L }
            .outDistribution(PowerLawDistribution(2.1))
            .inDistribution(PowerLawDistribution(2.1))
            .expectedNumEdges(100000).create().generate()
        val os: OutputStream = FileOutputStream(File(tempPath, "sample.kryo"))
        GryoWriter.build().mapper(GryoMapper.build().version(GryoVersion.V3_0).create()).create().writeGraph(os, g)
        os.close()
    }

    /**
     * This test helps with data conversions on Grateful Dead.  No Assertions...run as needed. Never read from the
     * GraphML source as it will always use a String identifier.
     */
    @Test
    @Throws(IOException::class)
    fun shouldWriteGratefulDead() {
        val g: Graph = open()

        // read from a Gryo 3.0 file for now
        val reader: GraphReader =
            GryoReader.build().mapper(GryoMapper.build().version(GryoVersion.V3_0).create()).create()
        AbstractGremlinTest::class.java.getResourceAsStream("/org/apache/tinkerpop/gremlin/structure/io/gryo/grateful-dead-v3d0.kryo")
            .use { stream -> reader.readGraph(stream, g) }

        /* keep this hanging around because changes to gryo format will need grateful dead generated from json so you can generate the gio
        final GraphSONMapper mapper = GraphSONMapper.build().typeInfo(TypeInfo.PARTIAL_TYPES).create();
        final GraphReader reader = org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONReader.build().mapper(mapper).create();
        try (final InputStream stream = AbstractGremlinTest.class.getResourceAsStream("/org/apache/tinkerpop/gremlin/structure/io/graphson/grateful-dead-typed.json")) {
            reader.readGraph(stream, g);
        }
        */
        val ng: Graph = open()
        g.traversal().V().sideEffect { ov: Traverser<Vertex> ->
            val v = ov.get()
            if (v.label() == "song") ng.addVertex(
                T.id,
                v.id().toString().toInt(),
                T.label,
                "song",
                "name",
                v.value("name"),
                "performances",
                v.property<Any>("performances").orElse(0),
                "songType",
                v.property<Any>("songType").orElse("")
            ) else if (v.label() == "artist") ng.addVertex(
                T.id,
                v.id().toString().toInt(),
                T.label,
                "artist",
                "name",
                v.value("name")
            ) else throw RuntimeException("damn")
        }.iterate()
        g.traversal().E().sideEffect { oe: Traverser<Edge> ->
            val e = oe.get()
            val v2 = ng.traversal().V(e.inVertex().id().toString().toInt()).next()
            val v1 = ng.traversal().V(e.outVertex().id().toString().toInt()).next()
            if (e.label() == "followedBy") v1.addEdge(
                "followedBy",
                v2,
                T.id,
                e.id().toString().toInt(),
                "weight",
                e.value("weight")
            ) else if (e.label() == "sungBy") v1.addEdge(
                "sungBy",
                v2,
                T.id,
                e.id().toString().toInt()
            ) else if (e.label() == "writtenBy") v1.addEdge(
                "writtenBy",
                v2,
                T.id,
                e.id().toString().toInt()
            ) else throw RuntimeException("bah")
        }.iterate()
        val os: OutputStream = FileOutputStream(File(tempPath, "grateful-dead-v1d0.kryo"))
        GryoWriter.build().mapper(GryoMapper.build().version(GryoVersion.V1_0).create()).create().writeGraph(os, ng)
        os.close()
        val os8: OutputStream = FileOutputStream(File(tempPath, "grateful-dead-v3d0.kryo"))
        GryoWriter.build().mapper(GryoMapper.build().version(GryoVersion.V3_0).create()).create().writeGraph(os8, ng)
        os8.close()

        // ****DEFAULT Grateful Dead Gryo****
        val os9: OutputStream = FileOutputStream(File(tempPath, "grateful-dead.kryo"))
        GryoWriter.build().mapper(GryoMapper.build().version(GryoVersion.V3_0).create()).create().writeGraph(os9, ng)
        os9.close()
        val os2: OutputStream = FileOutputStream(File(tempPath, "grateful-dead-v1d0.json"))
        GraphSONWriter.build()
            .mapper(GraphSONMapper.build().version(GraphSONVersion.V1_0).typeInfo(TypeInfo.NO_TYPES).create()).create()
            .writeGraph(os2, g)
        os2.close()
        val os3: OutputStream = FileOutputStream(File(tempPath, "grateful-dead-v2d0.json"))
        GraphSONWriter.build().mapper(
            GraphSONMapper.build().version(GraphSONVersion.V2_0)
                .typeInfo(TypeInfo.NO_TYPES).create()
        )
            .create()
            .writeGraph(os3, g)
        os3.close()
        val os4: OutputStream = FileOutputStream(File(tempPath, "grateful-dead.xml"))
        GraphMLWriter.build().create().writeGraph(os4, g)
        os4.close()
        val os5: OutputStream = FileOutputStream(File(tempPath, "grateful-dead-typed-v1d0.json"))
        GraphSONWriter.build()
            .mapper(GraphSONMapper.build().version(GraphSONVersion.V1_0).typeInfo(TypeInfo.PARTIAL_TYPES).create())
            .create().writeGraph(os5, g)
        os5.close()
        val os6: OutputStream = FileOutputStream(File(tempPath, "grateful-dead-typed-v2d0.json"))
        GraphSONWriter.build().mapper(
            GraphSONMapper.build().version(GraphSONVersion.V2_0)
                .typeInfo(TypeInfo.PARTIAL_TYPES).create()
        )
            .create()
            .writeGraph(os6, g)
        os6.close()
        val os7: OutputStream = FileOutputStream(File(tempPath, "grateful-dead-v3d0.json"))
        GraphSONWriter.build().mapper(
            GraphSONMapper.build().version(GraphSONVersion.V3_0)
                .typeInfo(TypeInfo.PARTIAL_TYPES).create()
        )
            .create()
            .writeGraph(os7, g)
        os7.close()

        // ****DEFAULT Grateful Dead GraphSON****
        val os10: OutputStream = FileOutputStream(File(tempPath, "grateful-dead.json"))
        GraphSONWriter.build().mapper(
            GraphSONMapper.build().version(GraphSONVersion.V3_0)
                .typeInfo(TypeInfo.PARTIAL_TYPES).create()
        )
            .create()
            .writeGraph(os10, g)
        os10.close()
    }
}