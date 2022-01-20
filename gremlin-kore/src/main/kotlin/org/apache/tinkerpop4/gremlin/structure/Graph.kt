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
package org.apache.tinkerpop4.gremlin.structure

import org.apache.commons.configuration2.Configuration

/**
 * A [Graph] is a container object for a collection of [Vertex], [Edge], [VertexProperty],
 * and [Property] objects.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 * @author Pieter Martin
 */
interface Graph : AutoCloseable, Host {
    /**
     * This should only be used by providers to create keys, labels, etc. in a namespace safe from users.
     * Users are not allowed to generate property keys, step labels, etc. that are key'd "hidden".
     */
    object Hidden {
        /**
         * The prefix to denote that a key is a hidden key.
         */
        private const val HIDDEN_PREFIX = "~"
        private const val HIDDEN_PREFIX_LENGTH: Int = HIDDEN_PREFIX.length()

        /**
         * Turn the provided key into a hidden key. If the key is already a hidden key, return key.
         *
         * @param key The key to make a hidden key
         * @return The hidden key
         */
        fun hide(key: String): String {
            return if (isHidden(key)) key else HIDDEN_PREFIX.concat(key)
        }

        /**
         * Turn the provided hidden key into an non-hidden key. If the key is not a hidden key, return key.
         *
         * @param key The hidden key
         * @return The non-hidden representation of the key
         */
        fun unHide(key: String): String {
            return if (isHidden(key)) key.substring(HIDDEN_PREFIX_LENGTH) else key
        }

        /**
         * Determines whether the provided key is a hidden key or not.
         *
         * @param key The key to check for hidden status
         * @return Whether the provided key is a hidden key or not
         */
        fun isHidden(key: String): Boolean {
            return key.startsWith(HIDDEN_PREFIX)
        }
    }

    /**
     * Add a [Vertex] to the graph given an optional series of key/value pairs.  These key/values
     * must be provided in an even number where the odd numbered arguments are [String] property keys and the
     * even numbered arguments are the related property values.
     *
     * @param keyValues The key/value pairs to turn into vertex properties
     * @return The newly created vertex
     */
    fun addVertex(vararg keyValues: Object?): Vertex?

    /**
     * Add a [Vertex] to the graph with provided vertex label.
     *
     * @param label the label of the vertex
     * @return The newly created labeled vertex
     */
    fun addVertex(label: String?): Vertex? {
        return this.addVertex(T.label, label)
    }

    /**
     * Declare the [GraphComputer] to use for OLAP operations on the graph.
     * If the graph does not support graph computer then an `UnsupportedOperationException` is thrown.
     *
     * @param graphComputerClass The graph computer class to use.
     * @return A graph computer for processing this graph
     * @throws IllegalArgumentException if the provided [GraphComputer] class is not supported.
     */
    @Throws(IllegalArgumentException::class)
    fun <C : GraphComputer?> compute(graphComputerClass: Class<C>?): C

    /**
     * Generate a [GraphComputer] using the default engine of the underlying graph system.
     * This is a shorthand method for the more involved method that uses [Graph.compute].
     *
     * @return A default graph computer
     * @throws IllegalArgumentException if there is no default graph computer
     */
    @Throws(IllegalArgumentException::class)
    fun compute(): GraphComputer?

    /**
     * Generate a [TraversalSource] using the specified `TraversalSource` class.
     * The reusable [TraversalSource] provides methods for spawning [Traversal] instances.
     *
     * @param traversalSourceClass The traversal source class
     * @param <C>                  The traversal source class
    </C> */
    fun <C : TraversalSource?> traversal(traversalSourceClass: Class<C>): C {
        return try {
            traversalSourceClass.getConstructor(Graph::class.java).newInstance(this)
        } catch (e: Exception) {
            throw IllegalStateException(e.getMessage(), e)
        }
    }

    /**
     * Generate a reusable [GraphTraversalSource] instance.
     * The [GraphTraversalSource] provides methods for creating [GraphTraversal] instances.
     *
     * @return A graph traversal source
     */
    fun traversal(): GraphTraversalSource? {
        return GraphTraversalSource(this)
    }

    /**
     * Get the [Vertex] objects in this graph with the provided vertex ids or [Vertex] objects themselves.
     * If no ids are provided, get all vertices.  Note that a vertex identifier does not need to correspond to the
     * actual id used in the graph.  It needs to be a bit more flexible than that in that given the
     * [Graph.Features] around id support, multiple arguments might be applicable here.
     *
     *
     * If the graph return `true` for [Features.VertexFeatures.supportsNumericIds] then it should support
     * filters as with:
     *
     *  * g.vertices(v)
     *  * g.vertices(v.id())
     *  * g.vertices(1)
     *  * g.vertices(1L)
     *  * g.vertices(1.0d)
     *  * g.vertices(1.0f)
     *  * g.vertices("1")
     *
     *
     *
     * If the graph return `true` for [Features.VertexFeatures.supportsCustomIds] ()} then it should support
     * filters as with:
     *
     *  * g.vertices(v)
     *  * g.vertices(v.id())
     *  * g.vertices(v.id().toString())
     *
     *
     *
     * If the graph return `true` for [Features.VertexFeatures.supportsAnyIds] ()} then it should support
     * filters as with:
     *
     *  * g.vertices(v)
     *  * g.vertices(v.id())
     *
     *
     *                                                                                                          
     * If the graph return `true` for [Features.VertexFeatures.supportsStringIds] ()} then it should support
     * filters as with:
     *
     *  * g.vertices(v)
     *  * g.vertices(v.id().toString())
     *  * g.vertices("id")
     *
     *
     *
     * If the graph return `true` for [Features.EdgeFeatures.supportsStringIds] ()} then it should support
     * filters as with:
     *
     *  * g.vertices(v)
     *  * g.vertices(v.id().toString())
     *  * g.vertices("id")
     *
     *
     * @param vertexIds the ids of the vertices to get
     * @return an [Iterator] of vertices that match the provided vertex ids
     */
    fun vertices(vararg vertexIds: Object?): Iterator<Vertex?>?

