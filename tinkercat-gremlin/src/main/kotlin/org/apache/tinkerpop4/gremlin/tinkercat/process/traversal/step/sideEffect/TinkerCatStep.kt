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
package org.apache.tinkerpop4.gremlin.tinkercat.process.traversal.step.sideEffect

import org.apache.tinkerpop4.gremlin.process.traversal.step.map.GraphStep
import org.apache.tinkerpop4.gremlin.process.traversal.step.HasContainerHolder
import java.lang.AutoCloseable
import org.apache.tinkerpop4.gremlin.process.traversal.step.util.HasContainer
import org.apache.tinkerpop4.gremlin.process.traversal.Compare
import org.apache.tinkerpop4.gremlin.structure.util.StringFactory
import org.apache.tinkerpop4.gremlin.structure.util.CloseableIterator
import org.apache.tinkerpop4.gremlin.process.traversal.util.AndP
import org.apache.tinkerpop4.gremlin.structure.Edge
import org.apache.tinkerpop4.gremlin.structure.Element
import org.apache.tinkerpop4.gremlin.structure.Vertex
import org.apache.tinkerpop4.gremlin.tinkercat.structure.*
import org.apache.tinkerpop4.gremlin.util.iterator.IteratorUtils
import java.util.*
import java.util.function.Consumer
import java.util.stream.Collectors

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Pieter Martin
 */
class TinkerCatStep<S, E : Element?>(originalGraphStep: GraphStep<*, *>) : GraphStep<S, E>(
    originalGraphStep.getTraversal<Any, Any>(),
    originalGraphStep.returnClass as Class<E>?,
    originalGraphStep.isStartStep,
    *originalGraphStep.ids
), HasContainerHolder, AutoCloseable {
    private val hasContainers: MutableList<HasContainer> = ArrayList()

    /**
     * List of iterators opened by this step.
     */
    private val iterators: MutableList<Iterator<*>> = ArrayList()

    init {
        originalGraphStep.labels.forEach(Consumer { label: String? -> addLabel(label) })

        // we used to only setIteratorSupplier() if there were no ids OR the first id was instanceof Element,
        // but that allowed the filter in g.V(v).has('k','v') to be ignored.  this created problems for
        // PartitionStrategy which wants to prevent someone from passing "v" from one TraversalSource to
        // another TraversalSource using a different partition
        setIteratorSupplier {
            when {
                Vertex::class.java.isAssignableFrom(returnClass) -> vertices()
                else -> edges()
            } as Iterator<E>
        }
    }

    private fun edges(): Iterator<Edge> {
        val graph = getTraversal<Any, Any>().graph.get() as TinkerCat
        val indexedContainer = getIndexKey(Edge::class.java)
        // ids are present, filter on them first
        val iterator = when {
            (null == ids) -> Collections.emptyIterator()
            ids.isNotEmpty() -> iteratorList(graph.edges(*ids))
            (null == indexedContainer) -> iteratorList(graph.edges())
            else ->
                TinkerHelper.queryEdgeIndex(graph, indexedContainer.key, indexedContainer.predicate.value).stream()
                    .filter { edge: TinkerEdge? -> HasContainer.testAll(edge, hasContainers) }
                    .collect(Collectors.toList<Edge>()).iterator()
        }
        iterators.add(iterator)
        return iterator
    }

    private fun vertices(): Iterator<Vertex> {
        val graph = getTraversal<Any, Any>().graph.get() as TinkerCat
        val indexedContainer = getIndexKey(Vertex::class.java)
        // ids are present, filter on them first
        val iterator = if (null == ids) Collections.emptyIterator() else if (ids.size > 0) iteratorList(graph.vertices(*ids)) else if (null == indexedContainer) iteratorList(
                graph.vertices()
            ) else IteratorUtils.filter(
                TinkerHelper.queryVertexIndex(graph, indexedContainer.key, indexedContainer.predicate.value).iterator()
            ) { vertex: TinkerVertex? -> HasContainer.testAll(vertex, hasContainers) }
        iterators.add(iterator)
        return iterator
    }

    private fun getIndexKey(indexedClass: Class<out Element>): HasContainer? {
        val indexedKeys = (getTraversal<Any, Any>().graph.get() as TinkerCat).getIndexedKeys(indexedClass)
        val itty = IteratorUtils.filter(
            hasContainers.iterator()
        ) { c: HasContainer -> c.predicate.biPredicate === Compare.eq && indexedKeys.contains(c.key) }
        return if (itty.hasNext()) itty.next() else null
    }

    override fun toString(): String {
        return if (hasContainers.isEmpty()) super.toString() else if (null == ids || ids.isEmpty()) StringFactory.stepString(
            this,
            returnClass.simpleName.lowercase(Locale.getDefault()),
            hasContainers
        ) else StringFactory.stepString(
            this,
            returnClass.simpleName.lowercase(Locale.getDefault()),
            Arrays.toString(ids),
            hasContainers
        )
    }

    private fun <E : Element?> iteratorList(iterator: Iterator<E>): Iterator<E> {
        val list: MutableList<E> = ArrayList()
        while (iterator.hasNext()) {
            val e = iterator.next()
            if (HasContainer.testAll(e, hasContainers)) list.add(e)
        }

        // close the old iterator to release resources since we are returning a new iterator (over list)
        // out of this function.
        CloseableIterator.closeIterator(iterator)
        return TinkerCatIterator(list.iterator())
    }

    override fun getHasContainers(): List<HasContainer> {
        return Collections.unmodifiableList(hasContainers)
    }

    override fun addHasContainer(hasContainer: HasContainer) {
        if (hasContainer.predicate is AndP<*>) {
            for (predicate in (hasContainer.predicate as AndP<*>).predicates) {
                addHasContainer(HasContainer(hasContainer.key, predicate))
            }
        } else hasContainers.add(hasContainer)
    }

    override fun hashCode(): Int {
        return super.hashCode() xor hasContainers.hashCode()
    }

    override fun close() {
        iterators.forEach(Consumer { iterator: Iterator<*>? -> CloseableIterator.closeIterator(iterator) })
    }

    override fun remove() {
        TODO("Not yet implemented")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as TinkerCatStep<*, *>

        if (hasContainers != other.hasContainers) return false
        if (iterators != other.iterators) return false

        return true
    }
}