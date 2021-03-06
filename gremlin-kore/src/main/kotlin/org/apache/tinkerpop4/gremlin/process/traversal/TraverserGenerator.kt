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
package org.apache.tinkerpop4.gremlin.process.traversal

import org.apache.tinkerpop4.gremlin.process.traversal.traverser.TraverserRequirement
import java.util.Iterator
import java.util.Set

/**
 * A TraverserGenerator will generate traversers for a particular [Traversal]. In essence, wrap objects in a [Traverser].
 * Typically the [TraverserGenerator] chosen is determined by the [TraverserRequirement] of the [Traversal].
 * Simple requirements, simpler traversers. Complex requirements, complex traversers.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
interface TraverserGenerator {
    val providedRequirements: Set<Any?>?
    fun <S> generate(start: S, startStep: Step<S, *>?, initialBulk: Long): Traverser.Admin<S>?
    fun <S> generateIterator(
        starts: Iterator<S>,
        startStep: Step<S, *>?,
        initialBulk: Long
    ): Iterator<Traverser.Admin<S>?>? {
        return object : Iterator<Traverser.Admin<S>?>() {
            @Override
            override fun hasNext(): Boolean {
                return starts.hasNext()
            }

            @Override
            override fun next(): Traverser.Admin<S>? {
                return generate(starts.next(), startStep, initialBulk)
            }
        }
    }
}