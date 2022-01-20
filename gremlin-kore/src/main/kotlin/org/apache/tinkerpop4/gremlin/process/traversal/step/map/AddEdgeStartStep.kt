/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.tinkerpop4.gremlin.process.traversal.step.map

import org.apache.tinkerpop4.gremlin.process.traversal.Step

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class AddEdgeStartStep : AbstractStep<Edge?, Edge?>, Mutating<EdgeAddedEvent?>, TraversalParent, Scoping,
    FromToModulating {
    private var first = true
    private var parameters: Parameters = Parameters()
    private var callbackRegistry: CallbackRegistry<EdgeAddedEvent>? = null

    constructor(traversal: Traversal.Admin?, edgeLabel: String?) : super(traversal) {
        parameters.set(this, T.label, edgeLabel)
    }

    constructor(traversal: Traversal.Admin?, edgeLabelTraversal: Traversal<*, String?>?) : super(traversal) {
        parameters.set(this, T.label, edgeLabelTraversal)
    }

    @Override
    fun <S, E> getLocalChildren(): List<Traversal.Admin<S, E>> {
        return parameters.getTraversals()
    }

    @Override
    fun getParameters(): Parameters {
        return parameters
    }

    @get:Override
    val scopeKeys: Set<String>
        get() = parameters.getReferencedLabels()

    @Override
    fun configure(vararg keyValues: Object?) {
        parameters.set(this, keyValues)
    }

    @Override
    fun addTo(toObject: Traversal.Admin<*, *>?) {
        parameters.set(this, TO, toObject)
    }

    @Override
    fun addFrom(fromObject: Traversal.Admin<*, *>?) {
        parameters.set(this, FROM, fromObject)
    }

    @Override
    protected fun processNextStart(): Traverser.Admin<Edge> {
        return if (first) {
            first = false
            val generator: TraverserGenerator = this.getTraversal().getTraverserGenerator()

            // a dead traverser to trigger the traversal
            val traverser: Traverser.Admin = generator.generate(1, this as Step, 1)
            val edgeLabel = parameters.get(traverser, T.label) { Edge.DEFAULT_LABEL }.get(0) as String

            // FROM/TO must be set and must be vertices
            val theTo: Object = parameters.get(traverser, TO) { null }.get(0)
            if (theTo !is Vertex) throw IllegalStateException(
                String.format(
                    "addE(%s) could not find a Vertex for to() - encountered: %s", edgeLabel,
                    if (null == theTo) "null" else theTo.getClass().getSimpleName()
                )
            )
            val theFrom: Object = parameters.get(traverser, FROM) { null }.get(0)
            if (theFrom !is Vertex) throw IllegalStateException(
                String.format(
                    "addE(%s) could not find a Vertex for from() - encountered: %s", edgeLabel,
                    if (null == theFrom) "null" else theFrom.getClass().getSimpleName()
                )
            )
            var toVertex: Vertex = theTo as Vertex
            var fromVertex: Vertex = theFrom as Vertex
            if (toVertex is Attachable) toVertex = (toVertex as Attachable<Vertex?>)
                .attach(Attachable.Method.get(this.getTraversal().getGraph().orElse(EmptyGraph.instance())))
            if (fromVertex is Attachable) fromVertex = (fromVertex as Attachable<Vertex?>)
                .attach(Attachable.Method.get(this.getTraversal().getGraph().orElse(EmptyGraph.instance())))
            val edge: Edge = fromVertex.addEdge(
                edgeLabel,
                toVertex,
                parameters.getKeyValues(
                    traverser,
                    TO,
                    FROM,
                    T.label
                )
            )
            if (callbackRegistry != null && !callbackRegistry.getCallbacks().isEmpty()) {
                val eventStrategy: EventStrategy =
                    getTraversal().getStrategies().getStrategy(EventStrategy::class.java).get()
                val vae = EdgeAddedEvent(eventStrategy.detach(edge))
                callbackRegistry.getCallbacks().forEach { c -> c.accept(vae) }
            }
            generator.generate(edge, this, 1L)
        } else throw FastNoSuchElementException.instance()
    }

    @get:Override
    val mutatingCallbackRegistry: CallbackRegistry<EdgeAddedEvent>
        get() {
            if (null == callbackRegistry) callbackRegistry = ListCallbackRegistry()
            return callbackRegistry
        }

    @Override
    override fun hashCode(): Int {
        return super.hashCode() xor parameters.hashCode()
    }

    @Override
    override fun toString(): String {
        return StringFactory.stepString(this, parameters.toString())
    }

    @Override
    fun setTraversal(parentTraversal: Traversal.Admin<*, *>?) {
        super.setTraversal(parentTraversal)
        parameters.getTraversals().forEach(this::integrateChild)
    }

    @Override
    fun clone(): AddEdgeStartStep {
        val clone = super.clone() as AddEdgeStartStep
        clone.parameters = parameters.clone()
        return clone
    }

    companion object {
        private val FROM: String = Graph.Hidden.hide("from")
        private val TO: String = Graph.Hidden.hide("to")
    }
}