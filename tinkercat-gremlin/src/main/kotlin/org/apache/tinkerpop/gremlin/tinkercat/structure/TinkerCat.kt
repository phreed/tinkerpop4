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
package org.apache.tinkerpop.gremlin.tinkercat.structure

import org.apache.commons.configuration2.BaseConfiguration
import org.apache.commons.configuration2.Configuration
import org.apache.tinkerpop.gremlin.tinkercat.process.traversal.strategy.optimization.TinkerCatStepStrategy.Companion.instance
import org.apache.tinkerpop.gremlin.tinkercat.process.traversal.strategy.optimization.TinkerCatCountStrategy.Companion.instance
import org.apache.tinkerpop.gremlin.tinkercat.process.computer.TinkerCatComputerView.legalVertex
import org.apache.tinkerpop.gremlin.tinkercat.process.computer.TinkerCatComputerView.legalEdge
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerCat.TinkerCatFeatures
import java.util.concurrent.ConcurrentHashMap
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerCatVariables
import org.apache.tinkerpop.gremlin.tinkercat.process.computer.TinkerCatComputerView
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerCat
import java.lang.IllegalStateException
import org.apache.tinkerpop.gremlin.structure.util.ElementHelper
import org.apache.tinkerpop.gremlin.process.computer.GraphComputer
import org.apache.tinkerpop.gremlin.tinkercat.process.computer.TinkerCatComputer
import org.apache.tinkerpop.gremlin.structure.io.Io
import org.apache.tinkerpop.gremlin.structure.io.gryo.GryoVersion
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONVersion
import org.apache.tinkerpop.gremlin.structure.util.StringFactory
import org.apache.tinkerpop.gremlin.structure.io.graphml.GraphMLIo
import org.apache.tinkerpop.gremlin.structure.io.IoCore
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONIo
import org.apache.tinkerpop.gremlin.structure.io.gryo.GryoIo
import java.lang.RuntimeException
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerCatIterator
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerCat.TinkerCatGraphFeatures
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerCat.TinkerCatEdgeFeatures
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerCat.TinkerCatVertexFeatures
import org.apache.tinkerpop.gremlin.structure.Graph.Features.GraphFeatures
import org.apache.tinkerpop.gremlin.structure.Graph.Features.EdgeFeatures
import org.apache.tinkerpop.gremlin.structure.Graph.Features.VertexFeatures
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerCat.TinkerCatVertexPropertyFeatures
import org.apache.tinkerpop.gremlin.structure.Graph.Features.VertexPropertyFeatures
import java.lang.IllegalArgumentException
import java.lang.NumberFormatException
import org.apache.tinkerpop.gremlin.process.traversal.TraversalStrategies
import org.apache.tinkerpop.gremlin.structure.*
import org.apache.tinkerpop.gremlin.structure.io.Mapper
import org.apache.tinkerpop.gremlin.tinkercat.process.traversal.strategy.optimization.TinkerCatStepStrategy
import org.apache.tinkerpop.gremlin.tinkercat.process.traversal.strategy.optimization.TinkerCatCountStrategy
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils
import java.io.File
import java.lang.Exception
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import java.util.stream.Stream
import kotlin.jvm.JvmOverloads

