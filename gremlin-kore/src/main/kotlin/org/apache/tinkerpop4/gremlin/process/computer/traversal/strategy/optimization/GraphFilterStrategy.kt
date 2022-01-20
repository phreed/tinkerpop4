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
package org.apache.tinkerpop4.gremlin.process.computer.traversal.strategy.optimization

import org.apache.tinkerpop4.gremlin.process.computer.Computer

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class GraphFilterStrategy private constructor() : AbstractTraversalStrategy<OptimizationStrategy?>(),
    OptimizationStrategy {
    @Override
    fun apply(traversal: Traversal.Admin<*, *>) {
        if (TraversalHelper.getStepsOfAssignableClass(VertexProgramStep::class.java, traversal)
                .size() > 1
        ) // do not do if there is an OLAP chain
            return
        val graph: Graph = traversal.getGraph()
            .orElse(EmptyGraph.instance()) // given that this strategy only works for single OLAP jobs, the graph is the traversal graph
        for (step in TraversalHelper.getStepsOfClass(
            TraversalVertexProgramStep::class.java,
            traversal
        )) {   // will be zero or one step
            val computerTraversal: Traversal.Admin<*, *> =
                step.generateProgram(graph, EmptyMemory.instance()).getTraversal().get().clone()
            if (!computerTraversal.isLocked()) computerTraversal.applyStrategies()
            val computer: Computer = step.getComputer()
            if (null == computer.getEdges() && !GraphComputer.Persist.EDGES.equals(computer.getPersist())) {  // if edges() already set, use it
                val edgeFilter: Traversal.Admin<Vertex, Edge>? = getEdgeFilter(computerTraversal)
                if (null != edgeFilter) // if no edges can be filtered, then don't set edges()
                    step.setComputer(computer.edges(edgeFilter))
            }
        }
    }

    companion object {
        private val INSTANCE = GraphFilterStrategy()
        protected fun getEdgeFilter(traversal: Traversal.Admin<*, *>): Traversal.Admin<Vertex, Edge>? {
            if (traversal.getStartStep() is GraphStep && (traversal.getStartStep() as GraphStep).returnsEdge()) return null // if the traversal is an edge traversal, don't filter (this can be made less stringent)
            if (TraversalHelper.hasStepOfAssignableClassRecursively(
                    LambdaHolder::class.java,
                    traversal
                )
            ) return null // if the traversal contains lambdas, don't filter as you don't know what is being accessed by the lambdas
            val directionLabels: Map<Direction, Set<String>> = EnumMap(Direction::class.java)
            val outLabels: Set<String?> = HashSet()
            val inLabels: Set<String?> = HashSet()
            val bothLabels: Set<String?> = HashSet()
            directionLabels.put(Direction.OUT, outLabels)
            directionLabels.put(Direction.IN, inLabels)
            directionLabels.put(Direction.BOTH, bothLabels)
            TraversalHelper.getStepsOfAssignableClassRecursively(VertexStep::class.java, traversal).forEach { step ->
                // in-edge traversals require the outgoing edges for attachment
                val direction: Direction = if (step.getDirection()
                        .equals(Direction.IN) && step.returnsEdge()
                ) Direction.BOTH else step.getDirection()
                val edgeLabels: Array<String> = step.getEdgeLabels()
                if (edgeLabels.size == 0) directionLabels[direction].add(null) // null means all edges (don't filter)
                else Collections.addAll(
                    directionLabels[direction],
                    edgeLabels
                ) // add edge labels associated with that direction
            }
            for (label in outLabels) { // if both in and out share the same labels, add them to both
                if (inLabels.contains(label)) {
                    bothLabels.add(label)
                }
            }
            if (bothLabels.contains(null)) // if both on everything, you can't edges() filter
                return null
            for (label in bothLabels) { // remove labels from out and in that are already handled by both
                outLabels.remove(label)
                inLabels.remove(label)
            }
            // construct edges(...)
            return if (outLabels.isEmpty() && inLabels.isEmpty() && bothLabels.isEmpty()) // out/in/both are never called, thus, filter all edges
                __.< Vertex > bothE < Vertex ? > ().limit(0).asAdmin() else {
                val ins =
                    if (inLabels.contains(null)) arrayOf<String>() else inLabels.toArray(arrayOfNulls<String>(inLabels.size()))
                val outs = if (outLabels.contains(null)) arrayOf<String>() else outLabels.toArray(
                    arrayOfNulls<String>(outLabels.size())
                )
                val boths = if (bothLabels.contains(null)) arrayOf<String>() else bothLabels.toArray(
                    arrayOfNulls<String>(bothLabels.size())
                )
                if (outLabels.isEmpty() && inLabels.isEmpty()) // only both has labels
                    return __.< Vertex > bothE < Vertex ? > boths.asAdmin() else if (inLabels.isEmpty() && bothLabels.isEmpty()) // only out has labels
                    return __.< Vertex > outE < Vertex ? > outs.asAdmin() else if (outLabels.isEmpty() && bothLabels.isEmpty()) // only in has labels
                    return __.< Vertex > inE < Vertex ? > ins.asAdmin() else if (bothLabels.isEmpty()) // out and in both have labels
                    return __.< Vertex, Edge>union<Vertex?, Edge?>(__.inE(ins), __.outE(outs)).asAdmin() else if (outLabels.isEmpty() && ins.size > 0) // in and both have labels (and in is not null)
                return __.< Vertex, Edge>union<Vertex?, Edge?>(__.inE(ins), __.bothE(boths)).asAdmin() else if (inLabels.isEmpty() && outs.size > 0) // out and both have labels (and out is not null)
                __.< Vertex, Edge>union<Vertex?, Edge?>(__.outE(outs), __.bothE(boths)).asAdmin() else return null
                //throw new IllegalStateException("The label combination should not have reached this point: " + outLabels + "::" + inLabels + "::" + bothLabels);
            }
        }

        fun instance(): GraphFilterStrategy {
            return INSTANCE
        }
    }
}