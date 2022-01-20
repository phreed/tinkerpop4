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
package org.apache.tinkerpop4.gremlin.process.computer.util

import org.apache.tinkerpop4.gremlin.process.computer.GraphComputer

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
object GraphComputerHelper {
    fun validateProgramOnComputer(computer: GraphComputer, vertexProgram: VertexProgram) {
        if (vertexProgram.getMemoryComputeKeys().contains(null)) throw Memory.Exceptions.memoryKeyCanNotBeNull()
        if (vertexProgram.getMemoryComputeKeys().contains("")) throw Memory.Exceptions.memoryKeyCanNotBeEmpty()
        val graphComputerFeatures: GraphComputer.Features = computer.features()
        val vertexProgramFeatures: VertexProgram.Features = vertexProgram.getFeatures()
        for (method in VertexProgram.Features::class.java.getMethods()) {
            if (method.getName().startsWith("requires")) {
                val supports: Boolean
                val requires: Boolean
                try {
                    supports =
                        GraphComputer.Features::class.java.getMethod(method.getName().replace("requires", "supports"))
                            .invoke(graphComputerFeatures)
                    requires = method.invoke(vertexProgramFeatures)
                } catch (e: Exception) {
                    throw IllegalStateException("A reflection exception has occurred: " + e.getMessage(), e)
                }
                if (requires && !supports) throw IllegalStateException("The vertex program can not be executed on the graph computer: " + method.getName())
            }
        }
    }

    fun getResultGraphState(vertexProgram: Optional<VertexProgram?>, resultGraph: Optional<ResultGraph?>): ResultGraph {
        return if (resultGraph.isPresent()) resultGraph.get() else if (vertexProgram.isPresent()) vertexProgram.get()
            .getPreferredResultGraph() else GraphComputer.ResultGraph.ORIGINAL
    }

    fun getPersistState(vertexProgram: Optional<VertexProgram?>, persist: Optional<Persist?>): Persist {
        return if (persist.isPresent()) persist.get() else if (vertexProgram.isPresent()) vertexProgram.get()
            .getPreferredPersist() else GraphComputer.Persist.NOTHING
    }

    fun areEqual(a: MapReduce?, b: Object?): Boolean {
        if (null == a) throw Graph.Exceptions.argumentCanNotBeNull("a")
        if (null == b) throw Graph.Exceptions.argumentCanNotBeNull("b")
        return if (b !is MapReduce) false else a.getClass().equals(b.getClass()) && a.getMemoryKey()
            .equals((b as MapReduce).getMemoryKey())
    }
}