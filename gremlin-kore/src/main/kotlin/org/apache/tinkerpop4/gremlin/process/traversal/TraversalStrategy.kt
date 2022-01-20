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

import org.apache.commons.configuration2.BaseConfiguration

/**
 * A [TraversalStrategy] defines a particular atomic operation for mutating a [Traversal] prior to its evaluation.
 * There are 5 pre-defined "traversal categories": [DecorationStrategy], [OptimizationStrategy], [ProviderOptimizationStrategy], [FinalizationStrategy], and [VerificationStrategy].
 * Strategies within a category are sorted amongst themselves and then category sorts are applied in the ordered specified previous.
 * That is, decorations are applied, then optimizations, then provider optimizations, then finalizations, and finally, verifications.
 * If a strategy does not fit within the specified categories, then it can simply implement [TraversalStrategy] and can have priors/posts that span categories.
 *
 *
 * A traversal strategy should be a final class as various internal operations on a strategy are based on its ability to be assigned to more general classes.
 * A traversal strategy should typically be stateless with a public static `instance()` method.
 * However, at limit, a traversal strategy can have a state defining constructor (typically via a "builder"), but that state can not mutate once instantiated.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Matthias Broecheler (me@matthiasb.com)
 */
interface TraversalStrategy<S : TraversalStrategy<*>?> : Serializable, Comparable<Class<out TraversalStrategy<*>?>?> {
    fun apply(traversal: Traversal.Admin<*, *>?)

    /**
     * The set of strategies that must be executed before this strategy is executed.
     * If there are no ordering requirements, the default implementation returns an empty set.
     *
     * @return the set of strategies that must be executed prior to this one.
     */
    fun applyPrior(): Set<Class<out S>?>? {
        return Collections.emptySet()
    }

    /**
     * The set of strategies that must be executed after this strategy is executed.
     * If there are no ordering requirements, the default implementation returns an empty set.
     *
     * @return the set of strategies that must be executed post this one
     */
    fun applyPost(): Set<Class<out S>?>? {
        return Collections.emptySet()
    }

    /**
     * The type of traversal strategy -- i.e. [DecorationStrategy], [OptimizationStrategy], [FinalizationStrategy], or [VerificationStrategy].
     *
     * @return the traversal strategy category class
     */
    val traversalCategory: Class<S>?
        get() = TraversalStrategy::class.java as Class?

    /**
     * Get the configuration representation of this strategy.
     * This is useful for converting a strategy into a serialized form.
     *
     * @return the configuration used to create this strategy
     */
    val configuration: Configuration?
        get() = BaseConfiguration()

    @Override
    override fun compareTo(otherTraversalCategory: Class<out TraversalStrategy<*>?>?): Int {
        return 0
    }

    /**
     * Implemented by strategies that adds "application logic" to the traversal (e.g. [PartitionStrategy]).
     */
    interface DecorationStrategy : TraversalStrategy<DecorationStrategy?> {
        @Override
        override fun getTraversalCategory(): Class<DecorationStrategy> {
            return DecorationStrategy::class.java
        }

        @Override
        override fun compareTo(otherTraversalCategory: Class<out TraversalStrategy<*>?>): Int {
            return if (otherTraversalCategory.equals(DecorationStrategy::class.java)) 0 else if (otherTraversalCategory.equals(
                    OptimizationStrategy::class.java
                )
            ) -1 else if (otherTraversalCategory.equals(
                    ProviderOptimizationStrategy::class.java
                )
            ) -1 else if (otherTraversalCategory.equals(
                    FinalizationStrategy::class.java
                )
            ) -1 else if (otherTraversalCategory.equals(VerificationStrategy::class.java)) -1 else 0
        }
    }

    /**
     * Implemented by strategies that rewrite the traversal to be more efficient, but with the same semantics
     * (e.g. [CountStrategy]). During a re-write ONLY TinkerPop steps should be used.
     * For strategies that utilize provider specific steps, use [ProviderOptimizationStrategy].
     */
    interface OptimizationStrategy : TraversalStrategy<OptimizationStrategy?> {
        @Override
        override fun getTraversalCategory(): Class<OptimizationStrategy> {
            return OptimizationStrategy::class.java
        }

