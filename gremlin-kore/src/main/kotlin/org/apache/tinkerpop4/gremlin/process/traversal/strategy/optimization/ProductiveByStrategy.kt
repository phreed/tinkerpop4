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

import org.apache.commons.configuration2.Configuration

/**
 * Takes an argument of `by()` and wraps it [CoalesceStep] so that the result is either the initial
 * [Traversal] argument or `null`. In this way, the `by()` is always "productive". This strategy
 * is an "optimization" but it is perhaps more of a "decoration", but it should follow
 * [ByModulatorOptimizationStrategy] which features optimizations relevant to this one.
 */
class ProductiveByStrategy private constructor(private val productiveKeys: List<String>) :
    AbstractTraversalStrategy<OptimizationStrategy?>(), OptimizationStrategy {
    @Override
    fun applyPrior(): Set<Class<out OptimizationStrategy?>> {
        return PRIORS
    }

    @Override
    fun apply(traversal: Traversal.Admin<*, *>?) {
        // ByModulating steps should also be TraversalParent in most cases - if they aren't this strategy
        // probably doesn't need to fuss with them
        TraversalHelper.getStepsOfAssignableClass(ByModulating::class.java, traversal).stream()
            .filter { bm -> bm is TraversalParent }
            .forEach { bm ->
                val parentStep: TraversalParent = bm as TraversalParent
                parentStep.getLocalChildren().forEach { child ->
                    if (child is ValueTraversal && !containsValidByPass(child as ValueTraversal) &&
                        hasKeyNotKnownAsProductive(child as ValueTraversal)
                    ) {
                        wrapValueTraversalInCoalesce(parentStep, child)
                    } else if (child.getEndStep() !is ReducingBarrierStep) {
                        // ending reducing barrier will always return something so seems safe to not bother wrapping
                        // that up in coalesce().
                        val extractedChildTraversal: Traversal.Admin = DefaultGraphTraversal()
                        TraversalHelper.removeToTraversal(
                            child.getStartStep(),
                            EmptyStep.instance(),
                            extractedChildTraversal
                        )
                        child.addStep(CoalesceStep(child, extractedChildTraversal, nullTraversal))

                        // replace so that internally the parent step gets to re-initialize the child as it may need to.
                        try {
                            parentStep.replaceLocalChild(child, child)
                        } catch (ignored: IllegalStateException) {
                            // ignore situations where the parent traversal doesn't support replacement. in those cases
                            // we simply retain whatever the original behavior was even if it is inconsistent
                        }
                    }
                }
            }
    }

    /**
     * Validate that the [ValueTraversal] needs to be wrapped. It can be skipped if a bypass is already in place
     * or if there isn't a [CoalesceStep] at the start or if there isn't a `null` in the
     * [CoalesceStep].
     */
    private fun containsValidByPass(vt: ValueTraversal): Boolean {
        if (null == vt.getBypassTraversal()) return false
        if (vt.getStartStep() !is CoalesceStep) return false
        val coalesceStep: CoalesceStep = vt.getStartStep() as CoalesceStep
        val children: List<Traversal> = coalesceStep.getLocalChildren()
        val lastChild: Traversal = children[children.size() - 1]
        return lastChild === nullTraversal ||
                lastChild is ConstantTraversal && (lastChild as ConstantTraversal).next() == null ||
                lastChild.asAdmin().getEndStep() is ConstantStep && (lastChild.asAdmin()
            .getEndStep() as ConstantStep).getConstant() == null
    }

    private fun wrapValueTraversalInCoalesce(parentStep: TraversalParent, child: Traversal.Admin<Object?, Object?>) {
        val temp: Traversal.Admin = DefaultGraphTraversal()
        temp.addStep(CoalesceStep(temp, child.clone(), nullTraversal))
        temp.setParent(parentStep)
        (child as ValueTraversal<Object?, Object?>).setBypassTraversal(temp)
    }

    /**
     * Determines if the [ValueTraversal] references a productive key.
     */
    private fun hasKeyNotKnownAsProductive(child: ValueTraversal): Boolean {
        return productiveKeys.isEmpty() ||
                child.getBypassTraversal() == null && !productiveKeys.contains(child.getPropertyKey())
    }

    @get:Override
    val configuration: Configuration
        get() {
            val map: Map<String, Object> = HashMap()
            map.put(STRATEGY, ProductiveByStrategy::class.java.getCanonicalName())
            map.put(PRODUCTIVE_KEYS, productiveKeys)
            return MapConfiguration(map)
        }

    class Builder private constructor() {
        private val productiveKeys: ArrayList<String> = ArrayList()

        /**
         * Specify the list of property keys that should always be productive for `by(String)`. If the keys are
         * not set, then all `by(String)` are productive. Arguments to `by()` that are not of type
         * [ValueTraversal] will not be considered.
         */
        fun productiveKeys(key: String?, vararg rest: String?): Builder {
            productiveKeys.clear()
            productiveKeys.add(key)
            productiveKeys.addAll(Arrays.asList(rest))
            return this
        }

        /**
         * Specify the list of property keys that should always be productive for `by(String)`. If the keys are
         * not set, then all `by(String)` are productive.  Arguments to `by()` that are not of type
         * [ValueTraversal] will not be considered.
         */
        fun productiveKeys(keys: Collection<String?>?): Builder {
            productiveKeys.clear()
            productiveKeys.addAll(keys)
            return this
        }

        fun create(): ProductiveByStrategy {
            return ProductiveByStrategy(productiveKeys)
        }
    }

    companion object {
        const val PRODUCTIVE_KEYS = "productiveKeys"
        private val INSTANCE = ProductiveByStrategy(Collections.emptyList())
        private val nullTraversal: ConstantTraversal<*, *> = ConstantTraversal(null)
        private val PRIORS: Set<Class<out OptimizationStrategy?>> = HashSet(
            Arrays.asList(
                ByModulatorOptimizationStrategy::class.java
            )
        )

        fun create(configuration: Configuration): ProductiveByStrategy {
            return ProductiveByStrategy(ArrayList(configuration.getProperty(PRODUCTIVE_KEYS) as Collection<String?>))
        }

        /**
         * Gets the standard configuration of this strategy that will apply it for all conditions. It is this version of
         * the strategy that is added as standard. Note that it may be helpful to configure a custom instance using the
         * [.build] method in cases where there is certainty that a `by()` will be productive as it will
         * reduce the complexity of the traversal and perhaps improve the execution of other optimizations.
         */
        fun instance(): ProductiveByStrategy {
            return INSTANCE
        }

        fun build(): Builder {
            return Builder()
        }
    }
}