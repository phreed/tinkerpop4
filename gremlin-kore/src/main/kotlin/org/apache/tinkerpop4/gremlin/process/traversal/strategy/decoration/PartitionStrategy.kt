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
 * `PartitionStrategy` partitions the vertices, edges and vertex properties of a graph into String named
 * partitions (i.e. buckets, subgraphs, etc.).  It blinds a [Traversal] from "seeing" specified areas of
 * the graph given the partition names assigned to [Builder.readPartitions].  The traversal will
 * ignore all graph elements not in those "read" partitions.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class PartitionStrategy private constructor(builder: Builder) : AbstractTraversalStrategy<DecorationStrategy?>(),
    DecorationStrategy {
    val writePartition: String?
    val partitionKey: String?
    val readPartitions: Set<String>?
    val isIncludeMetaProperties: Boolean
    @Override
    fun apply(traversal: Traversal.Admin<*, *>) {
        val graph: Graph = traversal.getGraph()
            .orElseThrow { IllegalStateException("PartitionStrategy does not work with anonymous Traversals") }
        val vertexFeatures: VertexFeatures = graph.features().vertex()
        val supportsMetaProperties: Boolean = vertexFeatures.supportsMetaProperties()
        if (isIncludeMetaProperties && !supportsMetaProperties) throw IllegalStateException("PartitionStrategy is configured to include meta-properties but the Graph does not support them")

        // no need to add has after mutating steps because we want to make it so that the write partition can
        // be independent of the read partition.  in other words, i don't need to be able to read from a partition
        // in order to write to it.
        val stepsToInsertHasAfter: List<Step> = ArrayList()
        stepsToInsertHasAfter.addAll(TraversalHelper.getStepsOfAssignableClass(GraphStep::class.java, traversal))
        stepsToInsertHasAfter.addAll(TraversalHelper.getStepsOfAssignableClass(VertexStep::class.java, traversal))
        stepsToInsertHasAfter.addAll(
            TraversalHelper.getStepsOfAssignableClass(
                EdgeOtherVertexStep::class.java,
                traversal
            )
        )
        stepsToInsertHasAfter.addAll(TraversalHelper.getStepsOfAssignableClass(EdgeVertexStep::class.java, traversal))

        // all steps that return a vertex need to have has(partitionKey,within,partitionValues) injected after it
        stepsToInsertHasAfter.forEach { step ->
            TraversalHelper.insertAfterStep(
                HasStep(traversal, HasContainer(partitionKey, P.within(ArrayList(readPartitions)))), step, traversal
            )
        }
        if (isIncludeMetaProperties) {
            val propertiesSteps: List<PropertiesStep> =
                TraversalHelper.getStepsOfAssignableClass(PropertiesStep::class.java, traversal)
            propertiesSteps.forEach { step ->
                // check length first because keyExists will return true otherwise
                if (step.getPropertyKeys().length > 0 && ElementHelper.keyExists(
                        partitionKey,
                        step.getPropertyKeys()
                    )
                ) throw IllegalStateException("Cannot explicitly request the partitionKey in the traversal")
                if (step.getReturnType() === PropertyType.PROPERTY) {
                    // check the following step to see if it is a has(partitionKey, *) - if so then this strategy was
                    // already applied down below via g.V().values() which injects a properties() step
                    val next: Step = step.getNextStep()
                    if (next !is HasStep || !((next as HasStep).getHasContainers().get(0) as HasContainer).getKey()
                            .equals(partitionKey)
                    ) {
                        // use choose() to determine if the properties() step is called on a Vertex to get a VertexProperty
                        // if not, pass it through.
                        val choose: Traversal = __.choose(
                            __.filter(
                                TypeChecker<A>(
                                    VertexProperty::class.java
                                )
                            ),
                            __.has(partitionKey, P.within(ArrayList(readPartitions))),
                            __.Anon()
                        ).filter(PartitionKeyHider<Any?>())
                        TraversalHelper.insertTraversal(step, choose.asAdmin(), traversal)
                    }
                } else if (step.getReturnType() === PropertyType.VALUE) {
                    // use choose() to determine if the values() step is called on a Vertex to get a VertexProperty
                    // if not, pass it through otherwise explode g.V().values() to g.V().properties().has().value()
                    val choose: Traversal = __.choose(
                        __.filter(
                            TypeChecker<A>(
                                Vertex::class.java
                            )
                        ),
                        __.properties(step.getPropertyKeys()).has(partitionKey, P.within(ArrayList(readPartitions)))
                            .filter(PartitionKeyHider<Any?>()).value(),
                        __.Anon().filter(PartitionKeyHider<Any?>())
                    )
                    TraversalHelper.insertTraversal(step, choose.asAdmin(), traversal)
                    traversal.removeStep(step)
                } else {
                    throw IllegalStateException(
                        String.format(
                            "%s is not accounting for a particular %s %s",
                            PartitionStrategy::class.java.getSimpleName(),
                            PropertyType::class.java.toString(),
                            step.getReturnType()
                        )
                    )
                }
            }
            val propertyMapSteps: List<PropertyMapStep> =
                TraversalHelper.getStepsOfAssignableClass(PropertyMapStep::class.java, traversal)
            propertyMapSteps.forEach { step ->
                // check length first because keyExists will return true otherwise
                if (step.getPropertyKeys().length > 0 && ElementHelper.keyExists(
                        partitionKey,
                        step.getPropertyKeys()
                    )
                ) throw IllegalStateException("Cannot explicitly request the partitionKey in the traversal")
                if (step.getReturnType() === PropertyType.PROPERTY) {
                    // via map() filter out properties that aren't in the partition if it is a PropertyVertex,
                    // otherwise just let them pass through
                    TraversalHelper.insertAfterStep(LambdaMapStep(traversal, MapPropertiesFilter()), step, traversal)
                } else if (step.getReturnType() === PropertyType.VALUE) {
                    // as this is a value map, replace that step with propertiesMap() that returns PropertyType.VALUE.
                    // from there, add the filter as shown above and then unwrap the properties as they would have
                    // been done under valueMap()
                    val propertyMapStep = PropertyMapStep(traversal, PropertyType.PROPERTY, step.getPropertyKeys())
                    propertyMapStep.configure(WithOptions.tokens, step.getIncludedTokens())
                    TraversalHelper.replaceStep(step, propertyMapStep, traversal)
                    val mapPropertiesFilterStep = LambdaMapStep(traversal, MapPropertiesFilter())
                    TraversalHelper.insertAfterStep(mapPropertiesFilterStep, propertyMapStep, traversal)
                    TraversalHelper.insertAfterStep(
                        LambdaMapStep(traversal, MapPropertiesConverter()),
                        mapPropertiesFilterStep,
                        traversal
                    )
                } else {
                    throw IllegalStateException(
                        String.format(
                            "%s is not accounting for a particular %s %s",
                            PartitionStrategy::class.java.getSimpleName(),
                            PropertyType::class.java.toString(),
                            step.getReturnType()
                        )
                    )
                }
            }
        }
        val stepsToInsertPropertyMutations: List<Step> = traversal.getSteps().stream().filter { step ->
            step is AddEdgeStep || step is AddVertexStep ||
                    step is AddEdgeStartStep || step is AddVertexStartStep ||
                    isIncludeMetaProperties && step is AddPropertyStep
        }.collect(Collectors.toList())
        stepsToInsertPropertyMutations.forEach { step ->
            // note that with AddPropertyStep we just add the partition key/value regardless of whether this
            // ends up being a Vertex or not.  AddPropertyStep currently chooses to simply not bother
            // to use the additional "property mutations" if the Element being mutated is a Edge or
            // VertexProperty
            (step as Mutating).configure(partitionKey, writePartition)
            if (isIncludeMetaProperties) {
                // GraphTraversal folds g.addV().property('k','v') to just AddVertexStep/AddVertexStartStep so this
                // has to be exploded back to g.addV().property(cardinality, 'k','v','partition','A')
                if (step is AddVertexStartStep || step is AddVertexStep) {
                    val parameters: Parameters = (step as Parameterizing).getParameters()
                    val params: Map<Object, List<Object>> = parameters.getRaw()
                    params.forEach { k, v ->

                        // need to filter out T based keys
                        if (k is String) {
                            val addPropertyStepsToAppend: List<Step> = ArrayList(v.size())
                            val cardinality: VertexProperty.Cardinality = vertexFeatures.getCardinality(k as String)
                            v.forEach { o ->
                                val addPropertyStep = AddPropertyStep(traversal, cardinality, k, o)
                                addPropertyStep.configure(partitionKey, writePartition)
                                addPropertyStepsToAppend.add(addPropertyStep)

                                // need to remove the parameter from the AddVertex/StartStep because it's now being added
                                // via the AddPropertyStep
                                parameters.remove(k)
                            }
                            Collections.reverse(addPropertyStepsToAppend)
                            addPropertyStepsToAppend.forEach { s ->
                                TraversalHelper.insertAfterStep(
                                    s,
                                    step,
                                    traversal
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * A concrete lambda implementation that checks if the type passing through on the [Traverser] is
     * of a specific [Element] type.
     */
    inner class TypeChecker<A>(toCheck: Class<out Element?>) : Predicate<Traverser<A>?>, Serializable {
        val toCheck: Class<out Element?>

        init {
            this.toCheck = toCheck
        }

        @Override
        fun test(traverser: Traverser): Boolean {
            return toCheck.isAssignableFrom(traverser.get().getClass())
        }

        @Override
        override fun toString(): String {
            return "instanceOf(" + toCheck.getSimpleName().toString() + ")"
        }
    }

    /**
     * A concrete lambda implementation that filters out the partition key so that it isn't visible when making
     * calls to [GraphTraversal.valueMap].
     */
    inner class PartitionKeyHider<A : Property?> : Predicate<Traverser<A>?>, Serializable {
        @Override
        fun test(traverser: Traverser<A>): Boolean {
            return !traverser.get().key().equals(partitionKey)
        }

        @Override
        override fun toString(): String {
            return "remove($partitionKey)"
        }
    }

    /**
     * Takes the result of a [Map] containing [Property] lists and if the property is a
     * [VertexProperty] it applies a filter based on the current partitioning.  If is not a
     * [VertexProperty] the property is simply passed through.
     */
    inner class MapPropertiesFilter :
        Function<Traverser<Map<String?, List<Property?>?>?>?, Map<String?, List<Property?>?>?>, Serializable {
        @Override
        fun apply(mapTraverser: Traverser<Map<String?, List<Property?>?>?>): Map<String, List<Property>> {
            val values: Map<String, List<Property>> = mapTraverser.get()
            val filtered: Map<String, List<Property>> = HashMap()

            // note the final filter that removes the partitionKey from the outgoing Map
            values.entrySet().forEach { p ->
                val l: List = p.getValue().stream().filter { property ->
                    if (property is VertexProperty) {
                        val itty: Iterator<String> = (property as VertexProperty).values(partitionKey)
                        return@filter itty.hasNext() && readPartitions!!.contains(itty.next())
                    } else {
                        return@filter true
                    }
                }.filter { property -> !property.key().equals(partitionKey) }.collect(Collectors.toList())
                if (l.size() > 0) filtered.put(p.getKey(), l)
            }
            return filtered
        }

        @Override
        override fun toString(): String {
            return "applyPartitionFilter"
        }
    }

    /**
     * Takes a [Map] of a [List] of [Property] objects and unwraps the [Property.value].
     */
    inner class MapPropertiesConverter :
        Function<Traverser<Map<String?, List<Property?>?>?>?, Map<String?, List<Property?>?>?>, Serializable {
        @Override
        fun apply(mapTraverser: Traverser<Map<String?, List<Property?>?>?>): Map<String, List<Property>> {
            val values: Map<String, List<Property>> = mapTraverser.get()
            val converted: Map<String, List<Property>> = HashMap()
            values.entrySet().forEach { p ->
                val l: List = p.getValue().stream().map { property -> property.value() }.collect(Collectors.toList())
                converted.put(p.getKey(), l)
            }
            return converted
        }

        @Override
        override fun toString(): String {
            return "extractValuesInPropertiesMap"
        }
    }

    @get:Override
    val configuration: Configuration
        get() {
            val map: Map<String, Object> = HashMap()
            map.put(STRATEGY, PartitionStrategy::class.java.getCanonicalName())
            map.put(INCLUDE_META_PROPERTIES, isIncludeMetaProperties)
            if (null != writePartition) map.put(WRITE_PARTITION, writePartition)
            if (null != readPartitions) map.put(READ_PARTITIONS, readPartitions)
            if (null != partitionKey) map.put(PARTITION_KEY, partitionKey)
            return MapConfiguration(map)
        }

    init {
        writePartition = builder.writePartition
        partitionKey = builder.partitionKey
        readPartitions = Collections.unmodifiableSet(builder.readPartitions)
        isIncludeMetaProperties = builder.includeMetaProperties
    }

    class Builder internal constructor() {
        var writePartition: String? = null
        var partitionKey: String? = null
        val readPartitions: Set<String> = HashSet()
        var includeMetaProperties = false

        /**
         * Set to `true` if the [VertexProperty] instances should get assigned to partitions.  This
         * has the effect of hiding properties within a particular partition so that in order for the
         * [VertexProperty] to be seen both the parent [Vertex] and the [VertexProperty] must have
         * readable partitions defined in the strategy.
         *
         *
         * When setting this to `true` (it is `false` by default) it is important that the [Graph]
         * support the meta-properties feature.  If it does not errors will ensue.
         */
        fun includeMetaProperties(includeMetaProperties: Boolean): Builder {
            this.includeMetaProperties = includeMetaProperties
            return this
        }

        /**
         * Specifies the name of the partition to write when adding vertices, edges and vertex properties.  This
         * name can be any user defined value.  It is only possible to write to a single partition at a time.
         */
        fun writePartition(writePartition: String?): Builder {
            this.writePartition = writePartition
            return this
        }

        /**
         * Specifies the partition key name.  This is the property key that contains the partition value. It
         * may a good choice to index on this key in certain cases (in graphs that support such things). This
         * value must be specified for the `PartitionStrategy` to be constructed properly.
         */
        fun partitionKey(partitionKey: String?): Builder {
            this.partitionKey = partitionKey
            return this
        }

        /**
         * Specifies the partition of the graph to read from.  It is possible to assign multiple partition keys so
         * as to read from multiple partitions at the same time.
         */
        fun readPartitions(readPartitions: List<String?>?): Builder {
            this.readPartitions.addAll(readPartitions)
            return this
        }

        /**
         * Specifies the partition of the graph to read from.  It is possible to assign multiple partition keys so
         * as to read from multiple partitions at the same time.
         */
        fun readPartitions(vararg readPartitions: String?): Builder {
            return this.readPartitions(Arrays.asList(readPartitions))
        }

        /**
         * Creates the `PartitionStrategy`.
         */
        fun create(): PartitionStrategy {
            if (partitionKey == null || partitionKey.isEmpty()) throw IllegalStateException("The partitionKey cannot be null or empty")
            return PartitionStrategy(this)
        }
    }

    companion object {
        fun build(): Builder {
            return Builder()
        }

        const val INCLUDE_META_PROPERTIES = "includeMetaProperties"
        const val WRITE_PARTITION = "writePartition"
        const val PARTITION_KEY = "partitionKey"
        const val READ_PARTITIONS = "readPartitions"
        fun create(configuration: Configuration): PartitionStrategy {
            val builder = build()
            if (configuration.containsKey(INCLUDE_META_PROPERTIES)) builder.includeMetaProperties(
                configuration.getBoolean(
                    INCLUDE_META_PROPERTIES
                )
            )
            if (configuration.containsKey(WRITE_PARTITION)) builder.writePartition(
                configuration.getString(
                    WRITE_PARTITION
                )
            )
            if (configuration.containsKey(PARTITION_KEY)) builder.partitionKey(configuration.getString(PARTITION_KEY))
            if (configuration.containsKey(READ_PARTITIONS)) builder.readPartitions(
                ArrayList(
                    configuration.getProperty(
                        READ_PARTITIONS
                    ) as Collection
                )
            )
            return builder.create()
        }
    }
}