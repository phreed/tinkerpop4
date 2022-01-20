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

import org.apache.commons.collections.map.MultiValueMap
import org.apache.tinkerpop4.gremlin.process.traversal.Traverser
import org.apache.tinkerpop4.gremlin.structure.Vertex
import org.apache.tinkerpop4.gremlin.structure.util.Host
import java.util.Collection
import java.util.function.Function

/**
 * A [TraverserSet] that has an index back to the object in the [Traverser]. Using this extension of
 * [TraverserSet] can make it easier to find traversers within the set if the internal value is known. Without
 * the index the entire [TraverserSet] needs to be iterated to find a particular value.
 */
class IndexedTraverserSet<S, I>(indexingFunction: Function<S, I>, traverser: Traverser.Admin<S>?) :
    TraverserSet<S>(traverser) {
    private val index: MultiValueMap = MultiValueMap()
    private val indexingFunction: Function<S, I>

    constructor(indexingFunction: Function<S, I>) : this(indexingFunction, null) {}

    init {
        this.indexingFunction = indexingFunction
    }

    @Override
    fun clear() {
        index.clear()
        super.clear()
    }

    @Override
    fun add(traverser: Traverser.Admin<S>): Boolean {
        val newOne: Boolean = super.add(traverser)

        // if newly added then the traverser will be the same as the one passed in here to add().
        // if it is not, then it was merged to an existing traverser and the bulk would have
        // updated on that reference, thus only new stuff really needs to be added to the index
        if (newOne) index.put(indexingFunction.apply(traverser.get()), traverser)
        return newOne
    }

    /**
     * Gets a collection of [Traverser] objects that contain the specified value.
     *
     * @param k the key produced by the indexing function
     * @return
     */
    operator fun get(k: I): Collection<Traverser.Admin<S>>? {
        val c: Collection<Traverser.Admin<S>> = index.getCollection(k)

        // if remove() is called on this class, then the MultiValueMap *may* (javadoc wasn't clear
        // what the expectation was - used the word "typically") return an empty list if the last
        // item removed leaves the list empty. i think we want to enforce null for TraverserSet
        // semantics
        return if (c != null && c.isEmpty()) null else c
    }

    @Override
    fun offer(traverser: Traverser.Admin<S>): Boolean {
        return add(traverser)
    }

    @Override
    fun remove(): Traverser.Admin<S> {
        val removed: Traverser.Admin<S> = super.remove()
        index.remove(indexingFunction.apply(removed.get()), removed)
        return removed
    }

    @Override
    fun remove(traverser: Object): Boolean {
        if (traverser !is Traverser.Admin) throw IllegalArgumentException("The object to remove must be traverser")
        val removed: Boolean = super.remove(traverser)
        if (removed) index.remove(indexingFunction.apply((traverser as Traverser.Admin<S>).get()), traverser)
        return removed
    }

}