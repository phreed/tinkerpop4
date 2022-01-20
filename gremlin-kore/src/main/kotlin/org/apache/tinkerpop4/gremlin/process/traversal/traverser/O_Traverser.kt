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
abstract class O_Traverser<T> : AbstractTraverser<T> {
    protected var tags: Set<String>? = null

    protected constructor() {}
    constructor(t: T) : super(t) {}

    fun getTags(): Set<String>? {
        if (null == tags) tags = HashSet()
        return tags
    }

    @Override
    fun <R> split(r: R, step: Step<T, R>?): Admin<R> {
        val clone = super.split(r, step) as O_Traverser<R>
        if (null != tags) clone.tags = HashSet(tags)
        return clone
    }

    @Override
    fun split(): Admin<T> {
        val clone = super.split() as O_Traverser<T>
        if (null != tags) clone.tags = HashSet(tags)
        return clone
    }

    @Override
    fun merge(other: Traverser.Admin<*>) {
        if (!other.getTags().isEmpty()) {
            if (tags == null) tags = HashSet()
            tags.addAll(other.getTags())
        }
    }

    @Override
    override fun hashCode(): Int {
        return super.hashCode()
    }

    protected fun equals(other: O_Traverser<*>): Boolean {
        return super.equals(other) && Objects.equals(tags, other.tags)
    }

    @Override
    override fun equals(`object`: Object?): Boolean {
        return `object` is O_Traverser<*> && this.equals(`object` as O_Traverser<*>?)
    }
}