    /**
     * Get the [Edge] objects in this graph with the provided edge ids or [Edge] objects. If no ids are
     * provided, get all edges. Note that an edge identifier does not need to correspond to the actual id used in the
     * graph.  It needs to be a bit more flexible than that in that given the [Graph.Features] around id support,
     * multiple arguments might be applicable here.
     *
     *
     * If the graph return `true` for [Features.EdgeFeatures.supportsNumericIds] then it should support
     * filters as with:
     *
     *  * g.edges(e)
     *  * g.edges(e.id())
     *  * g.edges(1)
     *  * g.edges(1L)
     *  * g.edges(1.0d)
     *  * g.edges(1.0f)
     *  * g.edges("1")
     *
     *
     *
     * If the graph return `true` for [Features.EdgeFeatures.supportsCustomIds] ()} then it should support
     * filters as with:
     *
     *  * g.edges(e)
     *  * g.edges(e.id())
     *  * g.edges(e.id().toString())
     *
     *
     *
     * If the graph return `true` for [Features.EdgeFeatures.supportsAnyIds] ()} then it should support
     * filters as with:
     *
     *  * g.edges(e)
     *  * g.edges(e.id())
     *
     *
     *
     * If the graph return `true` for [Features.EdgeFeatures.supportsStringIds] ()} then it should support
     * filters as with:
     *
     *  * g.edges(e)
     *  * g.edges(e.id().toString())
     *  * g.edges("id")
     *
     *
     * @param edgeIds the ids of the edges to get
     * @return an [Iterator] of edges that match the provided edge ids
     */
    fun edges(vararg edgeIds: Object?): Iterator<Edge?>?

    /**
     * Configure and control the transactions for those graphs that support this feature.
     */
    fun tx(): Transaction?

    /**
     * Configure and control the transactions for those graphs that support this feature. Graphs that support multiple
     * transaction models can use this method expose different sorts of [Transaction] implementations.
     */
    fun <Tx : Transaction?> tx(txClass: Class<Tx>?): Tx {
        throw UnsupportedOperationException("This Graph does not support multiple transaction types - use tx() instead")
    }

    /**
     * Closing a `Graph` is equivalent to "shutdown" and implies that no further operations can be executed on
     * the instance.  Users should consult the documentation of the underlying graph database implementation for what
     * this "shutdown" will mean in general and, if supported, how open transactions are handled.  It will typically
     * be the end user's responsibility to synchronize the thread that calls `close()` with other threads that
     * are accessing open transactions. In other words, be sure that all work performed on the `Graph` instance
     * is complete prior to calling this method.
     *
     *
     * TinkerPop does not enforce any particular semantics with respect to "shutdown". It is up to the graph provider
     * to decide what this method will do.
     */
    @Override
    @Throws(Exception::class)
    fun close()

    /**
     * Construct a particular [Io] implementation for reading and writing the `Graph` and other data.
     * End-users will "select" the [Io] implementation that they want to use by supplying the
     * [Io.Builder] that constructs it.  In this way, `Graph` vendors can supply their [IoRegistry]
     * to that builder thus allowing for custom serializers to be auto-configured into the [Io] instance.
     * Registering custom serializers is particularly useful for those  graphs that have complex types for
     * [Element] identifiers.
     *
     * For those graphs that do not need to register any custom serializers, the default implementation should suffice.
     * If the default is overridden, take care to register the current graph via the
     * [org.apache.tinkerpop4.gremlin.structure.io.Io.Builder.graph] method.
     *
     */
    @Deprecated
    @Deprecated(
        """As of release 3.4.0, partially replaced by {@link GraphTraversalSource#io(String)}. Notice
      {@link GraphTraversalSource#io(String)} doesn't support read operation from {@code java.io.InputStream}
      or write operation to {@code java.io.OutputStream}. Thus for readers or writers which need this functionality
      are safe to use this deprecated method. There is no intention to remove this method unless all the
      functionality is replaced by the `io` step of {@link GraphTraversalSource}."""
    )
    fun <I : Io?> io(builder: Io.Builder<I>): I {
        return builder.graph(this).create()
    }

    /**
     * A collection of global [Variables] associated with the graph.
     * Variables are used for storing metadata about the graph.
     *
     * @return The variables associated with this graph
     */
    fun variables(): Variables?

    /**
     * Get the `Configuration` associated with the construction of this graph. Whatever configuration was passed
     * to [GraphFactory.open] is what should be returned by this method.
     *
     * @return the configuration used during graph construction.
     */
    fun configuration(): Configuration?

    /**
     * Graph variables are a set of key/value pairs associated with the graph. The keys are String and the values
     * are Objects.
     */
    interface Variables {
        /**
         * Keys set for the available variables.
         */
        fun keys(): Set<String?>

        /**
         * Gets a variable.
         */
        operator fun <R> get(key: String?): Optional<R>?

        /**
         * Sets a variable.
         */
        operator fun set(key: String?, value: Object?)

        /**
         * Removes a variable.
         */
        fun remove(key: String?)

        /**
         * Gets the variables of the [Graph] as a `Map`.
         */
        fun asMap(): Map<String?, Object?>? {
            val map: Map<String, Object> = keys().stream()
                .map { key -> Pair.with(key, get(key).get()) }
                .collect(Collectors.toMap(Pair::getValue0, Pair::getValue1))
            return Collections.unmodifiableMap(map)
        }

        object Exceptions {
            fun variableKeyCanNotBeEmpty(): IllegalArgumentException {
                return IllegalArgumentException("Graph variable key can not be the empty string")
            }

