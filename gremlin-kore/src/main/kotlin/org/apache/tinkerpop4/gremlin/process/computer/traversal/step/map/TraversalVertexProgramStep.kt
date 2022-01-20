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
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class TraversalVertexProgramStep(traversal: Traversal.Admin?, computerTraversal: Traversal.Admin<*, *>?) :
    VertexProgramStep(traversal), TraversalParent {
    var computerTraversal: PureTraversal<*, *>

    init {
        this.computerTraversal = PureTraversal(computerTraversal)
        this.integrateChild(this.computerTraversal.get())
    }

    val globalChildren: List<Any>
        get() = Collections.singletonList(computerTraversal.get())

    fun setComputerTraversal(computerTraversal: Traversal.Admin<*, *>?) {
        this.computerTraversal = PureTraversal(computerTraversal)
        this.integrateChild(this.computerTraversal.get())
    }

    @Override
    override fun toString(): String {
        return StringFactory.stepString(this, computerTraversal.get(), GraphFilter(this.computer))
    }

    @get:Override
    val requirements: Set<Any>
        get() = super@TraversalParent.getSelfAndChildRequirements(TraverserRequirement.BULK)

    @Override
    fun generateProgram(graph: Graph, memory: Memory): TraversalVertexProgram {
        val computerSpecificTraversal: Traversal.Admin<*, *> = computerTraversal.getPure()
        val computerSpecificStrategies: TraversalStrategies = this.getTraversal().getStrategies().clone()
        IteratorUtils.filter(
            TraversalStrategies.GlobalCache.getStrategies(graph.getClass())
        ) { s -> s is ProviderOptimizationStrategy }.forEach(computerSpecificStrategies::addStrategies)
        computerSpecificTraversal.setStrategies(computerSpecificStrategies)
        computerSpecificTraversal.setSideEffects(MemoryTraversalSideEffects(this.getTraversal().getSideEffects()))
        computerSpecificTraversal.setParent(this)
        val builder: TraversalVertexProgram.Builder =
            TraversalVertexProgram.build().traversal(computerSpecificTraversal)
        if (memory.exists(TraversalVertexProgram.HALTED_TRAVERSERS)) builder.haltedTraversers(
            memory.get(
                TraversalVertexProgram.HALTED_TRAVERSERS
            )
        )
        return builder.create(graph)
    }

    @Override
    fun clone(): TraversalVertexProgramStep {
        val clone = super.clone() as TraversalVertexProgramStep
        clone.computerTraversal = computerTraversal.clone()
        return clone
    }

    @Override
    fun setTraversal(parentTraversal: Traversal.Admin<*, *>?) {
        super.setTraversal(parentTraversal)
        this.integrateChild(computerTraversal.get())
    } /*@Override
    public int hashCode() {
        return super.hashCode() ^ this.computerTraversal.hashCode();
    }*/
}