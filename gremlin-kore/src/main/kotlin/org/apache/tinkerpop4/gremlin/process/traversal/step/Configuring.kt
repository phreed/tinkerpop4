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

import org.apache.tinkerpop4.gremlin.process.traversal.Step
import org.apache.tinkerpop4.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop4.gremlin.process.traversal.step.util.Parameters

/**
 * Identifies a [Step] as one that can accept configurations via the [GraphTraversal.with]
 * step modulator. The nature of the configuration allowed is specific to the implementation.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
interface Configuring : Parameterizing {
    /**
     * Accept a configuration to the [Step]. Note that this interface extends [Parameterizing] and so
     * there is an expectation that the [Step] implementation will have a [Parameters] instance that will
     * house any values passed to this method. Storing these configurations in [Parameters] is not a requirement
     * however, IF the configuration is an expected option for the step and can be stored on a member field that can
     * be accessed on the step by more direct means (i.e. like a getter method).
     */
    fun configure(vararg keyValues: Object?)
}