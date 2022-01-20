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
package org.apache.tinkerpop4.gremlin.util.iterator

import org.apache.tinkerpop4.gremlin.process.traversal.util.FastNoSuchElementException
import java.util.ArrayList
import java.util.Collection
import java.util.Collections
import java.util.Comparator
import java.util.HashMap
import java.util.HashSet
import java.util.Iterator
import java.util.List
import java.util.Map
import java.util.Set
import java.util.Spliterator
import java.util.Spliterators
import java.util.function.BiFunction
import java.util.function.BinaryOperator
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Predicate
import java.util.stream.Stream
import java.util.stream.StreamSupport

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
object IteratorUtils {
    fun <S> of(a: S): Iterator<S> {
        return SingleIterator(a)
    }

    fun <S> of(a: S, b: S): Iterator<S> {
        return DoubleIterator(a, b)
    }

    ///////////////
    fun <S : Collection<T>?, T> fill(iterator: Iterator<T>, collection: S): S {
        while (iterator.hasNext()) {
            collection.add(iterator.next())
        }
        return collection
    }

    fun iterate(iterator: Iterator) {
        while (iterator.hasNext()) {
            iterator.next()
        }
    }

    fun count(iterator: Iterator): Long {
        var ix: Long = 0
        while (iterator.hasNext()) {
            iterator.next()
            ++ix
        }
        return ix
    }

    fun count(iterable: Iterable): Long {
        return count(iterable.iterator())
    }

    fun <S> list(iterator: Iterator<S>): List<S> {
        return fill(iterator, ArrayList())
    }

    fun <S> list(iterator: Iterator<S>, comparator: Comparator?): List<S> {
        val l: List<S> = list<Any>(iterator)
        Collections.sort(l, comparator)
        return l
    }

    fun <S> set(iterator: Iterator<S>): Set<S> {
        return fill(iterator, HashSet())
    }

    fun <S> limit(iterator: Iterator<S>, limit: Int): Iterator<S> {
        return object : Iterator<S>() {
            private var count = 0

            @Override
            override fun hasNext(): Boolean {
                return iterator.hasNext() && count < limit
            }

            @Override
            fun remove() {
                iterator.remove()
            }

            @Override
            override fun next(): S {
                if (count++ >= limit) throw FastNoSuchElementException.instance()
                return iterator.next()
            }
        }
    }

    ///////////////////
    fun <T> allMatch(iterator: Iterator<T>, predicate: Predicate<T>): Boolean {
        while (iterator.hasNext()) {
            if (!predicate.test(iterator.next())) {
                return false
            }
        }
        return true
    }

    fun <T> anyMatch(iterator: Iterator<T>, predicate: Predicate<T>): Boolean {
        while (iterator.hasNext()) {
            if (predicate.test(iterator.next())) {
                return true
            }
        }
        return false
    }

    fun <T> noneMatch(iterator: Iterator<T>, predicate: Predicate<T>): Boolean {
        while (iterator.hasNext()) {
            if (predicate.test(iterator.next())) {
                return false
            }
        }
        return true
    }

    ///////////////////
    fun <K, S> collectMap(iterator: Iterator<S>, key: Function<S, K>): Map<K, S> {
        return collectMap(iterator, key, Function.identity())
    }

    fun <K, S, V> collectMap(iterator: Iterator<S>, key: Function<S, K>, value: Function<S, V>): Map<K, V> {
        val map: Map<K, V> = HashMap()
        while (iterator.hasNext()) {
            val obj = iterator.next()
            map.put(key.apply(obj), value.apply(obj))
        }
        return map
    }

    fun <K, S> groupBy(iterator: Iterator<S>, groupBy: Function<S, K>): Map<K, List<S>> {
        val map: Map<K, List<S>> = HashMap()
        while (iterator.hasNext()) {
            val obj = iterator.next()
            map.computeIfAbsent(groupBy.apply(obj)) { k -> ArrayList() }.add(obj)
        }
        return map
    }

    fun <S> reduce(iterator: Iterator<S>, identity: S, accumulator: BinaryOperator<S>): S {
        var result = identity
        while (iterator.hasNext()) {
            result = accumulator.apply(result, iterator.next())
        }
        return result
    }

    fun <S> reduce(iterable: Iterable<S>, identity: S, accumulator: BinaryOperator<S>?): S {
        return reduce(iterable.iterator(), identity, accumulator)
    }

    fun <S, E> reduce(iterator: Iterator<S>, identity: E, accumulator: BiFunction<E, S, E>): E {
        var result = identity
        while (iterator.hasNext()) {
            result = accumulator.apply(result, iterator.next())
        }
        return result
    }

    fun <S, E> reduce(iterable: Iterable<S>, identity: E, accumulator: BiFunction<E, S, E>?): E {
        return reduce(iterable.iterator(), identity, accumulator)
    }

