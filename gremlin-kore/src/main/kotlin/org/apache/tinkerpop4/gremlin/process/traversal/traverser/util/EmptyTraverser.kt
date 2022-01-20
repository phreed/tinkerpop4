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
package org.apache.tinkerpop4.gremlin.process.traversal.traverser.util

import org.apache.tinkerpop4.gremlin.process.traversal.Path

/**
 * A [Traverser] with no bulk which effectively means that it will no longer be propagated through a
 * [Traversal].
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class EmptyTraverser<T> private constructor() : Traverser<T>, Traverser.Admin<T> {
    @Override
    fun addLabels(labels: Set<String?>?) {
    }

    @Override
    fun keepLabels(labels: Set<String?>?) {
    }

    @Override
    fun dropLabels(labels: Set<String?>?) {
    }

    @Override
    fun dropPath() {
    }

    @Override
    fun set(t: T) {
    }

    @Override
    fun initialiseLoops(stepLabel: String?, loopNam: String?) {
    }

    @Override
    fun incrLoops() {
    }

    @Override
    fun resetLoops() {
    }

    @get:Override
    @set:Override
    var stepId: String?
        get() = HALT
        set(stepId) {}

    @Override
    fun setBulk(count: Long) {
    }

    @Override
    fun <R> split(r: R, step: Step<T, R>?): Admin<R> {
        return INSTANCE
    }

    @Override
    fun split(): Admin<T> {
        return this
    }

    @Override
    fun detach(): Admin<T> {
        return this
    }

    @Override
    fun attach(method: Function<Attachable<T>?, T>?): T? {
        return null
    }

    @Override
    fun get(): T? {
        return null
    }

    @Override
    fun <S> sack(): S? {
        return null
    }

    @Override
    fun <S> sack(`object`: S) {
    }

    @Override
    fun merge(other: Traverser.Admin<*>?) {
    }

    @Override
    fun path(): Path {
        return EmptyPath.instance()
    }

    @Override
    fun loops(): Int {
        return 0
    }

    @Override
    fun loops(loopName: String?): Int {
        return 0
    }

    @Override
    fun bulk(): Long {
        return 0L
    }

    @get:Override
    @set:Override
    var sideEffects: TraversalSideEffects?
        get() = null
        set(sideEffects) {}

    @get:Override
    val tags: Set<String>
        get() = Collections.emptySet()

    @Override
    override fun hashCode(): Int {
        return 380473707
    }

    @Override
    override fun equals(`object`: Object?): Boolean {
        return `object` is EmptyTraverser<*>
    }

    @Override
    @SuppressWarnings("CloneDoesntCallSuperClone")
    fun clone(): EmptyTraverser<T> {
        return this
    }

    companion object {
        private val INSTANCE: EmptyTraverser<*> = EmptyTraverser<Any?>()

        /**
         * The empty [Traverser] instance.
         */
        fun <R> instance(): EmptyTraverser<R> {
            return INSTANCE
        }
    }
}