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
package org.apache.tinkerpop4.gremlin.process.traversal.strategy.decoration

import org.apache.commons.configuration2.Configuration

/**
 * This strategy will not alter the traversal. It is only a holder for configuration options associated with the
 * traversal meant to be accessed by steps or other classes that might have some interaction with it. It is
 * essentially a way for users to provide traversal level configuration options that can be used in various ways by
 * different graph providers.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class OptionsStrategy private constructor(builder: Builder) : AbstractTraversalStrategy<DecorationStrategy?>(),
    DecorationStrategy {
    private val options: Map<String, Object>

    init {
        options = builder.options
    }

    /**
     * Gets the options on the strategy as an immutable `Map`.
     */
    fun getOptions(): Map<String, Object> {
        return Collections.unmodifiableMap(options)
    }

    @get:Override
    val configuration: Configuration
        get() = MapConfiguration(options)

    @Override
    fun apply(traversal: Traversal.Admin<*, *>?) {
        // has not effect on the traversal itself - simply carries a options with it that individual steps
        // can choose to use or not.
    }

    class Builder {
        private val options: Map<String, Object> = HashMap()

        /**
         * Adds an key to the configuration with the value of `true`.
         */
        fun with(key: String?): Builder {
            return with(key, true)
        }

        /**
         * Adds an option to the configuration.
         */
        fun with(key: String?, value: Object?): Builder {
            options.put(key, value)
            return this
        }

        fun create(): OptionsStrategy {
            return OptionsStrategy(this)
        }
    }

    companion object {
        /**
         * An empty `OptionsStrategy` with no configuration values inside.
         */
        val EMPTY = build().create()
        fun create(configuration: Configuration): OptionsStrategy {
            val builder = build()
            configuration.getKeys().forEachRemaining { k -> builder.with(k, configuration.getProperty(k)) }
            return builder.create()
        }

        fun build(): Builder {
            return Builder()
        }
    }
}