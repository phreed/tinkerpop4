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
package org.apache.tinkerpop4.gremlin.process.traversal.strategy.verification

import org.apache.commons.configuration2.Configuration

/**
 * Base [TraversalStrategy] class that is configurable to throw warnings or exceptions.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
abstract class AbstractWarningVerificationStrategy internal constructor(builder: Builder<*, *>) :
    AbstractTraversalStrategy<TraversalStrategy.VerificationStrategy?>(), TraversalStrategy.VerificationStrategy {
    protected val throwException: Boolean
    protected val logWarning: Boolean

    init {
        throwException = builder.throwException
        logWarning = builder.logWarning
    }

    /**
     * Implementations should check the traversal and throw a standard [VerificationException] as it would if
     * it had directly implemented [.apply]. The message provided to the exception will be
     * used to log a warning and/or the same exception thrown if configured to do so.
     */
    @Throws(VerificationException::class)
    abstract fun verify(traversal: Traversal.Admin<*, *>?)
    @Override
    fun apply(traversal: Traversal.Admin<*, *>?) {
        try {
            verify(traversal)
        } catch (ve: VerificationException) {
            if (logWarning) LOGGER.warn(ve.getMessage())
            if (throwException) throw ve
        }
    }

    @get:Override
    val configuration: Configuration
        get() {
            val m: Map<String, Object> = HashMap(2)
            m.put(THROW_EXCEPTION, throwException)
            m.put(LOG_WARNING, logWarning)
            return MapConfiguration(m)
        }

    abstract class Builder<T : AbstractWarningVerificationStrategy?, B : Builder<*, *>?> internal constructor() {
        var throwException = false
        var logWarning = false
        @JvmOverloads
        fun throwException(throwException: Boolean = true): B {
            this.throwException = throwException
            return this as B
        }

        @JvmOverloads
        fun logWarning(logWarning: Boolean = true): B {
            this.logWarning = logWarning
            return this as B
        }

        abstract fun create(): T
    }

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(AbstractWarningVerificationStrategy::class.java)
        const val THROW_EXCEPTION = "throwException"
        const val LOG_WARNING = "logWarning"
    }
}