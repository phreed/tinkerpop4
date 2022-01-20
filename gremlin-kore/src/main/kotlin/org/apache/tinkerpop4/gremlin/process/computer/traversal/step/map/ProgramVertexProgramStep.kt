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

import org.apache.commons.configuration2.MapConfiguration
import org.apache.tinkerpop4.gremlin.process.computer.GraphFilter
import org.apache.tinkerpop4.gremlin.process.computer.Memory
import org.apache.tinkerpop4.gremlin.process.computer.VertexProgram
import org.apache.tinkerpop4.gremlin.process.computer.traversal.TraversalVertexProgram
import org.apache.tinkerpop4.gremlin.process.traversal.Traversal
import org.apache.tinkerpop4.gremlin.process.traversal.traverser.TraverserRequirement
import org.apache.tinkerpop4.gremlin.process.traversal.util.PureTraversal
import org.apache.tinkerpop4.gremlin.process.traversal.util.TraversalHelper
import org.apache.tinkerpop4.gremlin.structure.Graph
import org.apache.tinkerpop4.gremlin.structure.util.StringFactory
import java.util.HashMap
import java.util.Map
import java.util.Set

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class ProgramVertexProgramStep(traversal: Traversal.Admin?, vertexProgram: VertexProgram) :
    VertexProgramStep(traversal) {
    private val configuration: Map<String, Object>
    private val toStringOfVertexProgram: String
    private val traverserRequirements: Set<TraverserRequirement>

    init {
        configuration = HashMap()
        val base = MapConfiguration(configuration)
        vertexProgram.storeState(base)
        toStringOfVertexProgram = vertexProgram.toString()
        traverserRequirements = vertexProgram.getTraverserRequirements()
    }

    @Override
    fun generateProgram(graph: Graph?, memory: Memory): VertexProgram {
        val base = MapConfiguration(configuration)
        PureTraversal.storeState(base, ROOT_TRAVERSAL, TraversalHelper.getRootTraversal(this.getTraversal()).clone())
        base.setProperty(STEP_ID, this.getId())
        if (memory.exists(TraversalVertexProgram.HALTED_TRAVERSERS)) TraversalVertexProgram.storeHaltedTraversers(
            base,
            memory.get(TraversalVertexProgram.HALTED_TRAVERSERS)
        )
        return VertexProgram.createVertexProgram(graph, base)
    }

    @get:Override
    val requirements: Set<Any>
        get() = traverserRequirements

    @Override
    override fun hashCode(): Int {
        return super.hashCode() xor configuration.hashCode()
    }

    @Override
    override fun toString(): String {
        return StringFactory.stepString(this, toStringOfVertexProgram, GraphFilter(this.computer))
    }
}