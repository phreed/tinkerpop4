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

import org.apache.tinkerpop4.gremlin.process.computer.ComputerResult

/**
 * @author Daniel Kuppitz (http://gremlin.guru)
 */
class ShortestPathVertexProgramStep(traversal: Traversal.Admin<*, *>?) : VertexProgramStep(traversal), TraversalParent,
    Configuring {
    private val parameters: Parameters = Parameters()
    private var targetVertexFilter: PureTraversal<Vertex, *> =
        ShortestPathVertexProgram.DEFAULT_VERTEX_FILTER_TRAVERSAL.clone()
    private var edgeTraversal: PureTraversal<Vertex, Edge> = ShortestPathVertexProgram.DEFAULT_EDGE_TRAVERSAL.clone()
    private var distanceTraversal: PureTraversal<Edge, Number> =
        ShortestPathVertexProgram.DEFAULT_DISTANCE_TRAVERSAL.clone()
    private var maxDistance: Number? = null
    private var includeEdges = false
    fun setTargetVertexFilter(filterTraversal: Traversal) {
        targetVertexFilter = PureTraversal(this.integrateChild(filterTraversal.asAdmin()))
    }

    fun setEdgeTraversal(edgeTraversal: Traversal) {
        this.edgeTraversal = PureTraversal(this.integrateChild(edgeTraversal.asAdmin()))
    }

    fun setDistanceTraversal(distanceTraversal: Traversal) {
        this.distanceTraversal = PureTraversal(this.integrateChild(distanceTraversal.asAdmin()))
    }

    fun setMaxDistance(maxDistance: Number?) {
        this.maxDistance = maxDistance
    }

    fun setIncludeEdges(includeEdges: Boolean) {
        this.includeEdges = includeEdges
    }

    @Override
    fun configure(vararg keyValues: Object?) {
        if (!ShortestPath.configure(this, keyValues[0] as String?, keyValues[1])) {
            parameters.set(this, keyValues)
        }
    }

    @Override
    fun getParameters(): Parameters {
        return parameters
    }

    @Override
    @Throws(NoSuchElementException::class)
    protected fun processNextStart(): Traverser.Admin<ComputerResult> {
        return super.processNextStart()
    }

    @get:Override
    @get:SuppressWarnings("unchecked")
    val localChildren: List<Any>
        get() = Arrays.asList(
            targetVertexFilter.get(),
            edgeTraversal.get(),
            distanceTraversal.get()
        )

    @Override
    override fun toString(): String {
        return StringFactory.stepString(
            this, targetVertexFilter.get(), edgeTraversal.get(),
            distanceTraversal.get(), maxDistance, includeEdges, GraphFilter(this.computer)
        )
    }

    @Override
    fun generateProgram(graph: Graph?, memory: Memory): ShortestPathVertexProgram {
        val builder: ShortestPathVertexProgram.Builder = ShortestPathVertexProgram.build()
            .target(targetVertexFilter.getPure())
            .edgeTraversal(edgeTraversal.getPure())
            .distanceTraversal(distanceTraversal.getPure())
            .maxDistance(maxDistance)
            .includeEdges(includeEdges)
        val pureRootTraversal = PureTraversal(this.traversal)
        val rootTraversalValue: Object
        rootTraversalValue = try {
            Base64.getEncoder().encodeToString(Serializer.serializeObject(pureRootTraversal))
        } catch (ignored: IOException) {
            pureRootTraversal
        }
        builder.configure(
            ProgramVertexProgramStep.ROOT_TRAVERSAL, rootTraversalValue,
            ProgramVertexProgramStep.STEP_ID, this.id
        )

        // There are two locations in which halted traversers can be stored: in memory or as vertex properties. In the
        // former case they need to be copied to this VertexProgram's configuration as the VP won't have access to the
        // previous VP's memory.
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

    @get:Override
    val requirements: Set<Any>
        get() = super@TraversalParent.getSelfAndChildRequirements()

    @Override
    fun clone(): ShortestPathVertexProgramStep {
        val clone = super.clone() as ShortestPathVertexProgramStep
        clone.targetVertexFilter = targetVertexFilter.clone()
        clone.edgeTraversal = edgeTraversal.clone()
        clone.distanceTraversal = distanceTraversal.clone()
        return clone
    }

    @Override
    fun setTraversal(parentTraversal: Traversal.Admin<*, *>?) {
        super.setTraversal(parentTraversal)
        this.integrateChild(targetVertexFilter.get())
        this.integrateChild(edgeTraversal.get())
        this.integrateChild(distanceTraversal.get())
    }

    @Override
    override fun hashCode(): Int {
        return super.hashCode()
    }
}