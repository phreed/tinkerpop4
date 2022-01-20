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
package org.apache.tinkerpop4.gremlin.process.traversal.step

import org.apache.tinkerpop4.gremlin.process.computer.GraphComputer

/**
 * A `GraphComputing` step is one that will change its behavior whether its on a [GraphComputer] or not.
 * [ComputerFinalizationStrategy] is responsible for calling the [GraphComputing.onGraphComputer] method.
 * This method is only called for global children steps of a [TraversalParent].
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
interface GraphComputing {
    /**
     * The step will be executing on a [GraphComputer].
     */
    fun onGraphComputer()

    /**
     * Some steps should behave different whether they are executing at the master traversal or distributed across the worker traversals.
     * The default implementation does nothing.
     *
     * @param atMaster whether the step is currently executing at master
     */
    fun atMaster(atMaster: Boolean) {}

    companion object {
        fun atMaster(step: Step<*, *>, atMaster: Boolean) {
            if (step is GraphComputing) (step as GraphComputing).atMaster(atMaster)
            if (step is TraversalParent) {
                for (local in (step as TraversalParent).getLocalChildren()) {
                    for (s in local.getSteps()) {
                        atMaster(s, atMaster)
                    }
                }
                for (global in (step as TraversalParent).getGlobalChildren()) {
                    for (s in global.getSteps()) {
                        atMaster(s, atMaster)
                    }
                }
            }
        }
    }
}