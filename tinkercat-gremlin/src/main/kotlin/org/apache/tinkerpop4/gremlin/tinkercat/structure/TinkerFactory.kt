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
import org.apache.tinkerpop4.gremlin.structure.T
import org.apache.tinkerpop4.gremlin.structure.VertexProperty
import org.apache.tinkerpop4.gremlin.structure.io.IoCore
import org.apache.tinkerpop4.gremlin.tinkercat.structure.TinkerCat.Companion.open

/**
 * Helps create a variety of different toy graphs for testing and learning purposes.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
object TinkerFactory {
    /**
     * Create the "classic" graph which was the original toy graph from TinkerPop 2.x.
     */
    @JvmStatic
    fun createClassic(): TinkerCat {
        val conf: Configuration = BaseConfiguration()
        conf.setProperty(TinkerCat.GREMLIN_TINKERGRAPH_VERTEX_ID_MANAGER, TinkerCat.DefaultIdManager.INTEGER.name)
        conf.setProperty(TinkerCat.GREMLIN_TINKERGRAPH_EDGE_ID_MANAGER, TinkerCat.DefaultIdManager.INTEGER.name)
        conf.setProperty(
            TinkerCat.GREMLIN_TINKERGRAPH_VERTEX_PROPERTY_ID_MANAGER,
            TinkerCat.DefaultIdManager.INTEGER.name
        )
        val g = open(conf)
        generateClassic(g)
        return g
    }

    /**
     * Generate the graph in [.createClassic] into an existing graph.
     */
    fun generateClassic(g: TinkerCat) {
        val marko = g.addVertex(T.id, 1, "name", "marko", "age", 29)
        val vadas = g.addVertex(T.id, 2, "name", "vadas", "age", 27)
        val lop = g.addVertex(T.id, 3, "name", "lop", "lang", "java")
        val josh = g.addVertex(T.id, 4, "name", "josh", "age", 32)
        val ripple = g.addVertex(T.id, 5, "name", "ripple", "lang", "java")
        val peter = g.addVertex(T.id, 6, "name", "peter", "age", 35)
        marko.addEdge("knows", vadas, T.id, 7, "weight", 0.5f)
        marko.addEdge("knows", josh, T.id, 8, "weight", 1.0f)
        marko.addEdge("created", lop, T.id, 9, "weight", 0.4f)
        josh.addEdge("created", ripple, T.id, 10, "weight", 1.0f)
        josh.addEdge("created", lop, T.id, 11, "weight", 0.4f)
        peter.addEdge("created", lop, T.id, 12, "weight", 0.2f)
    }

    /**
     * Create the "modern" graph which has the same structure as the "classic" graph from TinkerPop 2.x but includes
     * 3.x features like vertex labels.
     */
    @JvmStatic
    fun createModern(): TinkerCat {
        val g = tinkerCatWithNumberManager
        generateModern(g)
        return g
    }

    /**
     * Generate the graph in [.createModern] into an existing graph.
     */
    @JvmStatic
    fun generateModern(g: TinkerCat) {
        val marko = g.addVertex(T.id, 1, T.label, "person", "name", "marko", "age", 29)
        val vadas = g.addVertex(T.id, 2, T.label, "person", "name", "vadas", "age", 27)
        val lop = g.addVertex(T.id, 3, T.label, "software", "name", "lop", "lang", "java")
        val josh = g.addVertex(T.id, 4, T.label, "person", "name", "josh", "age", 32)
        val ripple = g.addVertex(T.id, 5, T.label, "software", "name", "ripple", "lang", "java")
        val peter = g.addVertex(T.id, 6, T.label, "person", "name", "peter", "age", 35)
        marko.addEdge("knows", vadas, T.id, 7, "weight", 0.5)
        marko.addEdge("knows", josh, T.id, 8, "weight", 1.0)
        marko.addEdge("created", lop, T.id, 9, "weight", 0.4)
        josh.addEdge("created", ripple, T.id, 10, "weight", 1.0)
        josh.addEdge("created", lop, T.id, 11, "weight", 0.4)
        peter.addEdge("created", lop, T.id, 12, "weight", 0.2)
    }

    /**
     * Create the "the crew" graph which is a TinkerPop 3.x toy graph showcasing many 3.x features like meta-properties,
     * multi-properties and graph variables.
     */
    @JvmStatic
    fun createTheCrew(): TinkerCat {
        val conf = numberIdManagerConfiguration
        conf.setProperty(
            TinkerCat.GREMLIN_TINKERGRAPH_DEFAULT_VERTEX_PROPERTY_CARDINALITY,
            VertexProperty.Cardinality.list.name
        )
        val g = open(conf)
        generateTheCrew(g)
        return g
    }

    /**
     * Generate the graph in [.createTheCrew] into an existing graph.
     */
    @JvmStatic
    fun generateTheCrew(g: TinkerCat) {
        val marko = g.addVertex(T.id, 1, T.label, "person", "name", "marko")
        val stephen = g.addVertex(T.id, 7, T.label, "person", "name", "stephen")
        val matthias = g.addVertex(T.id, 8, T.label, "person", "name", "matthias")
        val daniel = g.addVertex(T.id, 9, T.label, "person", "name", "daniel")
        val gremlin = g.addVertex(T.id, 10, T.label, "software", "name", "gremlin")
        val tinkercat = g.addVertex(T.id, 11, T.label, "software", "name", "tinkercat")
        marko.property(VertexProperty.Cardinality.list, "location", "san diego", "startTime", 1997, "endTime", 2001)
        marko.property(VertexProperty.Cardinality.list, "location", "santa cruz", "startTime", 2001, "endTime", 2004)
        marko.property(VertexProperty.Cardinality.list, "location", "brussels", "startTime", 2004, "endTime", 2005)
        marko.property(VertexProperty.Cardinality.list, "location", "santa fe", "startTime", 2005)
        stephen.property(VertexProperty.Cardinality.list, "location", "centreville", "startTime", 1990, "endTime", 2000)
        stephen.property(VertexProperty.Cardinality.list, "location", "dulles", "startTime", 2000, "endTime", 2006)
        stephen.property(VertexProperty.Cardinality.list, "location", "purcellville", "startTime", 2006)
        matthias.property(VertexProperty.Cardinality.list, "location", "bremen", "startTime", 2004, "endTime", 2007)
        matthias.property(VertexProperty.Cardinality.list, "location", "baltimore", "startTime", 2007, "endTime", 2011)
        matthias.property(VertexProperty.Cardinality.list, "location", "oakland", "startTime", 2011, "endTime", 2014)
        matthias.property(VertexProperty.Cardinality.list, "location", "seattle", "startTime", 2014)
        daniel.property(VertexProperty.Cardinality.list, "location", "spremberg", "startTime", 1982, "endTime", 2005)
        daniel.property(
            VertexProperty.Cardinality.list,
            "location",
            "kaiserslautern",
            "startTime",
            2005,
            "endTime",
            2009
        )
        daniel.property(VertexProperty.Cardinality.list, "location", "aachen", "startTime", 2009)
        marko.addEdge("develops", gremlin, T.id, 13, "since", 2009)
        marko.addEdge("develops", tinkercat, T.id, 14, "since", 2010)
        marko.addEdge("uses", gremlin, T.id, 15, "skill", 4)
        marko.addEdge("uses", tinkercat, T.id, 16, "skill", 5)
        stephen.addEdge("develops", gremlin, T.id, 17, "since", 2010)
        stephen.addEdge("develops", tinkercat, T.id, 18, "since", 2011)
        stephen.addEdge("uses", gremlin, T.id, 19, "skill", 5)
        stephen.addEdge("uses", tinkercat, T.id, 20, "skill", 4)
        matthias.addEdge("develops", gremlin, T.id, 21, "since", 2012)
        matthias.addEdge("uses", gremlin, T.id, 22, "skill", 3)
        matthias.addEdge("uses", tinkercat, T.id, 23, "skill", 3)
        daniel.addEdge("uses", gremlin, T.id, 24, "skill", 5)
        daniel.addEdge("uses", tinkercat, T.id, 25, "skill", 3)
        gremlin.addEdge("traverses", tinkercat, T.id, 26)
        g.variables()["creator"] = "marko"
        g.variables()["lastModified"] = 2014
        g.variables()["comment"] =
            "this graph was created to provide examples and test coverage for tinkerpop3 api advances"
    }

    /**
     * Creates the "kitchen sink" graph which is a collection of structures (e.g. self-loops) that aren't represented
     * in other graphs and are useful for various testing scenarios.
     */
    @JvmStatic
    fun createKitchenSink(): TinkerCat {
        val g = tinkerCatWithNumberManager
        generateKitchenSink(g)
        return g
    }

    /**
     * Generate the graph in [.createKitchenSink] into an existing graph.
     */
    fun generateKitchenSink(graph: TinkerCat) {
        val g = graph.traversal()
        g.addV("loops").property(T.id, 1000).property("name", "loop").`as`("me").addE("self").to("me")
            .property(T.id, 1001).iterate()
        g.addV("message").property(T.id, 2000).property("name", "a").`as`("a").addV("message").property(T.id, 2001)
            .property("name", "b").`as`("b").addE("link").from("a").to("b").property(T.id, 2002).addE("link").from("a")
            .to("a").property(T.id, 2003).iterate()
    }

    /**
     * Creates the "grateful dead" graph which is a larger graph than most of the toy graphs but has real-world
     * structure and application and is therefore useful for demonstrating more complex traversals.
     */
    @JvmStatic
    fun createGratefulDead(): TinkerCat {
        val g = tinkerCatWithNumberManager
        generateGratefulDead(g)
        return g
    }

    /**
     * Generate the graph in [.createGratefulDead] into an existing graph.
     */
    fun generateGratefulDead(graph: TinkerCat) {
        val stream = TinkerFactory::class.java.getResourceAsStream("grateful-dead.kryo")
        try {
            graph.io(IoCore.gryo()).reader().create().readGraph(stream, graph)
        } catch (ex: Exception) {
            throw IllegalStateException(ex)
        }
    }

    private val tinkerCatWithNumberManager: TinkerCat
        private get() {
            val conf = numberIdManagerConfiguration
            return open(conf)
        }
    private val numberIdManagerConfiguration: Configuration
        private get() {
            val conf: Configuration = BaseConfiguration()
            conf.setProperty(TinkerCat.GREMLIN_TINKERGRAPH_VERTEX_ID_MANAGER, TinkerCat.DefaultIdManager.INTEGER.name)
            conf.setProperty(TinkerCat.GREMLIN_TINKERGRAPH_EDGE_ID_MANAGER, TinkerCat.DefaultIdManager.INTEGER.name)
            conf.setProperty(
                TinkerCat.GREMLIN_TINKERGRAPH_VERTEX_PROPERTY_ID_MANAGER,
                TinkerCat.DefaultIdManager.LONG.name
            )
            return conf
        }
}