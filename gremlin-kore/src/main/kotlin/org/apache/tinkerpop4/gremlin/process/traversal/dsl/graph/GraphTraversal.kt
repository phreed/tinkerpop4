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
package org.apache.tinkerpop4.gremlin.process.traversal.dsl.graph

import org.apache.tinkerpop4.gremlin.process.computer.VertexProgram

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
interface GraphTraversal<S, E> : Traversal<S, E> {
    interface Admin<S, E> : Traversal.Admin<S, E>, GraphTraversal<S, E> {
        @Override
        fun <E2> addStep(step: Step<*, E2>?): Admin<S, E2>? {
            return super@Admin.addStep(step as Step?) as Admin<S, E2>?
        }

        @Override
        override fun iterate(): GraphTraversal<S, E> {
            return super@GraphTraversal.iterate()
        }

        @Override
        fun clone(): Admin<S, E>?
    }

    @Override
    fun asAdmin(): Admin<S, E> {
        return this as Admin<S, E>
    }
    ///////////////////// MAP STEPS /////////////////////
    /**
     * Map a [Traverser] referencing an object of type `E` to an object of type `E2`.
     *
     * @param function the lambda expression that does the functional mapping
     * @return the traversal with an appended [LambdaMapStep].
     * @see [Reference Documentation - General Steps](http://tinkerpop.apache.org/docs/${project.version}/reference/.general-steps)
     *
     * @since 3.0.0-incubating
     */
    fun <E2> map(function: Function<Traverser<E>?, E2>?): GraphTraversal<S, E2>? {
        asAdmin().getBytecode().addStep(Symbols.map, function)
        return asAdmin().addStep(LambdaMapStep(asAdmin(), function))
    }

    /**
     * Map a [Traverser] referencing an object of type `E` to an object of type `E2`.
     *
     * @param mapTraversal the traversal expression that does the functional mapping
     * @return the traversal with an appended [LambdaMapStep].
     * @see [Reference Documentation - General Steps](http://tinkerpop.apache.org/docs/${project.version}/reference/.general-steps)
     *
     * @since 3.0.0-incubating
     */
    fun <E2> map(mapTraversal: Traversal<*, E2>?): GraphTraversal<S, E2>? {
        asAdmin().getBytecode().addStep(Symbols.map, mapTraversal)
        return asAdmin().addStep(TraversalMapStep(asAdmin(), mapTraversal))
    }

    /**
     * Map a [Traverser] referencing an object of type `E` to an iterator of objects of type `E2`.
     * The resultant iterator is drained one-by-one before a new `E` object is pulled in for processing.
     *
     * @param function the lambda expression that does the functional mapping
     * @param <E2>     the type of the returned iterator objects
     * @return the traversal with an appended [LambdaFlatMapStep].
     * @see [Reference Documentation - General Steps](http://tinkerpop.apache.org/docs/${project.version}/reference/.general-steps)
     *
     * @since 3.0.0-incubating
    </E2> */
    fun <E2> flatMap(function: Function<Traverser<E>?, Iterator<E2>?>?): GraphTraversal<S, E2>? {
        asAdmin().getBytecode().addStep(Symbols.flatMap, function)
        return asAdmin().addStep(LambdaFlatMapStep(asAdmin(), function))
    }

    /**
     * Map a [Traverser] referencing an object of type `E` to an iterator of objects of type `E2`.
     * The internal traversal is drained one-by-one before a new `E` object is pulled in for processing.
     *
     * @param flatMapTraversal the traversal generating objects of type `E2`
     * @param <E2>             the end type of the internal traversal
     * @return the traversal with an appended [TraversalFlatMapStep].
     * @see [Reference Documentation - General Steps](http://tinkerpop.apache.org/docs/${project.version}/reference/.general-steps)
     *
     * @since 3.0.0-incubating
    </E2> */
    fun <E2> flatMap(flatMapTraversal: Traversal<*, E2>?): GraphTraversal<S, E2>? {
        asAdmin().getBytecode().addStep(Symbols.flatMap, flatMapTraversal)
        return asAdmin().addStep(TraversalFlatMapStep(asAdmin(), flatMapTraversal))
    }

    /**
     * Map the [Element] to its [Element.id].
     *
     * @return the traversal with an appended [IdStep].
     * @see [Reference Documentation - Id Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.id-step)
     *
     * @since 3.0.0-incubating
     */
    fun id(): GraphTraversal<S, Object?>? {
        asAdmin().getBytecode().addStep(Symbols.id)
        return asAdmin().addStep(IdStep(asAdmin()))
    }

    /**
     * Map the [Element] to its [Element.label].
     *
     * @return the traversal with an appended [LabelStep].
     * @see [Reference Documentation - Label Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.label-step)
     *
     * @since 3.0.0-incubating
     */
    fun label(): GraphTraversal<S, String?>? {
        asAdmin().getBytecode().addStep(Symbols.label)
        return asAdmin().addStep(LabelStep(asAdmin()))
    }

