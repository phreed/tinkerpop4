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
package org.apache.tinkerpop4.gremlin.process.computer.util

import org.apache.tinkerpop4.gremlin.process.computer.Memory

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class EmptyMemory private constructor() : Memory.Admin {
    @Override
    fun asImmutable(): Memory {
        return this
    }

    @Override
    fun keys(): Set<String> {
        return Collections.emptySet()
    }

    @Override
    @Throws(IllegalArgumentException::class)
    operator fun <R> get(key: String?): R {
        throw Memory.Exceptions.memoryDoesNotExist(key)
    }

    @Override
    @Throws(IllegalArgumentException::class, IllegalStateException::class)
    operator fun set(key: String?, value: Object?) {
        throw Memory.Exceptions.memoryIsCurrentlyImmutable()
    }

    @Override
    @Throws(IllegalArgumentException::class, IllegalStateException::class)
    fun add(key: String?, value: Object?) {
        throw Memory.Exceptions.memoryIsCurrentlyImmutable()
    }

    @get:Override
    @set:Override
    var iteration: Int
        get() = 0
        set(iteration) {
            throw Memory.Exceptions.memoryIsCurrentlyImmutable()
        }

    @get:Override
    @set:Override
    var runtime: Long
        get() = 0
        set(runtime) {
            throw Memory.Exceptions.memoryIsCurrentlyImmutable()
        }

    @Override
    fun exists(key: String?): Boolean {
        return false
    }

    companion object {
        private val INSTANCE = EmptyMemory()
        fun instance(): EmptyMemory {
            return INSTANCE
        }
    }
}