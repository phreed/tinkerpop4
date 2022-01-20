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
class ImmutableMemory(baseMemory: Memory) : Memory.Admin {
    private val baseMemory: Memory

    init {
        this.baseMemory = baseMemory
    }

    @Override
    fun keys(): Set<String> {
        return baseMemory.keys()
    }

    @Override
    @Throws(IllegalArgumentException::class)
    operator fun <R> get(key: String?): R {
        return baseMemory.get(key)
    }

    @Override
    operator fun set(key: String?, value: Object?) {
        throw Memory.Exceptions.memoryIsCurrentlyImmutable()
    }

    @get:Override
    @set:Override
    var iteration: Int
        get() = baseMemory.getIteration()
        set(iteration) {
            throw Memory.Exceptions.memoryIsCurrentlyImmutable()
        }

    @get:Override
    @set:Override
    var runtime: Long
        get() = baseMemory.getRuntime()
        set(runtime) {
            throw Memory.Exceptions.memoryIsCurrentlyImmutable()
        }

    @Override
    fun add(key: String?, value: Object?) {
        throw Memory.Exceptions.memoryIsCurrentlyImmutable()
    }

    @Override
    fun incrIteration() {
        throw Memory.Exceptions.memoryIsCurrentlyImmutable()
    }

    @Override
    override fun toString(): String {
        return StringFactory.memoryString(baseMemory)
    }
}