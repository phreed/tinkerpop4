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

import org.apache.tinkerpop4.gremlin.process.traversal.TraversalSource
import org.apache.tinkerpop4.gremlin.process.traversal.TraversalStrategies
import org.apache.tinkerpop4.gremlin.structure.Graph
import java.io.Serializable

/**
 * [TraversalSource] is not [Serializable].
 * `TraversalSourceFactory` can be used to create a serializable representation of a traversal source.
 * This is is primarily an internal utility class for use and should not be used by standard users.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class TraversalSourceFactory<T : TraversalSource?>(traversalSource: T) : Serializable {
    private val traversalStrategies: TraversalStrategies
    private val traversalSourceClass: Class<T>

    init {
        traversalSourceClass = traversalSource.getClass() as Class<T>
        traversalStrategies = traversalSource.getStrategies()
    }

    fun createTraversalSource(graph: Graph?): T {
        return try {
            traversalSourceClass.getConstructor(Graph::class.java, TraversalStrategies::class.java)
                .newInstance(graph, traversalStrategies)
        } catch (e: Exception) {
            throw IllegalStateException(e.getMessage(), e)
        }
    }
}