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

import org.apache.tinkerpop.shaded.jackson.core.JsonGenerator

/**
 * GraphSON serializers for classes in `java.time.*` for the version 2.0 of GraphSON.
 */
internal class JavaTimeSerializersV2d0 private constructor() {
    /**
     * Base class for serializing the `java.time.*` to ISO-8061 formats.
     */
    internal abstract class AbstractJavaTimeSerializer<T>(clazz: Class<T>?) : StdSerializer<T>(clazz) {
        @Override
        @Throws(IOException::class)
        fun serialize(
            value: T, gen: JsonGenerator,
            serializerProvider: SerializerProvider?
        ) {
            gen.writeString(value.toString())
        }

        @Override
        @Throws(IOException::class)
        fun serializeWithType(
            value: T, gen: JsonGenerator,
            serializers: SerializerProvider?, typeSer: TypeSerializer
        ) {
            typeSer.writeTypePrefixForScalar(value, gen)
            gen.writeString(value.toString())
            typeSer.writeTypeSuffixForScalar(value, gen)
        }
    }

    /**
     * Base class for serializing the `java.time.*` from ISO-8061 formats.
     */
    internal abstract class AbstractJavaTimeJacksonDeserializer<T>(clazz: Class<T>?) : StdDeserializer<T>(clazz) {
        abstract fun parse(`val`: String?): T
        @Override
        @Throws(IOException::class)
        fun deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext?): T {
            return parse(jsonParser.getText())
        }

        @get:Override
        val isCachable: Boolean
            get() = true
    }

    internal class DurationJacksonSerializer : AbstractJavaTimeSerializer<Duration?>(
        Duration::class.java
    )

    internal class DurationJacksonDeserializer : AbstractJavaTimeJacksonDeserializer<Duration?>(
        Duration::class.java
    ) {
        @Override
        override fun parse(`val`: String?): Duration {
            return Duration.parse(`val`)
        }
    }

    internal class InstantJacksonSerializer : AbstractJavaTimeSerializer<Instant?>(
        Instant::class.java
    )

    internal class InstantJacksonDeserializer : AbstractJavaTimeJacksonDeserializer<Instant?>(
        Instant::class.java
    ) {
        @Override
        override fun parse(`val`: String?): Instant {
            return Instant.parse(`val`)
        }
    }

    internal class LocalDateJacksonSerializer : AbstractJavaTimeSerializer<LocalDate?>(
        LocalDate::class.java
    )

    internal class LocalDateJacksonDeserializer : AbstractJavaTimeJacksonDeserializer<LocalDate?>(
        LocalDate::class.java
    ) {
        @Override
        override fun parse(`val`: String?): LocalDate {
            return LocalDate.parse(`val`)
        }
    }

    internal class LocalDateTimeJacksonSerializer : AbstractJavaTimeSerializer<LocalDateTime?>(
        LocalDateTime::class.java
    )

    internal class LocalDateTimeJacksonDeserializer : AbstractJavaTimeJacksonDeserializer<LocalDateTime?>(
        LocalDateTime::class.java
    ) {
        @Override
        override fun parse(`val`: String?): LocalDateTime {
            return LocalDateTime.parse(`val`)
        }
    }

    internal class LocalTimeJacksonSerializer : AbstractJavaTimeSerializer<LocalTime?>(
        LocalTime::class.java
    )

    internal class LocalTimeJacksonDeserializer : AbstractJavaTimeJacksonDeserializer<LocalTime?>(
        LocalTime::class.java
    ) {
        @Override
        override fun parse(`val`: String?): LocalTime {
            return LocalTime.parse(`val`)
        }
    }

    internal class MonthDayJacksonSerializer : AbstractJavaTimeSerializer<MonthDay?>(
        MonthDay::class.java
    )

    internal class MonthDayJacksonDeserializer : AbstractJavaTimeJacksonDeserializer<MonthDay?>(
        MonthDay::class.java
    ) {
        @Override
        override fun parse(`val`: String?): MonthDay {
            return MonthDay.parse(`val`)
        }
    }

    internal class OffsetDateTimeJacksonSerializer : AbstractJavaTimeSerializer<OffsetDateTime?>(
        OffsetDateTime::class.java
    )

    internal class OffsetDateTimeJacksonDeserializer : AbstractJavaTimeJacksonDeserializer<OffsetDateTime?>(
        OffsetDateTime::class.java
    ) {
        @Override
        override fun parse(`val`: String?): OffsetDateTime {
            return OffsetDateTime.parse(`val`)
        }
    }

    internal class OffsetTimeJacksonSerializer : AbstractJavaTimeSerializer<OffsetTime?>(
        OffsetTime::class.java
    )

    internal class OffsetTimeJacksonDeserializer : AbstractJavaTimeJacksonDeserializer<OffsetTime?>(
        OffsetTime::class.java
    ) {
        @Override
        override fun parse(`val`: String?): OffsetTime {
            return OffsetTime.parse(`val`)
        }
    }

    internal class PeriodJacksonSerializer : AbstractJavaTimeSerializer<Period?>(
        Period::class.java
    )

    internal class PeriodJacksonDeserializer : AbstractJavaTimeJacksonDeserializer<Period?>(
        Period::class.java
    ) {
        @Override
        override fun parse(`val`: String?): Period {
            return Period.parse(`val`)
        }
    }

    internal class YearJacksonSerializer : AbstractJavaTimeSerializer<Year?>(
        Year::class.java
    )

    internal class YearJacksonDeserializer : AbstractJavaTimeJacksonDeserializer<Year?>(
        Year::class.java
    ) {
        @Override
        override fun parse(`val`: String?): Year {
            return Year.parse(`val`)
        }
    }

    internal class YearMonthJacksonSerializer : AbstractJavaTimeSerializer<YearMonth?>(
        YearMonth::class.java
    )

    internal class YearMonthJacksonDeserializer : AbstractJavaTimeJacksonDeserializer<YearMonth?>(
        YearMonth::class.java
    ) {
        @Override
        override fun parse(`val`: String?): YearMonth {
            return YearMonth.parse(`val`)
        }
    }

    internal class ZonedDateTimeJacksonSerializer : AbstractJavaTimeSerializer<ZonedDateTime?>(
        ZonedDateTime::class.java
    )

    internal class ZonedDateTimeJacksonDeserializer : AbstractJavaTimeJacksonDeserializer<ZonedDateTime?>(
        ZonedDateTime::class.java
    ) {
        @Override
        override fun parse(`val`: String?): ZonedDateTime {
            return ZonedDateTime.parse(`val`)
        }
    }

    internal class ZoneOffsetJacksonSerializer : AbstractJavaTimeSerializer<ZoneOffset?>(
        ZoneOffset::class.java
    )

    internal class ZoneOffsetJacksonDeserializer : AbstractJavaTimeJacksonDeserializer<ZoneOffset?>(
        ZoneOffset::class.java
    ) {
        @Override
        override fun parse(`val`: String?): ZoneOffset {
            return ZoneOffset.of(`val`)
        }
    }
}