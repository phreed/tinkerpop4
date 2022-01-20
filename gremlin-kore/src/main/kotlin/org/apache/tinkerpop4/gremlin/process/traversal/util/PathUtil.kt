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

import org.apache.tinkerpop4.gremlin.process.traversal.Step

/**
 * @author Ted Wilmes (http://twilmes.org)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
object PathUtil {
    fun getReferencedLabelsAfterStep(step: Step<*, *>): Set<String> {
        var step: Step<*, *> = step
        val labels: Set<String> = HashSet()
        while (step !is EmptyStep) {
            labels.addAll(getReferencedLabels(step))
            step = step.getNextStep()
        }
        return labels
    }

    fun getReferencedLabels(step: Step): Set<String> {
        val referencedLabels: Set<String> = HashSet()
        if (step is Scoping) {
            val labels: Set<String> = HashSet((step as Scoping).getScopeKeys())
            if (step is MatchStep) {
                // if this is the last step, keep everything, else just add founds
                if (step.getNextStep() is EmptyStep) {
                    labels.addAll((step as MatchStep).getMatchEndLabels())
                    labels.addAll((step as MatchStep).getMatchStartLabels())
                }
            }
            referencedLabels.addAll(labels)
        }
        return referencedLabels
    }
}