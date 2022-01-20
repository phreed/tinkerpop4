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

import org.apache.tinkerpop4.gremlin.process.traversal.step.map.LoopsStep

/**
 * A `Traverser` represents the current state of an object flowing through a [Traversal].
 * A traverser maintains a reference to the current object, a traverser-local "sack", a traversal-global sideEffect,
 * a bulk count, and a path history.
 *
 *
 * Different types of traversers can exist depending on the semantics of the traversal and the desire for
 * space/time optimizations of the developer.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
interface Traverser<T> : Serializable, Comparable<Traverser<T>?>, Cloneable {
    /**
     * Get the object that the traverser is current at.
     *
     * @return The current object of the traverser
     */
    fun get(): T

    /**
     * Get the sack local sack object of this traverser.
     *
     * @param <S> the type of the sack object
     * @return the sack object
    </S> */
    fun <S> sack(): S

    /**
     * Set the traversers sack object to the provided value ("sack the value").
     *
     * @param object the new value of the traverser's sack
     * @param <S>    the type of the object
    </S> */
    fun <S> sack(`object`: S)

    /**
     * Get the current path of the traverser.
     *
     * @return The path of the traverser
     */
    fun path(): Path

    /**
     * Get the object associated with the specified step-label in the traverser's path history.
     *
     * @param stepLabel the step-label in the path to access
     * @param <A>       the type of the object
     * @return the object associated with that path label (if more than one object occurs at that step, a list is returned)
    </A> */
    fun <A> path(stepLabel: String?): A {
        return this.path().get(stepLabel)
    }

    fun <A> path(pop: Pop?, stepLabel: String?): A {
        return this.path().get(pop, stepLabel)
    }

    /**
     * Return the number of times the traverser has gone through a looping section of a traversal.
     *
     * @return The number of times the traverser has gone through a loop
     */
    fun loops(): Int

    /**
     * Return the number of times the traverser has gone through the named looping section of a traversal.
     *
     * @param loopName the name applied to the loop or null for the containing loop
     * @return The number of times the traverser has gone through a loop
     * @throws IllegalArgumentException if the loopName is not defined
     */
    fun loops(loopName: String?): Int

    /**
     * A traverser may represent a grouping of traversers to allow for more efficient data propagation.
     *
     * @return the number of traversers represented in this traverser.
     */
    fun bulk(): Long

    /**
     * Get a particular value from the side-effects of the traverser (thus, traversal).
     *
     * @param sideEffectKey the key of the value to get from the sideEffects
     * @param <A>           the type of the returned object
     * @return the object in the sideEffects of the respective key
    </A> */
    @Throws(IllegalArgumentException::class)
    fun <A> sideEffects(sideEffectKey: String): A {
        return asAdmin().sideEffects.< A > get < A ? > sideEffectKey
    }

    /**
     * Add a particular value to the respective side-effect of the traverser (thus, traversal).
     *
     * @param sideEffectKey   the key of the value to set int the sideEffects
     * @param sideEffectValue the value to set for the sideEffect key
     */
    @Throws(IllegalArgumentException::class)
    fun sideEffects(sideEffectKey: String?, sideEffectValue: Object?) {
        asAdmin().sideEffects.add(sideEffectKey, sideEffectValue)
    }

    /**
     * If the underlying object of the traverser is comparable, compare it with the other traverser.
     *
     * @param other the other traverser that presumably has a comparable internal object
     * @return the comparison of the two objects of the traversers
     * @throws ClassCastException if the object of the traverser is not comparable
     */
    @Override
    @Throws(ClassCastException::class)
    operator fun compareTo(other: Traverser<T>): Int {
        val thisObj: Object? = get()
        val otherObj: Object? = other.get()
        if (thisObj === otherObj) return 0
        if (null == thisObj) return -1
        return if (null == otherObj) 1 else (thisObj as Comparable).compareTo(otherObj)
    }

    /**
     * Typecast the traverser to a "system traverser" so [Traverser.Admin] methods can be accessed.
     * This is used as a helper method to avoid the awkwardness of `((Traverser.Administrative)traverser)`.
     * The default implementation simply returns "this" type casted to [Traverser.Admin].
     *
     * @return The type-casted traverser
     */
    fun asAdmin(): Admin<T> {
        return this as Admin<T>
    }

    /**
     * Traverser cloning is important when splitting a traverser at a bifurcation point in a traversal.
     */
    @SuppressWarnings("CloneDoesntDeclareCloneNotSupportedException")
    fun clone(): Traverser<T>?

    /**
     * The methods in System.Traverser are useful to underlying Step and Traversal implementations.
     * They should not be accessed by the user during lambda-based manipulations.
     */
    interface Admin<T> : Traverser<T>, Attachable<T> {
        /**
         * When two traversers are have equality with each other, then they can be merged.
         * This method is used to merge the traversers into a single traverser.
         * This is used for optimization where instead of enumerating all traversers, they can be counted.
         *
         * @param other the other traverser to merge into this traverser. Once merged, the other can be garbage collected.
         */
        fun merge(other: Admin<*>?)

        /**
         * Generate a child traverser of the current traverser for current as step and new object location.
         * The child has the path history, future, and loop information of the parent.
         * The child extends that path history with the current as and provided R-object.
         *
         * @param r    The current object of the child
         * @param step The step yielding the split
         * @param <R>  The current object type of the child
         * @return The split traverser
        </R> */
        fun <R> split(r: R, step: Step<T, R>?): Admin<R>?

        /**
         * Generate a sibling traverser of the current traverser with a full copy of all state within the sibling.
         *
         * @return The split traverser
         */
        fun split(): Admin<T>?
        fun addLabels(labels: Set<String?>?)

        /**
         * Drop all path information not associated with specified labels.
         * This is an optimization method that allows a traverser to save memory and increase the likelihood of bulking.
         *
         * @param labels the labels to keep path information for.
         */
        fun keepLabels(labels: Set<String?>?)

        /**
         * Drop all path information associated with specified labels.
         * This is an optimization method that allows a traverser to save memory and increase the likelihood of bulking.
         *
         * @param labels the labels to drop path information for.
         */
        fun dropLabels(labels: Set<String?>?)

        /**
         * Drop the path of the traverser.
         * This is an optimization method that allows a traverser to save memory and increase the likelihood of bulking.
         */
        fun dropPath()

        /**
         * Set the current object location of the traverser.
         *
         * @param t The current object of the traverser
         */
        fun set(t: T)

        /**
         * Initialise a loop by setting up the looping construct.
         * The step label is important to create a stack of loop counters when within a nested context.
         * If the provided label is not the same as the current label on the stack, add a new loop counter.
         * The loopName can be used to refer to the loops counter via the [LoopsStep]
         *
         * @param stepLabel the label of the step that is being set-up.
         * @param loopName the user defined name for referencing the loop counter or null if not set
         */
        fun initialiseLoops(stepLabel: String?, loopName: String?)

        /**
         * Increment the number of times the traverser has gone through a looping section of traversal.
         */
        fun incrLoops()

        /**
         * Set the number of times the traverser has gone through a loop back to 0.
         * When a traverser exits a looping construct, this method should be called.
         * In a nested loop context, the highest stack loop counter should be removed.
         */
        fun resetLoops()
        /**
         * Get the step id of where the traverser is located.
         * This is typically used in multi-machine systems that require the movement of
         * traversers between different traversal instances.
         *
         * @return The future step for the traverser
         */
        /**
         * Set the step id of where the traverser is located.
         * If the future is [Traverser.Admin.HALT], then [Traverser.Admin.isHalted] is true.
         *
         * @param stepId The future step of the traverser
         */
        var stepId: String?

        /**
         * If the traverser has "no future" then it is done with its lifecycle.
         * This does not mean that the traverser is "dead," only that it has successfully passed through a
         * [Traversal].
         *
         * @return Whether the traverser is done executing or not
         */
        val isHalted: Boolean
            get() = stepId!!.equals(HALT)

        /**
         * Set the number of traversers represented by this traverser.
         *
         * @param count the number of traversers
         */
        fun setBulk(count: Long)

        /**
         * Prepare the traverser for migration across a JVM boundary.
         *
         * @return The deflated traverser
         */
        fun detach(): Admin<T>?

        /**
         * Regenerate the detached traverser given its location at a particular vertex.
         *
         * @param method The method by which to attach a `Traverser` to an vertex.
         * @return The inflated traverser
         */
        @Override
        fun attach(method: Function<Attachable<T>?, T>?): T
        /**
         * Get the sideEffects associated with the traversal of the traverser.
         *
         * @return the traversal sideEffects of the traverser
         */
        /**
         * Set the sideEffects of the [Traversal]. Given that traversers can move between machines,
         * it may be important to re-set this when the traverser crosses machine boundaries.
         *
         * @param sideEffects the sideEffects of the traversal.
         */
        var sideEffects: TraversalSideEffects

        /**
         * Get the tags associated with the traverser.
         * Tags are used to categorize historic behavior of a traverser.
         * The returned set is mutable.
         *
         * @return the set of tags associated with the traverser.
         */
        val tags: Set<String?>?

        /**
         * If this traverser supports loops then return the loop names if any.
         */
        val loopNames: Set<String?>?
            get() = Collections.emptySet()

        companion object {
            const val HALT = "halt"
        }
    }
}