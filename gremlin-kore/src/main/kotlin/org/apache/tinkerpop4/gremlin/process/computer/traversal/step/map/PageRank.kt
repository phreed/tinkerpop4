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
package org.apache.tinkerpop4.gremlin.process.computer.traversal.step.map

import org.apache.tinkerpop4.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop4.gremlin.structure.Graph

/**
 * Configuration options to be passed to the [GraphTraversal.with] step on
 * [GraphTraversal.pageRank].
 */
object PageRank {
    /**
     * Configures number of iterations that the algorithm should run.
     */
    val times: String = Graph.Hidden.hide("tinkerpop.pageRank.times")

    /**
     * Configures the edge to traverse when calculating the pagerank.
     */
    val edges: String = Graph.Hidden.hide("tinkerpop.pageRank.edges")

    /**
     * Configures the name of the property within which to store the pagerank value.
     */
    val propertyName: String = Graph.Hidden.hide("tinkerpop.pageRank.propertyName")
}