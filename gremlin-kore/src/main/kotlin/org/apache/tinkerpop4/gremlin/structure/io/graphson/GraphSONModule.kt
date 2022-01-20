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
package org.apache.tinkerpop4.gremlin.structure.io.graphson

import org.apache.tinkerpop4.gremlin.process.computer.traversal.strategy.decoration.VertexProgramStrategy

/**
 * The set of serializers that handle the core graph interfaces.  These serializers support normalization which
 * ensures that generated GraphSON will be compatible with line-based versioning tools. This setting comes with
 * some overhead, with respect to key sorting and other in-memory operations.
 *
 *
 * This is a base class for grouping these core serializers.  Concrete extensions of this class represent a "version"
 * that should be registered with the [GraphSONVersion] enum.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
internal abstract class GraphSONModule(name: String?) : TinkerPopJacksonModule(name) {
    /**
     * Version 3.0 of GraphSON.
     */
    internal class GraphSONModuleV3d0 protected constructor(normalize: Boolean) : GraphSONModule("graphson-3.0") {
        /**
         * Constructs a new object.
         */
        init {

            /////////////////////// SERIALIZERS ////////////////////////////

            // graph
            addSerializer(Edge::class.java, EdgeJacksonSerializer(normalize))
            addSerializer(Vertex::class.java, VertexJacksonSerializer(normalize))
            addSerializer(VertexProperty::class.java, VertexPropertyJacksonSerializer(normalize, true))
            addSerializer(Property::class.java, PropertyJacksonSerializer())
            addSerializer(Metrics::class.java, MetricsJacksonSerializer())
            addSerializer(TraversalMetrics::class.java, TraversalMetricsJacksonSerializer())
            addSerializer(TraversalExplanation::class.java, TraversalExplanationJacksonSerializer())
            addSerializer(Path::class.java, PathJacksonSerializer())
            addSerializer(DirectionalStarGraph::class.java, StarGraphGraphSONSerializerV3d0(normalize))
            addSerializer(Tree::class.java, TreeJacksonSerializer())

            // java.util
            addSerializer(Map.Entry::class.java, MapEntryJacksonSerializer())
            addSerializer(Map::class.java, MapJacksonSerializer())
            addSerializer(List::class.java, ListJacksonSerializer())
            addSerializer(Set::class.java, SetJacksonSerializer())

            // need to explicitly add serializers for those types because Jackson doesn't do it at all.
            addSerializer(Integer::class.java, IntegerGraphSONSerializer())
            addSerializer(Double::class.java, DoubleGraphSONSerializer())

            // traversal
            addSerializer(BulkSet::class.java, BulkSetJacksonSerializer())
            addSerializer(Traversal::class.java, TraversalJacksonSerializer())
            addSerializer(Bytecode::class.java, BytecodeJacksonSerializer())
            Stream.of(
                VertexProperty.Cardinality::class.java,
                Column::class.java,
                Direction::class.java,
                Operator::class.java,
                Order::class.java,
                Pop::class.java,
                SackFunctions.Barrier::class.java,
                Scope::class.java,
                Pick::class.java,
                T::class.java
            ).forEach { e -> addSerializer(e, EnumJacksonSerializer()) }
            addSerializer(P::class.java, PJacksonSerializer())
            addSerializer(Lambda::class.java, LambdaJacksonSerializer())
            addSerializer(Bytecode.Binding::class.java, BindingJacksonSerializer())
            addSerializer(Traverser::class.java, TraverserJacksonSerializer())
            addSerializer(TraversalStrategy::class.java, TraversalStrategyJacksonSerializer())

            /////////////////////// DESERIALIZERS ////////////////////////////

            // Tinkerpop Graph
            addDeserializer(Vertex::class.java, VertexJacksonDeserializer())
            addDeserializer(Edge::class.java, EdgeJacksonDeserializer())
            addDeserializer(Property::class.java, PropertyJacksonDeserializer())
            addDeserializer(Path::class.java, PathJacksonDeserializer())
            addDeserializer(TraversalExplanation::class.java, TraversalExplanationJacksonDeserializer())
            addDeserializer(VertexProperty::class.java, VertexPropertyJacksonDeserializer())
            addDeserializer(Metrics::class.java, MetricsJacksonDeserializer())
            addDeserializer(TraversalMetrics::class.java, TraversalMetricsJacksonDeserializer())
            addDeserializer(Tree::class.java, TreeJacksonDeserializer())

            // java.util
            addDeserializer(Map::class.java, MapJacksonDeserializer())
            addDeserializer(List::class.java, ListJacksonDeserializer())
            addDeserializer(Set::class.java, SetJacksonDeserializer())

            // numbers
            addDeserializer(Integer::class.java, IntegerJackonsDeserializer())
            addDeserializer(Double::class.java, DoubleJacksonDeserializer())

            // traversal
            addDeserializer(BulkSet::class.java, BulkSetJacksonDeserializer())
            addDeserializer(Bytecode::class.java, BytecodeJacksonDeserializer())
            addDeserializer(Bytecode.Binding::class.java, BindingJacksonDeserializer())
            Stream.of(
                VertexProperty.Cardinality.values(),
                Column.values(),
                Direction.values(),
                Operator.values(),
                Order.values(),
                Pop.values(),
                SackFunctions.Barrier.values(),
                Scope.values(),
                TraversalOptionParent.Pick.values(),
                T.values()
            ).flatMap(Stream::of)
                .forEach { e -> addDeserializer(e.getClass(), EnumJacksonDeserializer(e.getDeclaringClass())) }
            addDeserializer(P::class.java, PJacksonDeserializer())
            addDeserializer(TextP::class.java, TextPJacksonDeserializer())
            addDeserializer(Lambda::class.java, LambdaJacksonDeserializer())
            addDeserializer(Traverser::class.java, TraverserJacksonDeserializer())
            Arrays.asList(
                ConnectiveStrategy::class.java,
                ElementIdStrategy::class.java,
                EventStrategy::class.java,
                HaltedTraverserStrategy::class.java,
                PartitionStrategy::class.java,
                SubgraphStrategy::class.java,
                SeedStrategy::class.java,
                LazyBarrierStrategy::class.java,
                MatchAlgorithmStrategy::class.java,
                AdjacentToIncidentStrategy::class.java,
                ByModulatorOptimizationStrategy::class.java,
                ProductiveByStrategy::class.java,
                CountStrategy::class.java,
                FilterRankingStrategy::class.java,
                IdentityRemovalStrategy::class.java,
                IncidentToAdjacentStrategy::class.java,
                InlineFilterStrategy::class.java,
                MatchPredicateStrategy::class.java,
                OrderLimitStrategy::class.java,
                OptionsStrategy::class.java,
                PathProcessorStrategy::class.java,
                PathRetractionStrategy::class.java,
                RepeatUnrollStrategy::class.java,
                ComputerVerificationStrategy::class.java,
                LambdaRestrictionStrategy::class.java,
                ReadOnlyStrategy::class.java,
                StandardVerificationStrategy::class.java,
                EarlyLimitStrategy::class.java,
                EdgeLabelVerificationStrategy::class.java,
                ReservedKeysVerificationStrategy::class.java,  //
                GraphFilterStrategy::class.java,
                VertexProgramStrategy::class.java
            ).forEach { strategy -> addDeserializer(strategy, TraversalStrategyProxyJacksonDeserializer(strategy)) }
            tryLoadSparqlStrategy().ifPresent { s -> addDeserializer(s, TraversalStrategyProxyJacksonDeserializer(s)) }
        }

        @get:Override
        val typeDefinitions: Map<Any, String>
            get() = TYPE_DEFINITIONS

        @get:Override
        val typeNamespace: String
            get() = GraphSONTokens.GREMLIN_TYPE_NAMESPACE

        internal class Builder private constructor() : GraphSONModuleBuilder {
            @Override
            override fun create(normalize: Boolean): GraphSONModule {
                return GraphSONModuleV3d0(normalize)
            }
        }

        companion object {
            private val TYPE_DEFINITIONS: Map<Class, String> = Collections.unmodifiableMap(
                object : LinkedHashMap<Class?, String?>() {
                    init {
                        // Those don't have deserializers because handled by Jackson,
                        // but we still want to rename them in GraphSON
                        put(Integer::class.java, "Int32")
                        put(Long::class.java, "Int64")
                        put(Double::class.java, "Double")
                        put(Float::class.java, "Float")
                        put(Map::class.java, "Map")
                        put(List::class.java, "List")
                        put(Set::class.java, "Set")

                        // TinkerPop Graph objects
                        put(Lambda::class.java, "Lambda")
                        put(Vertex::class.java, "Vertex")
                        put(Edge::class.java, "Edge")
                        put(Property::class.java, "Property")
                        put(Path::class.java, "Path")
                        put(VertexProperty::class.java, "VertexProperty")
                        put(Metrics::class.java, "Metrics")
                        put(TraversalMetrics::class.java, "TraversalMetrics")
                        put(TraversalExplanation::class.java, "TraversalExplanation")
                        put(Traverser::class.java, "Traverser")
                        put(Tree::class.java, "Tree")
                        put(BulkSet::class.java, "BulkSet")
                        put(Bytecode::class.java, "Bytecode")
                        put(Bytecode.Binding::class.java, "Binding")
                        put(AndP::class.java, "P")
                        put(OrP::class.java, "P")
                        put(P::class.java, "P")
                        put(TextP::class.java, "TextP")
                        Stream.of(
                            VertexProperty.Cardinality::class.java,
                            Column::class.java,
                            Direction::class.java,
                            Operator::class.java,
                            Order::class.java,
                            Pop::class.java,
                            SackFunctions.Barrier::class.java,
                            Pick::class.java,
                            Scope::class.java,
                            T::class.java
                        ).forEach { e -> put(e, e.getSimpleName()) }
                        Arrays.asList(
                            ConnectiveStrategy::class.java,
                            ElementIdStrategy::class.java,
                            EventStrategy::class.java,
                            HaltedTraverserStrategy::class.java,
                            PartitionStrategy::class.java,
                            SubgraphStrategy::class.java,
                            SeedStrategy::class.java,
                            LazyBarrierStrategy::class.java,
                            MatchAlgorithmStrategy::class.java,
                            AdjacentToIncidentStrategy::class.java,
                            ByModulatorOptimizationStrategy::class.java,
                            ProductiveByStrategy::class.java,
                            CountStrategy::class.java,
                            FilterRankingStrategy::class.java,
                            IdentityRemovalStrategy::class.java,
                            IncidentToAdjacentStrategy::class.java,
                            InlineFilterStrategy::class.java,
                            MatchPredicateStrategy::class.java,
                            OrderLimitStrategy::class.java,
                            OptionsStrategy::class.java,
                            PathProcessorStrategy::class.java,
                            PathRetractionStrategy::class.java,
                            RepeatUnrollStrategy::class.java,
                            ComputerVerificationStrategy::class.java,
                            LambdaRestrictionStrategy::class.java,
                            ReadOnlyStrategy::class.java,
                            StandardVerificationStrategy::class.java,
                            EarlyLimitStrategy::class.java,
                            EdgeLabelVerificationStrategy::class.java,
                            ReservedKeysVerificationStrategy::class.java,  //
                            GraphFilterStrategy::class.java,
                            VertexProgramStrategy::class.java
                        ).forEach { strategy -> put(strategy, strategy.getSimpleName()) }
                        tryLoadSparqlStrategy().ifPresent { s -> put(s, s.getSimpleName()) }
                    }
                })

            fun build(): Builder {
                return Builder()
            }
        }
    }

    /**
     * Version 2.0 of GraphSON.
     */
    internal class GraphSONModuleV2d0 protected constructor(normalize: Boolean) : GraphSONModule("graphson-2.0") {
        /**
         * Constructs a new object.
         */
        init {

            /////////////////////// SERIALIZERS ////////////////////////////

            // graph
            addSerializer(Edge::class.java, EdgeJacksonSerializer(normalize))
            addSerializer(Vertex::class.java, VertexJacksonSerializer(normalize))
            addSerializer(VertexProperty::class.java, VertexPropertyJacksonSerializer(normalize, true))
            addSerializer(Property::class.java, PropertyJacksonSerializer())
            addSerializer(Metrics::class.java, MetricsJacksonSerializer())
            addSerializer(TraversalMetrics::class.java, TraversalMetricsJacksonSerializer())
            addSerializer(TraversalExplanation::class.java, TraversalExplanationJacksonSerializer())
            addSerializer(Path::class.java, PathJacksonSerializer())
            addSerializer(DirectionalStarGraph::class.java, StarGraphGraphSONSerializerV2d0(normalize))
            addSerializer(Tree::class.java, TreeJacksonSerializer())

            // java.util
            addSerializer(Map.Entry::class.java, MapEntryJacksonSerializer())

            // need to explicitly add serializers for those types because Jackson doesn't do it at all.
            addSerializer(Integer::class.java, IntegerGraphSONSerializer())
            addSerializer(Double::class.java, DoubleGraphSONSerializer())

            // traversal
            addSerializer(Traversal::class.java, TraversalJacksonSerializer())
            addSerializer(Bytecode::class.java, BytecodeJacksonSerializer())
            Stream.of(
                VertexProperty.Cardinality::class.java,
                Column::class.java,
                Direction::class.java,
                Operator::class.java,
                Order::class.java,
                Pop::class.java,
                SackFunctions.Barrier::class.java,
                Scope::class.java,
                Pick::class.java,
                T::class.java
            ).forEach { e -> addSerializer(e, EnumJacksonSerializer()) }
            addSerializer(P::class.java, PJacksonSerializer())
            addSerializer(Lambda::class.java, LambdaJacksonSerializer())
            addSerializer(Bytecode.Binding::class.java, BindingJacksonSerializer())
            addSerializer(Traverser::class.java, TraverserJacksonSerializer())
            addSerializer(TraversalStrategy::class.java, TraversalStrategyJacksonSerializer())

            /////////////////////// DESERIALIZERS ////////////////////////////

            // Tinkerpop Graph
            addDeserializer(Vertex::class.java, VertexJacksonDeserializer())
            addDeserializer(Edge::class.java, EdgeJacksonDeserializer())
            addDeserializer(Property::class.java, PropertyJacksonDeserializer())
            addDeserializer(Path::class.java, PathJacksonDeserializer())
            addDeserializer(VertexProperty::class.java, VertexPropertyJacksonDeserializer())
            addDeserializer(TraversalExplanation::class.java, TraversalExplanationJacksonDeserializer())
            addDeserializer(Metrics::class.java, MetricsJacksonDeserializer())
            addDeserializer(TraversalMetrics::class.java, TraversalMetricsJacksonDeserializer())
            addDeserializer(Tree::class.java, TreeJacksonDeserializer())

            // numbers
            addDeserializer(Integer::class.java, IntegerJacksonDeserializer())
            addDeserializer(Double::class.java, DoubleJacksonDeserializer())

            // traversal
            addDeserializer(Bytecode::class.java, BytecodeJacksonDeserializer())
            addDeserializer(Bytecode.Binding::class.java, BindingJacksonDeserializer())
            Stream.of(
                VertexProperty.Cardinality.values(),
                Column.values(),
                Direction.values(),
                Operator.values(),
                Order.values(),
                Pop.values(),
                SackFunctions.Barrier.values(),
                Scope.values(),
                TraversalOptionParent.Pick.values(),
                T.values()
            ).flatMap(Stream::of)
                .forEach { e -> addDeserializer(e.getClass(), EnumJacksonDeserializer(e.getDeclaringClass())) }
            addDeserializer(P::class.java, PJacksonDeserializer())
            addDeserializer(TextP::class.java, TextPJacksonDeserializer())
            addDeserializer(Lambda::class.java, LambdaJacksonDeserializer())
            addDeserializer(Traverser::class.java, TraverserJacksonDeserializer())
            Arrays.asList(
                ConnectiveStrategy::class.java,
                ElementIdStrategy::class.java,
                EventStrategy::class.java,
                HaltedTraverserStrategy::class.java,
                PartitionStrategy::class.java,
                SubgraphStrategy::class.java,
                SeedStrategy::class.java,
                LazyBarrierStrategy::class.java,
                MatchAlgorithmStrategy::class.java,
                AdjacentToIncidentStrategy::class.java,
                ByModulatorOptimizationStrategy::class.java,
                CountStrategy::class.java,
                FilterRankingStrategy::class.java,
                IdentityRemovalStrategy::class.java,
                IncidentToAdjacentStrategy::class.java,
                InlineFilterStrategy::class.java,
                MatchPredicateStrategy::class.java,
                OrderLimitStrategy::class.java,
                OptionsStrategy::class.java,
                PathProcessorStrategy::class.java,
                PathRetractionStrategy::class.java,
                RepeatUnrollStrategy::class.java,
                ComputerVerificationStrategy::class.java,
                LambdaRestrictionStrategy::class.java,
                ReadOnlyStrategy::class.java,
                StandardVerificationStrategy::class.java,
                EarlyLimitStrategy::class.java,
                EdgeLabelVerificationStrategy::class.java,
                ReservedKeysVerificationStrategy::class.java,  //
                GraphFilterStrategy::class.java,
                VertexProgramStrategy::class.java
            ).forEach { strategy -> addDeserializer(strategy, TraversalStrategyProxyJacksonDeserializer(strategy)) }
            tryLoadSparqlStrategy().ifPresent { s -> addDeserializer(s, TraversalStrategyProxyJacksonDeserializer(s)) }
        }

        @get:Override
        val typeDefinitions: Map<Any, String>
            get() = TYPE_DEFINITIONS

        @get:Override
        val typeNamespace: String
            get() = GraphSONTokens.GREMLIN_TYPE_NAMESPACE

        internal class Builder private constructor() : GraphSONModuleBuilder {
            @Override
            override fun create(normalize: Boolean): GraphSONModule {
                return GraphSONModuleV2d0(normalize)
            }
        }

        companion object {
            private val TYPE_DEFINITIONS: Map<Class, String> = Collections.unmodifiableMap(
                object : LinkedHashMap<Class?, String?>() {
                    init {
                        // Those don't have deserializers because handled by Jackson,
                        // but we still want to rename them in GraphSON
                        put(Integer::class.java, "Int32")
                        put(Long::class.java, "Int64")
                        put(Double::class.java, "Double")
                        put(Float::class.java, "Float")

                        // Tinkerpop Graph objects
                        put(Lambda::class.java, "Lambda")
                        put(Vertex::class.java, "Vertex")
                        put(Edge::class.java, "Edge")
                        put(Property::class.java, "Property")
                        put(Path::class.java, "Path")
                        put(VertexProperty::class.java, "VertexProperty")
                        put(Metrics::class.java, "Metrics")
                        put(TraversalMetrics::class.java, "TraversalMetrics")
                        put(TraversalExplanation::class.java, "TraversalExplanation")
                        put(Traverser::class.java, "Traverser")
                        put(Tree::class.java, "Tree")
                        put(Bytecode::class.java, "Bytecode")
                        put(Bytecode.Binding::class.java, "Binding")
                        put(AndP::class.java, "P")
                        put(OrP::class.java, "P")
                        put(P::class.java, "P")
                        put(TextP::class.java, "TextP")
                        Stream.of(
                            VertexProperty.Cardinality::class.java,
                            Column::class.java,
                            Direction::class.java,
                            Operator::class.java,
                            Order::class.java,
                            Pop::class.java,
                            SackFunctions.Barrier::class.java,
                            Pick::class.java,
                            Scope::class.java,
                            T::class.java
                        ).forEach { e -> put(e, e.getSimpleName()) }
                        Arrays.asList(
                            ConnectiveStrategy::class.java,
                            ElementIdStrategy::class.java,
                            EventStrategy::class.java,
                            HaltedTraverserStrategy::class.java,
                            PartitionStrategy::class.java,
                            SubgraphStrategy::class.java,
                            SeedStrategy::class.java,
                            LazyBarrierStrategy::class.java,
                            MatchAlgorithmStrategy::class.java,
                            AdjacentToIncidentStrategy::class.java,
                            ByModulatorOptimizationStrategy::class.java,
                            ProductiveByStrategy::class.java,
                            CountStrategy::class.java,
                            FilterRankingStrategy::class.java,
                            IdentityRemovalStrategy::class.java,
                            IncidentToAdjacentStrategy::class.java,
                            InlineFilterStrategy::class.java,
                            MatchPredicateStrategy::class.java,
                            OrderLimitStrategy::class.java,
                            OptionsStrategy::class.java,
                            PathProcessorStrategy::class.java,
                            PathRetractionStrategy::class.java,
                            RepeatUnrollStrategy::class.java,
                            ComputerVerificationStrategy::class.java,
                            LambdaRestrictionStrategy::class.java,
                            ReadOnlyStrategy::class.java,
                            StandardVerificationStrategy::class.java,
                            EarlyLimitStrategy::class.java,
                            EdgeLabelVerificationStrategy::class.java,
                            ReservedKeysVerificationStrategy::class.java,  //
                            GraphFilterStrategy::class.java,
                            VertexProgramStrategy::class.java
                        ).forEach { strategy -> put(strategy, strategy.getSimpleName()) }
                        tryLoadSparqlStrategy().ifPresent { s -> put(s, s.getSimpleName()) }
                    }
                })

            fun build(): Builder {
                return Builder()
            }
        }
    }

    /**
     * Version 1.0 of GraphSON.
     */
    internal class GraphSONModuleV1d0 protected constructor(normalize: Boolean) : GraphSONModule("graphson-1.0") {
        /**
         * Constructs a new object.
         */
        init {
            // graph
            addSerializer(Edge::class.java, EdgeJacksonSerializer(normalize))
            addSerializer(Vertex::class.java, VertexJacksonSerializer(normalize))
            addSerializer(VertexProperty::class.java, VertexPropertyJacksonSerializer(normalize))
            addSerializer(Property::class.java, PropertyJacksonSerializer())
            addSerializer(TraversalMetrics::class.java, TraversalMetricsJacksonSerializer())
            addSerializer(TraversalExplanation::class.java, TraversalExplanationJacksonSerializer())
            addSerializer(Path::class.java, PathJacksonSerializer())
            addSerializer(DirectionalStarGraph::class.java, StarGraphGraphSONSerializerV1d0(normalize))
            addSerializer(Tree::class.java, TreeJacksonSerializer())

            // java.util
            addSerializer(Map.Entry::class.java, MapEntryJacksonSerializer())

            // java.time
            addSerializer(Duration::class.java, DurationJacksonSerializer())
            addSerializer(Instant::class.java, InstantJacksonSerializer())
            addSerializer(LocalDate::class.java, LocalDateJacksonSerializer())
            addSerializer(LocalDateTime::class.java, LocalDateTimeJacksonSerializer())
            addSerializer(LocalTime::class.java, LocalTimeJacksonSerializer())
            addSerializer(MonthDay::class.java, MonthDayJacksonSerializer())
            addSerializer(OffsetDateTime::class.java, OffsetDateTimeJacksonSerializer())
            addSerializer(OffsetTime::class.java, OffsetTimeJacksonSerializer())
            addSerializer(Period::class.java, PeriodJacksonSerializer())
            addSerializer(Year::class.java, YearJacksonSerializer())
            addSerializer(YearMonth::class.java, YearMonthJacksonSerializer())
            addSerializer(ZonedDateTime::class.java, ZonedDateTimeJacksonSerializer())
            addSerializer(ZoneOffset::class.java, ZoneOffsetJacksonSerializer())
            addDeserializer(Duration::class.java, DurationJacksonDeserializer())
            addDeserializer(Instant::class.java, InstantJacksonDeserializer())
            addDeserializer(LocalDate::class.java, LocalDateJacksonDeserializer())
            addDeserializer(LocalDateTime::class.java, LocalDateTimeJacksonDeserializer())
            addDeserializer(LocalTime::class.java, LocalTimeJacksonDeserializer())
            addDeserializer(MonthDay::class.java, MonthDayJacksonDeserializer())
            addDeserializer(OffsetDateTime::class.java, OffsetDateTimeJacksonDeserializer())
            addDeserializer(OffsetTime::class.java, OffsetTimeJacksonDeserializer())
            addDeserializer(Period::class.java, PeriodJacksonDeserializer())
            addDeserializer(Year::class.java, YearJacksonDeserializer())
            addDeserializer(YearMonth::class.java, YearMonthJacksonDeserializer())
            addDeserializer(ZonedDateTime::class.java, ZonedDateTimeJacksonDeserializer())
            addDeserializer(ZoneOffset::class.java, ZoneOffsetJacksonDeserializer())
        }

        // null is fine and handled by the GraphSONMapper
        @get:Override
        val typeDefinitions: Map<Any, String>?
            get() =// null is fine and handled by the GraphSONMapper
                null

        // null is fine and handled by the GraphSONMapper
        @get:Override
        val typeNamespace: String?
            get() =// null is fine and handled by the GraphSONMapper
                null

        internal class Builder private constructor() : GraphSONModuleBuilder {
            @Override
            override fun create(normalize: Boolean): GraphSONModule {
                return GraphSONModuleV1d0(normalize)
            }
        }

        companion object {
            fun build(): Builder {
                return Builder()
            }
        }
    }

    /**
     * A "builder" used to create [GraphSONModule] instances.  Each "version" should have an associated
     * `GraphSONModuleBuilder` so that it can be registered with the [GraphSONVersion] enum.
     */
    internal interface GraphSONModuleBuilder {
        /**
         * Creates a new [GraphSONModule] object.
         *
         * @param normalize when set to true, keys and objects are ordered to ensure that they are the occur in
         * the same order.
         */
        fun create(normalize: Boolean): GraphSONModule?
    }

    companion object {
        /**
         * Attempt to load `SparqlStrategy` if it's on the path. Dynamically loading it from core makes it easier
         * for users as they won't have to register special modules for serialization purposes.
         */
        private fun tryLoadSparqlStrategy(): Optional<Class<*>> {
            return try {
                Optional.of(Class.forName("org.apache.tinkerpop4.gremlin.sparql.process.traversal.strategy.SparqlStrategy"))
            } catch (ignored: Exception) {
                Optional.empty()
            }
        }
    }
}