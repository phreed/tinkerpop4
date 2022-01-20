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
package org.apache.tinkerpop4.gremlin.process.remote

import org.apache.commons.configuration2.Configuration

/**
 * A simple abstraction of a "connection" to a "server" that is capable of processing a [Traversal] and
 * returning results. Results refer to both the [Iterator] of results from the submitted [Traversal]
 * as well as the side-effects produced by that [Traversal]. Those results together are wrapped in a
 * [Traversal].
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
interface RemoteConnection : AutoCloseable {
    /**
     * Creates a [Transaction] object designed to work with remote semantics.
     */
    fun tx(): Transaction? {
        throw UnsupportedOperationException("This implementation does not support remote transactions")
    }

    /**
     * Submits [Traversal] [Bytecode] to a server and returns a promise of a [RemoteTraversal].
     * The [RemoteTraversal] is an abstraction over two types of results that can be returned as part of the
     * response from the server: the results of the [Traversal] itself and the side-effects that it produced.
     */
    @Throws(RemoteConnectionException::class)
    fun <E> submitAsync(bytecode: Bytecode?): CompletableFuture<RemoteTraversal<*, E>?>?

    companion object {
        const val GREMLIN_REMOTE = "gremlin.remote."
        const val GREMLIN_REMOTE_CONNECTION_CLASS = GREMLIN_REMOTE + "remoteConnectionClass"

        /**
         * Create a [RemoteConnection] from a `Configuration` object. The configuration must contain a
         * `gremlin.remote.remoteConnectionClass` key which is the fully qualified class name of a
         * [RemoteConnection] class.
         */
        fun from(conf: Configuration): RemoteConnection? {
            if (!conf.containsKey(GREMLIN_REMOTE_CONNECTION_CLASS)) throw IllegalArgumentException("Configuration must contain the '" + GREMLIN_REMOTE_CONNECTION_CLASS + "' key")
            return try {
                val clazz: Class<out RemoteConnection> = Class.forName(
                    conf.getString(GREMLIN_REMOTE_CONNECTION_CLASS)
                ).asSubclass(RemoteConnection::class.java)
                val ctor: Constructor<out RemoteConnection> = clazz.getConstructor(Configuration::class.java)
                ctor.newInstance(conf)
            } catch (ex: Exception) {
                throw IllegalStateException(ex)
            }
        }
    }
}