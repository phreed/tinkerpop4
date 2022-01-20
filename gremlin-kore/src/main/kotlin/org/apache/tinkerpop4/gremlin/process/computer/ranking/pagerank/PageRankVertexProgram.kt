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
package org.apache.tinkerpop4.gremlin.process.computer.ranking.pagerank

import org.apache.commons.configuration2.Configuration

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class PageRankVertexProgram private constructor() : VertexProgram<Double?> {
    private var incidentMessageScope: MessageScope.Local<Double> = MessageScope.Local.of(__::outE)
    private var countMessageScope: MessageScope.Local<Double> = MessageScope.Local.of(
        ReverseTraversalSupplier(
            incidentMessageScope
        )
    )
    private var edgeTraversal: PureTraversal<Vertex, Edge>? = null
    private var initialRankTraversal: PureTraversal<Vertex, out Number?>? = null
    private var alpha = 0.85
    private var epsilon = 0.00001
    private var maxIterations = 20
    private var property = PAGE_RANK
    private var vertexComputeKeys: Set<VertexComputeKey>? = null
    private var memoryComputeKeys: Set<MemoryComputeKey>? = null
    @Override
    fun loadState(graph: Graph?, configuration: Configuration) {
        if (configuration.containsKey(INITIAL_RANK_TRAVERSAL)) initialRankTraversal =
            PureTraversal.loadState(configuration, INITIAL_RANK_TRAVERSAL, graph)
        if (configuration.containsKey(EDGE_TRAVERSAL)) {
            edgeTraversal = PureTraversal.loadState(configuration, EDGE_TRAVERSAL, graph)
            incidentMessageScope = MessageScope.Local.of { edgeTraversal.get().clone() }
            countMessageScope = MessageScope.Local.of(ReverseTraversalSupplier(incidentMessageScope))
        }
        alpha = configuration.getDouble(ALPHA, alpha)
        epsilon = configuration.getDouble(EPSILON, epsilon)
        maxIterations = configuration.getInt(MAX_ITERATIONS, 20)
        property = configuration.getString(PROPERTY, PAGE_RANK)
        vertexComputeKeys = HashSet(
            Arrays.asList(
                VertexComputeKey.of(property, false),
                VertexComputeKey.of(EDGE_COUNT, true)
            )
        )
        memoryComputeKeys = HashSet(
            Arrays.asList(
                MemoryComputeKey.of(TELEPORTATION_ENERGY, Operator.sum, true, true),
                MemoryComputeKey.of(VERTEX_COUNT, Operator.sum, true, true),
                MemoryComputeKey.of(CONVERGENCE_ERROR, Operator.sum, false, true)
            )
        )
    }

    @Override
    fun storeState(configuration: Configuration) {
        super@VertexProgram.storeState(configuration)
        configuration.setProperty(ALPHA, alpha)
        configuration.setProperty(EPSILON, epsilon)
        configuration.setProperty(PROPERTY, property)
        configuration.setProperty(MAX_ITERATIONS, maxIterations)
        if (null != edgeTraversal) edgeTraversal.storeState(configuration, EDGE_TRAVERSAL)
        if (null != initialRankTraversal) initialRankTraversal.storeState(configuration, INITIAL_RANK_TRAVERSAL)
    }

    @get:Override
    val preferredResultGraph: ResultGraph
        get() = GraphComputer.ResultGraph.NEW

    @get:Override
    val preferredPersist: Persist
        get() = GraphComputer.Persist.VERTEX_PROPERTIES

    @Override
    fun getVertexComputeKeys(): Set<VertexComputeKey>? {
        return vertexComputeKeys
    }

    @get:Override
    val messageCombiner: Optional<MessageCombiner<Double>>
        get() = PageRankMessageCombiner.instance() as Optional

    @Override
    fun getMemoryComputeKeys(): Set<MemoryComputeKey>? {
        return memoryComputeKeys
    }

    @Override
    fun getMessageScopes(memory: Memory): Set<MessageScope> {
        val set: Set<MessageScope> = HashSet()
        set.add(if (memory.isInitialIteration()) countMessageScope else incidentMessageScope)
        return set
    }

    @Override
    fun clone(): PageRankVertexProgram {
        return try {
            val clone = super.clone() as PageRankVertexProgram
            if (null != initialRankTraversal) clone.initialRankTraversal = initialRankTraversal.clone()
            clone
        } catch (e: CloneNotSupportedException) {
            throw IllegalStateException(e.getMessage(), e)
        }
    }

    @Override
    fun setup(memory: Memory) {
        memory.set(TELEPORTATION_ENERGY, if (null == initialRankTraversal) 1.0 else 0.0)
        memory.set(VERTEX_COUNT, 0.0)
        memory.set(CONVERGENCE_ERROR, 1.0)
    }

    @Override
    fun execute(vertex: Vertex, messenger: Messenger<Double?>, memory: Memory) {
        if (memory.isInitialIteration()) {
            messenger.sendMessage(countMessageScope, 1.0)
            memory.add(VERTEX_COUNT, 1.0)
        } else {
            val vertexCount: Double = memory.< Double > get < Double ? > VERTEX_COUNT
            val edgeCount: Double
            var pageRank: Double
            if (1 == memory.getIteration()) {
                edgeCount = IteratorUtils.reduce(messenger.receiveMessages(), 0.0) { a, b -> a + b }
                vertex.property(VertexProperty.Cardinality.single, EDGE_COUNT, edgeCount)
                pageRank =
                    if (null == initialRankTraversal) 0.0 else TraversalUtil.apply(vertex, initialRankTraversal.get())
                        .doubleValue()
            } else {
                edgeCount = vertex.value(EDGE_COUNT)
                pageRank = IteratorUtils.reduce(messenger.receiveMessages(), 0.0) { a, b -> a + b }
            }
            //////////////////////////
            val teleporationEnergy: Double = memory.get(TELEPORTATION_ENERGY)
            if (teleporationEnergy > 0.0) {
                val localTerminalEnergy = teleporationEnergy / vertexCount
                pageRank = pageRank + localTerminalEnergy
                memory.add(TELEPORTATION_ENERGY, -localTerminalEnergy)
            }
            val previousPageRank: Double = vertex.< Double > property < Double ? > property.orElse(0.0)
            memory.add(CONVERGENCE_ERROR, Math.abs(pageRank - previousPageRank))
            vertex.property(VertexProperty.Cardinality.single, property, pageRank)
            memory.add(TELEPORTATION_ENERGY, (1.0 - alpha) * pageRank)
            pageRank = alpha * pageRank
            if (edgeCount > 0.0) messenger.sendMessage(incidentMessageScope, pageRank / edgeCount) else memory.add(
                TELEPORTATION_ENERGY, pageRank
            )
        }
    }

    @Override
    fun terminate(memory: Memory): Boolean {
        val terminate =
            memory.< Double > get < Double ? > CONVERGENCE_ERROR < epsilon || memory.getIteration() >= maxIterations
        memory.set(CONVERGENCE_ERROR, 0.0)
        return terminate
    }

    @Override
    override fun toString(): String {
        return StringFactory.vertexProgramString(
            this,
            "alpha=" + alpha + ", epsilon=" + epsilon + ", iterations=" + maxIterations
        )
    }

    class Builder : AbstractVertexProgramBuilder<Builder?>(
        PageRankVertexProgram::class.java
    ) {
        fun iterations(iterations: Int): Builder {
            this.configuration.setProperty(MAX_ITERATIONS, iterations)
            return this
        }

        fun alpha(alpha: Double): Builder {
            this.configuration.setProperty(ALPHA, alpha)
            return this
        }

        fun property(key: String?): Builder {
            this.configuration.setProperty(PROPERTY, key)
            return this
        }

        fun epsilon(epsilon: Double): Builder {
            this.configuration.setProperty(EPSILON, epsilon)
            return this
        }

        fun edges(edgeTraversal: Traversal.Admin<Vertex?, Edge?>?): Builder {
            PureTraversal.storeState(this.configuration, EDGE_TRAVERSAL, edgeTraversal)
            return this
        }

        fun initialRank(initialRankTraversal: Traversal.Admin<Vertex?, out Number?>?): Builder {
            PureTraversal.storeState(this.configuration, INITIAL_RANK_TRAVERSAL, initialRankTraversal)
            return this
        }
    }

    ////////////////////////////
    @get:Override
    val features: Features
        get() = object : Features() {
            @Override
            fun requiresLocalMessageScopes(): Boolean {
                return true
            }

            @Override
            fun requiresVertexPropertyAddition(): Boolean {
                return true
            }
        }

    companion object {
        const val PAGE_RANK = "gremlin.pageRankVertexProgram.pageRank"
        private const val EDGE_COUNT = "gremlin.pageRankVertexProgram.edgeCount"
        private const val PROPERTY = "gremlin.pageRankVertexProgram.property"
        private const val VERTEX_COUNT = "gremlin.pageRankVertexProgram.vertexCount"
        private const val ALPHA = "gremlin.pageRankVertexProgram.alpha"
        private const val EPSILON = "gremlin.pageRankVertexProgram.epsilon"
        private const val MAX_ITERATIONS = "gremlin.pageRankVertexProgram.maxIterations"
        private const val EDGE_TRAVERSAL = "gremlin.pageRankVertexProgram.edgeTraversal"
        private const val INITIAL_RANK_TRAVERSAL = "gremlin.pageRankVertexProgram.initialRankTraversal"
        private const val TELEPORTATION_ENERGY = "gremlin.pageRankVertexProgram.teleportationEnergy"
        private const val CONVERGENCE_ERROR = "gremlin.pageRankVertexProgram.convergenceError"

        //////////////////////////////
        fun build(): Builder {
            return Builder()
        }
    }
}