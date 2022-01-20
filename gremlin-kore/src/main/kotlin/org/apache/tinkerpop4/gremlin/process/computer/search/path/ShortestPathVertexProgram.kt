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
package org.apache.tinkerpop4.gremlin.process.computer.search.path

import org.apache.commons.configuration2.Configuration

/**
 * @author Daniel Kuppitz (http://gremlin.guru)
 */
class ShortestPathVertexProgram private constructor() : VertexProgram<Triplet<Path?, Edge?, Number?>?> {
    private var haltedTraversers: TraverserSet<Vertex>? = null
    private var haltedTraversersIndex: IndexedTraverserSet<Vertex, Vertex>? = null
    private var traversal: PureTraversal<*, *>? = null
    private var sourceVertexFilterTraversal: PureTraversal<Vertex, *> = DEFAULT_VERTEX_FILTER_TRAVERSAL.clone()
    private var targetVertexFilterTraversal: PureTraversal<Vertex, *> = DEFAULT_VERTEX_FILTER_TRAVERSAL.clone()
    private var edgeTraversal: PureTraversal<Vertex, Edge> = DEFAULT_EDGE_TRAVERSAL.clone()
    private var distanceTraversal: PureTraversal<Edge, Number> = DEFAULT_DISTANCE_TRAVERSAL.clone()
    private var programStep: Step<Vertex, Path>? = null
    private var maxDistance: Number? = null
    private var distanceEqualsNumberOfHops = false
    private var includeEdges = false
    private var standalone = false
    private val memoryComputeKeys: Set<MemoryComputeKey> = HashSet(
        Arrays.asList(
            MemoryComputeKey.of(VOTE_TO_HALT, Operator.and, false, true),
            MemoryComputeKey.of(STATE, Operator.assign, true, true)
        )
    )

    @Override
    fun loadState(graph: Graph?, configuration: Configuration) {
        if (configuration.containsKey(SOURCE_VERTEX_FILTER)) sourceVertexFilterTraversal =
            PureTraversal.loadState(configuration, SOURCE_VERTEX_FILTER, graph)
        if (configuration.containsKey(TARGET_VERTEX_FILTER)) targetVertexFilterTraversal =
            PureTraversal.loadState(configuration, TARGET_VERTEX_FILTER, graph)
        if (configuration.containsKey(EDGE_TRAVERSAL)) edgeTraversal =
            PureTraversal.loadState(configuration, EDGE_TRAVERSAL, graph)
        if (configuration.containsKey(DISTANCE_TRAVERSAL)) distanceTraversal =
            PureTraversal.loadState(configuration, DISTANCE_TRAVERSAL, graph)
        if (configuration.containsKey(MAX_DISTANCE)) maxDistance = configuration.getProperty(MAX_DISTANCE)
        distanceEqualsNumberOfHops = distanceTraversal.equals(DEFAULT_DISTANCE_TRAVERSAL)
        includeEdges = configuration.getBoolean(INCLUDE_EDGES, false)
        standalone = !configuration.containsKey(VertexProgramStep.ROOT_TRAVERSAL)
        if (!standalone) {
            traversal = PureTraversal.loadState(configuration, VertexProgramStep.ROOT_TRAVERSAL, graph)
            val programStepId: String = configuration.getString(ProgramVertexProgramStep.STEP_ID)
            for (step in traversal.get().getSteps()) {
                if (step.getId().equals(programStepId)) {
                    programStep = step
                    break
                }
            }
        }

        // restore halted traversers from the configuration and build an index for direct access
        haltedTraversers = TraversalVertexProgram.loadHaltedTraversers(configuration)
        haltedTraversersIndex = IndexedTraverserSet { v -> v }
        for (traverser in haltedTraversers) {
            haltedTraversersIndex.add(traverser.split())
        }
        memoryComputeKeys.add(MemoryComputeKey.of(SHORTEST_PATHS, Operator.addAll, true, !standalone))
    }

