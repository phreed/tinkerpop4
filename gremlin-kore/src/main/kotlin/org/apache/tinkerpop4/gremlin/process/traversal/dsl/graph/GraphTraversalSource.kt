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

import org.apache.tinkerpop4.gremlin.process.computer.Computer

/**
 * A `GraphTraversalSource` is the primary DSL of the Gremlin traversal machine.
 * It provides access to all the configurations and steps for Turing complete graph computing.
 * Any DSL can be constructed based on the methods of both `GraphTraversalSource` and [GraphTraversal].
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class GraphTraversalSource(graph: Graph, traversalStrategies: TraversalStrategies) : TraversalSource {
    @kotlin.jvm.Transient
    protected var connection: RemoteConnection? = null
    protected val graph: Graph
    protected var strategies: TraversalStrategies
    protected var bytecode: Bytecode = Bytecode()

    ////////////////
    object Symbols {
        const val withBulk = "withBulk"
        const val withPath = "withPath"
    }

    ////////////////
    init {
        this.graph = graph
        strategies = traversalStrategies
    }

    constructor(graph: Graph) : this(graph, TraversalStrategies.GlobalCache.getStrategies(graph.getClass())) {}
    constructor(connection: RemoteConnection?) : this(
        EmptyGraph.instance(), TraversalStrategies.GlobalCache.getStrategies(
            EmptyGraph::class.java
        ).clone()
    ) {
        this.connection = connection
        strategies.addStrategies(RemoteStrategy(connection))
    }

    @get:Override
    val anonymousTraversalClass: Optional<Class<*>>
        get() = Optional.of(__::class.java)

    @Override
    fun getStrategies(): TraversalStrategies {
        return strategies
    }

    @Override
    fun getGraph(): Graph {
        return graph
    }

    @Override
    fun getBytecode(): Bytecode {
        return bytecode
    }

    @SuppressWarnings("CloneDoesntDeclareCloneNotSupportedException")
    fun clone(): GraphTraversalSource {
        return try {
            val clone = super.clone() as GraphTraversalSource
            clone.strategies = strategies.clone()
            clone.bytecode = bytecode.clone()
            clone
        } catch (e: CloneNotSupportedException) {
            throw IllegalStateException(e.getMessage(), e)
        }
    }
    //// CONFIGURATIONS
    /**
     * {@inheritDoc}
     */
    @Override
    fun with(key: String?): GraphTraversalSource {
        return super@TraversalSource.with(key)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun with(key: String?, value: Object?): GraphTraversalSource {
        return super@TraversalSource.with(key, value)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun withStrategies(vararg traversalStrategies: TraversalStrategy?): GraphTraversalSource {
        return super@TraversalSource.withStrategies(traversalStrategies)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings(["unchecked"])
    fun withoutStrategies(vararg traversalStrategyClasses: Class<out TraversalStrategy?>?): GraphTraversalSource {
        return super@TraversalSource.withoutStrategies(traversalStrategyClasses)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun withComputer(computer: Computer?): GraphTraversalSource {
        return super@TraversalSource.withComputer(computer)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun withComputer(graphComputerClass: Class<out GraphComputer?>?): GraphTraversalSource {
        return super@TraversalSource.withComputer(graphComputerClass)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun withComputer(): GraphTraversalSource {
        return super@TraversalSource.withComputer()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun <A> withSideEffect(
        key: String?,
        initialValue: Supplier<A>?,
        reducer: BinaryOperator<A>?
    ): GraphTraversalSource {
        return super@TraversalSource.withSideEffect(key, initialValue, reducer)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun <A> withSideEffect(key: String?, initialValue: A, reducer: BinaryOperator<A>?): GraphTraversalSource {
        return super@TraversalSource.withSideEffect(key, initialValue, reducer)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun <A> withSideEffect(key: String?, initialValue: A): GraphTraversalSource {
        return super@TraversalSource.withSideEffect(key, initialValue)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun <A> withSideEffect(key: String?, initialValue: Supplier<A>?): GraphTraversalSource {
        return super@TraversalSource.withSideEffect(key, initialValue)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun <A> withSack(
        initialValue: Supplier<A>?,
        splitOperator: UnaryOperator<A>?,
        mergeOperator: BinaryOperator<A>?
    ): GraphTraversalSource {
        return super@TraversalSource.withSack(initialValue, splitOperator, mergeOperator)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun <A> withSack(
        initialValue: A,
        splitOperator: UnaryOperator<A>?,
        mergeOperator: BinaryOperator<A>?
    ): GraphTraversalSource {
        return super@TraversalSource.withSack(initialValue, splitOperator, mergeOperator)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun <A> withSack(initialValue: A): GraphTraversalSource {
        return super@TraversalSource.withSack(initialValue)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun <A> withSack(initialValue: Supplier<A>?): GraphTraversalSource {
        return super@TraversalSource.withSack(initialValue)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun <A> withSack(initialValue: Supplier<A>?, splitOperator: UnaryOperator<A>?): GraphTraversalSource {
        return super@TraversalSource.withSack(initialValue, splitOperator)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun <A> withSack(initialValue: A, splitOperator: UnaryOperator<A>?): GraphTraversalSource {
        return super@TraversalSource.withSack(initialValue, splitOperator)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun <A> withSack(initialValue: Supplier<A>?, mergeOperator: BinaryOperator<A>?): GraphTraversalSource {
        return super@TraversalSource.withSack(initialValue, mergeOperator)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun <A> withSack(initialValue: A, mergeOperator: BinaryOperator<A>?): GraphTraversalSource {
        return super@TraversalSource.withSack(initialValue, mergeOperator)
    }

    fun withBulk(useBulk: Boolean): GraphTraversalSource {
        if (useBulk) return this
        val clone = clone()
        RequirementsStrategy.addRequirements(clone.getStrategies(), TraverserRequirement.ONE_BULK)
        clone.bytecode.addSource(Symbols.withBulk, useBulk)
        return clone
    }

    fun withPath(): GraphTraversalSource {
        val clone = clone()
        RequirementsStrategy.addRequirements(clone.getStrategies(), TraverserRequirement.PATH)
        clone.bytecode.addSource(Symbols.withPath)
        return clone
    }
    //// SPAWNS
    /**
     * Spawns a [GraphTraversal] by adding a vertex with the specified label. If the `label` is
     * `null` then it will default to [Vertex.DEFAULT_LABEL].
     */
    fun addV(vertexLabel: String?): GraphTraversal<Vertex, Vertex> {
        if (null == vertexLabel) throw IllegalArgumentException("vertexLabel cannot be null")
        val clone = clone()
        clone.bytecode.addStep(GraphTraversal.Symbols.addV, vertexLabel)
        val traversal: GraphTraversal.Admin<Vertex, Vertex> = DefaultGraphTraversal(clone)
        return traversal.addStep(AddVertexStartStep(traversal, vertexLabel))
    }

    /**
     * Spawns a [GraphTraversal] by adding a vertex with the label as determined by a [Traversal]. If the
     * `vertexLabelTraversal` is `null` then it will default to [Vertex.DEFAULT_LABEL].
     */
    fun addV(vertexLabelTraversal: Traversal<*, String?>?): GraphTraversal<Vertex, Vertex> {
        if (null == vertexLabelTraversal) throw IllegalArgumentException("vertexLabelTraversal cannot be null")
        val clone = clone()
        clone.bytecode.addStep(GraphTraversal.Symbols.addV, vertexLabelTraversal)
        val traversal: GraphTraversal.Admin<Vertex, Vertex> = DefaultGraphTraversal(clone)
        return traversal.addStep(AddVertexStartStep(traversal, vertexLabelTraversal))
    }

    /**
     * Spawns a [GraphTraversal] by adding a vertex with the default label.
     */
    fun addV(): GraphTraversal<Vertex, Vertex> {
        val clone = clone()
        clone.bytecode.addStep(GraphTraversal.Symbols.addV)
        val traversal: GraphTraversal.Admin<Vertex, Vertex> = DefaultGraphTraversal(clone)
        return traversal.addStep(AddVertexStartStep(traversal, null as String?))
    }

    /**
     * Spawns a [GraphTraversal] by adding a edge with the specified label.
     */
    fun addE(label: String?): GraphTraversal<Edge, Edge> {
        val clone = clone()
        clone.bytecode.addStep(GraphTraversal.Symbols.addE, label)
        val traversal: GraphTraversal.Admin<Edge, Edge> = DefaultGraphTraversal(clone)
        return traversal.addStep(AddEdgeStartStep(traversal, label))
    }

    /**
     * Spawns a [GraphTraversal] by adding a edge with a label as specified by the provided [Traversal].
     */
    fun addE(edgeLabelTraversal: Traversal<*, String?>?): GraphTraversal<Edge, Edge> {
        val clone = clone()
        clone.bytecode.addStep(GraphTraversal.Symbols.addE, edgeLabelTraversal)
        val traversal: GraphTraversal.Admin<Edge, Edge> = DefaultGraphTraversal(clone)
        return traversal.addStep(AddEdgeStartStep(traversal, edgeLabelTraversal))
    }

    /**
     * Spawns a [GraphTraversal] starting it with arbitrary values.
     */
    fun <S> inject(vararg starts: S): GraphTraversal<S, S> {
        // a single null is [null]
        val s: Array<S?> = starts ?: arrayOf(null) as Array<S?>
        val clone = clone()
        clone.bytecode.addStep(GraphTraversal.Symbols.inject, s)
        val traversal: GraphTraversal.Admin<S, S> = DefaultGraphTraversal(clone)
        return traversal.addStep(InjectStep<S>(traversal, s))
    }

    /**
     * Spawns a [GraphTraversal] starting with all vertices or some subset of vertices as specified by their
     * unique identifier.
     */
    fun V(vararg vertexIds: Object?): GraphTraversal<Vertex, Vertex> {
        // a single null is [null]
        val ids: Array<Object?> = vertexIds ?: arrayOf(null)
        val clone = clone()
        clone.bytecode.addStep(GraphTraversal.Symbols.V, ids)
        val traversal: GraphTraversal.Admin<Vertex, Vertex> = DefaultGraphTraversal(clone)
        return traversal.addStep(GraphStep(traversal, Vertex::class.java, true, ids))
    }

    /**
     * Spawns a [GraphTraversal] starting with all edges or some subset of edges as specified by their unique
     * identifier.
     */
    fun E(vararg edgeIds: Object?): GraphTraversal<Edge, Edge> {
        // a single null is [null]
        val ids: Array<Object?> = edgeIds ?: arrayOf(null)
        val clone = clone()
        clone.bytecode.addStep(GraphTraversal.Symbols.E, ids)
        val traversal: GraphTraversal.Admin<Edge, Edge> = DefaultGraphTraversal(clone)
        return traversal.addStep(GraphStep(traversal, Edge::class.java, true, ids))
    }

    /**
     * Performs a read or write based operation on the [Graph] backing this `GraphTraversalSource`. This
     * step can be accompanied by the [GraphTraversal.with] modulator for further configuration
     * and must be accompanied by a [GraphTraversal.read] or [GraphTraversal.write] modulator step
     * which will terminate the traversal.
     *
     * @param file the name of file for which the read or write will apply - note that the context of how this
     * parameter is used is wholly dependent on the implementation
     * @return the traversal with the [IoStep] added
     * @see [Reference Documentation - IO Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.io-step)
     *
     * @see [Reference Documentation - Read Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.read-step)
     *
     * @see [Reference Documentation - Write Step](http://tinkerpop.apache.org/docs/${project.version}/reference/.write-step)
     *
     * @since 3.4.0
     */
    fun <S> io(file: String?): GraphTraversal<S, S> {
        val clone = clone()
        clone.bytecode.addStep(GraphTraversal.Symbols.io, file)
        val traversal: GraphTraversal.Admin<S, S> = DefaultGraphTraversal(clone)
        return traversal.addStep(IoStep<S>(traversal, file))
    }

    /**
     * Proxies calls through to the underlying [Graph.tx] or to the [RemoteConnection.tx].
     */
    fun tx(): Transaction? {
        return if (null == connection) graph.tx() else {
            // prevent child transactions and let the current Transaction object be bound to the
            // TraversalSource that spawned it
            val tx: Transaction = connection.tx()
            if (tx === Transaction.NO_OP && connection is Transaction) connection as Transaction? else tx
        }
    }

    /**
     * If there is an underlying [RemoteConnection] it will be closed by this method.
     */
    @Override
    @Throws(Exception::class)
    fun close() {
        if (connection != null) connection.close()
    }

    @Override
    override fun toString(): String {
        return StringFactory.traversalSourceString(this)
    }
}