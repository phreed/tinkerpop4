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
package org.apache.tinkerpop4.gremlin.process.traversal.step.map

import org.apache.tinkerpop4.gremlin.process.traversal.Traversal
import org.apache.tinkerpop4.gremlin.process.traversal.Traverser
import org.apache.tinkerpop4.gremlin.process.traversal.step.Configuring
import org.apache.tinkerpop4.gremlin.process.traversal.step.util.Parameters
import org.apache.tinkerpop4.gremlin.process.traversal.traverser.TraverserRequirement
import org.apache.tinkerpop4.gremlin.structure.Edge
import org.apache.tinkerpop4.gremlin.structure.Vertex
import org.apache.tinkerpop4.gremlin.structure.util.ElementHelper
import java.util.Collections
import java.util.List
import java.util.Set

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class EdgeOtherVertexStep(traversal: Traversal.Admin?) : ScalarMapStep<Edge?, Vertex?>(traversal), Configuring {
    protected var parameters: Parameters = Parameters()
    @Override
    protected fun map(traverser: Traverser.Admin<Edge?>): Vertex {
        val objects: List<Object> = traverser.path().objects()
        for (i in objects.size() - 2 downTo 0) {
            if (objects[i] is Vertex) {
                return if (ElementHelper.areEqual(objects[i] as Vertex, traverser.get().outVertex())) traverser.get()
                    .inVertex() else traverser.get().outVertex()
            }
        }
        throw IllegalStateException("The path history of the traverser does not contain a previous vertex: " + traverser.path())
    }

    @Override
    fun getParameters(): Parameters {
        return parameters
    }

    @Override
    fun configure(vararg keyValues: Object?) {
        parameters.set(null, keyValues)
    }

    @get:Override
    val requirements: Set<Any>
        get() = Collections.singleton(TraverserRequirement.PATH)
}