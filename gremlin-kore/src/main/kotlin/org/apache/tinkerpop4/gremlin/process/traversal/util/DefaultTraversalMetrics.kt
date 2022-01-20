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

import org.apache.commons.lang3.StringUtils

/**
 * Default implementation for [TraversalMetrics] that aggregates [ImmutableMetrics] instances from a
 * [Traversal].
 *
 * @author Bob Briody (http://bobbriody.com)
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class DefaultTraversalMetrics : TraversalMetrics, Serializable {
    /**
     * [ImmutableMetrics] indexed by their step identifier.
     */
    private val stepIndexedMetrics: Map<String, ImmutableMetrics> = HashMap()

    /**
     * A computed value representing the total time spent on all steps.
     */
    private var totalStepDuration: Long = 0

    /**
     * [ImmutableMetrics] indexed by their step position.
     */
    private val positionIndexedMetrics: Map<Integer, ImmutableMetrics> = HashMap()
    /**
     * The metrics have been computed and can no longer be modified.
     */
    /**
     * Determines if final metrics have been computed
     */
    @Volatile
    var isFinalized = false
        private set

    constructor() {}

    /**
     * This is only a convenient constructor needed for GraphSON deserialization.
     */
    constructor(totalStepDurationNs: Long, orderedMetrics: List<MutableMetrics?>) {
        totalStepDuration = totalStepDurationNs
        var ix = 0
        for (metric in orderedMetrics) {
            stepIndexedMetrics.put(metric.getId(), metric.getImmutableClone())
            positionIndexedMetrics.put(ix++, metric.getImmutableClone())
        }
    }

    @Override
    fun getDuration(unit: TimeUnit): Long {
        return unit.convert(totalStepDuration, MutableMetrics.SOURCE_UNIT)
    }

    @Override
    fun getMetrics(index: Int): Metrics? {
        return positionIndexedMetrics[index]
    }

    @Override
    fun getMetrics(id: String): Metrics? {
        return stepIndexedMetrics[id]
    }

    @get:Override
    val metrics: Collection<Any>
        get() = positionIndexedMetrics.entrySet().stream().sorted(Map.Entry.comparingByKey())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                { oldValue, newValue -> oldValue }) { LinkedHashMap() }).values()

    @Override
    override fun toString(): String {
        // Build a pretty table of metrics data.

        // Append headers
        val sb: StringBuilder = StringBuilder("Traversal Metrics")
            .append(System.lineSeparator())
            .append(String.format("%-50s %21s %11s %15s %8s", HEADERS))
        sb.append(System.lineSeparator())
        sb.append("=============================================================================================================")
        appendMetrics(positionIndexedMetrics.values(), sb, 0)

        // Append total duration
        sb.append(
            String.format(
                "%n%50s %21s %11s %15.3f %8s",
                ">TOTAL", "-", "-", getDuration(TimeUnit.MICROSECONDS) / 1000.0, "-"
            )
        )
        return sb.toString()
    }

    /**
     * Extracts metrics from the provided [Traversal] and computes metrics. Calling this method finalizes the
     * metrics such that their values can no longer be modified.
     */
    @kotlin.jvm.Synchronized
    fun setMetrics(traversal: Traversal.Admin, onGraphComputer: Boolean) {
        // this is meant to be called on a traversal that is locked so that the metrics can get initialized
        // properly in all the ProfileStep instances
        if (!traversal.isLocked()) throw IllegalStateException("Metrics cannot be computed when the traversal is not locked")
        if (isFinalized) throw IllegalStateException("Metrics have been finalized and cannot be modified")
        isFinalized = true
        handleNestedTraversals(traversal, null, onGraphComputer)
        addTopLevelMetrics(traversal, onGraphComputer)
    }

    private fun addTopLevelMetrics(traversal: Traversal.Admin, onGraphComputer: Boolean) {
        totalStepDuration = 0
        val profileSteps: List<ProfileStep> = TraversalHelper.getStepsOfClass(ProfileStep::class.java, traversal)
        val tempMetrics: List<Pair<Integer, MutableMetrics>> = ArrayList(profileSteps.size())
        for (ii in 0 until profileSteps.size()) {
            // The index is necessary to ensure that step order is preserved after a merge.
            val step: ProfileStep<*> = profileSteps[ii]
            val stepMetrics: MutableMetrics =
                if (onGraphComputer) traversal.getSideEffects().get(step.getId()) else step.getMetrics().get()
            totalStepDuration += stepMetrics.getDuration(MutableMetrics.SOURCE_UNIT)
            tempMetrics.add(Pair.with(ii, stepMetrics.clone()))
        }
        tempMetrics.forEach { m ->
            val dur: Double = m.getValue1().getDuration(TimeUnit.NANOSECONDS) * 100.0 / totalStepDuration
            m.getValue1().setAnnotation(PERCENT_DURATION_KEY, dur)
        }
        tempMetrics.forEach { p ->
            stepIndexedMetrics.put(p.getValue1().getId(), p.getValue1().getImmutableClone())
            positionIndexedMetrics.put(p.getValue0(), p.getValue1().getImmutableClone())
        }
    }

    private fun handleNestedTraversals(
        traversal: Traversal.Admin,
        parentMetrics: MutableMetrics?,
        onGraphComputer: Boolean
    ) {
        var prevDur: Long = 0
        for (i in 0 until traversal.getSteps().size()) {
            val step: Step = traversal.getSteps().get(i) as Step as? ProfileStep ?: continue
            val metrics: MutableMetrics = if (onGraphComputer) traversal.getSideEffects()
                .get(step.getId()) else (step as ProfileStep<*>).getMetrics().get()
            if (null != metrics) { // this happens when a particular branch never received a .next() call (the metrics were never initialized)
                if (!onGraphComputer) {
                    // subtract upstream duration.
                    val durBeforeAdjustment: Long = metrics.getDuration(TimeUnit.NANOSECONDS)
                    // adjust duration
                    metrics.setDuration(metrics.getDuration(TimeUnit.NANOSECONDS) - prevDur, TimeUnit.NANOSECONDS)
                    prevDur = durBeforeAdjustment
                }
                if (parentMetrics != null) {
                    parentMetrics.addNested(metrics)
                }
                if (step.getPreviousStep() is TraversalParent) {
                    for (t in (step.getPreviousStep() as TraversalParent).getLocalChildren()) {
                        handleNestedTraversals(t, metrics, onGraphComputer)
                    }
                    for (t in (step.getPreviousStep() as TraversalParent).getGlobalChildren()) {
                        handleNestedTraversals(t, metrics, onGraphComputer)
                    }
                }
            }
        }
    }

    private fun appendMetrics(metrics: Collection<Metrics?>, sb: StringBuilder, indent: Int) {
        // Append each StepMetric's row. indexToLabelMap values are ordered by index.
        for (m in metrics) {
            val metricName = StringBuilder()

            // Handle indentation
            for (ii in 0 until indent) {
                metricName.append("  ")
            }
            metricName.append(m.getName())
            // Abbreviate if necessary
            val rowName = StringBuilder(StringUtils.abbreviate(metricName.toString(), 50))

            // Grab the values
            val itemCount: Long = m.getCount(ELEMENT_COUNT_ID)
            val traverserCount: Long = m.getCount(TRAVERSER_COUNT_ID)
            val percentDur = m.getAnnotation(PERCENT_DURATION_KEY) as Double

            // Build the row string
            sb.append(String.format("%n%-50s", rowName.toString()))
            if (itemCount != null) {
                sb.append(String.format(" %21d", itemCount))
            } else {
                sb.append(String.format(" %21s", ""))
            }
            if (traverserCount != null) {
                sb.append(String.format(" %11d", traverserCount))
            } else {
                sb.append(String.format(" %11s", ""))
            }
            sb.append(String.format(" %15.3f", m.getDuration(TimeUnit.MICROSECONDS) / 1000.0))
            if (percentDur != null) {
                sb.append(String.format(" %8.2f", percentDur))
            }

            // process any annotations
            val annotations: Map<String, Object> = m.getAnnotations()
            if (!annotations.isEmpty()) {
                // ignore the PERCENT_DURATION_KEY as that is a TinkerPop annotation that is displayed by default
                annotations.entrySet().stream().filter { kv -> !kv.getKey().equals(PERCENT_DURATION_KEY) }
                    .forEach { kv ->
                        val prefixBuilder = StringBuilder("  ")
                        for (i in 0 until indent) {
                            prefixBuilder.append("  ")
                        }
                        val prefix: String = prefixBuilder.append("\\_").toString()
                        val separator = "="
                        val k = prefix + StringUtils.abbreviate(kv.getKey(), 30)
                        val valueIndentLen: Int = separator.length() + k.length() + indent
                        val leftover = 110 - valueIndentLen
                        val splitValues = splitOnSize(kv.getValue().toString(), leftover)
                        for (ix in splitValues.indices) {
                            // the first lines gets the annotation prefix. the rest are indented to the separator
                            if (ix == 0) {
                                sb.append(String.format("%n%s", k + separator + splitValues[ix]))
                            } else {
                                sb.append(String.format("%n%s", padLeft(splitValues[ix], valueIndentLen - 1)))
                            }
                        }
                    }
            }
            appendMetrics(m.getNested(), sb, indent + 1)
        }
    }

    companion object {
        /**
         * toString() specific headers
         */
        private val HEADERS = arrayOf("Step", "Count", "Traversers", "Time (ms)", "% Dur")
        private fun splitOnSize(text: String, size: Int): Array<String?> {
            val ret = arrayOfNulls<String>((text.length() + size - 1) / size)
            var counter = 0
            var start = 0
            while (start < text.length()) {
                ret[counter] = text.substring(start, Math.min(text.length(), start + size))
                counter++
                start += size
            }
            return ret
        }

        private fun padLeft(text: String?, amountToPad: Int): String {
            // not sure why this method needed to exist. stupid string format stuff and commons utilities wouldn't
            // work for some reason in the context this method was used above.
            val newText = StringBuilder()
            for (ix in 0 until amountToPad) {
                newText.append(" ")
            }
            newText.append(text)
            return newText.toString()
        }
    }
}