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
package org.apache.tinkerpop4.gremlin.process.traversal.strategy.optimization

import org.apache.tinkerpop4.gremlin.process.traversal.Step

/**
 * `OrderLimitStrategy` is an OLAP strategy that folds a [RangeGlobalStep] into a preceding
 * [OrderGlobalStep]. This helps to eliminate traversers early in the traversal and can
 * significantly reduce the amount of memory required by the OLAP execution engine.
 *
 * It's worth noting that certain steps are allowed between [OrderGlobalStep] and [RangeGlobalStep]:
 *
 *
 *
 *  * [IdStep]
 *  * [LabelStep]
 *  * [SackStep]
 *  * [SelectOneStep]
 *  * [SelectStep]
 *  * [PathStep]
 *  * [TreeStep]
 *
 *
 *
 *
 * These steps will be ignored by the `OrderLimitStrategy` and thus not affect its behavior.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class OrderLimitStrategy private constructor() : AbstractTraversalStrategy<OptimizationStrategy?>(),
    OptimizationStrategy {
    @Override
    fun apply(traversal: Traversal.Admin<*, *>?) {
        if (!TraversalHelper.onGraphComputer(traversal)) return
        val orders: List<OrderGlobalStep> = TraversalHelper.getStepsOfClass(OrderGlobalStep::class.java, traversal)
        for (order in orders) {
            var range: RangeGlobalStep? = null
            var currentStep: Step<*, *> = order.getNextStep()
            while (true) {
                if (currentStep is RangeGlobalStep) {
                    range = currentStep as RangeGlobalStep
                    break
                } else if (!LEGAL_STEPS.contains(currentStep.getClass())) break else currentStep =
                    currentStep.getNextStep()
            }
            if (null != range) order.setLimit(range.getHighRange())
        }
    }

    companion object {
        private val INSTANCE = OrderLimitStrategy()
        private val LEGAL_STEPS: Set<Class<out Step?>> = HashSet(
            Arrays.asList(
                LabelStep::class.java,
                IdStep::class.java,
                PathStep::class.java,
                SelectStep::class.java,
                SelectOneStep::class.java,
                SackStep::class.java,
                TreeStep::class.java
            )
        )

        fun instance(): OrderLimitStrategy {
            return INSTANCE
        }
    }
}