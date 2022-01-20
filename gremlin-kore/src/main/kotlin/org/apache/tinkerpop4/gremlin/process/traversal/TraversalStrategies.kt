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

import org.apache.tinkerpop4.gremlin.process.computer.GraphComputer

/**
 * A [Traversal] maintains a set of [TraversalStrategy] instances within a TraversalStrategies object.
 * TraversalStrategies are responsible for compiling a traversal prior to its execution.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Matthias Broecheler (me@matthiasb.com)
 */
interface TraversalStrategies : Serializable, Cloneable, Iterable<TraversalStrategy<*>?> {
    /**
     * Return an immutable list of the [TraversalStrategy] instances.
     */
    fun toList(): List<TraversalStrategy<*>?>? {
        return Collections.unmodifiableList(IteratorUtils.list(iterator()))
    }

    /**
     * Return an `Iterator` of the [TraversalStrategy] instances.
     */
    @Override
    override fun iterator(): Iterator<TraversalStrategy<*>?>?

    /**
     * Return the [TraversalStrategy] instance associated with the provided class.
     *
     * @param traversalStrategyClass the class of the strategy to get
     * @param <T>                    the strategy class type
     * @return an optional containing the strategy instance or not
    </T> */
    fun <T : TraversalStrategy?> getStrategy(traversalStrategyClass: Class<T>): Optional<T>? {
        return IteratorUtils.stream(iterator()).filter { s -> traversalStrategyClass.isAssignableFrom(s.getClass()) }
            .findAny() as Optional<T>
    }

    /**
     * Add all the provided [TraversalStrategy] instances to the current collection. When all the provided
     * strategies have been added, the collection is resorted. If a strategy class is found to already be defined, it
     * is removed and replaced by the newly added one.
     *
     * @param strategies the traversal strategies to add
     * @return the newly updated/sorted traversal strategies collection
     */
    fun addStrategies(vararg strategies: TraversalStrategy<*>?): TraversalStrategies?

    /**
     * Remove all the provided [TraversalStrategy] classes from the current collection.
     * When all the provided strategies have been removed, the collection is resorted.
     *
     * @param strategyClasses the traversal strategies to remove by their class
     * @return the newly updated/sorted traversal strategies collection
     */
    @SuppressWarnings(["unchecked", "varargs"])
    fun removeStrategies(vararg strategyClasses: Class<out TraversalStrategy?>?): TraversalStrategies?

    @SuppressWarnings("CloneDoesntDeclareCloneNotSupportedException")
    fun clone(): TraversalStrategies?

    object GlobalCache {
        /**
         * Keeps track of [GraphComputer] and/or [Graph] classes that have been initialized to the
         * classloader so that they do not have to be reflected again.
         */
        private val LOADED: Set<Class<*>> = ConcurrentHashMap.newKeySet()
        private val GRAPH_CACHE: Map<Class<out Graph?>, TraversalStrategies> = HashMap()
        private val GRAPH_COMPUTER_CACHE: Map<Class<out GraphComputer?>, TraversalStrategies> = HashMap()

        init {
            val graphStrategies: TraversalStrategies = DefaultTraversalStrategies()
            org.apache.tinkerpop4.gremlin.process.traversal.graphStrategies.addStrategies(
                IdentityRemovalStrategy.instance(),
                ConnectiveStrategy.instance(),
                EarlyLimitStrategy.instance(),
                InlineFilterStrategy.instance(),
                IncidentToAdjacentStrategy.instance(),
                AdjacentToIncidentStrategy.instance(),
                ByModulatorOptimizationStrategy.instance(),
                FilterRankingStrategy.instance(),
                MatchPredicateStrategy.instance(),
                RepeatUnrollStrategy.instance(),
                CountStrategy.instance(),
                PathRetractionStrategy.instance(),
                LazyBarrierStrategy.instance(),
                ProfileStrategy.instance(),
                StandardVerificationStrategy.instance()
            )
            GRAPH_CACHE.put(Graph::class.java, org.apache.tinkerpop4.gremlin.process.traversal.graphStrategies)
            GRAPH_CACHE.put(EmptyGraph::class.java, DefaultTraversalStrategies())

            /////////////////////
            val graphComputerStrategies: TraversalStrategies = DefaultTraversalStrategies()
            org.apache.tinkerpop4.gremlin.process.traversal.graphComputerStrategies.addStrategies(
                GraphFilterStrategy.instance(),
                MessagePassingReductionStrategy.instance(),
                OrderLimitStrategy.instance(),
                PathProcessorStrategy.instance(),
                ComputerFinalizationStrategy.instance(),
                ComputerVerificationStrategy.instance()
            )
            GRAPH_COMPUTER_CACHE.put(
                GraphComputer::class.java,
                org.apache.tinkerpop4.gremlin.process.traversal.graphComputerStrategies
            )
        }