    @Override
    fun storeState(configuration: Configuration) {
        super@VertexProgram.storeState(configuration)
        sourceVertexFilterTraversal.storeState(configuration, SOURCE_VERTEX_FILTER)
        targetVertexFilterTraversal.storeState(configuration, TARGET_VERTEX_FILTER)
        edgeTraversal.storeState(configuration, EDGE_TRAVERSAL)
        distanceTraversal.storeState(configuration, DISTANCE_TRAVERSAL)
        configuration.setProperty(INCLUDE_EDGES, includeEdges)
        if (maxDistance != null) configuration.setProperty(MAX_DISTANCE, maxDistance)
        if (traversal != null) {
            traversal.storeState(configuration, ProgramVertexProgramStep.ROOT_TRAVERSAL)
            configuration.setProperty(ProgramVertexProgramStep.STEP_ID, programStep.getId())
        }
        TraversalVertexProgram.storeHaltedTraversers(configuration, haltedTraversers)
    }

    @get:Override
    val vertexComputeKeys: Set<Any>
        get() = VERTEX_COMPUTE_KEYS

    @Override
    fun getMemoryComputeKeys(): Set<MemoryComputeKey> {
        return memoryComputeKeys
    }

    @Override
    fun getMessageScopes(memory: Memory?): Set<MessageScope> {
        return Collections.emptySet()
    }

    @Override
    fun clone(): VertexProgram<Triplet<Path, Edge, Number>> {
        return try {
            val clone = super.clone() as ShortestPathVertexProgram
            if (null != edgeTraversal) clone.edgeTraversal = edgeTraversal.clone()
            if (null != sourceVertexFilterTraversal) clone.sourceVertexFilterTraversal =
                sourceVertexFilterTraversal.clone()
            if (null != targetVertexFilterTraversal) clone.targetVertexFilterTraversal =
                targetVertexFilterTraversal.clone()
            if (null != distanceTraversal) clone.distanceTraversal = distanceTraversal.clone()
            if (null != traversal) {
                clone.traversal = traversal.clone()
                for (step in clone.traversal.get().getSteps()) {
                    if (step.getId().equals(programStep.getId())) {
                        clone.programStep = step
                        break
                    }
                }
            }
            clone
        } catch (e: CloneNotSupportedException) {
            throw IllegalStateException(e.getMessage(), e)
        }
    }

    @get:Override
    val preferredResultGraph: ResultGraph
        get() = GraphComputer.ResultGraph.ORIGINAL

    @get:Override
    val preferredPersist: Persist
        get() = GraphComputer.Persist.NOTHING

    @Override
    fun setup(memory: Memory) {
        memory.set(VOTE_TO_HALT, true)
        memory.set(STATE, SEARCH)
    }