    /**
     * Map the `E` object to itself. In other words, a "no op."
     *
     * @return the traversal with an appended [IdentityStep].
     * @since 3.0.0-incubating
     */
    fun identity(): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.identity)
        return asAdmin().addStep(IdentityStep(asAdmin()))
    }

    /**
     * Map any object to a fixed `E` value.
     *
     * @return the traversal with an appended [ConstantStep].
     * @see [Reference Documentation - Constant Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.constant-step)
     *
     * @since 3.0.0-incubating
     */
    fun <E2> constant(e: E2): GraphTraversal<S, E2>? {
        asAdmin().getBytecode().addStep(Symbols.constant, e)
        return asAdmin().addStep(ConstantStep<E, E2>(asAdmin(), e))
    }

    /**
     * A `V` step is usually used to start a traversal but it may also be used mid-traversal.
     *
     * @param vertexIdsOrElements vertices to inject into the traversal
     * @return the traversal with an appended [GraphStep]
     * @see [Reference Documentation - Graph Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.graph-step)
     *
     * @since 3.1.0-incubating
     */
    fun V(vararg vertexIdsOrElements: Object?): GraphTraversal<S, Vertex?>? {
        // a single null is [null]
        val ids: Array<Object?> = vertexIdsOrElements ?: arrayOf(null)
        asAdmin().getBytecode().addStep(Symbols.V, ids)
        return asAdmin().addStep(GraphStep(asAdmin(), Vertex::class.java, false, ids))
    }

    /**
     * Map the [Vertex] to its adjacent vertices given a direction and edge labels.
     *
     * @param direction  the direction to traverse from the current vertex
     * @param edgeLabels the edge labels to traverse
     * @return the traversal with an appended [VertexStep].
     * @see [Reference Documentation - Vertex Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.vertex-steps)
     *
     * @since 3.0.0-incubating
     */
    fun to(direction: Direction?, vararg edgeLabels: String?): GraphTraversal<S, Vertex?>? {
        asAdmin().getBytecode().addStep(Symbols.to, direction, edgeLabels)
        return asAdmin().addStep(VertexStep(asAdmin(), Vertex::class.java, direction, edgeLabels))
    }

    /**
     * Map the [Vertex] to its outgoing adjacent vertices given the edge labels.
     *
     * @param edgeLabels the edge labels to traverse
     * @return the traversal with an appended [VertexStep].
     * @see [Reference Documentation - Vertex Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.vertex-steps)
     *
     * @since 3.0.0-incubating
     */
    fun out(vararg edgeLabels: String?): GraphTraversal<S, Vertex?>? {
        asAdmin().getBytecode().addStep(Symbols.out, edgeLabels)
        return asAdmin().addStep(VertexStep(asAdmin(), Vertex::class.java, Direction.OUT, edgeLabels))
    }

    /**
     * Map the [Vertex] to its incoming adjacent vertices given the edge labels.
     *
     * @param edgeLabels the edge labels to traverse
     * @return the traversal with an appended [VertexStep].
     * @see [Reference Documentation - Vertex Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.vertex-steps)
     *
     * @since 3.0.0-incubating
     */
    fun `in`(vararg edgeLabels: String?): GraphTraversal<S, Vertex?>? {
        asAdmin().getBytecode().addStep(Symbols.`in`, edgeLabels)
        return asAdmin().addStep(VertexStep(asAdmin(), Vertex::class.java, Direction.IN, edgeLabels))
    }

    /**
     * Map the [Vertex] to its adjacent vertices given the edge labels.
     *
     * @param edgeLabels the edge labels to traverse
     * @return the traversal with an appended [VertexStep].
     * @see [Reference Documentation - Vertex Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.vertex-steps)
     *
     * @since 3.0.0-incubating
     */
    fun both(vararg edgeLabels: String?): GraphTraversal<S, Vertex?>? {
        asAdmin().getBytecode().addStep(Symbols.both, edgeLabels)
        return asAdmin().addStep(VertexStep(asAdmin(), Vertex::class.java, Direction.BOTH, edgeLabels))
    }

    /**
     * Map the [Vertex] to its incident edges given the direction and edge labels.
     *
     * @param direction  the direction to traverse from the current vertex
     * @param edgeLabels the edge labels to traverse
     * @return the traversal with an appended [VertexStep].
     * @see [Reference Documentation - Vertex Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.vertex-steps)
     *
     * @since 3.0.0-incubating
     */
    fun toE(direction: Direction?, vararg edgeLabels: String?): GraphTraversal<S, Edge?>? {
        asAdmin().getBytecode().addStep(Symbols.toE, direction, edgeLabels)
        return asAdmin().addStep(VertexStep(asAdmin(), Edge::class.java, direction, edgeLabels))
    }

    /**
     * Map the [Vertex] to its outgoing incident edges given the edge labels.
     *
     * @param edgeLabels the edge labels to traverse
     * @return the traversal with an appended [VertexStep].
     * @see [Reference Documentation - Vertex Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.vertex-steps)
     *
     * @since 3.0.0-incubating
     */
    fun outE(vararg edgeLabels: String?): GraphTraversal<S, Edge?>? {
        asAdmin().getBytecode().addStep(Symbols.outE, edgeLabels)
        return asAdmin().addStep(VertexStep(asAdmin(), Edge::class.java, Direction.OUT, edgeLabels))
    }

    /**
     * Map the [Vertex] to its incoming incident edges given the edge labels.
     *
     * @param edgeLabels the edge labels to traverse
     * @return the traversal with an appended [VertexStep].
     * @see [Reference Documentation - Vertex Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.vertex-steps)
     *
     * @since 3.0.0-incubating
     */
    fun inE(vararg edgeLabels: String?): GraphTraversal<S, Edge?>? {
        asAdmin().getBytecode().addStep(Symbols.inE, edgeLabels)
        return asAdmin().addStep(VertexStep(asAdmin(), Edge::class.java, Direction.IN, edgeLabels))
    }

    /**
     * Map the [Vertex] to its incident edges given the edge labels.
     *
     * @param edgeLabels the edge labels to traverse
     * @return the traversal with an appended [VertexStep].
     * @see [Reference Documentation - Vertex Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.vertex-steps)
     *
     * @since 3.0.0-incubating
     */
    fun bothE(vararg edgeLabels: String?): GraphTraversal<S, Edge?>? {
        asAdmin().getBytecode().addStep(Symbols.bothE, edgeLabels)
        return asAdmin().addStep(VertexStep(asAdmin(), Edge::class.java, Direction.BOTH, edgeLabels))
    }

    /**
     * Map the [Edge] to its incident vertices given the direction.
     *
     * @param direction the direction to traverser from the current edge
     * @return the traversal with an appended [EdgeVertexStep].
     * @see [Reference Documentation - Vertex Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.vertex-steps)
     *
     * @since 3.0.0-incubating
     */
    fun toV(direction: Direction?): GraphTraversal<S, Vertex?>? {
        asAdmin().getBytecode().addStep(Symbols.toV, direction)
        return asAdmin().addStep(EdgeVertexStep(asAdmin(), direction))
    }

    /**
     * Map the [Edge] to its incoming/head incident [Vertex].
     *
     * @return the traversal with an appended [EdgeVertexStep].
     * @see [Reference Documentation - Vertex Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.vertex-steps)
     *
     * @since 3.0.0-incubating
     */
    fun inV(): GraphTraversal<S, Vertex?>? {
        asAdmin().getBytecode().addStep(Symbols.inV)
        return asAdmin().addStep(EdgeVertexStep(asAdmin(), Direction.IN))
    }

    /**
     * Map the [Edge] to its outgoing/tail incident [Vertex].
     *
     * @return the traversal with an appended [EdgeVertexStep].
     * @see [Reference Documentation - Vertex Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.vertex-steps)
     *
     * @since 3.0.0-incubating
     */
    fun outV(): GraphTraversal<S, Vertex?>? {
        asAdmin().getBytecode().addStep(Symbols.outV)
        return asAdmin().addStep(EdgeVertexStep(asAdmin(), Direction.OUT))
    }

    /**
     * Map the [Edge] to its incident vertices.
     *
     * @return the traversal with an appended [EdgeVertexStep].
     * @see [Reference Documentation - Vertex Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.vertex-steps)
     *
     * @since 3.0.0-incubating
     */
    fun bothV(): GraphTraversal<S, Vertex?>? {
        asAdmin().getBytecode().addStep(Symbols.bothV)
        return asAdmin().addStep(EdgeVertexStep(asAdmin(), Direction.BOTH))
    }

    /**
     * Map the [Edge] to the incident vertex that was not just traversed from in the path history.
     *
     * @return the traversal with an appended [EdgeOtherVertexStep].
     * @see [Reference Documentation - Vertex Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.vertex-steps)
     *
     * @since 3.0.0-incubating
     */
    fun otherV(): GraphTraversal<S, Vertex?>? {
        asAdmin().getBytecode().addStep(Symbols.otherV)
        return asAdmin().addStep(EdgeOtherVertexStep(asAdmin()))
    }

    /**
     * Order all the objects in the traversal up to this point and then emit them one-by-one in their ordered sequence.
     *
     * @return the traversal with an appended [OrderGlobalStep].
     * @see [Reference Documentation - Order Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.order-step)
     *
     * @since 3.0.0-incubating
     */
    fun order(): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.order)
        return asAdmin().addStep(OrderGlobalStep(asAdmin()))
    }

    /**
     * Order either the [Scope.local] object (e.g. a list, map, etc.) or the entire [Scope.global] traversal stream.
     *
     * @param scope whether the ordering is the current local object or the entire global stream.
     * @return the traversal with an appended [OrderGlobalStep] or [OrderLocalStep] depending on the `scope`.
     * @see [Reference Documentation - Order Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.order-step)
     *
     * @since 3.0.0-incubating
     */
    fun order(scope: Scope): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.order, scope)
        return asAdmin().addStep(if (scope.equals(Scope.global)) OrderGlobalStep(asAdmin()) else OrderLocalStep(asAdmin()))
    }

    /**
     * Map the [Element] to its associated properties given the provide property keys.
     * If no property keys are provided, then all properties are emitted.
     *
     * @param propertyKeys the properties to retrieve
     * @param <E2>         the value type of the returned properties
     * @return the traversal with an appended [PropertiesStep].
     * @see [Reference Documentation - Properties Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.properties-step)
     *
     * @since 3.0.0-incubating
    </E2> */
    fun <E2> properties(vararg propertyKeys: String?): GraphTraversal<S, out Property<E2?>?>? {
        asAdmin().getBytecode().addStep(Symbols.properties, propertyKeys)
        return asAdmin().addStep(PropertiesStep(asAdmin(), PropertyType.PROPERTY, propertyKeys))
    }

    /**
     * Map the [Element] to the values of the associated properties given the provide property keys.
     * If no property keys are provided, then all property values are emitted.
     *
     * @param propertyKeys the properties to retrieve their value from
     * @param <E2>         the value type of the properties
     * @return the traversal with an appended [PropertiesStep].
     * @see [Reference Documentation - Values Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.values-step)
     *
     * @since 3.0.0-incubating
    </E2> */
    fun <E2> values(vararg propertyKeys: String?): GraphTraversal<S, E2>? {
        asAdmin().getBytecode().addStep(Symbols.values, propertyKeys)
        return asAdmin().addStep(PropertiesStep(asAdmin(), PropertyType.VALUE, propertyKeys))
    }

    /**
     * Map the [Element] to a [Map] of the properties key'd according to their [Property.key].
     * If no property keys are provided, then all properties are retrieved.
     *
     * @param propertyKeys the properties to retrieve
     * @param <E2>         the value type of the returned properties
     * @return the traversal with an appended [PropertyMapStep].
     * @see [Reference Documentation - PropertyMap Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.propertymap-step)
     *
     * @since 3.0.0-incubating
    </E2> */
    fun <E2> propertyMap(vararg propertyKeys: String?): GraphTraversal<S, Map<String?, E2>?>? {
        asAdmin().getBytecode().addStep(Symbols.propertyMap, propertyKeys)
        return asAdmin().addStep(PropertyMapStep(asAdmin(), WithOptions.none, PropertyType.PROPERTY, propertyKeys))
    }

    /**
     * Map the [Element] to a `Map` of the property values key'd according to their [Property.key].
     * If no property keys are provided, then all property values are retrieved. For vertices, the `Map` will
     * be returned with the assumption of single property values along with [T.id] and [T.label]. Prefer
     * [.valueMap] if multi-property processing is required. For  edges, keys will include additional
     * related edge structure of [Direction.IN] and [Direction.OUT] which themselves are `Map`
     * instances of the particular [Vertex] represented by [T.id] and [T.label].
     *
     * @param propertyKeys the properties to retrieve
     * @param <E2>         the value type of the returned properties
     * @return the traversal with an appended [ElementMapStep].
     * @see [Reference Documentation - ElementMap Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.elementmap-step)
     *
     * @since 3.4.4
    </E2> */
    fun <E2> elementMap(vararg propertyKeys: String?): GraphTraversal<S, Map<Object?, E2>?>? {
        asAdmin().getBytecode().addStep(Symbols.elementMap, propertyKeys)
        return asAdmin().addStep(ElementMapStep(asAdmin(), propertyKeys))
    }

    /**
     * Map the [Element] to a `Map` of the property values key'd according to their [Property.key].
     * If no property keys are provided, then all property values are retrieved.
     *
     * @param propertyKeys the properties to retrieve
     * @param <E2>         the value type of the returned properties
     * @return the traversal with an appended [PropertyMapStep].
     * @see [Reference Documentation - ValueMap Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.valuemap-step)
     *
     * @since 3.0.0-incubating
    </E2> */
    fun <E2> valueMap(vararg propertyKeys: String?): GraphTraversal<S, Map<Object?, E2>?>? {
        asAdmin().getBytecode().addStep(Symbols.valueMap, propertyKeys)
        return asAdmin().addStep(PropertyMapStep(asAdmin(), WithOptions.none, PropertyType.VALUE, propertyKeys))
    }

    /**
     * Map the [Element] to a `Map` of the property values key'd according to their [Property.key].
     * If no property keys are provided, then all property values are retrieved.
     *
     * @param includeTokens whether to include [T] tokens in the emitted map.
     * @param propertyKeys  the properties to retrieve
     * @param <E2>          the value type of the returned properties
     * @return the traversal with an appended [PropertyMapStep].
     * @see [Reference Documentation - ValueMap Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.valuemap-step)
     *
     * @since 3.0.0-incubating
    </E2> */
    @Deprecated
    @Deprecated(
        """As of release 3.4.0, deprecated in favor of {@link GraphTraversal#valueMap(String...)} in conjunction with
                  {@link GraphTraversal#with(String, Object)} or simple prefer {@link #elementMap(String...)}."""
    )
    fun <E2> valueMap(includeTokens: Boolean, vararg propertyKeys: String?): GraphTraversal<S, Map<Object?, E2>?>? {
        asAdmin().getBytecode().addStep(Symbols.valueMap, includeTokens, propertyKeys)
        return asAdmin().addStep(PropertyMapStep(asAdmin(), WithOptions.all, PropertyType.VALUE, propertyKeys))
    }

    /**
     * Map the [Property] to its [Property.key].
     *
     * @return the traversal with an appended [PropertyKeyStep].
     * @see [Reference Documentation - Key Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.key-step)
     *
     * @since 3.0.0-incubating
     */
    fun key(): GraphTraversal<S, String?>? {
        asAdmin().getBytecode().addStep(Symbols.key)
        return asAdmin().addStep(PropertyKeyStep(asAdmin()))
    }

    /**
     * Map the [Property] to its [Property.value].
     *
     * @return the traversal with an appended [PropertyValueStep].
     * @see [Reference Documentation - Value Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.value-step)
     *
     * @since 3.0.0-incubating
     */
    fun <E2> value(): GraphTraversal<S, E2>? {
        asAdmin().getBytecode().addStep(Symbols.value)
        return asAdmin().addStep(PropertyValueStep(asAdmin()))
    }

    /**
     * Map the [Traverser] to its [Path] history via [Traverser.path].
     *
     * @return the traversal with an appended [PathStep].
     * @see [Reference Documentation - Path Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.path-step)
     *
     * @since 3.0.0-incubating
     */
    fun path(): GraphTraversal<S, Path?>? {
        asAdmin().getBytecode().addStep(Symbols.path)
        return asAdmin().addStep(PathStep(asAdmin()))
    }

    /**
     * Map the [Traverser] to a [Map] of bindings as specified by the provided match traversals.
     *
     * @param matchTraversals the traversal that maintain variables which must hold for the life of the traverser
     * @param <E2>            the type of the objects bound in the variables
     * @return the traversal with an appended [MatchStep].
     * @see [Reference Documentation - Match Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.match-step)
     *
     * @since 3.0.0-incubating
    </E2> */
    fun <E2> match(vararg matchTraversals: Traversal<*, *>?): GraphTraversal<S, Map<String?, E2>?>? {
        asAdmin().getBytecode().addStep(Symbols.match, matchTraversals)
        return asAdmin().addStep(MatchStep(asAdmin(), ConnectiveStep.Connective.AND, matchTraversals))
    }

    /**
     * Map the [Traverser] to its [Traverser.sack] value.
     *
     * @param <E2> the sack value type
     * @return the traversal with an appended [SackStep].
     * @see [Reference Documentation - Sack Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.sack-step)
     *
     * @since 3.0.0-incubating
    </E2> */
    fun <E2> sack(): GraphTraversal<S, E2>? {
        asAdmin().getBytecode().addStep(Symbols.sack)
        return asAdmin().addStep(SackStep(asAdmin()))
    }

    /**
     * If the [Traverser] supports looping then calling this method will extract the number of loops for that
     * traverser.
     *
     * @return the traversal with an appended [LoopsStep]
     * @see [Reference Documentation - Loops Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.loops-step)
     *
     * @since 3.1.0-incubating
     */
    fun loops(): GraphTraversal<S, Integer?>? {
        asAdmin().getBytecode().addStep(Symbols.loops)
        return asAdmin().addStep(LoopsStep(asAdmin(), null))
    }

    /**
     * If the [Traverser] supports looping then calling this method will extract the number of loops for that
     * traverser for the named loop.
     *
     * @return the traversal with an appended [LoopsStep]
     * @see [Reference Documentation - Loops Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.loops-step)
     *
     * @since 3.4.0
     */
    fun loops(loopName: String?): GraphTraversal<S, Integer?>? {
        asAdmin().getBytecode().addStep(Symbols.loops, loopName)
        return asAdmin().addStep(LoopsStep(asAdmin(), loopName))
    }

    /**
     * Projects the current object in the stream into a `Map` that is keyed by the provided labels.
     *
     * @return the traversal with an appended [ProjectStep]
     * @see [Reference Documentation - Project Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.project-step)
     *
     * @since 3.2.0-incubating
     */
    fun <E2> project(projectKey: String?, vararg otherProjectKeys: String?): GraphTraversal<S, Map<String?, E2>?>? {
        val projectKeys = arrayOfNulls<String>(otherProjectKeys.size + 1)
        projectKeys[0] = projectKey
        System.arraycopy(otherProjectKeys, 0, projectKeys, 1, otherProjectKeys.size)
        asAdmin().getBytecode().addStep(Symbols.project, projectKey, otherProjectKeys)
        return asAdmin().addStep(ProjectStep(asAdmin(), projectKeys))
    }

    /**
     * Map the [Traverser] to a [Map] projection of sideEffect values, map values, and/or path values.
     *
     * @param pop             if there are multiple objects referenced in the path, the [Pop] to use.
     * @param selectKey1      the first key to project
     * @param selectKey2      the second key to project
     * @param otherSelectKeys the third+ keys to project
     * @param <E2>            the type of the objects projected
     * @return the traversal with an appended [SelectStep].
     * @see [Reference Documentation - Select Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.select-step)
     *
     * @since 3.0.0-incubating
    </E2> */
    fun <E2> select(
        pop: Pop?,
        selectKey1: String?,
        selectKey2: String?,
        vararg otherSelectKeys: String?
    ): GraphTraversal<S, Map<String?, E2>?>? {
        val selectKeys = arrayOfNulls<String>(otherSelectKeys.size + 2)
        selectKeys[0] = selectKey1
        selectKeys[1] = selectKey2
        System.arraycopy(otherSelectKeys, 0, selectKeys, 2, otherSelectKeys.size)
        asAdmin().getBytecode().addStep(Symbols.select, pop, selectKey1, selectKey2, otherSelectKeys)
        return asAdmin().addStep(SelectStep(asAdmin(), pop, selectKeys))
    }

    /**
     * Map the [Traverser] to a [Map] projection of sideEffect values, map values, and/or path values.
     *
     * @param selectKey1      the first key to project
     * @param selectKey2      the second key to project
     * @param otherSelectKeys the third+ keys to project
     * @param <E2>            the type of the objects projected
     * @return the traversal with an appended [SelectStep].
     * @see [Reference Documentation - Select Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.select-step)
     *
     * @since 3.0.0-incubating
    </E2> */
    fun <E2> select(
        selectKey1: String?,
        selectKey2: String?,
        vararg otherSelectKeys: String?
    ): GraphTraversal<S, Map<String?, E2>?>? {
        val selectKeys = arrayOfNulls<String>(otherSelectKeys.size + 2)
        selectKeys[0] = selectKey1
        selectKeys[1] = selectKey2
        System.arraycopy(otherSelectKeys, 0, selectKeys, 2, otherSelectKeys.size)
        asAdmin().getBytecode().addStep(Symbols.select, selectKey1, selectKey2, otherSelectKeys)
        return asAdmin().addStep(SelectStep(asAdmin(), Pop.last, selectKeys))
    }

    /**
     * Map the [Traverser] to the object specified by the `selectKey` and apply the [Pop] operation
     * to it.
     *
     * @param selectKey the key to project
     * @return the traversal with an appended [SelectStep].
     * @see [Reference Documentation - Select Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.select-step)
     *
     * @since 3.0.0-incubating
     */
    fun <E2> select(pop: Pop?, selectKey: String?): GraphTraversal<S, E2>? {
        asAdmin().getBytecode().addStep(Symbols.select, pop, selectKey)
        return asAdmin().addStep(SelectOneStep(asAdmin(), pop, selectKey))
    }

    /**
     * Map the [Traverser] to the object specified by the `selectKey`. Note that unlike other uses of
     * `select` where there are multiple keys, this use of `select` with a single key does not produce a
     * `Map`.
     *
     * @param selectKey the key to project
     * @return the traversal with an appended [SelectStep].
     * @see [Reference Documentation - Select Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.select-step)
     *
     * @since 3.0.0-incubating
     */
    fun <E2> select(selectKey: String?): GraphTraversal<S, E2>? {
        asAdmin().getBytecode().addStep(Symbols.select, selectKey)
        return asAdmin().addStep(SelectOneStep(asAdmin(), Pop.last, selectKey))
    }

    /**
     * Map the [Traverser] to the object specified by the key returned by the `keyTraversal` and apply the [Pop] operation
     * to it.
     *
     * @param keyTraversal the traversal expression that selects the key to project
     * @return the traversal with an appended [SelectStep].
     * @see [Reference Documentation - Select Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.select-step)
     *
     * @since 3.3.3
     */
    fun <E2> select(pop: Pop?, keyTraversal: Traversal<S, E2>?): GraphTraversal<S, E2>? {
        asAdmin().getBytecode().addStep(Symbols.select, pop, keyTraversal)
        return asAdmin().addStep(TraversalSelectStep(asAdmin(), pop, keyTraversal))
    }

    /**
     * Map the [Traverser] to the object specified by the key returned by the `keyTraversal`. Note that unlike other uses of
     * `select` where there are multiple keys, this use of `select` with a traversal does not produce a
     * `Map`.
     *
     * @param keyTraversal the traversal expression that selects the key to project
     * @return the traversal with an appended [TraversalSelectStep].
     * @see [Reference Documentation - Select Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.select-step)
     *
     * @since 3.3.3
     */
    fun <E2> select(keyTraversal: Traversal<S, E2>?): GraphTraversal<S, E2>? {
        asAdmin().getBytecode().addStep(Symbols.select, keyTraversal)
        return asAdmin().addStep(TraversalSelectStep(asAdmin(), null, keyTraversal))
    }

    /**
     * A version of `select` that allows for the extraction of a [Column] from objects in the traversal.
     *
     * @param column the column to extract
     * @return the traversal with an appended [TraversalMapStep]
     * @see [Reference Documentation - Select Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.select-step)
     *
     * @since 3.1.0-incubating
     */
    fun <E2> select(column: Column?): GraphTraversal<S, Collection<E2>?>? {
        asAdmin().getBytecode().addStep(Symbols.select, column)
        return asAdmin().addStep(TraversalMapStep(asAdmin(), ColumnTraversal(column)))
    }

    /**
     * Unrolls a `Iterator`, `Iterable` or `Map` into a linear form or simply emits the object if it
     * is not one of those types.
     *
     * @return the traversal with an appended [UnfoldStep]
     * @see [Reference Documentation - Unfold Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.unfold-step)
     *
     * @since 3.0.0-incubating
     */
    fun <E2> unfold(): GraphTraversal<S, E2>? {
        asAdmin().getBytecode().addStep(Symbols.unfold)
        return asAdmin().addStep(UnfoldStep(asAdmin()))
    }

    /**
     * Rolls up objects in the stream into an aggregate list.
     *
     * @return the traversal with an appended [FoldStep]
     * @see [Reference Documentation - Fold Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.fold-step)
     *
     * @since 3.0.0-incubating
     */
    fun fold(): GraphTraversal<S, List<E>?>? {
        asAdmin().getBytecode().addStep(Symbols.fold)
        return asAdmin().addStep(FoldStep(asAdmin()))
    }

    /**
     * Rolls up objects in the stream into an aggregate value as defined by a `seed` and `BiFunction`.
     *
     * @param seed         the value to provide as the first argument to the `foldFunction`
     * @param foldFunction the function to fold by where the first argument is the `seed` or the value returned from subsequent class and
     * the second argument is the value from the stream
     * @return the traversal with an appended [FoldStep]
     * @see [Reference Documentation - Fold Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.fold-step)
     *
     * @since 3.0.0-incubating
     */
    fun <E2> fold(seed: E2, foldFunction: BiFunction<E2, E, E2>?): GraphTraversal<S, E2>? {
        asAdmin().getBytecode().addStep(Symbols.fold, seed, foldFunction)
        return asAdmin().addStep(FoldStep(asAdmin(), ConstantSupplier(seed), foldFunction))
    }

    /**
     * Map the traversal stream to its reduction as a sum of the [Traverser.bulk] values (i.e. count the number
     * of traversers up to this point).
     *
     * @return the traversal with an appended [CountGlobalStep].
     * @see [Reference Documentation - Count Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.count-step)
     *
     * @since 3.0.0-incubating
     */
    fun count(): GraphTraversal<S, Long?>? {
        asAdmin().getBytecode().addStep(Symbols.count)
        return asAdmin().addStep(CountGlobalStep(asAdmin()))
    }

    /**
     * Map the traversal stream to its reduction as a sum of the [Traverser.bulk] values given the specified
     * [Scope] (i.e. count the number of traversers up to this point).
     *
     * @return the traversal with an appended [CountGlobalStep] or [CountLocalStep] depending on the [Scope]
     * @see [Reference Documentation - Count Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.count-step)
     *
     * @since 3.0.0-incubating
     */
    fun count(scope: Scope): GraphTraversal<S, Long?>? {
        asAdmin().getBytecode().addStep(Symbols.count, scope)
        return asAdmin().addStep(if (scope.equals(Scope.global)) CountGlobalStep(asAdmin()) else CountLocalStep(asAdmin()))
    }

    /**
     * Map the traversal stream to its reduction as a sum of the [Traverser.get] values multiplied by their
     * [Traverser.bulk] (i.e. sum the traverser values up to this point).
     *
     * @return the traversal with an appended [SumGlobalStep].
     * @see [Reference Documentation - Sum Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.sum-step)
     *
     * @since 3.0.0-incubating
     */
    fun <E2 : Number?> sum(): GraphTraversal<S, E2>? {
        asAdmin().getBytecode().addStep(Symbols.sum)
        return asAdmin().addStep(SumGlobalStep(asAdmin()))
    }

    /**
     * Map the traversal stream to its reduction as a sum of the [Traverser.get] values multiplied by their
     * [Traverser.bulk] given the specified [Scope] (i.e. sum the traverser values up to this point).
     *
     * @return the traversal with an appended [SumGlobalStep] or [SumLocalStep] depending on the [Scope].
     * @see [Reference Documentation - Sum Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.sum-step)
     *
     * @since 3.0.0-incubating
     */
    fun <E2 : Number?> sum(scope: Scope): GraphTraversal<S, E2>? {
        asAdmin().getBytecode().addStep(Symbols.sum, scope)
        return asAdmin().addStep(if (scope.equals(Scope.global)) SumGlobalStep(asAdmin()) else SumLocalStep(asAdmin()))
    }

    /**
     * Determines the largest value in the stream.
     *
     * @return the traversal with an appended [MaxGlobalStep].
     * @see [Reference Documentation - Max Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.max-step)
     *
     * @since 3.0.0-incubating
     */
    fun <E2 : Comparable?> max(): GraphTraversal<S, E2>? {
        asAdmin().getBytecode().addStep(Symbols.max)
        return asAdmin().addStep(MaxGlobalStep(asAdmin()))
    }

    /**
     * Determines the largest value in the stream given the [Scope].
     *
     * @return the traversal with an appended [MaxGlobalStep] or [MaxLocalStep] depending on the [Scope]
     * @see [Reference Documentation - Max Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.max-step)
     *
     * @since 3.0.0-incubating
     */
    fun <E2 : Comparable?> max(scope: Scope): GraphTraversal<S, E2>? {
        asAdmin().getBytecode().addStep(Symbols.max, scope)
        return asAdmin().addStep(if (scope.equals(Scope.global)) MaxGlobalStep(asAdmin()) else MaxLocalStep(asAdmin()))
    }

    /**
     * Determines the smallest value in the stream.
     *
     * @return the traversal with an appended [MinGlobalStep].
     * @see [Reference Documentation - Min Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.min-step)
     *
     * @since 3.0.0-incubating
     */
    fun <E2 : Comparable?> min(): GraphTraversal<S, E2>? {
        asAdmin().getBytecode().addStep(Symbols.min)
        return asAdmin().addStep(MinGlobalStep(asAdmin()))
    }

    /**
     * Determines the smallest value in the stream given the [Scope].
     *
     * @return the traversal with an appended [MinGlobalStep] or [MinLocalStep] depending on the [Scope]
     * @see [Reference Documentation - Min Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.min-step)
     *
     * @since 3.0.0-incubating
     */
    fun <E2 : Comparable?> min(scope: Scope): GraphTraversal<S, E2>? {
        asAdmin().getBytecode().addStep(Symbols.min, scope)
        return asAdmin().addStep(if (scope.equals(Scope.global)) MinGlobalStep<E2>(asAdmin()) else MinLocalStep(asAdmin()))
    }

    /**
     * Determines the mean value in the stream.
     *
     * @return the traversal with an appended [MeanGlobalStep].
     * @see [Reference Documentation - Mean Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.mean-step)
     *
     * @since 3.0.0-incubating
     */
    fun <E2 : Number?> mean(): GraphTraversal<S, E2>? {
        asAdmin().getBytecode().addStep(Symbols.mean)
        return asAdmin().addStep(MeanGlobalStep(asAdmin()))
    }

    /**
     * Determines the mean value in the stream given the [Scope].
     *
     * @return the traversal with an appended [MeanGlobalStep] or [MeanLocalStep] depending on the [Scope]
     * @see [Reference Documentation - Mean Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.mean-step)
     *
     * @since 3.0.0-incubating
     */
    fun <E2 : Number?> mean(scope: Scope): GraphTraversal<S, E2>? {
        asAdmin().getBytecode().addStep(Symbols.mean, scope)
        return asAdmin().addStep(if (scope.equals(Scope.global)) MeanGlobalStep(asAdmin()) else MeanLocalStep(asAdmin()))
    }

    /**
     * Organize objects in the stream into a `Map`. Calls to `group()` are typically accompanied with
     * [.by] modulators which help specify how the grouping should occur.
     *
     * @return the traversal with an appended [GroupStep].
     * @see [Reference Documentation - Group Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.group-step)
     *
     * @since 3.1.0-incubating
     */
    fun <K, V> group(): GraphTraversal<S, Map<K, V>?>? {
        asAdmin().getBytecode().addStep(Symbols.group)
        return asAdmin().addStep(GroupStep(asAdmin()))
    }

    /**
     * Counts the number of times a particular objects has been part of a traversal, returning a `Map` where the
     * object is the key and the value is the count.
     *
     * @return the traversal with an appended [GroupCountStep].
     * @see [Reference Documentation - GroupCount Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.groupcount-step)
     *
     * @since 3.0.0-incubating
     */
    fun <K> groupCount(): GraphTraversal<S, Map<K, Long?>?>? {
        asAdmin().getBytecode().addStep(Symbols.groupCount)
        return asAdmin().addStep(GroupCountStep(asAdmin()))
    }

    /**
     * Aggregates the emanating paths into a [Tree] data structure.
     *
     * @return the traversal with an appended [TreeStep]
     * @see [Reference Documentation - Tree Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.tree-step)
     *
     * @since 3.0.0-incubating
     */
    fun tree(): GraphTraversal<S, Tree?>? {
        asAdmin().getBytecode().addStep(Symbols.tree)
        return asAdmin().addStep(TreeStep(asAdmin()))
    }

    /**
     * Adds a [Vertex].
     *
     * @param vertexLabel the label of the [Vertex] to add
     * @return the traversal with the [AddVertexStep] added
     * @see [Reference Documentation - AddVertex Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.addvertex-step)
     *
     * @since 3.1.0-incubating
     */
    fun addV(vertexLabel: String?): GraphTraversal<S, Vertex?>? {
        if (null == vertexLabel) throw IllegalArgumentException("vertexLabel cannot be null")
        asAdmin().getBytecode().addStep(Symbols.addV, vertexLabel)
        return asAdmin().addStep(AddVertexStep(asAdmin(), vertexLabel))
    }

    /**
     * Adds a [Vertex] with a vertex label determined by a [Traversal].
     *
     * @return the traversal with the [AddVertexStep] added
     * @see [Reference Documentation - AddVertex Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.addvertex-step)
     *
     * @since 3.3.1
     */
    fun addV(vertexLabelTraversal: Traversal<*, String?>?): GraphTraversal<S, Vertex?>? {
        if (null == vertexLabelTraversal) throw IllegalArgumentException("vertexLabelTraversal cannot be null")
        asAdmin().getBytecode().addStep(Symbols.addV, vertexLabelTraversal)
        return asAdmin().addStep(AddVertexStep(asAdmin(), vertexLabelTraversal.asAdmin()))
    }

    /**
     * Adds a [Vertex] with a default vertex label.
     *
     * @return the traversal with the [AddVertexStep] added
     * @see [Reference Documentation - AddVertex Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.addvertex-step)
     *
     * @since 3.1.0-incubating
     */
    fun addV(): GraphTraversal<S, Vertex?>? {
        asAdmin().getBytecode().addStep(Symbols.addV)
        return asAdmin().addStep(AddVertexStep(asAdmin(), null as String?))
    }

    /**
     * Adds an [Edge] with the specified edge label.
     *
     * @param edgeLabel the label of the newly added edge
     * @return the traversal with the [AddEdgeStep] added
     * @see [Reference Documentation - AddEdge Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.addedge-step)
     *
     * @since 3.1.0-incubating
     */
    fun addE(edgeLabel: String?): GraphTraversal<S, Edge?>? {
        asAdmin().getBytecode().addStep(Symbols.addE, edgeLabel)
        return asAdmin().addStep(AddEdgeStep(asAdmin(), edgeLabel))
    }

    /**
     * Adds a [Edge] with an edge label determined by a [Traversal].
     *
     * @return the traversal with the [AddEdgeStep] added
     * @see [Reference Documentation - AddEdge Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.addedge-step)
     *
     * @since 3.3.1
     */
    fun addE(edgeLabelTraversal: Traversal<*, String?>?): GraphTraversal<S, Edge?>? {
        asAdmin().getBytecode().addStep(Symbols.addE, edgeLabelTraversal)
        return asAdmin().addStep(
            AddEdgeStep(
                asAdmin(),
                if (null == edgeLabelTraversal) null else edgeLabelTraversal.asAdmin()
            )
        )
    }

    /**
     * Provide `to()`-modulation to respective steps.
     *
     * @param toStepLabel the step label to modulate to.
     * @return the traversal with the modified [FromToModulating] step.
     * @see [Reference Documentation - To Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.to-step)
     *
     * @since 3.1.0-incubating
     */
    fun to(toStepLabel: String?): GraphTraversal<S, E>? {
        val prev: Step<*, *> = asAdmin().getEndStep()
        if (prev !is FromToModulating) throw IllegalArgumentException(
            String.format(
                "The to() step cannot follow %s", prev.getClass().getSimpleName()
            )
        )
        asAdmin().getBytecode().addStep(Symbols.to, toStepLabel)
        (prev as FromToModulating).addTo(toStepLabel)
        return this
    }

    /**
     * Provide `from()`-modulation to respective steps.
     *
     * @param fromStepLabel the step label to modulate to.
     * @return the traversal with the modified [FromToModulating] step.
     * @see [Reference Documentation - From Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.from-step)
     *
     * @since 3.1.0-incubating
     */
    fun from(fromStepLabel: String?): GraphTraversal<S, E>? {
        val prev: Step<*, *> = asAdmin().getEndStep()
        if (prev !is FromToModulating) throw IllegalArgumentException(
            String.format(
                "The from() step cannot follow %s", prev.getClass().getSimpleName()
            )
        )
        asAdmin().getBytecode().addStep(Symbols.from, fromStepLabel)
        (prev as FromToModulating).addFrom(fromStepLabel)
        return this
    }

    /**
     * When used as a modifier to [.addE] this method specifies the traversal to use for selecting the
     * incoming vertex of the newly added [Edge].
     *
     * @param toVertex the traversal for selecting the incoming vertex
     * @return the traversal with the modified [AddEdgeStep]
     * @see [Reference Documentation - From Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.addedge-step)
     *
     * @since 3.1.0-incubating
     */
    fun to(toVertex: Traversal<*, Vertex?>): GraphTraversal<S, E>? {
        val prev: Step<*, *> = asAdmin().getEndStep()
        if (prev !is FromToModulating) throw IllegalArgumentException(
            String.format(
                "The to() step cannot follow %s", prev.getClass().getSimpleName()
            )
        )
        asAdmin().getBytecode().addStep(Symbols.to, toVertex)
        (prev as FromToModulating).addTo(toVertex.asAdmin())
        return this
    }

    /**
     * When used as a modifier to [.addE] this method specifies the traversal to use for selecting the
     * outgoing vertex of the newly added [Edge].
     *
     * @param fromVertex the traversal for selecting the outgoing vertex
     * @return the traversal with the modified [AddEdgeStep]
     * @see [Reference Documentation - From Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.addedge-step)
     *
     * @since 3.1.0-incubating
     */
    fun from(fromVertex: Traversal<*, Vertex?>): GraphTraversal<S, E>? {
        val prev: Step<*, *> = asAdmin().getEndStep()
        if (prev !is FromToModulating) throw IllegalArgumentException(
            String.format(
                "The from() step cannot follow %s", prev.getClass().getSimpleName()
            )
        )
        asAdmin().getBytecode().addStep(Symbols.from, fromVertex)
        (prev as FromToModulating).addFrom(fromVertex.asAdmin())
        return this
    }

    /**
     * When used as a modifier to [.addE] this method specifies the traversal to use for selecting the
     * incoming vertex of the newly added [Edge].
     *
     * @param toVertex the vertex for selecting the incoming vertex
     * @return the traversal with the modified [AddEdgeStep]
     * @see [Reference Documentation - From Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.addedge-step)
     *
     * @since 3.3.0
     */
    fun to(toVertex: Vertex?): GraphTraversal<S, E>? {
        val prev: Step<*, *> = asAdmin().getEndStep()
        if (prev !is FromToModulating) throw IllegalArgumentException(
            String.format(
                "The to() step cannot follow %s", prev.getClass().getSimpleName()
            )
        )
        asAdmin().getBytecode().addStep(Symbols.to, toVertex)
        (prev as FromToModulating).addTo(__.constant(toVertex).asAdmin())
        return this
    }

    /**
     * When used as a modifier to [.addE] this method specifies the traversal to use for selecting the
     * outgoing vertex of the newly added [Edge].
     *
     * @param fromVertex the vertex for selecting the outgoing vertex
     * @return the traversal with the modified [AddEdgeStep]
     * @see [Reference Documentation - From Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.addedge-step)
     *
     * @since 3.3.0
     */
    fun from(fromVertex: Vertex?): GraphTraversal<S, E>? {
        val prev: Step<*, *> = asAdmin().getEndStep()
        if (prev !is FromToModulating) throw IllegalArgumentException(
            String.format(
                "The from() step cannot follow %s", prev.getClass().getSimpleName()
            )
        )
        asAdmin().getBytecode().addStep(Symbols.from, fromVertex)
        (prev as FromToModulating).addFrom(__.constant(fromVertex).asAdmin())
        return this
    }

    /**
     * Map the [Traverser] to a [Double] according to the mathematical expression provided in the argument.
     *
     * @param expression the mathematical expression with variables refering to scope variables.
     * @return the traversal with the [MathStep] added.
     * @since 3.3.1
     */
    fun math(expression: String?): GraphTraversal<S, Double?>? {
        asAdmin().getBytecode().addStep(Symbols.math, expression)
        return asAdmin().addStep(MathStep(asAdmin(), expression))
    }
    ///////////////////// FILTER STEPS /////////////////////
    /**
     * Map the [Traverser] to either `true` or `false`, where `false` will not pass the
     * traverser to the next step.
     *
     * @param predicate the filter function to apply
     * @return the traversal with the [LambdaFilterStep] added
     * @see [Reference Documentation - General Steps](http://tinkerpop.apache.org/docs/${project.version}/reference/.general-steps)
     *
     * @since 3.0.0-incubating
     */
    fun filter(predicate: Predicate<Traverser<E>?>?): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.filter, predicate)
        return asAdmin().addStep(LambdaFilterStep(asAdmin(), predicate))
    }

    /**
     * Map the [Traverser] to either `true` or `false`, where `false` will not pass the
     * traverser to the next step.
     *
     * @param filterTraversal the filter traversal to apply
     * @return the traversal with the [TraversalFilterStep] added
     * @see [Reference Documentation - General Steps](http://tinkerpop.apache.org/docs/${project.version}/reference/.general-steps)
     *
     * @since 3.0.0-incubating
     */
    fun filter(filterTraversal: Traversal<*, *>?): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.filter, filterTraversal)
        return asAdmin().addStep(TraversalFilterStep(asAdmin(), filterTraversal as Traversal?))
    }

    /**
     * Filter all traversers in the traversal. This step has narrow use cases and is primarily intended for use as a
     * signal to remote servers that [.iterate] was called. While it may be directly used, it is often a sign
     * that a traversal should be re-written in another form.
     *
     * @return the updated traversal with respective [NoneStep].
     */
    @Override
    fun none(): GraphTraversal<S, E>? {
        return super@Traversal.none()
    }

    /**
     * Ensures that at least one of the provided traversals yield a result.
     *
     * @param orTraversals filter traversals where at least one must be satisfied
     * @return the traversal with an appended [OrStep]
     * @see [Reference Documentation - Or Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.or-step)
     *
     * @since 3.0.0-incubating
     */
    fun or(vararg orTraversals: Traversal<*, *>?): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.or, orTraversals)
        return asAdmin().addStep(OrStep(asAdmin(), orTraversals))
    }

    /**
     * Ensures that all of the provided traversals yield a result.
     *
     * @param andTraversals filter traversals that must be satisfied
     * @return the traversal with an appended [AndStep]
     * @see [Reference Documentation - And Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.and-step)
     *
     * @since 3.0.0-incubating
     */
    fun and(vararg andTraversals: Traversal<*, *>?): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.and, andTraversals)
        return asAdmin().addStep(AndStep(asAdmin(), andTraversals))
    }

    /**
     * Provides a way to add arbitrary objects to a traversal stream.
     *
     * @param injections the objects to add to the stream
     * @return the traversal with an appended [InjectStep]
     * @see [Reference Documentation - Inject Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.inject-step)
     *
     * @since 3.0.0-incubating
     */
    fun inject(vararg injections: E): GraphTraversal<S, E>? {
        // a single null is [null]
        val s: Array<E?> = injections ?: arrayOf(null) as Array<E?>
        asAdmin().getBytecode().addStep(Symbols.inject, s)
        return asAdmin().addStep(InjectStep(asAdmin(), s))
    }

    /**
     * Remove all duplicates in the traversal stream up to this point.
     *
     * @param scope       whether the deduplication is on the stream (global) or the current object (local).
     * @param dedupLabels if labels are provided, then the scope labels determine de-duplication. No labels implies current object.
     * @return the traversal with an appended [DedupGlobalStep] or [DedupLocalStep] depending on `scope`
     * @see [Reference Documentation - Dedup Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.dedup-step)
     *
     * @since 3.0.0-incubating
     */
    fun dedup(scope: Scope, vararg dedupLabels: String?): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.dedup, scope, dedupLabels)
        return asAdmin().addStep(
            if (scope.equals(Scope.global)) DedupGlobalStep(asAdmin(), dedupLabels) else DedupLocalStep(
                asAdmin()
            )
        )
    }

    /**
     * Remove all duplicates in the traversal stream up to this point.
     *
     * @param dedupLabels if labels are provided, then the scoped object's labels determine de-duplication. No labels implies current object.
     * @return the traversal with an appended [DedupGlobalStep].
     * @see [Reference Documentation - Dedup Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.dedup-step)
     *
     * @since 3.0.0-incubating
     */
    fun dedup(vararg dedupLabels: String?): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.dedup, dedupLabels)
        return asAdmin().addStep(DedupGlobalStep(asAdmin(), dedupLabels))
    }

    /**
     * Filters the current object based on the object itself or the path history.
     *
     * @param startKey  the key containing the object to filter
     * @param predicate the filter to apply
     * @return the traversal with an appended [WherePredicateStep]
     * @see [Reference Documentation - Where Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.where-step)
     *
     * @see [Reference Documentation - Where with Match](http://tinkerpop.apache.org/docs/${project.version}/reference/.using-where-with-match)
     *
     * @see [Reference Documentation - Where with Select](http://tinkerpop.apache.org/docs/${project.version}/reference/.using-where-with-select)
     *
     * @since 3.0.0-incubating
     */
    fun where(startKey: String?, predicate: P<String?>?): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.where, startKey, predicate)
        return asAdmin().addStep(WherePredicateStep(asAdmin(), Optional.ofNullable(startKey), predicate))
    }

    /**
     * Filters the current object based on the object itself or the path history.
     *
     * @param predicate the filter to apply
     * @return the traversal with an appended [WherePredicateStep]
     * @see [Reference Documentation - Where Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.where-step)
     *
     * @see [Reference Documentation - Where with Match](http://tinkerpop.apache.org/docs/${project.version}/reference/.using-where-with-match)
     *
     * @see [Reference Documentation - Where with Select](http://tinkerpop.apache.org/docs/${project.version}/reference/.using-where-with-select)
     *
     * @since 3.0.0-incubating
     */
    fun where(predicate: P<String?>?): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.where, predicate)
        return asAdmin().addStep(WherePredicateStep(asAdmin(), Optional.empty(), predicate))
    }

    /**
     * Filters the current object based on the object itself or the path history.
     *
     * @param whereTraversal the filter to apply
     * @return the traversal with an appended [WherePredicateStep]
     * @see [Reference Documentation - Where Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.where-step)
     *
     * @see [Reference Documentation - Where with Match](http://tinkerpop.apache.org/docs/${project.version}/reference/.using-where-with-match)
     *
     * @see [Reference Documentation - Where with Select](http://tinkerpop.apache.org/docs/${project.version}/reference/.using-where-with-select)
     *
     * @since 3.0.0-incubating
     */
    fun where(whereTraversal: Traversal<*, *>): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.where, whereTraversal)
        return if (TraversalHelper.getVariableLocations(whereTraversal.asAdmin()).isEmpty()) asAdmin().addStep(
            TraversalFilterStep(
                asAdmin(), whereTraversal as Traversal
            )
        ) else asAdmin().addStep(WhereTraversalStep(asAdmin(), whereTraversal))
    }

    /**
     * Filters vertices, edges and vertex properties based on their properties.
     *
     * @param propertyKey the key of the property to filter on
     * @param predicate   the filter to apply to the key's value
     * @return the traversal with an appended [HasStep]
     * @see [Reference Documentation - Has Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.has-step)
     *
     * @since 3.0.0-incubating
     */
    fun has(propertyKey: String?, predicate: P<*>?): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.has, propertyKey, predicate)
        return TraversalHelper.addHasContainer(asAdmin(), HasContainer(propertyKey, predicate))
    }

    /**
     * Filters vertices, edges and vertex properties based on their properties.
     *
     * @param accessor  the [T] accessor of the property to filter on
     * @param predicate the filter to apply to the key's value
     * @return the traversal with an appended [HasStep]
     * @see [Reference Documentation - Has Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.has-step)
     *
     * @since 3.0.0-incubating
     */
    fun has(accessor: T?, predicate: P<*>?): GraphTraversal<S, E>? {
        if (null == accessor) throw IllegalArgumentException("The T accessor value of has(T,Object) cannot be null")

        // Groovy can get the overload wrong for has(T, null) which should probably go at has(T,Object). users could
        // explicit cast but a redirect here makes this a bit more seamless
        if (null == predicate) return has(accessor, null as Object?)
        asAdmin().getBytecode().addStep(Symbols.has, accessor, predicate)
        return TraversalHelper.addHasContainer(asAdmin(), HasContainer(accessor.getAccessor(), predicate))
    }

    /**
     * Filters vertices, edges and vertex properties based on their properties.
     *
     * @param propertyKey the key of the property to filter on
     * @param value       the value to compare the property value to for equality
     * @return the traversal with an appended [HasStep]
     * @see [Reference Documentation - Has Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.has-step)
     *
     * @since 3.0.0-incubating
     */
    fun has(propertyKey: String?, value: Object?): GraphTraversal<S, E>? {
        return if (value is P) this.has(propertyKey, value as P?) else if (value is Traversal) this.has(
            propertyKey,
            value as Traversal?
        ) else {
            asAdmin().getBytecode().addStep(
                Symbols.has,
                propertyKey,
                value
            )
            TraversalHelper.addHasContainer(asAdmin(), HasContainer(propertyKey, P.eq(value)))
        }
    }

    /**
     * Filters vertices, edges and vertex properties based on their properties.
     *
     * @param accessor the [T] accessor of the property to filter on
     * @param value    the value to compare the accessor value to for equality
     * @return the traversal with an appended [HasStep]
     * @see [Reference Documentation - Has Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.has-step)
     *
     * @since 3.0.0-incubating
     */
    fun has(accessor: T?, value: Object?): GraphTraversal<S, E>? {
        if (null == accessor) throw IllegalArgumentException("The T accessor value of has(T,Object) cannot be null")
        return if (value is P) this.has(accessor, value as P?) else if (value is Traversal) this.has(
            accessor,
            value as Traversal?
        ) else {
            asAdmin().getBytecode().addStep(
                Symbols.has,
                accessor,
                value
            )
            TraversalHelper.addHasContainer(asAdmin(), HasContainer(accessor.getAccessor(), P.eq(value)))
        }
    }

    /**
     * Filters vertices, edges and vertex properties based on their properties.
     *
     * @param label       the label of the [Element]
     * @param propertyKey the key of the property to filter on
     * @param predicate   the filter to apply to the key's value
     * @return the traversal with an appended [HasStep]
     * @see [Reference Documentation - Has Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.has-step)
     *
     * @since 3.0.0-incubating
     */
    fun has(label: String?, propertyKey: String?, predicate: P<*>?): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.has, label, propertyKey, predicate)
        TraversalHelper.addHasContainer(asAdmin(), HasContainer(T.label.getAccessor(), P.eq(label)))
        return TraversalHelper.addHasContainer(asAdmin(), HasContainer(propertyKey, predicate))
    }

    /**
     * Filters vertices, edges and vertex properties based on their properties.
     *
     * @param label       the label of the [Element]
     * @param propertyKey the key of the property to filter on
     * @param value       the value to compare the accessor value to for equality
     * @return the traversal with an appended [HasStep]
     * @see [Reference Documentation - Has Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.has-step)
     *
     * @since 3.0.0-incubating
     */
    fun has(label: String?, propertyKey: String?, value: Object?): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.has, label, propertyKey, value)
        TraversalHelper.addHasContainer(asAdmin(), HasContainer(T.label.getAccessor(), P.eq(label)))
        return TraversalHelper.addHasContainer(
            asAdmin(),
            HasContainer(propertyKey, if (value is P) value as P? else P.eq(value))
        )
    }

    /**
     * Filters vertices, edges and vertex properties based on their value of [T] where only [T.id] and
     * [T.label] are supported.
     *
     * @param accessor          the [T] accessor of the property to filter on
     * @param propertyTraversal the traversal to filter the accessor value by
     * @return the traversal with an appended [HasStep]
     * @see [Reference Documentation - Has Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.has-step)
     *
     * @since 3.1.0-incubating
     */
    fun has(accessor: T?, propertyTraversal: Traversal<*, *>?): GraphTraversal<S, E>? {
        if (null == accessor) throw IllegalArgumentException("The T accessor value of has(T,Object) cannot be null")

        // Groovy can get the overload wrong for has(T, null) which should probably go at has(T,Object). users could
        // explicit cast but a redirect here makes this a bit more seamless
        if (null == propertyTraversal) return has(accessor, null as Object?)
        asAdmin().getBytecode().addStep(Symbols.has, accessor, propertyTraversal)
        return when (accessor) {
            id -> asAdmin().addStep(
                TraversalFilterStep(
                    asAdmin(), propertyTraversal.asAdmin().addStep(
                        0,
                        IdStep(propertyTraversal.asAdmin())
                    )
                )
            )
            label -> asAdmin().addStep(
                TraversalFilterStep(
                    asAdmin(), propertyTraversal.asAdmin().addStep(
                        0,
                        LabelStep(propertyTraversal.asAdmin())
                    )
                )
            )
            else -> throw IllegalArgumentException("has(T,Traversal) can only take id or label as its argument")
        }
    }

    /**
     * Filters vertices, edges and vertex properties based on the value of the specified property key.
     *
     * @param propertyKey       the key of the property to filter on
     * @param propertyTraversal the traversal to filter the property value by
     * @return the traversal with an appended [HasStep]
     * @see [Reference Documentation - Has Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.has-step)
     *
     * @since 3.0.0-incubating
     */
    fun has(propertyKey: String?, propertyTraversal: Traversal<*, *>): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.has, propertyKey, propertyTraversal)
        return asAdmin().addStep(
            TraversalFilterStep(
                asAdmin(), propertyTraversal.asAdmin().addStep(
                    0,
                    PropertiesStep(propertyTraversal.asAdmin(), PropertyType.VALUE, propertyKey)
                )
            )
        )
    }

    /**
     * Filters vertices, edges and vertex properties based on the existence of properties.
     *
     * @param propertyKey the key of the property to filter on for existence
     * @return the traversal with an appended [HasStep]
     * @see [Reference Documentation - Has Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.has-step)
     *
     * @since 3.0.0-incubating
     */
    fun has(propertyKey: String?): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.has, propertyKey)
        return asAdmin().addStep(TraversalFilterStep(asAdmin(), __.values(propertyKey)))
    }

    /**
     * Filters vertices, edges and vertex properties based on the non-existence of properties.
     *
     * @param propertyKey the key of the property to filter on for existence
     * @return the traversal with an appended [HasStep]
     * @see [Reference Documentation - Has Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.has-step)
     *
     * @since 3.0.0-incubating
     */
    fun hasNot(propertyKey: String?): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.hasNot, propertyKey)
        return asAdmin().addStep(NotStep(asAdmin(), __.values(propertyKey)))
    }

    /**
     * Filters vertices, edges and vertex properties based on their label.
     *
     * @param label       the label of the [Element]
     * @param otherLabels additional labels of the [Element]
     * @return the traversal with an appended [HasStep]
     * @see [Reference Documentation - Has Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.has-step)
     *
     * @since 3.2.2
     */
    fun hasLabel(label: String?, vararg otherLabels: String?): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.hasLabel, label, otherLabels)

        // groovy evaluation seems to do strange things with varargs given hasLabel(null, null). odd someone would
        // do this but the failure is ugly if not handled.
        val otherLabelsLength = otherLabels?.size ?: 0
        val labels = arrayOfNulls<String>(otherLabelsLength + 1)
        labels[0] = label
        if (otherLabelsLength > 0) System.arraycopy(otherLabels, 0, labels, 1, otherLabelsLength)
        return TraversalHelper.addHasContainer(
            asAdmin(), HasContainer(
                T.label.getAccessor(), if (labels.size == 1) P.eq(
                    labels[0]
                ) else P.within(labels)
            )
        )
    }

    /**
     * Filters vertices, edges and vertex properties based on their label. Note that calling this step with
     * `null` is the same as calling [.hasLabel] with a single `null`.
     *
     * @param predicate the filter to apply to the label of the [Element]
     * @return the traversal with an appended [HasStep]
     * @see [Reference Documentation - Has Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.has-step)
     *
     * @since 3.2.4
     */
    fun hasLabel(predicate: P<String?>?): GraphTraversal<S, E>? {
        // if calling hasLabel(null), the likely use the caller is going for is not a "no predicate" but a eq(null)
        return if (null == predicate) {
            hasLabel(null as String?)
        } else {
            asAdmin().getBytecode().addStep(
                Symbols.hasLabel,
                predicate
            )
            TraversalHelper.addHasContainer(asAdmin(), HasContainer(T.label.getAccessor(), predicate))
        }
    }

    /**
     * Filters vertices, edges and vertex properties based on their identifier.
     *
     * @param id       the identifier of the [Element]
     * @param otherIds additional identifiers of the [Element]
     * @return the traversal with an appended [HasStep]
     * @see [Reference Documentation - Has Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.has-step)
     *
     * @since 3.2.2
     */
    fun hasId(id: Object, vararg otherIds: Object?): GraphTraversal<S, E>? {
        return if (id is P) {
            this.hasId(id as P)
        } else {
            var ids: Array<Object?>
            if (id is Array<Object>) {
                ids = id
            } else {
                ids = arrayOf<Object?>(id)
            }
            var size = ids.size
            var capacity = size
            for (i in otherIds) {
                if (i != null && i.getClass().isArray()) {
                    val tmp: Array<Object> = i
                    val newLength = size + tmp.size
                    if (capacity < newLength) {
                        ids = Arrays.copyOf(ids, size + tmp.size.also { capacity = it })
                    }
                    System.arraycopy(tmp, 0, ids, size, tmp.size)
                    size = newLength
                } else {
                    if (capacity == size) {
                        ids = Arrays.copyOf(ids, size * 2.also { capacity = it })
                    }
                    ids[size++] = i
                }
            }
            if (capacity > size) {
                ids = Arrays.copyOf(ids, size)
            }
            asAdmin().getBytecode().addStep(Symbols.hasId, ids)
            TraversalHelper.addHasContainer(
                asAdmin(),
                HasContainer(T.id.getAccessor(), if (ids.size == 1) P.eq(ids[0]) else P.within(ids))
            )
        }
    }

    /**
     * Filters vertices, edges and vertex properties based on their identifier. Calling this step with a `null`
     * value will result in effectively calling [.hasId] wit a single `null` identifier
     * and therefore filter all results since a [T.id] cannot be `null`.
     *
     * @param predicate the filter to apply to the identifier of the [Element]
     * @return the traversal with an appended [HasStep]
     * @see [Reference Documentation - Has Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.has-step)
     *
     * @since 3.2.4
     */
    fun hasId(predicate: P<Object?>?): GraphTraversal<S, E>? {
        if (null == predicate) return hasId(null as Object?)
        asAdmin().getBytecode().addStep(Symbols.hasId, predicate)
        return TraversalHelper.addHasContainer(asAdmin(), HasContainer(T.id.getAccessor(), predicate))
    }

    /**
     * Filters [Property] objects based on their key. It is not meant to test key existence on an [Edge] or
     * a [Vertex]. In that case, prefer [.has].
     *
     * @param label       the key of the [Property]
     * @param otherLabels additional key of the [Property]
     * @return the traversal with an appended [HasStep]
     * @see [Reference Documentation - Has Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.has-step)
     *
     * @since 3.2.2
     */
    fun hasKey(label: String?, vararg otherLabels: String?): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.hasKey, label, otherLabels)

        // groovy evaluation seems to do strange things with varargs given hasLabel(null, null). odd someone would
        // do this but the failure is ugly if not handled.
        val otherLabelsLength = otherLabels?.size ?: 0
        val labels = arrayOfNulls<String>(otherLabelsLength + 1)
        labels[0] = label
        if (otherLabelsLength > 0) System.arraycopy(otherLabels, 0, labels, 1, otherLabelsLength)
        return TraversalHelper.addHasContainer(
            asAdmin(), HasContainer(
                T.key.getAccessor(), if (labels.size == 1) P.eq(
                    labels[0]
                ) else P.within(labels)
            )
        )
    }

    /**
     * Filters [Property] objects based on their key. It is not meant to test key existence on an [Edge] or
     * a [Vertex]. In that case, prefer [.has]. Note that calling this step with `null` is
     * the same as calling [.hasKey] with a single `null`.
     *
     * @param predicate the filter to apply to the key of the [Property]
     * @return the traversal with an appended [HasStep]
     * @see [Reference Documentation - Has Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.has-step)
     *
     * @since 3.2.4
     */
    fun hasKey(predicate: P<String?>?): GraphTraversal<S, E>? {
        // if calling hasKey(null), the likely use the caller is going for is not a "no predicate" but a eq(null)
        return if (null == predicate) {
            hasKey(null as String?)
        } else {
            asAdmin().getBytecode()
                .addStep(Symbols.hasKey, predicate)
            TraversalHelper.addHasContainer(asAdmin(), HasContainer(T.key.getAccessor(), predicate))
        }
    }

    /**
     * Filters [Property] objects based on their value.
     *
     * @param value       the value of the [Element]
     * @param otherValues additional values of the [Element]
     * @return the traversal with an appended [HasStep]
     * @see [Reference Documentation - Has Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.has-step)
     */
    fun hasValue(value: Object?, vararg otherValues: Object?): GraphTraversal<S, E>? {
        return if (value is P) this.hasValue(value as P?) else {
            asAdmin().getBytecode().addStep(Symbols.hasValue, value, otherValues)
            val values: List<Object> = ArrayList()
            if (value is Array<Object>) {
                Collections.addAll(values, value as Array<Object?>?)
            } else {
                values.add(value)
            }
            if (null == otherValues) {
                values.add(null)
            } else {
                for (v in otherValues) {
                    if (v is Array<Object>) {
                        Collections.addAll(values, v as Array<Object?>?)
                    } else values.add(v)
                }
            }
            TraversalHelper.addHasContainer(
                asAdmin(), HasContainer(
                    T.value.getAccessor(), if (values.size() === 1) P.eq(
                        values[0]
                    ) else P.within(values)
                )
            )
        }
    }

    /**
     * Filters [Property] objects based on their value.Note that calling this step with `null` is the same
     * as calling [.hasValue] with a single `null`.
     *
     * @param predicate the filter to apply to the value of the [Element]
     * @return the traversal with an appended [HasStep]
     * @see [Reference Documentation - Has Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.has-step)
     *
     * @since 3.2.4
     */
    fun hasValue(predicate: P<Object?>?): GraphTraversal<S, E>? {
        // if calling hasValue(null), the likely use the caller is going for is not a "no predicate" but a eq(null)
        return if (null == predicate) {
            hasValue(null as String?)
        } else {
            asAdmin().getBytecode().addStep(
                Symbols.hasValue,
                predicate
            )
            TraversalHelper.addHasContainer(asAdmin(), HasContainer(T.value.getAccessor(), predicate))
        }
    }

    /**
     * Filters `E` object values given the provided `predicate`.
     *
     * @param predicate the filter to apply
     * @return the traversal with an appended [IsStep]
     * @see [Reference Documentation - Is Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.is-step)
     *
     * @since 3.0.0-incubating
     */
    fun `is`(predicate: P<E>?): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.`is`, predicate)
        return asAdmin().addStep(IsStep(asAdmin(), predicate))
    }

    /**
     * Filter the `E` object if it is not [P.eq] to the provided value.
     *
     * @param value the value that the object must equal.
     * @return the traversal with an appended [IsStep].
     * @see [Reference Documentation - Is Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.is-step)
     *
     * @since 3.0.0-incubating
     */
    fun `is`(value: Object?): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.`is`, value)
        return asAdmin().addStep(IsStep(asAdmin(), if (value is P) value as P<E>? else P.eq(value as E?)))
    }

    /**
     * Removes objects from the traversal stream when the traversal provided as an argument does not return any objects.
     *
     * @param notTraversal the traversal to filter by.
     * @return the traversal with an appended [NotStep].
     * @see [Reference Documentation - Not Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.not-step)
     *
     * @since 3.0.0-incubating
     */
    fun not(notTraversal: Traversal<*, *>?): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.not, notTraversal)
        return asAdmin().addStep(NotStep(asAdmin(), notTraversal as Traversal<E, *>?))
    }

    /**
     * Filter the `E` object given a biased coin toss.
     *
     * @param probability the probability that the object will pass through
     * @return the traversal with an appended [CoinStep].
     * @see [Reference Documentation - Coin Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.coin-step)
     *
     * @since 3.0.0-incubating
     */
    fun coin(probability: Double): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.coin, probability)
        return asAdmin().addStep(CoinStep(asAdmin(), probability))
    }

    /**
     * Filter the objects in the traversal by the number of them to pass through the stream. Those before the value
     * of `low` do not pass through and those that exceed the value of `high` will end the iteration.
     *
     * @param low  the number at which to start allowing objects through the stream
     * @param high the number at which to end the stream - use `-1` to emit all remaining objects
     * @return the traversal with an appended [RangeGlobalStep]
     * @see [Reference Documentation - Range Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.range-step)
     *
     * @since 3.0.0-incubating
     */
    fun range(low: Long, high: Long): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.range, low, high)
        return asAdmin().addStep(RangeGlobalStep(asAdmin(), low, high))
    }

    /**
     * Filter the objects in the traversal by the number of them to pass through the stream as constrained by the
     * [Scope]. Those before the value of `low` do not pass through and those that exceed the value of
     * `high` will end the iteration.
     *
     * @param scope the scope of how to apply the `range`
     * @param low   the number at which to start allowing objects through the stream
     * @param high  the number at which to end the stream - use `-1` to emit all remaining objects
     * @return the traversal with an appended [RangeGlobalStep] or [RangeLocalStep] depending on `scope`
     * @see [Reference Documentation - Range Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.range-step)
     *
     * @since 3.0.0-incubating
     */
    fun <E2> range(scope: Scope, low: Long, high: Long): GraphTraversal<S, E2>? {
        asAdmin().getBytecode().addStep(Symbols.range, scope, low, high)
        return asAdmin().addStep(
            if (scope.equals(Scope.global)) RangeGlobalStep(asAdmin(), low, high) else RangeLocalStep(
                asAdmin(), low, high
            )
        )
    }

    /**
     * Filter the objects in the traversal by the number of them to pass through the stream, where only the first
     * `n` objects are allowed as defined by the `limit` argument.
     *
     * @param limit the number at which to end the stream
     * @return the traversal with an appended [RangeGlobalStep]
     * @see [Reference Documentation - Limit Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.limit-step)
     *
     * @since 3.0.0-incubating
     */
    fun limit(limit: Long): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.limit, limit)
        return asAdmin().addStep(RangeGlobalStep(asAdmin(), 0, limit))
    }

    /**
     * Filter the objects in the traversal by the number of them to pass through the stream given the [Scope],
     * where only the first `n` objects are allowed as defined by the `limit` argument.
     *
     * @param scope the scope of how to apply the `limit`
     * @param limit the number at which to end the stream
     * @return the traversal with an appended [RangeGlobalStep] or [RangeLocalStep] depending on `scope`
     * @see [Reference Documentation - Limit Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.limit-step)
     *
     * @since 3.0.0-incubating
     */
    fun <E2> limit(scope: Scope, limit: Long): GraphTraversal<S, E2>? {
        asAdmin().getBytecode().addStep(Symbols.limit, scope, limit)
        return asAdmin().addStep(
            if (scope.equals(Scope.global)) RangeGlobalStep(asAdmin(), 0, limit) else RangeLocalStep(
                asAdmin(), 0, limit
            )
        )
    }

    /**
     * Filters the objects in the traversal emitted as being last objects in the stream. In this case, only the last
     * object will be returned.
     *
     * @return the traversal with an appended [TailGlobalStep]
     * @see [Reference Documentation - Tail Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.tail-step)
     *
     * @since 3.0.0-incubating
     */
    fun tail(): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.tail)
        return asAdmin().addStep(TailGlobalStep(asAdmin(), 1))
    }

    /**
     * Filters the objects in the traversal emitted as being last objects in the stream. In this case, only the last
     * `n` objects will be returned as defined by the `limit`.
     *
     * @param limit the number at which to end the stream
     * @return the traversal with an appended [TailGlobalStep]
     * @see [Reference Documentation - Tail Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.tail-step)
     *
     * @since 3.0.0-incubating
     */
    fun tail(limit: Long): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.tail, limit)
        return asAdmin().addStep(TailGlobalStep(asAdmin(), limit))
    }

    /**
     * Filters the objects in the traversal emitted as being last objects in the stream given the [Scope]. In
     * this case, only the last object in the stream will be returned.
     *
     * @param scope the scope of how to apply the `tail`
     * @return the traversal with an appended [TailGlobalStep] or [TailLocalStep] depending on `scope`
     * @see [Reference Documentation - Tail Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.tail-step)
     *
     * @since 3.0.0-incubating
     */
    fun <E2> tail(scope: Scope): GraphTraversal<S, E2>? {
        asAdmin().getBytecode().addStep(Symbols.tail, scope)
        return asAdmin().addStep(
            if (scope.equals(Scope.global)) TailGlobalStep(
                asAdmin(),
                1
            ) else TailLocalStep(asAdmin(), 1)
        )
    }

    /**
     * Filters the objects in the traversal emitted as being last objects in the stream given the [Scope]. In
     * this case, only the last `n` objects will be returned as defined by the `limit`.
     *
     * @param scope the scope of how to apply the `tail`
     * @param limit the number at which to end the stream
     * @return the traversal with an appended [TailGlobalStep] or [TailLocalStep] depending on `scope`
     * @see [Reference Documentation - Tail Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.tail-step)
     *
     * @since 3.0.0-incubating
     */
    fun <E2> tail(scope: Scope, limit: Long): GraphTraversal<S, E2>? {
        asAdmin().getBytecode().addStep(Symbols.tail, scope, limit)
        return asAdmin().addStep(
            if (scope.equals(Scope.global)) TailGlobalStep(asAdmin(), limit) else TailLocalStep(
                asAdmin(), limit
            )
        )
    }

    /**
     * Filters out the first `n` objects in the traversal.
     *
     * @param skip the number of objects to skip
     * @return the traversal with an appended [RangeGlobalStep]
     * @see [Reference Documentation - Skip Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.skip-step)
     *
     * @since 3.3.0
     */
    fun skip(skip: Long): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.skip, skip)
        return asAdmin().addStep(RangeGlobalStep(asAdmin(), skip, -1))
    }

    /**
     * Filters out the first `n` objects in the traversal.
     *
     * @param scope the scope of how to apply the `tail`
     * @param skip  the number of objects to skip
     * @return the traversal with an appended [RangeGlobalStep] or [RangeLocalStep] depending on `scope`
     * @see [Reference Documentation - Skip Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.skip-step)
     *
     * @since 3.3.0
     */
    fun <E2> skip(scope: Scope, skip: Long): GraphTraversal<S, E2>? {
        asAdmin().getBytecode().addStep(Symbols.skip, scope, skip)
        return asAdmin().addStep(
            if (scope.equals(Scope.global)) RangeGlobalStep(asAdmin(), skip, -1) else RangeLocalStep(
                asAdmin(), skip, -1
            )
        )
    }

    /**
     * Once the first [Traverser] hits this step, a count down is started. Once the time limit is up, all remaining traversers are filtered out.
     *
     * @param timeLimit the count down time
     * @return the traversal with an appended [TimeLimitStep]
     * @see [Reference Documentation - TimeLimit Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.timelimit-step)
     *
     * @since 3.0.0-incubating
     */
    fun timeLimit(timeLimit: Long): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.timeLimit, timeLimit)
        return asAdmin().addStep(TimeLimitStep<E>(asAdmin(), timeLimit))
    }

    /**
     * Filter the `E` object if its [Traverser.path] is not [Path.isSimple].
     *
     * @return the traversal with an appended [PathFilterStep].
     * @see [Reference Documentation - SimplePath Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.simplepath-step)
     *
     * @since 3.0.0-incubating
     */
    fun simplePath(): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.simplePath)
        return asAdmin().addStep(PathFilterStep<E>(asAdmin(), true))
    }

    /**
     * Filter the `E` object if its [Traverser.path] is [Path.isSimple].
     *
     * @return the traversal with an appended [PathFilterStep].
     * @see [Reference Documentation - CyclicPath Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.cyclicpath-step)
     *
     * @since 3.0.0-incubating
     */
    fun cyclicPath(): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.cyclicPath)
        return asAdmin().addStep(PathFilterStep<E>(asAdmin(), false))
    }

    /**
     * Allow some specified number of objects to pass through the stream.
     *
     * @param amountToSample the number of objects to allow
     * @return the traversal with an appended [SampleGlobalStep]
     * @see [Reference Documentation - Sample Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.sample-step)
     *
     * @since 3.0.0-incubating
     */
    fun sample(amountToSample: Int): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.sample, amountToSample)
        return asAdmin().addStep(SampleGlobalStep(asAdmin(), amountToSample))
    }

    /**
     * Allow some specified number of objects to pass through the stream.
     *
     * @param scope          the scope of how to apply the `sample`
     * @param amountToSample the number of objects to allow
     * @return the traversal with an appended [SampleGlobalStep] or [SampleLocalStep] depending on the `scope`
     * @see [Reference Documentation - Sample Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.sample-step)
     *
     * @since 3.0.0-incubating
     */
    fun sample(scope: Scope, amountToSample: Int): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.sample, scope, amountToSample)
        return asAdmin().addStep(
            if (scope.equals(Scope.global)) SampleGlobalStep(asAdmin(), amountToSample) else SampleLocalStep(
                asAdmin(), amountToSample
            )
        )
    }

    /**
     * Removes elements and properties from the graph. This step is not a terminating, in the sense that it does not
     * automatically iterate the traversal. It is therefore necessary to do some form of iteration for the removal
     * to actually take place. In most cases, iteration is best accomplished with `g.V().drop().iterate()`.
     *
     * @return the traversal with the [DropStep] added
     * @see [Reference Documentation - Drop Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.drop-step)
     *
     * @since 3.0.0-incubating
     */
    fun drop(): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.drop)
        return asAdmin().addStep(DropStep(asAdmin()))
    }
    ///////////////////// SIDE-EFFECT STEPS /////////////////////
    /**
     * Perform some operation on the [Traverser] and pass it to the next step unmodified.
     *
     * @param consumer the operation to perform at this step in relation to the [Traverser]
     * @return the traversal with an appended [LambdaSideEffectStep]
     * @see [Reference Documentation - General Steps](http://tinkerpop.apache.org/docs/${project.version}/reference/.general-steps)
     *
     * @since 3.0.0-incubating
     */
    fun sideEffect(consumer: Consumer<Traverser<E>?>?): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.sideEffect, consumer)
        return asAdmin().addStep(LambdaSideEffectStep(asAdmin(), consumer))
    }

    /**
     * Perform some operation on the [Traverser] and pass it to the next step unmodified.
     *
     * @param sideEffectTraversal the operation to perform at this step in relation to the [Traverser]
     * @return the traversal with an appended [TraversalSideEffectStep]
     * @see [Reference Documentation - General Steps](http://tinkerpop.apache.org/docs/${project.version}/reference/.general-steps)
     *
     * @since 3.0.0-incubating
     */
    fun sideEffect(sideEffectTraversal: Traversal<*, *>?): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.sideEffect, sideEffectTraversal)
        return asAdmin().addStep(TraversalSideEffectStep(asAdmin(), sideEffectTraversal as Traversal?))
    }

    /**
     * Iterates the traversal up to the itself and emits the side-effect referenced by the key. If multiple keys are
     * supplied then the side-effects are emitted as a `Map`.
     *
     * @param sideEffectKey  the side-effect to emit
     * @param sideEffectKeys other side-effects to emit
     * @return the traversal with an appended [SideEffectCapStep]
     * @see [Reference Documentation - Cap Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.cap-step)
     *
     * @since 3.0.0-incubating
     */
    fun <E2> cap(sideEffectKey: String?, vararg sideEffectKeys: String?): GraphTraversal<S, E2>? {
        asAdmin().getBytecode().addStep(Symbols.cap, sideEffectKey, sideEffectKeys)
        return asAdmin().addStep(SideEffectCapStep(asAdmin(), sideEffectKey, sideEffectKeys))
    }

    /**
     * Extracts a portion of the graph being traversed into a [Graph] object held in the specified side-effect
     * key.
     *
     * @param sideEffectKey the name of the side-effect key that will hold the subgraph
     * @return the traversal with an appended [SubgraphStep]
     * @see [Reference Documentation - Subgraph Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.subgraph-step)
     *
     * @since 3.0.0-incubating
     */
    fun subgraph(sideEffectKey: String?): GraphTraversal<S, Edge?>? {
        asAdmin().getBytecode().addStep(Symbols.subgraph, sideEffectKey)
        return asAdmin().addStep(SubgraphStep(asAdmin(), sideEffectKey))
    }

    /**
     * Eagerly collects objects up to this step into a side-effect. Same as calling [.aggregate]
     * with a [Scope.local].
     *
     * @param sideEffectKey the name of the side-effect key that will hold the aggregated objects
     * @return the traversal with an appended [AggregateGlobalStep]
     * @see [Reference Documentation - Aggregate Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.aggregate-step)
     *
     * @since 3.0.0-incubating
     */
    fun aggregate(sideEffectKey: String?): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.aggregate, sideEffectKey)
        return asAdmin().addStep(AggregateGlobalStep(asAdmin(), sideEffectKey))
    }

    /**
     * Collects objects in a list using the [Scope] argument to determine whether it should be lazy
     * [Scope.local] or eager ([Scope.global] while gathering those objects.
     *
     * @param sideEffectKey the name of the side-effect key that will hold the aggregated objects
     * @return the traversal with an appended [AggregateGlobalStep]
     * @see [Reference Documentation - Aggregate Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.aggregate-step)
     *
     * @since 3.4.3
     */
    fun aggregate(scope: Scope, sideEffectKey: String?): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.aggregate, scope, sideEffectKey)
        return asAdmin().addStep(
            if (scope === Scope.global) AggregateGlobalStep(asAdmin(), sideEffectKey) else AggregateLocalStep(
                asAdmin(), sideEffectKey
            )
        )
    }

    /**
     * Organize objects in the stream into a `Map`. Calls to `group()` are typically accompanied with
     * [.by] modulators which help specify how the grouping should occur.
     *
     * @param sideEffectKey the name of the side-effect key that will hold the aggregated grouping
     * @return the traversal with an appended [GroupStep].
     * @see [Reference Documentation - Group Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.group-step)
     *
     * @since 3.0.0-incubating
     */
    fun group(sideEffectKey: String?): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.group, sideEffectKey)
        return asAdmin().addStep(GroupSideEffectStep(asAdmin(), sideEffectKey))
    }

    /**
     * Counts the number of times a particular objects has been part of a traversal, returning a `Map` where the
     * object is the key and the value is the count.
     *
     * @param sideEffectKey the name of the side-effect key that will hold the aggregated grouping
     * @return the traversal with an appended [GroupCountStep].
     * @see [Reference Documentation - GroupCount Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.groupcount-step)
     *
     * @since 3.0.0-incubating
     */
    fun groupCount(sideEffectKey: String?): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.groupCount, sideEffectKey)
        return asAdmin().addStep(GroupCountSideEffectStep(asAdmin(), sideEffectKey))
    }

    /**
     * When triggered, immediately throws a `RuntimeException` which implements the [Failure] interface.
     * The traversal will be terminated as a result.
     *
     * @return the traversal with an appended [FailStep].
     * @see [Reference Documentation - Fail Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.fail-step)
     *
     * @since 3.6.0
     */
    fun fail(): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.fail)
        return asAdmin().addStep(FailStep(asAdmin()))
    }

    /**
     * When triggered, immediately throws a `RuntimeException` which implements the [Failure] interface.
     * The traversal will be terminated as a result.
     *
     * @param message the error message to include in the exception
     * @return the traversal with an appended [FailStep].
     * @see [Reference Documentation - Fail Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.fail-step)
     *
     * @since 3.6.0
     */
    fun fail(message: String?): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.fail, message)
        return asAdmin().addStep(FailStep(asAdmin(), message))
    }

    /**
     * Aggregates the emanating paths into a [Tree] data structure.
     *
     * @param sideEffectKey the name of the side-effect key that will hold the tree
     * @return the traversal with an appended [TreeStep]
     * @see [Reference Documentation - Tree Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.tree-step)
     *
     * @since 3.0.0-incubating
     */
    fun tree(sideEffectKey: String?): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.tree, sideEffectKey)
        return asAdmin().addStep(TreeSideEffectStep(asAdmin(), sideEffectKey))
    }

    /**
     * Map the [Traverser] to its [Traverser.sack] value.
     *
     * @param sackOperator the operator to apply to the sack value
     * @return the traversal with an appended [SackStep].
     * @see [Reference Documentation - Sack Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.sack-step)
     *
     * @since 3.0.0-incubating
     */
    fun <V, U> sack(sackOperator: BiFunction<V, U, V>?): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.sack, sackOperator)
        return asAdmin().addStep(SackValueStep(asAdmin(), sackOperator))
    }

    /**
     * Lazily aggregates objects in the stream into a side-effect collection.
     *
     * @param sideEffectKey the name of the side-effect key that will hold the aggregate
     * @return the traversal with an appended [AggregateLocalStep]
     * @see [Reference Documentation - Store Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.store-step)
     *
     * @since 3.0.0-incubating
     */
    @Deprecated
    @Deprecated("As of release 3.4.3, replaced by {@link #aggregate(Scope, String)} using {@link Scope#local}.")
    fun store(sideEffectKey: String?): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.store, sideEffectKey)
        return asAdmin().addStep(AggregateLocalStep(asAdmin(), sideEffectKey))
    }

    /**
     * Allows developers to examine statistical information about a traversal providing data like execution times,
     * counts, etc.
     *
     * @param sideEffectKey the name of the side-effect key within which to hold the profile object
     * @return the traversal with an appended [ProfileSideEffectStep]
     * @see [Reference Documentation - Profile Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.profile-step)
     *
     * @since 3.2.0-incubating
     */
    fun profile(sideEffectKey: String?): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Traversal.Symbols.profile, sideEffectKey)
        return asAdmin().addStep(ProfileSideEffectStep(asAdmin(), sideEffectKey))
    }

    /**
     * Allows developers to examine statistical information about a traversal providing data like execution times,
     * counts, etc.
     *
     * @return the traversal with an appended [ProfileSideEffectStep]
     * @see [Reference Documentation - Profile Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.profile-step)
     *
     * @since 3.0.0-incubating
     */
    @Override
    fun profile(): GraphTraversal<S, TraversalMetrics?>? {
        return super@Traversal.profile()
    }

    /**
     * Sets a [Property] value and related meta properties if supplied, if supported by the [Graph]
     * and if the [Element] is a [VertexProperty].  This method is the long-hand version of
     * [.property] with the difference that the [VertexProperty.Cardinality]
     * can be supplied.
     *
     *
     * Generally speaking, this method will append an [AddPropertyStep] to the [Traversal] but when
     * possible, this method will attempt to fold key/value pairs into an [AddVertexStep], [AddEdgeStep] or
     * [AddVertexStartStep].  This potential optimization can only happen if cardinality is not supplied
     * and when meta-properties are not included.
     *
     * @param cardinality the specified cardinality of the property where `null` will allow the [Graph]
     * to use its default settings
     * @param key         the key for the property
     * @param value       the value for the property which may not be null if the `key` is of type [T]
     * @param keyValues   any meta properties to be assigned to this property
     * @return the traversal with the last step modified to add a property
     * @see [AddProperty Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.addproperty-step)
     *
     * @since 3.0.0-incubating
     */
    fun property(
        cardinality: VertexProperty.Cardinality?,
        key: Object?,
        value: Object?,
        vararg keyValues: Object?
    ): GraphTraversal<S, E>? {
        if (key is T && null == value) throw IllegalArgumentException("Value of T cannot be null")
        if (null == cardinality) asAdmin().getBytecode()
            .addStep(Symbols.property, key, value, keyValues) else asAdmin().getBytecode().addStep(
            Symbols.property, cardinality, key, value, keyValues
        )

        // if it can be detected that this call to property() is related to an addV/E() then we can attempt to fold
        // the properties into that step to gain an optimization for those graphs that support such capabilities.
        var endStep: Step = asAdmin().getEndStep()

        // always try to fold the property() into the initial "AddElementStep" as the performance will be better
        // and as it so happens with T the value must be set by way of that approach otherwise you get an error.
        // it should be safe to execute this loop this way as we'll either hit an "AddElementStep" or an "EmptyStep".
        // if empty, it will just use the regular AddPropertyStep being tacked on to the end of the traversal as usual
        while (endStep is AddPropertyStep) {
            endStep = endStep.getPreviousStep()
        }

        // edge properties can always be folded as there are no cardinality/metaproperties. of course, if the
        // cardinality is specified as something other than single or null it would be confusing to simply allow it to
        // execute and not throw an error.
        if ((endStep is AddEdgeStep || endStep is AddEdgeStartStep) && null != cardinality && cardinality !== single) throw IllegalStateException(
            String.format(
                "Multi-property cardinality of [%s] can only be set for a Vertex but is being used for addE() with key: %s",
                cardinality.name(), key
            )
        )

        // for a vertex mutation, it's possible to fold the property() into the Mutating step if there are no
        // metaproperties (i.e. keyValues) and if (1) the key is an instance of T OR OR (3) the key is a string and the
        // cardinality is not specified. Note that checking for single cardinality of the argument doesn't work well
        // because once folded we lose the cardinality argument associated to the key/value pair and then it relies on
        // the graph. that means that if you do:
        //
        // g.addV().property(single, 'k',1).property(single,'k',2)
        //
        // you could end up with whatever the cardinality is for the key which might seem "wrong" if you were explicit
        // about the specification of "single". it also isn't possible to check the Graph Features for cardinality
        // as folding seems to have different behavior based on different graphs - we clearly don't have that aspect
        // of things tested/enforced well.
        //
        // of additional note is the folding that occurs if the key is a Traversal. the key here is technically
        // unknown until traversal execution as the anonymous traversal result isn't evaluated during traversal
        // construction but during iteration. not folding to AddVertexStep creates different (breaking) traversal
        // semantics than we've had in previous versions so right/wrong could be argued, but since it's a breaking
        // change we'll just arbitrarily account for it to maintain the former behavior.
        if (endStep is AddEdgeStep || endStep is AddEdgeStartStep ||
            (endStep is AddVertexStep || endStep is AddVertexStartStep) && keyValues.size == 0 &&
            (key is T || key is String && null == cardinality || key is Traversal)
        ) {
            (endStep as Mutating).configure(key, value)
        } else {
            val addPropertyStep: AddPropertyStep<Element> = AddPropertyStep(asAdmin(), cardinality, key, value)
            asAdmin().addStep(addPropertyStep)
            addPropertyStep.configure(keyValues)
        }
        return this
    }

    /**
     * Sets the key and value of a [Property]. If the [Element] is a [VertexProperty] and the
     * [Graph] supports it, meta properties can be set.  Use of this method assumes that the
     * [VertexProperty.Cardinality] is defaulted to `null` which  means that the default cardinality for
     * the [Graph] will be used.
     *
     *
     * This method is effectively calls [.property]
     * as `property(null, key, value, keyValues`.
     *
     * @param key       the key for the property
     * @param value     the value for the property
     * @param keyValues any meta properties to be assigned to this property
     * @return the traversal with the last step modified to add a property
     * @see [AddProperty Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.addproperty-step)
     *
     * @since 3.0.0-incubating
     */
    fun property(key: Object?, value: Object?, vararg keyValues: Object?): GraphTraversal<S, E>? {
        return if (key is VertexProperty.Cardinality) this.property(
            key as VertexProperty.Cardinality?, value, if (null == keyValues) null else keyValues[0],
            if (keyValues != null && keyValues.size > 1) Arrays.copyOfRange(
                keyValues,
                1,
                keyValues.size
            ) else arrayOf<Object>()
        ) else this.property(null, key, value, *keyValues)
    }
    ///////////////////// BRANCH STEPS /////////////////////
    /**
     * Split the [Traverser] to all the specified traversals.
     *
     * @param branchTraversal the traversal to branch the [Traverser] to
     * @return the [Traversal] with the [BranchStep] added
     * @see [Reference Documentation - General Steps](http://tinkerpop.apache.org/docs/${project.version}/reference/.general-steps)
     *
     * @since 3.0.0-incubating
     */
    fun <M, E2> branch(branchTraversal: Traversal<*, M>?): GraphTraversal<S, E2>? {
        asAdmin().getBytecode().addStep(Symbols.branch, branchTraversal)
        val branchStep: BranchStep<E, E2, M> = BranchStep(asAdmin())
        branchStep.setBranchTraversal(branchTraversal as Traversal.Admin<E, M>?)
        return asAdmin().addStep(branchStep)
    }

    /**
     * Split the [Traverser] to all the specified functions.
     *
     * @param function the traversal to branch the [Traverser] to
     * @return the [Traversal] with the [BranchStep] added
     * @see [Reference Documentation - General Steps](http://tinkerpop.apache.org/docs/${project.version}/reference/.general-steps)
     *
     * @since 3.0.0-incubating
     */
    fun <M, E2> branch(function: Function<Traverser<E>?, M>?): GraphTraversal<S, E2>? {
        asAdmin().getBytecode().addStep(Symbols.branch, function)
        val branchStep: BranchStep<E, E2, M> = BranchStep(asAdmin())
        branchStep.setBranchTraversal(__.map(function) as Traversal.Admin<E, M>)
        return asAdmin().addStep(branchStep)
    }

    /**
     * Routes the current traverser to a particular traversal branch option which allows the creation of if-then-else
     * like semantics within a traversal. A `choose` is modified by [.option] which provides the various
     * branch choices.
     *
     * @param choiceTraversal the traversal used to determine the value for the branch
     * @return the traversal with the appended [ChooseStep]
     * @see [Reference Documentation - Choose Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.choose-step)
     *
     * @since 3.0.0-incubating
     */
    fun <M, E2> choose(choiceTraversal: Traversal<*, M>?): GraphTraversal<S, E2>? {
        asAdmin().getBytecode().addStep(Symbols.choose, choiceTraversal)
        return asAdmin().addStep(ChooseStep(asAdmin(), choiceTraversal as Traversal.Admin<E, M>?))
    }

    /**
     * Routes the current traverser to a particular traversal branch option which allows the creation of if-then-else
     * like semantics within a traversal.
     *
     * @param traversalPredicate the traversal used to determine the "if" portion of the if-then-else
     * @param trueChoice         the traversal to execute in the event the `traversalPredicate` returns true
     * @param falseChoice        the traversal to execute in the event the `traversalPredicate` returns false
     * @return the traversal with the appended [ChooseStep]
     * @see [Reference Documentation - Choose Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.choose-step)
     *
     * @since 3.0.0-incubating
     */
    fun <E2> choose(
        traversalPredicate: Traversal<*, *>?,
        trueChoice: Traversal<*, E2>?, falseChoice: Traversal<*, E2>?
    ): GraphTraversal<S, E2>? {
        asAdmin().getBytecode().addStep(Symbols.choose, traversalPredicate, trueChoice, falseChoice)
        return asAdmin().addStep(
            ChooseStep<E, E2, Boolean>(
                asAdmin(),
                traversalPredicate as Traversal.Admin<E, *>?,
                trueChoice as Traversal.Admin<E, E2>?,
                falseChoice as Traversal.Admin<E, E2>?
            )
        )
    }

    /**
     * Routes the current traverser to a particular traversal branch option which allows the creation of if-then
     * like semantics within a traversal.
     *
     * @param traversalPredicate the traversal used to determine the "if" portion of the if-then-else
     * @param trueChoice         the traversal to execute in the event the `traversalPredicate` returns true
     * @return the traversal with the appended [ChooseStep]
     * @see [Reference Documentation - Choose Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.choose-step)
     *
     * @since 3.2.4
     */
    fun <E2> choose(
        traversalPredicate: Traversal<*, *>?,
        trueChoice: Traversal<*, E2>?
    ): GraphTraversal<S, E2>? {
        asAdmin().getBytecode().addStep(Symbols.choose, traversalPredicate, trueChoice)
        return asAdmin().addStep(
            ChooseStep<E, E2, Boolean>(
                asAdmin(),
                traversalPredicate as Traversal.Admin<E, *>?,
                trueChoice as Traversal.Admin<E, E2>?,
                __.identity() as Traversal.Admin<E, E2>
            )
        )
    }

    /**
     * Routes the current traverser to a particular traversal branch option which allows the creation of if-then-else
     * like semantics within a traversal. A `choose` is modified by [.option] which provides the various
     * branch choices.
     *
     * @param choiceFunction the function used to determine the value for the branch
     * @return the traversal with the appended [ChooseStep]
     * @see [Reference Documentation - Choose Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.choose-step)
     *
     * @since 3.0.0-incubating
     */
    fun <M, E2> choose(choiceFunction: Function<E, M>?): GraphTraversal<S, E2>? {
        asAdmin().getBytecode().addStep(Symbols.choose, choiceFunction)
        return asAdmin().addStep(
            ChooseStep(
                asAdmin(),
                __.map(FunctionTraverser(choiceFunction)) as Traversal.Admin<E, M>
            )
        )
    }

    /**
     * Routes the current traverser to a particular traversal branch option which allows the creation of if-then-else
     * like semantics within a traversal.
     *
     * @param choosePredicate the function used to determine the "if" portion of the if-then-else
     * @param trueChoice      the traversal to execute in the event the `traversalPredicate` returns true
     * @param falseChoice     the traversal to execute in the event the `traversalPredicate` returns false
     * @return the traversal with the appended [ChooseStep]
     * @see [Reference Documentation - Choose Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.choose-step)
     *
     * @since 3.0.0-incubating
     */
    fun <E2> choose(
        choosePredicate: Predicate<E>?,
        trueChoice: Traversal<*, E2>?, falseChoice: Traversal<*, E2>?
    ): GraphTraversal<S, E2>? {
        asAdmin().getBytecode().addStep(Symbols.choose, choosePredicate, trueChoice, falseChoice)
        return asAdmin().addStep(
            ChooseStep<E, E2, Boolean>(
                asAdmin(),
                __.filter(PredicateTraverser(choosePredicate)) as Traversal.Admin<E, *>,
                trueChoice as Traversal.Admin<E, E2>?,
                falseChoice as Traversal.Admin<E, E2>?
            )
        )
    }

    /**
     * Routes the current traverser to a particular traversal branch option which allows the creation of if-then
     * like semantics within a traversal.
     *
     * @param choosePredicate the function used to determine the "if" portion of the if-then-else
     * @param trueChoice      the traversal to execute in the event the `traversalPredicate` returns true
     * @return the traversal with the appended [ChooseStep]
     * @see [Reference Documentation - Choose Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.choose-step)
     *
     * @since 3.2.4
     */
    fun <E2> choose(
        choosePredicate: Predicate<E>?,
        trueChoice: Traversal<*, E2>?
    ): GraphTraversal<S, E2>? {
        asAdmin().getBytecode().addStep(Symbols.choose, choosePredicate, trueChoice)
        return asAdmin().addStep(
            ChooseStep<E, E2, Boolean>(
                asAdmin(),
                __.filter(PredicateTraverser(choosePredicate)) as Traversal.Admin<E, *>,
                trueChoice as Traversal.Admin<E, E2>?,
                __.identity() as Traversal.Admin<E, E2>
            )
        )
    }

    /**
     * Returns the result of the specified traversal if it yields a result, otherwise it returns the calling element.
     *
     * @param optionalTraversal the traversal to execute for a potential result
     * @return the traversal with the appended [ChooseStep]
     * @see [Reference Documentation - Optional Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.optional-step)
     *
     * @since 3.0.0-incubating
     */
    fun <E2> optional(optionalTraversal: Traversal<*, E2>?): GraphTraversal<S, E2>? {
        asAdmin().getBytecode().addStep(Symbols.optional, optionalTraversal)
        return asAdmin().addStep(OptionalStep(asAdmin(), optionalTraversal as Traversal.Admin<E2, E2>?))
    }

    /**
     * Merges the results of an arbitrary number of traversals.
     *
     * @param unionTraversals the traversals to merge
     * @return the traversal with the appended [UnionStep]
     * @see [Reference Documentation - Union Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.union-step)
     *
     * @since 3.0.0-incubating
     */
    fun <E2> union(vararg unionTraversals: Traversal<*, E2>?): GraphTraversal<S, E2>? {
        asAdmin().getBytecode().addStep(Symbols.union, unionTraversals)
        return asAdmin().addStep(
            UnionStep(
                asAdmin(),
                Arrays.copyOf(unionTraversals, unionTraversals.size, Array<Traversal.Admin>::class.java)
            )
        )
    }

    /**
     * Evaluates the provided traversals and returns the result of the first traversal to emit at least one object.
     *
     * @param coalesceTraversals the traversals to coalesce
     * @return the traversal with the appended [CoalesceStep]
     * @see [Reference Documentation - Coalesce Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.coalesce-step)
     *
     * @since 3.0.0-incubating
     */
    fun <E2> coalesce(vararg coalesceTraversals: Traversal<*, E2>?): GraphTraversal<S, E2>? {
        asAdmin().getBytecode().addStep(Symbols.coalesce, coalesceTraversals)
        return asAdmin().addStep(
            CoalesceStep(
                asAdmin(),
                Arrays.copyOf(coalesceTraversals, coalesceTraversals.size, Array<Traversal.Admin>::class.java)
            )
        )
    }

    /**
     * This step is used for looping over a traversal given some break predicate.
     *
     * @param repeatTraversal the traversal to repeat over
     * @return the traversal with the appended [RepeatStep]
     * @see [Reference Documentation - Repeat Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.repeat-step)
     *
     * @since 3.0.0-incubating
     */
    fun repeat(repeatTraversal: Traversal<*, E>?): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.repeat, repeatTraversal)
        return RepeatStep.addRepeatToTraversal(this, repeatTraversal as Traversal.Admin<E, E>?)
    }

    /**
     * This step is used for looping over a traversal given some break predicate and with a specified loop name.
     *
     * @param repeatTraversal the traversal to repeat over
     * @param loopName The name given to the loop
     * @return the traversal with the appended [RepeatStep]
     * @see [Reference Documentation - Repeat Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.repeat-step)
     *
     * @since 3.4.0
     */
    fun repeat(loopName: String?, repeatTraversal: Traversal<*, E>?): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.repeat, loopName, repeatTraversal)
        return RepeatStep.addRepeatToTraversal(this, loopName, repeatTraversal as Traversal.Admin<E, E>?)
    }

    /**
     * Emit is used in conjunction with [.repeat] to determine what objects get emit from the loop.
     *
     * @param emitTraversal the emit predicate defined as a traversal
     * @return the traversal with the appended [RepeatStep]
     * @see [Reference Documentation - Repeat Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.repeat-step)
     *
     * @since 3.0.0-incubating
     */
    fun emit(emitTraversal: Traversal<*, *>?): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.emit, emitTraversal)
        return RepeatStep.addEmitToTraversal(this, emitTraversal as Traversal.Admin<E, *>?)
    }

    /**
     * Emit is used in conjunction with [.repeat] to determine what objects get emit from the loop.
     *
     * @param emitPredicate the emit predicate
     * @return the traversal with the appended [RepeatStep]
     * @see [Reference Documentation - Repeat Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.repeat-step)
     *
     * @since 3.0.0-incubating
     */
    fun emit(emitPredicate: Predicate<Traverser<E>?>?): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.emit, emitPredicate)
        return RepeatStep.addEmitToTraversal(this, __.filter(emitPredicate) as Traversal.Admin<E, *>)
    }

    /**
     * Emit is used in conjunction with [.repeat] to emit all objects from the loop.
     *
     * @return the traversal with the appended [RepeatStep]
     * @see [Reference Documentation - Repeat Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.repeat-step)
     *
     * @since 3.0.0-incubating
     */
    fun emit(): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.emit)
        return RepeatStep.addEmitToTraversal(this, TrueTraversal.instance())
    }

    /**
     * Modifies a [.repeat] to determine when the loop should exit.
     *
     * @param untilTraversal the traversal that determines when the loop exits
     * @return the traversal with the appended [RepeatStep]
     * @see [Reference Documentation - Repeat Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.repeat-step)
     *
     * @since 3.0.0-incubating
     */
    fun until(untilTraversal: Traversal<*, *>?): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.until, untilTraversal)
        return RepeatStep.addUntilToTraversal(this, untilTraversal as Traversal.Admin<E, *>?)
    }

    /**
     * Modifies a [.repeat] to determine when the loop should exit.
     *
     * @param untilPredicate the predicate that determines when the loop exits
     * @return the traversal with the appended [RepeatStep]
     * @see [Reference Documentation - Repeat Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.repeat-step)
     *
     * @since 3.0.0-incubating
     */
    fun until(untilPredicate: Predicate<Traverser<E>?>?): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.until, untilPredicate)
        return RepeatStep.addUntilToTraversal(this, __.filter(untilPredicate) as Traversal.Admin<E, *>)
    }

    /**
     * Modifies a [.repeat] to specify how many loops should occur before exiting.
     *
     * @param maxLoops the number of loops to execute prior to exiting
     * @return the traversal with the appended [RepeatStep]
     * @see [Reference Documentation - Repeat Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.repeat-step)
     *
     * @since 3.0.0-incubating
     */
    operator fun times(maxLoops: Int): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.times, maxLoops)
        return if (asAdmin().getEndStep() is TimesModulating) {
            (asAdmin().getEndStep() as TimesModulating).modulateTimes(maxLoops)
            this
        } else RepeatStep.addUntilToTraversal(this, LoopTraversal(maxLoops))
    }

    /**
     * Provides a execute a specified traversal on a single element within a stream.
     *
     * @param localTraversal the traversal to execute locally
     * @return the traversal with the appended [LocalStep]
     * @see [Reference Documentation - Local Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.local-step)
     *
     * @since 3.0.0-incubating
     */
    fun <E2> local(localTraversal: Traversal<*, E2>): GraphTraversal<S, E2>? {
        asAdmin().getBytecode().addStep(Symbols.local, localTraversal)
        return asAdmin().addStep(LocalStep(asAdmin(), localTraversal.asAdmin()))
    }
    /////////////////// VERTEX PROGRAM STEPS ////////////////
    /**
     * Calculates a PageRank over the graph using a 0.85 for the `alpha` value.
     *
     * @return the traversal with the appended [PageRankVertexProgramStep]
     * @see [Reference Documentation - PageRank Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.pagerank-step)
     *
     * @since 3.2.0-incubating
     */
    fun pageRank(): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.pageRank)
        return asAdmin().addStep(PageRankVertexProgramStep(asAdmin(), 0.85) as Step<E, E>)
    }

    /**
     * Calculates a PageRank over the graph.
     *
     * @return the traversal with the appended [PageRankVertexProgramStep]
     * @see [Reference Documentation - PageRank Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.pagerank-step)
     *
     * @since 3.2.0-incubating
     */
    fun pageRank(alpha: Double): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.pageRank, alpha)
        return asAdmin().addStep(PageRankVertexProgramStep(asAdmin(), alpha) as Step<E, E>)
    }

    /**
     * Executes a Peer Pressure community detection algorithm over the graph.
     *
     * @return the traversal with the appended [PeerPressureVertexProgramStep]
     * @see [Reference Documentation - PeerPressure Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.peerpressure-step)
     *
     * @since 3.2.0-incubating
     */
    fun peerPressure(): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.peerPressure)
        return asAdmin().addStep(PeerPressureVertexProgramStep(asAdmin()) as Step<E, E>)
    }

    /**
     * Executes a Connected Component algorithm over the graph.
     *
     * @return the traversal with the appended [ConnectedComponentVertexProgram]
     * @see [Reference Documentation - ConnectedComponent Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.connectedcomponent-step)
     *
     * @since 3.4.0
     */
    fun connectedComponent(): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.connectedComponent)
        return asAdmin().addStep(ConnectedComponentVertexProgramStep(asAdmin()) as Step<E, E>)
    }

    /**
     * Executes a Shortest Path algorithm over the graph.
     *
     * @return the traversal with the appended [ShortestPathVertexProgramStep]
     * @see [Reference Documentation - ShortestPath Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.shortestpath-step)
     */
    fun shortestPath(): GraphTraversal<S, Path?>? {
        if (asAdmin().getEndStep() is GraphStep) {
            // This is very unfortunate, but I couldn't find another way to make it work. Without the additional
            // IdentityStep, TraversalVertexProgram doesn't handle halted traversers as expected (it ignores both:
            // HALTED_TRAVERSER stored in memory and stored as vertex properties); instead it just emits all vertices.
            identity()
        }
        asAdmin().getBytecode().addStep(Symbols.shortestPath)
        return (asAdmin() as Traversal.Admin)
            .addStep(ShortestPathVertexProgramStep(asAdmin()))
    }

    /**
     * Executes an arbitrary [VertexProgram] over the graph.
     *
     * @return the traversal with the appended [ProgramVertexProgramStep]
     * @see [Reference Documentation - Program Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.program-step)
     *
     * @since 3.2.0-incubating
     */
    fun program(vertexProgram: VertexProgram<*>?): GraphTraversal<S, E>? {
        return asAdmin().addStep(ProgramVertexProgramStep(asAdmin(), vertexProgram) as Step<E, E>)
    }
    ///////////////////// UTILITY STEPS /////////////////////
    /**
     * A step modulator that provides a label to the step that can be accessed later in the traversal by other steps.
     *
     * @param stepLabel  the name of the step
     * @param stepLabels additional names for the label
     * @return the traversal with the modified end step
     * @see [Reference Documentation - As Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.as-step)
     *
     * @since 3.0.0-incubating
     */
    fun `as`(stepLabel: String?, vararg stepLabels: String?): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.`as`, stepLabel, stepLabels)
        if (asAdmin().getSteps().size() === 0) asAdmin().addStep(StartStep(asAdmin()))
        val endStep: Step<*, E> = asAdmin().getEndStep()
        endStep.addLabel(stepLabel)
        for (label in stepLabels) {
            endStep.addLabel(label)
        }
        return this
    }

    /**
     * Turns the lazy traversal pipeline into a bulk-synchronous pipeline which basically iterates that traversal to
     * the size of the barrier. In this case, it iterates the entire thing as the default barrier size is set to
     * `Integer.MAX_VALUE`.
     *
     * @return the traversal with an appended [NoOpBarrierStep]
     * @see [Reference Documentation - Barrier Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.barrier-step)
     *
     * @since 3.0.0-incubating
     */
    fun barrier(): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.barrier)
        return asAdmin().addStep(NoOpBarrierStep(asAdmin(), Integer.MAX_VALUE))
    }

    /**
     * Turns the lazy traversal pipeline into a bulk-synchronous pipeline which basically iterates that traversal to
     * the size of the barrier.
     *
     * @param maxBarrierSize the size of the barrier
     * @return the traversal with an appended [NoOpBarrierStep]
     * @see [Reference Documentation - Barrier Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.barrier-step)
     *
     * @since 3.0.0-incubating
     */
    fun barrier(maxBarrierSize: Int): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.barrier, maxBarrierSize)
        return asAdmin().addStep(NoOpBarrierStep(asAdmin(), maxBarrierSize))
    }

    /**
     * Indexes all items of the current collection. The indexing format can be configured using the [GraphTraversal.with]
     * and [org.apache.tinkerpop4.gremlin.process.traversal.step.util.WithOptions.indexer].
     *
     * Indexed as list: ["a","b","c"] =&gt; [["a",0],["b",1],["c",2]]
     * Indexed as map:  ["a","b","c"] =&gt; {0:"a",1:"b",2:"c"}
     *
     * If the current object is not a collection, this step will map the object to a single item collection/map:
     *
     * Indexed as list: "a" =&gt; ["a",0]
     * Indexed as map:  "a" =&gt; {0:"a"}
     *
     * @return the traversal with an appended [IndexStep]
     * @see [Reference Documentation - Index Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.index-step)
     *
     * @since 3.4.0
     */
    fun <E2> index(): GraphTraversal<S, E2>? {
        asAdmin().getBytecode().addStep(Symbols.index)
        return asAdmin().addStep(IndexStep(asAdmin()))
    }

    /**
     * Turns the lazy traversal pipeline into a bulk-synchronous pipeline which basically iterates that traversal to
     * the size of the barrier. In this case, it iterates the entire thing as the default barrier size is set to
     * `Integer.MAX_VALUE`.
     *
     * @param barrierConsumer a consumer function that is applied to the objects aggregated to the barrier
     * @return the traversal with an appended [NoOpBarrierStep]
     * @see [Reference Documentation - Barrier Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.barrier-step)
     *
     * @since 3.2.0-incubating
     */
    fun barrier(barrierConsumer: Consumer<TraverserSet<Object?>?>?): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.barrier, barrierConsumer)
        return asAdmin().addStep(
            LambdaCollectingBarrierStep(
                asAdmin(),
                barrierConsumer as Consumer?,
                Integer.MAX_VALUE
            )
        )
    }
    //// WITH-MODULATOR
    /**
     * Provides a configuration to a step in the form of a key which is the same as `with(key, true)`. The key
     * of the configuration must be step specific and therefore a configuration could be supplied that is not known to
     * be valid until execution.
     *
     * @param key the key of the configuration to apply to a step
     * @return the traversal with a modulated step
     * @see [Reference Documentation - With Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.with-step)
     *
     * @since 3.4.0
     */
    fun with(key: String): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.with, key)
        val configPair: Array<Object> = arrayOf(key, true)
        (asAdmin().getEndStep() as Configuring).configure(configPair)
        return this
    }

    /**
     * Provides a configuration to a step in the form of a key and value pair. The key of the configuration must be
     * step specific and therefore a configuration could be supplied that is not known to be valid until execution.
     *
     * @param key the key of the configuration to apply to a step
     * @param value the value of the configuration to apply to a step
     * @return the traversal with a modulated step
     * @see [Reference Documentation - With Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.with-step)
     *
     * @since 3.4.0
     */
    fun with(key: String, value: Object): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.with, key, value)
        val configPair: Array<Object> = arrayOf(key, value)
        (asAdmin().getEndStep() as Configuring).configure(configPair)
        return this
    }
    //// BY-MODULATORS
    /**
     * The `by()` can be applied to a number of different step to alter their behaviors. This form is essentially
     * an [.identity] modulation.
     *
     * @return the traversal with a modulated step.
     * @see [Reference Documentation - By Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.by-step)
     *
     * @since 3.0.0-incubating
     */
    fun by(): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.by)
        (asAdmin().getEndStep() as ByModulating).modulateBy()
        return this
    }

    /**
     * The `by()` can be applied to a number of different step to alter their behaviors. Modifies the previous
     * step with the specified traversal.
     *
     * @param traversal the traversal to apply
     * @return the traversal with a modulated step.
     * @see [Reference Documentation - By Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.by-step)
     *
     * @since 3.0.0-incubating
     */
    fun by(traversal: Traversal<*, *>): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.by, traversal)
        (asAdmin().getEndStep() as ByModulating).modulateBy(traversal.asAdmin())
        return this
    }

    /**
     * The `by()` can be applied to a number of different step to alter their behaviors. Modifies the previous
     * step with the specified token of [T].
     *
     * @param token the token to apply
     * @return the traversal with a modulated step.
     * @see [Reference Documentation - By Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.by-step)
     *
     * @since 3.0.0-incubating
     */
    fun by(token: T?): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.by, token)
        (asAdmin().getEndStep() as ByModulating).modulateBy(token)
        return this
    }

    /**
     * The `by()` can be applied to a number of different step to alter their behaviors. Modifies the previous
     * step with the specified key.
     *
     * @param key the key to apply
     * @return the traversal with a modulated step.
     * @see [Reference Documentation - By Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.by-step)
     *
     * @since 3.0.0-incubating
     */
    fun by(key: String?): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.by, key)
        (asAdmin().getEndStep() as ByModulating).modulateBy(key)
        return this
    }

    /**
     * The `by()` can be applied to a number of different step to alter their behaviors. Modifies the previous
     * step with the specified function.
     *
     * @param function the function to apply
     * @return the traversal with a modulated step.
     * @see [Reference Documentation - By Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.by-step)
     *
     * @since 3.0.0-incubating
     */
    fun <V> by(function: Function<V, Object?>?): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.by, function)
        (asAdmin().getEndStep() as ByModulating).modulateBy(function)
        return this
    }
    //// COMPARATOR BY-MODULATORS
    /**
     * The `by()` can be applied to a number of different step to alter their behaviors. Modifies the previous
     * step with the specified function.
     *
     * @param traversal  the traversal to apply
     * @param comparator the comparator to apply typically for some [.order]
     * @return the traversal with a modulated step.
     * @see [Reference Documentation - By Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.by-step)
     *
     * @since 3.0.0-incubating
     */
    fun <V> by(traversal: Traversal<*, *>, comparator: Comparator<V>?): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.by, traversal, comparator)
        (asAdmin().getEndStep() as ByModulating).modulateBy(traversal.asAdmin(), comparator)
        return this
    }

    /**
     * The `by()` can be applied to a number of different step to alter their behaviors. Modifies the previous
     * step with the specified function.
     *
     * @param comparator the comparator to apply typically for some [.order]
     * @return the traversal with a modulated step.
     * @see [Reference Documentation - By Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.by-step)
     *
     * @since 3.0.0-incubating
     */
    fun by(comparator: Comparator<E>?): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.by, comparator)
        (asAdmin().getEndStep() as ByModulating).modulateBy(comparator)
        return this
    }

    /**
     * The `by()` can be applied to a number of different step to alter their behaviors. Modifies the previous
     * step with the specified function.
     *
     * @param order the comparator to apply typically for some [.order]
     * @return the traversal with a modulated step.
     * @see [Reference Documentation - By Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.by-step)
     *
     * @since 3.0.0-incubating
     */
    fun by(order: Order?): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.by, order)
        (asAdmin().getEndStep() as ByModulating).modulateBy(order)
        return this
    }

    /**
     * The `by()` can be applied to a number of different step to alter their behaviors. Modifies the previous
     * step with the specified function.
     *
     * @param key        the key to apply                                                                                                     traversal
     * @param comparator the comparator to apply typically for some [.order]
     * @return the traversal with a modulated step.
     * @see [Reference Documentation - By Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.by-step)
     *
     * @since 3.0.0-incubating
     */
    fun <V> by(key: String?, comparator: Comparator<V>?): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.by, key, comparator)
        (asAdmin().getEndStep() as ByModulating).modulateBy(key, comparator)
        return this
    }

    /**
     * The `by()` can be applied to a number of different step to alter their behaviors. Modifies the previous
     * step with the specified function.
     *
     * @param function   the function to apply
     * @param comparator the comparator to apply typically for some [.order]
     * @return the traversal with a modulated step.
     * @see [Reference Documentation - By Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.by-step)
     *
     * @since 3.0.0-incubating
     */
    fun <U> by(function: Function<U, Object?>?, comparator: Comparator?): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.by, function, comparator)
        (asAdmin().getEndStep() as ByModulating).modulateBy(function, comparator)
        return this
    }
    ////
    /**
     * This step modifies [.choose] to specifies the available choices that might be executed.
     *
     * @param pick       the token that would trigger this option which may be a [TraversalOptionParent.Pick],
     * a [Traversal], [Predicate], or object depending on the step being modulated.
     * @param traversalOption the option as a traversal
     * @return the traversal with the modulated step
     * @see [Reference Documentation - Choose Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.choose-step)
     *
     * @since 3.0.0-incubating
     */
    fun <M, E2> option(pick: M, traversalOption: Traversal<*, E2>): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.option, pick, traversalOption)
        (asAdmin().getEndStep() as TraversalOptionParent<M, E, E2>).addGlobalChildOption(
            pick,
            traversalOption.asAdmin() as Traversal.Admin<E, E2>
        )
        return this
    }

    /**
     * This step modifies [.choose] to specifies the available choices that might be executed.
     *
     * @param traversalOption the option as a traversal
     * @return the traversal with the modulated step
     * @see [Reference Documentation - Choose Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.choose-step)
     *
     * @since 3.0.0-incubating
     */
    fun <E2> option(traversalOption: Traversal<*, E2>): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.option, traversalOption)
        (asAdmin().getEndStep() as TraversalOptionParent<Object?, E, E2>).addGlobalChildOption(
            TraversalOptionParent.Pick.any,
            traversalOption.asAdmin() as Traversal.Admin<E, E2>
        )
        return this
    }
    ////
    ///////////////////// IO STEPS /////////////////////
    /**
     * This step is technically a step modulator for the the [GraphTraversalSource.io] step which
     * instructs the step to perform a read with its given configuration.
     *
     * @return the traversal with the [IoStep] modulated to read
     * @see [Reference Documentation - IO Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.io-step)
     *
     * @see [Reference Documentation - Read Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.read-step)
     *
     * @since 3.4.0
     */
    fun read(): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.read)
        (asAdmin().getEndStep() as ReadWriting).setMode(ReadWriting.Mode.READING)
        return this
    }

    /**
     * This step is technically a step modulator for the the [GraphTraversalSource.io] step which
     * instructs the step to perform a write with its given configuration.
     *
     * @return the traversal with the [IoStep] modulated to write
     * @see [Reference Documentation - IO Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.io-step)
     *
     * @see [Reference Documentation - Write Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.write-step)
     *
     * @since 3.4.0
     */
    fun write(): GraphTraversal<S, E>? {
        asAdmin().getBytecode().addStep(Symbols.write)
        (asAdmin().getEndStep() as ReadWriting).setMode(ReadWriting.Mode.WRITING)
        return this
    }

    /**
     * Iterates the traversal presumably for the generation of side-effects.
     */
    @Override
    fun iterate(): GraphTraversal<S, E>? {
        super@Traversal.iterate()
        return this
    }

    ////
    object Symbols {
        const val map = "map"
        const val flatMap = "flatMap"
        const val id = "id"
        const val label = "label"
        const val identity = "identity"
        const val constant = "constant"
        const val V = "V"
        const val E = "E"
        const val to = "to"
        const val out = "out"
        const val `in` = "in"
        const val both = "both"
        const val toE = "toE"
        const val outE = "outE"
        const val inE = "inE"
        const val bothE = "bothE"
        const val toV = "toV"
        const val outV = "outV"
        const val inV = "inV"
        const val bothV = "bothV"
        const val otherV = "otherV"
        const val order = "order"
        const val properties = "properties"
        const val values = "values"
        const val propertyMap = "propertyMap"
        const val valueMap = "valueMap"
        const val elementMap = "elementMap"
        const val select = "select"
        const val key = "key"
        const val value = "value"
        const val path = "path"
        const val match = "match"
        const val math = "math"
        const val sack = "sack"
        const val loops = "loops"
        const val project = "project"
        const val unfold = "unfold"
        const val fold = "fold"
        const val count = "count"
        const val sum = "sum"
        const val max = "max"
        const val min = "min"
        const val mean = "mean"
        const val group = "group"
        const val groupCount = "groupCount"
        const val tree = "tree"
        const val addV = "addV"
        const val addE = "addE"
        const val from = "from"
        const val filter = "filter"
        const val or = "or"
        const val and = "and"
        const val inject = "inject"
        const val dedup = "dedup"
        const val where = "where"
        const val has = "has"
        const val hasNot = "hasNot"
        const val hasLabel = "hasLabel"
        const val hasId = "hasId"
        const val hasKey = "hasKey"
        const val hasValue = "hasValue"
        const val `is` = "is"
        const val not = "not"
        const val range = "range"
        const val limit = "limit"
        const val skip = "skip"
        const val tail = "tail"
        const val coin = "coin"
        const val io = "io"
        const val read = "read"
        const val write = "write"
        const val timeLimit = "timeLimit"
        const val simplePath = "simplePath"
        const val cyclicPath = "cyclicPath"
        const val sample = "sample"
        const val drop = "drop"
        const val sideEffect = "sideEffect"
        const val cap = "cap"
        const val property = "property"

        @Deprecated
        @Deprecated("As of release 3.4.3, replaced by {@link Symbols#aggregate} with a {@link Scope#local}.")
        val store = "store"
        const val aggregate = "aggregate"
        const val fail = "fail"
        const val subgraph = "subgraph"
        const val barrier = "barrier"
        const val index = "index"
        const val local = "local"
        const val emit = "emit"
        const val repeat = "repeat"
        const val until = "until"
        const val branch = "branch"
        const val union = "union"
        const val coalesce = "coalesce"
        const val choose = "choose"
        const val optional = "optional"
        const val pageRank = "pageRank"
        const val peerPressure = "peerPressure"
        const val connectedComponent = "connectedComponent"
        const val shortestPath = "shortestPath"
        const val program = "program"
        const val by = "by"
        const val with = "with"
        const val times = "times"
        const val `as` = "as"
        const val option = "option"
    }
}