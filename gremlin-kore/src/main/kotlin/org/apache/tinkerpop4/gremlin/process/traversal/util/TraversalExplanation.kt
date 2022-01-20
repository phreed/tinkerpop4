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
import org.apache.tinkerpop4.gremlin.process.traversal.TraversalStrategies
import org.apache.tinkerpop4.gremlin.process.traversal.TraversalStrategy
import org.javatuples.Pair
import org.javatuples.Triplet
import java.io.Serializable
import java.util.ArrayList
import java.util.Collections
import java.util.List
import java.util.stream.Collectors
import java.util.stream.Stream

/**
 * A TraversalExplanation takes a [Traversal] and, for each registered [TraversalStrategy], it creates a
 * mapping reflecting how each strategy alters the traversal. This is useful for understanding how each traversal
 * strategy mutates the traversal. This is useful in debugging and analysis of traversal compilation. The
 * [TraversalExplanation.toString] has a pretty-print representation that is useful in the Gremlin Console.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class TraversalExplanation : AbstractExplanation, Serializable {
    protected var traversal: Traversal.Admin<*, *>? = null
    protected var strategyTraversals: List<Pair<TraversalStrategy, Traversal.Admin<*, *>>> = ArrayList()

    protected constructor() {
        // no arg constructor for serialization
    }

    constructor(traversal: Traversal.Admin<*, *>) {
        this.traversal = traversal.clone()
        val mutatingStrategies: TraversalStrategies = DefaultTraversalStrategies()
        for (strategy in this.traversal.getStrategies()) {
            val mutatingTraversal: Traversal.Admin<*, *> = this.traversal.clone()
            mutatingStrategies.addStrategies(strategy)
            mutatingTraversal.setStrategies(mutatingStrategies)
            mutatingTraversal.applyStrategies()
            strategyTraversals.add(Pair.with(strategy, mutatingTraversal))
        }
    }

    /**
     * Get the list of [TraversalStrategy] applications. For strategy, the resultant mutated [Traversal] is provided.
     *
     * @return the list of strategy/traversal pairs
     */
    fun getStrategyTraversals(): List<Pair<TraversalStrategy, Traversal.Admin<*, *>>> {
        return Collections.unmodifiableList(strategyTraversals)
    }

    /**
     * Get the original [Traversal] used to create this explanation.
     *
     * @return the original traversal
     */
    val originalTraversal: Traversal.Admin<*, *>
        get() = traversal

    fun asImmutable(): ImmutableExplanation {
        return ImmutableExplanation(
            originalTraversalAsString,
            intermediates.collect(Collectors.toList())
        )
    }

    @get:Override
    protected val strategyTraversalsAsString: Stream<String>
        protected get() = strategyTraversals.stream()
            .map(Pair::getValue0)
            .map(Object::toString)

    @get:Override
    protected val originalTraversalAsString: String
        protected get() = originalTraversal.toString()

    @get:Override
    protected val intermediates: Stream<Triplet<String, String, String>>
        protected get() = strategyTraversals.stream().map { p ->
            Triplet.with(
                p.getValue0().toString(),
                p.getValue0().getTraversalCategory().getSimpleName(), p.getValue1().toString()
            )
        }
}