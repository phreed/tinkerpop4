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
package org.apache.tinkerpop4.gremlin.tinkercat.process

import org.apache.commons.configuration2.MapConfiguration
import org.apache.tinkerpop4.gremlin.GraphProvider
import org.apache.tinkerpop4.gremlin.tinkercat.process.computer.TinkerCatComputer
import org.apache.tinkerpop4.gremlin.tinkercat.TinkerCatProvider
import org.apache.tinkerpop4.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop4.gremlin.process.computer.traversal.strategy.decoration.VertexProgramStrategy
import org.apache.tinkerpop4.gremlin.tinkercat.process.TinkerCatComputerProvider
import org.apache.tinkerpop4.gremlin.process.computer.GraphComputer
import org.apache.tinkerpop4.gremlin.TestHelper
import org.apache.tinkerpop4.gremlin.structure.Graph
import java.util.HashMap

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
@GraphProvider.Descriptor(computer = TinkerCatComputer::class)
open class TinkerCatComputerProvider : TinkerCatProvider() {
    override fun traversal(graph: Graph): GraphTraversalSource {
        return graph.traversal()
            .withStrategies(VertexProgramStrategy.create(MapConfiguration(object : HashMap<String?, Any?>() {
                init {
                    put(VertexProgramStrategy.WORKERS, RANDOM.nextInt(Runtime.getRuntime().availableProcessors()) + 1)
                    put(
                        VertexProgramStrategy.GRAPH_COMPUTER,
                        if (RANDOM.nextBoolean()) GraphComputer::class.java.canonicalName else TinkerCatComputer::class.java.canonicalName
                    )
                }
            })))
    }

    companion object {
        private val RANDOM = TestHelper.RANDOM
    }
}