            fun variableKeyCanNotBeNull(): IllegalArgumentException {
                return IllegalArgumentException("Graph variable key can not be null")
            }

            fun variableValueCanNotBeNull(): IllegalArgumentException {
                return IllegalArgumentException("Graph variable value can not be null")
            }

            fun dataTypeOfVariableValueNotSupported(`val`: Object): UnsupportedOperationException {
                return dataTypeOfVariableValueNotSupported(`val`, null)
            }

            fun dataTypeOfVariableValueNotSupported(
                `val`: Object,
                rootCause: Exception?
            ): UnsupportedOperationException {
                return UnsupportedOperationException(
                    String.format(
                        "Graph variable value [%s] is of type %s is not supported",
                        `val`,
                        `val`.getClass()
                    ), rootCause
                )
            }
        }
    }

    /**
     * Gets the [Features] exposed by the underlying `Graph` implementation.
     */
    fun features(): Features? {
        return object : Features {}
    }

    /**
     * An interface that represents the capabilities of a `Graph` implementation.  By default all methods
     * of features return `true` and it is up to implementers to disable feature they don't support.  Users
     * should check features prior to using various functions of TinkerPop to help ensure code portability
     * across implementations.  For example, a common usage would be to check if a graph supports transactions prior
     * to calling the commit method on [.tx].
     *
     *
     * As an additional notice to Graph Providers, feature methods will be used by the test suite to determine which
     * tests will be ignored and which will be executed, therefore proper setting of these features is essential to
     * maximizing the amount of testing performed by the suite. Further note, that these methods may be called by the
     * TinkerPop core code to determine what operations may be appropriately executed which will have impact on
     * features utilized by users.
     */
    interface Features {
        /**
         * Gets the features related to "graph" operation.
         */
        fun graph(): GraphFeatures {
            return object : GraphFeatures {}
        }

        /**
         * Gets the features related to "vertex" operation.
         */
        fun vertex(): VertexFeatures {
            return object : VertexFeatures {}
        }

        /**
         * Gets the features related to "edge" operation.
         */
        fun edge(): EdgeFeatures {
            return object : EdgeFeatures {}
        }

        /**
         * Features specific to a operations of a "graph".
         */
        interface GraphFeatures : FeatureSet {
            /**
             * Determines if the `Graph` implementation supports [GraphComputer] based processing.
             */
            @FeatureDescriptor(name = FEATURE_COMPUTER)
            fun supportsComputer(): Boolean {
                return true
            }

            /**
             * Determines if the `Graph` implementation supports persisting it's contents natively to disk.
             * This feature does not refer to every graph's ability to write to disk via the Gremlin IO packages
             * (.e.g. GraphML), unless the graph natively persists to disk via those options somehow.  For example,
             * TinkerGraph does not support this feature as it is a pure in-sideEffects graph.
             */
            @FeatureDescriptor(name = FEATURE_PERSISTENCE)
            fun supportsPersistence(): Boolean {
                return true
            }

            /**
             * Determines if the `Graph` implementation supports more than one connection to the same instance
             * at the same time.  For example, Neo4j embedded does not support this feature because concurrent
             * access to the same database files by multiple instances is not possible.  However, Neo4j HA could
             * support this feature as each new `Graph` instance coordinates with the Neo4j cluster allowing
             * multiple instances to operate on the same database.
             */
            @FeatureDescriptor(name = FEATURE_CONCURRENT_ACCESS)
            fun supportsConcurrentAccess(): Boolean {
                return true
            }

            /**
             * Determines if the `Graph` implementations supports transactions.
             */
            @FeatureDescriptor(name = FEATURE_TRANSACTIONS)
            fun supportsTransactions(): Boolean {
                return true
            }

            /**
             * Determines if the `Graph` implementation supports threaded transactions which allow a transaction
             * to be executed across multiple threads via [Transaction.createThreadedTx].
             */
            @FeatureDescriptor(name = FEATURE_THREADED_TRANSACTIONS)
            fun supportsThreadedTransactions(): Boolean {
                return true
            }

            /**
             * Determines if the `Graph` implementations supports read operations as executed with the
             * [GraphTraversalSource.io] step. Graph implementations will generally support this by
             * default as any graph that can support direct mutation through the Structure API will by default
             * accept data from the standard TinkerPop [GraphReader] implementations. However, some graphs like
             * `HadoopGraph` don't accept direct mutations but can still do reads from that `io()` step.
             */
            @FeatureDescriptor(name = FEATURE_IO_READ)
            fun supportsIoRead(): Boolean {
                return true
            }

            /**
             * Determines if the `Graph` implementations supports write operations as executed with the
             * [GraphTraversalSource.io] step. Graph implementations will generally support this by
             * default given the standard TinkerPop [GraphWriter] implementations. However, some graphs like
             * `HadoopGraph` will use a different approach to handle writes.
             */
            @FeatureDescriptor(name = FEATURE_IO_WRITE)
            fun supportsIoWrite(): Boolean {
                return true
            }

            /**
             * Gets the features related to "graph sideEffects" operation.
             */
            fun variables(): VariableFeatures {
                return object : VariableFeatures {}
            }

            companion object {
                const val FEATURE_COMPUTER = "Computer"
                const val FEATURE_TRANSACTIONS = "Transactions"
                const val FEATURE_PERSISTENCE = "Persistence"
                const val FEATURE_THREADED_TRANSACTIONS = "ThreadedTransactions"
                const val FEATURE_CONCURRENT_ACCESS = "ConcurrentAccess"
                const val FEATURE_IO_READ = "IoRead"
                const val FEATURE_IO_WRITE = "IoWrite"
            }
        }

