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
package org.apache.tinkerpop4.gremlin.process.computer.clustering.peerpressure

import org.apache.commons.configuration2.Configuration

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class PeerPressureVertexProgram private constructor() : StaticVertexProgram<Pair<Serializable?, Double?>?>() {
    private var voteScope: MessageScope.Local<*> = MessageScope.Local.of(__::outE)
    private var countScope: MessageScope.Local<*> = MessageScope.Local.of(ReverseTraversalSupplier(voteScope))
    private var edgeTraversal: PureTraversal<Vertex, Edge>? = null
    private var initialVoteStrengthTraversal: PureTraversal<Vertex, out Number?>? = null
    private var maxIterations = 30
    private var distributeVote = false
    private var property = CLUSTER
    @Override
    fun loadState(graph: Graph?, configuration: Configuration) {
        if (configuration.containsKey(INITIAL_VOTE_STRENGTH_TRAVERSAL)) initialVoteStrengthTraversal =
            PureTraversal.loadState(configuration, INITIAL_VOTE_STRENGTH_TRAVERSAL, graph)
        if (configuration.containsKey(EDGE_TRAVERSAL)) {
            edgeTraversal = PureTraversal.loadState(configuration, EDGE_TRAVERSAL, graph)
            voteScope = MessageScope.Local.of { edgeTraversal.get().clone() }
            countScope = MessageScope.Local.of(ReverseTraversalSupplier(voteScope))
        }
        property = configuration.getString(PROPERTY, CLUSTER)
        maxIterations = configuration.getInt(MAX_ITERATIONS, 30)
        distributeVote = configuration.getBoolean(DISTRIBUTE_VOTE, false)
    }

    @Override
    fun storeState(configuration: Configuration) {
        super.storeState(configuration)
        configuration.setProperty(PROPERTY, property)
        configuration.setProperty(MAX_ITERATIONS, maxIterations)
        configuration.setProperty(DISTRIBUTE_VOTE, distributeVote)
        if (null != edgeTraversal) edgeTraversal.storeState(configuration, EDGE_TRAVERSAL)
        if (null != initialVoteStrengthTraversal) initialVoteStrengthTraversal.storeState(
            configuration,
            INITIAL_VOTE_STRENGTH_TRAVERSAL
        )
    }

    @get:Override
    val vertexComputeKeys: Set<Any>
        get() = HashSet(Arrays.asList(VertexComputeKey.of(property, false), VertexComputeKey.of(VOTE_STRENGTH, true)))

    @get:Override
    val memoryComputeKeys: Set<Any>
        get() = MEMORY_COMPUTE_KEYS

    @Override
    fun getMessageScopes(memory: Memory): Set<MessageScope> {
        val VOTE_SCOPE: Set<MessageScope> = HashSet(Collections.singletonList(voteScope))
        val COUNT_SCOPE: Set<MessageScope> = HashSet(Collections.singletonList(countScope))
        return if (distributeVote && memory.isInitialIteration()) COUNT_SCOPE else VOTE_SCOPE
    }

    @get:Override
    val preferredResultGraph: ResultGraph
        get() = GraphComputer.ResultGraph.NEW

    @get:Override
    val preferredPersist: Persist
        get() = GraphComputer.Persist.VERTEX_PROPERTIES

    @Override
    fun setup(memory: Memory) {
        memory.set(VOTE_TO_HALT, false)
    }

    @Override
    fun execute(vertex: Vertex, messenger: Messenger<Pair<Serializable?, Double?>?>, memory: Memory) {
        if (memory.isInitialIteration()) {
            if (distributeVote) {
                messenger.sendMessage(countScope, Pair.with('c', 1.0))
            } else {
                val voteStrength = if (null == initialVoteStrengthTraversal) 1.0 else TraversalUtil.apply(
                    vertex,
                    initialVoteStrengthTraversal.get()
                ).doubleValue()
                vertex.property(VertexProperty.Cardinality.single, property, vertex.id())
                vertex.property(VertexProperty.Cardinality.single, VOTE_STRENGTH, voteStrength)
                messenger.sendMessage(voteScope, Pair(vertex.id() as Serializable, voteStrength))
                memory.add(VOTE_TO_HALT, false)
            }
        } else if (1 == memory.getIteration() && distributeVote) {
            val voteStrength: Double = (if (null == initialVoteStrengthTraversal) 1.0 else TraversalUtil.apply(
                vertex,
                initialVoteStrengthTraversal.get()
            ).doubleValue()) /
                    IteratorUtils.reduce(
                        IteratorUtils.map(messenger.receiveMessages(), Pair::getValue1),
                        0.0
                    ) { a, b -> a + b }
            vertex.property(VertexProperty.Cardinality.single, property, vertex.id())
            vertex.property(VertexProperty.Cardinality.single, VOTE_STRENGTH, voteStrength)
            messenger.sendMessage(voteScope, Pair(vertex.id() as Serializable, voteStrength))
            memory.add(VOTE_TO_HALT, false)
        } else {
            val votes: Map<Serializable, Double> = HashMap()
            votes.put(vertex.value(property), vertex.< Double > value < Double ? > VOTE_STRENGTH)
            messenger.receiveMessages()
                .forEachRemaining { message -> MapHelper.incr(votes, message.getValue0(), message.getValue1()) }
            var cluster: Serializable = largestCount(votes)
            if (null == cluster) cluster = vertex.id() as Serializable
            memory.add(VOTE_TO_HALT, vertex.value(property).equals(cluster))
            vertex.property(VertexProperty.Cardinality.single, property, cluster)
            messenger.sendMessage(voteScope, Pair(cluster, vertex.< Double > value < Double ? > VOTE_STRENGTH))
        }
    }

    @Override
    fun terminate(memory: Memory): Boolean {
        val voteToHalt =
            memory.< Boolean > get < Boolean ? > VOTE_TO_HALT || memory.getIteration() >= if (distributeVote) maxIterations + 1 else maxIterations
        return if (voteToHalt) {
            true
        } else {
            memory.set(VOTE_TO_HALT, true)
            false
        }
    }

    @Override
    override fun toString(): String {
        return StringFactory.vertexProgramString(
            this,
            "distributeVote=" + distributeVote + ", maxIterations=" + maxIterations
        )
    }

    class Builder : AbstractVertexProgramBuilder<Builder?>(
        PeerPressureVertexProgram::class.java
    ) {
        fun property(key: String?): Builder {
            this.configuration.setProperty(PROPERTY, key)
            return this
        }

        fun maxIterations(iterations: Int): Builder {
            this.configuration.setProperty(MAX_ITERATIONS, iterations)
            return this
        }

        fun distributeVote(distributeVote: Boolean): Builder {
            this.configuration.setProperty(DISTRIBUTE_VOTE, distributeVote)
            return this
        }

        fun edges(edgeTraversal: Traversal.Admin<Vertex?, Edge?>?): Builder {
            PureTraversal.storeState(this.configuration, EDGE_TRAVERSAL, edgeTraversal)
            return this
        }

        fun initialVoteStrength(initialVoteStrengthTraversal: Traversal.Admin<Vertex?, out Number?>?): Builder {
            PureTraversal.storeState(this.configuration, INITIAL_VOTE_STRENGTH_TRAVERSAL, initialVoteStrengthTraversal)
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
        const val CLUSTER = "gremlin.peerPressureVertexProgram.cluster"
        private const val VOTE_STRENGTH = "gremlin.peerPressureVertexProgram.voteStrength"
        private const val INITIAL_VOTE_STRENGTH_TRAVERSAL = "gremlin.pageRankVertexProgram.initialVoteStrengthTraversal"
        private const val PROPERTY = "gremlin.peerPressureVertexProgram.property"
        private const val MAX_ITERATIONS = "gremlin.peerPressureVertexProgram.maxIterations"
        private const val DISTRIBUTE_VOTE = "gremlin.peerPressureVertexProgram.distributeVote"
        private const val EDGE_TRAVERSAL = "gremlin.peerPressureVertexProgram.edgeTraversal"
        private const val VOTE_TO_HALT = "gremlin.peerPressureVertexProgram.voteToHalt"
        private val MEMORY_COMPUTE_KEYS: Set<MemoryComputeKey> = Collections.singleton(
            MemoryComputeKey.of(
                VOTE_TO_HALT, Operator.and, false, true
            )
        )

        private fun <T> largestCount(map: Map<T, Double>): T? {
            var largestKey: T? = null
            var largestValue = Double.MIN_VALUE
            for (entry in map.entrySet()) {
                if (entry.getValue() === largestValue) {
                    if (null != largestKey && largestKey.toString().compareTo(entry.getKey().toString()) > 0) {
                        largestKey = entry.getKey()
                        largestValue = entry.getValue()
                    }
                } else if (entry.getValue() > largestValue) {
                    largestKey = entry.getKey()
                    largestValue = entry.getValue()
                }
            }
            return largestKey
        }

        //////////////////////////////
        fun build(): Builder {
            return Builder()
        }
    }
}