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

import org.apache.tinkerpop4.gremlin.process.traversal.Operator

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class GroupSideEffectStep<S, K, V>(
    traversal: Traversal.Admin?, ///
    @get:Override val sideEffectKey: String
) : SideEffectStep<S>(traversal), SideEffectCapable<Map<K, *>?, Map<K, V>?>, TraversalParent, ByModulating,
    ProfilingAware, Grouping<S, K, V> {
    private var state = 'k'
    private var keyTraversal: Traversal.Admin<S, K>? = null
    private var valueTraversal: Traversal.Admin<S, V>
    private var barrierStep: Barrier?
    private var resetBarrierForProfiling = false

    init {
        valueTraversal = this.integrateChild(__.fold().asAdmin())
        barrierStep = determineBarrierStep(valueTraversal)
        this.getTraversal().getSideEffects().registerIfAbsent(
            sideEffectKey, HashMapSupplier.instance(),
            GroupBiOperator(
                if (null == barrierStep) Operator.assign else barrierStep.getMemoryComputeKey().getReducer()
            )
        )
    }

    /**
     * Reset the [Barrier] on the step to be wrapped in a [ProfilingAware.ProfiledBarrier] which can
     * properly start/stop the timer on the associated [ProfileStep].
     */
    @Override
    fun prepareForProfiling() {
        resetBarrierForProfiling = barrierStep != null
    }

    @Override
    fun getKeyTraversal(): Traversal.Admin<S, K> {
        return keyTraversal
    }

    @Override
    fun getValueTraversal(): Traversal.Admin<S, V> {
        return valueTraversal
    }

    private fun setValueTraversal(valueTraversal: Traversal.Admin) {
        this.valueTraversal = this.integrateChild(convertValueTraversal(valueTraversal))
        barrierStep = determineBarrierStep(this.valueTraversal)
        this.getTraversal().getSideEffects().register(
            sideEffectKey, null,
            GroupBiOperator(
                if (null == barrierStep) Operator.assign else barrierStep.getMemoryComputeKey().getReducer()
            )
        )
    }

    @Override
    fun modulateBy(kvTraversal: Traversal.Admin<*, *>) {
        if ('k' == state) {
            keyTraversal = this.integrateChild(kvTraversal)
            state = 'v'
        } else if ('v' == state) {
            setValueTraversal(kvTraversal)
            state = 'x'
        } else {
            throw IllegalStateException("The key and value traversals for group()-step have already been set: $this")
        }
    }

    @Override
    fun replaceLocalChild(oldTraversal: Traversal.Admin<*, *>?, newTraversal: Traversal.Admin<*, *>?) {
        if (null != keyTraversal && keyTraversal.equals(oldTraversal)) keyTraversal =
            this.integrateChild(newTraversal) else if (null != valueTraversal && valueTraversal.equals(oldTraversal)) setValueTraversal(
            newTraversal
        )
    }

    @Override
    protected fun sideEffect(traverser: Traverser.Admin<S>?) {
        val map: Map<K, V> = HashMap(1)
        valueTraversal.reset()
        valueTraversal.addStart(traverser)

        // reset the barrierStep as there are now ProfileStep instances present and the timers won't start right
        // without specific configuration through wrapping both the Barrier and ProfileStep in ProfiledBarrier
        if (resetBarrierForProfiling) {
            barrierStep = determineBarrierStep(valueTraversal)

            // the barrier only needs to be reset once
            resetBarrierForProfiling = false
        }
        if (null == barrierStep) {
            if (valueTraversal.hasNext()) map.put(
                TraversalUtil.applyNullable(traverser, keyTraversal),
                valueTraversal.next() as V
            )
        } else if (barrierStep.hasNextBarrier()) map.put(
            TraversalUtil.applyNullable(traverser, keyTraversal),
            barrierStep.nextBarrier() as V
        )
        if (!map.isEmpty()) this.getTraversal().getSideEffects().add(sideEffectKey, map)
    }

    @Override
    override fun toString(): String {
        return StringFactory.stepString(this, sideEffectKey, keyTraversal, valueTraversal)
    }

    @get:Override
    val localChildren: List<Any>
        get() {
            val children: List<Traversal.Admin<*, *>> = ArrayList(2)
            if (null != keyTraversal) children.add(keyTraversal)
            children.add(valueTraversal)
            return children
        }

    @get:Override
    val requirements: Set<Any>
        get() = this.getSelfAndChildRequirements(
            TraverserRequirement.OBJECT,
            TraverserRequirement.BULK,
            TraverserRequirement.SIDE_EFFECTS
        )

    @Override
    fun clone(): GroupSideEffectStep<S, K, V> {
        val clone = super.clone() as GroupSideEffectStep<S, K, V>
        if (null != keyTraversal) clone.keyTraversal = keyTraversal.clone()
        clone.valueTraversal = valueTraversal.clone()
        clone.barrierStep = determineBarrierStep(clone.valueTraversal)
        return clone
    }

    @Override
    fun setTraversal(parentTraversal: Traversal.Admin<*, *>?) {
        super.setTraversal(parentTraversal)
        this.integrateChild(keyTraversal)
        this.integrateChild(valueTraversal)
    }

    @Override
    override fun hashCode(): Int {
        var result = super.hashCode() xor sideEffectKey.hashCode()
        if (keyTraversal != null) result = result xor keyTraversal.hashCode()
        result = result xor valueTraversal.hashCode()
        return result
    }

    @Override
    fun generateFinalResult(`object`: Map<K, *>?): Map<K, V> {
        return doFinalReduction(`object` as Map<K, Object?>?, valueTraversal)
    }
}