        /**
         * Features that are related to [Vertex] operations.
         */
        interface VertexFeatures : ElementFeatures {
            /**
             * Gets the [VertexProperty.Cardinality] for a key.  By default, this method will return
             * [VertexProperty.Cardinality.list].  Implementations that employ a schema can consult it to
             * determine the [VertexProperty.Cardinality].  Those that do no have a schema can return their
             * default [VertexProperty.Cardinality] for every key.
             *
             *
             * Note that this method is primarily used by TinkerPop for internal usage and may not be suitable to
             * reliably determine the cardinality of a key. For some implementation it may offer little more than a
             * hint on the actual cardinality. Generally speaking it is likely best to drop down to the API of the
             * [Graph] implementation for any schema related queries.
             */
            fun getCardinality(key: String?): VertexProperty.Cardinality? {
                return VertexProperty.Cardinality.list
            }

            /**
             * Determines if a [Vertex] can be added to the `Graph`.
             */
            @FeatureDescriptor(name = FEATURE_ADD_VERTICES)
            fun supportsAddVertices(): Boolean {
                return true
            }

            /**
             * Determines if a [Vertex] can be removed from the `Graph`.
             */
            @FeatureDescriptor(name = FEATURE_REMOVE_VERTICES)
            fun supportsRemoveVertices(): Boolean {
                return true
            }

            /**
             * Determines if a [Vertex] can support multiple properties with the same key.
             */
            @FeatureDescriptor(name = FEATURE_MULTI_PROPERTIES)
            fun supportsMultiProperties(): Boolean {
                return true
            }

            /**
             * Determines if a [Vertex] can support non-unique values on the same key. For this value to be
             * `true`, then [.supportsMetaProperties] must also return true. By default this method,
             * just returns what [.supportsMultiProperties] returns.
             */
            @FeatureDescriptor(name = FEATURE_DUPLICATE_MULTI_PROPERTIES)
            fun supportsDuplicateMultiProperties(): Boolean {
                return supportsMultiProperties()
            }

            /**
             * Determines if a [Vertex] can support properties on vertex properties.  It is assumed that a
             * graph will support all the same data types for meta-properties that are supported for regular
             * properties.
             */
            @FeatureDescriptor(name = FEATURE_META_PROPERTIES)
            fun supportsMetaProperties(): Boolean {
                return true
            }

            /**
             * Determines if the `Graph` implementation uses upsert functionality as opposed to insert
             * functionality for [.addVertex]. This feature gives graph providers some flexibility as
             * to how graph mutations are treated. For graph providers, testing of this feature (as far as TinkerPop
             * is concerned) only covers graphs that can support user supplied identifiers as there is no other way
             * for TinkerPop to know what aspect of a vertex is unique to appropriately apply assertions. Graph
             * providers, especially those who support schema features, may have other methods for uniquely identifying
             * a vertex and should therefore resort to their own body of tests to validate this feature.
             */
            @FeatureDescriptor(name = FEATURE_UPSERT)
            fun supportsUpsert(): Boolean {
                return false
            }

            /**
             * Gets features related to "properties" on a [Vertex].
             */
            fun properties(): VertexPropertyFeatures {
                return object : VertexPropertyFeatures {}
            }

            companion object {
                const val FEATURE_ADD_VERTICES = "AddVertices"
                const val FEATURE_MULTI_PROPERTIES = "MultiProperties"
                const val FEATURE_DUPLICATE_MULTI_PROPERTIES = "DuplicateMultiProperties"
                const val FEATURE_META_PROPERTIES = "MetaProperties"
                const val FEATURE_REMOVE_VERTICES = "RemoveVertices"
                const val FEATURE_UPSERT = "Upsert"
            }
        }

        /**
         * Features that are related to [Edge] operations.
         */
        interface EdgeFeatures : ElementFeatures {
            /**
             * Determines if an [Edge] can be added to a `Vertex`.
             */
            @FeatureDescriptor(name = FEATURE_ADD_EDGES)
            fun supportsAddEdges(): Boolean {
                return true
            }

            /**
             * Determines if an [Edge] can be removed from a `Vertex`.
             */
            @FeatureDescriptor(name = FEATURE_REMOVE_EDGES)
            fun supportsRemoveEdges(): Boolean {
                return true
            }

            /**
             * Determines if the `Graph` implementation uses upsert functionality as opposed to insert
             * functionality for [Vertex.addEdge]. This feature gives graph providers
             * some flexibility as to how graph mutations are treated. For graph providers, testing of this feature
             * (as far as TinkerPop is concerned) only covers graphs that can support user supplied identifiers as
             * there is no other way for TinkerPop to know what aspect of a edge is unique to appropriately apply
             * assertions. Graph providers, especially those who support schema features, may have other methods for
             * uniquely identifying a edge and should therefore resort to their own body of tests to validate this
             * feature.
             */
            @FeatureDescriptor(name = FEATURE_UPSERT)
            fun supportsUpsert(): Boolean {
                return false
            }

            /**
             * Gets features related to "properties" on an [Edge].
             */
            fun properties(): EdgePropertyFeatures {
                return object : EdgePropertyFeatures {}
            }

            companion object {
                const val FEATURE_ADD_EDGES = "AddEdges"
                const val FEATURE_REMOVE_EDGES = "RemoveEdges"
                const val FEATURE_UPSERT = "Upsert"
            }
        }

        /**
         * Features that are related to [Element] objects.  This is a base interface.
         */
        interface ElementFeatures : FeatureSet {
            /**
             * Determines if an [Element] allows properties with `null` property values. In the event that
             * this value is `false`, the underlying graph must treat `null` as an indication to remove
             * the property.
             */
            @FeatureDescriptor(name = FEATURE_NULL_PROPERTY_VALUES)
            fun supportsNullPropertyValues(): Boolean {
                return true
            }

            /**
             * Determines if an [Element] allows properties to be added.  This feature is set independently from
             * supporting "data types" and refers to support of calls to [Element.property].
             */
            @FeatureDescriptor(name = FEATURE_ADD_PROPERTY)
            fun supportsAddProperty(): Boolean {
                return true
            }

