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
package org.apache.tinkerpop4.gremlin.tinkercat

import io.cucumber.java.Scenario
import org.apache.commons.configuration2.BaseConfiguration
import org.apache.commons.configuration2.Configuration
import org.apache.commons.configuration2.MapConfiguration
import org.apache.tinkerpop4.gremlin.tinkercat.structure.TinkerFactory.createModern
import org.apache.tinkerpop4.gremlin.tinkercat.structure.TinkerFactory.createClassic
import org.apache.tinkerpop4.gremlin.tinkercat.structure.TinkerFactory.createTheCrew
import org.apache.tinkerpop4.gremlin.tinkercat.structure.TinkerFactory.createKitchenSink
import org.apache.tinkerpop4.gremlin.tinkercat.structure.TinkerFactory.createGratefulDead
import org.apache.tinkerpop4.gremlin.tinkercat.structure.TinkerCat.Companion.open
import org.apache.tinkerpop4.gremlin.features.World
import org.apache.tinkerpop4.gremlin.LoadGraphWith.GraphData
import org.apache.tinkerpop4.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop4.gremlin.tinkercat.structure.TinkerCat
import org.apache.tinkerpop4.gremlin.tinkercat.TinkerCatWorld
import org.apache.tinkerpop4.gremlin.LoadGraphWith
import java.lang.UnsupportedOperationException
import org.apache.tinkerpop4.gremlin.process.computer.traversal.strategy.decoration.VertexProgramStrategy
import org.apache.tinkerpop4.gremlin.process.computer.GraphComputer
import org.apache.tinkerpop4.gremlin.tinkercat.process.computer.TinkerCatComputer
import org.apache.tinkerpop4.gremlin.TestHelper
import org.junit.AssumptionViolatedException
import java.io.File
import java.util.Arrays
import java.util.HashMap
import java.util.stream.Collectors

/**
 * The [World] implementation for TinkerCat that provides the [GraphTraversalSource] instances required
 * by the Gherkin test suite.
 */
open class TinkerCatWorld : World {
    override fun getGraphTraversalSource(graphData: GraphData): GraphTraversalSource {
        return if (null == graphData) open(numberIdManagerConfiguration)
            .traversal() else if (graphData == GraphData.CLASSIC) classic.traversal() else if (graphData == GraphData.CREW) crew.traversal() else if (graphData == GraphData.MODERN) modern.traversal() else if (graphData == GraphData.SINK) sink.traversal() else if (graphData == GraphData.GRATEFUL) grateful.traversal() else throw UnsupportedOperationException(
            "GraphData not supported: " + graphData.name
        )
    }

    /**
     * We cannot use the File.separator here as that is not a valid gremlin character.
     * @param pathToFileFromGremlin the path to a data file as taken from the Gherkin tests
     * @return
     */
    override fun changePathToDataFile(pathToFileFromGremlin: String): String {
        return ".." + File.separator + pathToFileFromGremlin
    }

    /**
     * Enables the storing of `null` property values when testing.
     */
    class NullWorld : TinkerCatWorld() {
        override fun getGraphTraversalSource(graphData: GraphData): GraphTraversalSource {
            if (graphData != null) throw UnsupportedOperationException("GraphData not supported: " + graphData.name)
            val conf = numberIdManagerConfiguration
            conf.setProperty(TinkerCat.GREMLIN_TINKERGRAPH_ALLOW_NULL_PROPERTY_VALUES, true)
            return open(conf).traversal()
        }
    }

    /**
     * Turns on [GraphComputer] when testing.
     */
    class ComputerWorld : TinkerCatWorld() {
        override fun beforeEachScenario(scenario: Scenario) {
            val ignores = TAGS_TO_IGNORE.stream().filter { t: String? -> scenario.sourceTagNames.contains(t) }
                .collect(Collectors.toList())
            if (!ignores.isEmpty()) throw AssumptionViolatedException(
                String.format(
                    "This scenario is not supported with GraphComputer: %s",
                    ignores
                )
            )

            // the following needs some further investigation.........may need to improve the definition of result
            // equality with map<list>
            val scenarioName = scenario.name
            if (SCENARIOS_TO_IGNORE.contains(scenarioName)) throw AssumptionViolatedException("There are some internal ordering issues with result where equality is not required but is being enforced")
        }

        override fun getGraphTraversalSource(graphData: GraphData): GraphTraversalSource {
            if (null == graphData) throw AssumptionViolatedException("GraphComputer does not support mutation")
            return super.getGraphTraversalSource(graphData)
                .withStrategies(VertexProgramStrategy.create(MapConfiguration(object : HashMap<String?, Any?>() {
                    init {
                        put(VertexProgramStrategy.WORKERS, Runtime.getRuntime().availableProcessors())
                        put(
                            VertexProgramStrategy.GRAPH_COMPUTER,
                            if (RANDOM.nextBoolean()) GraphComputer::class.java.canonicalName else TinkerCatComputer::class.java.canonicalName
                        )
                    }
                })))
        }

        companion object {
            private val RANDOM = TestHelper.RANDOM
            private val TAGS_TO_IGNORE = Arrays.asList(
                "@StepDrop",
                "@StepV",
                "@GraphComputerVerificationOneBulk",
                "@GraphComputerVerificationStrategyNotSupported",
                "@GraphComputerVerificationMidVNotSupported",
                "@GraphComputerVerificationInjectionNotSupported",
                "@GraphComputerVerificationStarGraphExceeded",
                "@GraphComputerVerificationReferenceOnly"
            )
            private val SCENARIOS_TO_IGNORE = Arrays.asList(
                "g_V_group_byXoutE_countX_byXnameX",
                "g_V_asXvX_mapXbothE_weight_foldX_sumXlocalX_asXsX_selectXv_sX_order_byXselectXsX_descX",
                "g_V_hasXlangX_groupXaX_byXlangX_byXnameX_out_capXaX",
                "g_withStrategiesXProductiveByStrategyX_V_group_byXageX",
                "g_V_order_byXoutE_count_descX",
                "g_V_both_both_dedup_byXoutE_countX_name",
                "g_V_mapXbothE_weight_foldX_order_byXsumXlocalX_descX",
                "g_V_hasLabelXsoftwareX_order_byXnameX_index_withXmapX",
                "g_V_order_byXname_descX_barrier_dedup_age_name"
            )
        }
    }

    companion object {
        private val modern = createModern()
        private val classic = createClassic()
        private val crew = createTheCrew()
        private val sink = createKitchenSink()
        private val grateful = createGratefulDead()
        private val numberIdManagerConfiguration: Configuration
            private get() {
                val conf: Configuration = BaseConfiguration()
                conf.setProperty(
                    TinkerCat.GREMLIN_TINKERGRAPH_VERTEX_ID_MANAGER,
                    TinkerCat.DefaultIdManager.INTEGER.name
                )
                conf.setProperty(TinkerCat.GREMLIN_TINKERGRAPH_EDGE_ID_MANAGER, TinkerCat.DefaultIdManager.INTEGER.name)
                conf.setProperty(
                    TinkerCat.GREMLIN_TINKERGRAPH_VERTEX_PROPERTY_ID_MANAGER,
                    TinkerCat.DefaultIdManager.LONG.name
                )
                return conf
            }
    }
}