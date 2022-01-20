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
package org.apache.tinkerpop4.gremlin.process.remote.traversal.step.map

import org.apache.tinkerpop4.gremlin.process.remote.RemoteConnection

/**
 * Sends a [Traversal] to a [RemoteConnection] and iterates back the results.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class RemoteStep<S, E>(traversal: Traversal.Admin?, remoteConnection: RemoteConnection) :
    AbstractStep<S, E>(traversal) {
    @kotlin.jvm.Transient
    private val remoteConnection: RemoteConnection
    private var remoteTraversal: RemoteTraversal<*, E>? = null
    private val traversalFuture: AtomicReference<CompletableFuture<Traversal<*, E>>> = AtomicReference(null)

    init {
        this.remoteConnection = remoteConnection
    }

    @Override
    override fun toString(): String {
        return StringFactory.stepString(this, remoteConnection)
    }

    @Override
    @Throws(NoSuchElementException::class)
    protected fun processNextStart(): Traverser.Admin<E> {
        if (null == remoteTraversal) {
            try {
                promise().join()
            } catch (e: CompletionException) {
                val cause: Throwable = e.getCause()
                // If the underlying future failed, join() will throw a CompletionException, for consistency
                // with previous behavior:
                // - Throw underlying exception if it was unchecked (RuntimeException or Error).
                // - Wrap in IllegalStateException otherwise.
                if (cause is RuntimeException) {
                    throw cause as RuntimeException
                } else if (cause is Error) {
                    throw cause as Error
                }
                throw IllegalStateException(cause)
            }
        }
        return remoteTraversal.nextTraverser()
    }

    /**
     * Submits the traversal asynchronously to a "remote" using [RemoteConnection.submitAsync].
     */
    fun promise(): CompletableFuture<Traversal<*, E>> {
        return try {
            if (null == traversalFuture.get()) {
                traversalFuture.set(remoteConnection.submitAsync(this.traversal.getBytecode()).< Traversal <?, E>>thenApply<Traversal<*, E?>?>({ t ->
                    remoteTraversal = t as RemoteTraversal<*, E>
                    traversal
                }))
            }
            traversalFuture.get()
        } catch (rce: RemoteConnectionException) {
            throw IllegalStateException(rce)
        }
    }
}