            /**
             * Determines if an [Element] allows properties to be removed.
             */
            @FeatureDescriptor(name = FEATURE_REMOVE_PROPERTY)
            fun supportsRemoveProperty(): Boolean {
                return true
            }

            /**
             * Determines if an [Element] can have a user defined identifier.  Implementations that do not support
             * this feature will be expected to auto-generate unique identifiers.  In other words, if the [Graph]
             * allows `graph.addVertex(id,x)` to work and thus set the identifier of the newly added
             * [Vertex] to the value of `x` then this feature should return true.  In this case, `x`
             * is assumed to be an identifier data type that the [Graph] will accept.
             */
            @FeatureDescriptor(name = FEATURE_USER_SUPPLIED_IDS)
            fun supportsUserSuppliedIds(): Boolean {
                return true
            }

            /**
             * Determines if an [Element] has numeric identifiers as their internal representation. In other
             * words, if the value returned from [Element.id] is a numeric value then this method
             * should be return `true`.
             *
             *
             * Note that this feature is most generally used for determining the appropriate tests to execute in the
             * Gremlin Test Suite.
             */
            @FeatureDescriptor(name = FEATURE_NUMERIC_IDS)
            fun supportsNumericIds(): Boolean {
                return true
            }

            /**
             * Determines if an [Element] has string identifiers as their internal representation. In other
             * words, if the value returned from [Element.id] is a string value then this method
             * should be return `true`.
             *
             *
             * Note that this feature is most generally used for determining the appropriate tests to execute in the
             * Gremlin Test Suite.
             */
            @FeatureDescriptor(name = FEATURE_STRING_IDS)
            fun supportsStringIds(): Boolean {
                return true
            }

            /**
             * Determines if an [Element] has UUID identifiers as their internal representation. In other
             * words, if the value returned from [Element.id] is a [UUID] value then this method
             * should be return `true`.
             *
             *
             * Note that this feature is most generally used for determining the appropriate tests to execute in the
             * Gremlin Test Suite.
             */
            @FeatureDescriptor(name = FEATURE_UUID_IDS)
            fun supportsUuidIds(): Boolean {
                return true
            }

            /**
             * Determines if an [Element] has a specific custom object as their internal representation.
             * In other words, if the value returned from [Element.id] is a type defined by the graph
             * implementations, such as OrientDB's `Rid`, then this method should be return `true`.
             *
             *
             * Note that this feature is most generally used for determining the appropriate tests to execute in the
             * Gremlin Test Suite.
             */
            @FeatureDescriptor(name = FEATURE_CUSTOM_IDS)
            fun supportsCustomIds(): Boolean {
                return true
            }

            /**
             * Determines if an [Element] any Java object is a suitable identifier. TinkerGraph is a good
             * example of a [Graph] that can support this feature, as it can use any [Object] as
             * a value for the identifier.
             *
             *
             * Note that this feature is most generally used for determining the appropriate tests to execute in the
             * Gremlin Test Suite. This setting should only return `true` if [.supportsUserSuppliedIds]
             * is `true`.
             */
            @FeatureDescriptor(name = FEATURE_ANY_IDS)
            fun supportsAnyIds(): Boolean {
                return true
            }

            /**
             * Determines if an identifier will be accepted by the [Graph].  This check is different than
             * what identifier internally supports as defined in methods like [.supportsNumericIds].  Those
             * refer to internal representation of the identifier.  A [Graph] may accept an identifier that
             * is not of those types and internally transform it to a native representation.
             *
             *
             * Note that this method only applies if [.supportsUserSuppliedIds] is `true`. Those that
             * return `false` for that method can immediately return false for this one as it allows no ids
             * of any type (it generates them all).
             *
             *
             * The default implementation will immediately return `false` if [.supportsUserSuppliedIds]
             * is `false`.  If custom identifiers are supported then it will throw an exception.  Those that
             * return `true` for [.supportsCustomIds] should override this method. If
             * [.supportsAnyIds] is `true` then the identifier will immediately be allowed.  Finally,
             * if any of the other types are supported, they will be typed checked against the class of the supplied
             * identifier.
             */
            fun willAllowId(id: Object?): Boolean {
                if (!supportsUserSuppliedIds()) return false
                if (supportsCustomIds()) throw UnsupportedOperationException("The default implementation is not capable of validating custom ids - please override")
                return (supportsAnyIds() || supportsStringIds() && id is String
                        || supportsNumericIds() && id is Number || supportsUuidIds() && id is UUID)
            }

            companion object {
                const val FEATURE_USER_SUPPLIED_IDS = "UserSuppliedIds"
                const val FEATURE_NUMERIC_IDS = "NumericIds"
                const val FEATURE_STRING_IDS = "StringIds"
                const val FEATURE_UUID_IDS = "UuidIds"
                const val FEATURE_CUSTOM_IDS = "CustomIds"
                const val FEATURE_ANY_IDS = "AnyIds"
                const val FEATURE_ADD_PROPERTY = "AddProperty"
                const val FEATURE_REMOVE_PROPERTY = "RemoveProperty"
                const val FEATURE_NULL_PROPERTY_VALUES = "NullPropertyValues"
            }
        }

        /**
         * Features that are related to [Vertex] [Property] objects.
         */
        interface VertexPropertyFeatures : PropertyFeatures {
            /**
             * Determines if meta-properties allow for `null` property values.
             */
            @FeatureDescriptor(name = FEATURE_NULL_PROPERTY_VALUES)
            fun supportsNullPropertyValues(): Boolean {
                return true
            }

            /**
             * Determines if a [VertexProperty] allows properties to be removed.
             */
            @FeatureDescriptor(name = FEATURE_REMOVE_PROPERTY)
            fun supportsRemoveProperty(): Boolean {
                return true
            }

