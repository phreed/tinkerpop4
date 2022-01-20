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
package org.apache.tinkerpop4.gremlin.process.traversal.step

import org.apache.tinkerpop4.gremlin.process.traversal.Step

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
interface TraversalParent : AutoCloseable {
    fun <S, E> getGlobalChildren(): List<Traversal.Admin<S, E>?>? {
        return Collections.emptyList()
    }

    fun <S, E> getLocalChildren(): List<Traversal.Admin<S, E>?>? {
        return Collections.emptyList()
    }

    fun addLocalChild(localChildTraversal: Traversal.Admin<*, *>?) {
        throw IllegalStateException(
            "This traversal parent does not support the addition of local traversals: " + this.getClass()
                .getCanonicalName()
        )
    }

    fun addGlobalChild(globalChildTraversal: Traversal.Admin<*, *>?) {
        throw IllegalStateException(
            "This traversal parent does not support the addition of global traversals: " + this.getClass()
                .getCanonicalName()
        )
    }

    fun removeLocalChild(localChildTraversal: Traversal.Admin<*, *>?) {
        throw IllegalStateException(
            "This traversal parent does not support the removal of local traversals: " + this.getClass()
                .getCanonicalName()
        )
    }

    fun removeGlobalChild(globalChildTraversal: Traversal.Admin<*, *>?) {
        throw IllegalStateException(
            "This traversal parent does not support the removal of global traversals: " + this.getClass()
                .getCanonicalName()
        )
    }

    fun replaceLocalChild(oldTraversal: Traversal.Admin<*, *>?, newTraversal: Traversal.Admin<*, *>?) {
        throw IllegalStateException(
            "This traversal parent does not support the replacement of local traversals: " + this.getClass()
                .getCanonicalName()
        )
    }

    fun getSelfAndChildRequirements(vararg selfRequirements: TraverserRequirement?): Set<TraverserRequirement?>? {
        val requirements: Set<TraverserRequirement> = EnumSet.noneOf(TraverserRequirement::class.java)
        Collections.addAll(requirements, selfRequirements)
        for (local in getLocalChildren<Any, Any>()) {
            requirements.addAll(local.getTraverserRequirements())
        }
        for (global in getGlobalChildren<Any, Any>()) {
            requirements.addAll(global.getTraverserRequirements())
        }
        return requirements
    }

    fun asStep(): Step<*, *>? {
        return this as Step<*, *>
    }

    fun <S, E> integrateChild(childTraversal: Traversal.Admin<*, *>?): Traversal.Admin<S, E>? {
        if (null == childTraversal) return null
        childTraversal.setParent(this)
        childTraversal.getSideEffects().mergeInto(asStep().getTraversal().getSideEffects())
        childTraversal.setSideEffects(asStep().getTraversal().getSideEffects())
        return childTraversal as Traversal.Admin<S, E>?
    }

    @Override
    @Throws(Exception::class)
    fun close() {
        for (traversal in getLocalChildren<Any, Any>()) {
            traversal.close()
        }
        for (traversal in getGlobalChildren<Any, Any>()) {
            traversal.close()
        }
    }
}