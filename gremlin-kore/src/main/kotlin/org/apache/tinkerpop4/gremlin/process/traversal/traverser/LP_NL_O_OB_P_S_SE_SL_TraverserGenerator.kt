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
package org.apache.tinkerpop4.gremlin.process.traversal.traverser

import org.apache.tinkerpop4.gremlin.process.traversal.Step

class LP_NL_O_OB_P_S_SE_SL_TraverserGenerator private constructor() : TraverserGenerator {
    @Override
    fun <S> generate(start: S, startStep: Step<S, *>?, initialBulk: Long): Traverser.Admin<S> {
        return LP_NL_O_OB_P_S_SE_SL_Traverser(start, startStep)
    }

    @get:Override
    val providedRequirements: Set<Any>
        get() = REQUIREMENTS

    companion object {
        private val REQUIREMENTS: Set<TraverserRequirement> = EnumSet.of(
            TraverserRequirement.LABELED_PATH,
            TraverserRequirement.NESTED_LOOP,
            TraverserRequirement.OBJECT,
            TraverserRequirement.ONE_BULK,
            TraverserRequirement.PATH,
            TraverserRequirement.SACK,
            TraverserRequirement.SIDE_EFFECTS,
            TraverserRequirement.SINGLE_LOOP
        )
        private val INSTANCE = LP_NL_O_OB_P_S_SE_SL_TraverserGenerator()
        fun instance(): LP_NL_O_OB_P_S_SE_SL_TraverserGenerator {
            return INSTANCE
        }
    }
}