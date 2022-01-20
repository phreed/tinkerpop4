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
package org.apache.tinkerpop4.gremlin.process.traversal.strategy.finalization

import org.apache.tinkerpop4.gremlin.process.traversal.Traversal

/**
 * A strategy that detaches traversers with graph elements as references (i.e. without properties - just `id`
 * and `label`.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class ReferenceElementStrategy private constructor() : AbstractTraversalStrategy<FinalizationStrategy?>(),
    FinalizationStrategy {
    @Override
    fun apply(traversal: Traversal.Admin<*, *>) {
        if (traversal.getParent() === EmptyStep.instance()) {
            val profileStep: Optional<ProfileSideEffectStep> = TraversalHelper.getFirstStepOfAssignableClass(
                ProfileSideEffectStep::class.java, traversal
            )
            val index: Int = profileStep.map { step -> traversal.getSteps().indexOf(step) }
                .orElseGet { traversal.getSteps().size() }
            traversal.addStep(index, ReferenceElementStep<S, E>(traversal))
        }
    }

    class ReferenceElementStep<S, E>(traversal: Traversal.Admin?) : ScalarMapStep<S, E>(traversal) {
        @Override
        protected fun map(traverser: Traverser.Admin<S>): E {
            return ReferenceFactory.detach(traverser.get())
        }
    }

    companion object {
        private val INSTANCE = ReferenceElementStrategy()
        fun instance(): ReferenceElementStrategy {
            return INSTANCE
        }
    }
}