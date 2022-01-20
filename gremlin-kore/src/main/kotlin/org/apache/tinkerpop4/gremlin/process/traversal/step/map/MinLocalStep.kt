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
import org.apache.tinkerpop4.gremlin.process.traversal.util.FastNoSuchElementException
import java.util.Collections
import java.util.Iterator
import java.util.Set
import org.apache.tinkerpop4.gremlin.util.NumberHelper.min

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Daniel Kuppitz (http://gremlin.guru)
 */
class MinLocalStep<E : Comparable?, S : Iterable<E>?>(traversal: Traversal.Admin?) : ScalarMapStep<S, E>(traversal) {
    @Override
    protected fun map(traverser: Traverser.Admin<S>): E {
        val iterator: Iterator<E> = traverser.get().iterator()
        if (iterator.hasNext()) {
            var result: Comparable = iterator.next()
            while (iterator.hasNext()) {
                result = min(iterator.next(), result)
            }
            return result
        }
        throw FastNoSuchElementException.instance()
    }

    @get:Override
    val requirements: Set<Any>
        get() = Collections.singleton(TraverserRequirement.OBJECT)
}