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
package org.apache.tinkerpop4.gremlin.structure.util

import org.apache.tinkerpop4.gremlin.process.traversal.TraversalSource

/**
 * A simple base class for [Transaction] that provides some common functionality and default behavior.
 * While providers can choose to use this class, it is generally better to extend from
 * [AbstractThreadedTransaction] or [AbstractThreadLocalTransaction] which include default "listener"
 * functionality.  Implementers should note that this class assumes that threaded transactions are not enabled
 * and should explicitly override [.createThreadedTx] to implement that functionality if required.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
abstract class AbstractTransaction(graph: Graph) : Transaction {
    private val graph: Graph

    init {
        this.graph = graph
    }

    /**
     * Called within [.open] if it is determined that the transaction is not yet open given [.isOpen].
     * Implementers should assume the transaction is not yet started and should thus open one.
     */
    protected abstract fun doOpen()

    /**
     * Called with [.commit] after the [.onReadWrite] has been notified.  Implementers should
     * include their commit logic here.
     */
    @Throws(TransactionException::class)
    protected abstract fun doCommit()

    /**
     * Called with [.rollback] after the [.onReadWrite] has been notified.  Implementers should
     * include their rollback logic here.
     */
    @Throws(TransactionException::class)
    protected abstract fun doRollback()

    /**
     * Called within [.commit] just after the internal call to [.doCommit]. Implementations of this
     * method should raise [org.apache.tinkerpop4.gremlin.structure.Transaction.Status.COMMIT] events to any
     * listeners added via [.addTransactionListener].
     */
    protected abstract fun fireOnCommit()

    /**
     * Called within [.rollback] just after the internal call to [.doRollback] ()}. Implementations
     * of this method should raise [org.apache.tinkerpop4.gremlin.structure.Transaction.Status.ROLLBACK] events
     * to any listeners added via [.addTransactionListener].
     */
    protected abstract fun fireOnRollback()

    /**
     * Called [.readWrite].
     * Implementers should run their readWrite consumer here.
     */
    protected abstract fun doReadWrite()

    /**
     * Called [.close].
     * Implementers should run their readWrite consumer here.
     */
    protected abstract fun doClose()

    /**
     * {@inheritDoc}
     */
    @Override
    fun open() {
        if (isOpen()) throw Transaction.Exceptions.transactionAlreadyOpen() else doOpen()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun commit() {
        readWrite()
        doCommit()
        fireOnCommit()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun rollback() {
        readWrite()
        doRollback()
        fireOnRollback()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun <G : Graph?> createThreadedTx(): G {
        throw Transaction.Exceptions.threadedTransactionsNotSupported()
    }

    @Override
    fun <T : TraversalSource?> begin(traversalSourceClass: Class<T>?): T {
        return graph.traversal(traversalSourceClass)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun readWrite() {
        doReadWrite()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun close() {
        doClose()
    }
}