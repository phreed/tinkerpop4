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
package org.apache.tinkerpop4.gremlin.process.computer.clone

import org.apache.tinkerpop4.gremlin.process.computer.GraphComputer

/**
 * @author Daniel Kuppitz (http://gremlin.guru)
 */
class CloneVertexProgram private constructor() : VertexProgram<Tuple?> {
    @Override
    fun setup(memory: Memory?) {
    }

    @Override
    fun execute(sourceVertex: Vertex?, messenger: Messenger<Tuple?>?, memory: Memory?) {
    }

    @Override
    fun terminate(memory: Memory?): Boolean {
        return true
    }

    @Override
    fun getMessageScopes(memory: Memory?): Set<MessageScope> {
        return Collections.emptySet()
    }

    @SuppressWarnings(["CloneDoesntDeclareCloneNotSupportedException", "CloneDoesntCallSuperClone"])
    @Override
    fun clone(): VertexProgram<Tuple> {
        return this
    }

    @get:Override
    val preferredResultGraph: ResultGraph
        get() = GraphComputer.ResultGraph.NEW

    @get:Override
    val preferredPersist: Persist
        get() = GraphComputer.Persist.EDGES

    @Override
    override fun toString(): String {
        return StringFactory.vertexProgramString(this)
    }

    class Builder : AbstractVertexProgramBuilder<Builder?>(
        CloneVertexProgram::class.java
    ) {
        @SuppressWarnings("unchecked")
        @Override
        fun create(graph: Graph?): CloneVertexProgram {
            return VertexProgram.createVertexProgram(graph, configuration)
        }
    }

    companion object {
        fun build(): Builder {
            return Builder()
        }
    }
}