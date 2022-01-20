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
package org.apache.tinkerpop4.gremlin.process.traversal.util

import org.apache.tinkerpop4.gremlin.process.traversal.step.Profiling

/**
 * A [Metrics] implementation that can be modified.
 *
 * @author Bob Briody (http://bobbriody.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class MutableMetrics : ImmutableMetrics, Cloneable {
    // Note: if you add new members then you probably need to add them to the copy constructor;
    private var tempTime = -1L
    /**
     * Once these metrics are used in computing the final metrics to report through [TraversalMetrics] they
     * should no longer be modified and are thus finalized.
     */
    /**
     * Determines if metrics have been finalized, meaning that no more may be collected.
     */
    @Volatile
    var isFinalized = false
        private set

    protected constructor() {
        // necessary for gryo serialization
    }

    constructor(id: String, name: String) {
        id = id
        name = name
    }

    /**
     * Create a `MutableMetrics` from an immutable one.
     */
    constructor(other: Metrics) {
        this.id = other.getId()
        this.name = other.getName()
        this.annotations.putAll(other.getAnnotations())
        this.durationNs = other.getDuration(TimeUnit.NANOSECONDS)
        other.getCounts().forEach { key, count -> this.counts.put(key, AtomicLong(count)) }
        other.getNested().forEach { nested -> addNested(MutableMetrics(nested)) }
    }

    @kotlin.jvm.Synchronized
    fun addNested(metrics: MutableMetrics) {
        if (isFinalized) throw IllegalStateException("Metrics have been finalized and cannot be modified")
        this.nested.put(metrics.getId(), metrics)
    }

    /**
     * Starts the timer for this metric. Should not be called again without first calling [.stop].
     */
    fun start() {
        if (isFinalized) throw IllegalStateException("Metrics have been finalized and cannot be modified")
        if (-1L != tempTime) throw IllegalStateException("Internal Error: Concurrent Metrics start. Stop timer before starting timer.")
        tempTime = System.nanoTime()
    }

    /**
     * Stops the timer for this metric and increments the overall duration. Should not be called without first calling
     * [.start].
     */
    fun stop() {
        if (isFinalized) throw IllegalStateException("Metrics have been finalized and cannot be modified")
        if (-1L == tempTime) throw IllegalStateException("Internal Error: Metrics has not been started. Start timer before stopping timer")
        this.durationNs = this.durationNs + (System.nanoTime() - tempTime)
        tempTime = -1
    }

    /**
     * Increments a count metric.
     */
    @kotlin.jvm.Synchronized
    fun incrementCount(key: String?, incr: Long) {
        if (isFinalized) throw IllegalStateException("Metrics have been finalized and cannot be modified")
        var count: AtomicLong? = this.counts.get(key)
        if (count == null) {
            count = AtomicLong()
            this.counts.put(key, count)
        }
        count.addAndGet(incr)
    }

    /**
     * Directly set the duration for the metric.
     */
    fun setDuration(dur: Long, unit: TimeUnit?) {
        if (isFinalized) throw IllegalStateException("Metrics have been finalized and cannot be modified")
        this.durationNs = TimeUnit.NANOSECONDS.convert(dur, unit)
    }

    /**
     * Directly set the count for the metric.
     */
    fun setCount(key: String?, `val`: Long) {
        if (isFinalized) throw IllegalStateException("Metrics have been finalized and cannot be modified")
        this.counts.put(key, AtomicLong(`val`))
    }

    /**
     * Aggregate one set of metrics into the current body of metrics.
     */
    @kotlin.jvm.Synchronized
    fun aggregate(other: MutableMetrics) {
        if (isFinalized) throw IllegalStateException("Metrics have been finalized and cannot be modified")
        this.durationNs += other.durationNs
        for (otherCount in other.counts.entrySet()) {
            var thisCount: AtomicLong? = this.counts.get(otherCount.getKey())
            if (thisCount == null) {
                thisCount = AtomicLong(otherCount.getValue().get())
                this.counts.put(otherCount.getKey(), thisCount)
            } else {
                thisCount.addAndGet(otherCount.getValue().get())
            }
        }

        // Merge annotations. If multiple values for a given key are found then append it to a comma-separated list.
        for (p in other.annotations.entrySet()) {
            if (this.annotations.containsKey(p.getKey())) {
                // Strings are concatenated
                val existingVal: Object = this.annotations.get(p.getKey())
                if (existingVal is String) {
                    val existingValues: List<String> = Arrays.asList(existingVal.toString().split(","))
                    if (!existingValues.contains(p.getValue())) {
                        // New value. Append to comma-separated list.
                        this.annotations.put(p.getKey(), existingVal.toString() + ',' + p.getValue())
                    }
                } else {
                    // Numbers are summed
                    val existingNum = existingVal as Number
                    val otherNum = p.getValue() as Number
                    var newVal: Number
                    newVal = if (existingNum is Double || existingNum is Float) {
                        existingNum.doubleValue() + otherNum.doubleValue()
                    } else {
                        existingNum.longValue() + otherNum.longValue()
                    }
                    this.annotations.put(p.getKey(), newVal)
                }
            } else {
                this.annotations.put(p.getKey(), p.getValue())
            }
        }
        this.annotations.putAll(other.annotations)

        // Merge nested Metrics
        other.nested.values().forEach { nested ->
            var thisNested = nested.get(nested.getId()) as MutableMetrics
            if (thisNested == null) {
                thisNested = MutableMetrics(nested.getId(), nested.getName())
                nested.put(thisNested.getId(), thisNested)
            }
            thisNested.aggregate(nested as MutableMetrics)
        }
    }

    /**
     * Set an annotation value. Support exists for Strings and Numbers only. During a merge, Strings are concatenated
     * into a "," (comma) separated list of distinct values (duplicates are ignored), and Numbers are summed.
     */
    fun setAnnotation(key: String?, value: Object?) {
        if (isFinalized) throw IllegalStateException("Metrics have been finalized and cannot be modified")
        if (value !is String && value !is Number) {
            throw IllegalArgumentException("Metrics annotations only support String and Number values.")
        }
        annotations.put(key, value)
    }

    @Override
    fun getNested(metricsId: String?): MutableMetrics {
        return nested.get(metricsId)
    }

    /**
     * Gets a copy of the metrics that is immutable. Once this clone is made, the [MutableMetrics] can no
     * longer be modified themselves. This prevents custom steps that implement [Profiling] from adding to
     * the metrics after the traversal is complete.
     */
    @get:kotlin.jvm.Synchronized
    val immutableClone: ImmutableMetrics
        get() {
            isFinalized = true
            val clone = ImmutableMetrics()
            copyMembers(clone)
            this.nested.values()
                .forEach { nested -> clone.nested.put(nested.id, (nested as MutableMetrics).immutableClone) }
            return clone
        }

    protected fun copyMembers(clone: ImmutableMetrics) {
        clone.id = this.id
        clone.name = this.name
        // Note: This value is overwritten in the DependantMutableMetrics overridden copyMembers method.
        clone.durationNs = this.durationNs
        for (c in this.counts.entrySet()) {
            clone.counts.put(c.getKey(), AtomicLong(c.getValue().get()))
        }
        for (a in this.annotations.entrySet()) {
            clone.annotations.put(a.getKey(), a.getValue())
        }
    }

    @Override
    fun clone(): MutableMetrics {
        val clone = MutableMetrics()
        copyMembers(clone)
        this.nested.values().forEach { nested -> clone.nested.put(nested.id, (nested as MutableMetrics).clone()) }
        return clone
    }

    fun finish(bulk: Long) {
        stop()
        incrementCount(TraversalMetrics.TRAVERSER_COUNT_ID, 1)
        incrementCount(TraversalMetrics.ELEMENT_COUNT_ID, bulk)
    }
}