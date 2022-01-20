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
package org.apache.tinkerpop4.gremlin.process.traversal.step.map

import org.apache.tinkerpop4.gremlin.process.traversal.Traversal
import org.apache.tinkerpop4.gremlin.process.traversal.Traverser
import org.apache.tinkerpop4.gremlin.process.traversal.traverser.TraverserRequirement
import org.apache.tinkerpop4.gremlin.util.iterator.ArrayIterator
import org.apache.tinkerpop4.gremlin.util.iterator.IteratorUtils
import java.lang.reflect.Array
import java.util.Collections
import java.util.Iterator
import java.util.Map
import java.util.Set

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class UnfoldStep<S, E>(traversal: Traversal.Admin?) : FlatMapStep<S, E>(traversal) {
    @Override
    protected fun flatMap(traverser: Traverser.Admin<S>): Iterator<E> {
        val s: S = traverser.get()
        return if (s is Iterator) s else if (s is Iterable) (s as Iterable).iterator() else if (s is Map) (s as Map).entrySet()
            .iterator() else if (s.getClass().isArray()) handleArrays(s) else IteratorUtils.of(s as E)
    }

    @get:Override
    val requirements: Set<Any>
        get() = Collections.singleton(TraverserRequirement.OBJECT)

    private fun handleArrays(array: Object): Iterator<E> {
        return if (array is Array<Object>) {
            ArrayIterator(array as Array<E>)
        } else {
            val len: Int = Array.getLength(array)
            val objectArray: Array<Object?> = arrayOfNulls<Object>(len)
            for (i in 0 until len) objectArray[i] = Array.get(array, i)
            ArrayIterator(objectArray as Array<E?>)
        }
    }
}