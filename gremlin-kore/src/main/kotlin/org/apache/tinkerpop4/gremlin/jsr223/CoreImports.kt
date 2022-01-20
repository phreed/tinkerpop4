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
package org.apache.tinkerpop4.gremlin.jsr223

import org.apache.commons.configuration2.BaseConfiguration

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
object CoreImports {
    private val CLASS_IMPORTS: Set<Class> = LinkedHashSet()
    private val FIELD_IMPORTS: Set<Field> = LinkedHashSet()
    private val METHOD_IMPORTS: Set<Method> = LinkedHashSet()
    private val ENUM_IMPORTS: Set<Enum> = LinkedHashSet()

    init {
        /////////////
        // CLASSES //
        /////////////

        // structure
        CLASS_IMPORTS.add(Edge::class.java)
        CLASS_IMPORTS.add(Element::class.java)
        CLASS_IMPORTS.add(Graph::class.java)
        CLASS_IMPORTS.add(Property::class.java)
        CLASS_IMPORTS.add(Transaction::class.java)
        CLASS_IMPORTS.add(Vertex::class.java)
        CLASS_IMPORTS.add(VertexProperty::class.java)
        CLASS_IMPORTS.add(GraphFactory::class.java)
        CLASS_IMPORTS.add(ElementHelper::class.java)
        CLASS_IMPORTS.add(ReferenceEdge::class.java)
        CLASS_IMPORTS.add(ReferenceProperty::class.java)
        CLASS_IMPORTS.add(ReferenceVertex::class.java)
        CLASS_IMPORTS.add(ReferenceVertexProperty::class.java)

        // tokens
        CLASS_IMPORTS.add(SackFunctions::class.java)
        CLASS_IMPORTS.add(SackFunctions.Barrier::class.java)
        CLASS_IMPORTS.add(VertexProperty.Cardinality::class.java)
        CLASS_IMPORTS.add(Column::class.java)
        CLASS_IMPORTS.add(Direction::class.java)
        CLASS_IMPORTS.add(Operator::class.java)
        CLASS_IMPORTS.add(Order::class.java)
        CLASS_IMPORTS.add(Pop::class.java)
        CLASS_IMPORTS.add(Scope::class.java)
        CLASS_IMPORTS.add(T::class.java)
        CLASS_IMPORTS.add(TraversalOptionParent::class.java)
        CLASS_IMPORTS.add(Pick::class.java)
        CLASS_IMPORTS.add(P::class.java)
        CLASS_IMPORTS.add(TextP::class.java)
        CLASS_IMPORTS.add(WithOptions::class.java)
        // remote
        CLASS_IMPORTS.add(RemoteConnection::class.java)
        CLASS_IMPORTS.add(EmptyGraph::class.java)
        // io
        CLASS_IMPORTS.add(GraphReader::class.java)
        CLASS_IMPORTS.add(GraphWriter::class.java)
        CLASS_IMPORTS.add(Io::class.java)
        CLASS_IMPORTS.add(IO::class.java)
        CLASS_IMPORTS.add(IoCore::class.java)
        CLASS_IMPORTS.add(Storage::class.java)
        CLASS_IMPORTS.add(GraphMLIo::class.java)
        CLASS_IMPORTS.add(GraphMLMapper::class.java)
        CLASS_IMPORTS.add(GraphMLReader::class.java)
        CLASS_IMPORTS.add(GraphMLWriter::class.java)
        CLASS_IMPORTS.add(GraphSONIo::class.java)
        CLASS_IMPORTS.add(GraphSONMapper::class.java)
        CLASS_IMPORTS.add(GraphSONReader::class.java)
        CLASS_IMPORTS.add(GraphSONTokens::class.java)
        CLASS_IMPORTS.add(GraphSONVersion::class.java)
        CLASS_IMPORTS.add(GraphSONWriter::class.java)
        CLASS_IMPORTS.add(LegacyGraphSONReader::class.java)
        CLASS_IMPORTS.add(GryoClassResolverV1d0::class.java)
        CLASS_IMPORTS.add(GryoClassResolverV3d0::class.java)
        CLASS_IMPORTS.add(GryoIo::class.java)
        CLASS_IMPORTS.add(GryoMapper::class.java)
        CLASS_IMPORTS.add(GryoReader::class.java)
        CLASS_IMPORTS.add(GryoVersion::class.java)
        CLASS_IMPORTS.add(GryoWriter::class.java)
        // configuration
        CLASS_IMPORTS.add(BaseConfiguration::class.java)
        CLASS_IMPORTS.add(CombinedConfiguration::class.java)
        CLASS_IMPORTS.add(CompositeConfiguration::class.java)
        CLASS_IMPORTS.add(Configuration::class.java)
        CLASS_IMPORTS.add(ConfigurationUtils::class.java)
        CLASS_IMPORTS.add(HierarchicalConfiguration::class.java)
        CLASS_IMPORTS.add(MapConfiguration::class.java)
        CLASS_IMPORTS.add(PropertiesConfiguration::class.java)
        CLASS_IMPORTS.add(SubsetConfiguration::class.java)
        CLASS_IMPORTS.add(XMLConfiguration::class.java)
        CLASS_IMPORTS.add(Configurations::class.java)
        // strategies
        CLASS_IMPORTS.add(ConnectiveStrategy::class.java)
        CLASS_IMPORTS.add(ElementIdStrategy::class.java)
        CLASS_IMPORTS.add(EventStrategy::class.java)
        CLASS_IMPORTS.add(HaltedTraverserStrategy::class.java)
        CLASS_IMPORTS.add(PartitionStrategy::class.java)
        CLASS_IMPORTS.add(SubgraphStrategy::class.java)
        CLASS_IMPORTS.add(LazyBarrierStrategy::class.java)
        CLASS_IMPORTS.add(MatchAlgorithmStrategy::class.java)
        CLASS_IMPORTS.add(ProfileStrategy::class.java)
        CLASS_IMPORTS.add(AdjacentToIncidentStrategy::class.java)
        CLASS_IMPORTS.add(ByModulatorOptimizationStrategy::class.java)
        CLASS_IMPORTS.add(ProductiveByStrategy::class.java)
        CLASS_IMPORTS.add(CountStrategy::class.java)
        CLASS_IMPORTS.add(FilterRankingStrategy::class.java)
        CLASS_IMPORTS.add(IdentityRemovalStrategy::class.java)
        CLASS_IMPORTS.add(IncidentToAdjacentStrategy::class.java)
        CLASS_IMPORTS.add(MatchPredicateStrategy::class.java)
        CLASS_IMPORTS.add(EarlyLimitStrategy::class.java)
        CLASS_IMPORTS.add(OrderLimitStrategy::class.java)
        CLASS_IMPORTS.add(PathProcessorStrategy::class.java)
        CLASS_IMPORTS.add(ComputerVerificationStrategy::class.java)
        CLASS_IMPORTS.add(LambdaRestrictionStrategy::class.java)
        CLASS_IMPORTS.add(ReadOnlyStrategy::class.java)
        CLASS_IMPORTS.add(ReferenceElementStrategy::class.java)
        CLASS_IMPORTS.add(SeedStrategy::class.java)
        CLASS_IMPORTS.add(StandardVerificationStrategy::class.java)
        CLASS_IMPORTS.add(EdgeLabelVerificationStrategy::class.java)
        CLASS_IMPORTS.add(VertexProgramRestrictionStrategy::class.java)
        // graph traversal
        CLASS_IMPORTS.add(AnonymousTraversalSource::class.java)
        CLASS_IMPORTS.add(Anon::class.java)
        CLASS_IMPORTS.add(GraphTraversal::class.java)
        CLASS_IMPORTS.add(GraphTraversalSource::class.java)
        CLASS_IMPORTS.add(Traversal::class.java)
        CLASS_IMPORTS.add(TraversalMetrics::class.java)
        CLASS_IMPORTS.add(Translator::class.java)
        CLASS_IMPORTS.add(DotNetTranslator::class.java)
        CLASS_IMPORTS.add(GroovyTranslator::class.java)
        CLASS_IMPORTS.add(JavaTranslator::class.java)
        CLASS_IMPORTS.add(JavascriptTranslator::class.java)
        CLASS_IMPORTS.add(PythonTranslator::class.java)
        CLASS_IMPORTS.add(Bindings::class.java)
        // graph computer
        CLASS_IMPORTS.add(Computer::class.java)
        CLASS_IMPORTS.add(ComputerResult::class.java)
        CLASS_IMPORTS.add(ConnectedComponent::class.java)
        CLASS_IMPORTS.add(ConnectedComponentVertexProgram::class.java)
        CLASS_IMPORTS.add(GraphComputer::class.java)
        CLASS_IMPORTS.add(Memory::class.java)
        CLASS_IMPORTS.add(VertexProgram::class.java)
        CLASS_IMPORTS.add(CloneVertexProgram::class.java)
        CLASS_IMPORTS.add(ClusterCountMapReduce::class.java)
        CLASS_IMPORTS.add(ClusterPopulationMapReduce::class.java)
        CLASS_IMPORTS.add(MemoryTraversalSideEffects::class.java)
        CLASS_IMPORTS.add(PeerPressure::class.java)
        CLASS_IMPORTS.add(PeerPressureVertexProgram::class.java)
        CLASS_IMPORTS.add(PageRank::class.java)
        CLASS_IMPORTS.add(PageRankMapReduce::class.java)
        CLASS_IMPORTS.add(PageRankVertexProgram::class.java)
        CLASS_IMPORTS.add(ShortestPath::class.java)
        CLASS_IMPORTS.add(ShortestPathVertexProgram::class.java)
        CLASS_IMPORTS.add(GraphFilterStrategy::class.java)
        CLASS_IMPORTS.add(TraversalVertexProgram::class.java)
        CLASS_IMPORTS.add(VertexProgramStrategy::class.java)
        // utils
        CLASS_IMPORTS.add(Gremlin::class.java)
        CLASS_IMPORTS.add(IteratorUtils::class.java)
        CLASS_IMPORTS.add(TimeUtil::class.java)
        CLASS_IMPORTS.add(Lambda::class.java)
        CLASS_IMPORTS.add(java.util.Date::class.java)
        CLASS_IMPORTS.add(java.sql.Timestamp::class.java)
        CLASS_IMPORTS.add(java.util.UUID::class.java)

        /////////////
        // METHODS //
        /////////////
        uniqueMethods(IoCore::class.java).forEach(METHOD_IMPORTS::add)
        uniqueMethods(P::class.java).forEach(METHOD_IMPORTS::add)
        uniqueMethods(AnonymousTraversalSource::class.java).forEach(METHOD_IMPORTS::add)
        uniqueMethods(TextP::class.java).forEach(METHOD_IMPORTS::add)
        uniqueMethods(Anon::class.java).filter { m -> !m.getName().equals("__") }.forEach(METHOD_IMPORTS::add)
        uniqueMethods(Computer::class.java).forEach(METHOD_IMPORTS::add)
        uniqueMethods(TimeUtil::class.java).forEach(METHOD_IMPORTS::add)
        uniqueMethods(Lambda::class.java).forEach(METHOD_IMPORTS::add)
        try {
            METHOD_IMPORTS.add(DatetimeHelper::class.java.getMethod("datetime", String::class.java))
        } catch (ex: Exception) {
            throw IllegalStateException("Could not load datetime() function to imports")
        }

        ///////////
        // ENUMS //
        ///////////
        Collections.addAll(ENUM_IMPORTS, SackFunctions.Barrier.values())
        Collections.addAll(ENUM_IMPORTS, VertexProperty.Cardinality.values())
        Collections.addAll(ENUM_IMPORTS, Column.values())
        Collections.addAll(ENUM_IMPORTS, Direction.values())
        Collections.addAll(ENUM_IMPORTS, Operator.values())
        Collections.addAll(ENUM_IMPORTS, Order.values())
        Collections.addAll(ENUM_IMPORTS, Pop.values())
        Collections.addAll(ENUM_IMPORTS, Scope.values())
        Collections.addAll(ENUM_IMPORTS, T.values())
        Collections.addAll(ENUM_IMPORTS, TraversalOptionParent.Pick.values())
    }

    val classImports: Set<Any>
        get() = Collections.unmodifiableSet(CLASS_IMPORTS)
    val methodImports: Set<Any>
        get() = Collections.unmodifiableSet(METHOD_IMPORTS)
    val enumImports: Set<Any>
        get() = Collections.unmodifiableSet(ENUM_IMPORTS)
    val fieldImports: Set<Any>
        get() = Collections.unmodifiableSet(FIELD_IMPORTS)

    /**
     * Filters to unique method names on each class.
     */
    private fun uniqueMethods(clazz: Class<*>): Stream<Method> {
        val unique: Set<String> = LinkedHashSet()
        return Stream.of(clazz.getMethods())
            .filter { m -> Modifier.isStatic(m.getModifiers()) }
            .map { m -> Pair.with(generateMethodDescriptor(m), m) }
            .filter { p ->
                val exists = unique.contains(p.getValue0())
                if (!exists) unique.add(p.getValue0())
                !exists
            }
            .map(Pair::getValue1)
    }

    private fun generateMethodDescriptor(m: Method): String {
        return m.getDeclaringClass().getCanonicalName() + "." + m.getName()
    }
}