    @Override
    fun execute(vertex: Vertex, messenger: Messenger<Triplet<Path, Edge, Number>>, memory: Memory) {
        when (memory.< Integer > get < Integer ? > STATE) {
            COLLECT_PATHS -> {
                collectShortestPaths(vertex, memory)
                return
            }
            UPDATE_HALTED_TRAVERSERS -> {
                updateHaltedTraversers(vertex, memory)
                return
            }
        }
        var voteToHalt = true
        if (memory.isInitialIteration()) {

            // Use the first iteration to copy halted traversers from the halted traverser index to the respective
            // vertices. This way the rest of the code can be simplified and always expect the HALTED_TRAVERSERS
            // property to be available (if halted traversers exist for this vertex).
            copyHaltedTraversersFromMemory(vertex)

            // ignore vertices that don't pass the start-vertex filter
            if (!isStartVertex(vertex)) return

            // start to track paths for all valid start-vertices
            val paths: Map<Vertex, Pair<Number, Set<Path>>> = HashMap()
            var path: Path
            val pathSet: Set<Path> = HashSet()
            pathSet.add(makePath(vertex).also { path = it })
            paths.put(vertex, Pair.with(0, pathSet))
            vertex.property(VertexProperty.Cardinality.single, PATHS, paths)

            // send messages to valid adjacent vertices
            processEdges(vertex, path, 0, messenger)
            voteToHalt = false
        } else {

            // load existing paths to this vertex and extend them based on messages received from adjacent vertices
            val paths: Map<Vertex, Pair<Number, Set<Path>>> =
                vertex.< Map < Vertex, Pair<Number, Set<Path>>>>property<Map<Vertex?, Pair<Number?, Set<Path?>?>?>?>(ShortestPathVertexProgram.Companion.PATHS).orElseGet({ HashMap() })
            val iterator: Iterator<Triplet<Path, Edge, Number>> = messenger.receiveMessages()
            while (iterator.hasNext()) {
                val triplet: Triplet<Path, Edge, Number> = iterator.next()
                val sourcePath: Path = triplet.getValue0()
                val distance: Number = triplet.getValue2()
                val sourceVertex: Vertex = sourcePath.get(0)
                var newPath: Path? = null

                // already know a path coming from this source vertex?
                if (paths.containsKey(sourceVertex)) {
                    val currentShortestDistance: Number = paths[sourceVertex].getValue0()
                    val cmp: Int = NumberHelper.compare(distance, currentShortestDistance)
                    if (cmp <= 0) {
                        newPath = extendPath(sourcePath, triplet.getValue1(), vertex)
                        if (cmp < 0) {
                            // if the path length is smaller than the current shortest path's length, replace the
                            // current set of shortest paths
                            val pathSet: Set<Path> = HashSet()
                            pathSet.add(newPath)
                            paths.put(sourceVertex, Pair.with(distance, pathSet))
                        } else {
                            // if the path length is equal to the current shortest path's length, add the new path
                            // to the set of shortest paths
                            paths[sourceVertex].getValue1().add(newPath)
                        }
                    }
                } else if (!exceedsMaxDistance(distance)) {
                    // store the new path as the shortest path from the source vertex to the current vertex
                    val pathSet: Set<Path> = HashSet()
                    pathSet.add(extendPath(sourcePath, triplet.getValue1(), vertex).also { newPath = it })
                    paths.put(sourceVertex, Pair.with(distance, pathSet))
                }

                // if a new path was found, send messages to adjacent vertices, otherwise do nothing as there's no
                // chance to find any new paths going forward
                if (newPath != null) {
                    vertex.property(VertexProperty.Cardinality.single, PATHS, paths)
                    processEdges(vertex, newPath, distance, messenger)
                    voteToHalt = false
                }
            }
        }

        // VOTE_TO_HALT will be set to true if an iteration hasn't found any new paths
        memory.add(VOTE_TO_HALT, voteToHalt)
    }

    @Override
    fun terminate(memory: Memory): Boolean {
        if (memory.isInitialIteration() && haltedTraversersIndex != null) {
            haltedTraversersIndex.clear()
        }
        val voteToHalt: Boolean = memory.get(VOTE_TO_HALT)
        return if (voteToHalt) {
            val state: Int = memory.get(STATE)
            if (state == COLLECT_PATHS) {
                // After paths were collected,
                // a) the VP is done in standalone mode (paths will be in memory) or
                // b) the halted traversers will be updated in order to have the paths available in the traversal
                if (standalone) return true
                memory.set(
                    STATE,
                    UPDATE_HALTED_TRAVERSERS
                )
                return false
            }
            if (state == UPDATE_HALTED_TRAVERSERS) return true else memory.set(
                STATE,
                COLLECT_PATHS
            ) // collect paths if no new paths were found
            false
        } else {
            memory.set(VOTE_TO_HALT, true)
            false
        }
    }

