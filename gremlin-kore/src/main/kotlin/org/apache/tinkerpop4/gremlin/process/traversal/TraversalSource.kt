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

import org.apache.tinkerpop4.gremlin.process.computer.Computer

/**
 * A `TraversalSource` is used to create [Traversal] instances.
 * A traversal source can generate any number of [Traversal] instances.
 * A traversal source is primarily composed of a [Graph] and a [TraversalStrategies].
 * Various `withXXX`-based methods are used to configure the traversal strategies (called "configurations").
 * Various other methods (dependent on the traversal source type) will then generate a traversal given the graph and configured strategies (called "spawns").
 * A traversal source is immutable in that fluent chaining of configurations create new traversal sources.
 * This is unlike [Traversal] and [GraphComputer], where chained methods configure the same instance.
 * Every traversal source implementation must maintain two constructors to enable proper reflection-based construction.
 *
 *
 * `TraversalSource(Graph)` and `TraversalSource(Graph,TraversalStrategies)`
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
interface TraversalSource : Cloneable, AutoCloseable {
    /**
     * Get the [TraversalStrategies] associated with this traversal source.
     *
     * @return the traversal strategies of the traversal source
     */
    val strategies: TraversalStrategies

    /**
     * Get the [Graph] associated with this traversal source.
     *
     * @return the graph of the traversal source
     */
    val graph: Graph?

    /**
     * Get the [Bytecode] associated with the current state of this traversal source.
     *
     * @return the traversal source byte code
     */
    val bytecode: Bytecode

    /////////////////////////////
    object Symbols {
        const val with = "with"
        const val withSack = "withSack"
        const val withStrategies = "withStrategies"
        const val withoutStrategies = "withoutStrategies"
        const val withComputer = "withComputer"
        const val withSideEffect = "withSideEffect"
        const val withRemote = "withRemote"
    }
    /////////////////////////////
    /**
     * Provides a configuration to a traversal in the form of a key which is the same as `with(key, true)`. The
     * key of the configuration must be graph provider specific and therefore a configuration could be supplied that
     * is not known to be valid until execution.
     *
     * @param key the key of the configuration to apply to a traversal
     * @return a new traversal source with the included configuration
     * @since 3.4.0
     */
    fun with(key: String?): TraversalSource? {
        return with(key, true)
    }

    /**
     * Provides a configuration to a traversal in the form of a key value pair. The  key of the configuration must be
     * graph provider specific and therefore a configuration could be supplied that is not known to be valid until
     * execution. This is a handy shortcut for building an [OptionsStrategy] manually and then add with
     * [.withStrategies].
     *
     * @param key the key of the configuration to apply to a traversal
     * @param value the value of the configuration to apply to a traversal
     * @return a new traversal source with the included configuration
     * @since 3.4.0
     */
    fun with(key: String?, value: Object?): TraversalSource? {
        val builder: OptionsStrategy.Builder = OptionsStrategy.build()
        strategies.getStrategy(OptionsStrategy::class.java)
            .ifPresent { optionsStrategy -> optionsStrategy.getOptions().forEach(builder::with) }
        builder.with(key, value)
        return withStrategies(builder.create())
    }

    /**
     * Add an arbitrary collection of [TraversalStrategy] instances to the traversal source.
     *
     * @param traversalStrategies a collection of traversal strategies to add
     * @return a new traversal source with updated strategies
     */
    fun withStrategies(vararg traversalStrategies: TraversalStrategy?): TraversalSource? {
        val clone = clone()
        clone.strategies.addStrategies(traversalStrategies)
        clone.bytecode.addSource(Symbols.withStrategies, traversalStrategies)
        for (traversalStrategy in traversalStrategies) {
            if (traversalStrategy is VertexProgramStrategy) {
                (traversalStrategy as VertexProgramStrategy).addGraphComputerStrategies(clone)
            }
        }
        return clone
    }

    /**
     * Remove an arbitrary collection of [TraversalStrategy] classes from the traversal source.
     *
     * @param traversalStrategyClasses a collection of traversal strategy classes to remove
     * @return a new traversal source with updated strategies
     */
    @SuppressWarnings(["unchecked", "varargs"])
    fun withoutStrategies(vararg traversalStrategyClasses: Class<out TraversalStrategy?>?): TraversalSource? {
        val clone = clone()
        clone.strategies.removeStrategies(traversalStrategyClasses)
        clone.bytecode.addSource(Symbols.withoutStrategies, traversalStrategyClasses)
        return clone
    }

    /**
     * Add a [Computer] that will generate a [GraphComputer] from the [Graph] that will be used to execute the traversal.
     * This adds a [VertexProgramStrategy] to the strategies.
     *
     * @param computer a builder to generate a graph computer from the graph
     * @return a new traversal source with updated strategies
     */
    fun withComputer(computer: Computer?): TraversalSource? {
        return withStrategies(VertexProgramStrategy(computer))
    }

    /**
     * Add a [GraphComputer] class used to execute the traversal.
     * This adds a [VertexProgramStrategy] to the strategies.
     *
     * @param graphComputerClass the graph computer class
     * @return a new traversal source with updated strategies
     */
    fun withComputer(graphComputerClass: Class<out GraphComputer?>?): TraversalSource? {
        return withStrategies(VertexProgramStrategy(Computer.compute(graphComputerClass)))
    }

    /**
     * Add the standard [GraphComputer] of the graph that will be used to execute the traversal.
     * This adds a [VertexProgramStrategy] to the strategies.
     *
     * @return a new traversal source with updated strategies
     */
    fun withComputer(): TraversalSource? {
        return withStrategies(VertexProgramStrategy(Computer.compute()))
    }

    /**
     * Add a sideEffect to be used throughout the life of a spawned [Traversal].
     * This adds a [SideEffectStrategy] to the strategies.
     *
     * @param key          the key of the sideEffect
     * @param initialValue a supplier that produces the initial value of the sideEffect
     * @param reducer      a reducer to merge sideEffect mutations into a single result
     * @return a new traversal source with updated strategies
     */
    fun <A> withSideEffect(key: String?, initialValue: Supplier<A>, reducer: BinaryOperator<A>?): TraversalSource? {
        val clone = clone()
        SideEffectStrategy.addSideEffect(clone.strategies, key, initialValue as A, reducer)
        clone.bytecode.addSource(Symbols.withSideEffect, key, initialValue, reducer)
        return clone
    }

    /**
     * Add a sideEffect to be used throughout the life of a spawned [Traversal].
     * This adds a [SideEffectStrategy] to the strategies.
     *
     * @param key          the key of the sideEffect
     * @param initialValue the initial value of the sideEffect
     * @param reducer      a reducer to merge sideEffect mutations into a single result
     * @return a new traversal source with updated strategies
     */
    fun <A> withSideEffect(key: String?, initialValue: A, reducer: BinaryOperator<A>?): TraversalSource? {
        val clone = clone()
        SideEffectStrategy.addSideEffect(clone.strategies, key, initialValue, reducer)
        clone.bytecode.addSource(Symbols.withSideEffect, key, initialValue, reducer)
        return clone
    }

    /**
     * Add a sideEffect to be used throughout the life of a spawned [Traversal].
     * This adds a [SideEffectStrategy] to the strategies.
     *
     * @param key          the key of the sideEffect
     * @param initialValue a supplier that produces the initial value of the sideEffect
     * @return a new traversal source with updated strategies
     */
    fun <A> withSideEffect(key: String?, initialValue: Supplier<A>): TraversalSource? {
        val clone = clone()
        SideEffectStrategy.addSideEffect(clone.strategies, key, initialValue as A, null)
        clone.bytecode.addSource(Symbols.withSideEffect, key, initialValue)
        return clone
    }

    /**
     * Add a sideEffect to be used throughout the life of a spawned [Traversal].
     * This adds a [SideEffectStrategy] to the strategies.
     *
     * @param key          the key of the sideEffect
     * @param initialValue the initial value of the sideEffect
     * @return a new traversal source with updated strategies
     */
    fun <A> withSideEffect(key: String?, initialValue: A): TraversalSource? {
        val clone = clone()
        SideEffectStrategy.addSideEffect(clone.strategies, key, initialValue, null)
        clone.bytecode.addSource(Symbols.withSideEffect, key, initialValue)
        return clone
    }

    /**
     * Add a sack to be used throughout the life of a spawned [Traversal].
     * This adds a [SackStrategy] to the strategies.
     *
     * @param initialValue  a supplier that produces the initial value of the sideEffect
     * @param splitOperator the sack split operator
     * @param mergeOperator the sack merge operator
     * @return a new traversal source with updated strategies
     */
    fun <A> withSack(
        initialValue: Supplier<A>?,
        splitOperator: UnaryOperator<A>?,
        mergeOperator: BinaryOperator<A>?
    ): TraversalSource? {
        val clone = clone()
        clone.strategies.addStrategies(
            SackStrategy.< A > build < A ? > ().initialValue(initialValue).splitOperator(splitOperator)
                .mergeOperator(mergeOperator).create()
        )
        clone.bytecode.addSource(Symbols.withSack, initialValue, splitOperator, mergeOperator)
        return clone
    }

    /**
     * Add a sack to be used throughout the life of a spawned [Traversal].
     * This adds a [SackStrategy] to the strategies.
     *
     * @param initialValue  the initial value of the sideEffect
     * @param splitOperator the sack split operator
     * @param mergeOperator the sack merge operator
     * @return a new traversal source with updated strategies
     */
    fun <A> withSack(
        initialValue: A,
        splitOperator: UnaryOperator<A>?,
        mergeOperator: BinaryOperator<A>?
    ): TraversalSource? {
        val clone = clone()
        clone.strategies.addStrategies(
            SackStrategy.< A > build < A ? > ().initialValue(ConstantSupplier(initialValue))
                .splitOperator(splitOperator).mergeOperator(mergeOperator).create()
        )
        clone.bytecode.addSource(Symbols.withSack, initialValue, splitOperator, mergeOperator)
        return clone
    }

    /**
     * Add a sack to be used throughout the life of a spawned [Traversal].
     * This adds a [SackStrategy] to the strategies.
     *
     * @param initialValue the initial value of the sideEffect
     * @return a new traversal source with updated strategies
     */
    fun <A> withSack(initialValue: A): TraversalSource? {
        val clone = clone()
        clone.strategies.addStrategies(
            SackStrategy.< A > build < A ? > ().initialValue(ConstantSupplier(initialValue)).create()
        )
        clone.bytecode.addSource(Symbols.withSack, initialValue)
        return clone
    }

    /**
     * Add a sack to be used throughout the life of a spawned [Traversal].
     * This adds a [SackStrategy] to the strategies.
     *
     * @param initialValue a supplier that produces the initial value of the sideEffect
     * @return a new traversal source with updated strategies
     */
    fun <A> withSack(initialValue: Supplier<A>?): TraversalSource? {
        val clone = clone()
        clone.strategies.addStrategies(SackStrategy.< A > build < A ? > ().initialValue(initialValue).create())
        clone.bytecode.addSource(Symbols.withSack, initialValue)
        return clone
    }

    /**
     * Add a sack to be used throughout the life of a spawned [Traversal].
     * This adds a [SackStrategy] to the strategies.
     *
     * @param initialValue  a supplier that produces the initial value of the sideEffect
     * @param splitOperator the sack split operator
     * @return a new traversal source with updated strategies
     */
    fun <A> withSack(initialValue: Supplier<A>?, splitOperator: UnaryOperator<A>?): TraversalSource? {
        val clone = clone()
        clone.strategies.addStrategies(
            SackStrategy.< A > build < A ? > ().initialValue(initialValue).splitOperator(splitOperator).create()
        )
        clone.bytecode.addSource(Symbols.withSack, initialValue, splitOperator)
        return clone
    }

    /**
     * Add a sack to be used throughout the life of a spawned [Traversal].
     * This adds a [SackStrategy] to the strategies.
     *
     * @param initialValue  the initial value of the sideEffect
     * @param splitOperator the sack split operator
     * @return a new traversal source with updated strategies
     */
    fun <A> withSack(initialValue: A, splitOperator: UnaryOperator<A>?): TraversalSource? {
        val clone = clone()
        clone.strategies.addStrategies(
            SackStrategy.< A > build < A ? > ().initialValue(ConstantSupplier(initialValue))
                .splitOperator(splitOperator).create()
        )
        clone.bytecode.addSource(Symbols.withSack, initialValue, splitOperator)
        return clone
    }

    /**
     * Add a sack to be used throughout the life of a spawned [Traversal].
     * This adds a [SackStrategy] to the strategies.
     *
     * @param initialValue  a supplier that produces the initial value of the sideEffect
     * @param mergeOperator the sack merge operator
     * @return a new traversal source with updated strategies
     */
    fun <A> withSack(initialValue: Supplier<A>?, mergeOperator: BinaryOperator<A>?): TraversalSource? {
        val clone = clone()
        clone.strategies.addStrategies(
            SackStrategy.< A > build < A ? > ().initialValue(initialValue).mergeOperator(mergeOperator).create()
        )
        clone.bytecode.addSource(Symbols.withSack, initialValue, mergeOperator)
        return clone
    }

    /**
     * Add a sack to be used throughout the life of a spawned [Traversal].
     * This adds a [SackStrategy] to the strategies.
     *
     * @param initialValue  the initial value of the sideEffect
     * @param mergeOperator the sack merge operator
     * @return a new traversal source with updated strategies
     */
    fun <A> withSack(initialValue: A, mergeOperator: BinaryOperator<A>?): TraversalSource? {
        val clone = clone()
        clone.strategies.addStrategies(
            SackStrategy.< A > build < A ? > ().initialValue(ConstantSupplier(initialValue))
                .mergeOperator(mergeOperator).create()
        )
        clone.bytecode.addSource(Symbols.withSack, initialValue, mergeOperator)
        return clone
    }

    val anonymousTraversalClass: Optional<Class<*>?>?
        get() = Optional.empty()

    /**
     * The clone-method should be used to create immutable traversal sources with each call to a configuration "withXXX"-method.
     * The clone-method should clone the [Bytecode], [TraversalStrategies], mutate the cloned strategies accordingly,
     * and then return the cloned traversal source leaving the original unaltered.
     *
     * @return the cloned traversal source
     */
    @SuppressWarnings("CloneDoesntDeclareCloneNotSupportedException")
    fun clone(): TraversalSource

    @Override
    @Throws(Exception::class)
    fun close() {
        // do nothing
    }
}