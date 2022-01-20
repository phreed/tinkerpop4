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
package org.apache.tinkerpop4.gremlin.structure.io.binary.types

import org.apache.tinkerpop4.gremlin.structure.io.binary.GraphBinaryReader

class TraversalExplanationSerializer : SimpleTypeSerializer<TraversalExplanation?>(null),
    TransformSerializer<TraversalExplanation?> {
    @Override
    @Throws(IOException::class)
    protected fun readValue(buffer: Buffer?, context: GraphBinaryReader?): TraversalExplanation {
        throw IOException("A TraversalExplanation should not be read individually")
    }

    @Override
    @Throws(IOException::class)
    protected fun writeValue(value: TraversalExplanation?, buffer: Buffer?, context: GraphBinaryWriter?) {
        throw IOException("A TraversalExplanation should not be written individually")
    }

    /**
     * Creates a Map containing "original", "intermediate" and "final" keys.
     */
    @Override
    fun transform(value: TraversalExplanation): Object {
        val result: Map<String, Object> = HashMap()
        result.put(ORIGINAL, getTraversalSteps(value.getOriginalTraversal()))
        val strategyTraversals: List<Pair<TraversalStrategy, Traversal.Admin<*, *>>> = value.getStrategyTraversals()
        result.put(
            INTERMEDIATE,
            strategyTraversals.stream().map { pair ->
                val item: Map<String?, Object> = HashMap()
                item.put(STRATEGY, pair.getValue0().toString())
                item.put(CATEGORY, pair.getValue0().getTraversalCategory().getSimpleName())
                item.put(TRAVERSAL, getTraversalSteps(pair.getValue1()))
                item
            }.collect(Collectors.toList())
        )
        result.put(
            FINAL,
            getTraversalSteps(if (strategyTraversals.isEmpty()) value.getOriginalTraversal() else strategyTraversals[strategyTraversals.size() - 1].getValue1())
        )
        return result
    }

    companion object {
        private const val ORIGINAL = "original"
        private const val FINAL = "final"
        private const val INTERMEDIATE = "intermediate"
        private const val CATEGORY = "category"
        private const val TRAVERSAL = "traversal"
        private const val STRATEGY = "strategy"
        private fun getTraversalSteps(t: Traversal.Admin<*, *>): List<String> {
            return t.getSteps().stream().map(Object::toString).collect(Collectors.toList())
        }
    }
}