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

import java.math.BigDecimal

/**
 * Version 2.0 of GraphSON extensions.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class GraphSONXModuleV2d0 protected constructor(normalize: Boolean) : GraphSONModule("graphsonx-2.0") {
    /**
     * Constructs a new object.
     */
    init {

        /////////////////////// SERIALIZERS ////////////////////////////

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

        /////////////////////// DESERIALIZERS ////////////////////////////

        // java.time
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

    @get:Override
    val typeDefinitions: Map<Any, String>
        get() = TYPE_DEFINITIONS

    @get:Override
    val typeNamespace: String
        get() = GraphSONTokens.GREMLINX_TYPE_NAMESPACE

    class Builder private constructor() : GraphSONModuleBuilder {
        @Override
        fun create(normalize: Boolean): GraphSONModule {
            return GraphSONXModuleV2d0(normalize)
        }
    }

    companion object {
        private val TYPE_DEFINITIONS: Map<Class, String> = Collections.unmodifiableMap(
            object : LinkedHashMap<Class?, String?>() {
                init {
                    put(ByteBuffer::class.java, "ByteBuffer")
                    put(Short::class.java, "Int16")
                    put(BigInteger::class.java, "BigInteger")
                    put(BigDecimal::class.java, "BigDecimal")
                    put(Byte::class.java, "Byte")
                    put(Character::class.java, "Char")
                    put(InetAddress::class.java, "InetAddress")

                    // Time serializers/deserializers
                    put(Duration::class.java, "Duration")
                    put(Instant::class.java, "Instant")
                    put(LocalDate::class.java, "LocalDate")
                    put(LocalDateTime::class.java, "LocalDateTime")
                    put(LocalTime::class.java, "LocalTime")
                    put(MonthDay::class.java, "MonthDay")
                    put(OffsetDateTime::class.java, "OffsetDateTime")
                    put(OffsetTime::class.java, "OffsetTime")
                    put(Period::class.java, "Period")
                    put(Year::class.java, "Year")
                    put(YearMonth::class.java, "YearMonth")
                    put(ZonedDateTime::class.java, "ZonedDateTime")
                    put(ZoneOffset::class.java, "ZoneOffset")
                }
            })

        fun build(): Builder {
            return Builder()
        }
    }
}