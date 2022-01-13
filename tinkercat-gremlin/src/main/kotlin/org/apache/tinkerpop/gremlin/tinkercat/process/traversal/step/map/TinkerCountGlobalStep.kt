/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.tinkerpop.gremlin.tinkercat.process.traversal.step.map

import org.apache.tinkerpop.gremlin.process.traversal.Step
import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.Traverser
import org.apache.tinkerpop.gremlin.process.traversal.step.util.AbstractStep
import kotlin.Throws
import java.util.NoSuchElementException
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerCat
import org.apache.tinkerpop.gremlin.process.traversal.util.FastNoSuchElementException
import org.apache.tinkerpop.gremlin.structure.Element
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.util.StringFactory
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerHelper
import java.util.Locale

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class TinkerCountGlobalStep<S : Element?>(traversal: Traversal.Admin<*, *>?, private val elementClass: Class<S>) :
    AbstractStep<S, Long?>(traversal) {
    private var done = false
    @Throws(NoSuchElementException::class)
    override fun processNextStart(): Traverser.Admin<Long?>? {
        return if (!done) {
            done = true
            val graph = getTraversal<Any, Any>().graph.get() as TinkerCat
            val start = when {
                Vertex::class.java.isAssignableFrom(elementClass) ->
                    TinkerHelper.getVertices(graph).size.toLong()
                else -> TinkerHelper.getEdges(graph).size.toLong()
            }
            getTraversal<Any, Any>().traverserGenerator.generate<Long>( start, this as Step<Long, *>, 1L )
        } else throw FastNoSuchElementException.instance()
    }

    override fun toString(): String {
        return StringFactory.stepString(this, elementClass.simpleName.lowercase(Locale.getDefault()))
    }

    override fun hashCode(): Int {
        return super.hashCode() xor elementClass.hashCode()
    }

    override fun reset() {
        done = false
    }

    override fun remove() {
        TODO("Not yet implemented")
    }
}