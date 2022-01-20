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

import org.apache.tinkerpop4.gremlin.process.traversal.Traversal

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class GroupCountStep<S, E>(traversal: Traversal.Admin?) : ReducingBarrierStep<S, Map<E, Long?>?>(traversal),
    TraversalParent, ByModulating {
    private var keyTraversal: Traversal.Admin<S, E>? = null

    init {
        this.setSeedSupplier(HashMapSupplier.instance())
        this.setReducingBiOperator(GroupCountBiOperator.instance())
    }

    @Override
    fun projectTraverser(traverser: Traverser.Admin<S>): Map<E, Long> {
        val map: Map<E, Long> = HashMap(1)
        TraversalUtil.produce(traverser, keyTraversal).ifProductive { p -> map.put(p as E, traverser.bulk()) }
        return map
    }

    @Override
    fun addLocalChild(groupTraversal: Traversal.Admin<*, *>?) {
        keyTraversal = this.integrateChild(groupTraversal)
    }

    @get:Override
    val localChildren: List<Any>
        get() = if (null == keyTraversal) Collections.emptyList() else Collections.singletonList(keyTraversal)

    @get:Override
    val requirements: Set<Any>
        get() = this.getSelfAndChildRequirements(TraverserRequirement.BULK)

    @Override
    @Throws(UnsupportedOperationException::class)
    fun modulateBy(keyTraversal: Traversal.Admin<*, *>?) {
        this.keyTraversal = this.integrateChild(keyTraversal)
    }

    @Override
    fun replaceLocalChild(oldTraversal: Traversal.Admin<*, *>?, newTraversal: Traversal.Admin<*, *>?) {
        if (null != keyTraversal && keyTraversal.equals(oldTraversal)) keyTraversal = this.integrateChild(newTraversal)
    }

    @Override
    fun clone(): GroupCountStep<S, E> {
        val clone = super.clone() as GroupCountStep<S, E>
        if (null != keyTraversal) clone.keyTraversal = keyTraversal.clone()
        return clone
    }

    @Override
    fun setTraversal(parentTraversal: Traversal.Admin<*, *>?) {
        super.setTraversal(parentTraversal)
        integrateChild(keyTraversal)
    }

    @Override
    override fun hashCode(): Int {
        var result = super.hashCode()
        if (keyTraversal != null) result = result xor keyTraversal.hashCode()
        return result
    }

    @Override
    override fun toString(): String {
        return StringFactory.stepString(this, keyTraversal)
    }

    ///////////////////////////
    class GroupCountBiOperator<E> : BinaryOperator<Map<E, Long?>?>, Serializable {
        @Override
        fun apply(mutatingSeed: Map<E, Long>, map: Map<E, Long?>): Map<E, Long> {
            for (entry in map.entrySet()) {
                MapHelper.incr(mutatingSeed, entry.getKey(), entry.getValue())
            }
            return mutatingSeed
        }

        companion object {
            private val INSTANCE: GroupCountBiOperator<*> = GroupCountBiOperator<Any?>()
            fun <E> instance(): GroupCountBiOperator<E> {
                return INSTANCE
            }
        }
    }
}