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

import org.apache.tinkerpop4.gremlin.process.traversal.Traversal

/**
 * GraphFilter is used by [GraphComputer] implementations to prune the source graph data being loaded into the OLAP system.
 * There are two types of filters: a [Vertex] filter and an [Edge] filter.
 * The vertex filter is a [Traversal] that can only check the id, label, and properties of the vertex.
 * The edge filter is a [Traversal] that starts at the vertex are emits all legal incident edges.
 * If no vertex filter is provided, then no vertices are filtered. If no edge filter is provided, then no edges are filtered.
 * The use of a GraphFilter can greatly reduce the amount of data processed by the [GraphComputer].
 * For instance, for `g.V().count()`, there is no reason to load edges, and thus, the edge filter can be `bothE().limit(0)`.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class GraphFilter : Cloneable, Serializable {
    /**
     * A enum denoting whether a particular result will be allowed or not.
     * [Legal.YES] means that the specified element set will definitely not be removed by [GraphFilter].
     * [Legal.MAYBE] means that the element set may or may not be filtered out by the [GraphFilter].
     * [Legal.NO] means that the specified element set will definitely be removed by [GraphFilter].
     */
    enum class Legal {
        YES, MAYBE, NO;

        /**
         * The enum is either [Legal.YES] or [Legal.MAYBE].
         *
         * @return true if potentially legal.
         */
        fun positive(): Boolean {
            return this != NO
        }

        /**
         * The enum is [Legal.NO].
         *
         * @return true if definitely not legal.
         */
        fun negative(): Boolean {
            return this == NO
        }
    }

    private var vertexFilter: Traversal.Admin<Vertex, Vertex>? = null
    private var edgeFilter: Traversal.Admin<Vertex, Edge>? = null
    private var edgeLegality: Map<Direction, Map<String?, Legal>> = EnumMap(Direction::class.java)
    private var allowNoEdges = false

    constructor() {
        // no args constructor
    }

    constructor(computer: Computer) {
        if (null != computer.getVertices()) setVertexFilter(computer.getVertices())
        if (null != computer.getEdges()) setEdgeFilter(computer.getEdges())
    }

    /**
     * Set the filter for selecting vertices from the source graph.
     * The vertex filter can only access the vertex, its properties, and its properties properties.
     * Vertex filters can not access the incident edges of the vertex.
     *
     * @param vertexFilter The [Traversal] that will either let the vertex pass or not.
     */
    fun setVertexFilter(vertexFilter: Traversal<Vertex?, Vertex?>) {
        if (!TraversalHelper.isLocalProperties(vertexFilter.asAdmin())) throw GraphComputer.Exceptions.vertexFilterAccessesIncidentEdges(
            vertexFilter
        )
        this.vertexFilter = vertexFilter.asAdmin().clone()
    }

    /**
     * Set the filter for selecting edges from the source graph.
     * The edge filter can only access the local star graph (not adjacent vertices).
     *
     * @param edgeFilter The [Traversal] that will generate the legal incident edges of the vertex.
     */
    fun setEdgeFilter(edgeFilter: Traversal<Vertex?, Edge?>) {
        if (!TraversalHelper.isLocalStarGraph(edgeFilter.asAdmin())) throw GraphComputer.Exceptions.edgeFilterAccessesAdjacentVertices(
            edgeFilter
        )
        this.edgeFilter = edgeFilter.asAdmin().clone()
        ////
        edgeLegality = EnumMap(Direction::class.java)
        edgeLegality.put(Direction.OUT, HashMap())
        edgeLegality.put(Direction.IN, HashMap())
        edgeLegality.put(Direction.BOTH, HashMap())
        if (this.edgeFilter.getEndStep() is RangeGlobalStep && 0 == (this.edgeFilter.getEndStep() as RangeGlobalStep).getHighRange()) allowNoEdges =
            true
        ////
        if (this.edgeFilter.getStartStep() is VertexStep) {
            val step: VertexStep = this.edgeFilter.getStartStep() as VertexStep
            val map = edgeLegality[step.getDirection()]!!
            if (step.returnsEdge()) {
                if (step.getEdgeLabels().length === 0) map.put(
                    null,
                    if (1 == this.edgeFilter.getSteps().size()) Legal.YES else Legal.MAYBE
                ) else {
                    for (label in step.getEdgeLabels()) {
                        map.put(label, if (1 == this.edgeFilter.getSteps().size()) Legal.YES else Legal.MAYBE)
                    }
                }
            }
        } else if (this.edgeFilter.getStartStep() is UnionStep) {
            val step: UnionStep<*, *> = this.edgeFilter.getStartStep() as UnionStep
            for (union in step.getGlobalChildren()) {
                if (union.getStartStep() is VertexStep) {
                    val vertexStep: VertexStep = union.getStartStep() as VertexStep
                    val map = edgeLegality[vertexStep.getDirection()]!!
                    if (vertexStep.returnsEdge()) {
                        if (vertexStep.getEdgeLabels().length === 0) map.put(
                            null,
                            if (2 == union.getSteps().size()) Legal.YES else Legal.MAYBE
                        ) else {
                            for (label in vertexStep.getEdgeLabels()) {
                                map.put(label, if (2 == union.getSteps().size()) Legal.YES else Legal.MAYBE)
                            }
                        }
                    }
                }
            }
        }
        val outMap = edgeLegality[Direction.OUT]!!
        val inMap = edgeLegality[Direction.IN]!!
        val bothMap = edgeLegality[Direction.BOTH]!!
        for (entry in bothMap.entrySet()) {
            val legal = inMap[entry.getKey()]
            if (null == legal || legal.compareTo(entry.getValue()) > 0) inMap.put(entry.getKey(), entry.getValue())
        }
        for (entry in bothMap.entrySet()) {
            val legal = outMap[entry.getKey()]
            if (null == legal || legal.compareTo(entry.getValue()) > 0) outMap.put(entry.getKey(), entry.getValue())
        }
        for (entry in outMap.entrySet()) {
            val legal = inMap[entry.getKey()]
            if (null != legal) bothMap.put(
                entry.getKey(),
                if (legal.compareTo(entry.getValue()) > 0) legal else entry.getValue()
            )
        }
        if (outMap.isEmpty() && inMap.isEmpty() && bothMap.isEmpty()) { // the edge filter could not be reasoned on
            outMap.put(null, Legal.MAYBE)
            inMap.put(null, Legal.MAYBE)
            bothMap.put(null, Legal.MAYBE)
        }
    }

    /**
     * Returns true if the provided vertex meets the vertex-filter criteria.
     * If no vertex filter is provided, then the vertex is considered legal.
     *
     * @param vertex the vertex to test for legality
     * @return whether the vertex is [Legal.YES].
     */
    fun legalVertex(vertex: Vertex?): Boolean {
        return null == vertexFilter || TraversalUtil.test(vertex, vertexFilter)
    }

    /**
     * Returns an iterator of legal edges incident to the provided vertex.
     * If no edge filter is provided, then all incident edges are returned.
     *
     * @param vertex the vertex whose legal edges are to be access.
     * @return an iterator of edges that are [Legal.YES].
     */
    fun legalEdges(vertex: Vertex): Iterator<Edge> {
        return if (null == edgeFilter) vertex.edges(Direction.BOTH) else TraversalUtil.applyAll(vertex, edgeFilter)
    }

    /**
     * Get the vertex filter associated with this graph filter.
     *
     * @return the vertex filter or null if no vertex filter was provided.
     */
    fun getVertexFilter(): Traversal.Admin<Vertex, Vertex> {
        return vertexFilter
    }

    /**
     * Get the edge filter associated with this graph filter.
     *
     * @return the edge filter or null if no edge filter was provided.
     */
    fun getEdgeFilter(): Traversal.Admin<Vertex, Edge> {
        return edgeFilter
    }

    /**
     * Whether filters have been defined.
     *
     * @return true if either a vertex or edge filter has been provided.
     */
    fun hasFilter(): Boolean {
        return vertexFilter != null || edgeFilter != null
    }

    /**
     * Whether an edge filter has been defined.
     *
     * @return true if an edge filter was provided.
     */
    fun hasEdgeFilter(): Boolean {
        return edgeFilter != null
    }

    /**
     * Whether a vertex filter has been defined.
     *
     * @return true if a vertex filter was provided.
     */
    fun hasVertexFilter(): Boolean {
        return vertexFilter != null
    }

    /**
     * For a particular edge directionality, get all the [Legal.YES] or [Legal.MAYBE] edge labels.
     * If the label set contains `null`, then all edge labels for that direction are positively legal.
     *
     * @param direction the direction to get the positively legal edge labels for.
     * @return the set of positively legal edge labels for the direction.
     */
    fun getLegallyPositiveEdgeLabels(direction: Direction?): Set<String> {
        return if (null == edgeFilter) Collections.singleton(null) else if (allowNoEdges) Collections.emptySet() else if (edgeLegality[direction]!!
                .containsKey(null)
        ) Collections.singleton(null) else edgeLegality[direction].entrySet()
            .stream()
            .filter { entry -> entry.getValue().positive() }
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet())
    }

    /**
     * Get the legality of a particular edge direction and label.
     *
     * @param direction the direction of the edge.
     * @param label     the label of the edge.
     * @return the [Legal] of the arguments.
     */
    fun checkEdgeLegality(direction: Direction?, label: String?): Legal? {
        if (null == edgeFilter) return Legal.YES
        if (this.checkEdgeLegality(direction).negative()) return Legal.NO
        val legalMap = edgeLegality[direction]!!
        return if (legalMap.containsKey(label)) legalMap[label] else if (legalMap.containsKey(null)) legalMap[null] else Legal.NO
    }

    /**
     * Get the legality of a particular edge direction.
     *
     * @param direction the direction of the edge.
     * @return the [Legal] of the edge direction.
     */
    fun checkEdgeLegality(direction: Direction?): Legal {
        if (null == edgeFilter) return Legal.YES else if (allowNoEdges) return Legal.NO
        return edgeLegality[direction]!!.values()
            .stream()
            .reduce(Legal.NO) { a, b -> if (a.compareTo(b) < 0) a else b }
    }

    @Override
    override fun hashCode(): Int {
        return (if (null == edgeFilter) 111 else edgeFilter.hashCode()) xor if (null == vertexFilter) 222 else vertexFilter.hashCode()
    }

    @Override
    override fun equals(`object`: Object): Boolean {
        return if (`object` !is GraphFilter) false else if ((`object` as GraphFilter).hasVertexFilter() && !(`object` as GraphFilter).vertexFilter.equals(
                vertexFilter
            )
        ) false else if ((`object` as GraphFilter).hasEdgeFilter() && !(`object` as GraphFilter).edgeFilter.equals(
                edgeFilter
            )
        ) false else true
    }

    @Override
    fun clone(): GraphFilter {
        return try {
            val clone = super.clone() as GraphFilter
            if (null != vertexFilter) clone.vertexFilter = vertexFilter.clone()
            if (null != edgeFilter) clone.edgeFilter = edgeFilter.clone()
            clone
        } catch (e: CloneNotSupportedException) {
            throw IllegalStateException(e.getMessage(), e)
        }
    }

    @Override
    override fun toString(): String {
        return if (!hasFilter()) "graphfilter[none]" else if (hasVertexFilter() && hasEdgeFilter()) "graphfilter[" + vertexFilter + "," + edgeFilter + "]" else if (hasVertexFilter()) "graphfilter[" + vertexFilter + "]" else "graphfilter[" + edgeFilter + "]"
    }
}