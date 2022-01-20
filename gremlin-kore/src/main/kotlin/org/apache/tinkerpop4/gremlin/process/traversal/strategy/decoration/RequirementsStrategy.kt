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

import org.apache.tinkerpop4.gremlin.process.computer.traversal.strategy.decoration.VertexProgramStrategy

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class RequirementsStrategy private constructor() : AbstractTraversalStrategy<DecorationStrategy?>(),
    DecorationStrategy {
    private val requirements: Set<TraverserRequirement> = EnumSet.noneOf(TraverserRequirement::class.java)
    @Override
    fun apply(traversal: Traversal.Admin<*, *>) {
        if (traversal.isRoot() && !requirements.isEmpty()) traversal.addStep(RequirementsStep(traversal, requirements))
    }

    @Override
    fun applyPost(): Set<Class<out DecorationStrategy?>> {
        return Collections.singleton(VertexProgramStrategy::class.java)
    }

    companion object {
        fun addRequirements(traversalStrategies: TraversalStrategies, vararg requirements: TraverserRequirement?) {
            var strategy: RequirementsStrategy? =
                traversalStrategies.getStrategy(RequirementsStrategy::class.java).orElse(null)
            if (null == strategy) {
                strategy = RequirementsStrategy()
                traversalStrategies.addStrategies(strategy)
            } else {
                val cloneStrategy = RequirementsStrategy()
                cloneStrategy.requirements.addAll(strategy.requirements)
                strategy = cloneStrategy
                traversalStrategies.addStrategies(strategy)
            }
            Collections.addAll(strategy.requirements, requirements)
        }
    }
}