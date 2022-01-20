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
package org.apache.tinkerpop4.gremlin.structure.util.star

import org.apache.commons.configuration2.BaseConfiguration

/**
 * A `StarGraph` is a form of [Attachable] (though the [Graph] implementation does not implement
 * that interface itself).  It is a very limited [Graph] implementation that holds a single [Vertex]
 * and its related properties and edges (and their properties).  It is designed to be an efficient memory
 * representation of this data structure, thus making it good for network and disk-based serialization.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class StarGraph private constructor(
    protected val internStrings: Boolean = true,
    protected val compareIdsUsingStrings: Boolean = true
) : Graph, Serializable {
    protected var nextId = 0L

    /**
     * Gets the [Vertex] representative of the [StarGraph].
     */
    var starVertex: StarVertex? = null
        protected set
    protected var edgeProperties: Map<Object, Map<String, Object>>? = null
    protected var metaProperties: Map<Object, Map<String, Object>>? = null
    private fun nextId(): Long {
        return nextId++
    }

    @Override
    fun addVertex(vararg keyValues: Object?): Vertex? {
        return if (null == starVertex) {
            ElementHelper.legalPropertyKeyValueArray(keyValues)
            starVertex = StarVertex(
                ElementHelper.getIdValue(keyValues).orElse(nextId()),
                ElementHelper.getLabelValue(keyValues).orElse(Vertex.DEFAULT_LABEL)
            )
            ElementHelper.attachProperties(
                starVertex,
                VertexProperty.Cardinality.list,
                keyValues
            ) // TODO: is this smart? I say no... cause vertex property ids are not preserved.
            starVertex
        } else StarAdjacentVertex(ElementHelper.getIdValue(keyValues).orElse(nextId()))
    }

    @Override
    @Throws(IllegalArgumentException::class)
    fun <C : GraphComputer?> compute(graphComputerClass: Class<C>?): C {
        throw Graph.Exceptions.graphComputerNotSupported()
    }

    @Override
    @Throws(IllegalArgumentException::class)
    fun compute(): GraphComputer {
        throw Graph.Exceptions.graphComputerNotSupported()
    }

    @Override
    fun vertices(vararg vertexIds: Object?): Iterator<Vertex> {
        return if (null == starVertex) Collections.emptyIterator() else if (vertexIds.size > 0 && vertexIds[0] is StarVertex) Stream.of(
            vertexIds
        ).map { v -> v as Vertex }.iterator() // todo: maybe do this better - not sure of star semantics here
        else if (idExists(
                starVertex!!.id(),
                *vertexIds
            )
        ) IteratorUtils.of(starVertex) else Collections.emptyIterator()
        // TODO: is this the semantics we want? the only "real vertex" is star vertex.
        /*return null == this.starVertex ?
                Collections.emptyIterator() :
                Stream.concat(
                        Stream.of(this.starVertex),
                        Stream.concat(
                                this.starVertex.outEdges.values()
                                        .stream()
                                        .flatMap(List::stream)
                                        .map(Edge::inVertex),
                                this.starVertex.inEdges.values()
                                        .stream()
                                        .flatMap(List::stream)
                                        .map(Edge::outVertex)))
                        .filter(vertex -> ElementHelper.idExists(vertex.id(), vertexIds))
                        .iterator();*/
    }

    @Override
    fun edges(vararg edgeIds: Object?): Iterator<Edge> {
        return if (null == starVertex) Collections.emptyIterator() else Stream.concat(
            if (null == starVertex!!.inEdges) Stream.empty() else starVertex!!.inEdges!!.values().stream(),
            if (null == starVertex!!.outEdges) Stream.empty() else starVertex!!.outEdges!!.values().stream()
        )
            .flatMap(List::stream)
            .filter { edge ->
                // todo: kinda fishy - need to better nail down how stuff should work here - none of these feel consistent right now.
                if (edgeIds.size > 0 && edgeIds[0] is Edge) return@filter idExists(
                    edge.id(),
                    Stream.of(edgeIds).map { e -> (e as Edge).id() }
                        .toArray()) else return@filter idExists(edge.id(), edgeIds)
            }
            .iterator()
    }

    @Override
    fun tx(): Transaction {
        throw Graph.Exceptions.transactionsNotSupported()
    }

    @Override
    fun variables(): Variables {
        throw Graph.Exceptions.variablesNotSupported()
    }

    @Override
    fun configuration(): Configuration {
        return STAR_GRAPH_CONFIGURATION
    }

    @Override
    fun features(): Features {
        return StarGraphFeatures.INSTANCE
    }

    @Override
    @Throws(Exception::class)
    fun close() {
    }

    @Override
    override fun toString(): String {
        return StringFactory.graphString(this, "starOf:" + starVertex)
    }

    /**
     * StarGraph builder with options to customize its internals
     */
    class Builder
    /**
     * Call [.build] to instantiate
     */
    {
        private var internStrings = true
        private var compareIdsUsingStrings = true

        /**
         * Tell StarGraph whether to invoke [String.intern] on label and property key strings.
         * The default value is deliberately undefined, so that StarGraph's internals may freely change.
         * However, if this builder method is never invoked, then the builder is guaranteed to use
         * whatever default value StarGraph's other public constructors or factory methods would use.
         * This option exists solely for performance tuning in specialized use-cases.
         *
         * @param b true to allow interning, false otherwise
         * @return this builder
         */
        fun internStrings(b: Boolean): Builder {
            internStrings = b
            return this
        }

        /**
         * Tell StarGraph whether to invoke [Object.toString] on vertex and edge IDs during
         * comparisons (including "does an element with this ID already exist" checks).
         * The default value is deliberately undefined, so that StarGraph's internals may freely change.
         * However, if this builder method is never invoked, then the builder is guaranteed to use
         * whatever default value StarGraph's other public constructors or factory methods would use.
         * This option exists solely for performance tuning in specialized use-cases.
         */
        fun compareIdsUsingStrings(b: Boolean): Builder {
            compareIdsUsingStrings = b
            return this
        }

        /**
         * @return a new StarGraph
         */
        fun create(): StarGraph {
            return StarGraph(internStrings, compareIdsUsingStrings)
        }
    }

    fun applyGraphFilter(graphFilter: GraphFilter): Optional<StarGraph> {
        if (null == starVertex) return Optional.empty()
        val filtered: Optional<StarVertex> = starVertex!!.applyGraphFilter(graphFilter)
        return if (filtered.isPresent()) Optional.of(filtered.get().graph() as StarGraph) else Optional.empty()
    }

    private fun idExists(id: Object, vararg providedIds: Object): Boolean {
        return if (compareIdsUsingStrings) {
            ElementHelper.idExists(id, providedIds)
        } else {
            // Almost identical to ElementHelper#idExists, but without toString() calls
            if (0 == providedIds.size) return true
            if (1 == providedIds.size) id.equals(providedIds[0]) else {
                for (temp in providedIds) {
                    if (temp.equals(id)) return true
                }
                false
            }
        }
    }

    ///////////////////////
    //// STAR ELEMENT ////
    //////////////////////
    abstract inner class StarElement<E : Element?> protected constructor(id: Object, label: String) : Element,
        Attachable<E> {
        protected val id: Object
        protected val label: String

        init {
            this.id = id
            this.label = if (internStrings) label.intern() else label
        }

        @Override
        fun id(): Object {
            return id
        }

        @Override
        fun label(): String {
            return label
        }

        @Override
        fun graph(): Graph {
            return this@StarGraph
        }

        @Override
        override fun equals(other: Object?): Boolean {
            return ElementHelper.areEqual(this, other)
        }

        @Override
        override fun hashCode(): Int {
            return ElementHelper.hashCode(this)
        }

        @Override
        fun get(): E {
            return this as E
        }
    }

    //////////////////////
    //// STAR VERTEX ////
    /////////////////////
    inner class StarVertex(id: Object, label: String) : StarElement<Vertex?>(id, label), Vertex {
        var outEdges: Map<String, List<Edge>>? = null
        var inEdges: Map<String, List<Edge>>? = null
        var vertexProperties: Map<String, List<VertexProperty>>? = null
        fun dropEdges(direction: Direction) {
            if ((direction.equals(Direction.OUT) || direction.equals(Direction.BOTH)) && null != outEdges) {
                outEdges.clear()
                outEdges = null
            }
            if ((direction.equals(Direction.IN) || direction.equals(Direction.BOTH)) && null != inEdges) {
                inEdges.clear()
                inEdges = null
            }
        }

        fun dropEdges(direction: Direction, edgeLabel: String?) {
            if (null != outEdges && (direction.equals(Direction.OUT) || direction.equals(Direction.BOTH))) {
                outEdges.remove(edgeLabel)
                if (outEdges!!.isEmpty()) outEdges = null
            }
            if (null != inEdges && (direction.equals(Direction.IN) || direction.equals(Direction.BOTH))) {
                inEdges.remove(edgeLabel)
                if (inEdges!!.isEmpty()) inEdges = null
            }
        }

        fun dropVertexProperties(vararg propertyKeys: String?) {
            if (null != vertexProperties) {
                for (key in propertyKeys) {
                    vertexProperties.remove(key)
                }
            }
        }

        @Override
        fun addEdge(label: String, inVertex: Vertex, vararg keyValues: Object?): Edge {
            val edge: Edge = addOutEdge(label, inVertex, *keyValues)
            if (inVertex.equals(this)) {
                if (ElementHelper.getIdValue(keyValues).isPresent()) {
                    // reuse edge ID from method params
                    addInEdge(label, this, keyValues)
                } else {
                    // copy edge ID that we just allocated with addOutEdge
                    val keyValuesWithId: Array<Object> = Arrays.copyOf(keyValues, keyValues.size + 2)
                    keyValuesWithId[keyValuesWithId.size - 2] = T.id
                    keyValuesWithId[keyValuesWithId.size - 1] = edge.id()
                    addInEdge(label, this, keyValuesWithId)
                }
            }
            return edge
        }

        @Override
        fun <V> property(key: String, value: V, vararg keyValues: Object?): VertexProperty<V> {
            ElementHelper.validateProperty(key, value)
            ElementHelper.legalPropertyKeyValueArray(keyValues)
            return this.property(VertexProperty.Cardinality.single, key, value, *keyValues)
        }

        fun addOutEdge(label: String, inVertex: Vertex?, vararg keyValues: Object?): Edge {
            ElementHelper.validateLabel(label)
            ElementHelper.legalPropertyKeyValueArray(keyValues)
            if (null == outEdges) outEdges = HashMap()
            var outE: List<Edge?>? = outEdges!![label]
            if (null == outE) {
                outE = ArrayList()
                outEdges.put(label, outE)
            }
            val outEdge: StarEdge =
                StarOutEdge(ElementHelper.getIdValue(keyValues).orElse(nextId()), label, inVertex.id())
            ElementHelper.attachProperties(outEdge, keyValues)
            outE.add(outEdge)
            return outEdge
        }

        fun addInEdge(label: String, outVertex: Vertex?, vararg keyValues: Object?): Edge {
            ElementHelper.validateLabel(label)
            ElementHelper.legalPropertyKeyValueArray(keyValues)
            if (null == inEdges) inEdges = HashMap()
            var inE: List<Edge?>? = inEdges!![label]
            if (null == inE) {
                inE = ArrayList()
                inEdges.put(label, inE)
            }
            val inEdge: StarEdge =
                StarInEdge(ElementHelper.getIdValue(keyValues).orElse(nextId()), label, outVertex.id())
            ElementHelper.attachProperties(inEdge, keyValues)
            inE.add(inEdge)
            return inEdge
        }

        @Override
        fun <V> property(
            cardinality: VertexProperty.Cardinality,
            key: String,
            value: V,
            vararg keyValues: Object?
        ): VertexProperty<V> {
            ElementHelper.legalPropertyKeyValueArray(keyValues)
            if (null == vertexProperties) vertexProperties = HashMap()
            val list: List<VertexProperty> =
                if (cardinality.equals(VertexProperty.Cardinality.single)) ArrayList(1) else vertexProperties.getOrDefault(
                    key,
                    ArrayList()
                )
            val vertexProperty: VertexProperty<V> =
                StarVertexProperty(ElementHelper.getIdValue(keyValues).orElse(nextId()), key, value)
            ElementHelper.attachProperties(vertexProperty, keyValues)
            list.add(vertexProperty)
            vertexProperties.put(key, list)
            return vertexProperty
        }

        @Override
        fun edges(direction: Direction, vararg edgeLabels: String?): Iterator<Edge> {
            return if (direction.equals(Direction.OUT)) {
                if (null == outEdges) Collections.emptyIterator() else if (edgeLabels.size == 0) IteratorUtils.flatMap(
                    outEdges!!.values().iterator(), List::iterator
                ) else outEdges.entrySet().stream()
                    .filter { entry -> ElementHelper.keyExists(entry.getKey(), edgeLabels) }
                    .map(Map.Entry::getValue)
                    .flatMap(List::stream)
                    .iterator()
            } else if (direction.equals(Direction.IN)) {
                if (null == inEdges) Collections.emptyIterator() else if (edgeLabels.size == 0) IteratorUtils.flatMap(
                    inEdges!!.values().iterator(), List::iterator
                ) else inEdges.entrySet().stream()
                    .filter { entry -> ElementHelper.keyExists(entry.getKey(), edgeLabels) }
                    .map(Map.Entry::getValue)
                    .flatMap(List::stream)
                    .iterator()
            } else IteratorUtils.concat(this.edges(Direction.IN, edgeLabels), this.edges(Direction.OUT, edgeLabels))
        }

        @Override
        fun vertices(direction: Direction, vararg edgeLabels: String?): Iterator<Vertex> {
            return if (direction.equals(Direction.OUT)) IteratorUtils.map(
                this.edges(direction, *edgeLabels),
                Edge::inVertex
            ) else if (direction.equals(Direction.IN)) IteratorUtils.map(
                this.edges(direction, *edgeLabels),
                Edge::outVertex
            ) else IteratorUtils.concat(
                this.vertices(Direction.IN, edgeLabels),
                this.vertices(Direction.OUT, edgeLabels)
            )
        }

        @Override
        fun remove() {
            throw IllegalStateException("The star vertex can not be removed from the StarGraph: $this")
        }

        @Override
        override fun toString(): String {
            return StringFactory.vertexString(this)
        }

        @Override
        fun <V> properties(vararg propertyKeys: String?): Iterator<VertexProperty<V>> {
            return if (null == vertexProperties || vertexProperties!!.isEmpty()) Collections.emptyIterator() else if (propertyKeys.size == 0) vertexProperties.entrySet()
                .stream()
                .flatMap { entry -> entry.getValue().stream() }
                .iterator() else if (propertyKeys.size == 1) vertexProperties.getOrDefault(
                propertyKeys[0],
                Collections.emptyList()
            )
                .iterator() else vertexProperties.entrySet().stream()
                .filter { entry -> ElementHelper.keyExists(entry.getKey(), propertyKeys) }
                .flatMap { entry -> entry.getValue().stream() }
                .iterator()
        }

        ///////////////
        fun applyGraphFilter(graphFilter: GraphFilter): Optional<StarVertex> {
            return if (!graphFilter.hasFilter()) Optional.of(this) else if (graphFilter.legalVertex(this)) {
                if (graphFilter.hasEdgeFilter()) {
                    if (graphFilter.checkEdgeLegality(Direction.OUT).negative()) this.dropEdges(Direction.OUT)
                    if (graphFilter.checkEdgeLegality(Direction.IN).negative()) this.dropEdges(Direction.IN)
                    if (null != outEdges) for (key in HashSet(outEdges.keySet())) {
                        if (graphFilter.checkEdgeLegality(Direction.OUT, key).negative()) this.dropEdges(
                            Direction.OUT,
                            key
                        )
                    }
                    if (null != inEdges) for (key in HashSet(inEdges.keySet())) {
                        if (graphFilter.checkEdgeLegality(Direction.IN, key).negative()) this.dropEdges(
                            Direction.IN,
                            key
                        )
                    }
                    if (null != inEdges || null != outEdges) {
                        val outEdges: Map<String, List<Edge>> = HashMap()
                        val inEdges: Map<String, List<Edge>> = HashMap()
                        graphFilter.legalEdges(this).forEachRemaining { edge ->
                            if (edge is StarOutEdge) {
                                var edges: List<Edge?>? = outEdges[edge.label()]
                                if (null == edges) {
                                    edges = ArrayList()
                                    outEdges.put(edge.label(), edges)
                                }
                                edges.add(edge)
                            } else {
                                var edges: List<Edge?>? = inEdges[edge.label()]
                                if (null == edges) {
                                    edges = ArrayList()
                                    inEdges.put(edge.label(), edges)
                                }
                                edges.add(edge)
                            }
                        }
                        if (outEdges.isEmpty()) this.dropEdges(Direction.OUT) else this.outEdges = outEdges
                        if (inEdges.isEmpty()) this.dropEdges(Direction.IN) else this.inEdges = inEdges
                    }
                }
                Optional.of(this)
            } else {
                Optional.empty()
            }
        }
    }

    ///////////////////////////////
    //// STAR VERTEX PROPERTY ////
    //////////////////////////////
    inner class StarVertexProperty<V>(id: Object, key: String, private val value: V) :
        StarElement<VertexProperty<V>?>(id, key), VertexProperty<V> {
        @Override
        fun key(): String {
            return label()
        }

        @Override
        @Throws(NoSuchElementException::class)
        fun value(): V {
            return value
        }

        @get:Override
        val isPresent: Boolean
            get() = true

        @Override
        fun element(): Vertex? {
            return starVertex
        }

        @Override
        fun remove() {
            if (null != starVertex!!.vertexProperties) starVertex!!.vertexProperties!![label].remove(this)
        }

        @Override
        fun <U> properties(vararg propertyKeys: String): Iterator<Property<U>> {
            val properties: Map<String, Object>? = if (null == metaProperties) null else metaProperties!![id]
            return if (null == properties || properties.isEmpty()) Collections.emptyIterator() else if (propertyKeys.size == 0) properties.entrySet()
                .stream()
                .map { entry -> StarProperty<V>(entry.getKey(), entry.getValue(), this) }
                .iterator() else if (propertyKeys.size == 1) {
                val v: Object? = properties[propertyKeys[0]]
                if (null == v) Collections.emptyIterator() else (IteratorUtils.of(
                    StarProperty<V?>(
                        propertyKeys[0],
                        v,
                        this
                    )
                ) as Iterator)
            } else {
                properties.entrySet().stream()
                    .filter { entry -> ElementHelper.keyExists(entry.getKey(), propertyKeys) }
                    .map { entry -> StarProperty<V>(entry.getKey(), entry.getValue(), this) }
                    .iterator()
            }
        }

        @Override
        fun <U> property(key: String, value: U): Property<U> {
            ElementHelper.validateProperty(key, value)
            if (null == metaProperties) metaProperties = HashMap()
            var properties: Map<String, Object?>? = metaProperties!![id]
            if (null == properties) {
                properties = HashMap()
                metaProperties.put(id, properties)
            }
            properties.put(key, value)
            return StarProperty<V>(key, value, this)
        }

        @Override
        override fun toString(): String {
            return StringFactory.propertyString(this)
        }
    }

    ///////////////////////////////
    //// STAR ADJACENT VERTEX ////
    //////////////////////////////
    inner class StarAdjacentVertex(id: Object) : Vertex {
        private val id: Object

        init {
            this.id = id
        }

        @Override
        fun addEdge(label: String, inVertex: Vertex, vararg keyValues: Object?): Edge {
            return if (inVertex.equals(starVertex)) starVertex!!.addInEdge(
                label,
                this,
                keyValues
            ) else throw GraphComputer.Exceptions.adjacentVertexEdgesAndVerticesCanNotBeReadOrUpdated()
        }

        @Override
        fun <V> property(key: String?, value: V, vararg keyValues: Object?): VertexProperty<V> {
            throw GraphComputer.Exceptions.adjacentVertexPropertiesCanNotBeReadOrUpdated()
        }

        @Override
        fun <V> property(
            cardinality: VertexProperty.Cardinality?,
            key: String?,
            value: V,
            vararg keyValues: Object?
        ): VertexProperty<V> {
            throw GraphComputer.Exceptions.adjacentVertexPropertiesCanNotBeReadOrUpdated()
        }

        @Override
        fun edges(direction: Direction?, vararg edgeLabels: String?): Iterator<Edge> {
            throw GraphComputer.Exceptions.adjacentVertexEdgesAndVerticesCanNotBeReadOrUpdated()
        }

        @Override
        fun vertices(direction: Direction?, vararg edgeLabels: String?): Iterator<Vertex> {
            throw GraphComputer.Exceptions.adjacentVertexEdgesAndVerticesCanNotBeReadOrUpdated()
        }

        @Override
        fun id(): Object {
            return id
        }

        @Override
        fun label(): String {
            throw GraphComputer.Exceptions.adjacentVertexLabelsCanNotBeRead()
        }

        @Override
        fun graph(): Graph {
            return this@StarGraph
        }

        @Override
        fun remove() {
            throw Vertex.Exceptions.vertexRemovalNotSupported()
        }

        @Override
        fun <V> properties(vararg propertyKeys: String?): Iterator<VertexProperty<V>> {
            throw GraphComputer.Exceptions.adjacentVertexPropertiesCanNotBeReadOrUpdated()
        }

        @Override
        override fun equals(other: Object?): Boolean {
            return ElementHelper.areEqual(this, other)
        }

        @Override
        override fun hashCode(): Int {
            return ElementHelper.hashCode(this)
        }

        @Override
        override fun toString(): String {
            return StringFactory.vertexString(this)
        }
    }

    ////////////////////
    //// STAR EDGE ////
    ///////////////////
    abstract inner class StarEdge private constructor(id: Object, label: String, otherId: Object) :
        StarElement<Edge?>(id, label), Edge {
        protected val otherId: Object

        init {
            this.otherId = otherId
        }

        @Override
        fun <V> property(key: String, value: V): Property<V> {
            ElementHelper.validateProperty(key, value)
            if (null == edgeProperties) edgeProperties = HashMap()
            var properties: Map<String, Object?>? = edgeProperties!![id]
            if (null == properties) {
                properties = HashMap()
                edgeProperties.put(id, properties)
            }
            properties.put(key, value)
            return StarProperty(key, value, this)
        }

        @Override
        fun <V> properties(vararg propertyKeys: String): Iterator<Property<V>> {
            val properties: Map<String, Object>? = if (null == edgeProperties) null else edgeProperties!![id]
            return if (null == properties || properties.isEmpty()) Collections.emptyIterator() else if (propertyKeys.size == 0) properties.entrySet()
                .stream()
                .map { entry -> StarProperty<V>(entry.getKey(), entry.getValue(), this) }
                .iterator() else if (propertyKeys.size == 1) {
                val v: Object? = properties[propertyKeys[0]]
                if (null == v) Collections.emptyIterator() else (IteratorUtils.of(
                    StarProperty<V?>(
                        propertyKeys[0],
                        v,
                        this
                    )
                ) as Iterator)
            } else {
                properties.entrySet().stream()
                    .filter { entry -> ElementHelper.keyExists(entry.getKey(), propertyKeys) }
                    .map { entry -> StarProperty<V>(entry.getKey(), entry.getValue(), this) }
                    .iterator()
            }
        }

        @Override
        fun vertices(direction: Direction): Iterator<Vertex> {
            return if (direction.equals(Direction.OUT)) IteratorUtils.of(this.outVertex()) else if (direction.equals(
                    Direction.IN
                )
            ) IteratorUtils.of(
                this.inVertex()
            ) else IteratorUtils.of(
                this.outVertex(),
                this.inVertex()
            )
        }

        @Override
        fun remove() {
            throw Edge.Exceptions.edgeRemovalNotSupported()
        }

        @Override
        override fun toString(): String {
            return StringFactory.edgeString(this)
        }
    }

    inner class StarOutEdge(id: Object, label: String, otherId: Object) : StarEdge(id, label, otherId) {
        @Override
        fun outVertex(): Vertex? {
            return starVertex
        }

        @Override
        fun inVertex(): Vertex {
            return StarAdjacentVertex(otherId)
        }
    }

    inner class StarInEdge(id: Object, label: String, otherId: Object) : StarEdge(id, label, otherId) {
        @Override
        fun outVertex(): Vertex {
            return StarAdjacentVertex(otherId)
        }

        @Override
        fun inVertex(): Vertex? {
            return starVertex
        }
    }

    ////////////////////////
    //// STAR PROPERTY ////
    ///////////////////////
    inner class StarProperty<V>(key: String, value: V, element: Element) : Property<V>, Attachable<Property<V>?> {
        private val key: String
        private val value: V
        private val element: Element

        init {
            this.key = if (internStrings) key.intern() else key
            this.value = value
            this.element = element
        }

        @Override
        fun key(): String {
            return key
        }

        @Override
        @Throws(NoSuchElementException::class)
        fun value(): V {
            return value
        }

        @get:Override
        val isPresent: Boolean
            get() = true

        @Override
        fun element(): Element {
            return element
        }

        @Override
        fun remove() {
            throw Property.Exceptions.propertyRemovalNotSupported()
        }

        @Override
        override fun toString(): String {
            return StringFactory.propertyString(this)
        }

        @Override
        override fun equals(`object`: Object?): Boolean {
            return ElementHelper.areEqual(this, `object`)
        }

        @Override
        override fun hashCode(): Int {
            return ElementHelper.hashCode(this)
        }

        @Override
        fun get(): Property<V> {
            return this
        }
    }

    class StarGraphFeatures private constructor() : Features {
        @Override
        fun graph(): GraphFeatures {
            return StarGraphGraphFeatures.INSTANCE
        }

        @Override
        fun edge(): EdgeFeatures {
            return StarGraphEdgeFeatures.INSTANCE
        }

        @Override
        fun vertex(): VertexFeatures {
            return StarGraphVertexFeatures.INSTANCE
        }

        @Override
        override fun toString(): String {
            return StringFactory.featureString(this)
        }

        companion object {
            val INSTANCE = StarGraphFeatures()
        }
    }

    internal class StarGraphVertexFeatures private constructor() : VertexFeatures {
        @Override
        fun properties(): VertexPropertyFeatures {
            return StarGraphVertexPropertyFeatures.INSTANCE
        }

        @Override
        fun supportsCustomIds(): Boolean {
            return false
        }

        @Override
        fun willAllowId(id: Object?): Boolean {
            return true
        }

        companion object {
            val INSTANCE = StarGraphVertexFeatures()
        }
    }

    internal class StarGraphEdgeFeatures private constructor() : EdgeFeatures {
        @Override
        fun supportsCustomIds(): Boolean {
            return false
        }

        @Override
        fun willAllowId(id: Object?): Boolean {
            return true
        }

        companion object {
            val INSTANCE = StarGraphEdgeFeatures()
        }
    }

    internal class StarGraphGraphFeatures private constructor() : GraphFeatures {
        @Override
        fun supportsTransactions(): Boolean {
            return false
        }

        @Override
        fun supportsPersistence(): Boolean {
            return false
        }

        @Override
        fun supportsThreadedTransactions(): Boolean {
            return false
        }

        companion object {
            val INSTANCE = StarGraphGraphFeatures()
        }
    }

    internal class StarGraphVertexPropertyFeatures private constructor() : VertexPropertyFeatures {
        @Override
        fun supportsCustomIds(): Boolean {
            return false
        }

        @Override
        fun willAllowId(id: Object?): Boolean {
            return true
        }

        companion object {
            val INSTANCE = StarGraphVertexPropertyFeatures()
        }
    }

    companion object {
        private val STAR_GRAPH_CONFIGURATION: Configuration = BaseConfiguration()

        init {
            STAR_GRAPH_CONFIGURATION.setProperty(Graph.GRAPH, StarGraph::class.java.getCanonicalName())
        }

        /**
         * Creates an empty [StarGraph].
         */
        fun open(): StarGraph {
            return StarGraph()
        }

        /**
         * Creates a new [StarGraph] from a [Vertex].
         */
        fun of(vertex: Vertex): StarGraph {
            if (vertex is StarVertex) return vertex.graph()
            // else convert to a star graph
            val starGraph = StarGraph()
            val starVertex = starGraph.addVertex(T.id, vertex.id(), T.label, vertex.label()) as StarVertex?
            val supportsMetaProperties: Boolean = vertex.graph().features().vertex().supportsMetaProperties()
            vertex.properties().forEachRemaining { vp ->
                val starVertexProperty: VertexProperty<*> =
                    starVertex!!.property(VertexProperty.Cardinality.list, vp.key(), vp.value(), T.id, vp.id())
                if (supportsMetaProperties) vp.properties()
                    .forEachRemaining { p -> starVertexProperty.property(p.key(), p.value()) }
            }
            vertex.edges(Direction.IN).forEachRemaining { edge ->
                val starEdge: Edge = starVertex!!.addInEdge(
                    edge.label(),
                    starGraph.addVertex(T.id, edge.outVertex().id()),
                    T.id,
                    edge.id()
                )
                edge.properties().forEachRemaining { p -> starEdge.property(p.key(), p.value()) }
            }
            vertex.edges(Direction.OUT).forEachRemaining { edge ->
                val starEdge: Edge = starVertex!!.addOutEdge(
                    edge.label(),
                    starGraph.addVertex(T.id, edge.inVertex().id()),
                    T.id,
                    edge.id()
                )
                edge.properties().forEachRemaining { p -> starEdge.property(p.key(), p.value()) }
            }
            return starGraph
        }

        fun build(): Builder {
            return Builder()
        }
    }
}