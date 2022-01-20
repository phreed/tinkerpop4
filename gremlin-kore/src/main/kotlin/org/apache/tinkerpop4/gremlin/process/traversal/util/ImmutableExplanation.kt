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

import org.apache.tinkerpop4.gremlin.process.traversal.Traversal

/**
 * A data-only representation of a [TraversalExplanation] which doesn't re-calculate the "explanation" from
 * the raw traversal data each time the explanation is displayed.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class ImmutableExplanation(
    @get:Override
    protected val originalTraversalAsString: String,
    intermediates: List<Triplet<String?, String?, String?>?>
) : TraversalExplanation() {
    private val intermediates: List<Triplet<String, String, String>>

    init {
        this.intermediates = intermediates
    }

    @get:Override
    val strategyTraversals: List<Any>
        get() {
            throw UnsupportedOperationException("This instance is immutable")
        }

    @Override
    fun getOriginalTraversal(): Traversal.Admin<*, *> {
        throw UnsupportedOperationException("This instance is immutable")
    }

    @Override
    fun asImmutable(): ImmutableExplanation {
        return this
    }

    @get:Override
    protected val strategyTraversalsAsString: Stream<String>
        protected get() = getIntermediates().map(Triplet::getValue0)

    @get:Override
    protected val traversalStepsAsString: Stream<String>
        protected get() = Stream.concat(
            Stream.of(originalTraversalAsString),
            getIntermediates().map(Triplet::getValue2)
        )

    @Override
    protected fun getIntermediates(): Stream<Triplet<String, String, String>> {
        return intermediates.stream()
    }
}