/**
 * An in-memory (with optional persistence on calls to [.close]), reference implementation of the property
 * graph interfaces provided by TinkerPop.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
@Graph.OptIn(Graph.OptIn.SUITE_STRUCTURE_STANDARD)
@Graph.OptIn(Graph.OptIn.SUITE_STRUCTURE_INTEGRATE)
@Graph.OptIn(
    Graph.OptIn.SUITE_PROCESS_STANDARD
)
@Graph.OptIn(Graph.OptIn.SUITE_PROCESS_COMPUTER)
@Graph.OptIn(Graph.OptIn.SUITE_PROCESS_LIMITED_STANDARD)
@Graph.OptIn(
    Graph.OptIn.SUITE_PROCESS_LIMITED_COMPUTER
)
class TinkerCat private constructor(private val configuration: Configuration) : Graph {
    private val features = TinkerCatFeatures()
    protected var currentId = AtomicLong(-1L)
    var vertices: MutableMap<Any, Vertex> = ConcurrentHashMap()
    var edges: MutableMap<Any, Edge> = ConcurrentHashMap()
    protected var variables: TinkerCatVariables? = null
    @JvmField
    var graphComputerView: TinkerCatComputerView? = null
    @JvmField
    var vertexIndex: TinkerIndex<TinkerVertex>? = null
    @JvmField
    var edgeIndex: TinkerIndex<TinkerEdge>? = null
    protected val vertexIdManager: IdManager<*>
    @JvmField
    val edgeIdManager: IdManager<*>
    @JvmField
    val vertexPropertyIdManager: IdManager<*>
    protected val defaultVertexPropertyCardinality: VertexProperty.Cardinality
    protected val allowNullPropertyValues: Boolean
    private val graphLocation: String?
    private val graphFormat: String?

    /**
     * An empty private constructor that initializes [TinkerCat].
     */
    init {
        vertexIdManager = selectIdManager(configuration, GREMLIN_TINKERGRAPH_VERTEX_ID_MANAGER, Vertex::class.java)
        edgeIdManager = selectIdManager(configuration, GREMLIN_TINKERGRAPH_EDGE_ID_MANAGER, Edge::class.java)
        vertexPropertyIdManager =
            selectIdManager(configuration, GREMLIN_TINKERGRAPH_VERTEX_PROPERTY_ID_MANAGER, VertexProperty::class.java)
        defaultVertexPropertyCardinality = VertexProperty.Cardinality.valueOf(
            configuration.getString(
                GREMLIN_TINKERGRAPH_DEFAULT_VERTEX_PROPERTY_CARDINALITY,
                VertexProperty.Cardinality.single.name
            )
        )
        allowNullPropertyValues = configuration.getBoolean(GREMLIN_TINKERGRAPH_ALLOW_NULL_PROPERTY_VALUES, false)
        graphLocation = configuration.getString(GREMLIN_TINKERGRAPH_GRAPH_LOCATION, null)
        graphFormat = configuration.getString(GREMLIN_TINKERGRAPH_GRAPH_FORMAT, null)
        check(!(graphLocation != null && null == graphFormat || null == graphLocation && graphFormat != null)) {
            String.format(
                "The %s and %s must both be specified if either is present",
                GREMLIN_TINKERGRAPH_GRAPH_LOCATION, GREMLIN_TINKERGRAPH_GRAPH_FORMAT
            )
        }
        if (graphLocation != null) loadGraph()
    }

    ////////////// STRUCTURE API METHODS //////////////////
    override fun addVertex(vararg keyValues: Any): Vertex {
        ElementHelper.legalPropertyKeyValueArray(*keyValues)
        var idValue = vertexIdManager.convert(ElementHelper.getIdValue(*keyValues).orElse(null))
        val label = ElementHelper.getLabelValue(*keyValues).orElse(Vertex.DEFAULT_LABEL)
        if (null != idValue) {
            if (vertices.containsKey(idValue)) throw Graph.Exceptions.vertexWithIdAlreadyExists(idValue)
        } else {
            idValue = vertexIdManager.getNextId(this)
        }
        val vertex: Vertex = TinkerVertex(idValue, label, this)
        vertices[vertex.id()] = vertex
        ElementHelper.attachProperties(vertex, VertexProperty.Cardinality.list, *keyValues)
        return vertex
    }

    override fun <C : GraphComputer?> compute(graphComputerClass: Class<C>): C {
        if (graphComputerClass != TinkerCatComputer::class.java) throw Graph.Exceptions.graphDoesNotSupportProvidedGraphComputer(
            graphComputerClass
        )
        return TinkerCatComputer(this) as C
    }

    override fun compute(): GraphComputer {
        return TinkerCatComputer(this)
    }

    override fun variables(): Graph.Variables {
        if (null == variables) variables = TinkerCatVariables()
        return variables!!
    }

    override fun <I : Io<*, *, *>?> io(builder: Io.Builder<I>): I {
        return if (builder.requiresVersion(GryoVersion.V1_0) || builder.requiresVersion(GraphSONVersion.V1_0)) builder.graph(
            this
        ).onMapper { mapper: Mapper.Builder<*> ->
            mapper.addRegistry(
                TinkerIoRegistryV1d0.instance()
            )
        }
            .create() as I else if (builder.requiresVersion(GraphSONVersion.V2_0)) // there is no gryo v2
            builder.graph(this)
                .onMapper { mapper: Mapper.Builder<*> -> mapper.addRegistry(TinkerIoRegistryV2d0.instance()) }
                .create() as I else builder.graph(this).onMapper { mapper: Mapper.Builder<*> ->
            mapper.addRegistry(
                TinkerIoRegistryV3d0.instance()
            )
        }
            .create() as I
    }

    override fun toString(): String {
        return StringFactory.graphString(this, "vertices:" + vertices.size + " edges:" + edges.size)
    }

    fun clear() {
        vertices.clear()
        edges.clear()
        variables = null
        currentId.set(-1L)
        vertexIndex = null
        edgeIndex = null
        graphComputerView = null
    }

    /**
     * This method only has an effect if the [.GREMLIN_TINKERGRAPH_GRAPH_LOCATION] is set, in which case the
     * data in the graph is persisted to that location. This method may be called multiple times and does not release
     * resources.
     */
    override fun close() {
        if (graphLocation != null) saveGraph()
    }

    override fun tx(): Transaction {
        throw Graph.Exceptions.transactionsNotSupported()
    }

    override fun configuration(): Configuration {
        return configuration
    }

    override fun vertices(vararg vertexIds: Any): Iterator<Vertex> {
        return createElementIterator(Vertex::class.java, vertices, vertexIdManager, *vertexIds)
    }

    override fun edges(vararg edgeIds: Any): Iterator<Edge> {
        return createElementIterator(Edge::class.java, edges, edgeIdManager, *edgeIds)
    }

    private fun loadGraph() {
        val f = File(graphLocation)
        if (f.exists() && f.isFile) {
            try {
                if (graphFormat == "graphml") {
                    io(IoCore.graphml()).readGraph(graphLocation)
                } else if (graphFormat == "graphson") {
                    io(IoCore.graphson()).readGraph(graphLocation)
                } else if (graphFormat == "gryo") {
                    io(IoCore.gryo()).readGraph(graphLocation)
                } else {
                    io(IoCore.createIoBuilder(graphFormat)).readGraph(graphLocation)
                }
            } catch (ex: Exception) {
                throw RuntimeException(
                    String.format("Could not load graph at %s with %s", graphLocation, graphFormat),
                    ex
                )
            }
        }
    }

    private fun saveGraph() {
        val f = File(graphLocation)
        if (f.exists()) {
            f.delete()
        } else {
            val parent = f.parentFile

            // the parent would be null in the case of an relative path if the graphLocation was simply: "f.gryo"
            if (parent != null && !parent.exists()) {
                parent.mkdirs()
            }
        }
        try {
            if (graphFormat == "graphml") {
                io(IoCore.graphml()).writeGraph(graphLocation)
            } else if (graphFormat == "graphson") {
                io(IoCore.graphson()).writeGraph(graphLocation)
            } else if (graphFormat == "gryo") {
                io(IoCore.gryo()).writeGraph(graphLocation)
            } else {
                io(IoCore.createIoBuilder(graphFormat)).writeGraph(graphLocation)
            }
        } catch (ex: Exception) {
            throw RuntimeException(String.format("Could not save graph at %s with %s", graphLocation, graphFormat), ex)
        }
    }

    private fun <T : Element?> createElementIterator(
        clazz: Class<T>, elements: Map<Any, T>,
        idManager: IdManager<*>,
        vararg ids: Any
    ): Iterator<T?> {
        val iterator: Iterator<T>
        iterator = if (0 == ids.size) {
            TinkerCatIterator(elements.values.iterator())
        } else {
            val idList = Arrays.asList(*ids)

            // TinkerCat can take a Vertex/Edge or any object as an "id". If it is an Element then we just cast
            // to that type and pop off the identifier. there is no need to pass that through the IdManager since
            // the assumption is that if it's already an Element, its identifier must be valid to the Graph and to
            // its associated IdManager. All other objects are passed to the IdManager for conversion.
            return TinkerCatIterator(IteratorUtils.filter(IteratorUtils.map(idList) { id: Any? ->
                // ids cant be null so all of those filter out
                if (null == id) return@map null
                val iid = (if (clazz.isAssignableFrom(id.javaClass)) clazz.cast(id)!!.id() else idManager.convert(id))!!
                elements[idManager.convert(iid)]
            }.iterator()) { obj: T? -> Objects.nonNull(obj) })
        }
        return if (TinkerHelper.inComputerMode(this)) (if (clazz == Vertex::class.java) IteratorUtils.filter(iterator as Iterator<Vertex>) { t: Vertex? ->
            graphComputerView!!.legalVertex(
                t!!
            )
        } else IteratorUtils.filter(iterator as Iterator<Edge>) { t: Edge ->
            graphComputerView!!.legalEdge(
                t.outVertex(),
                t
            )
        }) as Iterator<T> else iterator
    }

    /**
     * Return TinkerCat feature set.
     *
     *
     * **Reference Implementation Help:** Implementers only need to implement features for which there are
     * negative or instance configured features.  By default, all [Graph.Features] return true.
     */
    override fun features(): Graph.Features {
        return features
    }

    inner class TinkerCatFeatures() : Graph.Features {
        private val graphFeatures = TinkerCatGraphFeatures()
        private val edgeFeatures = TinkerCatEdgeFeatures()
        private val vertexFeatures = TinkerCatVertexFeatures()
        override fun graph(): GraphFeatures {
            return graphFeatures
        }

        override fun edge(): EdgeFeatures {
            return edgeFeatures
        }

        override fun vertex(): VertexFeatures {
            return vertexFeatures
        }

        override fun toString(): String {
            return StringFactory.featureString(this)
        }
    }

    inner class TinkerCatVertexFeatures private constructor() : VertexFeatures {
        private val vertexPropertyFeatures = TinkerCatVertexPropertyFeatures()
        override fun supportsNullPropertyValues(): Boolean {
            return allowNullPropertyValues
        }

        override fun properties(): VertexPropertyFeatures {
            return vertexPropertyFeatures
        }

        override fun supportsCustomIds(): Boolean {
            return false
        }

        override fun willAllowId(id: Any): Boolean {
            return vertexIdManager.allow(id)
        }

        override fun getCardinality(key: String): VertexProperty.Cardinality {
            return defaultVertexPropertyCardinality
        }
    }

    inner class TinkerCatEdgeFeatures private constructor() : EdgeFeatures {
        override fun supportsNullPropertyValues(): Boolean {
            return allowNullPropertyValues
        }

        override fun supportsCustomIds(): Boolean {
            return false
        }

        override fun willAllowId(id: Any): Boolean {
            return edgeIdManager.allow(id)
        }
    }

    inner class TinkerCatGraphFeatures private constructor() : GraphFeatures {
        override fun supportsConcurrentAccess(): Boolean {
            return false
        }

        override fun supportsTransactions(): Boolean {
            return false
        }

        override fun supportsThreadedTransactions(): Boolean {
            return false
        }
    }

    inner class TinkerCatVertexPropertyFeatures private constructor() : VertexPropertyFeatures {
        override fun supportsNullPropertyValues(): Boolean {
            return allowNullPropertyValues
        }

        override fun supportsCustomIds(): Boolean {
            return false
        }

        override fun willAllowId(id: Any): Boolean {
            return vertexIdManager.allow(id)
        }
    }
    ///////////// GRAPH SPECIFIC INDEXING METHODS ///////////////
    /**
     * Create an index for said element class ([Vertex] or [Edge]) and said property key.
     * Whenever an element has the specified key mutated, the index is updated.
     * When the index is created, all existing elements are indexed to ensure that they are captured by the index.
     *
     * @param key          the property key to index
     * @param elementClass the element class to index
     * @param <E>          The type of the element class
    </E> */
    fun <E : Element?> createIndex(key: String?, elementClass: Class<E>) {
        if (Vertex::class.java.isAssignableFrom(elementClass)) {
            if (null == vertexIndex) vertexIndex = TinkerIndex(this, TinkerVertex::class.java)
            vertexIndex!!.createKeyIndex(key)
        } else if (Edge::class.java.isAssignableFrom(elementClass)) {
            if (null == edgeIndex) edgeIndex = TinkerIndex(this, TinkerEdge::class.java)
            edgeIndex!!.createKeyIndex(key)
        } else {
            throw IllegalArgumentException("Class is not indexable: $elementClass")
        }
    }

    /**
     * Drop the index for the specified element class ([Vertex] or [Edge]) and key.
     *
     * @param key          the property key to stop indexing
     * @param elementClass the element class of the index to drop
     * @param <E>          The type of the element class
    </E> */
    fun <E : Element?> dropIndex(key: String?, elementClass: Class<E>) {
        if (Vertex::class.java.isAssignableFrom(elementClass)) {
            if (null != vertexIndex) vertexIndex!!.dropKeyIndex(key)
        } else if (Edge::class.java.isAssignableFrom(elementClass)) {
            if (null != edgeIndex) edgeIndex!!.dropKeyIndex(key)
        } else {
            throw IllegalArgumentException("Class is not indexable: $elementClass")
        }
    }

    /**
     * Return all the keys currently being index for said element class  ([Vertex] or [Edge]).
     *
     * @param elementClass the element class to get the indexed keys for
     * @param <E>          The type of the element class
     * @return the set of keys currently being indexed
    </E> */
    fun <E : Element?> getIndexedKeys(elementClass: Class<E>): Set<String> {
        return if (Vertex::class.java.isAssignableFrom(elementClass)) {
            if (null == vertexIndex) emptySet() else vertexIndex!!.indexedKeys
        } else if (Edge::class.java.isAssignableFrom(elementClass)) {
            if (null == edgeIndex) emptySet() else edgeIndex!!.indexedKeys
        } else {
            throw IllegalArgumentException("Class is not indexable: $elementClass")
        }
    }

    /**
     * TinkerCat will use an implementation of this interface to generate identifiers when a user does not supply
     * them and to handle identifier conversions when querying to provide better flexibility with respect to
     * handling different data types that mean the same thing.  For example, the
     * [DefaultIdManager.LONG] implementation will allow `g.vertices(1l, 2l)` and
     * `g.vertices(1, 2)` to both return values.
     *
     * @param <T> the id type
    </T> */
    interface IdManager<T> {
        /**
         * Generate an identifier which should be unique to the [TinkerCat] instance.
         */
        fun getNextId(graph: TinkerCat): T

        /**
         * Convert an identifier to the type required by the manager.
         */
        fun convert(id: Any?): T?

        /**
         * Determine if an identifier is allowed by this manager given its type.
         */
        fun allow(id: Any?): Boolean
    }

    /**
     * A default set of [IdManager] implementations for common identifier types.
     */
    enum class DefaultIdManager : IdManager<Any?> {
        /**
         * Manages identifiers of type `Long`. Will convert any class that extends from [Number] to a
         * [Long] and will also attempt to convert `String` values
         */
        LONG {
            override fun getNextId(graph: TinkerCat): Long {
                return Stream.generate { graph.currentId.incrementAndGet() }
                    .filter { id: Long -> !graph.vertices.containsKey(id) && !graph.edges.containsKey(id) }
                    .findAny().get()
            }

            override fun convert(id: Any?): Any? {
                return if (null == id) null else id as? Long
                    ?: if (id is Number) id.toLong() else if (id is String) {
                        try {
                            id as String?. toLong ()
                        } catch (nfe: NumberFormatException) {
                            throw IllegalArgumentException(
                                createErrorMessage(
                                    Long::class.java, id
                                )
                            )
                        }
                    } else throw IllegalArgumentException(
                        createErrorMessage(
                            Long::class.java, id
                        )
                    )
            }

            override fun allow(id: Any?): Boolean {
                return id is Number || id is String
            }
        },

        /**
         * Manages identifiers of type `Integer`. Will convert any class that extends from [Number] to a
         * [Integer] and will also attempt to convert `String` values
         */
        INTEGER {
            override fun getNextId(graph: TinkerCat): Int {
                return Stream.generate { graph.currentId.incrementAndGet() }
                    .map { obj: Long -> obj.toInt() }
                    .filter { id: Int -> !graph.vertices.containsKey(id) && !graph.edges.containsKey(id) }
                    .findAny().get()
            }

            override fun convert(id: Any?): Any? {
                return if (null == id) null else id as? Int
                    ?: if (id is Number) id.toInt() else if (id is String) {
                        try {
                            id as String?. toInt ()
                        } catch (nfe: NumberFormatException) {
                            throw IllegalArgumentException(
                                createErrorMessage(
                                    Int::class.java, id
                                )
                            )
                        }
                    } else throw IllegalArgumentException(
                        createErrorMessage(
                            Int::class.java, id
                        )
                    )
            }

            override fun allow(id: Any?): Boolean {
                return id is Number || id is String
            }
        },

        /**
         * Manages identifiers of type `UUID`. Will convert `String` values to
         * `UUID`.
         */
        UUID {
            override fun getNextId(graph: TinkerCat): java.util.UUID {
                return java.util.UUID.randomUUID()
            }

            override fun convert(id: Any?): Any? {
                return if (null == id) null else id as? java.util.UUID
                    ?: if (id is String) {
                        try {
                            java.util.UUID.fromString(id as String?)
                        } catch (iae: IllegalArgumentException) {
                            throw IllegalArgumentException(
                                createErrorMessage(
                                    java.util.UUID::class.java, id
                                )
                            )
                        }
                    } else throw IllegalArgumentException(
                        createErrorMessage(
                            java.util.UUID::class.java, id
                        )
                    )
            }

            override fun allow(id: Any?): Boolean {
                return id is java.util.UUID || id is String
            }
        },

        /**
         * Manages identifiers of any type.  This represents the default way [TinkerCat] has always worked.
         * In other words, there is no identifier conversion so if the identifier of a vertex is a `Long`, then
         * trying to request it with an `Integer` will have no effect. Also, like the original
         * [TinkerCat], it will generate [Long] values for identifiers.
         */
        ANY {
            override fun getNextId(graph: TinkerCat): Long {
                return Stream.generate { graph.currentId.incrementAndGet() }
                    .filter { id: Long -> !graph.vertices.containsKey(id) && !graph.edges.containsKey(id) }
                    .findAny().get()
            }

            override fun convert(id: Any?): Any? {
                return id
            }

            override fun allow(id: Any?): Boolean {
                return true
            }
        };

        companion object {
            private fun createErrorMessage(expectedType: Class<*>, id: Any): String {
                return String.format(
                    "Expected an id that is convertible to %s but received %s - [%s]",
                    expectedType,
                    id.javaClass,
                    id
                )
            }
        }
    }

    companion object {
        init {
            TraversalStrategies.GlobalCache.registerStrategies(
                TinkerCat::class.java,
                TraversalStrategies.GlobalCache.getStrategies(Graph::class.java).clone().addStrategies(
                    TinkerCatStepStrategy.instance(),
                    TinkerCatCountStrategy.instance()
                )
            )
        }

        private val EMPTY_CONFIGURATION: Configuration = object : BaseConfiguration() {
            init {
                setProperty(Graph.GRAPH, TinkerCat::class.java.name)
            }
        }
        const val GREMLIN_TINKERGRAPH_VERTEX_ID_MANAGER = "gremlin.tinkercat.vertexIdManager"
        const val GREMLIN_TINKERGRAPH_EDGE_ID_MANAGER = "gremlin.tinkercat.edgeIdManager"
        const val GREMLIN_TINKERGRAPH_VERTEX_PROPERTY_ID_MANAGER = "gremlin.tinkercat.vertexPropertyIdManager"
        const val GREMLIN_TINKERGRAPH_DEFAULT_VERTEX_PROPERTY_CARDINALITY =
            "gremlin.tinkercat.defaultVertexPropertyCardinality"
        const val GREMLIN_TINKERGRAPH_GRAPH_LOCATION = "gremlin.tinkercat.graphLocation"
        const val GREMLIN_TINKERGRAPH_GRAPH_FORMAT = "gremlin.tinkercat.graphFormat"
        const val GREMLIN_TINKERGRAPH_ALLOW_NULL_PROPERTY_VALUES = "gremlin.tinkercat.allowNullPropertyValues"
        /**
         * Open a new `TinkerCat` instance.
         *
         *
         * **Reference Implementation Help:** This method is the one use by the [GraphFactory] to instantiate
         * [Graph] instances.  This method must be overridden for the Structure Test Suite to pass. Implementers have
         * latitude in terms of how exceptions are handled within this method.  Such exceptions will be considered
         * implementation specific by the test suite as all test generate graph instances by way of
         * [GraphFactory]. As such, the exceptions get generalized behind that facade and since
         * [GraphFactory] is the preferred method to opening graphs it will be consistent at that level.
         *
         * @param configuration the configuration for the instance
         * @return a newly opened [Graph]
         */
        /**
         * Open a new [TinkerCat] instance.
         *
         *
         * **Reference Implementation Help:** If a [Graph] implementation does not require a `Configuration`
         * (or perhaps has a default configuration) it can choose to implement a zero argument
         * `open()` method. This is an optional constructor method for TinkerCat. It is not enforced by the Gremlin
         * Test Suite.
         */
        @JvmStatic
        @JvmOverloads
        fun open(configuration: Configuration = EMPTY_CONFIGURATION): TinkerCat {
            return TinkerCat(configuration)
        }

        /**
         * Construct an [TinkerCat.IdManager] from the TinkerCat `Configuration`.
         */
        private fun selectIdManager(config: Configuration, configKey: String, clazz: Class<out Element>): IdManager<*> {
            val vertexIdManagerConfigValue = config.getString(configKey, DefaultIdManager.ANY.name)
            return try {
                DefaultIdManager.valueOf(vertexIdManagerConfigValue)
            } catch (iae: IllegalArgumentException) {
                try {
                    Class.forName(vertexIdManagerConfigValue).newInstance() as IdManager<*>
                } catch (ex: Exception) {
                    throw IllegalStateException(
                        String.format(
                            "Could not configure TinkerCat %s id manager with %s",
                            clazz.simpleName,
                            vertexIdManagerConfigValue
                        )
                    )
                }
            }
        }
    }
}