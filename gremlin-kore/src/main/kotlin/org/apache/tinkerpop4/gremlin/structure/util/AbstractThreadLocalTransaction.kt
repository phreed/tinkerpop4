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
import java.util.ArrayList
import java.util.List
import java.util.Optional
import java.util.function.Consumer

/**
 * A base implementation of [Transaction] that provides core functionality for transaction listeners using
 * [ThreadLocal].  In this implementation, the listeners are bound to the current thread of execution (usually
 * the same as the transaction for most graph database implementations).  Therefore, when [.commit] is
 * called on a particular thread, the only listeners that get notified are those bound to that thread.
 *
 * @see AbstractThreadedTransaction
 *
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
abstract class AbstractThreadLocalTransaction(g: Graph?) : AbstractTransaction(g) {
    protected val readWriteConsumerInternal: ThreadLocal<Consumer<Transaction>> =
        object : ThreadLocal<Consumer<Transaction?>?>() {
            @Override
            protected fun initialValue(): Consumer<Transaction> {
                return READ_WRITE_BEHAVIOR.AUTO
            }
        }
    protected val closeConsumerInternal: ThreadLocal<Consumer<Transaction>> =
        object : ThreadLocal<Consumer<Transaction?>?>() {
            @Override
            protected fun initialValue(): Consumer<Transaction> {
                return CLOSE_BEHAVIOR.ROLLBACK
            }
        }
    protected val transactionListeners: ThreadLocal<List<Consumer<Transaction.Status>>> =
        object : ThreadLocal<List<Consumer<Transaction.Status?>?>?>() {
            @Override
            protected fun initialValue(): List<Consumer<Transaction.Status>> {
                return ArrayList()
            }
        }

    @Override
    protected fun fireOnCommit() {
        transactionListeners.get().forEach { c -> c.accept(Status.COMMIT) }
    }

    @Override
    protected fun fireOnRollback() {
        transactionListeners.get().forEach { c -> c.accept(Status.ROLLBACK) }
    }

    @Override
    fun addTransactionListener(listener: Consumer<Status?>?) {
        transactionListeners.get().add(listener)
    }

    @Override
    fun removeTransactionListener(listener: Consumer<Status?>?) {
        transactionListeners.get().remove(listener)
    }

    @Override
    fun clearTransactionListeners() {
        transactionListeners.get().clear()
    }

    @Override
    protected fun doReadWrite() {
        readWriteConsumerInternal.get().accept(this)
    }

    @Override
    protected fun doClose() {
        closeConsumerInternal.get().accept(this)
        closeConsumerInternal.remove()
        readWriteConsumerInternal.remove()
    }

    @Override
    fun onReadWrite(consumer: Consumer<Transaction?>?): Transaction {
        readWriteConsumerInternal.set(
            Optional.ofNullable(consumer).orElseThrow(Transaction.Exceptions::onReadWriteBehaviorCannotBeNull)
        )
        return this
    }

    @Override
    fun onClose(consumer: Consumer<Transaction?>?): Transaction {
        closeConsumerInternal.set(
            Optional.ofNullable(consumer).orElseThrow(Transaction.Exceptions::onReadWriteBehaviorCannotBeNull)
        )
        return this
    }
}