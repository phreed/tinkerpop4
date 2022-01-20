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
package org.apache.tinkerpop4.gremlin.process.computer.traversal.strategy.verification

import org.apache.tinkerpop4.gremlin.process.computer.traversal.strategy.decoration.VertexProgramStrategy

/**
 * Detects the presence of a [VertexProgramStrategy] and throws an [IllegalStateException] if it is found.
 *
 * @author Marc de Lignie
 */
class VertexProgramRestrictionStrategy private constructor() :
    AbstractTraversalStrategy<TraversalStrategy.VerificationStrategy?>(), TraversalStrategy.VerificationStrategy {
    @Override
    fun apply(traversal: Traversal.Admin<*, *>) {
        if (traversal.getStrategies().toList().contains(VertexProgramStrategy.instance())) {
            throw VerificationException("The TraversalSource does not allow the use of a GraphComputer", traversal)
        }
    }

    @Override
    fun applyPost(): Set<Class<out VerificationStrategy?>> {
        return Collections.singleton(ComputerVerificationStrategy::class.java)
    }

    companion object {
        private val INSTANCE = VertexProgramRestrictionStrategy()
        fun instance(): VertexProgramRestrictionStrategy {
            return INSTANCE
        }
    }
}