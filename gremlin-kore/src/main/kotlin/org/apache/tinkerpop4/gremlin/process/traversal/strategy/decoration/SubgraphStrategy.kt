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
 * This [TraversalStrategy] provides a way to limit the view of a [Traversal].  By providing
 * [Traversal] representations that represent a form of filtering criterion for vertices and/or edges,
 * this strategy will inject that criterion into the appropriate places of a traversal thus restricting what
 * it traverses and returns.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class SubgraphStrategy private constructor(builder: Builder) : AbstractTraversalStrategy<DecorationStrategy?>(),
    DecorationStrategy {
    private val vertexCriterion: Traversal.Admin<Vertex, *>
    private var edgeCriterion: Traversal.Admin<Edge, *>? = null
    private val vertexPropertyCriterion: Traversal.Admin<VertexProperty, *>
    private val checkAdjacentVertices: Boolean
    private val MARKER: String = Graph.Hidden.hide("gremlin.subgraphStrategy")
    @Override
    fun apply(traversal: Traversal.Admin<*, *>) {
        // do not apply subgraph strategy to already created subgraph filter branches (or else you get infinite recursion)
        if (traversal.getStartStep().getLabels().contains(MARKER)) {
            traversal.getStartStep().removeLabel(MARKER)
            return
        }

        //
        val graphSteps: List<GraphStep> = TraversalHelper.getStepsOfAssignableClass(GraphStep::class.java, traversal)
        val vertexSteps: List<VertexStep> = TraversalHelper.getStepsOfAssignableClass(VertexStep::class.java, traversal)
        if (null != vertexCriterion) {
            val vertexStepsToInsertFilterAfter: List<Step> = ArrayList()
            vertexStepsToInsertFilterAfter.addAll(
                TraversalHelper.getStepsOfAssignableClass(
                    EdgeOtherVertexStep::class.java,
                    traversal
                )
            )
            vertexStepsToInsertFilterAfter.addAll(
                TraversalHelper.getStepsOfAssignableClass(
                    EdgeVertexStep::class.java,
                    traversal
                )
            )
            vertexStepsToInsertFilterAfter.addAll(
                TraversalHelper.getStepsOfAssignableClass(
                    AddVertexStep::class.java,
                    traversal
                )
            )
            vertexStepsToInsertFilterAfter.addAll(
                TraversalHelper.getStepsOfAssignableClass(
                    AddVertexStartStep::class.java,
                    traversal
                )
            )
            vertexStepsToInsertFilterAfter.addAll(
                graphSteps.stream().filter(GraphStep::returnsVertex).collect(Collectors.toList())
            )
            applyCriterion(vertexStepsToInsertFilterAfter, traversal, vertexCriterion)
        }
        if (null != edgeCriterion) {
            val edgeStepsToInsertFilterAfter: List<Step> = ArrayList()
            edgeStepsToInsertFilterAfter.addAll(
                TraversalHelper.getStepsOfAssignableClass(
                    AddEdgeStep::class.java,
                    traversal
                )
            )
            edgeStepsToInsertFilterAfter.addAll(
                graphSteps.stream().filter(GraphStep::returnsEdge).collect(Collectors.toList())
            )
            edgeStepsToInsertFilterAfter.addAll(
                vertexSteps.stream().filter(VertexStep::returnsEdge).collect(Collectors.toList())
            )
            applyCriterion(edgeStepsToInsertFilterAfter, traversal, edgeCriterion)
        }

        // turn g.V().out() to g.V().outE().inV() only if there is an edge predicate otherwise
        for (step in vertexSteps) {
            if (step.returnsEdge()) continue
            if (null != vertexCriterion && null == edgeCriterion) {
                TraversalHelper.insertAfterStep(
                    TraversalFilterStep(traversal, vertexCriterion.clone() as Traversal),
                    step,
                    traversal
                )
            } else {
                val someEStep: VertexStep<Edge> =
                    VertexStep(traversal, Edge::class.java, step.getDirection(), step.getEdgeLabels())
                val someVStep: Step<Edge, Vertex> =
                    if (step.getDirection() === Direction.BOTH) EdgeOtherVertexStep(traversal) else EdgeVertexStep(
                        traversal,
                        step.getDirection().opposite()
                    )
                TraversalHelper.replaceStep(step as Step<Vertex?, Edge?>, someEStep, traversal)
                TraversalHelper.insertAfterStep(someVStep, someEStep, traversal)
                TraversalHelper.copyLabels(step, someVStep, true)
                if (null != edgeCriterion) TraversalHelper.insertAfterStep(
                    TraversalFilterStep(
                        traversal,
                        edgeCriterion.clone()
                    ), someEStep, traversal
                )
                if (null != vertexCriterion) TraversalHelper.insertAfterStep(
                    TraversalFilterStep(
                        traversal,
                        vertexCriterion.clone()
                    ), someVStep, traversal
                )
            }
        }

        // turn g.V().properties() to g.V().properties().xxx
        // turn g.V().values() to g.V().properties().xxx.value()
        if (null != vertexPropertyCriterion) {
            val checkPropertyCriterion: OrStep<Object> = OrStep(
                traversal,
                DefaultTraversal().addStep(ClassFilterStep(traversal, VertexProperty::class.java, false)),
                DefaultTraversal().addStep(TraversalFilterStep(traversal, vertexPropertyCriterion))
            )
            val nonCheckPropertyCriterion: Traversal.Admin =
                DefaultTraversal().addStep(TraversalFilterStep(traversal, vertexPropertyCriterion))

            // turn all ElementValueTraversals into filters
            for (step in traversal.getSteps()) {
                if (step is TraversalParent) {
                    if (step is PropertyMapStep) {
                        val propertyType = processesPropertyType(step.getPreviousStep())
                        if ('p' != propertyType) {
                            val temp: Traversal.Admin<*, *> = DefaultTraversal()
                            temp.addStep(
                                PropertiesStep(
                                    temp,
                                    PropertyType.PROPERTY,
                                    (step as PropertyMapStep).getPropertyKeys()
                                )
                            )
                            if ('v' == propertyType) TraversalHelper.insertTraversal(
                                0,
                                nonCheckPropertyCriterion.clone(),
                                temp
                            ) else temp.addStep(checkPropertyCriterion.clone())
                            (step as PropertyMapStep).setPropertyTraversal(temp)
                        }
                    } else {
                        Stream.concat(
                            (step as TraversalParent).getGlobalChildren().stream(),
                            (step as TraversalParent).getLocalChildren().stream()
                        )
                            .filter { t -> t is ValueTraversal }
                            .forEach { t ->
                                val propertyType = processesPropertyType(step.getPreviousStep())
                                if ('p' != propertyType) {
                                    val temp: Traversal.Admin<*, *> = DefaultTraversal()
                                    temp.addStep(
                                        PropertiesStep(
                                            temp,
                                            PropertyType.PROPERTY,
                                            (t as ValueTraversal).getPropertyKey()
                                        )
                                    )
                                    if ('v' == propertyType) TraversalHelper.insertTraversal(
                                        0,
                                        nonCheckPropertyCriterion.clone(),
                                        temp
                                    ) else temp.addStep(checkPropertyCriterion.clone())
                                    temp.addStep(PropertyValueStep(temp))
                                    temp.setParent(step as TraversalParent)
                                    (t as ValueTraversal).setBypassTraversal(temp)
                                }
                            }
                    }
                }
            }
            for (step in TraversalHelper.getStepsOfAssignableClass(PropertiesStep::class.java, traversal)) {
                val propertyType = processesPropertyType(step.getPreviousStep())
                if ('p' != propertyType) {
                    if (PropertyType.PROPERTY === (step as PropertiesStep).getReturnType()) {
                        // if the property step returns a property, then simply append the criterion
                        if ('v' == propertyType) {
                            val temp: Traversal.Admin<*, *> = nonCheckPropertyCriterion.clone()
                            TraversalHelper.insertTraversal(step as Step, temp, traversal)
                            TraversalHelper.copyLabels(step, temp.getEndStep(), true)
                        } else {
                            val temp: Step<*, *> = checkPropertyCriterion.clone()
                            TraversalHelper.insertAfterStep(temp, step as Step, traversal)
                            TraversalHelper.copyLabels(step, temp, true)
                        }
                    } else {
                        // if the property step returns value, then replace it with a property step, append criterion, then append a value() step
                        val propertiesStep: Step =
                            PropertiesStep(traversal, PropertyType.PROPERTY, (step as PropertiesStep).getPropertyKeys())
                        TraversalHelper.replaceStep(step, propertiesStep, traversal)
                        val propertyValueStep: Step = PropertyValueStep(traversal)
                        TraversalHelper.copyLabels(step, propertyValueStep, false)
                        if ('v' == propertyType) {
                            TraversalHelper.insertAfterStep(propertyValueStep, propertiesStep, traversal)
                            TraversalHelper.insertTraversal(
                                propertiesStep,
                                nonCheckPropertyCriterion.clone(),
                                traversal
                            )
                        } else {
                            TraversalHelper.insertAfterStep(propertyValueStep, propertiesStep, traversal)
                            TraversalHelper.insertAfterStep(checkPropertyCriterion.clone(), propertiesStep, traversal)
                        }
                    }
                }
            }
        }
    }

    @get:Override
    val configuration: Configuration
        get() {
            val map: Map<String, Object> = HashMap()
            if (null != vertexCriterion) map.put(VERTICES, vertexCriterion)
            if (null != edgeCriterion) map.put(EDGES, edgeCriterion)
            if (null != vertexPropertyCriterion) map.put(VERTEX_PROPERTIES, vertexPropertyCriterion)
            map.put(CHECK_ADJACENT_VERTICES, checkAdjacentVertices)
            return MapConfiguration(map)
        }

    @Override
    fun applyPost(): Set<Class<out DecorationStrategy?>> {
        return POSTS
    }

    fun getVertexCriterion(): Traversal<Vertex, *> {
        return vertexCriterion
    }

    fun getEdgeCriterion(): Traversal<Edge, *> {
        return edgeCriterion
    }

    fun getVertexPropertyCriterion(): Traversal<VertexProperty, *> {
        return vertexPropertyCriterion
    }

    init {
        vertexCriterion = if (null == builder.vertexCriterion) null else builder.vertexCriterion.asAdmin().clone()
        checkAdjacentVertices = builder.checkAdjacentVertices

        // if there is no vertex predicate there is no need to test either side of the edge - also this option can
        // be simply configured in the builder to not be used
        if (null == vertexCriterion || !checkAdjacentVertices) {
            edgeCriterion = if (null == builder.edgeCriterion) null else builder.edgeCriterion.asAdmin().clone()
        } else {
            val vertexPredicate: Traversal.Admin<Edge, *>
            vertexPredicate = __.< Edge > and < Edge ? > __.inV().filter(vertexCriterion),
            __.outV().filter(vertexCriterion)).asAdmin()

            // if there is a vertex predicate then there is an implied edge filter on vertices even if there is no
            // edge predicate provided by the user.
            edgeCriterion =
                if (null == builder.edgeCriterion) vertexPredicate else builder.edgeCriterion.asAdmin().clone()
                    .addStep(TraversalFilterStep(builder.edgeCriterion.asAdmin(), vertexPredicate))
        }
        vertexPropertyCriterion =
            if (null == builder.vertexPropertyCriterion) null else builder.vertexPropertyCriterion.asAdmin().clone()
        if (null != vertexCriterion) TraversalHelper.applyTraversalRecursively({ t ->
            t.getStartStep().addLabel(MARKER)
        }, vertexCriterion)
        if (null != edgeCriterion) TraversalHelper.applyTraversalRecursively(
            { t -> t.getStartStep().addLabel(MARKER) },
            edgeCriterion
        )
        if (null != vertexPropertyCriterion) TraversalHelper.applyTraversalRecursively({ t ->
            t.getStartStep().addLabel(MARKER)
        }, vertexPropertyCriterion)
    }

    class Builder private constructor() {
        private var vertexCriterion: Traversal<Vertex, *>? = null
        private var edgeCriterion: Traversal<Edge, *>? = null
        private var vertexPropertyCriterion: Traversal<VertexProperty, *>? = null
        private var checkAdjacentVertices = true

        /**
         * Enables the strategy to apply the [.vertices] filter to the adjacent vertices of an edge.
         * If using this strategy for OLAP then this value should be set to `false` as checking adjacent vertices
         * will force the traversal to leave the local star graph (which is not possible in OLAP) and will cause an
         * error. By default, this value is `true`.
         */
        fun checkAdjacentVertices(enable: Boolean): Builder {
            checkAdjacentVertices = enable
            return this
        }

        /**
         * The traversal predicate that defines the vertices to include in the subgraph. If
         * [.checkAdjacentVertices] is `true` then this predicate will also be applied to the
         * adjacent vertices of edges. Take care when setting this value for OLAP based traversals as the traversal
         * predicate cannot be written in such a way as to leave the local star graph and can thus only evaluate the
         * current vertex and its related edges.
         */
        fun vertices(vertexPredicate: Traversal<Vertex?, *>): Builder {
            vertexCriterion = vertexPredicate
            return this
        }

        /**
         * The traversal predicate that defines the edges to include in the subgraph.
         */
        fun edges(edgePredicate: Traversal<Edge?, *>): Builder {
            edgeCriterion = edgePredicate
            return this
        }

        /**
         * The traversal predicate that defines the vertex properties to include in the subgraph.
         */
        fun vertexProperties(vertexPropertyPredicate: Traversal<VertexProperty?, *>): Builder {
            vertexPropertyCriterion = vertexPropertyPredicate
            return this
        }

        fun create(): SubgraphStrategy {
            if (null == vertexCriterion && null == edgeCriterion && null == vertexPropertyCriterion) throw IllegalStateException(
                "A subgraph must be filtered by a vertex, edge, or vertex property criterion"
            )
            return SubgraphStrategy(this)
        }
    }

    companion object {
        private val POSTS: Set<Class<out DecorationStrategy?>> = Collections.singleton(ConnectiveStrategy::class.java)
        private fun applyCriterion(
            stepsToApplyCriterionAfter: List<Step>, traversal: Traversal.Admin,
            criterion: Traversal.Admin<out Element?, *>
        ) {
            for (step in stepsToApplyCriterionAfter) {
                // re-assign the step label to the criterion because the label should apply seamlessly after the filter
                val filter: Step = TraversalFilterStep(traversal, criterion.clone())
                TraversalHelper.insertAfterStep(filter, step, traversal)
                TraversalHelper.copyLabels(step, filter, true)
            }
        }

        private fun processesPropertyType(step: Step): Char {
            var step: Step = step
            while (step !is EmptyStep) {
                step =
                    if (step is FilterStep || step is SideEffectStep) step.getPreviousStep() else return if (step is GraphStep && (step as GraphStep).returnsVertex()) 'v' else if (step is EdgeVertexStep) 'v' else if (step is VertexStep) if ((step as VertexStep).returnsVertex()) 'v' else 'p' else if (step is PropertyMapStep || step is PropertiesStep) 'p' else 'x'
            }
            return 'x'
        }

        const val VERTICES = "vertices"
        const val EDGES = "edges"
        const val VERTEX_PROPERTIES = "vertexProperties"
        const val CHECK_ADJACENT_VERTICES = "checkAdjacentVertices"
        fun create(configuration: Configuration): SubgraphStrategy {
            val builder = build()
            if (configuration.containsKey(VERTICES)) builder.vertices(configuration.getProperty(VERTICES) as Traversal)
            if (configuration.containsKey(EDGES)) builder.edges(configuration.getProperty(EDGES) as Traversal)
            if (configuration.containsKey(VERTEX_PROPERTIES)) builder.vertexProperties(
                configuration.getProperty(
                    VERTEX_PROPERTIES
                ) as Traversal
            )
            if (configuration.containsKey(CHECK_ADJACENT_VERTICES)) builder.checkAdjacentVertices(
                configuration.getBoolean(
                    CHECK_ADJACENT_VERTICES
                )
            )
            return builder.create()
        }

        fun build(): Builder {
            return Builder()
        }
    }
}