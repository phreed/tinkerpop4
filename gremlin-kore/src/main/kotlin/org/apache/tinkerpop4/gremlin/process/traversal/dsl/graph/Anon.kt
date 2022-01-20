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
package org.apache.tinkerpop4.gremlin.process.traversal.dsl.graph

import org.apache.tinkerpop4.gremlin.process.traversal.P
import org.apache.tinkerpop4.gremlin.process.traversal.Path
import org.apache.tinkerpop4.gremlin.process.traversal.Pop
import org.apache.tinkerpop4.gremlin.process.traversal.Scope
import org.apache.tinkerpop4.gremlin.process.traversal.Traversal
import org.apache.tinkerpop4.gremlin.process.traversal.Traverser
import org.apache.tinkerpop4.gremlin.process.traversal.step.util.Tree
import org.apache.tinkerpop4.gremlin.process.traversal.traverser.util.TraverserSet
import org.apache.tinkerpop4.gremlin.structure.Column
import org.apache.tinkerpop4.gremlin.structure.Direction
import org.apache.tinkerpop4.gremlin.structure.Edge
import org.apache.tinkerpop4.gremlin.structure.Element
import org.apache.tinkerpop4.gremlin.structure.Property
import org.apache.tinkerpop4.gremlin.structure.T
import org.apache.tinkerpop4.gremlin.structure.Vertex
import org.apache.tinkerpop4.gremlin.structure.VertexProperty
import java.util.Collection
import java.util.Iterator
import java.util.List
import java.util.Map
import java.util.function.BiFunction
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Predicate

