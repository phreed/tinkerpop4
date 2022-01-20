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
package org.apache.tinkerpop4.gremlin.process.computer

import org.apache.tinkerpop4.gremlin.process.computer.util.DefaultComputerResult

/**
 * The [GraphComputer] is responsible for the execution of a [VertexProgram] and then a set of
 * [MapReduce] jobs over the vertices in the [Graph]. It is up to the [GraphComputer] implementation
 * to determine the appropriate memory structures given the computing substrate.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Matthias Broecheler (me@matthiasb.com)
 */
interface GraphComputer {
    enum class ResultGraph {
        /**
         * When the computation is complete, the [Graph] in [ComputerResult] is the original graph that
         * spawned the graph computer.
         */
        ORIGINAL,

        /**
         * When the computation is complete, the [Graph] in [ComputerResult] is a new graph cloned from
         * the original graph.
         */
        NEW
    }

    enum class Persist {
        /**
         * Write nothing to the declared [ResultGraph].
         */
        NOTHING,

        /**
         * Write vertex and vertex properties to the [ResultGraph].
         */
        VERTEX_PROPERTIES,

        /**
         * Write vertex, vertex properties, and edges to the [ResultGraph].
         */
        EDGES
    }

    /**
     * Set the [ResultGraph] of the computation. If this is not set explicitly by the user, then the
     * [VertexProgram] can choose the most efficient result for its intended use. If there is no declared
     * vertex program, then the [GraphComputer] defaults to [ResultGraph.ORIGINAL].
     *
     * @param resultGraph the type of graph to be returned by [ComputerResult.graph]
     * @return the updated GraphComputer with newly set result graph
     */
    fun result(resultGraph: ResultGraph?): GraphComputer?

    /**
     * Set the [Persist] level of the computation. If this is not set explicitly by the user, then the\
     * [VertexProgram] can choose the most efficient persist for the its intended use.  If there is no declared
     * vertex program, then the [GraphComputer] defaults to [Persist.NOTHING].
     *
     * @param persist the persistence level of the resultant computation
     * @return the updated GraphComputer with newly set persist
     */
    fun persist(persist: Persist?): GraphComputer?

    /**
     * Set the [VertexProgram] to be executed by the [GraphComputer].
     * There can only be one VertexProgram for the GraphComputer.
     *
     * @param vertexProgram the VertexProgram to be executed
     * @return the updated GraphComputer with newly set VertexProgram
     */
    fun program(vertexProgram: VertexProgram?): GraphComputer?

    /**
     * Add a [MapReduce] job to the set of MapReduce jobs to be executed by the [GraphComputer].
     * There can be any number of MapReduce jobs.
     *
     * @param mapReduce the MapReduce job to add to the computation
     * @return the updated GraphComputer with newly added MapReduce job
     */
    fun mapReduce(mapReduce: MapReduce?): GraphComputer?

    /**
     * Set the desired number of workers to execute the [VertexProgram] and [MapReduce] jobs. This is a
     * recommendation to the underlying [GraphComputer] implementation and is allowed to deviate accordingly by
     * the implementation.
     *
     * @param workers the number of workers to execute the submission
     * @return the updated GraphComputer with newly set worker count
     */
    fun workers(workers: Int): GraphComputer?

    /**
     * Add a filter that will limit which vertices are loaded from the graph source. The provided [Traversal]
     * can only check the vertex, its vertex properties, and the vertex property properties. The loaded graph will
     * only have those vertices that pass through the provided filter.
     *
     * @param vertexFilter the traversal to verify whether or not to load the current vertex
     * @return the updated GraphComputer with newly set vertex filter
     * @throws IllegalArgumentException if the provided traversal attempts to access vertex edges
     */
    @Throws(IllegalArgumentException::class)
    fun vertices(vertexFilter: Traversal<Vertex?, Vertex?>?): GraphComputer?

    /**
     * Add a filter that will limit which edges of the vertices are loaded from the graph source.
     * The provided [Traversal] can only check the local star graph of the vertex and thus,
     * can not access properties/labels of the adjacent vertices.
     * The vertices of the loaded graph will only have those edges that pass through the provided filter.
     *
     * @param edgeFilter the traversal that determines which edges are loaded for each vertex
     * @return the updated GraphComputer with newly set edge filter
     * @throws IllegalArgumentException if the provided traversal attempts to access adjacent vertices
     */
    @Throws(IllegalArgumentException::class)
    fun edges(edgeFilter: Traversal<Vertex?, Edge?>?): GraphComputer?

    /**
     * Set an arbitrary configuration key/value for the underlying `Configuration` in the [GraphComputer].
     * Typically, the other fluent methods in [GraphComputer] should be used to configure the computation.
     * However, for some custom configuration in the underlying engine, this method should be used.
     * Different GraphComputer implementations will have different key/values and thus, parameters placed here are
     * generally not universal to all GraphComputer implementations. The default implementation simply does nothing
     * and returns the [GraphComputer] unchanged.
     *
     * @param key   the key of the configuration
     * @param value the value of the configuration
     * @return the updated GraphComputer with newly set key/value configuration
     */
    fun configure(key: String?, value: Object?): GraphComputer? {
        return this
    }

