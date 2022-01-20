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
package org.apache.tinkerpop4.gremlin.structure

import org.apache.tinkerpop4.gremlin.process.traversal.TraversalSource
import org.apache.tinkerpop4.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop4.gremlin.structure.util.AbstractTransaction
import org.apache.tinkerpop4.gremlin.structure.util.TransactionException
import java.util.function.Consumer

/**
 * A set of methods that allow for control of transactional behavior of a [Graph] instance. Providers may
 * consider using [AbstractTransaction] as a base implementation that provides default features for most of
 * these methods.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 * @author TinkerPop Community (http://tinkerpop.apache.org)
 */
interface Transaction : AutoCloseable {
    /**
     * Opens a transaction.
     */
    fun open()

    /**
     * Commits a transaction. This method may optionally throw [TransactionException] on error. Providers should
     * consider wrapping their transaction exceptions in this TinkerPop exception as it will lead to better error
     * handling with Gremlin Server and other parts of the stack.
     */
    fun commit()

    /**
     * Rolls back a transaction. This method may optionally throw [TransactionException] on error. Providers should
     * consider wrapping their transaction exceptions in this TinkerPop exception as it will lead to better error
     * handling with Gremlin Server and other parts of the stack.
     */
    fun rollback()

    /**
     * Creates a transaction that can be executed across multiple threads. The [Graph] returned from this
     * method is not meant to represent some form of child transaction that can be committed from this object.
     * A threaded transaction is a [Graph] instance that has a transaction context that enables multiple
     * threads to collaborate on the same transaction.  A standard transactional context tied to a [Graph]
     * that supports transactions will typically bind a transaction to a single thread via [ThreadLocal].
     */
    fun <G : Graph?> createThreadedTx(): G {
        throw Exceptions.threadedTransactionsNotSupported()
    }

    /**
     * Starts a transaction in the context of a [GraphTraversalSource] instance. It is up to the
     * [Transaction] implementation to decide what this means and up to users to be aware of that meaning.
     */
    fun <T : TraversalSource?> begin(): T {
        return begin(GraphTraversalSource::class.java) as T
    }

    /**
     * Starts a transaction in the context of a particular [TraversalSource] instance. It is up to the
     * [Transaction] implementation to decide what this means and up to users to be aware of that meaning.
     */
    fun <T : TraversalSource?> begin(traversalSourceClass: Class<T>?): T

    /**
     * Determines if a transaction is currently open.
     */
    val isOpen: Boolean

    /**
     * An internal function that signals a read or a write has occurred - not meant to be called directly by end users.
     */
    fun readWrite()

    /**
     * Closes the transaction where the default close behavior defined by {[.onClose]} will be
     * executed.
     */
    @Override
    fun close()

    /**
     * Describes how a transaction is started when a read or a write occurs.  This value can be set using standard
     * behaviors defined in [READ_WRITE_BEHAVIOR] or a mapper [Consumer] function.
     */
    fun onReadWrite(consumer: Consumer<Transaction?>?): Transaction?

    /**
     * Describes what happens to a transaction on a call to [Graph.close]. This value can be set using
     * standard behavior defined in [CLOSE_BEHAVIOR] or a mapper [Consumer] function.
     */
    fun onClose(consumer: Consumer<Transaction?>?): Transaction?

    /**
     * Adds a listener that is called back with a status when a commit or rollback is successful.  It is expected
     * that listeners be bound to the current thread as is standard for transactions.  Therefore a listener registered
     * in the current thread will not get callback events from a commit or rollback call in a different thread.
     */
    fun addTransactionListener(listener: Consumer<Status?>?)

    /**
     * Removes a transaction listener.
     */
    fun removeTransactionListener(listener: Consumer<Status?>?)

    /**
     * Removes all transaction listeners.
     */
    fun clearTransactionListeners()

    /**
     * A status provided to transaction listeners to inform whether a transaction was successfully committed
     * or rolled back.
     */
    enum class Status {
        COMMIT, ROLLBACK
    }

    object Exceptions {
        fun transactionAlreadyOpen(): IllegalStateException {
            return IllegalStateException("Stop the current transaction before opening another")
        }

        fun transactionMustBeOpenToReadWrite(): IllegalStateException {
            return IllegalStateException("Open a transaction before attempting to read/write the transaction")
        }

