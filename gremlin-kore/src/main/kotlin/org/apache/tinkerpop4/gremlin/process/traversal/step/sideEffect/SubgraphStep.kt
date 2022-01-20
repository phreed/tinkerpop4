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
package org.apache.tinkerpop4.gremlin.process.traversal.step.sideEffect

import org.apache.tinkerpop4.gremlin.process.traversal.Operator

/**
 * A side-effect step that produces an edge induced subgraph.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class SubgraphStep(traversal: Traversal.Admin?, @get:Override val sideEffectKey: String) :
    SideEffectStep<Edge?>(traversal), SideEffectCapable {
    private var subgraph: Graph? = null
    private var parentGraphFeatures: VertexFeatures? = null
    private var subgraphSupportsMetaProperties = false

    init {
        this.getTraversal().asAdmin().getSideEffects().registerIfAbsent(sideEffectKey, {
            GraphFactory.open(
                DEFAULT_CONFIGURATION
            )
        }, Operator.assign)
    }

    @Override
    protected fun sideEffect(traverser: Traverser.Admin<Edge?>) {
        parentGraphFeatures = (traversal.getGraph().get() as Graph).features().vertex()
        if (null == subgraph) {
            subgraph = traverser.sideEffects(sideEffectKey)
            if (!subgraph.features().vertex().supportsUserSuppliedIds() || !subgraph.features().edge()
                    .supportsUserSuppliedIds()
            ) throw IllegalArgumentException("The provided subgraph must support user supplied ids for vertices and edges: " + subgraph)
        }
        subgraphSupportsMetaProperties = subgraph.features().vertex().supportsMetaProperties()
        addEdgeToSubgraph(traverser.get())
    }

    @Override
    override fun toString(): String {
        return StringFactory.stepString(this, sideEffectKey)
    }

    @get:Override
    val requirements: Set<Any>
        get() = REQUIREMENTS

    @Override
    fun clone(): SubgraphStep {
        val clone = super.clone() as SubgraphStep
        subgraph = null
        return clone
    }

    @Override
    override fun hashCode(): Int {
        return super.hashCode() xor sideEffectKey.hashCode()
    }

    ///
    private fun getOrCreate(vertex: Vertex): Vertex {
        val vertexIterator: Iterator<Vertex> = subgraph.vertices(vertex.id())
        try {
            if (vertexIterator.hasNext()) return vertexIterator.next()
        } finally {
            CloseableIterator.closeIterator(vertexIterator)
        }
        val subgraphVertex: Vertex = subgraph.addVertex(T.id, vertex.id(), T.label, vertex.label())
        vertex.properties().forEachRemaining { vertexProperty ->
            val cardinality: VertexProperty.Cardinality = parentGraphFeatures.getCardinality(vertexProperty.key())
            val subgraphVertexProperty: VertexProperty<*> = subgraphVertex.property(
                cardinality,
                vertexProperty.key(),
                vertexProperty.value(),
                T.id,
                vertexProperty.id()
            )

            // only iterate the VertexProperties if the current graph can have them and if the subgraph can support
            // them. unfortunately we don't have a way to write a test for this as we dont' have a graph that supports
            // user supplied ids and doesn't support metaproperties.
            if (parentGraphFeatures.supportsMetaProperties() && subgraphSupportsMetaProperties) {
                vertexProperty.properties()
                    .forEachRemaining { property -> subgraphVertexProperty.property(property.key(), property.value()) }
            }
        }
        return subgraphVertex
    }

    private fun addEdgeToSubgraph(edge: Edge) {
        val edgeIterator: Iterator<Edge> = subgraph.edges(edge.id())
        try {
            if (edgeIterator.hasNext()) return
        } finally {
            CloseableIterator.closeIterator(edgeIterator)
        }
        val vertexIterator: Iterator<Vertex> = edge.vertices(Direction.BOTH)
        val subGraphOutVertex: Vertex = getOrCreate(vertexIterator.next())
        val subGraphInVertex: Vertex = getOrCreate(vertexIterator.next())
        val subGraphEdge: Edge = subGraphOutVertex.addEdge(edge.label(), subGraphInVertex, T.id, edge.id())
        edge.properties().forEachRemaining { property -> subGraphEdge.property(property.key(), property.value()) }
    }

    companion object {
        private val REQUIREMENTS: Set<TraverserRequirement> = EnumSet.of(
            TraverserRequirement.OBJECT,
            TraverserRequirement.SIDE_EFFECTS
        )
        private val DEFAULT_CONFIGURATION: Map<String, Object> = object : HashMap<String?, Object?>() {
            init {
                put(
                    Graph.GRAPH,
                    "org.apache.tinkerpop4.gremlin.tinkergraph.structure.TinkerGraph"
                ) // hard coded because TinkerGraph is not part of gremlin-core
            }
        }
    }
}