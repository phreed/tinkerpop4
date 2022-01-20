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
class GroupStep<S, K, V>(traversal: Traversal.Admin?) : ReducingBarrierStep<S, Map<K, V>?>(traversal), ByModulating,
    TraversalParent, ProfilingAware, Grouping<S, K, V> {
    private var state = 'k'
    private var keyTraversal: Traversal.Admin<S, K>? = null
    private var valueTraversal: Traversal.Admin<S, V>
    private var barrierStep: Barrier?
    private var resetBarrierForProfiling = false

    init {
        valueTraversal = this.integrateChild(__.fold().asAdmin())
        barrierStep = determineBarrierStep(valueTraversal)
        this.setReducingBiOperator(
            GroupBiOperator<K, V>(
                if (null == barrierStep) Operator.assign else barrierStep.getMemoryComputeKey().getReducer()
            )
        )
        this.setSeedSupplier(HashMapSupplier.instance())
    }

    /**
     * Reset the [Barrier] on the step to be wrapped in a [ProfiledBarrier] which can properly start/stop
     * the timer on the associated [ProfileStep].
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

    private fun setValueTraversal(kvTraversal: Traversal.Admin) {
        valueTraversal = this.integrateChild(convertValueTraversal(kvTraversal))
        barrierStep = determineBarrierStep(valueTraversal)
        this.setReducingBiOperator(
            GroupBiOperator<K, V>(
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
    fun projectTraverser(traverser: Traverser.Admin<S>?): Map<K, V> {
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
        TraversalUtil.produce(traverser, keyTraversal).ifProductive { p ->
            if (null == barrierStep) {
                if (valueTraversal.hasNext()) {
                    map.put(p as K, valueTraversal.next() as V)
                }
            } else if (barrierStep.hasNextBarrier()) map.put(p as K, barrierStep.nextBarrier() as V)
        }
        return map
    }

    @Override
    override fun toString(): String {
        return StringFactory.stepString(this, keyTraversal, valueTraversal)
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
        get() = this.getSelfAndChildRequirements(TraverserRequirement.OBJECT, TraverserRequirement.BULK)

    @Override
    fun clone(): GroupStep<S, K, V> {
        val clone = super.clone() as GroupStep<S, K, V>
        if (null != keyTraversal) clone.keyTraversal = keyTraversal.clone()
        clone.valueTraversal = valueTraversal.clone()
        clone.barrierStep = determineBarrierStep(clone.valueTraversal)
        return clone
    }

    @Override
    fun setTraversal(parentTraversal: Traversal.Admin<*, *>?) {
        super.setTraversal(parentTraversal)
        integrateChild(keyTraversal)
        integrateChild(valueTraversal)
    }

    @Override
    override fun hashCode(): Int {
        var result = super.hashCode()
        if (keyTraversal != null) result = result xor keyTraversal.hashCode()
        result = result xor valueTraversal.hashCode()
        return result
    }

    @Override
    fun generateFinalResult(`object`: Map<K, V>?): Map<K, V> {
        return doFinalReduction(`object` as Map<K, Object?>?, valueTraversal)
    }

    ///////////////////////
    class GroupBiOperator<K, V> : BinaryOperator<Map<K, V>?>, Serializable {
        private var barrierAggregator: BinaryOperator<V>? = null

        constructor() {
            // no-arg constructor for serialization
        }

        constructor(barrierAggregator: BinaryOperator<V>) {
            this.barrierAggregator = barrierAggregator
        }

        @Override
        fun apply(mapA: Map<K, V>, mapB: Map<K, V>): Map<K, V> {
            for (key in mapB.keySet()) {
                var objectA = mapA[key]
                val objectB = mapB[key]
                if (null == objectA) objectA = objectB else if (null != objectB) objectA =
                    barrierAggregator.apply(objectA, objectB)
                mapA.put(key, objectA)
            }
            return mapA
        }
    }
}