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
package org.apache.tinkerpop4.gremlin.process.traversal

import org.apache.commons.configuration2.Configuration

/**
 * Provides a unified way to construct a [TraversalSource] from the perspective of the traversal. In this syntax
 * the user is creating the source and binding it to a reference which is either an existing [Graph] instance
 * or a [RemoteConnection].
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class AnonymousTraversalSource<T : TraversalSource?> private constructor(traversalSourceClass: Class<T>) {
    private val traversalSourceClass: Class<T>

    init {
        this.traversalSourceClass = traversalSourceClass
    }

    /**
     * Creates a [TraversalSource] binding a [RemoteConnection] to a remote [Graph] instances as its
     * reference so that traversals spawned from it will execute over that reference.
     *
     * @param configFile a path to a file that would normally be provided to configure a [RemoteConnection]
     */
    @Throws(Exception::class)
    fun withRemote(configFile: String?): T {
        val configs = Configurations()
        return withRemote(configs.properties(configFile))
    }

    /**
     * Creates a [TraversalSource] binding a [RemoteConnection] to a remote [Graph] instances as its
     * reference so that traversals spawned from it will execute over that reference.
     *
     * @param conf a `Configuration` object that would normally be provided to configure a [RemoteConnection]
     */
    fun withRemote(conf: Configuration?): T {
        return withRemote(RemoteConnection.from(conf))
    }

    /**
     * Creates a [TraversalSource] binding a [RemoteConnection] to a remote [Graph] instances as its
     * reference so that traversals spawned from it will execute over that reference.
     */
    fun withRemote(remoteConnection: RemoteConnection?): T {
        return try {
            traversalSourceClass.getConstructor(RemoteConnection::class.java).newInstance(remoteConnection)
        } catch (e: Exception) {
            throw IllegalStateException(e.getMessage(), e)
        }
    }

    /**
     * Creates the specified [TraversalSource] binding an embedded [Graph] as its reference such that
     * traversals spawned from it will execute over that reference.
     *
     */
    @Deprecated
    @Deprecated("As of release 3.4.9, replaced by {@link #withEmbedded(Graph)}")
    fun withGraph(graph: Graph?): T {
        return withEmbedded(graph)
    }

    /**
     * Creates the specified [TraversalSource] binding an embedded [Graph] as its reference such that
     * traversals spawned from it will execute over that reference.
     */
    fun withEmbedded(graph: Graph?): T {
        return try {
            traversalSourceClass.getConstructor(Graph::class.java).newInstance(graph)
        } catch (e: Exception) {
            throw IllegalStateException(e.getMessage(), e)
        }
    }

    companion object {
        /**
         * Constructs an `AnonymousTraversalSource` which will then be configured to spawn a
         * [GraphTraversalSource].
         */
        fun traversal(): AnonymousTraversalSource<GraphTraversalSource> {
            return traversal(GraphTraversalSource::class.java)
        }

        /**
         * Constructs an `AnonymousTraversalSource` which will then be configured to spawn the specified
         * [TraversalSource].
         */
        fun <T : TraversalSource?> traversal(traversalSourceClass: Class<T>): AnonymousTraversalSource<T> {
            return AnonymousTraversalSource(traversalSourceClass)
        }
    }
}