    @Override
    override fun toString(): String {
        val options: List<String> = ArrayList()
        val shortName: Function<String, String> =
            Function<String, String> { name -> name.substring(name.lastIndexOf(".") + 1) }
        if (!sourceVertexFilterTraversal.equals(DEFAULT_VERTEX_FILTER_TRAVERSAL)) {
            options.add(shortName.apply(SOURCE_VERTEX_FILTER) + "=" + sourceVertexFilterTraversal.get())
        }
        if (!targetVertexFilterTraversal.equals(DEFAULT_VERTEX_FILTER_TRAVERSAL)) {
            options.add(shortName.apply(TARGET_VERTEX_FILTER) + "=" + targetVertexFilterTraversal.get())
        }
        if (!edgeTraversal.equals(DEFAULT_EDGE_TRAVERSAL)) {
            options.add(shortName.apply(EDGE_TRAVERSAL) + "=" + edgeTraversal.get())
        }
        if (!distanceTraversal.equals(DEFAULT_DISTANCE_TRAVERSAL)) {
            options.add(shortName.apply(DISTANCE_TRAVERSAL) + "=" + distanceTraversal.get())
        }
        options.add(shortName.apply(INCLUDE_EDGES) + "=" + includeEdges)
        return StringFactory.vertexProgramString(this, String.join(", ", options))
    }

    //////////////////////////////
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

    private fun isStartVertex(vertex: Vertex): Boolean {
        // use the sourceVertexFilterTraversal if the VP is running in standalone mode (not part of a traversal)
        if (standalone) {
            val filterTraversal: Traversal.Admin<Vertex, *> = sourceVertexFilterTraversal.getPure()
            filterTraversal.addStart(
                filterTraversal.getTraverserGenerator().generate(vertex, filterTraversal.getStartStep(), 1)
            )
            return filterTraversal.hasNext()
        }
        // ...otherwise use halted traversers to determine whether this is a start vertex
        return vertex.property(TraversalVertexProgram.HALTED_TRAVERSERS).isPresent()
    }

    private fun isEndVertex(vertex: Vertex): Boolean {
        val filterTraversal: Traversal.Admin<Vertex, *> = targetVertexFilterTraversal.getPure()
        val startStep: Step<Vertex, Vertex> = filterTraversal.getStartStep() as Step<Vertex, Vertex>
        filterTraversal.addStart(filterTraversal.getTraverserGenerator().generate(vertex, startStep, 1))
        return filterTraversal.hasNext()
    }

    private fun processEdges(
        vertex: Vertex, currentPath: Path, currentDistance: Number,
        messenger: Messenger<Triplet<Path, Edge, Number>>
    ) {
        val edgeTraversal: Traversal.Admin<Vertex, Edge> = edgeTraversal.getPure()
        edgeTraversal.addStart(edgeTraversal.getTraverserGenerator().generate(vertex, edgeTraversal.getStartStep(), 1))
        while (edgeTraversal.hasNext()) {
            val edge: Edge = edgeTraversal.next()
            val distance = getDistance(edge)
            var otherV: Vertex = edge.inVertex()
            if (otherV.equals(vertex)) otherV = edge.outVertex()

            // only send message if the adjacent vertex is not yet part of the current path
            if (!currentPath.objects().contains(otherV)) {
                messenger.sendMessage(
                    MessageScope.Global.of(otherV),
                    Triplet.with(
                        currentPath, if (includeEdges) edge else null,
                        NumberHelper.add(currentDistance, distance)
                    )
                )
            }
        }
    }

    private fun updateHaltedTraversers(vertex: Vertex, memory: Memory) {
        if (isStartVertex(vertex)) {
            val paths: List<Path> = memory.get(SHORTEST_PATHS)
            if (vertex.property(TraversalVertexProgram.HALTED_TRAVERSERS).isPresent()) {
                // replace the current set of halted traversers with new new traversers that hold the shortest paths
                // found for this vertex
                val haltedTraversers: TraverserSet<Vertex> = vertex.value(TraversalVertexProgram.HALTED_TRAVERSERS)
                val newHaltedTraversers: TraverserSet<Path> = TraverserSet()
                for (traverser in haltedTraversers) {
                    val v: Vertex = traverser.get()
                    for (path in paths) {
                        if (path.get(0).equals(v)) {
                            newHaltedTraversers.add(traverser.split(path, programStep))
                        }
                    }
                }
                vertex.property(
                    VertexProperty.Cardinality.single,
                    TraversalVertexProgram.HALTED_TRAVERSERS,
                    newHaltedTraversers
                )
            }
        }
    }

