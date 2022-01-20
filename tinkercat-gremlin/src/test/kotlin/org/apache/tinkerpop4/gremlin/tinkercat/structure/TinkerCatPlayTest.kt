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

import org.apache.tinkerpop4.gremlin.process.computer.Computer
import org.apache.tinkerpop4.gremlin.process.traversal.Operator
import org.apache.tinkerpop4.gremlin.process.traversal.P
import org.apache.tinkerpop4.gremlin.process.traversal.Traversal
import org.apache.tinkerpop4.gremlin.process.traversal.Traverser
import org.apache.tinkerpop4.gremlin.tinkercat.structure.TinkerFactory.createModern
import org.apache.tinkerpop4.gremlin.tinkercat.structure.TinkerCat.Companion.open
import org.apache.tinkerpop4.gremlin.tinkercat.structure.TinkerCat.createIndex
import org.apache.tinkerpop4.gremlin.tinkercat.structure.TinkerCat.io
import kotlin.Throws
import org.apache.tinkerpop4.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import java.util.function.BiFunction
import org.apache.tinkerpop4.gremlin.tinkercat.structure.TinkerCat
import org.apache.tinkerpop4.gremlin.structure.io.graphml.GraphMLIo
import java.util.Arrays
import org.apache.tinkerpop4.gremlin.tinkercat.structure.TinkerCatPlayTest
import org.apache.tinkerpop4.gremlin.process.traversal.strategy.decoration.EventStrategy
import org.apache.tinkerpop4.gremlin.process.traversal.step.util.event.ConsoleMutationListener
import org.apache.tinkerpop4.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop4.gremlin.process.traversal.dsl.graph.__
import org.apache.tinkerpop4.gremlin.process.traversal.strategy.optimization.PathRetractionStrategy
import org.apache.tinkerpop4.gremlin.structure.*
import org.apache.tinkerpop4.gremlin.util.TimeUtil
import org.junit.Ignore
import org.junit.Test
import org.slf4j.LoggerFactory
import java.lang.Exception
import java.util.function.Consumer
import java.util.function.Supplier

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class TinkerCatPlayTest {
    @Test
    @Ignore
    @Throws(Exception::class)
    fun testPlay8() {
        val graph: Graph = createModern()
        val g = graph.traversal()
        println(
            g.withSack(1).inject(1)
                .repeat(__.sack<Any, Any, Any>(Operator.sum as BiFunction<*, *, *>).by(__.constant(1))).times(10).emit()
                .math("sin _").by(
                __.sack<Any, Any>()
            ).toList()
        )
    }

    @Test
    @Ignore
    @Throws(Exception::class)
    fun benchmarkStandardTraversals() {
        val graph: Graph = open()
        val g = graph.traversal()
        graph.io(GraphMLIo.build()).readGraph("data/grateful-dead.xml")
        val traversals = Arrays.asList(
            Supplier<Traversal<*, *>> { g.V().outE().inV().outE().inV().outE().inV() },
            Supplier<Traversal<*, *>> { g.V().out().out().out() },
            Supplier<Traversal<*, *>> { g.V().out().out().out().path() },
            Supplier<Traversal<*, *>> { g.V().repeat(__.out()).times(2) },
            Supplier<Traversal<*, *>> { g.V().repeat(__.out()).times(3) },
            Supplier<Traversal<*, *>> { g.V().local(__.out().out().values<Any>("name").fold()) },
            Supplier<Traversal<*, *>> { g.V().out().local(__.out().out().values<Any>("name").fold()) },
            Supplier<Traversal<*, *>> {
                g.V().out().map { v: Traverser<Vertex?> -> g.V(v.get()).out().out().values<Any>("name").toList() }
            }
        )
        traversals.forEach(Consumer { traversal: Supplier<Traversal<*, *>> ->
            logger.info("\nTESTING: {}", traversal.get())
            for (i in 0..6) {
                val t = System.currentTimeMillis()
                traversal.get().iterate()
                print("   " + (System.currentTimeMillis() - t))
            }
        })
    }

    @Test
    @Ignore
    @Throws(Exception::class)
    fun testPlay4() {
        val graph: Graph = open()
        graph.io(GraphMLIo.build()).readGraph("/Users/marko/software/tinkerpop/tinkerpop3/data/grateful-dead.xml")
        val g = graph.traversal()
        val traversals = Arrays.asList(
            Supplier<Traversal<*, *>> {
                g.V().has(T.label, "song").out().groupCount<Any>().by { t: Vertex? ->
                    g.V(t).choose(
                        { r: Vertex? -> g.V(r).has(T.label, "artist").hasNext() },
                        __.`in`("writtenBy", "sungBy"),
                        __.both("followedBy")
                    ).values<Any>("name").next()
                }
                    .fold()
            },
            Supplier<Traversal<*, *>> {
                g.V().has(T.label, "song").out().groupCount<Any>().by { t: Vertex? ->
                    g.V(t).choose(
                        __.has<Any>(T.label, "artist"),
                        __.`in`("writtenBy", "sungBy"),
                        __.both("followedBy")
                    ).values<Any>("name").next()
                }
                    .fold()
            },
            Supplier<Traversal<*, *>> {
                g.V().has(T.label, "song").out().groupCount<Any>().by(
                    __.choose<Any, Any, Vertex>(
                        __.has(T.label, "artist"),
                        __.`in`("writtenBy", "sungBy"),
                        __.both("followedBy")
                    ).values<Any>("name")
                ).fold()
            },
            Supplier<Traversal<*, *>> {
                g.V().has(T.label, "song").both().groupCount<Any>()
                    .by { t: Vertex? -> g.V(t).both().values<Any>("name").next() }
            },
            Supplier<Traversal<*, *>> {
                g.V().has(T.label, "song").both().groupCount<Any>().by(__.both().values<Any>("name"))
            })
        traversals.forEach(Consumer { traversal: Supplier<Traversal<*, *>> ->
            logger.info("\nTESTING: {}", traversal.get())
            for (i in 0..9) {
                val t = System.currentTimeMillis()
                traversal.get().iterate()
                //System.out.println(traversal.get().toList());
                print("   " + (System.currentTimeMillis() - t))
            }
        })
    }

    @Test
    @Ignore
    @Throws(Exception::class)
    fun testPlayDK() {
        val graph: Graph = open()
        val strategy = EventStrategy.build().addListener(ConsoleMutationListener(graph)).create()
        val g = graph.traversal().withStrategies(strategy)
        g.addV().property(T.id, 1).iterate()
        g.V(1).property("name", "name1").iterate()
        g.V(1).property("name", "name2").iterate()
        g.V(1).property("name", "name2").iterate()
        g.addV().property(T.id, 2).iterate()
        g.V(2).property(VertexProperty.Cardinality.list, "name", "name1").iterate()
        g.V(2).property(VertexProperty.Cardinality.list, "name", "name2").iterate()
        g.V(2).property(VertexProperty.Cardinality.list, "name", "name2").iterate()
        g.addV().property(T.id, 3).iterate()
        g.V(3).property(VertexProperty.Cardinality.set, "name", "name1", "ping", "pong").iterate()
        g.V(3).property(VertexProperty.Cardinality.set, "name", "name2", "ping", "pong").iterate()
        g.V(3).property(VertexProperty.Cardinality.set, "name", "name2", "pong", "ping").iterate()
    }

    @Test
    @Ignore
    @Throws(Exception::class)
    fun testPlay7() {
        /*TinkerCat graph = TinkerCat.open();
        graph.createIndex("name",Vertex.class);
        graph.io(GraphMLIo.build()).readGraph("/Users/marko/software/tinkerpop/tinkerpop3/data/grateful-dead.xml");*/
        //System.out.println(g.V().properties().key().groupCount().next());
        val graph = createModern()
        val g = graph.traversal()
        val traversals = Arrays.asList(
            Supplier<GraphTraversal<*, *>> {
                g.V().out().`as`("v").match<Any>(
                    __.`as`<Any>("v").outE().count().`as`("outDegree"),
                    __.`as`<Any>("v").inE().count().`as`("inDegree")
                ).select<Any>("v", "outDegree", "inDegree").by(__.valueMap<Element, Any>()).by().by().local(
                    __.union<Any, Map<String, Any>>(
                        __.select<Any, Map<String, Any>>("v"),
                        __.select<Any, Any>("inDegree", "outDegree")
                    ).unfold<Any>().fold()
                )
            }
        )
        traversals.forEach(Consumer { traversal: Supplier<GraphTraversal<*, *>> ->
            logger.info("pre-strategy:  {}", traversal.get())
            logger.info("post-strategy: {}", traversal.get().iterate())
            logger.info(TimeUtil.clockWithResult(50) { traversal.get().toList() }
                .toString())
        })
    }

    @Test
    @Ignore
    @Throws(Exception::class)
    fun testPlay5() {
        val graph = open()
        graph.createIndex("name", Vertex::class.java)
        graph.io(GraphMLIo.build()).readGraph("/Users/marko/software/tinkerpop/tinkerpop3/data/grateful-dead.xml")
        val g = graph.traversal()
        val traversal = Supplier<Traversal<*, *>> {
            g.V().match<Any>(
                __.`as`<Any>("a").has("name", "Garcia"),
                __.`as`<Any>("a").`in`("writtenBy").`as`("b"),
                __.`as`<Any>("b").out("followedBy").`as`("c"),
                __.`as`<Any>("c").out("writtenBy").`as`("d"),
                __.`as`<Any>("d").where(P.neq("a"))
            ).select<Any>("a", "b", "c", "d").by("name")
        }
        logger.info(traversal.get().toString())
        logger.info(traversal.get().iterate<Any, Any>().toString())
        traversal.get().forEachRemaining { x: Any -> logger.info(x.toString()) }
    }

    @Test
    @Ignore
    @Throws(Exception::class)
    fun testPaths() {
        val graph = open()
        graph.io(GraphMLIo.build())
            .readGraph("/Users/twilmes/work/repos/scratch/tinkerpop/gremlin-test/src/main/resources/org/apache/tinkerpop/gremlin/structure/io/graphml/grateful-dead.xml")
        //        graph = TinkerFactory.createModern();
        val g = graph.traversal().withComputer(Computer.compute().workers(1))
        println(
            g.V().match<Any>(
                __.`as`<Any>("a").`in`("sungBy").`as`("b"),
                __.`as`<Any>("a").`in`("sungBy").`as`("c"),
                __.`as`<Any>("b").out("writtenBy").`as`("d"),
                __.`as`<Any>("c").out("writtenBy").`as`("e"),
                __.`as`<Any>("d").has("name", "George_Harrison"),
                __.`as`<Any>("e").has("name", "Bob_Marley")
            ).select<Any>("a").count().next()
        )

//        System.out.println(g.V().out("created").
//                project("a","b").
//                by("name").
//                by(__.in("created").count()).
//                order().by(select("b")).
//                select("a").toList());

//        System.out.println(g.V().as("a").out().where(neq("a")).barrier().out().count().profile().next());
//        System.out.println(g.V().out().as("a").where(out().select("a").values("prop").count().is(gte(1))).out().where(neq("a")).toList());
//        System.out.println(g.V().match(
//                __.as("a").out().as("b"),
//                __.as("b").out().as("c")).select("c").count().profile().next());
    }

    @Test
    @Ignore
    @Throws(Exception::class)
    fun testPlay9() {
        val graph: Graph = open()
        graph.io(GraphMLIo.build()).readGraph("../data/grateful-dead.xml")
        val g = graph.traversal().withComputer(Computer.compute().workers(4))
            .withStrategies(PathRetractionStrategy.instance())
        val h = graph.traversal().withComputer(Computer.compute().workers(4)).withoutStrategies(
            PathRetractionStrategy::class.java
        )
        for (source in Arrays.asList(g, h)) {
            println(
                source.V().match<Any>(
                    __.`as`<Any>("a").`in`("sungBy").`as`("b"),
                    __.`as`<Any>("a").`in`("sungBy").`as`("c"),
                    __.`as`<Any>("b").out("writtenBy").`as`("d"),
                    __.`as`<Any>("c").out("writtenBy").`as`("e"),
                    __.`as`<Any>("d").has("name", "George_Harrison"),
                    __.`as`<Any>("e").has("name", "Bob_Marley")
                ).select<Any>("a").count().profile().next()
            )
        }
    }

    @Test
    @Ignore
    @Throws(Exception::class)
    fun testPlay6() {
        val graph: Graph = open()
        val g = graph.traversal()
        for (i in 0..999) {
            graph.addVertex(T.label, "person", T.id, i)
        }
        graph.vertices().forEachRemaining { a: Vertex ->
            graph.vertices().forEachRemaining { b: Vertex ->
                if (a !== b) {
                    a.addEdge("knows", b)
                }
            }
        }
        graph.vertices(50).next().addEdge("uncle", graph.vertices(70).next())
        logger.info(TimeUtil.clockWithResult(500) {
            g.V().match<Any>(__.`as`<Any>("a").out("knows").`as`("b"), __.`as`<Any>("a").out("uncle").`as`("b"))
                .toList()
        }
            .toString())
    }

    @Test
    @Ignore
    fun testBugs() {
        val g = createModern().traversal()
        val o1: Any = g.V().map(__.V<Any>(1))
        println(g.V().`as`("a").both().`as`("b").dedup("a", "b").by(T.label).select<Any>("a", "b").explain())
        println(g.V().`as`("a").both().`as`("b").dedup("a", "b").by(T.label).select<Any>("a", "b").toList())
        val t: Traversal<*, *> = g.V("3").union(
            __.repeat(__.out().simplePath()).times(2).count(),
            __.repeat(__.`in`().simplePath()).times(2).count()
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TinkerCatPlayTest::class.java)
    }
}