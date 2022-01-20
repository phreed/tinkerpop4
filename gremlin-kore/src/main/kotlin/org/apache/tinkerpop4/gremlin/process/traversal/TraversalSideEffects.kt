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
package org.apache.tinkerpop4.gremlin.process.traversal

import org.apache.tinkerpop4.gremlin.util.function.ConstantSupplier

/**
 * A [Traversal] can maintain global sideEffects.
 * Unlike [Traverser] "sacks" which are local sideEffects, TraversalSideEffects are accessible by all [Traverser] instances within the [Traversal].
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
interface TraversalSideEffects : Cloneable, Serializable, AutoCloseable {
    /**
     * Return true if the key is a registered side-effect.
     *
     * @param key the key to check for existence
     * @return whether the key exists or not
     */
    fun exists(key: String?): Boolean {
        return keys().contains(key)
    }

    /**
     * Get the sideEffect associated with the provided key.
     * If the sideEffect contains an object for the key, return it.
     * Else if the sideEffect has a registered `Supplier` for that key, generate the object, store the object in the sideEffects, and return it.
     *
     * @param key the key to get the value for
     * @param <V> the type of the value to retrieve
     * @return the value associated with key
     * @throws IllegalArgumentException if the key does not reference an object or a registered supplier.
    </V> */
    @Throws(IllegalArgumentException::class)
    operator fun <V> get(key: String?): V

    /**
     * Set the specified key to the specified value.
     * This method should not be used in a distributed environment. Instead, use [TraversalSideEffects.add].
     * This method is only safe when there is only one representation of the side-effect and thus, not distributed across threads or machines.
     *
     * @param key   the key they key of the side-effect
     * @param value the value the new value for the side-effect
     * @throws IllegalArgumentException if the key does not reference a registered side-effect.
     */
    @Throws(IllegalArgumentException::class)
    operator fun set(key: String?, value: Object?)

    /**
     * Remove both the value and registered [java.util.function.Supplier] associated with provided key.
     *
     * @param key the key of the value and registered supplier to remove
     */
    fun remove(key: String?)

    /**
     * The keys of the sideEffect which includes registered `Supplier` keys. In essence, that which is possible
     * to [.get].
     *
     * @return the keys of the sideEffect
     */
    fun keys(): Set<String?>

    /**
     * Invalidate the side effect cache for traversal.
     */
    @Throws(Exception::class)
    fun close() {
        // do nothing
    }

    /**
     * Determines if there are any side-effects to be retrieved.
     */
    val isEmpty: Boolean
        get() = keys().size() === 0

    /**
     * Register a side-effect with the [TraversalSideEffects] providing a [Supplier] and a [BinaryOperator].
     * If a null value is provided for the supplier or reducer, then it no supplier or reducer is registered.
     *
     * @param key          the key of the side-effect value
     * @param initialValue the initial value supplier
     * @param reducer      the reducer to use for merging a distributed side-effect value into a single value
     * @param <V>          the type of the side-effect value
    </V> */
    fun <V> register(key: String?, initialValue: Supplier<V>?, reducer: BinaryOperator<V>?)

    /**
     * Register a side-effect with the [TraversalSideEffects] providing a [Supplier] and a [BinaryOperator].
     * The registration will only overwrite a supplier or reducer if no supplier or reducer existed prior.
     * If a null value is provided for the supplier or reducer, then it no supplier or reducer is registered.
     *
     * @param key          the key of the side-effect value
     * @param initialValue the initial value supplier
     * @param reducer      the reducer to use for merging a distributed side-effect value into a single value
     * @param <V>          the type of the side-effect value
    </V> */
    fun <V> registerIfAbsent(key: String?, initialValue: Supplier<V>?, reducer: BinaryOperator<V>?)

    /**
     * Get the reducer associated with the side-effect key. If no reducer was registered, then [Operator.assign] is provided.
     *
     * @param key the key of the side-effect
     * @param <V> the type of the side-effect value
     * @return the registered reducer
     * @throws IllegalArgumentException if no side-effect exists for the provided key
    </V> */
    @Throws(IllegalArgumentException::class)
    fun <V> getReducer(key: String?): BinaryOperator<V>?

    /**
     * Get the supplier associated with the side-effect key. If no supplier was registered, then [ConstantSupplier] is provided.
     *
     * @param key the key of the side-effect
     * @param <V> the type of the side-effect value
     * @return the registered supplier
     * @throws IllegalArgumentException if no side-effect exists for the provided key
    </V> */
    @Throws(IllegalArgumentException::class)
    fun <V> getSupplier(key: String?): Supplier<V>?

    /**
     * Add a value to the global side-effect value.
     * This should be used by steps to ensure that side-effects are merged properly in a distributed environment.
     * [TraversalSideEffects.set]  should only be used in single-threaded systems or by a master traversal in a distributed environment.
     *
     * @param key   the key of the side-effect.
     * @param value the partital value (to be merged) of the side-effect.
     * @throws IllegalArgumentException if no side-effect exists for the provided key
     */
    @Throws(IllegalArgumentException::class)
    fun add(key: String?, value: Object?)

    /**
     * Set the initial value of each [Traverser] "sack" along with the operators for splitting and merging sacks.
     * If no split operator is provided, then a direct memory copy is assumed (this is typically good for primitive types and strings).
     * If no merge operator is provided, then traversers with sacks will not be merged.
     *
     * @param initialValue  the initial value supplier of the traverser sack
     * @param splitOperator the split operator for splitting traverser sacks
     * @param mergeOperator the merge operator for merging traverser sacks
     * @param <S>           the sack type
    </S> */
    fun <S> setSack(initialValue: Supplier<S>?, splitOperator: UnaryOperator<S>?, mergeOperator: BinaryOperator<S>?)

    /**
     * If sacks are enabled, get the initial value of the [Traverser] sack.
     * If its not enabled, then `null` is returned.
     *
     * @param <S> the sack type
     * @return the supplier of the initial value of the traverser sack
    </S> */
    fun <S> getSackInitialValue(): Supplier<S>?

    /**
     * If sacks are enabled and a split operator has been specified, then get it (else get `null`).
     * The split operator is used to split a sack when a bifurcation in a [Traverser] happens.
     *
     * @param <S> the sack type
     * @return the operator for splitting a traverser sack
    </S> */
    fun <S> getSackSplitter(): UnaryOperator<S>?

    /**
     * If sacks are enabled and a merge function has been specified, then get it (else get `null`).
     * The merge function is used to merge two sacks when two [Traverser]s converge.
     *
     * @param <S> the sack type
     * @return the operator for merging two traverser sacks
    </S> */
    fun <S> getSackMerger(): BinaryOperator<S>?

    ////////////
    fun <V> forEach(biConsumer: BiConsumer<String?, V>) {
        keys().forEach { key -> biConsumer.accept(key, get<V>(key)) }
    }

    /**
     * Cloning is used to duplicate the sideEffects typically in distributed execution environments.
     *
     * @return The cloned sideEffects
     */
    @SuppressWarnings("CloneDoesntDeclareCloneNotSupportedException")
    fun clone(): TraversalSideEffects?

    /**
     * Add the current [TraversalSideEffects] values, suppliers, and reducers to the provided [TraversalSideEffects].
     * The implementation should (under the hood), use [TraversalSideEffects.registerIfAbsent] so that
     * if the argument [TraversalSideEffects] already has a registered supplier or binary operator, then don't overwrite it.
     *
     * @param sideEffects the sideEffects to add this traversal's sideEffect data to.
     */
    fun mergeInto(sideEffects: TraversalSideEffects?)

    object Exceptions {
        fun sideEffectKeyCanNotBeEmpty(): IllegalArgumentException {
            return IllegalArgumentException("Side-effect key can not be the empty string")
        }

        fun sideEffectKeyCanNotBeNull(): IllegalArgumentException {
            return IllegalArgumentException("Side-effect key can not be null")
        }

        fun sideEffectValueCanNotBeNull(): IllegalArgumentException {
            return IllegalArgumentException("Side-effect value can not be null")
        }

        fun sideEffectKeyDoesNotExist(key: String): IllegalArgumentException {
            return IllegalArgumentException("The side-effect key does not exist in the side-effects: $key")
        }
    }
}