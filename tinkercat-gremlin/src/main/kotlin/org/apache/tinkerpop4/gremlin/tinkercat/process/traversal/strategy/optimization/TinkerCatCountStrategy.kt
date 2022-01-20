/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.tinkerpop4.gremlin.tinkercat.process.traversal.strategy.optimization

import org.apache.tinkerpop4.gremlin.process.traversal.Step
import org.apache.tinkerpop4.gremlin.process.traversal.Traversal
import org.apache.tinkerpop4.gremlin.process.traversal.strategy.AbstractTraversalStrategy
import org.apache.tinkerpop4.gremlin.process.traversal.TraversalStrategy.ProviderOptimizationStrategy
import org.apache.tinkerpop4.gremlin.process.traversal.step.map.GraphStep
import org.apache.tinkerpop4.gremlin.process.traversal.step.map.CountGlobalStep
import org.apache.tinkerpop4.gremlin.process.traversal.step.sideEffect.IdentityStep
import org.apache.tinkerpop4.gremlin.process.traversal.step.map.NoOpBarrierStep
import org.apache.tinkerpop4.gremlin.process.traversal.step.util.CollectingBarrierStep
import org.apache.tinkerpop4.gremlin.process.traversal.step.TraversalParent
import org.apache.tinkerpop4.gremlin.process.traversal.step.sideEffect.SideEffectStep
import org.apache.tinkerpop4.gremlin.process.traversal.step.sideEffect.AggregateGlobalStep
import org.apache.tinkerpop4.gremlin.process.traversal.util.TraversalHelper
import org.apache.tinkerpop4.gremlin.structure.Element
import org.apache.tinkerpop4.gremlin.tinkercat.process.traversal.step.map.TinkerCountGlobalStep

/**
 * This strategy will do a direct [org.apache.tinkerpop4.gremlin.tinkercat.structure.TinkerHelper.getVertices]
 * size call if the traversal is a count of the vertices and edges of the graph or a one-to-one map chain thereof.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @example <pre>
 * g.V().count()               // is replaced by TinkerCountGlobalStep
 * g.V().map(out()).count()    // is replaced by TinkerCountGlobalStep
 * g.E().label().count()       // is replaced by TinkerCountGlobalStep
</pre> *
 */
class TinkerCatCountStrategy private constructor() : AbstractTraversalStrategy<ProviderOptimizationStrategy?>(),
    ProviderOptimizationStrategy {
    override fun apply(traversal: Traversal.Admin<*, *>) {
        if (!traversal.isRoot || TraversalHelper.onGraphComputer(traversal)) return
        val steps = traversal.steps
        if (steps.size < 2 ||
            steps[0] !is GraphStep<*, *> || 0 != (steps[0] as GraphStep<*, *>).ids.size ||
            steps[steps.size - 1] !is CountGlobalStep<*>
        ) return
        for (i in 1 until steps.size - 1) {
            val current = steps[i]
            if (!( //current instanceof MapStep ||  // MapSteps will not necessarily emit an element as demonstrated in https://issues.apache.org/jira/browse/TINKERPOP-1958
                        current is IdentityStep<*> ||
                                current is NoOpBarrierStep<*> ||
                                current is CollectingBarrierStep<*>) ||
                current is TraversalParent &&
                TraversalHelper.anyStepRecursively(
                    { s: Step<*, *>? -> s is SideEffectStep<*> || s is AggregateGlobalStep<*> },
                    current as TraversalParent
                )
            ) return
        }
        val elementClass = (steps[0] as GraphStep<*, *>).returnClass
        TraversalHelper.removeAllSteps(traversal)
        traversal.addStep(TinkerCountGlobalStep(traversal, elementClass as Class<Element?>))
    }

    override fun applyPost(): Set<Class<out ProviderOptimizationStrategy>> {
        return setOf<Class<out ProviderOptimizationStrategy>>(TinkerCatStepStrategy::class.java)
    }

    companion object {
        private val INSTANCE = TinkerCatCountStrategy()
        @JvmStatic
        fun instance(): TinkerCatCountStrategy {
            return INSTANCE
        }
    }
}