    private fun getDistance(edge: Edge): Number {
        if (distanceEqualsNumberOfHops) return 1
        val traversal: Traversal.Admin<Edge, Number> = distanceTraversal.getPure()
        traversal.addStart(traversal.getTraverserGenerator().generate(edge, traversal.getStartStep(), 1))
        return traversal.tryNext().orElse(0)
    }

    private fun exceedsMaxDistance(distance: Number): Boolean {
        // This method is used to stop the message sending for paths that exceed the specified maximum distance. Since
        // custom distances can be negative, this method should only return true if the distance is calculated based on
        // the number of hops.
        return distanceEqualsNumberOfHops && maxDistance != null && NumberHelper.compare(distance, maxDistance) > 0
    }

    /**
     * Move any valid path into the VP's memory.
     * @param vertex The current vertex.
     * @param memory The VertexProgram's memory.
     */
    private fun collectShortestPaths(vertex: Vertex, memory: Memory) {
        val pathProperty: VertexProperty<Map<Vertex, Pair<Number, Set<Path>>>> = vertex.property(PATHS)
        if (pathProperty.isPresent()) {
            val paths: Map<Vertex, Pair<Number, Set<Path>>> = pathProperty.value()
            val result: List<Path> = ArrayList()
            for (pair in paths.values()) {
                for (path in pair.getValue1()) {
                    if (isEndVertex(vertex)) {
                        if (distanceEqualsNumberOfHops || maxDistance == null || NumberHelper.compare(
                                pair.getValue0(),
                                maxDistance
                            ) <= 0
                        ) {
                            result.add(path)
                        }
                    }
                }
            }
            pathProperty.remove()
            memory.add(SHORTEST_PATHS, result)
        }
    }

    @SuppressWarnings("WeakerAccess")
    class Builder : AbstractVertexProgramBuilder<Builder?>(
        ShortestPathVertexProgram::class.java
    ) {
        fun source(sourceVertexFilter: Traversal<Vertex?, *>?): Builder {
            if (null == sourceVertexFilter) throw Graph.Exceptions.argumentCanNotBeNull("sourceVertexFilter")
            PureTraversal.storeState(this.configuration, SOURCE_VERTEX_FILTER, sourceVertexFilter.asAdmin())
            return this
        }

        fun target(targetVertexFilter: Traversal<Vertex?, *>?): Builder {
            if (null == targetVertexFilter) throw Graph.Exceptions.argumentCanNotBeNull("targetVertexFilter")
            PureTraversal.storeState(this.configuration, TARGET_VERTEX_FILTER, targetVertexFilter.asAdmin())
            return this
        }

        fun edgeDirection(direction: Direction?): Builder {
            if (null == direction) throw Graph.Exceptions.argumentCanNotBeNull("direction")
            return edgeTraversal(__.toE(direction))
        }

        fun edgeTraversal(edgeTraversal: Traversal<Vertex?, Edge?>?): Builder {
            if (null == edgeTraversal) throw Graph.Exceptions.argumentCanNotBeNull("edgeTraversal")
            PureTraversal.storeState(this.configuration, EDGE_TRAVERSAL, edgeTraversal.asAdmin())
            return this
        }

        fun distanceProperty(distance: String?): Builder {
            return if (distance != null) distanceTraversal(__.values(distance)) // todo: (Traversal) new ElementValueTraversal<>(distance)
            else distanceTraversal(DEFAULT_DISTANCE_TRAVERSAL.getPure())
        }

        fun distanceTraversal(distanceTraversal: Traversal<Edge?, Number?>?): Builder {
            if (null == distanceTraversal) throw Graph.Exceptions.argumentCanNotBeNull("distanceTraversal")
            PureTraversal.storeState(this.configuration, DISTANCE_TRAVERSAL, distanceTraversal.asAdmin())
            return this
        }

        fun maxDistance(distance: Number?): Builder {
            if (null != distance) this.configuration.setProperty(
                MAX_DISTANCE,
                distance
            ) else this.configuration.clearProperty(
                MAX_DISTANCE
            )
            return this
        }

        fun includeEdges(include: Boolean): Builder {
            this.configuration.setProperty(INCLUDE_EDGES, include)
            return this
        }
    }

