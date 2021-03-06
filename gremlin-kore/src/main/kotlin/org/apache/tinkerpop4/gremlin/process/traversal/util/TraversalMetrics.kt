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

import java.util.Collection
import java.util.concurrent.TimeUnit

/**
 * Contains the Metrics gathered for a Traversal as the result of the .profile()-step.
 *
 * @author Bob Briody (http://bobbriody.com)
 */
interface TraversalMetrics {
    /**
     * Get the total duration taken by the Traversal.
     *
     * @return total duration taken by the Traversal.
     */
    fun getDuration(unit: TimeUnit?): Long

    /**
     * Get an individual Metrics object by the index of the profiled Step.
     *
     * @return an individual Metrics object.
     */
    fun getMetrics(stepIndex: Int): Metrics?

    /**
     * Get an individual Metrics object by the id of the profiled Step.
     *
     * @return an individual Metrics object.
     */
    fun getMetrics(id: String?): Metrics?

    /**
     * Gets all the metrics.
     */
    val metrics: Collection<Any?>?

    companion object {
        /**
         * The MetricsId used to obtain the element count via Metrics.getCount(String countKey)
         */
        const val ELEMENT_COUNT_ID = "elementCount"

        /**
         * The MetricsId used to obtain the traverser count via Metrics.getCount(String countKey)
         */
        const val TRAVERSER_COUNT_ID = "traverserCount"

        /**
         * The annotation key used to obtain the percent duration via Metrics.getAnnotation(String key)
         */
        const val PERCENT_DURATION_KEY = "percentDur"
    }
}