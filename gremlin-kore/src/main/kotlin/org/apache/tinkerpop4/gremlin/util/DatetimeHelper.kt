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
package org.apache.tinkerpop4.gremlin.util

import java.time.Instant

object DatetimeHelper {
    private val datetimeFormatter: DateTimeFormatter = DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .append(ISO_LOCAL_DATE_TIME)
        .optionalStart()
        .appendOffset("+HHMMss", "Z").toFormatter()
    private val yearMonthFormatter: DateTimeFormatter = DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .appendValue(ChronoField.YEAR)
        .appendLiteral('-')
        .appendValue(ChronoField.MONTH_OF_YEAR).toFormatter().withResolverStyle(ResolverStyle.LENIENT)
    private val formatter: DateTimeFormatter = DateTimeFormatterBuilder()
        .appendOptional(datetimeFormatter)
        .appendOptional(ISO_LOCAL_DATE)
        .appendOptional(yearMonthFormatter)
        .toFormatter()

    /**
     * Formats an `Instant` to a form of `2018-03-22T00:35:44Z` at UTC.
     */
    fun format(d: Instant): String {
        return datetimeFormatter.format(d.atZone(UTC))
    }

    /**
     * Parses a `String` representing a date and/or time to a `Date` object with a default time zone offset
     * of UTC (+00:00). It can parse dates in any of the following formats.
     *
     *
     *  * 2018-03-22
     *  * 2018-03-22T00:35:44
     *  * 2018-03-22T00:35:44Z
     *  * 2018-03-22T00:35:44.741
     *  * 2018-03-22T00:35:44.741Z
     *  * 2018-03-22T00:35:44.741+1600
     * >
     *
     */
    fun parse(d: String?): Date {
        val t: TemporalAccessor = formatter.parse(d)
        return if (!t.isSupported(ChronoField.HOUR_OF_DAY)) {
            // no hours field so it must be a Date or a YearMonth
            if (!t.isSupported(ChronoField.DAY_OF_MONTH)) {
                // must be a YearMonth coz no day
                Date.from(YearMonth.from(t).atDay(1).atStartOfDay(UTC).toInstant())
            } else {
                // must be a Date as the day is present
                Date.from(Instant.ofEpochSecond(LocalDate.from(t).atStartOfDay().toEpochSecond(UTC)))
            }
        } else if (!t.isSupported(ChronoField.MONTH_OF_YEAR)) {
            // no month field so must be a Time
            val timeOnEpochDay: Instant = LocalDate.ofEpochDay(0)
                .atTime(LocalTime.from(t))
                .atZone(UTC)
                .toInstant()
            Date.from(timeOnEpochDay)
        } else if (t.isSupported(ChronoField.OFFSET_SECONDS)) {
            // has all datetime components including an offset
            Date.from(ZonedDateTime.from(t).toInstant())
        } else {
            // has all datetime components but no offset so throw in some UTC
            Date.from(ZonedDateTime.of(LocalDateTime.from(t), UTC).toInstant())
        }
    }

    /**
     * A proxy call to [.parse] but allows for syntax similar to Gremlin grammar of `datetime()`.
     */
    fun datetime(d: String?): Date {
        return parse(d)
    }
}