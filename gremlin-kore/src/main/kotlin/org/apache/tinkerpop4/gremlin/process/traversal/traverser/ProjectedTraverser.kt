/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.tinkerpop4.gremlin.process.traversal.traverser

import org.apache.tinkerpop4.gremlin.process.traversal.Path

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class ProjectedTraverser<T, P> : Traverser.Admin<T> {
    private var baseTraverser: Traverser.Admin<T>? = null
    var projections: List<P>? = null
        private set

    private constructor() {
        // for serialization
    }

    constructor(baseTraverser: Traverser.Admin<T>, projections: List<P>?) {
        this.baseTraverser = baseTraverser
        this.projections = projections
    }

    @Override
    fun merge(other: Admin<*>?) {
        baseTraverser.merge(other)
    }

    @Override
    fun <R> split(r: R, step: Step<T, R>?): Admin<R> {
        return ProjectedTraverser<T, P>(baseTraverser.split(r, step), projections)
    }

    @Override
    fun split(): Admin<T> {
        return ProjectedTraverser<T, P>(baseTraverser.split(), projections)
    }

    @Override
    fun addLabels(labels: Set<String?>?) {
        baseTraverser.addLabels(labels)
    }

    @Override
    fun keepLabels(labels: Set<String?>?) {
        baseTraverser.keepLabels(labels)
    }

    @Override
    fun dropLabels(labels: Set<String?>?) {
        baseTraverser.dropLabels(labels)
    }

    @Override
    fun dropPath() {
        baseTraverser.dropPath()
    }

    @Override
    fun set(t: T) {
        baseTraverser.set(t)
    }

    @Override
    fun initialiseLoops(stepLabel: String?, loopName: String?) {
        baseTraverser.initialiseLoops(stepLabel, loopName)
    }

    @Override
    fun incrLoops() {
        baseTraverser.incrLoops()
    }

    @Override
    fun resetLoops() {
        baseTraverser.resetLoops()
    }

    @get:Override
    @set:Override
    var stepId: String?
        get() = baseTraverser.getStepId()
        set(stepId) {
            baseTraverser.setStepId(stepId)
        }

    @Override
    fun setBulk(count: Long) {
        baseTraverser.setBulk(count)
    }

    @Override
    fun detach(): Admin<T> {
        baseTraverser = baseTraverser.detach()
        return this
    }

    @Override
    fun attach(method: Function<Attachable<T>?, T>?): T {
        return baseTraverser.attach(method)
    }

    @get:Override
    @set:Override
    var sideEffects: TraversalSideEffects
        get() = baseTraverser.getSideEffects()
        set(sideEffects) {
            baseTraverser.setSideEffects(sideEffects)
        }

    @get:Override
    val tags: Set<String>
        get() = baseTraverser.getTags()

    @Override
    fun get(): T {
        return baseTraverser.get()
    }

    @Override
    fun <S> sack(): S {
        return baseTraverser.sack()
    }

    @Override
    fun <S> sack(`object`: S) {
        baseTraverser.sack(`object`)
    }

    @Override
    fun path(): Path {
        return baseTraverser.path()
    }

    @Override
    fun loops(): Int {
        return baseTraverser.loops()
    }

    @Override
    fun loops(loopName: String?): Int {
        return baseTraverser.loops(loopName)
    }

    @Override
    fun bulk(): Long {
        return baseTraverser.bulk()
    }

    @Override
    override fun hashCode(): Int {
        return baseTraverser.hashCode()
    }

    @Override
    override fun equals(`object`: Object): Boolean {
        return `object` is ProjectedTraverser<*, *> && (`object` as ProjectedTraverser<*, *>).baseTraverser.equals(
            baseTraverser
        )
    }

    @Override
    override fun toString(): String {
        return baseTraverser.toString()
    }

    @Override
    fun clone(): ProjectedTraverser<T, P> {
        return try {
            val clone = super.clone() as ProjectedTraverser<T, P>
            clone.baseTraverser = baseTraverser.clone() as Traverser.Admin<T>
            clone
        } catch (e: CloneNotSupportedException) {
            throw IllegalStateException(e.getMessage(), e)
        }
    }

    companion object {
        fun <T> tryUnwrap(traverser: Traverser.Admin<T>): Traverser.Admin<T> {
            return if (traverser is ProjectedTraverser<*, *>) (traverser as ProjectedTraverser<*, *>).baseTraverser else traverser
        }
    }
}