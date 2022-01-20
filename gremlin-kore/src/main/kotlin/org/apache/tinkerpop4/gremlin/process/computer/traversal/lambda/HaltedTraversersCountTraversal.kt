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
package org.apache.tinkerpop4.gremlin.process.computer.traversal.lambda

import org.apache.tinkerpop4.gremlin.process.computer.traversal.TraversalVertexProgram
import org.apache.tinkerpop4.gremlin.process.traversal.Traverser
import org.apache.tinkerpop4.gremlin.process.traversal.lambda.AbstractLambdaTraversal
import org.apache.tinkerpop4.gremlin.process.traversal.traverser.util.TraverserSet
import org.apache.tinkerpop4.gremlin.structure.Vertex
import org.apache.tinkerpop4.gremlin.structure.VertexProperty

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class HaltedTraversersCountTraversal : AbstractLambdaTraversal<Vertex?, Long?>() {
    private var count: Long? = null

    @Override
    operator fun next(): Long? {
        return count
    }

    @Override
    fun addStart(start: Traverser.Admin<Vertex?>) {
        val property: VertexProperty<TraverserSet<Object>> =
            start.get().< TraverserSet < Object > > property<TraverserSet<Object>>(TraversalVertexProgram.HALTED_TRAVERSERS)
        count = if (property.isPresent()) property.value().bulkSize() else 0L
    }

    @Override
    override fun toString(): String {
        return "count(" + TraversalVertexProgram.HALTED_TRAVERSERS.toString() + ")"
    }

    @Override
    override fun hashCode(): Int {
        return this.getClass().hashCode()
    }
}