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
package org.apache.tinkerpop4.gremlin.process.computer.clustering.connected

import org.apache.commons.configuration2.BaseConfiguration

/**
 * Identifies "Connected Component" instances in a graph by assigning a component identifier (the lexicographically
 * least string value of the vertex in the component) to each vertex.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 * @author Daniel Kuppitz (http://gremlin.guru)
 */
class ConnectedComponentVertexProgram private constructor() : VertexProgram<String?> {
    private var scope: MessageScope.Local<*> = MessageScope.Local.of(__::bothE)
    private var scopes: Set<MessageScope>? = null
    private var property = COMPONENT
    private var edgeTraversal: PureTraversal<Vertex, Edge>? = null
    private var configuration: Configuration? = null
    private var haltedTraversers: TraverserSet<Vertex>? = null
    private var haltedTraversersIndex: IndexedTraverserSet<Vertex, Vertex>? = null
    @Override
    fun loadState(graph: Graph?, config: Configuration?) {
        configuration = BaseConfiguration()
        if (config != null) {
            ConfigurationUtils.copy(config, configuration)
        }
        if (configuration.containsKey(EDGE_TRAVERSAL)) {
            edgeTraversal = PureTraversal.loadState(configuration, EDGE_TRAVERSAL, graph)
            scope = MessageScope.Local.of { edgeTraversal.get().clone() }
        }
        scopes = HashSet(Collections.singletonList(scope))
        property = configuration.getString(PROPERTY, COMPONENT)
        haltedTraversers = TraversalVertexProgram.loadHaltedTraversers(configuration)
        haltedTraversersIndex = IndexedTraverserSet { v -> v }
        for (traverser in haltedTraversers) {
            haltedTraversersIndex.add(traverser.split())
        }
    }

    @Override
    fun storeState(config: Configuration?) {
        super@VertexProgram.storeState(config)
        if (configuration != null) {
            ConfigurationUtils.copy(configuration, config)
        }
    }

    @Override
    fun setup(memory: Memory) {
        memory.set(VOTE_TO_HALT, true)
    }

    @Override
    fun execute(vertex: Vertex, messenger: Messenger<String?>, memory: Memory) {
        if (memory.isInitialIteration()) {
            copyHaltedTraversersFromMemory(vertex)

            // on the first pass, just initialize the component to its own id then pass it to all adjacent vertices
            // for evaluation
            vertex.property(VertexProperty.Cardinality.single, property, vertex.id().toString())

            // vertices that have no edges remain in their own component - nothing to message pass here
            if (vertex.edges(Direction.BOTH).hasNext()) {
                // since there was message passing we don't want to halt on the first round. this should only trigger
                // a single pass finish if the graph is completely disconnected (technically, it won't even really
                // work in cases where halted traversers come into play
                messenger.sendMessage(scope, vertex.id().toString())
                memory.add(VOTE_TO_HALT, false)
            }
        } else {
            // by the second iteration all vertices that matter should have a component assigned
            var currentComponent: String? = vertex.value(property)
            var different = false

            // iterate through messages received and determine if there is a component that has a lesser value than
            // the currently assigned one
            val componentIterator: Iterator<String> = messenger.receiveMessages()
            while (componentIterator.hasNext()) {
                val candidateComponent = componentIterator.next()
                if (candidateComponent.compareTo(currentComponent!!) < 0) {
                    currentComponent = candidateComponent
                    different = true
                }
            }

            // if there is a better component then assign it and notify adjacent vertices. triggering the message
            // passing should not halt future executions
            if (different) {
                vertex.property(VertexProperty.Cardinality.single, property, currentComponent)
                messenger.sendMessage(scope, currentComponent)
                memory.add(VOTE_TO_HALT, false)
            }
        }
    }

    @get:Override
    val vertexComputeKeys: Set<Any>
        get() = HashSet(
            Arrays.asList(
                VertexComputeKey.of(property, false),
                VertexComputeKey.of(TraversalVertexProgram.HALTED_TRAVERSERS, false)
            )
        )

    @get:Override
    val memoryComputeKeys: Set<Any>
        get() = MEMORY_COMPUTE_KEYS

    @Override
    fun terminate(memory: Memory): Boolean {
        if (memory.isInitialIteration() && haltedTraversersIndex != null) {
            haltedTraversersIndex.clear()
        }
        val voteToHalt: Boolean = memory.< Boolean > get < Boolean ? > VOTE_TO_HALT
        return if (voteToHalt) {
            true
        } else {
            // it is basically always assumed that the program will want to halt, but if message passing occurs, the
            // program will want to continue, thus reset false values to true for future iterations
            memory.set(VOTE_TO_HALT, true)
            false
        }
    }

    @Override
    fun getMessageScopes(memory: Memory?): Set<MessageScope>? {
        return scopes
    }

    @get:Override
    val preferredResultGraph: ResultGraph
        get() = GraphComputer.ResultGraph.NEW

    @get:Override
    val preferredPersist: Persist
        get() = GraphComputer.Persist.VERTEX_PROPERTIES

    @Override
    @SuppressWarnings("CloneDoesntCallSuperClone,CloneDoesntDeclareCloneNotSupportedException")
    fun clone(): ConnectedComponentVertexProgram {
        return this
    }

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

    private fun copyHaltedTraversersFromMemory(vertex: Vertex) {
        val traversers: Collection<Traverser.Admin<Vertex>> = haltedTraversersIndex.get(vertex)
        if (traversers != null) {
            val newHaltedTraversers: TraverserSet<Vertex> = TraverserSet()
            newHaltedTraversers.addAll(traversers)
            vertex.property(
                VertexProperty.Cardinality.single,
                TraversalVertexProgram.HALTED_TRAVERSERS,
                newHaltedTraversers
            )
        }
    }

    class Builder : AbstractVertexProgramBuilder<Builder?>(
        ConnectedComponentVertexProgram::class.java
    ) {
        fun edges(edgeTraversal: Traversal.Admin<Vertex?, Edge?>?): Builder {
            PureTraversal.storeState(this.configuration, EDGE_TRAVERSAL, edgeTraversal)
            return this
        }

        fun property(key: String?): Builder {
            this.configuration.setProperty(PROPERTY, key)
            return this
        }
    }

    companion object {
        const val COMPONENT = "gremlin.connectedComponentVertexProgram.component"
        private const val PROPERTY = "gremlin.connectedComponentVertexProgram.property"
        private const val EDGE_TRAVERSAL = "gremlin.pageRankVertexProgram.edgeTraversal"
        private const val VOTE_TO_HALT = "gremlin.connectedComponentVertexProgram.voteToHalt"
        private val MEMORY_COMPUTE_KEYS: Set<MemoryComputeKey> = Collections.singleton(
            MemoryComputeKey.of(
                VOTE_TO_HALT, Operator.and, false, true
            )
        )

        fun build(): Builder {
            return Builder()
        }
    }
}