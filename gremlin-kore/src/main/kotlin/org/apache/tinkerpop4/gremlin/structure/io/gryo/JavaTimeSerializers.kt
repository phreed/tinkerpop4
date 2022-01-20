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

import org.apache.tinkerpop4.gremlin.structure.io.gryo.kryoshim.InputShim
import org.apache.tinkerpop4.gremlin.structure.io.gryo.kryoshim.KryoShim
import org.apache.tinkerpop4.gremlin.structure.io.gryo.kryoshim.OutputShim
import org.apache.tinkerpop4.gremlin.structure.io.gryo.kryoshim.SerializerShim
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.MonthDay
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.Period
import java.time.Year
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Serializers for classes in the `java.time` package.
 */
internal class JavaTimeSerializers private constructor() {
    /**
     * Serializer for the [Duration] class.
     */
    internal class DurationSerializer : SerializerShim<Duration?> {
        @Override
        fun <O : OutputShim?> write(kryo: KryoShim<*, O>?, output: O, duration: Duration) {
            output.writeLong(duration.toNanos())
        }

        @Override
        fun <I : InputShim?> read(kryo: KryoShim<I, *>?, input: I, durationClass: Class<Duration?>?): Duration {
            return Duration.ofNanos(input.readLong())
        }
    }

    /**
     * Serializer for the [Instant] class.
     */
    internal class InstantSerializer : SerializerShim<Instant?> {
        @Override
        fun <O : OutputShim?> write(kryo: KryoShim<*, O>?, output: O, instant: Instant) {
            output.writeLong(instant.getEpochSecond())
            output.writeInt(instant.getNano())
        }

        @Override
        fun <I : InputShim?> read(kryo: KryoShim<I, *>?, input: I, aClass: Class<Instant?>?): Instant {
            return Instant.ofEpochSecond(input.readLong(), input.readInt())
        }
    }

    /**
     * Serializer for the [LocalDate] class.
     */
    internal class LocalDateSerializer : SerializerShim<LocalDate?> {
        @Override
        fun <O : OutputShim?> write(kryo: KryoShim<*, O>?, output: O, localDate: LocalDate) {
            output.writeLong(localDate.toEpochDay())
        }

        @Override
        fun <I : InputShim?> read(kryo: KryoShim<I, *>?, input: I, clazz: Class<LocalDate?>?): LocalDate {
            return LocalDate.ofEpochDay(input.readLong())
        }
    }

    /**
     * Serializer for the [LocalDateTime] class.
     */
    internal class LocalDateTimeSerializer : SerializerShim<LocalDateTime?> {
        @Override
        fun <O : OutputShim?> write(kryo: KryoShim<*, O>?, output: O, localDateTime: LocalDateTime) {
            output.writeInt(localDateTime.getYear())
            output.writeInt(localDateTime.getMonthValue())
            output.writeInt(localDateTime.getDayOfMonth())
            output.writeInt(localDateTime.getHour())
            output.writeInt(localDateTime.getMinute())
            output.writeInt(localDateTime.getSecond())
            output.writeInt(localDateTime.getNano())
        }

        @Override
        fun <I : InputShim?> read(kryo: KryoShim<I, *>?, input: I, clazz: Class<LocalDateTime?>?): LocalDateTime {
            return LocalDateTime.of(
                input.readInt(),
                input.readInt(),
                input.readInt(),
                input.readInt(),
                input.readInt(),
                input.readInt(),
                input.readInt()
            )
        }
    }

    /**
     * Serializer for the [LocalTime] class.
     */
    internal class LocalTimeSerializer : SerializerShim<LocalTime?> {
        @Override
        fun <O : OutputShim?> write(kryo: KryoShim<*, O>?, output: O, localTime: LocalTime) {
            output.writeLong(localTime.toNanoOfDay())
        }

        @Override
        fun <I : InputShim?> read(kryo: KryoShim<I, *>?, input: I, clazz: Class<LocalTime?>?): LocalTime {
            return LocalTime.ofNanoOfDay(input.readLong())
        }
    }

    /**
     * Serializer for the [MonthDay] class.
     */
    internal class MonthDaySerializer : SerializerShim<MonthDay?> {
        @Override
        fun <O : OutputShim?> write(kryo: KryoShim<*, O>?, output: O, monthDay: MonthDay) {
            output.writeInt(monthDay.getMonthValue())
            output.writeInt(monthDay.getDayOfMonth())
        }

        @Override
        fun <I : InputShim?> read(kryo: KryoShim<I, *>?, input: I, clazz: Class<MonthDay?>?): MonthDay {
            return MonthDay.of(input.readInt(), input.readInt())
        }
    }