        fun registerStrategies(graphOrGraphComputerClass: Class, traversalStrategies: TraversalStrategies?) {
            if (Graph::class.java.isAssignableFrom(graphOrGraphComputerClass)) GRAPH_CACHE.put(
                graphOrGraphComputerClass,
                traversalStrategies
            ) else if (GraphComputer::class.java.isAssignableFrom(graphOrGraphComputerClass)) GRAPH_COMPUTER_CACHE.put(
                graphOrGraphComputerClass,
                traversalStrategies
            ) else throw IllegalArgumentException("The TraversalStrategies.GlobalCache only supports Graph and GraphComputer strategy caching: " + graphOrGraphComputerClass.getCanonicalName())
        }

        fun getStrategies(graphOrGraphComputerClass: Class): TraversalStrategies? {
            try {
                // be sure to load the class so that its static{} traversal strategy registration component is loaded.
                // this is more important for GraphComputer classes as they are typically not instantiated prior to
                // strategy usage like Graph classes.
                if (!LOADED.contains(graphOrGraphComputerClass)) {
                    val graphComputerClassName: String =
                        if (null != graphOrGraphComputerClass.getDeclaringClass()) graphOrGraphComputerClass.getCanonicalName()
                            .replace(
                                "." + graphOrGraphComputerClass.getSimpleName(),
                                "$" + graphOrGraphComputerClass.getSimpleName()
                            ) else graphOrGraphComputerClass.getCanonicalName()
                    Class.forName(graphComputerClassName)

                    // keep track of stuff we already loaded once - stuff in this if/statement isn't cheap and this
                    // method gets called a lot, basically every time a new traversal gets spun up (that includes
                    // child traversals. perhaps it is possible to just check the cache keys for this information, but
                    // it's not clear if this method will be called with something not in the cache and if it is and
                    // it results in error, then we'd probably not want to deal with this block again anyway
                    LOADED.add(graphOrGraphComputerClass)
                }
            } catch (e: ClassNotFoundException) {
                throw IllegalStateException(e.getMessage(), e)
            }
            return if (GRAPH_CACHE.containsKey(graphOrGraphComputerClass)) {
                GRAPH_CACHE[graphOrGraphComputerClass]
            } else if (Graph::class.java.isAssignableFrom(graphOrGraphComputerClass)) {
                GRAPH_CACHE[Graph::class.java]
            } else if (GRAPH_COMPUTER_CACHE.containsKey(graphOrGraphComputerClass)) {
                GRAPH_COMPUTER_CACHE[graphOrGraphComputerClass]
            } else if (GraphComputer::class.java.isAssignableFrom(graphOrGraphComputerClass)) {
                GRAPH_COMPUTER_CACHE[GraphComputer::class.java]
            } else {
                throw IllegalArgumentException("The TraversalStrategies.GlobalCache only supports Graph and GraphComputer strategy caching: " + graphOrGraphComputerClass.getCanonicalName())
            }
        }
    }

