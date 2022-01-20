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

import org.apache.tinkerpop4.gremlin.process.computer.ComputerResult
import org.apache.tinkerpop4.gremlin.process.computer.GraphComputer
import org.apache.tinkerpop4.gremlin.process.computer.Memory
import org.apache.tinkerpop4.gremlin.structure.Graph
import org.apache.tinkerpop4.gremlin.structure.util.StringFactory

/**
 * The result of the [GraphComputer]'s computation. This is returned in a `Future` by
 * [GraphComputer.submit]. A [GraphComputer] computation yields two things: an updated view of the
 * computed on [Graph] and any computational sideEffects called [Memory].
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class DefaultComputerResult(graph: Graph, memory: Memory) : ComputerResult {
    protected val graph: Graph
    protected val memory: Memory

    init {
        this.graph = graph
        this.memory = memory
    }

    @Override
    fun graph(): Graph {
        return graph
    }

    @Override
    fun memory(): Memory {
        return memory
    }

    @Override
    fun close() {
    }

    @Override
    override fun toString(): String {
        return StringFactory.computeResultString(this)
    }
}