        @Override
        override fun compareTo(otherTraversalCategory: Class<out TraversalStrategy<*>?>): Int {
            return if (otherTraversalCategory.equals(DecorationStrategy::class.java)) 1 else if (otherTraversalCategory.equals(
                    OptimizationStrategy::class.java
                )
            ) 0 else if (otherTraversalCategory.equals(
                    ProviderOptimizationStrategy::class.java
                )
            ) -1 else if (otherTraversalCategory.equals(
                    FinalizationStrategy::class.java
                )
            ) -1 else if (otherTraversalCategory.equals(VerificationStrategy::class.java)) -1 else 0
        }
    }

    /**
     * Implemented by strategies that rewrite the traversal to be more efficient, but with the same semantics.
     * This is for graph system/language/driver providers that want to rewrite a traversal using provider specific steps.
     */
    interface ProviderOptimizationStrategy : TraversalStrategy<ProviderOptimizationStrategy?> {
        @Override
        override fun getTraversalCategory(): Class<ProviderOptimizationStrategy> {
            return ProviderOptimizationStrategy::class.java
        }

        @Override
        override fun compareTo(otherTraversalCategory: Class<out TraversalStrategy<*>?>): Int {
            return if (otherTraversalCategory.equals(DecorationStrategy::class.java)) 1 else if (otherTraversalCategory.equals(
                    OptimizationStrategy::class.java
                )
            ) 1 else if (otherTraversalCategory.equals(
                    ProviderOptimizationStrategy::class.java
                )
            ) 0 else if (otherTraversalCategory.equals(
                    FinalizationStrategy::class.java
                )
            ) -1 else if (otherTraversalCategory.equals(VerificationStrategy::class.java)) -1 else 0
        }
    }

    /**
     * Implemented by strategies that do final behaviors that require a fully compiled traversal to work (e.g.
     * [ProfileStrategy]).
     */
    interface FinalizationStrategy : TraversalStrategy<FinalizationStrategy?> {
        @Override
        override fun getTraversalCategory(): Class<FinalizationStrategy> {
            return FinalizationStrategy::class.java
        }

        @Override
        override fun compareTo(otherTraversalCategory: Class<out TraversalStrategy<*>?>): Int {
            return if (otherTraversalCategory.equals(DecorationStrategy::class.java)) 1 else if (otherTraversalCategory.equals(
                    OptimizationStrategy::class.java
                )
            ) 1 else if (otherTraversalCategory.equals(
                    ProviderOptimizationStrategy::class.java
                )
            ) 1 else if (otherTraversalCategory.equals(
                    FinalizationStrategy::class.java
                )
            ) 0 else if (otherTraversalCategory.equals(VerificationStrategy::class.java)) -1 else 0
        }
    }

    /**
     * Implemented by strategies where there is no more behavioral tweaking of the traversal required.  Strategies that
     * implement this category will simply analyze the traversal and throw exceptions if the traversal is not correct
     * for the execution context (e.g. [LambdaRestrictionStrategy]).
     */
    interface VerificationStrategy : TraversalStrategy<VerificationStrategy?> {
        @Override
        override fun getTraversalCategory(): Class<VerificationStrategy> {
            return VerificationStrategy::class.java
        }

        @Override
        override fun compareTo(otherTraversalCategory: Class<out TraversalStrategy<*>?>): Int {
            return if (otherTraversalCategory.equals(DecorationStrategy::class.java)) 1 else if (otherTraversalCategory.equals(
                    OptimizationStrategy::class.java
                )
            ) 1 else if (otherTraversalCategory.equals(
                    ProviderOptimizationStrategy::class.java
                )
            ) 1 else if (otherTraversalCategory.equals(
                    FinalizationStrategy::class.java
                )
            ) 1 else 0
        }
    }

    companion object {
        const val STRATEGY = "strategy"
    }
}