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
package org.apache.tinkerpop4.gremlin.process.traversal.step.filter

import org.apache.tinkerpop4.gremlin.process.traversal.Traversal

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
abstract class ConnectiveStep<S>(traversal: Traversal.Admin?, vararg traversals: Traversal<S, *>?) :
    FilterStep<S>(traversal), TraversalParent {
    enum class Connective {
        AND, OR
    }

    protected var traversals: List<Traversal.Admin<S, *>>

    init {
        this.traversals = Stream.of(traversals).map(Traversal::asAdmin).collect(Collectors.toList())
        this.traversals.forEach(this::integrateChild)
    }

    @get:Override
    val localChildren: List<Any>
        get() = traversals

    @get:Override
    val requirements: Set<Any>
        get() = this.getSelfAndChildRequirements()

    fun addLocalChild(localChildTraversal: Traversal.Admin<*, *>?) {
        traversals.add(this.integrateChild(localChildTraversal as Traversal.Admin?))
    }

    @Override
    fun clone(): ConnectiveStep<S> {
        val clone = super.clone() as ConnectiveStep<S>
        clone.traversals = ArrayList()
        for (traversal in traversals) {
            clone.traversals.add(traversal.clone())
        }
        return clone
    }

    @Override
    fun setTraversal(parentTraversal: Traversal.Admin<*, *>?) {
        super.setTraversal(parentTraversal)
        for (traversal in traversals) {
            integrateChild(traversal)
        }
    }

    @Override
    override fun toString(): String {
        return StringFactory.stepString(this, traversals)
    }

    @Override
    override fun hashCode(): Int {
        return super.hashCode() xor traversals.hashCode()
    }
}