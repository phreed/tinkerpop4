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

import org.apache.tinkerpop4.gremlin.process.traversal.step.util.EmptyStep
import org.apache.tinkerpop4.gremlin.process.traversal.step.util.ReducingBarrierStep
import org.apache.tinkerpop4.gremlin.process.traversal.traverser.TraverserRequirement
import java.io.Serializable
import java.util.Collections
import java.util.Iterator
import java.util.Set

/**
 * A [Step] denotes a unit of computation within a [Traversal].
 * A step takes an incoming object and yields an outgoing object.
 * Steps are chained together in a [Traversal] to yield a lazy function chain of computation.
 *
 *
 * In the constructor of a Step, never store explicit sideEffect objects in [TraversalSideEffects].
 * If a sideEffect needs to be registered with the [Traversal], use SideEffects.registerSupplier().
 *
 * @param <S> The incoming object type of the step
 * @param <E> The outgoing object type of the step
</E></S> */
interface Step<S, E> : Iterator<Traverser.Admin<E>?>, Serializable, Cloneable {
    /**
     * Add an iterator of [Traverser.Admin] objects of type S to the step.
     *
     * @param starts The iterator of objects to add
     */
    fun addStarts(starts: Iterator<Traverser.Admin<S>?>?)

    /**
     * Add a single [Traverser.Admin] to the step.
     *
     * @param start The traverser to add
     */
    fun addStart(start: Traverser.Admin<S>?)

    /**
     * Determines if starts objects are present without iterating forward. This function has special applicability
     * around [ReducingBarrierStep] implementations where they always return `true` for calls to
     * [.hasNext]. Using this function gives insight to what the step itself is holding in its iterator without
     * performing any sort of processing on the step itself.
     */
    fun hasStarts(): Boolean
    /**
     * Get the step prior to the current step.
     *
     * @return The previous step
     */
    /**
     * Set the step that is previous to the current step.
     * Used for linking steps together to form a function chain.
     *
     * @param step the previous step of this step
     */
    var previousStep: Step<*, S>?
    /**
     * Get the next step to the current step.
     *
     * @return The next step
     */
    /**
     * Set the step that is next to the current step.
     * Used for linking steps together to form a function chain.
     *
     * @param step the next step of this step
     */
    var nextStep: Step<E, *>?

    /**
     * Get the [Traversal.Admin] that this step is contained within.
     *
     * @param <A> The incoming object type of the traversal
     * @param <B> The outgoing object type of the traversal
     * @return The traversal of this step
    </B></A> */
    fun <A, B> getTraversal(): Traversal.Admin<A, B>?

    /**
     * Set the [Traversal] that this step is contained within.
     *
     * @param traversal the new traversal for this step
     */
    fun setTraversal(traversal: Traversal.Admin<*, *>?)

    /**
     * Reset the state of the step such that it has no incoming starts.
     * Internal states are to be reset, but any sideEffect data structures are not to be recreated.
     */
    fun reset()

    /**
     * Cloning is used to duplicate steps for the purpose of traversal optimization and OLTP replication.
     * When cloning a step, it is important that the steps, the cloned step is equivalent to the state of the step
     * when [.reset] is called. Moreover, the previous and next steps should be set to [EmptyStep].
     *
     * @return The cloned step
     */
    @SuppressWarnings("CloneDoesntDeclareCloneNotSupportedException")
    fun clone(): Step<S, E>?

    /**
     * Get the labels of this step.
     * The labels are ordered by the order of the calls to [Step.addLabel].
     *
     * @return the set of labels for this step
     */
    val labels: Set<String?>?

    /**
     * Add a label to this step.
     *
     * @param label the label to add to this step
     */
    fun addLabel(label: String?)

    /**
     * Remove a label from this step.
     *
     * @param label the label to remove from this step
     */
    fun removeLabel(label: String?)
    /**
     * Get the unique id of this step.
     *
     * @return the unique id of the step
     */
    /**
     * Get the unique id of the step.
     * These ids can change when strategies are applied and anonymous traversals are embedded in the parent traversal.
     * A developer should typically not need to call this method.
     *
     * @param id the unique id of the step
     */
    var id: String?

    /**
     * Provide the necessary [TraverserRequirement] that must be met by the traverser in order for the step to
     * function properly. The provided default implements returns an empty set.
     *
     * @return the set of requirements
     */
    val requirements: Set<Any?>?
        get() = Collections.emptySet()

    /**
     * Compare the current step with another step.
     *
     * @param other      the other step
     * @param compareIds whether to compare step IDs or not
     * @return true if the steps are equal, otherwise false
     */
    fun equals(other: Step<*, *>?, compareIds: Boolean): Boolean {
        return (!compareIds || other != null && id!!.equals(other.id)) && this.equals(other)
    }
}