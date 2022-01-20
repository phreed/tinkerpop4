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
package org.apache.tinkerpop4.gremlin.process.traversal.util

import org.apache.tinkerpop4.gremlin.process.computer.GraphComputer
import org.apache.tinkerpop4.gremlin.process.computer.traversal.TraversalVertexProgram
import org.apache.tinkerpop4.gremlin.process.traversal.Step
import org.apache.tinkerpop4.gremlin.process.traversal.Traversal
import org.apache.tinkerpop4.gremlin.process.traversal.step.TraversalParent
import java.util.HashMap
import java.util.Map

/**
 * A TraversalMatrix provides random, non-linear access to the steps of a traversal by their step id.
 * This is useful in situations where traversers becomes detached from their traversal (and step) and later need to be re-attached.
 * A classic use case is [TraversalVertexProgram] on [GraphComputer].
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class TraversalMatrix<S, E>(traversal: Traversal.Admin<S, E>) {
    private val matrix: Map<String, Step<*, *>> = HashMap()
    private val traversal: Traversal.Admin<S, E>? = null

    init {
        harvestSteps(traversal.also { this.traversal = it })
    }

    fun <A, B, C : Step<A, B>?> getStepById(stepId: String?): C? {
        return matrix[stepId!!]
    }

    fun getTraversal(): Traversal.Admin<S, E> {
        return traversal
    }

    private fun harvestSteps(traversal: Traversal.Admin<*, *>) {
        for (step in traversal.getSteps()) {
            matrix.put(step.getId(), step)
            if (step is TraversalParent) {
                for (globalChild in (step as TraversalParent).getGlobalChildren()) {
                    harvestSteps(globalChild)
                }
                for (localChild in (step as TraversalParent).getLocalChildren()) {
                    harvestSteps(localChild)
                }
            }
        }
    }
}