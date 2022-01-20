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

import org.apache.tinkerpop4.gremlin.process.traversal.Path

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class B_LP_O_P_S_SE_SL_Traverser<T> : B_O_S_SE_SL_Traverser<T> {
    protected var path: Path? = null

    protected constructor() {}
    constructor(t: T, step: Step<T, *>, initialBulk: Long) : super(t, step, initialBulk) {
        path = ImmutablePath.make().extend(t, step.getLabels())
    }

    /////////////////
    @Override
    fun path(): Path? {
        return path
    }

    /////////////////
    @Override
    fun detach(): Traverser.Admin<T> {
        super.detach()
        path = ReferenceFactory.detach(path)
        return this
    }

    /////////////////
    @Override
    fun <R> split(r: R, step: Step<T, R>): Traverser.Admin<R> {
        val clone = super.split(r, step) as B_LP_O_P_S_SE_SL_Traverser<R>
        clone.path = clone.path.clone().extend(r, step.getLabels())
        return clone
    }

    @Override
    fun split(): Traverser.Admin<T> {
        val clone = super.split() as B_LP_O_P_S_SE_SL_Traverser<T>
        clone.path = clone.path.clone()
        return clone
    }

    @Override
    fun addLabels(labels: Set<String?>?) {
        path = path.extend(labels)
    }

    @Override
    override fun hashCode(): Int {
        return if (carriesUnmergeableSack()) System.identityHashCode(this) else super.hashCode() xor path.hashCode()
    }

    protected fun equals(other: B_LP_O_P_S_SE_SL_Traverser<*>): Boolean {
        return super.equals(other) && other.path.equals(path)
    }

    @Override
    override fun equals(`object`: Object?): Boolean {
        return `object` is B_LP_O_P_S_SE_SL_Traverser<*> && this.equals(`object` as B_LP_O_P_S_SE_SL_Traverser<*>?)
    }
}