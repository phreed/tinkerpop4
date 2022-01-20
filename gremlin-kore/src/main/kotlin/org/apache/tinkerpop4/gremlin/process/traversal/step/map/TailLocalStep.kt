/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.tinkerpop4.gremlin.process.traversal.step.map

import org.apache.tinkerpop4.gremlin.process.traversal.Path
import org.apache.tinkerpop4.gremlin.process.traversal.Traversal
import org.apache.tinkerpop4.gremlin.process.traversal.Traverser
import org.apache.tinkerpop4.gremlin.process.traversal.traverser.TraverserRequirement
import org.apache.tinkerpop4.gremlin.structure.util.StringFactory
import org.apache.tinkerpop4.gremlin.util.iterator.IteratorUtils
import java.util.Collection
import java.util.Collections
import java.util.Map
import java.util.Set

/**
 * @author Matt Frantz (http://github.com/mhfrantz)
 */
class TailLocalStep<S>(traversal: Traversal.Admin?, private val limit: Long) : ScalarMapStep<S, S>(traversal) {
    @Override
    protected fun map(traverser: Traverser.Admin<S>): S {
        // We may consider optimizing the iteration of these containers using subtype-specific interfaces.  For
        // example, we could use descendingIterator if we have a Deque.  But in general, we cannot reliably iterate a
        // collection in reverse, so we use the range algorithm with dynamically computed boundaries.
        val start: S = traverser.get()
        val high = if (start is Map) (start as Map).size()
            .toLong() else if (start is Collection) (start as Collection).size() else if (start is Path) (start as Path).size() else if (start is Iterable) IteratorUtils.count(
            start as Iterable
        ) else limit
        val low = high - limit
        return RangeLocalStep.applyRange(start, low, high)
    }

    @Override
    override fun toString(): String {
        return StringFactory.stepString(this, limit)
    }

    @Override
    override fun hashCode(): Int {
        return super.hashCode() xor Long.hashCode(limit)
    }

    @get:Override
    val requirements: Set<Any>
        get() = Collections.singleton(TraverserRequirement.OBJECT)
}