    /**
     * Submit the [VertexProgram] and the set of [MapReduce] jobs for execution by the [GraphComputer].
     *
     * @return a [Future] denoting a reference to the asynchronous computation and where to get the [DefaultComputerResult] when its is complete.
     */
    fun submit(): Future<ComputerResult?>?
    fun features(): Features? {
        return object : Features {}
    }

    interface Features {
        val maxWorkers: Int
            get() = Integer.MAX_VALUE

        fun supportsGlobalMessageScopes(): Boolean {
            return true
        }

        fun supportsLocalMessageScopes(): Boolean {
            return true
        }

        fun supportsVertexAddition(): Boolean {
            return true
        }

        fun supportsVertexRemoval(): Boolean {
            return true
        }

        fun supportsVertexPropertyAddition(): Boolean {
            return true
        }

        fun supportsVertexPropertyRemoval(): Boolean {
            return true
        }

        fun supportsEdgeAddition(): Boolean {
            return true
        }

        fun supportsEdgeRemoval(): Boolean {
            return true
        }

        fun supportsEdgePropertyAddition(): Boolean {
            return true
        }

        fun supportsEdgePropertyRemoval(): Boolean {
            return true
        }

        fun supportsResultGraphPersistCombination(resultGraph: ResultGraph?, persist: Persist?): Boolean {
            return true
        }

        fun supportsGraphFilter(): Boolean {
            return true
        }

        /**
         * Supports [VertexProgram] and [MapReduce] parameters to be direct referenced Java objects
         * (no serialization required). This is typically true for single machine graph computer engines. For cluster
         * oriented graph computers, this is typically false.
         */
        fun supportsDirectObjects(): Boolean {
            return true
        }
    }

    object Exceptions {
        fun adjacentVertexLabelsCanNotBeRead(): UnsupportedOperationException {
            return UnsupportedOperationException("The label of an adjacent vertex can not be read")
        }

        fun adjacentVertexPropertiesCanNotBeReadOrUpdated(): UnsupportedOperationException {
            return UnsupportedOperationException("The properties of an adjacent vertex can not be read or updated")
        }

        fun adjacentVertexEdgesAndVerticesCanNotBeReadOrUpdated(): UnsupportedOperationException {
            return UnsupportedOperationException("The edges and vertices of an adjacent vertex can not be read or updated")
        }

        fun graphFilterNotSupported(): UnsupportedOperationException {
            return UnsupportedOperationException("The computer does not support graph filter")
        }

        fun providedKeyIsNotAnElementComputeKey(key: String): IllegalArgumentException {
            return IllegalArgumentException("The provided key is not an element compute key: $key")
        }

        fun providedKeyIsNotAMemoryComputeKey(key: String): IllegalArgumentException {
            return IllegalArgumentException("The provided key is not a memory compute key: $key")
        }

        fun resultGraphPersistCombinationNotSupported(
            resultGraph: ResultGraph,
            persist: Persist
        ): IllegalArgumentException {
            return IllegalArgumentException("The computer does not support the following result graph and persist combination: $resultGraph:$persist")
        }

        fun computerHasAlreadyBeenSubmittedAVertexProgram(): IllegalStateException {
            return IllegalStateException("This computer has already had a vertex program submitted to it")
        }

        fun computerHasNoVertexProgramNorMapReducers(): IllegalStateException {
            return IllegalStateException("The computer has no vertex program or map reducers to execute")
        }

        fun incidentAndAdjacentElementsCanNotBeAccessedInMapReduce(): UnsupportedOperationException {
            return UnsupportedOperationException("The computer is in MapReduce mode and a vertex's incident and adjacent elements can not be accessed")
        }

        fun vertexPropertiesCanNotBeUpdatedInMapReduce(): UnsupportedOperationException {
            return UnsupportedOperationException("The computer is in MapReduce mode and a vertex's properties can not be updated")
        }

        fun computerRequiresMoreWorkersThanSupported(workers: Int, maxWorkers: Int): IllegalArgumentException {
            return IllegalArgumentException("The computer requires more workers than supported: $workers [max:$maxWorkers]")
        }

        fun vertexFilterAccessesIncidentEdges(vertexFilter: Traversal<Vertex?, Vertex?>): IllegalArgumentException {
            return IllegalArgumentException("The provided vertex filter traversal accesses incident edges: $vertexFilter")
        }

        fun edgeFilterAccessesAdjacentVertices(edgeFilter: Traversal<Vertex?, Edge?>): IllegalArgumentException {
            return IllegalArgumentException("The provided edge filter traversal accesses data on adjacent vertices: $edgeFilter")
        }
    }
}