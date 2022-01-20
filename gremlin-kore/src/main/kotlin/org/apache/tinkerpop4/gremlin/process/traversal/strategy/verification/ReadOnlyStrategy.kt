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
package org.apache.tinkerpop4.gremlin.process.traversal.strategy.verification

import org.apache.tinkerpop4.gremlin.process.traversal.Step

/**
 * Detects steps marked with [Mutating] and throws an [IllegalStateException] if one is found.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @example <pre>
 * __.out().addE()                 // throws an VerificationException
 * __.addV()                       // throws an VerificationException
 * __.property(key,value)          // throws an VerificationException
 * __.out().drop()                 // throws an VerificationException
</pre> *
 */
class ReadOnlyStrategy private constructor() : AbstractTraversalStrategy<TraversalStrategy.VerificationStrategy?>(),
    TraversalStrategy.VerificationStrategy {
    @Override
    fun apply(traversal: Traversal.Admin<*, *>) {
        for (step in traversal.getSteps()) {
            if (step is Mutating) throw VerificationException(
                "The provided traversal has a mutating step and thus is not read only: $step",
                traversal
            )
        }
    }

    @Override
    fun applyPost(): Set<Class<out VerificationStrategy?>> {
        return Collections.singleton(ComputerVerificationStrategy::class.java)
    }

    companion object {
        private val INSTANCE = ReadOnlyStrategy()
        fun instance(): ReadOnlyStrategy {
            return INSTANCE
        }
    }
}