    ///////////////
    fun <S> consume(iterator: Iterator<S>, consumer: Consumer<S>): Iterator<S> {
        return object : Iterator<S>() {
            @Override
            override fun hasNext(): Boolean {
                return iterator.hasNext()
            }

            @Override
            fun remove() {
                iterator.remove()
            }

            @Override
            override fun next(): S {
                val s = iterator.next()
                consumer.accept(s)
                return s
            }
        }
    }

    fun <S> consume(iterable: Iterable<S>, consumer: Consumer<S>): Iterable<S> {
        return Iterable<S> { consume(iterable.iterator(), consumer) }
    }

    ///////////////
    fun <S, E> map(iterator: Iterator<S>, function: Function<S, E>): Iterator<E> {
        return object : Iterator<E>() {
            @Override
            override fun hasNext(): Boolean {
                return iterator.hasNext()
            }

            @Override
            fun remove() {
                iterator.remove()
            }

            @Override
            override fun next(): E {
                return function.apply(iterator.next())
            }
        }
    }

    fun <S, E> map(iterable: Iterable<S>, function: Function<S, E>): Iterable<E> {
        return Iterable<E> { map(iterable.iterator(), function) }
    }

    ///////////////
    fun <S> filter(iterator: Iterator<S>, predicate: Predicate<S>): Iterator<S> {
        return object : Iterator<S>() {
            var nextResult: S? = null

            @Override
            override fun hasNext(): Boolean {
                return if (null != nextResult) {
                    true
                } else {
                    advance()
                    null != nextResult
                }
            }

            @Override
            fun remove() {
                iterator.remove()
            }

            @Override
            override fun next(): S {
                return try {
                    if (null != nextResult) {
                        nextResult
                    } else {
                        advance()
                        if (null != nextResult) nextResult else throw FastNoSuchElementException.instance()
                    }
                } finally {
                    nextResult = null
                }
            }

            private fun advance() {
                nextResult = null
                while (iterator.hasNext()) {
                    val s = iterator.next()
                    if (predicate.test(s)) {
                        nextResult = s
                        return
                    }
                }
            }
        }
    }

    fun <S> filter(iterable: Iterable<S>, predicate: Predicate<S>): Iterable<S> {
        return Iterable<S> { filter(iterable.iterator(), predicate) }
    }

    ///////////////////
    fun <S, E> flatMap(iterator: Iterator<S>, function: Function<S, Iterator<E>?>): Iterator<E> {
        return object : Iterator<E>() {
            private var currentIterator: Iterator<E> = Collections.emptyIterator()

            @Override
            override fun hasNext(): Boolean {
                if (currentIterator.hasNext()) return true else {
                    while (iterator.hasNext()) {
                        currentIterator = function.apply(iterator.next())
                        if (currentIterator.hasNext()) return true
                    }
                }
                return false
            }

            @Override
            fun remove() {
                iterator.remove()
            }

            @Override
            override fun next(): E {
                return if (hasNext()) currentIterator.next() else throw FastNoSuchElementException.instance()
            }
        }
    }

    ///////////////////
    fun <S> concat(vararg iterators: Iterator<S>?): Iterator<S> {
        val iterator: MultiIterator<S> = MultiIterator()
        for (itty in iterators) {
            iterator.addIterator(itty)
        }
        return iterator
    }

    ///////////////////
    fun asIterator(o: Object): Iterator {
        val itty: Iterator
        if (o is Iterable) itty = (o as Iterable).iterator() else if (o is Iterator) itty =
            o else if (o is Array<Object>) itty = ArrayIterator(o as Array<Object?>) else if (o is Stream) itty =
            (o as Stream).iterator() else if (o is Map) itty =
            (o as Map).entrySet().iterator() else if (o is Throwable) itty =
            of((o as Throwable).getMessage()) else itty = of<Any>(o)
        return itty
    }

    fun asList(o: Object): List {
        return list(asIterator(o))
    }

    /**
     * Construct a [Stream] from an [Iterator].
     */
    fun <T> stream(iterator: Iterator<T>?): Stream<T> {
        return StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(
                iterator,
                Spliterator.IMMUTABLE or Spliterator.SIZED
            ), false
        )
    }

    fun <T> stream(iterable: Iterable<T>): Stream<T> {
        return stream(iterable.iterator())
    }

    fun <T> noRemove(iterator: Iterator<T>): Iterator<T> {
        return object : Iterator<T>() {
            @Override
            override fun hasNext(): Boolean {
                return iterator.hasNext()
            }

            @Override
            fun remove() {
                // do nothing
            }

            @Override
            override fun next(): T {
                return iterator.next()
            }
        }
    }

    fun <T> removeOnNext(iterator: Iterator<T>): Iterator<T> {
        return object : Iterator<T>() {
            @Override
            override fun hasNext(): Boolean {
                return iterator.hasNext()
            }

            @Override
            fun remove() {
                iterator.remove()
            }

            @Override
            override fun next(): T {
                val `object` = iterator.next()
                iterator.remove()
                return `object`
            }
        }
    }
}