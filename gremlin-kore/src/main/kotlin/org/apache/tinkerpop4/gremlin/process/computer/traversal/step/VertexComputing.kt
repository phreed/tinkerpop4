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
package org.apache.tinkerpop4.gremlin.process.computer.traversal.step

import org.apache.tinkerpop4.gremlin.process.computer.Computer
import org.apache.tinkerpop4.gremlin.process.computer.GraphComputer
import org.apache.tinkerpop4.gremlin.process.computer.Memory
import org.apache.tinkerpop4.gremlin.process.computer.VertexProgram
import org.apache.tinkerpop4.gremlin.process.traversal.Step
import org.apache.tinkerpop4.gremlin.process.traversal.Traversal
import org.apache.tinkerpop4.gremlin.structure.Graph

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
interface VertexComputing {
    /**
     * Set the [Computer] to be used to generate the [GraphComputer].
     *
     * @param computer the computer specification.
     */
    fun setComputer(computer: Computer?)

    /**
     * Get the [Computer] for generating the [GraphComputer].
     * Inferences on the state of the [Step] within the [Traversal] can be use applied here.
     *
     * @return the computer specification for generating the graph computer.
     */
    fun getComputer(): Computer?

    /**
     * Generate the [VertexProgram].
     *
     * @param graph  the [Graph] that the program will be executed over.
     * @param memory the [Memory] from the previous OLAP job if it exists, else its an empty memory structure.
     * @return the generated vertex program instance.
     */
    fun generateProgram(graph: Graph?, memory: Memory?): VertexProgram?
}