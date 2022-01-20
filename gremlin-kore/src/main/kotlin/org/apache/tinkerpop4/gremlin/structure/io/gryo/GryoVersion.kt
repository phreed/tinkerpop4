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
package org.apache.tinkerpop4.gremlin.structure.io.gryo

import org.apache.tinkerpop4.gremlin.process.computer.GraphFilter

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
enum class GryoVersion(
    versionNumber: String,
    registrations: List<TypeRegistration<*>>,
    classResolverMaker: Supplier<ClassResolver>
) {
    V1_0("1.0", initV1d0Registrations(), Supplier<ClassResolver> { GryoClassResolverV1d0() }), V3_0(
        "3.0",
        initV3d0Registrations(),
        Supplier<ClassResolver> { GryoClassResolverV3d0() });

    val version: String
    private val registrations: List<TypeRegistration<*>>
    private val classResolverMaker: Supplier<ClassResolver>

    /**
     * Creates a new [GryoVersion].
     *
     * @param versionNumber the user facing string representation of the version which should follow an `x.y`
     * pattern
     * @param registrations the list of registrations for this version
     * @param classResolverMaker providers the default `ClassResolver` for a particular version of Gryo
     */
    init {
        // Validate the default registrations
        // For justification of these default registration rules, see TinkerPopKryoRegistrator
        for (tr in registrations) {
            if (tr.hasSerializer() /* no serializer is acceptable */ && null == tr.getSerializerShim() /* a shim serializer is acceptable */ &&
                tr.getShadedSerializer() !is JavaSerializer /* shaded JavaSerializer is acceptable */) {
                // everything else is invalid
                val msg: String = String.format(
                    "The default GryoMapper type registration %s is invalid.  " +
                            "It must supply either an implementation of %s or %s, but supplies neither.  " +
                            "This is probably a bug in GryoMapper's default serialization class registrations.", tr,
                    SerializerShim::class.java.getCanonicalName(), JavaSerializer::class.java.getCanonicalName()
                )
                throw IllegalStateException(msg)
            }
        }
        version = versionNumber
        this.registrations = registrations
        this.classResolverMaker = classResolverMaker
    }

    fun cloneRegistrations(): List<TypeRegistration<*>> {
        return ArrayList(registrations)
    }

    fun getRegistrations(): List<TypeRegistration<*>> {
        return Collections.unmodifiableList(registrations)
    }

    fun getClassResolverMaker(): Supplier<ClassResolver> {
        return classResolverMaker
    }

    private object Types {
        /**
         * Map with one entry that is used so that it is possible to get the class of LinkedHashMap.Entry.
         */
        private val m: LinkedHashMap = object : LinkedHashMap() {
            init {
                put("junk", "dummy")
            }
        }
        private val ARRAYS_AS_LIST: Class = Arrays.asList("dummy").getClass()
        private val LINKED_HASH_MAP_ENTRY_CLASS: Class = m.entrySet().iterator().next().getClass()

        /**
         * The `HashMap$Node` class comes into serialization play when a `Map.entrySet()` is
         * serialized.
         */
        private val HASH_MAP_NODE: Class? = null
        private val HASH_MAP_TREE_NODE: Class? = null
        private val COLLECTIONS_SYNCHRONIZED_MAP: Class? = null

        init {
            // have to instantiate this via reflection because it is a private inner class of HashMap
            val className: String = HashMap::class.java.getName() + "\$Node"
            try {
                HASH_MAP_NODE = Class.forName(org.apache.tinkerpop4.gremlin.structure.io.gryo.className)
            } catch (ex: Exception) {
                throw RuntimeException(
                    "Could not access " + org.apache.tinkerpop4.gremlin.structure.io.gryo.className,
                    ex
                )
            }
            org.apache.tinkerpop4.gremlin.structure.io.gryo.className = HashMap::class.java.getName() + "\$TreeNode"
            try {
                HASH_MAP_TREE_NODE = Class.forName(org.apache.tinkerpop4.gremlin.structure.io.gryo.className)
            } catch (ex: Exception) {
                throw RuntimeException(
                    "Could not access " + org.apache.tinkerpop4.gremlin.structure.io.gryo.className,
                    ex
                )
            }
            org.apache.tinkerpop4.gremlin.structure.io.gryo.className =
                Collections::class.java.getName() + "\$SynchronizedMap"
            try {
                COLLECTIONS_SYNCHRONIZED_MAP = Class.forName(org.apache.tinkerpop4.gremlin.structure.io.gryo.className)
            } catch (ex: Exception) {
                throw RuntimeException(
                    "Could not access " + org.apache.tinkerpop4.gremlin.structure.io.gryo.className,
                    ex
                )
            }
        }
    }

    companion object {
        fun initV3d0Registrations(): List<TypeRegistration<*>> {
            return object : ArrayList<TypeRegistration<*>?>() {
                init {
                    add(GryoTypeReg.of(ByteArray::class.java, 25))
                    add(GryoTypeReg.of(CharArray::class.java, 26))
                    add(GryoTypeReg.of(ShortArray::class.java, 27))
                    add(GryoTypeReg.of(IntArray::class.java, 28))
                    add(GryoTypeReg.of(LongArray::class.java, 29))
                    add(GryoTypeReg.of(FloatArray::class.java, 30))
                    add(GryoTypeReg.of(DoubleArray::class.java, 31))
                    add(GryoTypeReg.of(Array<String>::class.java, 32))
                    add(GryoTypeReg.of(Array<Object>::class.java, 33))
                    add(GryoTypeReg.of(ArrayList::class.java, 10))
                    add(GryoTypeReg.of(Types.ARRAYS_AS_LIST, 134, ArraysAsListSerializer()))
                    add(GryoTypeReg.of(BigInteger::class.java, 34))
                    add(GryoTypeReg.of(BigDecimal::class.java, 35))
                    add(GryoTypeReg.of(Calendar::class.java, 39))
                    add(GryoTypeReg.of(Class::class.java, 41, ClassSerializer()))
                    add(GryoTypeReg.of(Array<Class>::class.java, 166, ClassArraySerializer()))
                    add(GryoTypeReg.of(Collection::class.java, 37))
                    add(GryoTypeReg.of(Collections.EMPTY_LIST.getClass(), 51))
                    add(GryoTypeReg.of(Collections.EMPTY_MAP.getClass(), 52))
                    add(GryoTypeReg.of(Collections.EMPTY_SET.getClass(), 53))
                    add(GryoTypeReg.of(Collections.singleton(null).getClass(), 54))
                    add(GryoTypeReg.of(Collections.singletonList(null).getClass(), 24))
                    add(GryoTypeReg.of(Collections.singletonMap(null, null).getClass(), 23))
                    add(GryoTypeReg.of(Types.COLLECTIONS_SYNCHRONIZED_MAP, 185, SynchronizedMapSerializer()))
                    add(GryoTypeReg.of(Contains::class.java, 49))
                    add(GryoTypeReg.of(Currency::class.java, 40))
                    add(GryoTypeReg.of(Date::class.java, 38))
                    add(GryoTypeReg.of(Direction::class.java, 12))
                    add(GryoTypeReg.of(DetachedEdge::class.java, 21))
                    add(GryoTypeReg.of(DetachedVertexProperty::class.java, 20))
                    add(GryoTypeReg.of(DetachedProperty::class.java, 18))
                    add(GryoTypeReg.of(DetachedVertex::class.java, 19))
                    add(GryoTypeReg.of(DetachedPath::class.java, 60))
                    // skip 14
                    add(GryoTypeReg.of(EnumSet::class.java, 46))
                    add(GryoTypeReg.of(HashMap::class.java, 11))
                    add(GryoTypeReg.of(HashMap.Entry::class.java, 16))
                    add(GryoTypeReg.of(Types.HASH_MAP_NODE, 92))
                    add(GryoTypeReg.of(Types.HASH_MAP_TREE_NODE, 172))
                    add(GryoTypeReg.of(KryoSerializable::class.java, 36))
                    add(GryoTypeReg.of(LinkedHashMap::class.java, 47))
                    add(GryoTypeReg.of(LinkedHashSet::class.java, 71))
                    add(GryoTypeReg.of(ConcurrentHashMap::class.java, 170))
                    add(GryoTypeReg.of(ConcurrentHashMap.Entry::class.java, 171))
                    add(GryoTypeReg.of(LinkedList::class.java, 116))
                    add(GryoTypeReg.of(Types.LINKED_HASH_MAP_ENTRY_CLASS, 15))
                    add(GryoTypeReg.of(Locale::class.java, 22))
                    add(GryoTypeReg.of(StringBuffer::class.java, 43))
                    add(GryoTypeReg.of(StringBuilder::class.java, 44))
                    add(GryoTypeReg.of(T::class.java, 48))
                    add(GryoTypeReg.of(TimeZone::class.java, 42))
                    add(GryoTypeReg.of(TreeMap::class.java, 45))
                    add(GryoTypeReg.of(TreeSet::class.java, 50))
                    add(GryoTypeReg.of(UUID::class.java, 17, UUIDSerializer()))
                    add(GryoTypeReg.of(URI::class.java, 72, URISerializer()))
                    add(GryoTypeReg.of(VertexTerminator::class.java, 13))
                    add(GryoTypeReg.of(AbstractMap.SimpleEntry::class.java, 120))
                    add(GryoTypeReg.of(SimpleImmutableEntry::class.java, 121))
                    add(GryoTypeReg.of(java.sql.Timestamp::class.java, 161))
                    add(GryoTypeReg.of(InetAddress::class.java, 162, InetAddressSerializer()))
                    add(GryoTypeReg.of(ByteBuffer::class.java, 163, ByteBufferSerializer()))
                    add(GryoTypeReg.of(ReferenceEdge::class.java, 81))
                    add(GryoTypeReg.of(ReferenceVertexProperty::class.java, 82))
                    add(GryoTypeReg.of(ReferenceProperty::class.java, 83))
                    add(GryoTypeReg.of(ReferenceVertex::class.java, 84))
                    add(GryoTypeReg.of(ReferencePath::class.java, 85))
                    add(GryoTypeReg.of(StarGraph::class.java, 86, StarGraphSerializer(Direction.BOTH, GraphFilter())))
                    add(GryoTypeReg.of(Edge::class.java, 65, EdgeSerializer()))
                    add(GryoTypeReg.of(Vertex::class.java, 66, VertexSerializer()))
                    add(GryoTypeReg.of(Property::class.java, 67, PropertySerializer()))
                    add(GryoTypeReg.of(VertexProperty::class.java, 68, VertexPropertySerializer()))
                    add(GryoTypeReg.of(Path::class.java, 59, PathSerializer()))
                    // skip 55
                    add(GryoTypeReg.of(B_O_Traverser::class.java, 75))
                    add(GryoTypeReg.of(O_Traverser::class.java, 76))
                    add(GryoTypeReg.of(B_LP_O_P_S_SE_SL_Traverser::class.java, 77))
                    add(GryoTypeReg.of(B_O_S_SE_SL_Traverser::class.java, 78))
                    add(GryoTypeReg.of(B_LP_O_S_SE_SL_Traverser::class.java, 87))
                    add(GryoTypeReg.of(O_OB_S_SE_SL_Traverser::class.java, 89))
                    add(GryoTypeReg.of(LP_O_OB_S_SE_SL_Traverser::class.java, 90))
                    add(GryoTypeReg.of(LP_O_OB_P_S_SE_SL_Traverser::class.java, 91))
                    add(GryoTypeReg.of(ProjectedTraverser::class.java, 168))
                    add(GryoTypeReg.of(DefaultRemoteTraverser::class.java, 123, DefaultRemoteTraverserSerializer()))
                    add(GryoTypeReg.of(Bytecode::class.java, 122, BytecodeSerializer()))
                    add(GryoTypeReg.of(P::class.java, 124, PSerializer()))
                    add(GryoTypeReg.of(TextP::class.java, 186, TextPSerializer()))
                    add(GryoTypeReg.of(Lambda::class.java, 125, LambdaSerializer()))
                    add(GryoTypeReg.of(Bytecode.Binding::class.java, 126, BindingSerializer()))
                    add(GryoTypeReg.of(Order::class.java, 127))
                    add(GryoTypeReg.of(Scope::class.java, 128))
                    add(GryoTypeReg.of(VertexProperty.Cardinality::class.java, 131))
                    add(GryoTypeReg.of(Column::class.java, 132))
                    add(GryoTypeReg.of(Pop::class.java, 133))
                    add(GryoTypeReg.of(SackFunctions.Barrier::class.java, 135))
                    add(GryoTypeReg.of(Pick::class.java, 137))
                    add(GryoTypeReg.of(HashSetSupplier::class.java, 136, HashSetSupplierSerializer()))
                    add(GryoTypeReg.of(MultiComparator::class.java, 165))
                    add(GryoTypeReg.of(ConnectiveStrategy::class.java, 138))
                    add(GryoTypeReg.of(HaltedTraverserStrategy::class.java, 139))
                    add(GryoTypeReg.of(PartitionStrategy::class.java, 140, JavaSerializer()))
                    add(GryoTypeReg.of(SubgraphStrategy::class.java, 141, JavaSerializer()))
                    add(GryoTypeReg.of(SeedStrategy::class.java, 192, JavaSerializer()))
                    add(GryoTypeReg.of(VertexProgramStrategy::class.java, 142, JavaSerializer()))
                    add(GryoTypeReg.of(MatchAlgorithmStrategy::class.java, 143))
                    add(GryoTypeReg.of(GreedyMatchAlgorithm::class.java, 144))
                    add(GryoTypeReg.of(AdjacentToIncidentStrategy::class.java, 145))
                    add(GryoTypeReg.of(ByModulatorOptimizationStrategy::class.java, 191))
                    add(GryoTypeReg.of(ProductiveByStrategy::class.java, 195, JavaSerializer())) // ***LAST ID***
                    add(GryoTypeReg.of(CountStrategy::class.java, 155))
                    add(GryoTypeReg.of(FilterRankingStrategy::class.java, 146))
                    add(GryoTypeReg.of(IdentityRemovalStrategy::class.java, 147))
                    add(GryoTypeReg.of(IncidentToAdjacentStrategy::class.java, 148))
                    add(GryoTypeReg.of(InlineFilterStrategy::class.java, 149))
                    add(GryoTypeReg.of(LazyBarrierStrategy::class.java, 150))
                    add(GryoTypeReg.of(MatchPredicateStrategy::class.java, 151))
                    add(GryoTypeReg.of(OrderLimitStrategy::class.java, 152))
                    add(GryoTypeReg.of(OptionsStrategy::class.java, 187, JavaSerializer()))
                    add(GryoTypeReg.of(PathProcessorStrategy::class.java, 153))
                    add(GryoTypeReg.of(PathRetractionStrategy::class.java, 154))
                    add(GryoTypeReg.of(RepeatUnrollStrategy::class.java, 156))
                    add(GryoTypeReg.of(GraphFilterStrategy::class.java, 157))
                    add(GryoTypeReg.of(LambdaRestrictionStrategy::class.java, 158))
                    add(GryoTypeReg.of(ReadOnlyStrategy::class.java, 159))
                    add(GryoTypeReg.of(EarlyLimitStrategy::class.java, 188))
                    add(GryoTypeReg.of(CountMatchAlgorithm::class.java, 160))
                    add(GryoTypeReg.of(GreedyMatchAlgorithm::class.java, 164))
                    add(GryoTypeReg.of(EdgeLabelVerificationStrategy::class.java, 189))
                    add(GryoTypeReg.of(ReservedKeysVerificationStrategy::class.java, 190))
                    add(GryoTypeReg.of(TraverserSet::class.java, 58))
                    add(GryoTypeReg.of(Tree::class.java, 61))
                    add(GryoTypeReg.of(HashSet::class.java, 62))
                    add(GryoTypeReg.of(BulkSet::class.java, 64))
                    add(GryoTypeReg.of(Metrics::class.java, 69, MetricsSerializer()))
                    add(GryoTypeReg.of(TraversalMetrics::class.java, 70, TraversalMetricsSerializer()))
                    add(GryoTypeReg.of(MapMemory::class.java, 73))
                    add(GryoTypeReg.of(MapReduce.NullObject::class.java, 74))
                    add(GryoTypeReg.of(AtomicLong::class.java, 79))
                    add(GryoTypeReg.of(Pair::class.java, 88, PairSerializer()))
                    add(GryoTypeReg.of(Triplet::class.java, 183, TripletSerializer()))
                    add(GryoTypeReg.of(TraversalExplanation::class.java, 106, JavaSerializer()))
                    add(GryoTypeReg.of(Duration::class.java, 93, DurationSerializer()))
                    add(GryoTypeReg.of(Instant::class.java, 94, InstantSerializer()))
                    add(GryoTypeReg.of(LocalDate::class.java, 95, LocalDateSerializer()))
                    add(GryoTypeReg.of(LocalDateTime::class.java, 96, LocalDateTimeSerializer()))
                    add(GryoTypeReg.of(LocalTime::class.java, 97, LocalTimeSerializer()))
                    add(GryoTypeReg.of(MonthDay::class.java, 98, MonthDaySerializer()))
                    add(GryoTypeReg.of(OffsetDateTime::class.java, 99, OffsetDateTimeSerializer()))
                    add(GryoTypeReg.of(OffsetTime::class.java, 100, OffsetTimeSerializer()))
                    add(GryoTypeReg.of(Period::class.java, 101, PeriodSerializer()))
                    add(GryoTypeReg.of(Year::class.java, 102, YearSerializer()))
                    add(GryoTypeReg.of(YearMonth::class.java, 103, YearMonthSerializer()))
                    add(GryoTypeReg.of(ZonedDateTime::class.java, 104, ZonedDateTimeSerializer()))
                    add(GryoTypeReg.of(ZoneOffset::class.java, 105, ZoneOffsetSerializer()))
                    add(GryoTypeReg.of(Operator::class.java, 107))
                    add(GryoTypeReg.of(FoldBiOperator::class.java, 108))
                    add(GryoTypeReg.of(GroupCountBiOperator::class.java, 109))
                    add(GryoTypeReg.of(GroupBiOperator::class.java, 117))
                    add(GryoTypeReg.of(MeanGlobalBiOperator::class.java, 110))
                    add(GryoTypeReg.of(MeanNumber::class.java, 111))
                    add(GryoTypeReg.of(TreeBiOperator::class.java, 112))
                    // skip 113
                    add(GryoTypeReg.of(RangeBiOperator::class.java, 114))
                    add(GryoTypeReg.of(OrderBiOperator::class.java, 118))
                    add(GryoTypeReg.of(ProfileBiOperator::class.java, 119))
                    add(GryoTypeReg.of(VertexIndexedTraverserSet::class.java, 173))
                    add(GryoTypeReg.of(NonEmittingSeed::class.java, 194))
                    add(GryoTypeReg.of(B_LP_NL_O_P_S_SE_SL_Traverser::class.java, 174))
                    add(GryoTypeReg.of(B_NL_O_S_SE_SL_Traverser::class.java, 175))
                    add(GryoTypeReg.of(B_LP_NL_O_S_SE_SL_Traverser::class.java, 176))
                    add(GryoTypeReg.of(NL_O_OB_S_SE_SL_Traverser::class.java, 177))
                    add(GryoTypeReg.of(LP_NL_O_OB_S_SE_SL_Traverser::class.java, 178))
                    add(GryoTypeReg.of(LP_NL_O_OB_P_S_SE_SL_Traverser::class.java, 179))
                    add(GryoTypeReg.of(LabelledCounter::class.java, 180))
                    add(GryoTypeReg.of(Stack::class.java, 181))
                    add(GryoTypeReg.of(ReferenceMap::class.java, 182))

                    // placeholder serializers for classes that don't live here in core. this will allow them to be used if
                    // present  or ignored if the class isn't available. either way the registration numbers are held as
                    // placeholders so that the format stays stable
                    tryAddDynamicType(
                        this, "org.apache.tinkerpop4.gremlin.driver.message.RequestMessage",
                        "org.apache.tinkerpop4.gremlin.driver.ser.RequestMessageGryoSerializer", 167
                    )
                    tryAddDynamicType(
                        this, "org.apache.tinkerpop4.gremlin.driver.message.ResponseMessage",
                        "org.apache.tinkerpop4.gremlin.driver.ser.ResponseMessageGryoSerializer", 169
                    )
                    tryAddDynamicType(
                        this, "org.apache.tinkerpop4.gremlin.sparql.process.traversal.strategy.SparqlStrategy",
                        null, 184
                    )
                }
            }
        }

        fun initV1d0Registrations(): List<TypeRegistration<*>> {
            return object : ArrayList<TypeRegistration<*>?>() {
                init {
                    add(GryoTypeReg.of(ByteArray::class.java, 25))
                    add(GryoTypeReg.of(CharArray::class.java, 26))
                    add(GryoTypeReg.of(ShortArray::class.java, 27))
                    add(GryoTypeReg.of(IntArray::class.java, 28))
                    add(GryoTypeReg.of(LongArray::class.java, 29))
                    add(GryoTypeReg.of(FloatArray::class.java, 30))
                    add(GryoTypeReg.of(DoubleArray::class.java, 31))
                    add(GryoTypeReg.of(Array<String>::class.java, 32))
                    add(GryoTypeReg.of(Array<Object>::class.java, 33))
                    add(GryoTypeReg.of(ArrayList::class.java, 10))
                    add(GryoTypeReg.of(Types.ARRAYS_AS_LIST, 134, ArraysAsListSerializer()))
                    add(GryoTypeReg.of(BigInteger::class.java, 34))
                    add(GryoTypeReg.of(BigDecimal::class.java, 35))
                    add(GryoTypeReg.of(Calendar::class.java, 39))
                    add(GryoTypeReg.of(Class::class.java, 41, ClassSerializer()))
                    add(GryoTypeReg.of(Array<Class>::class.java, 166, ClassArraySerializer()))
                    add(GryoTypeReg.of(Collection::class.java, 37))
                    add(GryoTypeReg.of(Collections.EMPTY_LIST.getClass(), 51))
                    add(GryoTypeReg.of(Collections.EMPTY_MAP.getClass(), 52))
                    add(GryoTypeReg.of(Collections.EMPTY_SET.getClass(), 53))
                    add(GryoTypeReg.of(Collections.singleton(null).getClass(), 54))
                    add(GryoTypeReg.of(Collections.singletonList(null).getClass(), 24))
                    add(GryoTypeReg.of(Collections.singletonMap(null, null).getClass(), 23))
                    add(GryoTypeReg.of(Types.COLLECTIONS_SYNCHRONIZED_MAP, 185, SynchronizedMapSerializer()))
                    add(GryoTypeReg.of(Contains::class.java, 49))
                    add(GryoTypeReg.of(Currency::class.java, 40))
                    add(GryoTypeReg.of(Date::class.java, 38))
                    add(GryoTypeReg.of(Direction::class.java, 12))
                    add(GryoTypeReg.of(DetachedEdge::class.java, 21))
                    add(GryoTypeReg.of(DetachedVertexProperty::class.java, 20))
                    add(GryoTypeReg.of(DetachedProperty::class.java, 18))
                    add(GryoTypeReg.of(DetachedVertex::class.java, 19))
                    add(GryoTypeReg.of(DetachedPath::class.java, 60))
                    // skip 14
                    add(GryoTypeReg.of(EnumSet::class.java, 46))
                    add(GryoTypeReg.of(HashMap::class.java, 11))
                    add(GryoTypeReg.of(HashMap.Entry::class.java, 16))
                    add(GryoTypeReg.of(Types.HASH_MAP_NODE, 92))
                    add(GryoTypeReg.of(Types.HASH_MAP_TREE_NODE, 170))
                    add(GryoTypeReg.of(KryoSerializable::class.java, 36))
                    add(GryoTypeReg.of(LinkedHashMap::class.java, 47))
                    add(GryoTypeReg.of(LinkedHashSet::class.java, 71))
                    add(GryoTypeReg.of(LinkedList::class.java, 116))
                    add(GryoTypeReg.of(ConcurrentHashMap::class.java, 168))
                    add(GryoTypeReg.of(ConcurrentHashMap.Entry::class.java, 169))
                    add(GryoTypeReg.of(Types.LINKED_HASH_MAP_ENTRY_CLASS, 15))
                    add(GryoTypeReg.of(Locale::class.java, 22))
                    add(GryoTypeReg.of(StringBuffer::class.java, 43))
                    add(GryoTypeReg.of(StringBuilder::class.java, 44))
                    add(GryoTypeReg.of(T::class.java, 48))
                    add(GryoTypeReg.of(TimeZone::class.java, 42))
                    add(GryoTypeReg.of(TreeMap::class.java, 45))
                    add(GryoTypeReg.of(TreeSet::class.java, 50))
                    add(GryoTypeReg.of(UUID::class.java, 17, UUIDSerializer()))
                    add(GryoTypeReg.of(URI::class.java, 72, URISerializer()))
                    add(GryoTypeReg.of(VertexTerminator::class.java, 13))
                    add(GryoTypeReg.of(AbstractMap.SimpleEntry::class.java, 120))
                    add(GryoTypeReg.of(SimpleImmutableEntry::class.java, 121))
                    add(GryoTypeReg.of(java.sql.Timestamp::class.java, 161))
                    add(GryoTypeReg.of(InetAddress::class.java, 162, InetAddressSerializer()))
                    add(GryoTypeReg.of(ByteBuffer::class.java, 163, ByteBufferSerializer()))
                    add(GryoTypeReg.of(ReferenceEdge::class.java, 81))
                    add(GryoTypeReg.of(ReferenceVertexProperty::class.java, 82))
                    add(GryoTypeReg.of(ReferenceProperty::class.java, 83))
                    add(GryoTypeReg.of(ReferenceVertex::class.java, 84))
                    add(GryoTypeReg.of(ReferencePath::class.java, 85))
                    add(GryoTypeReg.of(StarGraph::class.java, 86, StarGraphSerializer(Direction.BOTH, GraphFilter())))
                    add(GryoTypeReg.of(Edge::class.java, 65, EdgeSerializer()))
                    add(GryoTypeReg.of(Vertex::class.java, 66, VertexSerializer()))
                    add(GryoTypeReg.of(Property::class.java, 67, PropertySerializer()))
                    add(GryoTypeReg.of(VertexProperty::class.java, 68, VertexPropertySerializer()))
                    add(GryoTypeReg.of(Path::class.java, 59, PathSerializer()))
                    // skip 55
                    add(GryoTypeReg.of(B_O_Traverser::class.java, 75))
                    add(GryoTypeReg.of(O_Traverser::class.java, 76))
                    add(GryoTypeReg.of(B_LP_O_P_S_SE_SL_Traverser::class.java, 77))
                    add(GryoTypeReg.of(B_O_S_SE_SL_Traverser::class.java, 78))
                    add(GryoTypeReg.of(B_LP_O_S_SE_SL_Traverser::class.java, 87))
                    add(GryoTypeReg.of(O_OB_S_SE_SL_Traverser::class.java, 89))
                    add(GryoTypeReg.of(LP_O_OB_S_SE_SL_Traverser::class.java, 90))
                    add(GryoTypeReg.of(LP_O_OB_P_S_SE_SL_Traverser::class.java, 91))
                    add(GryoTypeReg.of(ProjectedTraverser::class.java, 164))
                    add(GryoTypeReg.of(DefaultRemoteTraverser::class.java, 123, DefaultRemoteTraverserSerializer()))
                    add(GryoTypeReg.of(Bytecode::class.java, 122, BytecodeSerializer()))
                    add(GryoTypeReg.of(P::class.java, 124, PSerializer()))
                    add(GryoTypeReg.of(TextP::class.java, 186, TextPSerializer()))
                    add(GryoTypeReg.of(Lambda::class.java, 125, LambdaSerializer()))
                    add(GryoTypeReg.of(Bytecode.Binding::class.java, 126, BindingSerializer()))
                    add(GryoTypeReg.of(Order::class.java, 127))
                    add(GryoTypeReg.of(Scope::class.java, 128))
                    add(GryoTypeReg.of(VertexProperty.Cardinality::class.java, 131))
                    add(GryoTypeReg.of(Column::class.java, 132))
                    add(GryoTypeReg.of(Pop::class.java, 133))
                    add(GryoTypeReg.of(SackFunctions.Barrier::class.java, 135))
                    add(GryoTypeReg.of(Pick::class.java, 137))
                    add(GryoTypeReg.of(HashSetSupplier::class.java, 136, HashSetSupplierSerializer()))
                    add(GryoTypeReg.of(MultiComparator::class.java, 165))
                    add(GryoTypeReg.of(TraverserSet::class.java, 58))
                    add(GryoTypeReg.of(Tree::class.java, 61))
                    add(GryoTypeReg.of(HashSet::class.java, 62))
                    add(GryoTypeReg.of(BulkSet::class.java, 64))
                    add(GryoTypeReg.of(MutableMetrics::class.java, 69))
                    add(GryoTypeReg.of(ImmutableMetrics::class.java, 115))
                    add(GryoTypeReg.of(DefaultTraversalMetrics::class.java, 70))
                    add(GryoTypeReg.of(MapMemory::class.java, 73))
                    add(GryoTypeReg.of(MapReduce.NullObject::class.java, 74))
                    add(GryoTypeReg.of(AtomicLong::class.java, 79))
                    add(GryoTypeReg.of(Pair::class.java, 88, PairSerializer()))
                    add(GryoTypeReg.of(Triplet::class.java, 183, TripletSerializer()))
                    add(GryoTypeReg.of(TraversalExplanation::class.java, 106, JavaSerializer()))
                    add(GryoTypeReg.of(Duration::class.java, 93, DurationSerializer()))
                    add(GryoTypeReg.of(Instant::class.java, 94, InstantSerializer()))
                    add(GryoTypeReg.of(LocalDate::class.java, 95, LocalDateSerializer()))
                    add(GryoTypeReg.of(LocalDateTime::class.java, 96, LocalDateTimeSerializer()))
                    add(GryoTypeReg.of(LocalTime::class.java, 97, LocalTimeSerializer()))
                    add(GryoTypeReg.of(MonthDay::class.java, 98, MonthDaySerializer()))
                    add(GryoTypeReg.of(OffsetDateTime::class.java, 99, OffsetDateTimeSerializer()))
                    add(GryoTypeReg.of(OffsetTime::class.java, 100, OffsetTimeSerializer()))
                    add(GryoTypeReg.of(Period::class.java, 101, PeriodSerializer()))
                    add(GryoTypeReg.of(Year::class.java, 102, YearSerializer()))
                    add(GryoTypeReg.of(YearMonth::class.java, 103, YearMonthSerializer()))
                    add(GryoTypeReg.of(ZonedDateTime::class.java, 104, ZonedDateTimeSerializer()))
                    add(GryoTypeReg.of(ZoneOffset::class.java, 105, ZoneOffsetSerializer()))
                    add(GryoTypeReg.of(Operator::class.java, 107))
                    add(GryoTypeReg.of(FoldBiOperator::class.java, 108))
                    add(GryoTypeReg.of(GroupCountBiOperator::class.java, 109))
                    add(GryoTypeReg.of(GroupBiOperator::class.java, 117, JavaSerializer()))
                    add(GryoTypeReg.of(MeanGlobalBiOperator::class.java, 110))
                    add(GryoTypeReg.of(MeanNumber::class.java, 111))
                    add(GryoTypeReg.of(TreeBiOperator::class.java, 112))
                    add(GryoTypeReg.of(NonEmittingSeed::class.java, 194))

                    // skip 113
                    add(GryoTypeReg.of(RangeBiOperator::class.java, 114))
                    add(GryoTypeReg.of(OrderBiOperator::class.java, 118, JavaSerializer()))
                    add(GryoTypeReg.of(ProfileBiOperator::class.java, 119))
                    add(GryoTypeReg.of(ConnectiveStrategy::class.java, 138))
                    add(GryoTypeReg.of(HaltedTraverserStrategy::class.java, 139))
                    add(GryoTypeReg.of(PartitionStrategy::class.java, 140, JavaSerializer()))
                    add(GryoTypeReg.of(SubgraphStrategy::class.java, 141, JavaSerializer()))
                    add(GryoTypeReg.of(SeedStrategy::class.java, 192, JavaSerializer()))
                    add(GryoTypeReg.of(VertexProgramStrategy::class.java, 142, JavaSerializer()))
                    add(GryoTypeReg.of(MatchAlgorithmStrategy::class.java, 143))
                    add(GryoTypeReg.of(GreedyMatchAlgorithm::class.java, 144))
                    add(GryoTypeReg.of(AdjacentToIncidentStrategy::class.java, 145))
                    add(GryoTypeReg.of(ByModulatorOptimizationStrategy::class.java, 191))
                    add(GryoTypeReg.of(ProductiveByStrategy::class.java, 195, JavaSerializer())) // ***LAST ID***
                    add(GryoTypeReg.of(CountStrategy::class.java, 155))
                    add(GryoTypeReg.of(FilterRankingStrategy::class.java, 146))
                    add(GryoTypeReg.of(IdentityRemovalStrategy::class.java, 147))
                    add(GryoTypeReg.of(IncidentToAdjacentStrategy::class.java, 148))
                    add(GryoTypeReg.of(InlineFilterStrategy::class.java, 149))
                    add(GryoTypeReg.of(LazyBarrierStrategy::class.java, 150))
                    add(GryoTypeReg.of(MatchPredicateStrategy::class.java, 151))
                    add(GryoTypeReg.of(OrderLimitStrategy::class.java, 152))
                    add(GryoTypeReg.of(OptionsStrategy::class.java, 187, JavaSerializer()))
                    add(GryoTypeReg.of(PathProcessorStrategy::class.java, 153))
                    add(GryoTypeReg.of(PathRetractionStrategy::class.java, 154))
                    add(GryoTypeReg.of(RepeatUnrollStrategy::class.java, 156))
                    add(GryoTypeReg.of(GraphFilterStrategy::class.java, 157))
                    add(GryoTypeReg.of(LambdaRestrictionStrategy::class.java, 158))
                    add(GryoTypeReg.of(ReadOnlyStrategy::class.java, 159))
                    add(GryoTypeReg.of(EarlyLimitStrategy::class.java, 188))
                    add(GryoTypeReg.of(CountMatchAlgorithm::class.java, 160))
                    add(GryoTypeReg.of(GreedyMatchAlgorithm::class.java, 167))
                    add(GryoTypeReg.of(EdgeLabelVerificationStrategy::class.java, 189))
                    add(GryoTypeReg.of(ReservedKeysVerificationStrategy::class.java, 190))
                    // skip 171, 172 to sync with the 3.3.x
                    add(GryoTypeReg.of(VertexIndexedTraverserSet::class.java, 173))
                    add(GryoTypeReg.of(B_LP_NL_O_P_S_SE_SL_Traverser::class.java, 174))
                    add(GryoTypeReg.of(B_NL_O_S_SE_SL_Traverser::class.java, 175))
                    add(GryoTypeReg.of(B_LP_NL_O_S_SE_SL_Traverser::class.java, 176))
                    add(GryoTypeReg.of(NL_O_OB_S_SE_SL_Traverser::class.java, 177))
                    add(GryoTypeReg.of(LP_NL_O_OB_S_SE_SL_Traverser::class.java, 178))
                    add(GryoTypeReg.of(LP_NL_O_OB_P_S_SE_SL_Traverser::class.java, 179))
                    add(GryoTypeReg.of(LabelledCounter::class.java, 180))
                    add(GryoTypeReg.of(Stack::class.java, 181))
                    add(GryoTypeReg.of(ReferenceMap::class.java, 182))
                }
            }
        }

        private fun tryAddDynamicType(
            types: List<TypeRegistration<*>>, type: String,
            serializer: String?, registrationId: Int
        ) {
            try {
                val typeClass: Class = Class.forName(type)
                val serializerInstance: Optional<SerializerShim<*>> = Optional.ofNullable(serializer)
                    .map(FunctionUtils.wrapFunction(Class::forName))
                    .map(FunctionUtils.wrapFunction { c -> c.getConstructor().newInstance() as SerializerShim<*> })
                if (serializerInstance.isPresent()) {
                    types.add(GryoTypeReg.of(typeClass, registrationId, serializerInstance.get()))
                } else {
                    types.add(GryoTypeReg.of(typeClass, registrationId))
                }
            } catch (ignored: Exception) {
                // if the class isn't here - no worries
            }
        }
    }
}