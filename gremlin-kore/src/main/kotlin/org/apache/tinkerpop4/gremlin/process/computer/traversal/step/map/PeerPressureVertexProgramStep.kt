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

import org.apache.tinkerpop4.gremlin.process.computer.GraphFilter

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class PeerPressureVertexProgramStep(traversal: Traversal.Admin?) : VertexProgramStep(traversal), TraversalParent,
    Configuring {
    private val parameters: Parameters = Parameters()
    private var edgeTraversal: PureTraversal<Vertex, Edge>? = null
    private var clusterProperty: String = PeerPressureVertexProgram.CLUSTER
    private var times = 30

    init {
        configure(PeerPressure.edges, __.< Vertex > outE < Vertex ? > ().asAdmin())
    }

    @Override
    fun configure(vararg keyValues: Object) {
        if (keyValues[0].equals(PeerPressure.edges)) {
            if (keyValues[1] !is Traversal) throw IllegalArgumentException("PeerPressure.edges requires a Traversal as its argument")
            edgeTraversal = PureTraversal((keyValues[1] as Traversal<Vertex?, Edge?>).asAdmin())
            this.integrateChild(edgeTraversal.get())
        } else if (keyValues[0].equals(PeerPressure.propertyName)) {
            if (keyValues[1] !is String) throw IllegalArgumentException("PeerPressure.propertyName requires a String as its argument")
            clusterProperty = keyValues[1]
        } else if (keyValues[0].equals(PeerPressure.times)) {
            if (keyValues[1] !is Integer) throw IllegalArgumentException("PeerPressure.times requires an Integer as its argument")
            times = keyValues[1]
        } else {
            parameters.set(this, keyValues)
        }
    }

    @Override
    fun getParameters(): Parameters {
        return parameters
    }

    @Override
    override fun hashCode(): Int {
        return super.hashCode() xor edgeTraversal.hashCode() xor clusterProperty.hashCode() xor times
    }

    @get:Override
    val localChildren: List<Any>
        get() = Collections.singletonList(edgeTraversal.get())

    @Override
    override fun toString(): String {
        return StringFactory.stepString(this, edgeTraversal.get(), clusterProperty, times, GraphFilter(this.computer))
    }

    @Override
    fun generateProgram(graph: Graph, memory: Memory?): PeerPressureVertexProgram {
        val detachedTraversal: Traversal.Admin<Vertex, Edge> = edgeTraversal.getPure()
        detachedTraversal.setStrategies(TraversalStrategies.GlobalCache.getStrategies(graph.getClass()))
        val builder: PeerPressureVertexProgram.Builder = PeerPressureVertexProgram.build()
            .property(clusterProperty)
            .maxIterations(times)
            .edges(detachedTraversal)
        if (this.previousTraversalVertexProgram()) builder.initialVoteStrength(HaltedTraversersCountTraversal())
        return builder.create(graph)
    }

    @get:Override
    val requirements: Set<Any>
        get() = super@TraversalParent.getSelfAndChildRequirements()

    @Override
    fun clone(): PeerPressureVertexProgramStep {
        val clone = super.clone() as PeerPressureVertexProgramStep
        clone.edgeTraversal = edgeTraversal.clone()
        return clone
    }

    @Override
    fun setTraversal(parentTraversal: Traversal.Admin<*, *>?) {
        super.setTraversal(parentTraversal)
        this.integrateChild(edgeTraversal.get())
    }
}