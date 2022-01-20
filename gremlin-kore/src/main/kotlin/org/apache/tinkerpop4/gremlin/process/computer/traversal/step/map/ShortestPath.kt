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

import org.apache.tinkerpop4.gremlin.process.traversal.Traversal

/**
 * Configuration options to be passed to the [GraphTraversal.with] step.
 */
object ShortestPath {
    /**
     * Configures the traversal to use to filter the target vertices for all shortest paths.
     */
    val target: String = Graph.Hidden.hide("tinkerpop.shortestPath.target")

    /**
     * Configures the direction or traversal to use to filter the edges traversed during the shortest path search phase.
     */
    val edges: String = Graph.Hidden.hide("tinkerpop.shortestPath.edges")

    /**
     * Configures the edge property or traversal to use for shortest path distance calculations.
     */
    val distance: String = Graph.Hidden.hide("tinkerpop.shortestPath.distance")

    /**
     * Configures the maximum distance for all shortest paths. Any path with a distance greater than the specified
     * value will not be returned.
     */
    val maxDistance: String = Graph.Hidden.hide("tinkerpop.shortestPath.maxDistance")

    /**
     * Configures the inclusion of edges in the shortest path computation result.
     */
    val includeEdges: String = Graph.Hidden.hide("tinkerpop.shortestPath.includeEdges")
    fun configure(step: ShortestPathVertexProgramStep, key: String?, value: Object?): Boolean {
        if (target.equals(key)) {
            return if (value is Traversal) {
                step.setTargetVertexFilter(value as Traversal?)
                true
            } else throw IllegalArgumentException("ShortestPath.target requires a Traversal as its argument")
        } else if (edges.equals(key)) {
            return if (value is Traversal) {
                step.setEdgeTraversal(value as Traversal?)
                true
            } else if (value is Direction) {
                step.setEdgeTraversal(__.toE(value as Direction?))
                true
            } else throw IllegalArgumentException(
                "ShortestPath.edges requires a Traversal or a Direction as its argument"
            )
        } else if (distance.equals(key)) {
            return if (value is Traversal) {
                step.setDistanceTraversal(value as Traversal?)
                true
            } else if (value is String) {
                // todo: new ElementValueTraversal((String) value)
                step.setDistanceTraversal(__.values(value as String?))
                true
            } else throw IllegalArgumentException(
                "ShortestPath.distance requires a Traversal or a property name as its argument"
            )
        } else if (maxDistance.equals(key)) {
            return if (value is Number) {
                step.setMaxDistance(value as Number?)
                true
            } else throw IllegalArgumentException("ShortestPath.maxDistance requires a Number as its argument")
        } else if (includeEdges.equals(key)) {
            return if (value is Boolean) {
                step.setIncludeEdges(value as Boolean?)
                true
            } else throw IllegalArgumentException("ShortestPath.includeEdges requires a Boolean as its argument")
        }
        return false
    }
}