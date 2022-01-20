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
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class ConnectedComponentVertexProgramStep(traversal: Traversal.Admin?) : VertexProgramStep(traversal), TraversalParent,
    Configuring {
    private val parameters: Parameters = Parameters()
    private var edgeTraversal: PureTraversal<Vertex, Edge>? = null
    private var clusterProperty: String = ConnectedComponentVertexProgram.COMPONENT

    init {
        configure(ConnectedComponent.edges, __.< Vertex > bothE < Vertex ? > ())
    }

    @Override
    fun configure(vararg keyValues: Object) {
        if (keyValues[0].equals(ConnectedComponent.edges)) {
            if (keyValues[1] !is Traversal) throw IllegalArgumentException("ConnectedComponent.edges requires a Traversal as its argument")
            edgeTraversal = PureTraversal((keyValues[1] as Traversal<Vertex?, Edge?>).asAdmin())
            this.integrateChild(edgeTraversal.get())
        } else if (keyValues[0].equals(ConnectedComponent.propertyName)) {
            if (keyValues[1] !is String) throw IllegalArgumentException("ConnectedComponent.propertyName requires a String as its argument")
            clusterProperty = keyValues[1]
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
        return super.hashCode() xor clusterProperty.hashCode()
    }

    @Override
    override fun toString(): String {
        return StringFactory.stepString(this, clusterProperty, GraphFilter(this.computer))
    }

    @Override
    fun generateProgram(graph: Graph, memory: Memory): ConnectedComponentVertexProgram {
        val detachedTraversal: Traversal.Admin<Vertex, Edge> = edgeTraversal.getPure()
        detachedTraversal.setStrategies(TraversalStrategies.GlobalCache.getStrategies(graph.getClass()))
        val builder: ConnectedComponentVertexProgram.Builder =
            ConnectedComponentVertexProgram.build().edges(detachedTraversal).property(
                clusterProperty
            )
        if (memory.exists(TraversalVertexProgram.HALTED_TRAVERSERS)) {
            val haltedTraversers: TraverserSet<*> = memory.get(TraversalVertexProgram.HALTED_TRAVERSERS)
            if (!haltedTraversers.isEmpty()) {
                val haltedTraversersValue: Object
                haltedTraversersValue = try {
                    Base64.getEncoder().encodeToString(Serializer.serializeObject(haltedTraversers))
                } catch (ignored: IOException) {
                    haltedTraversers
                }
                builder.configure(TraversalVertexProgram.HALTED_TRAVERSERS, haltedTraversersValue)
            }
        }
        return builder.create(graph)
    }

    @Override
    fun clone(): ConnectedComponentVertexProgramStep {
        return super.clone() as ConnectedComponentVertexProgramStep
    }
}