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

import org.apache.tinkerpop4.gremlin.jsr223.JavaTranslator

/**
 * Allows a [RemoteConnection] to be submitted to a "local" [Graph] instance thus simulating a connection
 * to a remote source. Basic usage is as follows:
 *
 * <pre>
 * `// Using TinkerGraph here but any embedded Graph instance would suffice
 * Graph graph = TinkerFactory.createModern();
 * GraphTraversalSource g = graph.traversal();
 *
 * // setup the remote as normal but give it the embedded "g" so that it executes against that
 * GraphTraversalSource simulatedRemoteG = TraversalSourceFactory.traversal(new EmbeddedRemoteConnection(g));
 * assertEquals(6, simulatedRemoteG.V().count().next().intValue());
` *
</pre> *
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class EmbeddedRemoteConnection(g: GraphTraversalSource) : RemoteConnection {
    private val g: GraphTraversalSource

    init {
        this.g = g
    }

    @Override
    @Throws(RemoteConnectionException::class)
    fun <E> submitAsync(bytecode: Bytecode?): CompletableFuture<RemoteTraversal<*, E>> {
        // default implementation for backward compatibility to 3.2.4 - this method will probably just become
        // the new submit() in 3.3.x when the deprecation is removed
        val promise: CompletableFuture<RemoteTraversal<*, E>> = CompletableFuture()
        try {
            promise.complete(EmbeddedRemoteTraversal(JavaTranslator.of(g).translate(bytecode)))
        } catch (t: Exception) {
            promise.completeExceptionally(t)
        }
        return promise
    }

    @Override
    @Throws(Exception::class)
    fun close() {
        g.close()
    }
}