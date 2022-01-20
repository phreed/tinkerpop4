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

import org.apache.tinkerpop4.gremlin.structure.Graph
import org.apache.tinkerpop4.gremlin.structure.Transaction
import java.util.List
import java.util.Optional
import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.Consumer
import java.util.function.Function

/**
 * A base implementation of [Transaction] that provides core functionality for transaction listeners using a
 * shared set of transaction listeners.  Therefore, when [.commit] is called from any thread, all listeners
 * get notified.  This implementation would be useful for graph implementations that support threaded transactions,
 * specifically in the [Graph] instance returned from [Transaction.createThreadedTx].
 *
 * @see AbstractThreadLocalTransaction
 *
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
abstract class AbstractThreadedTransaction(g: Graph?) : AbstractTransaction(g) {
    protected val transactionListeners: List<Consumer<Status>> = CopyOnWriteArrayList()
    @Override
    protected fun fireOnCommit() {
        transactionListeners.forEach { c -> c.accept(Status.COMMIT) }
    }

    @Override
    protected fun fireOnRollback() {
        transactionListeners.forEach { c -> c.accept(Status.ROLLBACK) }
    }

    @Override
    fun addTransactionListener(listener: Consumer<Status?>?) {
        transactionListeners.add(listener)
    }

    @Override
    fun removeTransactionListener(listener: Consumer<Status?>?) {
        transactionListeners.remove(listener)
    }

    @Override
    fun clearTransactionListeners() {
        transactionListeners.clear()
    }

    /**
     * Threaded transactions should be open immediately upon creation so most implementations should do nothing with
     * this method.
     */
    @Override
    protected fun doReadWrite() {
        // do nothing
    }

    /**
     * Clears transaction listeners
     */
    @Override
    protected fun doClose() {
        clearTransactionListeners()
    }

    /**
     * The nature of threaded transactions are such that they are always open when created and manual in nature,
     * therefore setting this value is not required.
     *
     * @throws UnsupportedOperationException
     */
    @Override
    @kotlin.jvm.Synchronized
    fun onReadWrite(consumer: Consumer<Transaction?>?): Transaction {
        throw UnsupportedOperationException("Threaded transactions are open when created and in manual mode")
    }

    /**
     * The nature of threaded transactions are such that they are always open when created and manual in nature,
     * therefore setting this value is not required.
     *
     * @throws UnsupportedOperationException
     */
    @Override
    @kotlin.jvm.Synchronized
    fun onClose(consumer: Consumer<Transaction?>?): Transaction {
        throw UnsupportedOperationException("Threaded transactions are open when created and in manual mode")
    }
}