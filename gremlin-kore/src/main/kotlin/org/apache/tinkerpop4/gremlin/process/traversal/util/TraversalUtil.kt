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
package org.apache.tinkerpop4.gremlin.process.traversal.util

import org.apache.tinkerpop4.gremlin.process.traversal.Step

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
object TraversalUtil {
    fun <S, E> apply(traverser: Traverser.Admin<S>, traversal: Traversal.Admin<S, E>): E {
        val split: Traverser<S> = prepare<Any, Any>(traverser, traversal)
        return try {
            traversal.next() // map
        } catch (e: NoSuchElementException) {
            val clazzOfTraverserValue = if (null == split.get()) "null" else split.get().getClass().getSimpleName()
            throw IllegalArgumentException(
                String.format(
                    "The provided traverser does not map to a value: %s[%s]->%s[%s] parent[%s]",
                    split,
                    clazzOfTraverserValue,
                    traversal,
                    traversal.getClass().getSimpleName(),
                    traversal.getParent().asStep().getTraversal()
                )
            )
        } finally {
            //Close the traversal to release any underlying resources.
            CloseableIterator.closeIterator(traversal)
        }
    }

    fun <S, E> apply(start: S, traversal: Traversal.Admin<S, E>): E {
        traversal.reset()
        traversal.addStart(traversal.getTraverserGenerator().generate(start, traversal.getStartStep(), 1L))
        return try {
            traversal.next() // map
        } catch (e: NoSuchElementException) {
            throw IllegalArgumentException("The provided start does not map to a value: $start->$traversal")
        } finally {
            //Close the traversal to release any underlying resources.
            CloseableIterator.closeIterator(traversal)
        }
    }

    fun <S, E> applyNullable(start: S, traversal: Traversal.Admin<S, E>?): E {
        return if (null == traversal) start as E else apply(start, traversal)
    }

    fun <S, E> applyNullable(traverser: Traverser.Admin<S>, traversal: Traversal.Admin<S, E>?): E {
        return if (null == traversal) traverser.get() else apply(traverser, traversal)
    }

    fun <S, E> applyAll(traverser: Traverser.Admin<S>, traversal: Traversal.Admin<S, E>): Iterator<E> {
        prepare<Any, Any>(traverser, traversal)
        return traversal // flatmap
    }

    fun <S, E> applyAll(start: S, traversal: Traversal.Admin<S, E>): Iterator<E> {
        traversal.reset()
        traversal.addStart(traversal.getTraverserGenerator().generate(start, traversal.getStartStep(), 1L))
        return traversal // flatMap
    }

    fun <S, E> test(traverser: Traverser.Admin<S>, traversal: Traversal.Admin<S, E>, end: E?): Boolean {
        if (null == end) return test(traverser, traversal)
        prepare<Any, Any>(traverser, traversal)
        val endStep: Step<*, E> = traversal.getEndStep()
        var result = false
        while (traversal.hasNext()) {
            if (endStep.next().get().equals(end)) {
                result = true
                break
            }
        }

        // The traversal might not have been fully consumed in the loop above. Close the traversal to release any underlying
        // resources.
        CloseableIterator.closeIterator(traversal)
        return result
    }

    fun <S, E> produce(traverser: Traverser.Admin<S>, traversal: Traversal.Admin<S, E>?): TraversalProduct {
        return if (null == traversal) {
            TraversalProduct(traverser.get())
        } else {
            prepare<Any, Any>(traverser, traversal)
            try {
                if (traversal.hasNext()) {
                    TraversalProduct(traversal.next())
                } else {
                    TraversalProduct.UNPRODUCTIVE
                }
            } finally {
                CloseableIterator.closeIterator(traversal)
            }
        }
    }

    fun <S, E> produce(start: S, traversal: Traversal.Admin<S, E>?): TraversalProduct {
        return if (null == traversal) {
            TraversalProduct(start)
        } else {
            traversal.reset()
            traversal.addStart(traversal.getTraverserGenerator().generate(start, traversal.getStartStep(), 1L))
            try {
                if (traversal.hasNext()) {
                    TraversalProduct(traversal.next())
                } else {
                    TraversalProduct.UNPRODUCTIVE
                }
            } finally {
                CloseableIterator.closeIterator(traversal)
            }
        }
    }

    fun <S, E> test(traverser: Traverser.Admin<S>, traversal: Traversal.Admin<S, E>): Boolean {
        prepare<Any, Any>(traverser, traversal)
        val `val`: Boolean = traversal.hasNext() // filter

        //Close the traversal to release any underlying resources.
        CloseableIterator.closeIterator(traversal)
        return `val`
    }

    fun <S, E> test(start: S, traversal: Traversal.Admin<S, E>, end: E?): Boolean {
        if (null == end) return test<S, Any>(start, traversal)
        traversal.reset()
        traversal.addStart(traversal.getTraverserGenerator().generate(start, traversal.getStartStep(), 1L))
        val endStep: Step<*, E> = traversal.getEndStep()
        var result = false
        while (traversal.hasNext()) {
            if (endStep.next().get().equals(end)) {
                result = true
                break
            }
        }

        //Close the traversal to release any underlying resources.
        CloseableIterator.closeIterator(traversal)
        return result
    }

    fun <S, E> test(start: S, traversal: Traversal.Admin<S, E>): Boolean {
        traversal.reset()
        traversal.addStart(traversal.getTraverserGenerator().generate(start, traversal.getStartStep(), 1L))
        val result: Boolean = traversal.hasNext() // filter

        //Close the traversal to release any underlying resources.
        CloseableIterator.closeIterator(traversal)
        return result
    }

    fun <S, E> prepare(traverser: Traverser.Admin<S>, traversal: Traversal.Admin<S, E>): Traverser<S> {
        val split: Traverser.Admin<S> = traverser.split()
        split.setSideEffects(traversal.getSideEffects())
        split.setBulk(1L)
        traversal.reset()
        traversal.addStart(split)
        return split
    }
}