    companion object {
        val STRATEGY_CATEGORIES: List<Class<out TraversalStrategy?>> = Collections.unmodifiableList(
            Arrays.asList(
                DecorationStrategy::class.java,
                OptimizationStrategy::class.java,
                ProviderOptimizationStrategy::class.java,
                FinalizationStrategy::class.java,
                TraversalStrategy.VerificationStrategy::class.java
            )
        )

        /**
         * Sorts the list of provided strategies such that the [TraversalStrategy.applyPost]
         * and [TraversalStrategy.applyPrior] dependencies are respected.
         *
         *
         * Note, that the order may not be unique.
         *
         * @param strategies the traversal strategies to sort
         */
        fun sortStrategies(strategies: Set<TraversalStrategy<*>?>): Set<TraversalStrategy<*>?>? {
            val dependencyMap: Map<Class<out TraversalStrategy?>?, Set<Class<out TraversalStrategy?>>> = HashMap()
            val strategiesByCategory: Map<Class<out TraversalStrategy?>, Set<Class<out TraversalStrategy?>>> = HashMap()
            val strategyClasses: Set<Class<out TraversalStrategy?>> = HashSet(strategies.size())
            //Initialize data structure
            strategies.forEach { s ->
                strategyClasses.add(s.getClass())
                MultiMap.put(strategiesByCategory, s.getTraversalCategory(), s.getClass())
            }

            //Initialize all the dependencies
            strategies.forEach { strategy ->
                strategy.applyPrior().forEach { s ->
                    if (strategyClasses.contains(s)) MultiMap.put(
                        dependencyMap,
                        strategy.getClass(),
                        s
                    )
                }
                strategy.applyPost().forEach { s ->
                    if (strategyClasses.contains(s)) MultiMap.put(
                        dependencyMap,
                        s,
                        strategy.getClass()
                    )
                }
            }

            //Add dependencies by category
            val strategiesInPreviousCategories: List<Class<out TraversalStrategy?>> = ArrayList()
            for (category in STRATEGY_CATEGORIES) {
                val strategiesInThisCategory: Set<Class<out TraversalStrategy?>> =
                    MultiMap.get(strategiesByCategory, category)
                for (strategy in strategiesInThisCategory) {
                    for (previousStrategy in strategiesInPreviousCategories) {
                        MultiMap.put(dependencyMap, strategy, previousStrategy)
                    }
                }
                strategiesInPreviousCategories.addAll(strategiesInThisCategory)
            }

            //Finally sort via t-sort
            val unprocessedStrategyClasses: List<Class<out TraversalStrategy?>> =
                ArrayList(strategies.stream().map { s -> s.getClass() }
                    .collect(Collectors.toSet()))
            val sortedStrategyClasses: List<Class<out TraversalStrategy?>> = ArrayList()
            val seenStrategyClasses: Set<Class<out TraversalStrategy?>> = HashSet()
            while (!unprocessedStrategyClasses.isEmpty()) {
                val strategy: Class<out TraversalStrategy?> = unprocessedStrategyClasses[0]
                visit(dependencyMap, sortedStrategyClasses, seenStrategyClasses, unprocessedStrategyClasses, strategy)
            }
            val sortedStrategies: Set<TraversalStrategy<*>> = LinkedHashSet()
            //We now have a linked set of sorted strategy classes
            for (strategyClass in sortedStrategyClasses) {
                for (strategy in strategies) {
                    if (strategy.getClass().equals(strategyClass)) {
                        sortedStrategies.add(strategy)
                    }
                }
            }
            return sortedStrategies
        }

        fun visit(
            dependencyMap: Map<Class<out TraversalStrategy?>?, Set<Class<out TraversalStrategy?>?>?>?,
            sortedStrategyClasses: List<Class<out TraversalStrategy?>?>,
            seenStrategyClases: Set<Class<out TraversalStrategy?>?>,
            unprocessedStrategyClasses: List<Class<out TraversalStrategy?>?>,
            strategyClass: Class<out TraversalStrategy?>?
        ) {
            if (seenStrategyClases.contains(strategyClass)) {
                throw IllegalStateException(
                    "Cyclic dependency between traversal strategies: ["
                            + seenStrategyClases + ']'
                )
            }
            if (unprocessedStrategyClasses.contains(strategyClass)) {
                seenStrategyClases.add(strategyClass)
                for (dependency in MultiMap.get(dependencyMap, strategyClass)) {
                    visit(
                        dependencyMap,
                        sortedStrategyClasses,
                        seenStrategyClases,
                        unprocessedStrategyClasses,
                        dependency
                    )
                }
                seenStrategyClases.remove(strategyClass)
                unprocessedStrategyClasses.remove(strategyClass)
                sortedStrategyClasses.add(strategyClass)
            }
        }
    }
}