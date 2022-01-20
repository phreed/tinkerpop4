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
package org.apache.tinkerpop4.gremlin.process.traversal.strategy.decoration

import org.apache.commons.configuration2.Configuration

/**
 * Provides a degree of control over element identifier assignment as some graphs don't provide that feature. This
 * strategy provides for identifier assignment by enabling users to utilize vertex and edge indices under the hood,
 * thus simulating that capability.
 *
 *
 * By default, when an identifier is not supplied by the user, newly generated identifiers are [UUID] objects.
 * This behavior can be overridden by setting the [Builder.idMaker].
 *
 *
 * Unless otherwise specified the identifier is stored in the `__id` property.  This can be changed by setting
 * the [Builder.idPropertyKey]
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class ElementIdStrategy private constructor(val idPropertyKey: String, idMaker: Supplier<Object>) :
    AbstractTraversalStrategy<DecorationStrategy?>(), DecorationStrategy {
    private val idMaker: Supplier<Object>
    fun getIdMaker(): Supplier<Object> {
        return idMaker
    }

    @Override
    fun apply(traversal: Traversal.Admin<*, *>) {
        TraversalHelper.getStepsOfAssignableClass(HasStep::class.java, traversal).stream()
            .filter { hasStep -> (hasStep as HasStep<*>).getHasContainers().get(0).getKey().equals(T.id.getAccessor()) }
            .forEach { hasStep -> (hasStep as HasStep<*>).getHasContainers().get(0).setKey(idPropertyKey) }
        if (traversal.getStartStep() is GraphStep) {
            val graphStep: GraphStep = traversal.getStartStep() as GraphStep
            // only need to apply the custom id if ids were assigned - otherwise we want the full iterator.
            // note that it is then only necessary to replace the step if the id is a non-element.  other tests
            // in the suite validate that items in getIds() is uniform so it is ok to just test the first item
            // in the list.
            if (graphStep.getIds().length > 0 && graphStep.getIds().get(0) !is Element) {
                if (graphStep is HasContainerHolder) (graphStep as HasContainerHolder).addHasContainer(
                    HasContainer(
                        idPropertyKey, P.within(Arrays.asList(graphStep.getIds()))
                    )
                ) else TraversalHelper.insertAfterStep(
                    HasStep(
                        traversal, HasContainer(
                            idPropertyKey, P.within(Arrays.asList(graphStep.getIds()))
                        )
                    ), graphStep, traversal
                )
                graphStep.clearIds()
            }
        }
        TraversalHelper.getStepsOfAssignableClass(IdStep::class.java, traversal).stream().forEach { step ->
            TraversalHelper.replaceStep(
                step,
                PropertiesStep(traversal, PropertyType.VALUE, idPropertyKey),
                traversal
            )
        }

        // in each case below, determine if the T.id is present and if so, replace T.id with the idPropertyKey or if
        // it is not present then shove it in there and generate an id
        traversal.getSteps().forEach { step ->
            if (step is AddVertexStep || step is AddVertexStartStep || step is AddEdgeStep) {
                val parameterizing: Parameterizing = step as Parameterizing
                if (parameterizing.getParameters().contains(T.id)) parameterizing.getParameters()
                    .rename(T.id, idPropertyKey) else if (!parameterizing.getParameters().contains(
                        idPropertyKey
                    )
                ) parameterizing.getParameters().set(null, idPropertyKey, idMaker.get())
            }
        }
    }

    @Override
    override fun toString(): String {
        return StringFactory.traversalStrategyString(this)
    }

    class Builder {
        private var idPropertyKey = "__id"
        private var idMaker: Supplier<Object> = Supplier<Object> { UUID.randomUUID().toString() }

        /**
         * Creates a new unique identifier for the next created [Element].
         */
        fun idMaker(idMaker: Supplier<Object?>): Builder {
            this.idMaker = idMaker
            return this
        }

        /**
         * This key within which to hold the user-specified identifier.  This field should be indexed by the
         * underlying graph.
         */
        fun idPropertyKey(idPropertyKey: String): Builder {
            this.idPropertyKey = idPropertyKey
            return this
        }

        fun create(): ElementIdStrategy {
            return ElementIdStrategy(idPropertyKey, idMaker)
        }
    }

    init {
        this.idMaker = idMaker
    }

    @get:Override
    val configuration: Configuration
        get() {
            val map: Map<String, Object> = HashMap()
            map.put(STRATEGY, ElementIdStrategy::class.java.getCanonicalName())
            map.put(ID_PROPERTY_KEY, idPropertyKey)
            map.put(ID_MAKER, idMaker)
            return MapConfiguration(map)
        }

    companion object {
        fun build(): Builder {
            return Builder()
        }

        const val ID_PROPERTY_KEY = "idPropertyKey"
        const val ID_MAKER = "idMaker"
        fun create(configuration: Configuration): ElementIdStrategy {
            val builder = build()
            if (configuration.containsKey(ID_MAKER)) builder.idMaker(configuration.getProperty(ID_MAKER) as Supplier)
            if (configuration.containsKey(ID_PROPERTY_KEY)) builder.idPropertyKey(
                configuration.getString(
                    ID_PROPERTY_KEY
                )
            )
            return builder.create()
        }
    }
}