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
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
abstract class AbstractTraverser<T> : Traverser<T>, Traverser.Admin<T> {
    protected var t: T? = null

    protected constructor() {}
    constructor(t: T) {
        this.t = t
    }

    /////////////
    @Override
    fun merge(other: Admin<*>?) {
        throw UnsupportedOperationException(
            "This traverser does not support merging: " + this.getClass().getCanonicalName()
        )
    }

    @Override
    fun <R> split(r: R, step: Step<T, R>?): Admin<R> {
        return try {
            val clone = super.clone() as AbstractTraverser<R>
            clone.t = r
            clone
        } catch (e: CloneNotSupportedException) {
            throw IllegalStateException(e.getMessage(), e)
        }
    }

    @Override
    fun split(): Admin<T> {
        return try {
            super.clone() as AbstractTraverser<T>
        } catch (e: CloneNotSupportedException) {
            throw IllegalStateException(e.getMessage(), e)
        }
    }

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
        this.t = t
    }

    @Override
    fun initialiseLoops(stepLabel: String?, loopName: String?) {
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
        get() {
            throw UnsupportedOperationException(
                "This traverser does not support futures: " + this.getClass().getCanonicalName()
            )
        }
        set(stepId) {}

    @Override
    fun setBulk(count: Long) {
    }

    @Override
    fun detach(): Admin<T> {
        t = ReferenceFactory.detach(t)
        return this
    }

    @Override
    fun attach(method: Function<Attachable<T>?, T>?): T {
        // you do not want to attach a path because it will reference graph objects not at the current vertex
        if (t is Attachable && (t as Attachable).get() !is Path) t = (t as Attachable<T>).attach(method)
        return t
    }

    //throw new UnsupportedOperationException("This traverser does not support sideEffects: " + this.getClass().getCanonicalName());
    @get:Override
    @set:Override
    var sideEffects: TraversalSideEffects
        get() = EmptyTraversalSideEffects.instance()
        //throw new UnsupportedOperationException("This traverser does not support sideEffects: " + this.getClass().getCanonicalName());
        set(sideEffects) {}

    @Override
    fun get(): T {
        return t
    }

    @Override
    fun <S> sack(): S {
        throw UnsupportedOperationException(
            "This traverser does not support sacks: " + this.getClass().getCanonicalName()
        )
    }

    @Override
    fun <S> sack(`object`: S) {
    }

    @Override
    fun path(): Path {
        return EmptyPath.instance()
    }

    @Override
    fun loops(): Int {
        throw UnsupportedOperationException(
            "This traverser does not support loops: " + this.getClass().getCanonicalName()
        )
    }

    @Override
    fun loops(loopName: String?): Int {
        throw UnsupportedOperationException(
            "This traverser does not support named loops: " + this.getClass().getCanonicalName()
        )
    }

    @Override
    fun bulk(): Long {
        return 1L
    }

    @Override
    @SuppressWarnings("CloneDoesntDeclareCloneNotSupportedException")
    fun clone(): AbstractTraverser<T> {
        return try {
            super.clone() as AbstractTraverser<T>
        } catch (e: CloneNotSupportedException) {
            throw IllegalStateException(e.getMessage(), e)
        }
    }

    ///////////
    @Override
    override fun hashCode(): Int {
        return Objects.hashCode(t)
    }

    @Override
    override fun equals(`object`: Object): Boolean {
        return `object` is AbstractTraverser<*> && Objects.equals(t, (`object` as AbstractTraverser<*>).t)
    }

    @Override
    override fun toString(): String {
        return Objects.toString(t)
    }
}