            /**
             * Determines if a [VertexProperty] allows an identifier to be assigned to it.
             */
            @FeatureDescriptor(name = FEATURE_USER_SUPPLIED_IDS)
            fun supportsUserSuppliedIds(): Boolean {
                return true
            }

            /**
             * Determines if an [VertexProperty] has numeric identifiers as their internal representation.
             */
            @FeatureDescriptor(name = FEATURE_NUMERIC_IDS)
            fun supportsNumericIds(): Boolean {
                return true
            }

            /**
             * Determines if an [VertexProperty] has string identifiers as their internal representation.
             */
            @FeatureDescriptor(name = FEATURE_STRING_IDS)
            fun supportsStringIds(): Boolean {
                return true
            }

            /**
             * Determines if an [VertexProperty] has UUID identifiers as their internal representation.
             */
            @FeatureDescriptor(name = FEATURE_UUID_IDS)
            fun supportsUuidIds(): Boolean {
                return true
            }

            /**
             * Determines if an [VertexProperty] has a specific custom object as their internal representation.
             */
            @FeatureDescriptor(name = FEATURE_CUSTOM_IDS)
            fun supportsCustomIds(): Boolean {
                return true
            }

            /**
             * Determines if an [VertexProperty] any Java object is a suitable identifier.  Note that this
             * setting can only return true if [.supportsUserSuppliedIds] is true.
             */
            @FeatureDescriptor(name = FEATURE_ANY_IDS)
            fun supportsAnyIds(): Boolean {
                return true
            }

            /**
             * Determines if an identifier will be accepted by the [Graph].  This check is different than
             * what identifier internally supports as defined in methods like [.supportsNumericIds].  Those
             * refer to internal representation of the identifier.  A [Graph] may accept an identifier that
             * is not of those types and internally transform it to a native representation.
             *
             *
             * Note that this method only applies if [.supportsUserSuppliedIds] is `true`. Those that
             * return `false` for that method can immediately return false for this one as it allows no ids
             * of any type (it generates them all).
             *
             *
             * The default implementation will immediately return `false` if [.supportsUserSuppliedIds]
             * is `false`.  If custom identifiers are supported then it will throw an exception.  Those that
             * return `true` for [.supportsCustomIds] should override this method. If
             * [.supportsAnyIds] is `true` then the identifier will immediately be allowed.  Finally,
             * if any of the other types are supported, they will be typed checked against the class of the supplied
             * identifier.
             */
            fun willAllowId(id: Object?): Boolean {
                if (!supportsUserSuppliedIds()) return false
                if (supportsCustomIds()) throw UnsupportedOperationException("The default implementation is not capable of validating custom ids - please override")
                return (supportsAnyIds() || supportsStringIds() && id is String
                        || supportsNumericIds() && id is Number || supportsUuidIds() && id is UUID)
            }

            companion object {
                const val FEATURE_REMOVE_PROPERTY = "RemoveProperty"
                const val FEATURE_USER_SUPPLIED_IDS = "UserSuppliedIds"
                const val FEATURE_NUMERIC_IDS = "NumericIds"
                const val FEATURE_STRING_IDS = "StringIds"
                const val FEATURE_UUID_IDS = "UuidIds"
                const val FEATURE_CUSTOM_IDS = "CustomIds"
                const val FEATURE_ANY_IDS = "AnyIds"
                const val FEATURE_NULL_PROPERTY_VALUES = "NullPropertyValues"
            }
        }

        /**
         * Features that are related to [Edge] [Property] objects.
         */
        interface EdgePropertyFeatures : PropertyFeatures

        /**
         * A base interface for [Edge] or [Vertex] [Property] features.
         */
        interface PropertyFeatures : DataTypeFeatures {
            /**
             * Determines if an [Element] allows for the processing of at least one data type defined by the
             * features.  In this case "processing" refers to at least "reading" the data type. If any of the
             * features on [PropertyFeatures] is true then this value must be true.
             */
            @FeatureDescriptor(name = FEATURE_PROPERTIES)
            fun supportsProperties(): Boolean {
                return (supportsBooleanValues() || supportsByteValues() || supportsDoubleValues() || supportsFloatValues()
                        || supportsIntegerValues() || supportsLongValues() || supportsMapValues()
                        || supportsMixedListValues() || supportsSerializableValues()
                        || supportsStringValues() || supportsUniformListValues() || supportsBooleanArrayValues()
                        || supportsByteArrayValues() || supportsDoubleArrayValues() || supportsFloatArrayValues()
                        || supportsIntegerArrayValues() || supportsLongArrayValues() || supportsStringArrayValues())
            }

            companion object {
                const val FEATURE_PROPERTIES = "Properties"
            }
        }

        /**
         * Features for [Graph.Variables].
         */
        interface VariableFeatures : DataTypeFeatures {
            /**
             * If any of the features on [VariableFeatures] is `true` then this value must be `true`.
             */
            @FeatureDescriptor(name = FEATURE_VARIABLES)
            fun supportsVariables(): Boolean {
                return (supportsBooleanValues() || supportsByteValues() || supportsDoubleValues() || supportsFloatValues()
                        || supportsIntegerValues() || supportsLongValues() || supportsMapValues()
                        || supportsMixedListValues() || supportsSerializableValues()
                        || supportsStringValues() || supportsUniformListValues() || supportsBooleanArrayValues()
                        || supportsByteArrayValues() || supportsDoubleArrayValues() || supportsFloatArrayValues()
                        || supportsIntegerArrayValues() || supportsLongArrayValues() || supportsStringArrayValues())
            }

            companion object {
                const val FEATURE_VARIABLES = "Variables"
            }
        }

        /**
         * Base interface for features that relate to supporting different data types.
         */
        interface DataTypeFeatures : FeatureSet {
            /**
             * Supports setting of a boolean value.
             */
            @FeatureDescriptor(name = FEATURE_BOOLEAN_VALUES)
            fun supportsBooleanValues(): Boolean {
                return true
            }

