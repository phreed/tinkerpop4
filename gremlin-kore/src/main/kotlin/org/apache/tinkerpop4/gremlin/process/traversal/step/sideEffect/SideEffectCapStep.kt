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
package org.apache.tinkerpop4.gremlin.process.traversal.step.sideEffect

import org.apache.tinkerpop4.gremlin.process.traversal.Traversal

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class SideEffectCapStep<S, E>(traversal: Traversal.Admin?, sideEffectKey: String?, vararg sideEffectKeys: String?) :
    SupplyingBarrierStep<S, E>(traversal) {
    val sideEffectKeys: List<String>

    @kotlin.jvm.Transient
    var sideEffectCapableSteps: Map<String, SideEffectCapable<Object, E>>? = null

    init {
        this.sideEffectKeys = ArrayList(1 + sideEffectKeys.size)
        this.sideEffectKeys.add(sideEffectKey)
        Collections.addAll(this.sideEffectKeys, sideEffectKeys)
    }

    @Override
    override fun toString(): String {
        return StringFactory.stepString(this, sideEffectKeys)
    }

    @Override
    override fun hashCode(): Int {
        var result = super.hashCode()
        for (sideEffectKey in sideEffectKeys) {
            result = result xor sideEffectKey.hashCode()
        }
        return result
    }

    @get:Override
    val requirements: Set<Any>
        get() = Collections.singleton(TraverserRequirement.SIDE_EFFECTS)

    @Override
    protected fun supply(): E {
        if (null == sideEffectCapableSteps) {
            sideEffectCapableSteps = HashMap()
            var parentTraversal: Traversal.Admin<*, *> = this.getTraversal()
            while (parentTraversal !is EmptyTraversal) {
                for (capableStep in TraversalHelper.getStepsOfAssignableClassRecursively(
                    SideEffectCapable::class.java, parentTraversal
                )) {
                    if (sideEffectKeys.contains(capableStep.getSideEffectKey()) && !sideEffectCapableSteps!!.containsKey(
                            capableStep.getSideEffectKey()
                        )
                    ) sideEffectCapableSteps.put(capableStep.getSideEffectKey(), capableStep)
                }
                if (sideEffectKeys.size() === this.sideEffectCapableSteps.size()) break
                parentTraversal = parentTraversal.getParent().asStep().getTraversal()
            }
        }
        ////////////
        return if (sideEffectKeys.size() === 1) {
            val sideEffectKey = sideEffectKeys[0]
            val result: E = this.getTraversal().getSideEffects().< E > get < E ? > sideEffectKey
            val sideEffectCapable: SideEffectCapable<Object, E>? = sideEffectCapableSteps!![sideEffectKey]
            val finalResult = if (null == sideEffectCapable) result else sideEffectCapable.generateFinalResult(result)
            this.getTraversal().getSideEffects().set(sideEffectKey, finalResult)
            finalResult
        } else mapOfSideEffects as E
    }

    private val mapOfSideEffects: Map<String, Any>
        private get() {
            val temp: TraversalSideEffects = this.getTraversal().getSideEffects()
            val sideEffects: Map<String, Object> = HashMap()
            for (sideEffectKey in sideEffectKeys) {
                if (temp.exists(sideEffectKey)) {
                    val result: E = temp.get(sideEffectKey)
                    val sideEffectCapable: SideEffectCapable<Object, E>? = sideEffectCapableSteps!![sideEffectKey]
                    val finalResult =
                        if (null == sideEffectCapable) result else sideEffectCapable.generateFinalResult(result)
                    temp.set(sideEffectKey, finalResult)
                    sideEffects.put(sideEffectKey, finalResult)
                }
            }
            return sideEffects
        }

    @Override
    fun clone(): SideEffectCapStep<S, E> {
        val clone = super.clone() as SideEffectCapStep<S, E>
        clone.sideEffectCapableSteps = null
        return clone
    }
}