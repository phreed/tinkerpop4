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
class AddPropertyStep<S : Element?>(
    traversal: Traversal.Admin?,
    cardinality: VertexProperty.Cardinality?,
    keyObject: Object?,
    valueObject: Object?
) : SideEffectStep<S>(traversal), Mutating<ElementPropertyChangedEvent?>, TraversalParent, Scoping {
    private var parameters: Parameters = Parameters()
    private val cardinality: VertexProperty.Cardinality?
    private var callbackRegistry: CallbackRegistry<ElementPropertyChangedEvent>? = null

    init {
        parameters.set(this, T.key, keyObject, T.value, valueObject)
        this.cardinality = cardinality
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
    fun configure(vararg keyValues: Object?) {
        parameters.set(this, keyValues)
    }

    @Override
    protected fun sideEffect(traverser: Traverser.Admin<S>) {
        val k: Object = parameters.get(
            traverser,
            T.key
        ) { throw IllegalStateException("The AddPropertyStep does not have a provided key: $this") }
            .get(0)

        // T identifies immutable components of elements. only property keys can be modified.
        if (k is T) throw IllegalStateException(
            String.format(
                "T.%s is immutable on existing elements",
                (k as T).name()
            )
        )
        val key = k as String
        val value: Object = parameters.get(
            traverser,
            T.value
        ) { throw IllegalStateException("The AddPropertyStep does not have a provided value: $this") }
            .get(0)
        val vertexPropertyKeyValues: Array<Object> = parameters.getKeyValues(traverser, T.key, T.value)
        val element: Element = traverser.get()

        // can't set cardinality if the element is something other than a vertex as only vertices can have
        // a cardinality of properties. if we don't throw an error here we end up with a confusing cast exception
        // which doesn't explain what went wrong
        if (cardinality != null && element !is Vertex) throw IllegalStateException(
            String.format(
                "Property cardinality can only be set for a Vertex but the traversal encountered %s for key: %s",
                element.getClass().getSimpleName(), key
            )
        )
        if (callbackRegistry != null && !callbackRegistry.getCallbacks().isEmpty()) {
            getTraversal().getStrategies().getStrategy(EventStrategy::class.java)
                .ifPresent { eventStrategy ->
                    var evt: ElementPropertyChangedEvent? = null
                    if (element is Vertex) {
                        val cardinality: VertexProperty.Cardinality =
                            if (cardinality != null) cardinality else element.graph().features().vertex()
                                .getCardinality(key)
                        if (cardinality === VertexProperty.Cardinality.list) {
                            evt = VertexPropertyChangedEvent(
                                eventStrategy.detach(element as Vertex),
                                KeyedVertexProperty(key), value, vertexPropertyKeyValues
                            )
                        } else if (cardinality === VertexProperty.Cardinality.set) {
                            var currentProperty: Property? = null
                            val properties: Iterator<Property?> = traverser.get().properties(key)
                            while (properties.hasNext()) {
                                val property: Property? = properties.next()
                                if (Objects.equals(property.value(), value)) {
                                    currentProperty = property
                                    break
                                }
                            }
                            evt = VertexPropertyChangedEvent(
                                eventStrategy.detach(element as Vertex),
                                if (currentProperty == null) KeyedVertexProperty(key) else eventStrategy.detach(
                                    currentProperty as VertexProperty?
                                ), value, vertexPropertyKeyValues
                            )
                        }
                    }
                    if (evt == null) {
                        val currentProperty: Property = traverser.get().property(key)
                        val newProperty =
                            if (element is Vertex) currentProperty === VertexProperty.empty() else currentProperty === Property.empty()
                        if (element is Vertex) evt = VertexPropertyChangedEvent(
                            eventStrategy.detach(element as Vertex),
                            if (newProperty) KeyedVertexProperty(key) else eventStrategy.detach(currentProperty as VertexProperty),
                            value,
                            vertexPropertyKeyValues
                        ) else if (element is Edge) evt = EdgePropertyChangedEvent(
                            eventStrategy.detach(element as Edge),
                            if (newProperty) KeyedProperty(key) else eventStrategy.detach(currentProperty), value
                        ) else if (element is VertexProperty) evt = VertexPropertyPropertyChangedEvent(
                            eventStrategy.detach(element as VertexProperty),
                            if (newProperty) KeyedProperty(key) else eventStrategy.detach(currentProperty), value
                        ) else throw IllegalStateException(
                            String.format(
                                "The incoming object cannot be processed by change eventing in %s:  %s",
                                AddPropertyStep::class.java.getName(),
                                element
                            )
                        )
                    }
                    val event: ElementPropertyChangedEvent? = evt
                    callbackRegistry.getCallbacks().forEach { c -> c.accept(event) }
                }
        }
        if (null != cardinality) (element as Vertex).property(
            cardinality,
            key,
            value,
            vertexPropertyKeyValues
        ) else if (vertexPropertyKeyValues.size > 0) (element as Vertex).property(
            key,
            value,
            vertexPropertyKeyValues
        ) else element.property(key, value)
    }

    @get:Override
    val requirements: Set<Any>
        get() = this.getSelfAndChildRequirements(TraverserRequirement.OBJECT)

    @get:Override
    val mutatingCallbackRegistry: CallbackRegistry<ElementPropertyChangedEvent>
        get() {
            if (null == callbackRegistry) callbackRegistry = ListCallbackRegistry()
            return callbackRegistry
        }

    @Override
    override fun hashCode(): Int {
        val hash = super.hashCode() xor parameters.hashCode()
        return if (null != cardinality) hash xor cardinality.hashCode() else hash
    }

    @Override
    fun setTraversal(parentTraversal: Traversal.Admin<*, *>?) {
        super.setTraversal(parentTraversal)
        parameters.getTraversals().forEach(this::integrateChild)
    }

    fun getCardinality(): VertexProperty.Cardinality? {
        return cardinality
    }

    @Override
    override fun toString(): String {
        return StringFactory.stepString(this, parameters)
    }

    @Override
    fun clone(): AddPropertyStep<S> {
        val clone = super.clone() as AddPropertyStep<S>
        clone.parameters = parameters.clone()
        return clone
    }
}