    ////////////////////////////
    @get:Override
    val features: Features
        get() = object : Features() {
            @Override
            fun requiresGlobalMessageScopes(): Boolean {
                return true
            }

            @Override
            fun requiresVertexPropertyAddition(): Boolean {
                return true
            }
        }

    companion object {
        @SuppressWarnings("WeakerAccess")
        val SHORTEST_PATHS = "gremlin.shortestPathVertexProgram.shortestPaths"
        private const val SOURCE_VERTEX_FILTER = "gremlin.shortestPathVertexProgram.sourceVertexFilter"
        private const val TARGET_VERTEX_FILTER = "gremlin.shortestPathVertexProgram.targetVertexFilter"
        private const val EDGE_TRAVERSAL = "gremlin.shortestPathVertexProgram.edgeTraversal"
        private const val DISTANCE_TRAVERSAL = "gremlin.shortestPathVertexProgram.distanceTraversal"
        private const val MAX_DISTANCE = "gremlin.shortestPathVertexProgram.maxDistance"
        private const val INCLUDE_EDGES = "gremlin.shortestPathVertexProgram.includeEdges"
        private const val STATE = "gremlin.shortestPathVertexProgram.state"
        private const val PATHS = "gremlin.shortestPathVertexProgram.paths"
        private const val VOTE_TO_HALT = "gremlin.shortestPathVertexProgram.voteToHalt"
        private const val SEARCH = 0
        private const val COLLECT_PATHS = 1
        private const val UPDATE_HALTED_TRAVERSERS = 2
        val DEFAULT_VERTEX_FILTER_TRAVERSAL: PureTraversal<Vertex, *> = PureTraversal(
            __.< Vertex > identity < Vertex ? > ().asAdmin()
        ) // todo: new IdentityTraversal<>()
        val DEFAULT_EDGE_TRAVERSAL: PureTraversal<Vertex, Edge> = PureTraversal(__.bothE().asAdmin())
        val DEFAULT_DISTANCE_TRAVERSAL: PureTraversal<Edge, Number> = PureTraversal(
            __.< Edge > start < Edge ? > ().< Number > constant < Number ? > 1.asAdmin()
        ) // todo: new ConstantTraversal<>(1)
        private val VERTEX_COMPUTE_KEYS: Set<VertexComputeKey> = HashSet(
            Arrays.asList(
                VertexComputeKey.of(PATHS, true),
                VertexComputeKey.of(TraversalVertexProgram.HALTED_TRAVERSERS, false)
            )
        )

        private fun makePath(newVertex: Vertex): Path {
            return extendPath(null, newVertex)
        }

        private fun extendPath(currentPath: Path?, vararg elements: Element): Path {
            var result: Path = ImmutablePath.make()
            if (currentPath != null) {
                for (o in currentPath.objects()) {
                    result = result.extend(o, Collections.emptySet())
                }
            }
            for (element in elements) {
                if (element != null) {
                    result = result.extend(ReferenceFactory.detach(element), Collections.emptySet())
                }
            }
            return result
        }

        //////////////////////////////
        fun build(): Builder {
            return Builder()
        }
    }
}