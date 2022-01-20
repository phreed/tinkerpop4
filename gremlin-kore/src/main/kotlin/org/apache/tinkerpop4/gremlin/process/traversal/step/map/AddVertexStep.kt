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
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class AddVertexStep<S> : ScalarMapStep<S, Vertex?>, Mutating<VertexAddedEvent?>, TraversalParent, Scoping {
    private var parameters: Parameters = Parameters()
    private var callbackRegistry: CallbackRegistry<VertexAddedEvent>? = null

    constructor(traversal: Traversal.Admin?, label: String?) : super(traversal) {
        parameters.set(this, T.label, label ?: Vertex.DEFAULT_LABEL)
    }

    constructor(traversal: Traversal.Admin?, vertexLabelTraversal: Traversal.Admin<S, String?>?) : super(traversal) {
        parameters.set(this, T.label, if (null == vertexLabelTraversal) Vertex.DEFAULT_LABEL else vertexLabelTraversal)
    }

    @Override
    fun getParameters(): Parameters {
        return parameters
    }

    @get:Override
    val scopeKeys: Set<String>
        get() = parameters.getReferencedLabels()

    @Override
    fun <S, E> getLocalChildren(): List<Traversal.Admin<S, E>> {
        return parameters.getTraversals()
    }

    @Override
    fun configure(vararg keyValues: Object) {
        if (keyValues[0] === T.label && parameters.contains(T.label)) {
            if (parameters.contains(T.label, Vertex.DEFAULT_LABEL)) {
                parameters.remove(T.label)
                parameters.set(this, keyValues)
            } else {
                throw IllegalArgumentException(
                    String.format(
                        "Vertex T.label has already been set to [%s] and cannot be overridden with [%s]",
                        parameters.getRaw().get(T.label).get(0), keyValues[1]
                    )
                )
            }
        } else if (keyValues[0] === T.id && parameters.contains(T.id)) {
            throw IllegalArgumentException(
                String.format(
                    "Vertex T.id has already been set to [%s] and cannot be overridden with [%s]",
                    parameters.getRaw().get(T.id).get(0), keyValues[1]
                )
            )
        } else {
            parameters.set(this, keyValues)
        }
    }

    @Override
    protected fun map(traverser: Traverser.Admin<S>?): Vertex {
        val vertex: Vertex = this.getTraversal().getGraph().get().addVertex(parameters.getKeyValues(traverser))
        if (callbackRegistry != null && !callbackRegistry.getCallbacks().isEmpty()) {
            val eventStrategy: EventStrategy =
                getTraversal().getStrategies().getStrategy(EventStrategy::class.java).get()
            val vae = VertexAddedEvent(eventStrategy.detach(vertex))
            callbackRegistry.getCallbacks().forEach { c -> c.accept(vae) }
        }
        return vertex
    }

    @get:Override
    val mutatingCallbackRegistry: CallbackRegistry<VertexAddedEvent>?
        get() {
            if (null == callbackRegistry) callbackRegistry = ListCallbackRegistry()
            return callbackRegistry
        }

    @Override
    override fun hashCode(): Int {
        return super.hashCode() xor parameters.hashCode()
    }

    @get:Override
    val requirements: Set<Any>
        get() = this.getSelfAndChildRequirements()

    @Override
    override fun toString(): String {
        return StringFactory.stepString(this, parameters)
    }

    @Override
    fun setTraversal(parentTraversal: Traversal.Admin<*, *>?) {
        super.setTraversal(parentTraversal)
        parameters.getTraversals().forEach(this::integrateChild)
    }

    @Override
    fun clone(): AddVertexStep<S> {
        val clone = super.clone() as AddVertexStep<S>
        clone.parameters = parameters.clone()
        return clone
    }
}