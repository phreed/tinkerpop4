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
package org.apache.tinkerpop4.gremlin.process.traversal.strategy.decoration

import org.apache.commons.configuration2.Configuration

/**
 * A strategy that resets the specified `seed` value for [Seedable] steps, which in turn will produce
 * deterministic results from those steps. It is important to note that when using this strategy that it only
 * guarantees deterministic results from a step but not from an entire traversal. For example, if a graph does no
 * guarantee iteration order for `g.V()` then repeated runs of `g.V().coin(0.5)` with this strategy
 * will return the same number of results but not necessarily the same ones. The same problem can occur in OLAP-based
 * traversals where iteration order is not explicitly guaranteed. The only way to ensure completely deterministic
 * results in that sense is to apply some form of `order()` in these cases
 */
class SeedStrategy(val seed: Long) : AbstractTraversalStrategy<DecorationStrategy?>(), DecorationStrategy {
    @Override
    fun apply(traversal: Traversal.Admin<*, *>?) {
        val seedableSteps: List<Seedable> = TraversalHelper.getStepsOfAssignableClass(Seedable::class.java, traversal)
        for (seedableStepsToReset in seedableSteps) {
            seedableStepsToReset.resetSeed(seed)
        }
    }

    @get:Override
    val configuration: Configuration
        get() {
            val map: Map<String, Object> = HashMap()
            map.put(STRATEGY, SeedStrategy::class.java.getCanonicalName())
            map.put(ID_SEED, seed)
            return MapConfiguration(map)
        }

    companion object {
        const val ID_SEED = "seed"
        fun create(configuration: Configuration): SeedStrategy {
            if (!configuration.containsKey(ID_SEED)) throw IllegalArgumentException("SeedStrategy configuration requires a 'seed' value")
            return SeedStrategy(Long.parseLong(configuration.getProperty(ID_SEED).toString()))
        }
    }
}