            /**
             * Supports setting of a byte value.
             */
            @FeatureDescriptor(name = FEATURE_BYTE_VALUES)
            fun supportsByteValues(): Boolean {
                return true
            }

            /**
             * Supports setting of a double value.
             */
            @FeatureDescriptor(name = FEATURE_DOUBLE_VALUES)
            fun supportsDoubleValues(): Boolean {
                return true
            }

            /**
             * Supports setting of a float value.
             */
            @FeatureDescriptor(name = FEATURE_FLOAT_VALUES)
            fun supportsFloatValues(): Boolean {
                return true
            }

            /**
             * Supports setting of a integer value.
             */
            @FeatureDescriptor(name = FEATURE_INTEGER_VALUES)
            fun supportsIntegerValues(): Boolean {
                return true
            }

            /**
             * Supports setting of a long value.
             */
            @FeatureDescriptor(name = FEATURE_LONG_VALUES)
            fun supportsLongValues(): Boolean {
                return true
            }

            /**
             * Supports setting of a `Map` value.  The assumption is that the `Map` can contain
             * arbitrary serializable values that may or may not be defined as a feature itself.
             */
            @FeatureDescriptor(name = FEATURE_MAP_VALUES)
            fun supportsMapValues(): Boolean {
                return true
            }

            /**
             * Supports setting of a `List` value.  The assumption is that the `List` can contain
             * arbitrary serializable values that may or may not be defined as a feature itself.  As this
             * `List` is "mixed" it does not need to contain objects of the same type.
             *
             * @see .supportsMixedListValues
             */
            @FeatureDescriptor(name = FEATURE_MIXED_LIST_VALUES)
            fun supportsMixedListValues(): Boolean {
                return true
            }

            /**
             * Supports setting of an array of boolean values.
             */
            @FeatureDescriptor(name = FEATURE_BOOLEAN_ARRAY_VALUES)
            fun supportsBooleanArrayValues(): Boolean {
                return true
            }

            /**
             * Supports setting of an array of byte values.
             */
            @FeatureDescriptor(name = FEATURE_BYTE_ARRAY_VALUES)
            fun supportsByteArrayValues(): Boolean {
                return true
            }

            /**
             * Supports setting of an array of double values.
             */
            @FeatureDescriptor(name = FEATURE_DOUBLE_ARRAY_VALUES)
            fun supportsDoubleArrayValues(): Boolean {
                return true
            }

            /**
             * Supports setting of an array of float values.
             */
            @FeatureDescriptor(name = FEATURE_FLOAT_ARRAY_VALUES)
            fun supportsFloatArrayValues(): Boolean {
                return true
            }

            /**
             * Supports setting of an array of integer values.
             */
            @FeatureDescriptor(name = FEATURE_INTEGER_ARRAY_VALUES)
            fun supportsIntegerArrayValues(): Boolean {
                return true
            }

            /**
             * Supports setting of an array of string values.
             */
            @FeatureDescriptor(name = FEATURE_STRING_ARRAY_VALUES)
            fun supportsStringArrayValues(): Boolean {
                return true
            }

            /**
             * Supports setting of an array of long values.
             */
            @FeatureDescriptor(name = FEATURE_LONG_ARRAY_VALUES)
            fun supportsLongArrayValues(): Boolean {
                return true
            }

            /**
             * Supports setting of a Java serializable value.
             */
            @FeatureDescriptor(name = FEATURE_SERIALIZABLE_VALUES)
            fun supportsSerializableValues(): Boolean {
                return true
            }

            /**
             * Supports setting of a string value.
             */
            @FeatureDescriptor(name = FEATURE_STRING_VALUES)
            fun supportsStringValues(): Boolean {
                return true
            }

            /**
             * Supports setting of a `List` value.  The assumption is that the `List` can contain
             * arbitrary serializable values that may or may not be defined as a feature itself.  As this
             * `List` is "uniform" it must contain objects of the same type.
             *
             * @see .supportsMixedListValues
             */
            @FeatureDescriptor(name = FEATURE_UNIFORM_LIST_VALUES)
            fun supportsUniformListValues(): Boolean {
                return true
            }

            companion object {
                const val FEATURE_BOOLEAN_VALUES = "BooleanValues"
                const val FEATURE_BYTE_VALUES = "ByteValues"
                const val FEATURE_DOUBLE_VALUES = "DoubleValues"
                const val FEATURE_FLOAT_VALUES = "FloatValues"
                const val FEATURE_INTEGER_VALUES = "IntegerValues"
                const val FEATURE_LONG_VALUES = "LongValues"
                const val FEATURE_MAP_VALUES = "MapValues"
                const val FEATURE_MIXED_LIST_VALUES = "MixedListValues"
                const val FEATURE_BOOLEAN_ARRAY_VALUES = "BooleanArrayValues"
                const val FEATURE_BYTE_ARRAY_VALUES = "ByteArrayValues"
                const val FEATURE_DOUBLE_ARRAY_VALUES = "DoubleArrayValues"
                const val FEATURE_FLOAT_ARRAY_VALUES = "FloatArrayValues"
                const val FEATURE_INTEGER_ARRAY_VALUES = "IntegerArrayValues"
                const val FEATURE_LONG_ARRAY_VALUES = "LongArrayValues"
                const val FEATURE_SERIALIZABLE_VALUES = "SerializableValues"
                const val FEATURE_STRING_ARRAY_VALUES = "StringArrayValues"
                const val FEATURE_STRING_VALUES = "StringValues"
                const val FEATURE_UNIFORM_LIST_VALUES = "UniformListValues"
            }
        }

        /**
         * A marker interface to identify any set of Features. There is no need to implement this interface.
         */
        interface FeatureSet