    /**
     * Serializer for the [OffsetDateTime] class.
     */
    internal class OffsetDateTimeSerializer : SerializerShim<OffsetDateTime?> {
        @Override
        fun <O : OutputShim?> write(kryo: KryoShim<*, O>, output: O, offsetDateTime: OffsetDateTime) {
            kryo.writeObject(output, offsetDateTime.toLocalDateTime())
            kryo.writeObject(output, offsetDateTime.getOffset())
        }

        @Override
        fun <I : InputShim?> read(kryo: KryoShim<I, *>, input: I, clazz: Class<OffsetDateTime?>?): OffsetDateTime {
            return OffsetDateTime.of(
                kryo.readObject(input, LocalDateTime::class.java),
                kryo.readObject(input, ZoneOffset::class.java)
            )
        }
    }

    /**
     * Serializer for the [OffsetTime] class.
     */
    internal class OffsetTimeSerializer : SerializerShim<OffsetTime?> {
        @Override
        fun <O : OutputShim?> write(kryo: KryoShim<*, O>, output: O, offsetTime: OffsetTime) {
            kryo.writeObject(output, offsetTime.toLocalTime())
            kryo.writeObject(output, offsetTime.getOffset())
        }

        @Override
        fun <I : InputShim?> read(kryo: KryoShim<I, *>, input: I, clazz: Class<OffsetTime?>?): OffsetTime {
            return OffsetTime.of(
                kryo.readObject(input, LocalTime::class.java),
                kryo.readObject(input, ZoneOffset::class.java)
            )
        }
    }

    /**
     * Serializer for the [Period] class.
     */
    internal class PeriodSerializer : SerializerShim<Period?> {
        @Override
        fun <O : OutputShim?> write(kryo: KryoShim<*, O>?, output: O, period: Period) {
            output.writeInt(period.getYears())
            output.writeInt(period.getMonths())
            output.writeInt(period.getDays())
        }

        @Override
        fun <I : InputShim?> read(kryo: KryoShim<I, *>?, input: I, clazz: Class<Period?>?): Period {
            return Period.of(input.readInt(), input.readInt(), input.readInt())
        }
    }

    /**
     * Serializer for the [Year] class.
     */
    internal class YearSerializer : SerializerShim<Year?> {
        @Override
        fun <O : OutputShim?> write(kryo: KryoShim<*, O>?, output: O, year: Year) {
            output.writeInt(year.getValue())
        }

        @Override
        fun <I : InputShim?> read(kryo: KryoShim<I, *>?, input: I, clazz: Class<Year?>?): Year {
            return Year.of(input.readInt())
        }
    }

    /**
     * Serializer for the [YearMonth] class.
     */
    internal class YearMonthSerializer : SerializerShim<YearMonth?> {
        @Override
        fun <O : OutputShim?> write(kryo: KryoShim<*, O>?, output: O, monthDay: YearMonth) {
            output.writeInt(monthDay.getYear())
            output.writeInt(monthDay.getMonthValue())
        }

        @Override
        fun <I : InputShim?> read(kryo: KryoShim<I, *>?, input: I, clazz: Class<YearMonth?>?): YearMonth {
            return YearMonth.of(input.readInt(), input.readInt())
        }
    }

    /**
     * Serializer for the [ZonedDateTime] class.
     */
    internal class ZonedDateTimeSerializer : SerializerShim<ZonedDateTime?> {
        @Override
        fun <O : OutputShim?> write(kryo: KryoShim<*, O>?, output: O, zonedDateTime: ZonedDateTime) {
            output.writeInt(zonedDateTime.getYear())
            output.writeInt(zonedDateTime.getMonthValue())
            output.writeInt(zonedDateTime.getDayOfMonth())
            output.writeInt(zonedDateTime.getHour())
            output.writeInt(zonedDateTime.getMinute())
            output.writeInt(zonedDateTime.getSecond())
            output.writeInt(zonedDateTime.getNano())
            output.writeString(zonedDateTime.getZone().getId())
        }

        @Override
        fun <I : InputShim?> read(kryo: KryoShim<I, *>?, input: I, clazz: Class<ZonedDateTime?>?): ZonedDateTime {
            return ZonedDateTime.of(
                input.readInt(), input.readInt(), input.readInt(),
                input.readInt(), input.readInt(), input.readInt(), input.readInt(),
                ZoneId.of(input.readString())
            )
        }
    }

    /**
     * Serializer for the [ZoneOffset] class.
     */
    internal class ZoneOffsetSerializer : SerializerShim<ZoneOffset?> {
        @Override
        fun <O : OutputShim?> write(kryo: KryoShim<*, O>?, output: O, zoneOffset: ZoneOffset) {
            output.writeString(zoneOffset.getId())
        }

        @Override
        fun <I : InputShim?> read(kryo: KryoShim<I, *>?, input: I, clazz: Class<ZoneOffset?>?): ZoneOffset {
            return ZoneOffset.of(input.readString())
        }
    }
}