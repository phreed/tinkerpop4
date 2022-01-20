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
class ProjectStep<S, E>(traversal: Traversal.Admin?, vararg projectKeys: String?) :
    ScalarMapStep<S, Map<String?, E>?>(traversal), TraversalParent, ByModulating {
    val projectKeys: List<String>
    private var traversalRing: TraversalRing<S, E>

    init {
        this.projectKeys = Arrays.asList(projectKeys)
        traversalRing = TraversalRing()
    }

    @Override
    protected fun map(traverser: Traverser.Admin<S>?): Map<String, E> {
        val end: Map<String, E> = LinkedHashMap(projectKeys.size(), 1.0f)
        for (projectKey in projectKeys) {
            TraversalUtil.produce(traverser, traversalRing.next()).ifProductive { p -> end.put(projectKey, p as E) }
        }
        traversalRing.reset()
        return end
    }

    @Override
    fun reset() {
        super.reset()
        traversalRing.reset()
    }

    @Override
    override fun toString(): String {
        return StringFactory.stepString(this, projectKeys, traversalRing)
    }

    @Override
    fun clone(): ProjectStep<S, E> {
        val clone = super.clone() as ProjectStep<S, E>
        clone.traversalRing = traversalRing.clone()
        return clone
    }

    @Override
    fun setTraversal(parentTraversal: Traversal.Admin<*, *>?) {
        super.setTraversal(parentTraversal)
        traversalRing.getTraversals().forEach(this::integrateChild)
    }

    @Override
    override fun hashCode(): Int {
        return super.hashCode() xor traversalRing.hashCode() xor projectKeys.hashCode()
    }

    @get:Override
    val localChildren: List<Any>
        get() = traversalRing.getTraversals()

    @Override
    fun modulateBy(selectTraversal: Traversal.Admin<*, *>?) {
        traversalRing.addTraversal(this.integrateChild(selectTraversal))
    }

    @Override
    fun replaceLocalChild(oldTraversal: Traversal.Admin<*, *>?, newTraversal: Traversal.Admin<*, *>?) {
        traversalRing.replaceTraversal(
            oldTraversal as Traversal.Admin<S, E>?,
            newTraversal as Traversal.Admin<S, E>?
        )
    }

    @get:Override
    val requirements: Set<Any>
        get() = this.getSelfAndChildRequirements()
}