        fun openTransactionsOnClose(): IllegalStateException {
            return IllegalStateException("Commit or rollback all outstanding transactions before closing the transaction")
        }

        fun threadedTransactionsNotSupported(): UnsupportedOperationException {
            return UnsupportedOperationException("Graph does not support threaded transactions")
        }

        fun onCloseBehaviorCannotBeNull(): IllegalArgumentException {
            return IllegalArgumentException("Transaction behavior for onClose cannot be null")
        }

        fun onReadWriteBehaviorCannotBeNull(): IllegalArgumentException {
            return IllegalArgumentException("Transaction behavior for onReadWrite cannot be null")
        }
    }

    /**
     * Behaviors to supply to the [.onClose]. The semantics of these behaviors must be examined in
     * the context of the implementation.  In most cases, these behaviors will be applied as {[ThreadLocal]}.
     */
    enum class CLOSE_BEHAVIOR : Consumer<Transaction?> {
        /**
         * Commit the transaction when [.close] is called.
         */
        COMMIT {
            @Override
            fun accept(transaction: Transaction) {
                if (transaction.isOpen) transaction.commit()
            }
        },

        /**
         * Rollback the transaction when [.close] is called.
         */
        ROLLBACK {
            @Override
            fun accept(transaction: Transaction) {
                if (transaction.isOpen) transaction.rollback()
            }
        },

        /**
         * Throw an exception if the current transaction is open when [.close] is called.
         */
        MANUAL {
            @Override
            fun accept(transaction: Transaction) {
                if (transaction.isOpen) throw Exceptions.openTransactionsOnClose()
            }
        }
    }

    /**
     * Behaviors to supply to the [.onReadWrite].
     */
    enum class READ_WRITE_BEHAVIOR : Consumer<Transaction?> {
        /**
         * Transactions are automatically started when a read or a write occurs.
         */
        AUTO {
            @Override
            fun accept(transaction: Transaction) {
                if (!transaction.isOpen) transaction.open()
            }
        },

        /**
         * Transactions must be explicitly opened for operations to occur on the graph.
         */
        MANUAL {
            @Override
            fun accept(transaction: Transaction) {
                if (!transaction.isOpen) throw Exceptions.transactionMustBeOpenToReadWrite()
            }
        }
    }

    companion object {
        val NO_OP: Transaction = object : Transaction {
            @Override
            override fun open() {
                throw UnsupportedOperationException("This Transaction implementation is a no-op for all methods")
            }

            @Override
            override fun commit() {
                throw UnsupportedOperationException("This Transaction implementation is a no-op for all methods")
            }

            @Override
            override fun rollback() {
                throw UnsupportedOperationException("This Transaction implementation is a no-op for all methods")
            }

            @Override
            override fun <C : TraversalSource?> begin(traversalSourceClass: Class<C>?): C {
                throw UnsupportedOperationException("This Transaction implementation is a no-op for all methods")
            }

            @get:Override
            override val isOpen: Boolean
                get() {
                    throw UnsupportedOperationException("This Transaction implementation is a no-op for all methods")
                }

            @Override
            override fun readWrite() {
                throw UnsupportedOperationException("This Transaction implementation is a no-op for all methods")
            }

            @Override
            override fun close() {
                throw UnsupportedOperationException("This Transaction implementation is a no-op for all methods")
            }

            @Override
            override fun onReadWrite(consumer: Consumer<Transaction?>?): Transaction {
                throw UnsupportedOperationException("This Transaction implementation is a no-op for all methods")
            }

            @Override
            override fun onClose(consumer: Consumer<Transaction?>?): Transaction {
                throw UnsupportedOperationException("This Transaction implementation is a no-op for all methods")
            }

            @Override
            override fun addTransactionListener(listener: Consumer<Status?>?) {
                throw UnsupportedOperationException("This Transaction implementation is a no-op for all methods")
            }

            @Override
            override fun removeTransactionListener(listener: Consumer<Status?>?) {
                throw UnsupportedOperationException("This Transaction implementation is a no-op for all methods")
            }

            @Override
            override fun clearTransactionListeners() {
                throw UnsupportedOperationException("This Transaction implementation is a no-op for all methods")
            }
        }
    }
}