/**
 * An anonymous [GraphTraversal].
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class Anon {
    protected constructor() {}

    companion object {
        fun <A> start(): GraphTraversal<A, A> {
            return DefaultGraphTraversal()
        }

        constructor(vararg starts: A?) {
            return inject(*starts)
        }
        ///////////////////// MAP STEPS /////////////////////
        /**
         * @see GraphTraversal.map
         */
        fun <A, B> map(function: Function<Traverser<A>?, B>?): GraphTraversal<A, B> {
            return start<A>().map(function)
        }

        /**
         * @see GraphTraversal.map
         */
        fun <A, B> map(mapTraversal: Traversal<*, B>?): GraphTraversal<A, B> {
            return start<A>().map(mapTraversal)
        }

        /**
         * @see GraphTraversal.flatMap
         */
        fun <A, B> flatMap(function: Function<Traverser<A>?, Iterator<B>?>?): GraphTraversal<A, B> {
            return start<A>().flatMap(function)
        }

        /**
         * @see GraphTraversal.flatMap
         */
        fun <A, B> flatMap(flatMapTraversal: Traversal<*, B>?): GraphTraversal<A, B> {
            return start<A>().flatMap(flatMapTraversal)
        }

        /**
         * @see GraphTraversal.identity
         */
        fun <A> identity(): GraphTraversal<A, A> {
            return start<A>().identity()
        }

        /**
         * @see GraphTraversal.constant
         */
        fun <A> constant(a: A): GraphTraversal<A, A> {
            return start<A>().constant(a)
        }

        /**
         * @see GraphTraversal.label
         */
        fun <A : Element?> label(): GraphTraversal<A, String> {
            return start<A>().label()
        }

        /**
         * @see GraphTraversal.id
         */
        fun <A : Element?> id(): GraphTraversal<A, Object> {
            return start<A>().id()
        }

        /**
         * @see GraphTraversal.V
         */
        fun <A> V(vararg vertexIdsOrElements: Object?): GraphTraversal<A, Vertex> {
            return start<A>().V(vertexIdsOrElements)
        }

        /**
         * @see GraphTraversal.to
         */
        fun to(direction: Direction?, vararg edgeLabels: String?): GraphTraversal<Vertex, Vertex> {
            return start<Vertex>().to(direction, edgeLabels)
        }

        /**
         * @see GraphTraversal.out
         */
        fun out(vararg edgeLabels: String?): GraphTraversal<Vertex, Vertex> {
            return start<Vertex>().out(edgeLabels)
        }

        /**
         * @see GraphTraversal. in
         */
        fun `in`(vararg edgeLabels: String?): GraphTraversal<Vertex, Vertex> {
            return start<Vertex>().`in`(edgeLabels)
        }

        /**
         * @see GraphTraversal.both
         */
        fun both(vararg edgeLabels: String?): GraphTraversal<Vertex, Vertex> {
            return start<Vertex>().both(edgeLabels)
        }

        /**
         * @see GraphTraversal.toE
         */
        fun toE(direction: Direction?, vararg edgeLabels: String?): GraphTraversal<Vertex, Edge> {
            return start<Vertex>().toE(direction, edgeLabels)
        }

        /**
         * @see GraphTraversal.outE
         */
        fun outE(vararg edgeLabels: String?): GraphTraversal<Vertex, Edge> {
            return start<Vertex>().outE(edgeLabels)
        }

        /**
         * @see GraphTraversal.inE
         */
        fun inE(vararg edgeLabels: String?): GraphTraversal<Vertex, Edge> {
            return start<Vertex>().inE(edgeLabels)
        }

        /**
         * @see GraphTraversal.bothE
         */
        fun bothE(vararg edgeLabels: String?): GraphTraversal<Vertex, Edge> {
            return start<Vertex>().bothE(edgeLabels)
        }

        /**
         * @see GraphTraversal.toV
         */
        fun toV(direction: Direction?): GraphTraversal<Edge, Vertex> {
            return start<Edge>().toV(direction)
        }

        /**
         * @see GraphTraversal.inV
         */
        fun inV(): GraphTraversal<Edge, Vertex> {
            return start<Edge>().inV()
        }

        /**
         * @see GraphTraversal.outV
         */
        fun outV(): GraphTraversal<Edge, Vertex> {
            return start<Edge>().outV()
        }

        /**
         * @see GraphTraversal.bothV
         */
        fun bothV(): GraphTraversal<Edge, Vertex> {
            return start<Edge>().bothV()
        }

        /**
         * @see GraphTraversal.otherV
         */
        fun otherV(): GraphTraversal<Edge, Vertex> {
            return start<Edge>().otherV()
        }

        /**
         * @see GraphTraversal.order
         */
        fun <A> order(): GraphTraversal<A, A> {
            return start<A>().order()
        }

        /**
         * @see GraphTraversal.order
         */
        fun <A> order(scope: Scope?): GraphTraversal<A, A> {
            return start<A>().order(scope)
        }

        /**
         * @see GraphTraversal.properties
         */
        fun <A : Element?, B> properties(vararg propertyKeys: String?): GraphTraversal<A, out Property<B>?> {
            return start<A>().< B > properties < B ? > propertyKeys
        }

        /**
         * @see GraphTraversal.values
         */
        fun <A : Element?, B> values(vararg propertyKeys: String?): GraphTraversal<A, B> {
            return start<A>().values(propertyKeys)
        }

        /**
         * @see GraphTraversal.propertyMap
         */
        fun <A : Element?, B> propertyMap(vararg propertyKeys: String?): GraphTraversal<A, Map<String, B>> {
            return start<A>().propertyMap(propertyKeys)
        }

        /**
         * @see GraphTraversal.elementMap
         */
        fun <A : Element?, B> elementMap(vararg propertyKeys: String?): GraphTraversal<A, Map<Object, B>> {
            return start<A>().elementMap(propertyKeys)
        }

        /**
         * @see GraphTraversal.valueMap
         */
        fun <A : Element?, B> valueMap(vararg propertyKeys: String?): GraphTraversal<A, Map<Object, B>> {
            return start<A>().valueMap(propertyKeys)
        }

        /**
         * @see GraphTraversal.valueMap
         */
        @Deprecated
        @Deprecated(
            """As of release 3.4.0, deprecated in favor of {@link __#valueMap(String...)} in conjunction with
                  {@link GraphTraversal#with(String, Object)}."""
        )
        fun <A : Element?, B> valueMap(
            includeTokens: Boolean,
            vararg propertyKeys: String?
        ): GraphTraversal<A, Map<Object, B>> {
            return start<A>().valueMap(includeTokens, propertyKeys)
        }

        /**
         * @see GraphTraversal.project
         */
        fun <A, B> project(projectKey: String?, vararg projectKeys: String?): GraphTraversal<A, Map<String, B>> {
            return start<A>().project(projectKey, projectKeys)
        }

        /**
         * @see GraphTraversal.select
         */
        fun <A, B> select(column: Column?): GraphTraversal<A, Collection<B>> {
            return start<A>().select(column)
        }

        /**
         * @see GraphTraversal.key
         */
        fun <A : Property?> key(): GraphTraversal<A, String> {
            return start<A>().key()
        }

        /**
         * @see GraphTraversal.value
         */
        fun <A : Property?, B> value(): GraphTraversal<A, B> {
            return start<A>().value()
        }

        /**
         * @see GraphTraversal.path
         */
        fun <A> path(): GraphTraversal<A, Path> {
            return start<A>().path()
        }

        /**
         * @see GraphTraversal.match
         */
        fun <A, B> match(vararg matchTraversals: Traversal<*, *>?): GraphTraversal<A, Map<String, B>> {
            return start<A>().match(matchTraversals)
        }

        /**
         * @see GraphTraversal.sack
         */
        fun <A, B> sack(): GraphTraversal<A, B> {
            return start<A>().sack()
        }

        /**
         * @see GraphTraversal.loops
         */
        fun <A> loops(): GraphTraversal<A, Integer> {
            return start<A>().loops()
        }

        /**
         * @see GraphTraversal.loops
         */
        fun <A> loops(loopName: String?): GraphTraversal<A, Integer> {
            return start<A>().loops(loopName)
        }

        /**
         * @see GraphTraversal.select
         */
        fun <A, B> select(pop: Pop?, selectKey: String?): GraphTraversal<A, B> {
            return start<A>().select(pop, selectKey)
        }

        /**
         * @see GraphTraversal.select
         */
        fun <A, B> select(selectKey: String?): GraphTraversal<A, B> {
            return start<A>().select(selectKey)
        }

        /**
         * @see GraphTraversal.select
         */
        fun <A, B> select(
            pop: Pop?,
            selectKey1: String?,
            selectKey2: String?,
            vararg otherSelectKeys: String?
        ): GraphTraversal<A, Map<String, B>> {
            return start<A>().select(pop, selectKey1, selectKey2, otherSelectKeys)
        }

        /**
         * @see GraphTraversal.select
         */
        fun <A, B> select(
            selectKey1: String?,
            selectKey2: String?,
            vararg otherSelectKeys: String?
        ): GraphTraversal<A, Map<String, B>> {
            return start<A>().select(selectKey1, selectKey2, otherSelectKeys)
        }

        /**
         * @see GraphTraversal.select
         */
        fun <A, B> select(pop: Pop?, keyTraversal: Traversal<A, B>?): GraphTraversal<A, B> {
            return start<A>().select(pop, keyTraversal)
        }

        /**
         * @see GraphTraversal.select
         */
        fun <A, B> select(keyTraversal: Traversal<A, B>?): GraphTraversal<A, B> {
            return start<A>().select(keyTraversal)
        }

        /**
         * @see GraphTraversal.unfold
         */
        fun <A> unfold(): GraphTraversal<A, A> {
            return start<A>().unfold()
        }

        /**
         * @see GraphTraversal.fold
         */
        fun <A> fold(): GraphTraversal<A, List<A>> {
            return start<A>().fold()
        }

        /**
         * @see GraphTraversal.fold
         */
        fun <A, B> fold(seed: B, foldFunction: BiFunction<B, A, B>?): GraphTraversal<A, B> {
            return start<A>().fold(seed, foldFunction)
        }

        /**
         * @see GraphTraversal.count
         */
        fun <A> count(): GraphTraversal<A, Long> {
            return start<A>().count()
        }

        /**
         * @see GraphTraversal.count
         */
        fun <A> count(scope: Scope?): GraphTraversal<A, Long> {
            return start<A>().count(scope)
        }

        /**
         * @see GraphTraversal.sum
         */
        fun <A> sum(): GraphTraversal<A, Double> {
            return start<A>().sum()
        }

        /**
         * @see GraphTraversal.sum
         */
        fun <A> sum(scope: Scope?): GraphTraversal<A, Double> {
            return start<A>().sum(scope)
        }

        /**
         * @see GraphTraversal.min
         */
        fun <A, B : Comparable?> min(): GraphTraversal<A, B> {
            return start<A>().min()
        }

        /**
         * @see GraphTraversal.min
         */
        fun <A, B : Comparable?> min(scope: Scope?): GraphTraversal<A, B> {
            return start<A>().min(scope)
        }

        /**
         * @see GraphTraversal.max
         */
        fun <A, B : Comparable?> max(): GraphTraversal<A, B> {
            return start<A>().max()
        }

        /**
         * @see GraphTraversal.max
         */
        fun <A, B : Comparable?> max(scope: Scope?): GraphTraversal<A, B> {
            return start<A>().max(scope)
        }

        /**
         * @see GraphTraversal.mean
         */
        fun <A> mean(): GraphTraversal<A, Double> {
            return start<A>().mean()
        }

        /**
         * @see GraphTraversal.mean
         */
        fun <A> mean(scope: Scope?): GraphTraversal<A, Double> {
            return start<A>().mean(scope)
        }

        /**
         * @see GraphTraversal.group
         */
        fun <A, K, V> group(): GraphTraversal<A, Map<K, V>> {
            return start<A>().group()
        }

        /**
         * @see GraphTraversal.groupCount
         */
        fun <A, K> groupCount(): GraphTraversal<A, Map<K, Long>> {
            return start<A>().< K > groupCount < K ? > ()
        }

        /**
         * @see GraphTraversal.tree
         */
        fun <A> tree(): GraphTraversal<A, Tree> {
            return start<A>().tree()
        }

        /**
         * @see GraphTraversal.addV
         */
        fun <A> addV(vertexLabel: String?): GraphTraversal<A, Vertex> {
            return start<A>().addV(vertexLabel)
        }

        /**
         * @see GraphTraversal.addV
         */
        fun <A> addV(vertexLabelTraversal: Traversal<*, String?>?): GraphTraversal<A, Vertex> {
            return start<A>().addV(vertexLabelTraversal)
        }

        /**
         * @see GraphTraversal.addV
         */
        fun <A> addV(): GraphTraversal<A, Vertex> {
            return start<A>().addV()
        }

        /**
         * @see GraphTraversal.addE
         */
        fun <A> addE(edgeLabel: String?): GraphTraversal<A, Edge> {
            return start<A>().addE(edgeLabel)
        }

        /**
         * @see GraphTraversal.addE
         */
        fun <A> addE(edgeLabelTraversal: Traversal<*, String?>?): GraphTraversal<A, Edge> {
            return start<A>().addE(edgeLabelTraversal)
        }

        /**
         * @see GraphTraversal.math
         */
        fun <A> math(expression: String?): GraphTraversal<A, Double> {
            return start<A>().math(expression)
        }
        ///////////////////// FILTER STEPS /////////////////////
        /**
         * @see GraphTraversal.filter
         */
        fun <A> filter(predicate: Predicate<Traverser<A>?>?): GraphTraversal<A, A> {
            return start<A>().filter(predicate)
        }

        /**
         * @see GraphTraversal.filter
         */
        fun <A> filter(filterTraversal: Traversal<*, *>?): GraphTraversal<A, A> {
            return start<A>().filter(filterTraversal)
        }

        /**
         * @see GraphTraversal.and
         */
        fun <A> and(vararg andTraversals: Traversal<*, *>?): GraphTraversal<A, A> {
            return start<A>().and(andTraversals)
        }

        /**
         * @see GraphTraversal.or
         */
        fun <A> or(vararg orTraversals: Traversal<*, *>?): GraphTraversal<A, A> {
            return start<A>().or(orTraversals)
        }

        /**
         * @see GraphTraversal.inject
         */
        fun <A> inject(vararg injections: A): GraphTraversal<A, A> {
            return start<A>().inject(injections as Array<A>)
        }

        /**
         * @see GraphTraversal.dedup
         */
        fun <A> dedup(vararg dedupLabels: String?): GraphTraversal<A, A> {
            return start<A>().dedup(dedupLabels)
        }

        /**
         * @see GraphTraversal.dedup
         */
        fun <A> dedup(scope: Scope?, vararg dedupLabels: String?): GraphTraversal<A, A> {
            return start<A>().dedup(scope, dedupLabels)
        }

        /**
         * @see GraphTraversal.has
         */
        fun <A> has(propertyKey: String?, predicate: P<*>?): GraphTraversal<A, A> {
            return start<A>().has(propertyKey, predicate)
        }

        /**
         * @see GraphTraversal.has
         */
        fun <A> has(accessor: T?, predicate: P<*>?): GraphTraversal<A, A> {
            return start<A>().has(accessor, predicate)
        }

        /**
         * @see GraphTraversal.has
         */
        fun <A> has(propertyKey: String?, value: Object?): GraphTraversal<A, A> {
            return start<A>().has(propertyKey, value)
        }

        /**
         * @see GraphTraversal.has
         */
        fun <A> has(accessor: T?, value: Object?): GraphTraversal<A, A> {
            return start<A>().has(accessor, value)
        }

        /**
         * @see GraphTraversal.has
         */
        fun <A> has(label: String?, propertyKey: String?, value: Object?): GraphTraversal<A, A> {
            return start<A>().has(label, propertyKey, value)
        }

        /**
         * @see GraphTraversal.has
         */
        fun <A> has(label: String?, propertyKey: String?, predicate: P<*>?): GraphTraversal<A, A> {
            return start<A>().has(label, propertyKey, predicate)
        }

        /**
         * @see GraphTraversal.has
         */
        fun <A> has(accessor: T?, propertyTraversal: Traversal<*, *>?): GraphTraversal<A, A> {
            return start<A>().has(accessor, propertyTraversal)
        }

        /**
         * @see GraphTraversal.has
         */
        fun <A> has(propertyKey: String?, propertyTraversal: Traversal<*, *>?): GraphTraversal<A, A> {
            return start<A>().has(propertyKey, propertyTraversal)
        }

        /**
         * @see GraphTraversal.has
         */
        fun <A> has(propertyKey: String?): GraphTraversal<A, A> {
            return start<A>().has(propertyKey)
        }

        /**
         * @see GraphTraversal.hasNot
         */
        fun <A> hasNot(propertyKey: String?): GraphTraversal<A, A> {
            return start<A>().hasNot(propertyKey)
        }

        /**
         * @see GraphTraversal.hasLabel
         */
        fun <A> hasLabel(label: String?, vararg otherLabels: String?): GraphTraversal<A, A> {
            return start<A>().hasLabel(label, otherLabels)
        }

        /**
         * @see GraphTraversal.hasLabel
         */
        fun <A> hasLabel(predicate: P<String?>?): GraphTraversal<A, A> {
            return start<A>().hasLabel(predicate)
        }

        /**
         * @see GraphTraversal.hasId
         */
        fun <A> hasId(id: Object?, vararg otherIds: Object?): GraphTraversal<A, A> {
            return start<A>().hasId(id, otherIds)
        }

        /**
         * @see GraphTraversal.hasId
         */
        fun <A> hasId(predicate: P<Object?>?): GraphTraversal<A, A> {
            return start<A>().hasId(predicate)
        }

        /**
         * @see GraphTraversal.hasKey
         */
        fun <A> hasKey(label: String?, vararg otherLabels: String?): GraphTraversal<A, A> {
            return start<A>().hasKey(label, otherLabels)
        }

        /**
         * @see GraphTraversal.hasKey
         */
        fun <A> hasKey(predicate: P<String?>?): GraphTraversal<A, A> {
            return start<A>().hasKey(predicate)
        }

        /**
         * @see GraphTraversal.hasValue
         */
        fun <A> hasValue(value: Object?, vararg values: Object?): GraphTraversal<A, A> {
            return start<A>().hasValue(value, values)
        }

        /**
         * @see GraphTraversal.hasValue
         */
        fun <A> hasValue(predicate: P<Object?>?): GraphTraversal<A, A> {
            return start<A>().hasValue(predicate)
        }

        /**
         * @see GraphTraversal.where
         */
        fun <A> where(startKey: String?, predicate: P<String?>?): GraphTraversal<A, A> {
            return start<A>().where(startKey, predicate)
        }

        /**
         * @see GraphTraversal.where
         */
        fun <A> where(predicate: P<String?>?): GraphTraversal<A, A> {
            return start<A>().where(predicate)
        }

        /**
         * @see GraphTraversal.where
         */
        fun <A> where(whereTraversal: Traversal<*, *>?): GraphTraversal<A, A> {
            return start<A>().where(whereTraversal)
        }

        /**
         * @see GraphTraversal. is
         */
        fun <A> `is`(predicate: P<A>?): GraphTraversal<A, A> {
            return start<A>().`is`(predicate)
        }

        /**
         * @see GraphTraversal. is
         */
        fun <A> `is`(value: Object?): GraphTraversal<A, A> {
            return start<A>().`is`(value)
        }

        /**
         * @see GraphTraversal.not
         */
        fun <A> not(notTraversal: Traversal<*, *>?): GraphTraversal<A, A> {
            return start<A>().not(notTraversal)
        }

        /**
         * @see GraphTraversal.coin
         */
        fun <A> coin(probability: Double): GraphTraversal<A, A> {
            return start<A>().coin(probability)
        }

        /**
         * @see GraphTraversal.range
         */
        fun <A> range(low: Long, high: Long): GraphTraversal<A, A> {
            return start<A>().range(low, high)
        }

        /**
         * @see GraphTraversal.range
         */
        fun <A> range(scope: Scope?, low: Long, high: Long): GraphTraversal<A, A> {
            return start<A>().range(scope, low, high)
        }

        /**
         * @see GraphTraversal.limit
         */
        fun <A> limit(limit: Long): GraphTraversal<A, A> {
            return start<A>().limit(limit)
        }

        /**
         * @see GraphTraversal.limit
         */
        fun <A> limit(scope: Scope?, limit: Long): GraphTraversal<A, A> {
            return start<A>().limit(scope, limit)
        }

        /**
         * @see GraphTraversal.skip
         */
        fun <A> skip(skip: Long): GraphTraversal<A, A> {
            return start<A>().skip(skip)
        }

        /**
         * @see GraphTraversal.skip
         */
        fun <A> skip(scope: Scope?, skip: Long): GraphTraversal<A, A> {
            return start<A>().skip(scope, skip)
        }

        /**
         * @see GraphTraversal.tail
         */
        fun <A> tail(): GraphTraversal<A, A> {
            return start<A>().tail()
        }

        /**
         * @see GraphTraversal.tail
         */
        fun <A> tail(limit: Long): GraphTraversal<A, A> {
            return start<A>().tail(limit)
        }

        /**
         * @see GraphTraversal.tail
         */
        fun <A> tail(scope: Scope?): GraphTraversal<A, A> {
            return start<A>().tail(scope)
        }

        /**
         * @see GraphTraversal.tail
         */
        fun <A> tail(scope: Scope?, limit: Long): GraphTraversal<A, A> {
            return start<A>().tail(scope, limit)
        }

        /**
         * @see GraphTraversal.simplePath
         */
        fun <A> simplePath(): GraphTraversal<A, A> {
            return start<A>().simplePath()
        }

        /**
         * @see GraphTraversal.cyclicPath
         */
        fun <A> cyclicPath(): GraphTraversal<A, A> {
            return start<A>().cyclicPath()
        }

        /**
         * @see GraphTraversal.sample
         */
        fun <A> sample(amountToSample: Int): GraphTraversal<A, A> {
            return start<A>().sample(amountToSample)
        }

        /**
         * @see GraphTraversal.sample
         */
        fun <A> sample(scope: Scope?, amountToSample: Int): GraphTraversal<A, A> {
            return start<A>().sample(scope, amountToSample)
        }

        /**
         * @see GraphTraversal.drop
         */
        fun <A> drop(): GraphTraversal<A, A> {
            return start<A>().drop()
        }
        ///////////////////// SIDE-EFFECT STEPS /////////////////////
        /**
         * @see GraphTraversal.sideEffect
         */
        fun <A> sideEffect(consumer: Consumer<Traverser<A>?>?): GraphTraversal<A, A> {
            return start<A>().sideEffect(consumer)
        }

        /**
         * @see GraphTraversal.sideEffect
         */
        fun <A> sideEffect(sideEffectTraversal: Traversal<*, *>?): GraphTraversal<A, A> {
            return start<A>().sideEffect(sideEffectTraversal)
        }

        /**
         * @see GraphTraversal.cap
         */
        fun <A, B> cap(sideEffectKey: String?, vararg sideEffectKeys: String?): GraphTraversal<A, B> {
            return start<A>().cap(sideEffectKey, sideEffectKeys)
        }

        /**
         * @see GraphTraversal.subgraph
         */
        fun <A> subgraph(sideEffectKey: String?): GraphTraversal<A, Edge> {
            return start<A>().subgraph(sideEffectKey)
        }

        /**
         * @see GraphTraversal.aggregate
         */
        fun <A> aggregate(sideEffectKey: String?): GraphTraversal<A, A> {
            return start<A>().aggregate(sideEffectKey)
        }

        /**
         * @see GraphTraversal.aggregate
         */
        fun <A> aggregate(scope: Scope?, sideEffectKey: String?): GraphTraversal<A, A> {
            return start<A>().aggregate(scope, sideEffectKey)
        }

        /**
         * @see GraphTraversal.fail
         */
        fun <A> fail(): GraphTraversal<A, A> {
            return start<A>().fail()
        }

        /**
         * @see GraphTraversal.fail
         */
        fun <A> fail(message: String?): GraphTraversal<A, A> {
            return start<A>().fail(message)
        }

        /**
         * @see GraphTraversal.group
         */
        fun <A> group(sideEffectKey: String?): GraphTraversal<A, A> {
            return start<A>().group(sideEffectKey)
        }

        /**
         * @see GraphTraversal.groupCount
         */
        fun <A> groupCount(sideEffectKey: String?): GraphTraversal<A, A> {
            return start<A>().groupCount(sideEffectKey)
        }

        /**
         * @see GraphTraversal.timeLimit
         */
        fun <A> timeLimit(timeLimit: Long): GraphTraversal<A, A> {
            return start<A>().timeLimit(timeLimit)
        }

        /**
         * @see GraphTraversal.tree
         */
        fun <A> tree(sideEffectKey: String?): GraphTraversal<A, A> {
            return start<A>().tree(sideEffectKey)
        }

        /**
         * @see GraphTraversal.sack
         */
        fun <A, V, U> sack(sackOperator: BiFunction<V, U, V>?): GraphTraversal<A, A> {
            return start<A>().sack(sackOperator)
        }

        /**
         * @see GraphTraversal.store
         */
        @Deprecated
        @Deprecated("As of release 3.4.3, replaced by {@link #aggregate(Scope, String)} using {@link Scope#local}.")
        fun <A> store(sideEffectKey: String?): GraphTraversal<A, A> {
            return start<A>().store(sideEffectKey)
        }

        /**
         * @see GraphTraversal.property
         */
        fun <A> property(key: Object?, value: Object?, vararg keyValues: Object?): GraphTraversal<A, A> {
            return start<A>().property(key, value, keyValues)
        }

        /**
         * @see GraphTraversal.property
         */
        fun <A> property(
            cardinality: VertexProperty.Cardinality?,
            key: Object?,
            value: Object?,
            vararg keyValues: Object?
        ): GraphTraversal<A, A> {
            return start<A>().property(cardinality, key, value, keyValues)
        }
        ///////////////////// BRANCH STEPS /////////////////////
        /**
         * @see GraphTraversal.branch
         */
        fun <A, M, B> branch(function: Function<Traverser<A>?, M>?): GraphTraversal<A, B> {
            return start<A>().branch(function)
        }

        /**
         * @see GraphTraversal.branch
         */
        fun <A, M, B> branch(traversalFunction: Traversal<*, M>?): GraphTraversal<A, B> {
            return start<A>().branch(traversalFunction)
        }

        /**
         * @see GraphTraversal.choose
         */
        fun <A, B> choose(
            choosePredicate: Predicate<A>?,
            trueChoice: Traversal<*, B>?,
            falseChoice: Traversal<*, B>?
        ): GraphTraversal<A, B> {
            return start<A>().choose(choosePredicate, trueChoice, falseChoice)
        }

        /**
         * @see GraphTraversal.choose
         */
        fun <A, B> choose(choosePredicate: Predicate<A>?, trueChoice: Traversal<*, B>?): GraphTraversal<A, B> {
            return start<A>().choose(choosePredicate, trueChoice)
        }

        /**
         * @see GraphTraversal.choose
         */
        fun <A, M, B> choose(choiceFunction: Function<A, M>?): GraphTraversal<A, B> {
            return start<A>().choose(choiceFunction)
        }

        /**
         * @see GraphTraversal.choose
         */
        fun <A, M, B> choose(traversalFunction: Traversal<*, M>?): GraphTraversal<A, B> {
            return start<A>().choose(traversalFunction)
        }

        /**
         * @see GraphTraversal.choose
         */
        fun <A, M, B> choose(
            traversalPredicate: Traversal<*, M>?,
            trueChoice: Traversal<*, B>?,
            falseChoice: Traversal<*, B>?
        ): GraphTraversal<A, B> {
            return start<A>().choose(traversalPredicate, trueChoice, falseChoice)
        }

        /**
         * @see GraphTraversal.choose
         */
        fun <A, M, B> choose(traversalPredicate: Traversal<*, M>?, trueChoice: Traversal<*, B>?): GraphTraversal<A, B> {
            return start<A>().choose(traversalPredicate, trueChoice)
        }

        /**
         * @see GraphTraversal.optional
         */
        fun <A> optional(optionalTraversal: Traversal<*, A>?): GraphTraversal<A, A> {
            return start<A>().optional(optionalTraversal)
        }

        /**
         * @see GraphTraversal.union
         */
        fun <A, B> union(vararg traversals: Traversal<*, B>?): GraphTraversal<A, B> {
            return start<A>().union(traversals)
        }

        /**
         * @see GraphTraversal.coalesce
         */
        fun <A, B> coalesce(vararg traversals: Traversal<*, B>?): GraphTraversal<A, B> {
            return start<A>().coalesce(traversals)
        }

        /**
         * @see GraphTraversal.repeat
         */
        fun <A> repeat(traversal: Traversal<*, A>?): GraphTraversal<A, A> {
            return start<A>().repeat(traversal)
        }

        /**
         * @see GraphTraversal.repeat
         */
        fun <A> repeat(loopName: String?, traversal: Traversal<*, A>?): GraphTraversal<A, A> {
            return start<A>().repeat(loopName, traversal)
        }

        /**
         * @see GraphTraversal.emit
         */
        fun <A> emit(emitTraversal: Traversal<*, *>?): GraphTraversal<A, A> {
            return start<A>().emit(emitTraversal)
        }

        /**
         * @see GraphTraversal.emit
         */
        fun <A> emit(emitPredicate: Predicate<Traverser<A>?>?): GraphTraversal<A, A> {
            return start<A>().emit(emitPredicate)
        }

        /**
         * @see GraphTraversal.until
         */
        fun <A> until(untilTraversal: Traversal<*, *>?): GraphTraversal<A, A> {
            return start<A>().until(untilTraversal)
        }

        /**
         * @see GraphTraversal.until
         */
        fun <A> until(untilPredicate: Predicate<Traverser<A>?>?): GraphTraversal<A, A> {
            return start<A>().until(untilPredicate)
        }

        /**
         * @see GraphTraversal.times
         */
        operator fun <A> times(maxLoops: Int): GraphTraversal<A, A> {
            return start<A>().times(maxLoops)
        }

        /**
         * @see GraphTraversal.emit
         */
        fun <A> emit(): GraphTraversal<A, A> {
            return start<A>().emit()
        }

        /**
         * @see GraphTraversal.local
         */
        fun <A, B> local(localTraversal: Traversal<*, B>?): GraphTraversal<A, B> {
            return start<A>().local(localTraversal)
        }
        ///////////////////// UTILITY STEPS /////////////////////
        /**
         * @see GraphTraversal. as
         */
        fun <A> `as`(label: String?, vararg labels: String?): GraphTraversal<A, A> {
            return start<A>().`as`(label, labels)
        }

        /**
         * @see GraphTraversal.barrier
         */
        fun <A> barrier(): GraphTraversal<A, A> {
            return start<A>().barrier()
        }

        /**
         * @see GraphTraversal.barrier
         */
        fun <A> barrier(maxBarrierSize: Int): GraphTraversal<A, A> {
            return start<A>().barrier(maxBarrierSize)
        }

        /**
         * @see GraphTraversal.barrier
         */
        fun <A> barrier(barrierConsumer: Consumer<TraverserSet<Object?>?>?): GraphTraversal<A, A> {
            return start<A>().barrier(barrierConsumer)
        }

        /**
         * @see GraphTraversal.index
         */
        fun <A, B> index(): GraphTraversal<A, B> {
            return start<A>().index()
        }
    }
}