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
package org.apache.tinkerpop4.gremlin.process.traversal.traverser

import org.apache.tinkerpop4.gremlin.process.traversal.Step

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class B_O_S_SE_SL_Traverser<T> : B_O_Traverser<T> {
    protected var sack: Object? = null
    protected var loops: Short = 0 // an optimization hack to use a short internally to save bits :)
    protected var loopName: String? = null

    @kotlin.jvm.Transient
    protected var sideEffects: TraversalSideEffects? = null

    protected constructor() {}
    constructor(t: T, step: Step<T, *>, initialBulk: Long) : super(t, initialBulk) {
        sideEffects = step.getTraversal().getSideEffects()
        if (null != sideEffects.getSackInitialValue()) sack = sideEffects.getSackInitialValue().get()
    }

    /////////////////
    @Override
    fun <S> sack(): S? {
        return sack
    }

    @Override
    fun <S> sack(`object`: S) {
        sack = `object`
    }

    /////////////////
    @Override
    fun loops(): Int {
        return loops.toInt()
    }

    @Override
    fun loops(loopName: String?): Int {
        return if (loopName == null || this.loopName != null && this.loopName!!.equals(loopName)) loops.toInt() else throw IllegalArgumentException(
            "Loop name not defined: $loopName"
        )
    }

    @get:Override
    val loopNames: Set<String>
        get() = Collections.singleton(loopName)

    @Override
    fun initialiseLoops(stepLabel: String?, loopName: String?) {
        this.loopName = loopName
    }

    @Override
    fun incrLoops() {
        loops++
    }

    @Override
    fun resetLoops() {
        loops = 0
    }

    /////////////////
    @Override
    fun getSideEffects(): TraversalSideEffects? {
        return sideEffects
    }

    @Override
    fun setSideEffects(sideEffects: TraversalSideEffects?) {
        this.sideEffects = sideEffects
    }

    /////////////////
    @Override
    fun <R> split(r: R, step: Step<T, R>?): Traverser.Admin<R> {
        val clone = super.split(r, step) as B_O_S_SE_SL_Traverser<R>
        clone.sack =
            if (null == clone.sack) null else if (null == clone.sideEffects.getSackSplitter()) clone.sack else clone.sideEffects.getSackSplitter()
                .apply(clone.sack)
        return clone
    }

    @Override
    fun split(): Traverser.Admin<T> {
        val clone = super.split() as B_O_S_SE_SL_Traverser<T>
        clone.sack =
            if (null == clone.sack) null else if (null == clone.sideEffects.getSackSplitter()) clone.sack else clone.sideEffects.getSackSplitter()
                .apply(clone.sack)
        return clone
    }

    @Override
    fun merge(other: Traverser.Admin<*>) {
        super.merge(other)
        if (null != sack && null != sideEffects.getSackMerger()) sack =
            sideEffects.getSackMerger().apply(sack, other.sack())
    }

    /////////////////
    fun carriesUnmergeableSack(): Boolean {
        // hmmm... serialization in OLAP destroys the transient sideEffects
        return null != sack && (null == sideEffects || null == sideEffects.getSackMerger())
    }

    @Override
    override fun hashCode(): Int {
        return if (carriesUnmergeableSack()) System.identityHashCode(this) else super.hashCode() xor loops.toInt() xor Objects.hashCode(
            loopName
        )
    }

    protected fun equals(other: B_O_S_SE_SL_Traverser<*>): Boolean {
        return (super.equals(other) && other.loops == loops && Objects.equals(loopName, other.loopName)
                && !carriesUnmergeableSack())
    }

    @Override
    override fun equals(`object`: Object?): Boolean {
        return `object` is B_O_S_SE_SL_Traverser<*> && this.equals(`object` as B_O_S_SE_SL_Traverser<*>?)
    }
}