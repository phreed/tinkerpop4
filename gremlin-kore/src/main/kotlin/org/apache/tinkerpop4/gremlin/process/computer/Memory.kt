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
package org.apache.tinkerpop4.gremlin.process.computer

import org.apache.tinkerpop4.gremlin.process.computer.util.ImmutableMemory

/**
 * The Memory of a [GraphComputer] is a global data structure where by vertices can communicate information with one another.
 * Moreover, it also contains global information about the state of the computation such as runtime and the current iteration.
 * The Memory data is logically updated in parallel using associative/commutative methods which have embarrassingly parallel implementations.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
interface Memory {
    /**
     * Whether the key exists in the memory.
     *
     * @param key key to search the memory for.
     * @return whether the key exists
     */
    fun exists(key: String?): Boolean {
        return keys().contains(key)
    }

    /**
     * The set of keys currently associated with this memory.
     *
     * @return the memory's key set.
     */
    fun keys(): Set<String?>

    /**
     * Get the value associated with the provided key.
     *
     * @param key the key of the value
     * @param <R> the type of the value
     * @return the value
     * @throws IllegalArgumentException is thrown if the key does not exist
    </R> */
    @Throws(IllegalArgumentException::class)
    operator fun <R> get(key: String?): R

    @Throws(IllegalArgumentException::class, IllegalStateException::class)
    operator fun set(key: String?, value: Object?)

    /**
     * Set the value of the provided key. This is typically called in setup() and/or terminate() of the
     * [VertexProgram]. If this is called during execute(), there is no guarantee as to the ultimately stored
     * value as call order is indeterminate. It is up to the implementation to determine the states in which this
     * method can be called.
     *
     * @param key   they key to set a value for
     * @param value the value to set for the key
     */
    @Throws(IllegalArgumentException::class, IllegalStateException::class)
    fun add(key: String?, value: Object?)

    /**
     * A helper method that generates a [Map] of the memory key/values.
     *
     * @return the map representation of the memory key/values
     */
    fun asMap(): Map<String?, Object?>? {
        val map: Map<String, Object> = keys().stream()
            .map { key -> Pair.with(key, get(key)) }
            .collect(Collectors.toMap(Pair::getValue0, Pair::getValue1))
        return Collections.unmodifiableMap(map)
    }

    /**
     * Get the current iteration number.
     *
     * @return the current iteration
     */
    val iteration: Int

    /**
     * Get the amount of milliseconds the [GraphComputer] has been executing thus far.
     *
     * @return the total time in milliseconds
     */
    val runtime: Long

    /**
     * A helper method that states whether the current iteration is 0.
     *
     * @return whether this is the first iteration
     */
    val isInitialIteration: Boolean
        get() = iteration == 0

    /**
     * The Admin interface is used by the [GraphComputer] to update the Memory.
     * The developer should never need to type-cast the provided Memory to Memory.Admin.
     */
    interface Admin : Memory {
        fun incrIteration() {
            setIteration(iteration + 1)
        }

        fun setIteration(iteration: Int)
        fun setRuntime(runtime: Long)
        fun asImmutable(): Memory? {
            return ImmutableMemory(this)
        }
    }

    object Exceptions {
        fun memoryKeyCanNotBeEmpty(): IllegalArgumentException {
            return IllegalArgumentException("Graph computer memory key can not be the empty string")
        }

        fun memoryKeyCanNotBeNull(): IllegalArgumentException {
            return IllegalArgumentException("Graph computer memory key can not be null")
        }

        fun memoryValueCanNotBeNull(): IllegalArgumentException {
            return IllegalArgumentException("Graph computer memory value can not be null")
        }

        fun memoryIsCurrentlyImmutable(): IllegalStateException {
            return IllegalStateException("Graph computer memory is currently immutable")
        }

        fun memoryDoesNotExist(key: String): IllegalArgumentException {
            return IllegalArgumentException("The memory does not have a value for provided key: $key")
        }

        fun memorySetOnlyDuringVertexProgramSetUpAndTerminate(key: String): IllegalArgumentException {
            return IllegalArgumentException("The memory can only be set() during vertex program setup and terminate: $key")
        }

        fun memoryAddOnlyDuringVertexProgramExecute(key: String): IllegalArgumentException {
            return IllegalArgumentException("The memory can only be add() during vertex program execute: $key")
        }
    }
}