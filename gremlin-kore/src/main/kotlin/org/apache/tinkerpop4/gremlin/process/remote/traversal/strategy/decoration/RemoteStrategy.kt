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
package org.apache.tinkerpop4.gremlin.process.remote.traversal.strategy.decoration

import org.apache.tinkerpop4.gremlin.process.computer.traversal.strategy.decoration.VertexProgramStrategy

/**
 * Reconstructs a [Traversal] by appending a [RemoteStep] to its end. That step will submit the
 * [Traversal] to a [RemoteConnection] instance which will typically send it to a remote server for
 * execution and return results.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class RemoteStrategy(remoteConnection: RemoteConnection?) : AbstractTraversalStrategy<DecorationStrategy?>(),
    DecorationStrategy {
    private val remoteConnection: RemoteConnection

    init {
        if (null == remoteConnection) throw IllegalArgumentException("remoteConnection cannot be null")
        this.remoteConnection = remoteConnection
    }

    @Override
    fun applyPost(): Set<Class<out DecorationStrategy?>> {
        return POSTS
    }

    @Override
    fun apply(traversal: Traversal.Admin<*, *>) {
        if (!traversal.isRoot()) return

        // remote step wraps the traversal and emits the results from the remote connection.
        val remoteStep: RemoteStep<*, *> = RemoteStep(traversal, remoteConnection)
        TraversalHelper.removeAllSteps(traversal)
        traversal.addStep(remoteStep)
        assert(traversal.getStartStep().equals(remoteStep))
        assert(traversal.getSteps().size() === 1)
        assert(traversal.getEndStep() === remoteStep)
    }

    companion object {
        /**
         * Should be applied before all [DecorationStrategy] instances.
         */
        private val POSTS: Set<Class<out DecorationStrategy?>> = object : HashSet<Class<out DecorationStrategy?>?>() {
            init {
                add(VertexProgramStrategy::class.java)
                add(ConnectiveStrategy::class.java)
                add(ElementIdStrategy::class.java)
                add(EventStrategy::class.java)
                add(HaltedTraverserStrategy::class.java)
                add(PartitionStrategy::class.java)
                add(RequirementsStrategy::class.java)
                add(SackStrategy::class.java)
                add(SideEffectStrategy::class.java)
                add(SubgraphStrategy::class.java)
            }
        }
    }
}