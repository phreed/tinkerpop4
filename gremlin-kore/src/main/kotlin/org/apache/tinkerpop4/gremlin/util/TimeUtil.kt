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

import org.javatuples.Pair
import java.util.concurrent.TimeUnit
import java.util.function.Supplier
import java.util.stream.IntStream

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 * @author Daniel Kuppitz (http://gremlin.guru)
 */
object TimeUtil {
    fun secondsSince(startNanos: Long): Long {
        return timeSince(startNanos, TimeUnit.SECONDS)
    }

    fun millisSince(startNanos: Long): Long {
        return timeSince(startNanos, TimeUnit.MILLISECONDS)
    }

    fun minutesSince(startNanos: Long): Long {
        return timeSince(startNanos, TimeUnit.MINUTES)
    }

    fun timeSince(startNanos: Long, destUnit: TimeUnit): Long {
        return destUnit.convert(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS)
    }

    fun clock(runnable: Runnable): Double {
        return clock(100, runnable)
    }

    fun clock(loops: Int, runnable: Runnable): Double {
        runnable.run() // warm-up
        return IntStream.range(0, loops).mapToDouble { i ->
            val t: Long = System.nanoTime()
            runnable.run()
            (System.nanoTime() - t) * 0.000001
        }.sum() / loops
    }

    fun <S> clockWithResult(supplier: Supplier<S>): Pair<Double, S> {
        return clockWithResult<Any>(100, supplier)
    }

    fun <S> clockWithResult(loops: Int, supplier: Supplier<S>): Pair<Double, S> {
        val result: S = supplier.get() // warm up
        return Pair.with(IntStream.range(0, loops).mapToDouble { i ->
            val t: Long = System.nanoTime()
            supplier.get()
            (System.nanoTime() - t) * 0.000001
        }.sum() / loops, result)
    }
}