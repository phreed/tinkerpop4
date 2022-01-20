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
package org.apache.tinkerpop4.gremlin.structure.io.binary

import org.apache.tinkerpop4.gremlin.structure.io.binary.types.*

class TypeSerializerRegistry private constructor(
    entries: Collection<RegistryEntry<*>>,
    fallbackResolver: Function<Class<*>, TypeSerializer<*>>
) {
    class Builder {
        private val list: List<RegistryEntry<*>> = LinkedList()
        private var fallbackResolver: Function<Class<*>, TypeSerializer<*>>? = null

        /**
         * Adds a serializer for a built-in type.
         *
         *
         * Note that when providing a serializer for an Interface ([VertexProperty], [Property],
         * [Vertex], ...), the serializer resolution from the registry will be defined by the order that
         * it was provided. In this case, you should provide the type serializer starting from most specific to
         * less specific or to put it in other words, start from derived types and then parent types, e.g.,
         * [VertexProperty] before [Property].
         *
         */
        fun <DT> add(type: Class<DT>, serializer: TypeSerializer<DT>): Builder {
            if (serializer.getDataType() === DataType.CUSTOM) {
                throw IllegalArgumentException("DataType can not be CUSTOM, use addCustomType() method instead")
            }
            if (serializer.getDataType() === DataType.UNSPECIFIED_NULL) {
                throw IllegalArgumentException("Adding a serializer for a UNSPECIFIED_NULL is not permitted")
            }
            if (serializer is CustomTypeSerializer) {
                throw IllegalArgumentException(
                    "CustomTypeSerializer implementations are reserved for customtypes"
                )
            }
            list.add(RegistryEntry<DT>(type, serializer))
            return this
        }

        /**
         * Adds a serializer for a custom type.
         */
        fun <DT> addCustomType(type: Class<DT>?, serializer: CustomTypeSerializer<DT>?): Builder {
            if (serializer == null) {
                throw NullPointerException("serializer can not be null")
            }
            if (serializer.getDataType() !== DataType.CUSTOM) {
                throw IllegalArgumentException("Custom serializer must use CUSTOM data type")
            }
            if (serializer.getTypeName() == null) {
                throw NullPointerException("serializer custom type name can not be null")
            }
            list.add(RegistryEntry<DT>(type, serializer))
            return this
        }

        /**
         * Provides a way to resolve the type serializer to use when there isn't any direct match.
         */
        fun withFallbackResolver(fallbackResolver: Function<Class<*>?, TypeSerializer<*>?>): Builder {
            this.fallbackResolver = fallbackResolver
            return this
        }

        /**
         * Add [CustomTypeSerializer] by way of an [IoRegistry]. The registry entries should be bound to
         * [GraphBinaryIo].
         */
        fun addRegistry(registry: IoRegistry?): Builder {
            if (null == registry) throw IllegalArgumentException("The registry cannot be null")
            val classSerializers: List<Pair<Class, CustomTypeSerializer>> =
                registry.find(GraphBinaryIo::class.java, CustomTypeSerializer::class.java)
            for (cs in classSerializers) {
                addCustomType(cs.getValue0(), cs.getValue1())
            }
            return this
        }

        /**
         * Creates a new [TypeSerializerRegistry] instance based on the serializers added.
         */
        fun create(): TypeSerializerRegistry {
            return TypeSerializerRegistry(list, fallbackResolver)
        }
    }

    private class RegistryEntry<DT> private constructor(type: Class<DT>, typeSerializer: TypeSerializer<DT>) {
        private val type: Class<DT>
        private val typeSerializer: TypeSerializer<DT>

        init {
            this.type = type
            this.typeSerializer = typeSerializer
        }

        fun getType(): Class<DT> {
            return type
        }

        val dataType: DataType
            get() = typeSerializer.getDataType()
        val customTypeName: String?
            get() {
                if (getDataType() !== DataType.CUSTOM) {
                    return null
                }
                val customTypeSerializer: CustomTypeSerializer = typeSerializer as CustomTypeSerializer
                return customTypeSerializer.getTypeName()
            }

        fun getTypeSerializer(): TypeSerializer<DT> {
            return typeSerializer
        }
    }

    private val serializers: Map<Class<*>, TypeSerializer<*>> = HashMap()
    private val serializersByInterface: Map<Class<*>, TypeSerializer<*>> = LinkedHashMap()
    private val serializersByDataType: Map<DataType, TypeSerializer<*>> = HashMap()
    private val serializersByCustomTypeName: Map<String, CustomTypeSerializer> = HashMap()
    private val fallbackResolver: Function<Class<*>, TypeSerializer<*>>?

    /**
     * Stores serializers by class, where the class resolution involved a lookup or special conditions.
     * This is used for interface implementations and enums.
     */
    private val serializersByImplementation: ConcurrentHashMap<Class<*>, TypeSerializer<*>> = ConcurrentHashMap()

    init {
        val providedTypes: Set<Class> = HashSet(entries.size())

        // Include user-provided entries first
        for (entry in entries) {
            put(entry)
            providedTypes.add(entry.type)
        }

        // Followed by the defaults
        Arrays.stream(defaultEntries).filter { e -> !providedTypes.contains(e.type) }
            .forEach { entry: RegistryEntry<*> -> put(entry) }
        this.fallbackResolver = fallbackResolver
    }

    private fun put(entry: RegistryEntry<*>) {
        val type: Class = entry.getType()
        val serializer: TypeSerializer = entry.getTypeSerializer()
        if (type == null) {
            throw NullPointerException("Type can not be null")
        }
        if (serializer == null) {
            throw NullPointerException("Serializer instance can not be null")
        }
        if (serializer.getDataType() == null && serializer !is TransformSerializer) {
            throw NullPointerException("Serializer data type can not be null")
        }
        if (!type.isInterface() && !Modifier.isAbstract(type.getModifiers())) {
            // Direct class match
            serializers.put(type, serializer)
        } else {
            // Interface or abstract class can be assigned by provided type
            serializersByInterface.put(type, serializer)
        }
        if (serializer.getDataType() === DataType.CUSTOM) {
            serializersByCustomTypeName.put(entry.getCustomTypeName(), serializer as CustomTypeSerializer)
        } else if (serializer.getDataType() != null) {
            serializersByDataType.put(serializer.getDataType(), serializer)
        }
    }

    @Throws(IOException::class)
    fun <DT> getSerializer(type: Class<DT>): TypeSerializer<DT>? {
        var serializer: TypeSerializer<*>? = serializers[type]
        if (null == serializer) {
            // Try to obtain the serializer by the type interface implementation or superclass,
            // when previously accessed.
            serializer = serializersByImplementation.get(type)
        }
        if (serializer != null) {
            return serializer as TypeSerializer?
        }

        // Use different lookup techniques and cache the lookup result when successful
        if (Enum::class.java.isAssignableFrom(type)) {
            // maybe it's a enum - enums with bodies are weird in java, they are subclasses of themselves, so
            // Columns.values will be of type Column$2.
            serializer = serializers[type.getSuperclass()]
        }
        if (null == serializer) {
            // Lookup by interface
            for (entry in serializersByInterface.entrySet()) {
                if (entry.getKey().isAssignableFrom(type)) {
                    serializer = entry.getValue()
                    break
                }
            }
        }
        if (null == serializer && fallbackResolver != null) {
            serializer = fallbackResolver.apply(type)
        }
        validateInstance(serializer, type.getTypeName())

        // Store the lookup match to avoid looking it up in the future
        serializersByImplementation.put(type, serializer)
        return serializer as TypeSerializer<DT>?
    }

    @Throws(IOException::class)
    fun <DT> getSerializer(dataType: DataType): TypeSerializer<DT> {
        if (dataType === DataType.CUSTOM) {
            throw IllegalArgumentException("Custom type serializers can not be retrieved using this method")
        }
        return validateInstance(serializersByDataType[dataType], dataType.toString())
    }

    /**
     * Gets the serializer for a given custom type name.
     */
    @Throws(IOException::class)
    fun <DT> getSerializerForCustomType(name: String): CustomTypeSerializer<DT> {
        return serializersByCustomTypeName[name]
            ?: throw IOException(String.format("Serializer for custom type '%s' not found", name))
    }

    companion object {
        fun build(): Builder {
            return Builder()
        }

        private val defaultEntries: Array<RegistryEntry<*>> = arrayOf(
            RegistryEntry<DT>(Integer::class.java, SingleTypeSerializer.IntSerializer),
            RegistryEntry<DT>(Long::class.java, SingleTypeSerializer.LongSerializer),
            RegistryEntry<DT>(String::class.java, StringSerializer()),
            RegistryEntry<DT>(Date::class.java, DateSerializer.DateSerializer),
            RegistryEntry<DT>(Timestamp::class.java, DateSerializer.TimestampSerializer),
            RegistryEntry<DT>(Class::class.java, ClassSerializer()),
            RegistryEntry<DT>(Double::class.java, SingleTypeSerializer.DoubleSerializer),
            RegistryEntry<DT>(Float::class.java, SingleTypeSerializer.FloatSerializer),
            RegistryEntry<DT>(List::class.java, ListSerializer()),
            RegistryEntry<DT>(Map::class.java, MapSerializer()),
            RegistryEntry<DT>(Set::class.java, SetSerializer()),
            RegistryEntry<DT>(UUID::class.java, UUIDSerializer()),
            RegistryEntry<DT>(Edge::class.java, EdgeSerializer()),
            RegistryEntry<DT>(Path::class.java, PathSerializer()),
            RegistryEntry<DT>(
                VertexProperty::class.java,
                VertexPropertySerializer()
            ),  // needs to register before the less specific Property
            RegistryEntry<DT>(Property::class.java, PropertySerializer()),
            RegistryEntry<DT>(Graph::class.java, GraphSerializer()),
            RegistryEntry<DT>(Vertex::class.java, VertexSerializer()),
            RegistryEntry<DT>(SackFunctions.Barrier::class.java, EnumSerializer.BarrierSerializer),
            RegistryEntry<DT>(Bytecode.Binding::class.java, BindingSerializer()),
            RegistryEntry<DT>(Bytecode::class.java, ByteCodeSerializer()),
            RegistryEntry<DT>(VertexProperty.Cardinality::class.java, EnumSerializer.CardinalitySerializer),
            RegistryEntry<DT>(Column::class.java, EnumSerializer.ColumnSerializer),
            RegistryEntry<DT>(Direction::class.java, EnumSerializer.DirectionSerializer),
            RegistryEntry<DT>(Operator::class.java, EnumSerializer.OperatorSerializer),
            RegistryEntry<DT>(Order::class.java, EnumSerializer.OrderSerializer),
            RegistryEntry<DT>(Pick::class.java, EnumSerializer.PickSerializer),
            RegistryEntry<DT>(Pop::class.java, EnumSerializer.PopSerializer),
            RegistryEntry<DT>(Lambda::class.java, LambdaSerializer()),
            RegistryEntry<DT>(P::class.java, PSerializer(DataType.P, P::class.java)),
            RegistryEntry<DT>(AndP::class.java, PSerializer(DataType.P, AndP::class.java)),
            RegistryEntry<DT>(OrP::class.java, PSerializer(DataType.P, OrP::class.java)),
            RegistryEntry<DT>(TextP::class.java, PSerializer(DataType.TEXTP, TextP::class.java)),
            RegistryEntry<DT>(Scope::class.java, EnumSerializer.ScopeSerializer),
            RegistryEntry<DT>(T::class.java, EnumSerializer.TSerializer),
            RegistryEntry<DT>(Traverser::class.java, TraverserSerializer()),
            RegistryEntry<DT>(BigDecimal::class.java, BigDecimalSerializer()),
            RegistryEntry<DT>(BigInteger::class.java, BigIntegerSerializer()),
            RegistryEntry<DT>(Byte::class.java, SingleTypeSerializer.ByteSerializer),
            RegistryEntry<DT>(ByteBuffer::class.java, ByteBufferSerializer()),
            RegistryEntry<DT>(Short::class.java, SingleTypeSerializer.ShortSerializer),
            RegistryEntry<DT>(Boolean::class.java, SingleTypeSerializer.BooleanSerializer),
            RegistryEntry<DT>(TraversalStrategy::class.java, TraversalStrategySerializer()),
            RegistryEntry<DT>(BulkSet::class.java, BulkSetSerializer()),
            RegistryEntry<DT>(Tree::class.java, TreeSerializer()),
            RegistryEntry<DT>(Metrics::class.java, MetricsSerializer()),
            RegistryEntry<DT>(
                TraversalMetrics::class.java,
                TraversalMetricsSerializer()
            ),  // TransformSerializer implementations
            RegistryEntry<DT>(Map.Entry::class.java, MapEntrySerializer()),
            RegistryEntry<DT>(TraversalExplanation::class.java, TraversalExplanationSerializer()),
            RegistryEntry<DT>(Character::class.java, CharSerializer()),
            RegistryEntry<DT>(Duration::class.java, DurationSerializer()),
            RegistryEntry<DT>(InetAddress::class.java, InetAddressSerializer()),
            RegistryEntry<DT>(Inet4Address::class.java, InetAddressSerializer()),
            RegistryEntry<DT>(Inet6Address::class.java, InetAddressSerializer()),
            RegistryEntry<DT>(Instant::class.java, InstantSerializer()),
            RegistryEntry<DT>(LocalDate::class.java, LocalDateSerializer()),
            RegistryEntry<DT>(LocalTime::class.java, LocalTimeSerializer()),
            RegistryEntry<DT>(LocalDateTime::class.java, LocalDateTimeSerializer()),
            RegistryEntry<DT>(MonthDay::class.java, MonthDaySerializer()),
            RegistryEntry<DT>(OffsetDateTime::class.java, OffsetDateTimeSerializer()),
            RegistryEntry<DT>(OffsetTime::class.java, OffsetTimeSerializer()),
            RegistryEntry<DT>(Period::class.java, PeriodSerializer()),
            RegistryEntry<DT>(Year::class.java, SingleTypeSerializer.YearSerializer),
            RegistryEntry<DT>(YearMonth::class.java, YearMonthSerializer()),
            RegistryEntry<DT>(ZonedDateTime::class.java, ZonedDateTimeSerializer()),
            RegistryEntry<DT>(ZoneOffset::class.java, ZoneOffsetSerializer())
        )
        val INSTANCE = build().create()
        @Throws(IOException::class)
        private fun validateInstance(serializer: TypeSerializer?, typeName: String): TypeSerializer {
            if (serializer == null) {
                throw IOException(String.format("Serializer for type %s not found", typeName))
            }
            return serializer
        }
    }
}