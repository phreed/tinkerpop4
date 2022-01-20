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
package org.apache.tinkerpop4.gremlin.process.traversal.step.map

import org.apache.tinkerpop4.gremlin.process.traversal.Operator

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class SumGlobalStep<S : Number?>(traversal: Traversal.Admin?) : ReducingBarrierStep<S, S>(traversal) {
    init {
        this.setReducingBiOperator(Operator.sum as BinaryOperator)
    }

    /**
     * Advances the starts until a non-null value is found or simply returns `null`. In this way, an all
     * `null` stream will result in `null`.
     */
    @Override
    protected fun generateSeedFromStarts(): S? {
        var s: S? = null
        while (starts.hasNext() && null == s) {
            s = projectTraverser(this.starts.next())
        }
        return s
    }

    @Override
    fun processAllStarts() {
        if (this.starts.hasNext()) super.processAllStarts()
    }

    @Override
    fun projectTraverser(traverser: Traverser.Admin<S>): S {
        return mul(traverser.get(), traverser.bulk()) as S
    }

    @get:Override
    val requirements: Set<Any>
        get() = REQUIREMENTS

    companion object {
        private val REQUIREMENTS: Set<TraverserRequirement> = EnumSet.of(
            TraverserRequirement.BULK,
            TraverserRequirement.OBJECT
        )
    }
}