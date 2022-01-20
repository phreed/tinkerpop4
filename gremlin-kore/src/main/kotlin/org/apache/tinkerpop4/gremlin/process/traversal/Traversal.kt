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

import org.apache.commons.configuration2.Configuration

/**
 * A [Traversal] represents a directed walk over a [Graph].
 * This is the base interface for all traversal's, where each extending interface is seen as a domain specific language.
 * For example, [GraphTraversal] is a domain specific language for traversing a graph using "graph concepts" (e.g. vertices, edges).
 * Another example may represent the graph using "social concepts" (e.g. people, cities, artifacts).
 * A [Traversal] is evaluated in one of two ways: iterator-based OLTP or [GraphComputer]-based OLAP.
 * OLTP traversals leverage an iterator and are executed within a single JVM (with data access allowed to be remote).
 * OLAP traversals leverage [GraphComputer] and are executed between multiple JVMs (and/or cores).
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
interface Traversal<S, E> : Iterator<E>, Serializable, Cloneable, AutoCloseable {
    object Symbols {
        const val profile = "profile"
        const val none = "none"
    }

    /**
     * Get access to administrative methods of the traversal via its accompanying [Traversal.Admin].
     *
     * @return the admin of this traversal
     */
    fun asAdmin(): Admin<S, E> {
        return this as Admin<S, E>
    }

    /**
     * Return an [Optional] of the next E object in the traversal.
     * If the traversal is empty, then an [Optional.empty] is returned.
     *
     * @return an optional of the next object in the traversal
     */
    fun tryNext(): Optional<E>? {
        return if (hasNext()) Optional.of(this.next()) else Optional.empty()
    }

    /**
     * Get the next n-number of results from the traversal.
     * If the traversal has less than n-results, then only that number of results are returned.
     *
     * @param amount the number of results to get
     * @return the n-results in a [List]
     */
    fun next(amount: Int): List<E>? {
        val result: List<E> = ArrayList()
        var counter = 0
        while (counter++ < amount && hasNext()) {
            result.add(this.next())
        }
        return result
    }

    /**
     * Put all the results into an [ArrayList].
     *
     * @return the results in a list
     */
    fun toList(): List<E>? {
        return fill(ArrayList())
    }

    /**
     * Put all the results into a [HashSet].
     *
     * @return the results in a set
     */
    fun toSet(): Set<E>? {
        return fill(HashSet())
    }

    /**
     * Put all the results into a [BulkSet].
     * This can reduce both time and space when aggregating results by ensuring a weighted set.
     *
     * @return the results in a bulk set
     */
    fun toBulkSet(): BulkSet<E>? {
        return fill(BulkSet())
    }

    /**
     * Return the traversal as a [Stream].
     *
     * @return the traversal as a stream.
     */
    fun toStream(): Stream<E>? {
        return StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(
                this,
                Spliterator.IMMUTABLE or Spliterator.SIZED
            ), false
        )
    }

    /**
     * Starts a promise to execute a function on the current `Traversal` that will be completed in the future.
     * Note that this method can only be used if the `Traversal` is constructed using
     * [AnonymousTraversalSource.withRemote]. Calling this method otherwise will yield an
     * `IllegalStateException`.
     */
    fun <T> promise(traversalFunction: Function<Traversal<S, E>?, T>?): CompletableFuture<T>? {
        // apply strategies to see if RemoteStrategy has any effect (i.e. add RemoteStep)
        if (!asAdmin().isLocked) asAdmin().applyStrategies()

        // use the end step so the results are bulked
        val endStep: Step<*, E> = asAdmin().endStep
        return if (endStep is RemoteStep) {
            (endStep as RemoteStep).promise().thenApply(traversalFunction)
        } else {
            throw IllegalStateException("Only traversals created using withRemote() can be used in an async way")
        }
    }

    /**
     * Add all the results of the traversal to the provided collection.
     *
     * @param collection the collection to fill
     * @return the collection now filled
     */
    fun <C : Collection<E>?> fill(collection: C): C {
        try {
            if (!asAdmin().isLocked) asAdmin().applyStrategies()
            // use the end step so the results are bulked
            val endStep: Step<*, E> = asAdmin().endStep
            while (true) {
                val traverser: Traverser<E> = endStep.next()
                TraversalHelper.addToCollection(collection, traverser.get(), traverser.bulk())
            }
        } catch (ignored: NoSuchElementException) {
        } finally {
            CloseableIterator.closeIterator(this)
        }
        return collection
    }

    /**
     * Iterate all the [Traverser] instances in the traversal.
     * What is returned is the empty traversal.
     * It is assumed that what is desired from the computation is are the sideEffects yielded by the traversal.
     *
     * @return the fully drained traversal
     */
    fun <A, B> iterate(): Traversal<A, B>? {
        try {
            if (!asAdmin().isLocked) {
                none()
                asAdmin().applyStrategies()
            }
            // use the end step so the results are bulked
            val endStep: Step<*, E> = asAdmin().endStep
            while (true) {
                endStep.next()
            }
        } catch (ignored: NoSuchElementException) {
        } finally {
            CloseableIterator.closeIterator(this)
        }
        return this as Traversal<A, B>
    }

    /**
     * Filter all traversers in the traversal. This step has narrow use cases and is primarily intended for use as a
     * signal to remote servers that [.iterate] was called. While it may be directly used, it is often a sign
     * that a traversal should be re-written in another form.
     *
     * @return the updated traversal with respective [NoneStep].
     */
    fun none(): Traversal<S, E>? {
        asAdmin().bytecode.addStep(Symbols.none)
        return asAdmin().addStep(NoneStep(asAdmin()))
    }

    /**
     * Profile the traversal.
     *
     * @return the updated traversal with respective [ProfileSideEffectStep].
     */
    fun profile(): Traversal<S, TraversalMetrics?>? {
        asAdmin().bytecode.addStep(Symbols.profile)
        return asAdmin()
            .addStep(ProfileSideEffectStep(asAdmin(), ProfileSideEffectStep.DEFAULT_METRICS_KEY))
            .addStep(SideEffectCapStep<Object, TraversalMetrics>(asAdmin(), ProfileSideEffectStep.DEFAULT_METRICS_KEY))
    }

    /**
     * Return a [TraversalExplanation] that shows how this traversal will mutate with each applied [TraversalStrategy].
     *
     * @return a traversal explanation
     */
    fun explain(): TraversalExplanation? {
        if (asAdmin().isLocked) throw IllegalStateException("The traversal is locked and can not be explained on a strategy-by-strategy basis")
        return TraversalExplanation(asAdmin())
    }

    /**
     * A traversal can be rewritten such that its defined end type E may yield objects of a different type.
     * This helper method allows for the casting of the output to the known the type.
     *
     * @param endType  the true output type of the traversal
     * @param consumer a [Consumer] to process each output
     * @param <E2>     the known output type of the traversal
    </E2> */
    fun <E2> forEachRemaining(endType: Class<E2>?, consumer: Consumer<E2>) {
        try {
            while (true) {
                consumer.accept(next() as E2)
            }
        } catch (ignore: NoSuchElementException) {
        } finally {
            CloseableIterator.closeIterator(this)
        }
    }

    @Override
    fun forEachRemaining(action: Consumer<in E>) {
        try {
            while (true) {
                action.accept(next())
            }
        } catch (ignore: NoSuchElementException) {
        } finally {
            CloseableIterator.closeIterator(this)
        }
    }

    /**
     * Releases resources opened in any steps that implement [AutoCloseable]. If this method is overridden,the
     * implementer should invoke [.notifyClose].
     */
    @Override
    @Throws(Exception::class)
    fun close() {
        for (step in asAdmin().steps) {
            if (step is AutoCloseable) (step as AutoCloseable).close()
        }
        notifyClose()
    }

    /**
     * Gets a callback from [.close] for additional operations specific to the [Traversal] implementation.
     * A good implementation will use [.close] to release resources in steps and this method to release
     * resources specific to the [Traversal] implementations.
     */
    fun notifyClose() {
        // do nothing by default
    }

    /**
     * A collection of [Exception] types associated with Traversal execution.
     */
    object Exceptions {
        fun traversalIsLocked(): IllegalStateException {
            return IllegalStateException("The traversal strategies are complete and the traversal can no longer be modulated")
        }

        fun traversalIsNotReversible(): IllegalStateException {
            return IllegalStateException("The traversal is not reversible as it contains steps that are not reversible")
        }
    }

    interface Admin<S, E> : Traversal<S, E> {
        /**
         * Get the [Bytecode] associated with the construction of this traversal.
         *
         * @return the byte code representation of the traversal
         */
        val bytecode: Bytecode

        /**
         * Add an iterator of [Traverser.Admin] objects to the head/start of the traversal. Users should
         * typically not need to call this method. For dynamic inject of data, they should use [InjectStep].
         *
         * @param starts an iterators of traversers
         */
        fun addStarts(starts: Iterator<Traverser.Admin<S>?>?) {
            if (!isLocked) applyStrategies()
            startStep.addStarts(starts)
        }

        /**
         * Add a single [Traverser.Admin] object to the head of the traversal. Users should typically not need
         * to call this method. For dynamic inject of data, they should use [InjectStep].
         *
         * @param start a traverser to add to the traversal
         */
        fun addStart(start: Traverser.Admin<S>?) {
            if (!isLocked) applyStrategies()
            startStep.addStart(start)
        }

        /**
         * Get the [Step] instances associated with this traversal.
         * The steps are ordered according to their linked list structure as defined by [Step.getPreviousStep] and [Step.getNextStep].
         *
         * @return the ordered steps of the traversal
         */
        val steps: List<Any>

        /**
         * Add a [Step] to the end of the traversal. This method should link the step to its next and previous step accordingly.
         *
         * @param step the step to add
         * @param <E2> the output of the step
         * @return the updated traversal
         * @throws IllegalStateException if the [TraversalStrategies] have already been applied
        </E2> */
        @Throws(IllegalStateException::class)
        fun <E2> addStep(step: Step<*, E2>?): Admin<S, E2> {
            return this.addStep(steps.size(), step)
        }

        /**
         * Add a [Step] to an arbitrary point in the traversal.
         *
         * @param index the location in the traversal to insert the step
         * @param step  the step to add
         * @param <S2>  the new start type of the traversal (if the added step was a start step)
         * @param <E2>  the new end type of the traversal (if the added step was an end step)
         * @return the newly modulated traversal
         * @throws IllegalStateException if the [TraversalStrategies] have already been applied
        </E2></S2> */
        @Throws(IllegalStateException::class)
        fun <S2, E2> addStep(index: Int, step: Step<*, *>?): Admin<S2, E2>

        /**
         * Remove a [Step] from the traversal.
         *
         * @param step the step to remove
         * @param <S2> the new start type of the traversal (if the removed step was a start step)
         * @param <E2> the new end type of the traversal (if the removed step was an end step)
         * @return the newly modulated traversal
         * @throws IllegalStateException if the [TraversalStrategies] have already been applied
        </E2></S2> */
        @Throws(IllegalStateException::class)
        fun <S2, E2> removeStep(step: Step<*, *>?): Admin<S2, E2>? {
            return this.removeStep(TraversalHelper.stepIndex(step, this))
        }

        /**
         * Remove a [Step] from the traversal.
         *
         * @param index the location in the traversal of the step to be evicted
         * @param <S2>  the new start type of the traversal (if the removed step was a start step)
         * @param <E2>  the new end type of the traversal (if the removed step was an end step)
         * @return the newly modulated traversal
         * @throws IllegalStateException if the [TraversalStrategies] have already been applied
        </E2></S2> */
        @Throws(IllegalStateException::class)
        fun <S2, E2> removeStep(index: Int): Admin<S2, E2>?

        /**
         * Get the start/head of the traversal. If the traversal is empty, then an [EmptyStep] instance is returned.
         *
         * @return the start step of the traversal
         */
        val startStep: Step<S, *>?
            get() {
                val steps: List<Step> = steps
                return if (steps.isEmpty()) EmptyStep.instance() else steps[0]
            }

        /**
         * Get the end/tail of the traversal. If the traversal is empty, then an [EmptyStep] instance is returned.
         *
         * @return the end step of the traversal
         */
        val endStep: Step<*, E>?
            get() {
                val steps: List<Step> = steps
                return if (steps.isEmpty()) EmptyStep.instance() else steps[steps.size() - 1]
            }

        /**
         * Apply the registered [TraversalStrategies] to the traversal.
         * Once the strategies are applied, the traversal is "locked" and can no longer have steps added to it.
         * The order of operations for strategy applications should be: globally id steps, apply each strategy in turn
         * to root traversal, then recursively to nested traversals.
         *
         * @throws IllegalStateException if the [TraversalStrategies] have already been applied
         */
        @Throws(IllegalStateException::class)
        fun applyStrategies()

        /**
         * Get the [TraverserGenerator] associated with this traversal.
         * The traversal generator creates [Traverser] instances that are respective of the traversal's
         * [TraverserRequirement].
         *
         * @return the generator of traversers
         */
        val traverserGenerator: TraverserGenerator?

        /**
         * Gets a generator that creates new [TraverserSet] instances for steps in the traversal. Providers may
         * override this default implementation to provider their own [TraverserSet].
         */
        val traverserSetSupplier: Supplier<TraverserSet<S>?>?
            get() = TraverserSetSupplier.instance()

        /**
         * Get the set of all [TraverserRequirement]s for this traversal.
         *
         * @return the features of a traverser that are required to execute properly in this traversal
         */
        val traverserRequirements: Set<Any?>?

        /**
         * Call the [Step.reset] method on every step in the traversal.
         */
        fun reset() {
            steps.forEach(Step::reset)
        }
        /**
         * Get the [TraversalSideEffects] associated with the traversal. This method should not be called
         * externally for purposes of retrieving side-effects as traversal results. Traversal results should only be
         * returned by way of the execution of the traversal itself. Should a side-effect of a traversal be needed it
         * should only be obtained by using [GraphTraversal.cap] so that the side-effect can
         * be included as part of the traversal iteration. Relying on this method to get side-effects in these
         * situations may not result in consistent behavior across all types of executions and environments (e.g.
         * remoting).
         *
         * @return The traversal sideEffects
         */
        /**
         * Set the [TraversalSideEffects] of this traversal.
         *
         * @param sideEffects the sideEffects to set for this traversal.
         */
        var sideEffects: TraversalSideEffects?
        /**
         * Get the [TraversalStrategies] associated with this traversal.
         *
         * @return the strategies associated with this traversal
         */
        /**
         * Set the [TraversalStrategies] to be used by this traversal at evaluation time.
         *
         * @param strategies the strategies to use on this traversal
         */
        var strategies: TraversalStrategies?
        /**
         * Get the [TraversalParent] [Step] that is the parent of this traversal. Traversals can be nested
         * and this is the means by which the traversal tree is walked.
         *
         * @return the traversal holder parent step or [EmptyStep] if it has no parent.
         */
        /**
         * Set the [TraversalParent] [Step] that is the parent of this traversal. Traversals can be nested
         * and this is the means by which the traversal tree is connected. If there is no parent, then it should be a
         * [EmptyStep].
         *
         * @param step the traversal holder parent step or [EmptyStep] if it has no parent
         */
        var parent: TraversalParent?

        /**
         * Determines if the traversal is at the root level.
         */
        val isRoot: Boolean
            get() = parent is EmptyStep

        /**
         * Cloning is used to duplicate the traversal typically in OLAP environments.
         *
         * @return The cloned traversal
         */
        @SuppressWarnings("CloneDoesntDeclareCloneNotSupportedException")
        fun clone(): Admin<S, E>?

        /**
         * When the traversal has had its [TraversalStrategies] applied to it, it is locked.
         *
         * @return whether the traversal is locked
         */
        val isLocked: Boolean

        /**
         * Gets the [Graph] instance associated to this [Traversal].
         */
        var graph: Optional<Graph?>?

        /**
         * Gets the [TraversalSource] that spawned the [Traversal] instance initially if present. This
         * [TraversalSource] should have spawned from the associated [Graph] returned from
         * [.getGraph].
         */
        val traversalSource: Optional<TraversalSource?>?
            get() = Optional.empty()

        fun equals(other: Admin<S, E>): Boolean {
            if (this.getClass().equals(other.getClass())) {
                val steps: List<Step> = steps
                val otherSteps: List<Step> = other.steps
                if (steps.size() === otherSteps.size()) {
                    for (i in 0 until steps.size()) {
                        if (!steps[i].equals(otherSteps[i])) {
                            return false
                        }
                    }
                    return true
                }
            }
            return false
        }

        fun nextTraverser(): Traverser.Admin<E>? {
            return endStep.next()
        }
    }
}