        /**
         * Implementers should not override this method. Note that this method utilizes reflection to check for
         * feature support.
         */
        @Throws(NoSuchMethodException::class, IllegalAccessException::class, InvocationTargetException::class)
        fun supports(featureClass: Class<out FeatureSet?>, feature: String): Boolean {
            val instance: Object
            instance = if (featureClass.equals(GraphFeatures::class.java)) graph() else if (featureClass.equals(
                    VariableFeatures::class.java
                )
            ) graph().variables() else if (featureClass.equals(
                    VertexFeatures::class.java
                )
            ) vertex() else if (featureClass.equals(
                    VertexPropertyFeatures::class.java
                )
            ) vertex().properties() else if (featureClass.equals(
                    EdgeFeatures::class.java
                )
            ) edge() else if (featureClass.equals(EdgePropertyFeatures::class.java)) edge().properties() else if (featureClass.equals(
                    PropertyFeatures::class.java
                )
            ) throw IllegalArgumentException(
                String.format(
                    "Do not reference PropertyFeatures directly in tests, utilize a specific instance: %s, %s",
                    EdgePropertyFeatures::class.java, VertexPropertyFeatures::class.java
                )
            ) else throw IllegalArgumentException(
                String.format(
                    "Expecting featureClass to be a valid Feature instance and not %s", featureClass
                )
            )
            return featureClass.getMethod("supports$feature").invoke(instance)
        }
    }

    /**
     * Common exceptions to use with a graph.
     */
    object Exceptions {
        private val debug: Boolean =
            Boolean.parseBoolean(java.lang.System.getenv().getOrDefault("gremlin.structure.debug", "false"))

        fun variablesNotSupported(): UnsupportedOperationException {
            return UnsupportedOperationException("Graph does not support graph variables")
        }

        fun transactionsNotSupported(): UnsupportedOperationException {
            return UnsupportedOperationException("Graph does not support transactions")
        }

        fun graphComputerNotSupported(): UnsupportedOperationException {
            return UnsupportedOperationException("Graph does not support graph computer")
        }

        fun graphDoesNotSupportProvidedGraphComputer(graphComputerClass: Class): IllegalArgumentException {
            return IllegalArgumentException("Graph does not support the provided graph computer: " + graphComputerClass.getSimpleName())
        }

        fun vertexAdditionsNotSupported(): UnsupportedOperationException {
            return UnsupportedOperationException("Graph does not support adding vertices")
        }

        fun vertexWithIdAlreadyExists(id: Object?): IllegalArgumentException {
            return IllegalArgumentException(String.format("Vertex with id already exists: %s", id))
        }

        fun edgeWithIdAlreadyExists(id: Object?): IllegalArgumentException {
            return IllegalArgumentException(String.format("Edge with id already exists: %s", id))
        }

        fun argumentCanNotBeNull(argument: String?): IllegalArgumentException {
            return IllegalArgumentException(String.format("The provided argument can not be null: %s", argument))
        }
    }

    /**
     * Defines the test suite that the implementer has decided to support and represents publicly as "passing".
     * Marking the [Graph] instance with this class allows that particular test suite to run.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Repeatable(OptIns::class)
    @Inherited
    annotation class OptIn(
        /**
         * The test suite class to opt in to.
         */
        val value: String
    ) {
        companion object {
            var SUITE_STRUCTURE_STANDARD = "org.apache.tinkerpop4.gremlin.structure.StructureStandardSuite"
            var SUITE_STRUCTURE_INTEGRATE = "org.apache.tinkerpop4.gremlin.structure.StructureIntegrateSuite"
            var SUITE_PROCESS_COMPUTER = "org.apache.tinkerpop4.gremlin.process.ProcessComputerSuite"
            var SUITE_PROCESS_STANDARD = "org.apache.tinkerpop4.gremlin.process.ProcessStandardSuite"
            var SUITE_PROCESS_LIMITED_COMPUTER = "org.apache.tinkerpop4.gremlin.process.ProcessLimitedComputerSuite"
            var SUITE_PROCESS_LIMITED_STANDARD = "org.apache.tinkerpop4.gremlin.process.ProcessLimitedStandardSuite"
        }
    }

    /**
     * Holds a collection of [OptIn] enabling multiple [OptIn] to be applied to a
     * single suite.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Inherited
    annotation class OptIns(vararg val value: OptIn)

    /**
     * Defines a test in the suite that the implementer does not want to run.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Repeatable(OptOuts::class)
    @Inherited
    annotation class OptOut(
        /**
         * The test class to opt out of. This may be set to a base class of a test as in the case of the Gremlin
         * process class of tests from which Gremlin flavors extend.  If the actual test class is an inner class
         * of then use a "$" as a separator between the outer class and inner class.
         */
        val test: String,
        /**
         * The specific name of the test method to opt out of or asterisk to opt out of all methods in a
         * [.test].
         */
        val method: String,
        /**
         * The reason the implementation is opting out of this test.
         */
        val reason: String,
        /**
         * For parameterized tests specify the name of the test itself without its "square brackets".
         */
        val specific: String = "",
        /**
         * The list of [GraphComputer] implementations by class name that a test should opt-out from using (i.e. other
         * graph computers not in this list will execute the test).  This setting should only be included when
         * the test is one that uses the `TraversalEngine.COMPUTER` - it will otherwise be ignored.  By
         * default, an empty array is assigned and it is thus assumed that all computers are excluded when an
         * `OptOut` annotation is used, therefore this value must be overridden to be more specific.
         */
        val computers: Array<String> = []
    )

    /**
     * Holds a collection of [OptOut] enabling multiple [OptOut] to be applied to a
     * single suite.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Inherited
    annotation class OptOuts(vararg val value: OptOut)
    companion object {
        /**
         * Configuration key used by [GraphFactory]} to determine which graph to instantiate.
         */
        const val GRAPH = "gremlin.graph"
    }
}