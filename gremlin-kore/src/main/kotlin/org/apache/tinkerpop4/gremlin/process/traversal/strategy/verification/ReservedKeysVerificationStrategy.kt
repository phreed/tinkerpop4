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
 * This verification strategy detects property keys that should not be used by the traversal. A term may be reserved
 * by a particular graph implementation or as a convention given best practices.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 * * @example <pre>
 * * __.addV("person").property("id", 123)           // throws an IllegalStateException
 * * __.addE("knows").property("label", "green")     // throws an IllegalStateException
 * * </pre>
 */
class ReservedKeysVerificationStrategy private constructor(builder: Builder) :
    AbstractWarningVerificationStrategy(builder) {
    private val reservedKeys: Set<String>

    init {
        reservedKeys = builder.reservedKeys
    }

    @Override
    @Throws(VerificationException::class)
    fun verify(traversal: Traversal.Admin<*, *>) {
        for (step in traversal.getSteps()) {
            if (step is AddVertexStep || step is AddVertexStartStep ||
                step is AddEdgeStartStep || step is AddEdgeStep ||
                step is AddPropertyStep
            ) {
                val propertySettingStep: Parameterizing = step as Parameterizing
                val params: Parameters = propertySettingStep.getParameters()
                for (key in reservedKeys) {
                    if (params.contains(key)) {
                        val msg: String = String.format(
                            "The provided traversal contains a %s that is setting a property key to a reserved" +
                                    " word: %s", propertySettingStep.getClass().getSimpleName(), key
                        )
                        throw VerificationException(msg, traversal)
                    }
                }
            }
        }
    }

    @get:Override
    val configuration: Configuration
        get() {
            val c: Configuration = super.getConfiguration()
            c.setProperty(KEYS, reservedKeys)
            return c
        }

    class Builder private constructor() :
        AbstractWarningVerificationStrategy.Builder<ReservedKeysVerificationStrategy?, Builder?>() {
        private var reservedKeys = DEFAULT_RESERVED_KEYS
        fun reservedKeys(keys: Set<String>): Builder {
            reservedKeys = keys
            return this
        }

        @Override
        fun create(): ReservedKeysVerificationStrategy {
            return ReservedKeysVerificationStrategy(this)
        }
    }

    companion object {
        const val KEYS = "keys"
        private val DEFAULT_RESERVED_KEYS: Set<String> = HashSet(Arrays.asList("id", "label"))
        fun create(configuration: Configuration): ReservedKeysVerificationStrategy {
            return build()
                .reservedKeys(
                    configuration.getList(KEYS, ArrayList(DEFAULT_RESERVED_KEYS)).stream().map(Object::toString)
                        .collect(Collectors.toSet())
                )
                .throwException(configuration.getBoolean(THROW_EXCEPTION, false))
                .logWarning(configuration.getBoolean(LOG_WARNING, false)).create()
        }

        fun build(): Builder {
            return Builder()
        }
    }
}