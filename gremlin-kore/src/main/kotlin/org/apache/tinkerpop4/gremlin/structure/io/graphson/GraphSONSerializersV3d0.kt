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
package org.apache.tinkerpop4.gremlin.structure.io.graphson

import org.apache.tinkerpop4.gremlin.process.traversal.Path

/**
 * GraphSON serializers for graph-based objects such as vertices, edges, properties, and paths. These serializers
 * present a generalized way to serialize the implementations of core interfaces.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
internal class GraphSONSerializersV3d0 private constructor() {
    ////////////////////////////// SERIALIZERS /////////////////////////////////
    internal class VertexJacksonSerializer(private val normalize: Boolean) : StdScalarSerializer<Vertex?>(
        Vertex::class.java
    ) {
        @Override
        @Throws(IOException::class)
        fun serialize(vertex: Vertex, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider?) {
            jsonGenerator.writeStartObject()
            jsonGenerator.writeObjectField(GraphSONTokens.ID, vertex.id())
            jsonGenerator.writeStringField(GraphSONTokens.LABEL, vertex.label())
            writeProperties(vertex, jsonGenerator)
            jsonGenerator.writeEndObject()
        }

        @Throws(IOException::class)
        private fun writeProperties(vertex: Vertex, jsonGenerator: JsonGenerator) {
            if (vertex.keys().size() === 0) return
            jsonGenerator.writeFieldName(GraphSONTokens.PROPERTIES)
            jsonGenerator.writeStartObject()
            val keys: List<String> =
                if (normalize) IteratorUtils.list(vertex.keys().iterator(), Comparator.naturalOrder()) else ArrayList(
                    vertex.keys()
                )
            for (key in keys) {
                val vertexProperties: Iterator<VertexProperty<Object>> =
                    if (normalize) IteratorUtils.list(vertex.properties(key), Comparators.PROPERTY_COMPARATOR)
                        .iterator() else vertex.properties(key)
                if (vertexProperties.hasNext()) {
                    jsonGenerator.writeFieldName(key)
                    jsonGenerator.writeStartArray()
                    while (vertexProperties.hasNext()) {
                        jsonGenerator.writeObject(vertexProperties.next())
                    }
                    jsonGenerator.writeEndArray()
                }
            }
            jsonGenerator.writeEndObject()
        }
    }

    internal class EdgeJacksonSerializer(private val normalize: Boolean) : StdScalarSerializer<Edge?>(
        Edge::class.java
    ) {
        @Override
        @Throws(IOException::class)
        fun serialize(edge: Edge, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider?) {
            jsonGenerator.writeStartObject()
            jsonGenerator.writeObjectField(GraphSONTokens.ID, edge.id())
            jsonGenerator.writeStringField(GraphSONTokens.LABEL, edge.label())
            jsonGenerator.writeStringField(GraphSONTokens.IN_LABEL, edge.inVertex().label())
            jsonGenerator.writeStringField(GraphSONTokens.OUT_LABEL, edge.outVertex().label())
            jsonGenerator.writeObjectField(GraphSONTokens.IN, edge.inVertex().id())
            jsonGenerator.writeObjectField(GraphSONTokens.OUT, edge.outVertex().id())
            writeProperties(edge, jsonGenerator)
            jsonGenerator.writeEndObject()
        }

        @Throws(IOException::class)
        private fun writeProperties(edge: Edge, jsonGenerator: JsonGenerator) {
            val elementProperties: Iterator<Property<Object>> =
                if (normalize) IteratorUtils.list(edge.properties(), Comparators.PROPERTY_COMPARATOR)
                    .iterator() else edge.properties()
            if (elementProperties.hasNext()) {
                jsonGenerator.writeFieldName(GraphSONTokens.PROPERTIES)
                jsonGenerator.writeStartObject()
                elementProperties.forEachRemaining { prop -> safeWriteObjectField(jsonGenerator, prop.key(), prop) }
                jsonGenerator.writeEndObject()
            }
        }
    }

    internal class PropertyJacksonSerializer : StdScalarSerializer<Property?>(Property::class.java) {
        @Override
        @Throws(IOException::class)
        fun serialize(property: Property, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider?) {
            jsonGenerator.writeStartObject()
            jsonGenerator.writeObjectField(GraphSONTokens.KEY, property.key())
            jsonGenerator.writeObjectField(GraphSONTokens.VALUE, property.value())
            jsonGenerator.writeEndObject()
        }
    }

    internal class VertexPropertyJacksonSerializer(private val normalize: Boolean, private val includeLabel: Boolean) :
        StdScalarSerializer<VertexProperty?>(
            VertexProperty::class.java
        ) {
        @Override
        @Throws(IOException::class)
        fun serialize(property: VertexProperty, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider?) {
            jsonGenerator.writeStartObject()
            jsonGenerator.writeObjectField(GraphSONTokens.ID, property.id())
            jsonGenerator.writeObjectField(GraphSONTokens.VALUE, property.value())
            if (includeLabel) jsonGenerator.writeStringField(GraphSONTokens.LABEL, property.label())
            tryWriteMetaProperties(property, jsonGenerator, normalize)
            jsonGenerator.writeEndObject()
        }

        companion object {
            @Throws(IOException::class)
            private fun tryWriteMetaProperties(
                property: VertexProperty, jsonGenerator: JsonGenerator,
                normalize: Boolean
            ) {
                // when "detached" you can't check features of the graph it detached from so it has to be
                // treated differently from a regular VertexProperty implementation.
                if (property is DetachedVertexProperty) {
                    // only write meta properties key if they exist
                    if (property.properties().hasNext()) {
                        writeMetaProperties(property, jsonGenerator, normalize)
                    }
                } else {
                    // still attached - so we can check the features to see if it's worth even trying to write the
                    // meta properties key
                    if (property.graph().features().vertex().supportsMetaProperties() && property.properties()
                            .hasNext()
                    ) {
                        writeMetaProperties(property, jsonGenerator, normalize)
                    }
                }
            }

            @Throws(IOException::class)
            private fun writeMetaProperties(
                property: VertexProperty, jsonGenerator: JsonGenerator,
                normalize: Boolean
            ) {
                jsonGenerator.writeFieldName(GraphSONTokens.PROPERTIES)
                jsonGenerator.writeStartObject()
                val metaProperties: Iterator<Property<Object>> = if (normalize) IteratorUtils.list(
                    property.properties() as Iterator<Property<Object?>?>,
                    Comparators.PROPERTY_COMPARATOR
                ).iterator() else property.properties()
                while (metaProperties.hasNext()) {
                    val metaProperty: Property<Object> = metaProperties.next()
                    jsonGenerator.writeObjectField(metaProperty.key(), metaProperty.value())
                }
                jsonGenerator.writeEndObject()
            }
        }
    }

    internal class PathJacksonSerializer : StdScalarSerializer<Path?>(Path::class.java) {
        @Override
        @Throws(IOException::class, JsonGenerationException::class)
        fun serialize(path: Path?, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider?) {
            jsonGenerator.writeStartObject()

            // paths shouldn't serialize with properties if the path contains graph elements
            val p: Path = DetachedFactory.detach(path, false)
            jsonGenerator.writeObjectField(GraphSONTokens.LABELS, p.labels())
            jsonGenerator.writeObjectField(GraphSONTokens.OBJECTS, p.objects())
            jsonGenerator.writeEndObject()
        }
    }

    internal class TreeJacksonSerializer : StdScalarSerializer<Tree?>(Tree::class.java) {
        @Override
        @Throws(IOException::class, JsonGenerationException::class)
        fun serialize(tree: Tree, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider?) {
            jsonGenerator.writeStartArray()
            val set: Set<Map.Entry<Element, Tree>> = tree.entrySet()
            for (entry in set) {
                jsonGenerator.writeStartObject()
                jsonGenerator.writeObjectField(GraphSONTokens.KEY, entry.getKey())
                jsonGenerator.writeObjectField(GraphSONTokens.VALUE, entry.getValue())
                jsonGenerator.writeEndObject()
            }
            jsonGenerator.writeEndArray()
        }
    }

    internal class TraversalExplanationJacksonSerializer : StdScalarSerializer<TraversalExplanation?>(
        TraversalExplanation::class.java
    ) {
        @Override
        @Throws(IOException::class)
        fun serialize(
            traversalExplanation: TraversalExplanation, jsonGenerator: JsonGenerator,
            serializerProvider: SerializerProvider?
        ) {
            val m: Map<String, Object> = HashMap()
            m.put(GraphSONTokens.ORIGINAL, getStepsAsList(traversalExplanation.getOriginalTraversal()))
            val strategyTraversals: List<Pair<TraversalStrategy, Traversal.Admin<*, *>>> =
                traversalExplanation.getStrategyTraversals()
            val intermediates: List<Map<String, Object>> = ArrayList()
            for (pair in strategyTraversals) {
                val intermediate: Map<String, Object> = HashMap()
                intermediate.put(GraphSONTokens.STRATEGY, pair.getValue0().toString())
                intermediate.put(GraphSONTokens.CATEGORY, pair.getValue0().getTraversalCategory().getSimpleName())
                intermediate.put(GraphSONTokens.TRAVERSAL, getStepsAsList(pair.getValue1()))
                intermediates.add(intermediate)
            }
            m.put(GraphSONTokens.INTERMEDIATE, intermediates)
            if (strategyTraversals.isEmpty()) m.put(
                GraphSONTokens.FINAL,
                getStepsAsList(traversalExplanation.getOriginalTraversal())
            ) else m.put(
                GraphSONTokens.FINAL, getStepsAsList(
                    strategyTraversals[strategyTraversals.size() - 1].getValue1()
                )
            )
            jsonGenerator.writeObject(m)
        }

        private fun getStepsAsList(t: Traversal.Admin<*, *>): List<String> {
            val steps: List<String> = ArrayList()
            t.getSteps().iterator().forEachRemaining { s -> steps.add(s.toString()) }
            return steps
        }
    }

    internal class IntegerGraphSONSerializer : StdScalarSerializer<Integer?>(Integer::class.java) {
        @Override
        @Throws(IOException::class)
        fun serialize(
            integer: Integer, jsonGenerator: JsonGenerator,
            serializerProvider: SerializerProvider?
        ) {
            jsonGenerator.writeNumber((integer as Integer).intValue())
        }
    }

    internal class DoubleGraphSONSerializer : StdScalarSerializer<Double?>(Double::class.java) {
        @Override
        @Throws(IOException::class)
        fun serialize(
            doubleValue: Double?, jsonGenerator: JsonGenerator,
            serializerProvider: SerializerProvider?
        ) {
            jsonGenerator.writeNumber(doubleValue)
        }
    }

    internal class TraversalMetricsJacksonSerializer : StdScalarSerializer<TraversalMetrics?>(
        TraversalMetrics::class.java
    ) {
        @Override
        @Throws(IOException::class)
        fun serialize(
            traversalMetrics: TraversalMetrics,
            jsonGenerator: JsonGenerator,
            serializerProvider: SerializerProvider?
        ) {
            // creation of the map enables all the fields to be properly written with their type if required
            val m: Map<String, Object> = HashMap()
            m.put(GraphSONTokens.DURATION, traversalMetrics.getDuration(TimeUnit.NANOSECONDS) / 1000000.0)
            val metrics: List<Metrics> = ArrayList()
            metrics.addAll(traversalMetrics.getMetrics())
            m.put(GraphSONTokens.METRICS, metrics)
            jsonGenerator.writeObject(m)
        }
    }

    internal class MetricsJacksonSerializer : StdScalarSerializer<Metrics?>(Metrics::class.java) {
        @Override
        @Throws(IOException::class)
        fun serialize(
            metrics: Metrics, jsonGenerator: JsonGenerator,
            serializerProvider: SerializerProvider?
        ) {
            val m: Map<String, Object> = HashMap()
            m.put(GraphSONTokens.ID, metrics.getId())
            m.put(GraphSONTokens.NAME, metrics.getName())
            m.put(GraphSONTokens.COUNTS, metrics.getCounts())
            m.put(GraphSONTokens.DURATION, metrics.getDuration(TimeUnit.NANOSECONDS) / 1000000.0)
            if (!metrics.getAnnotations().isEmpty()) {
                m.put(GraphSONTokens.ANNOTATIONS, metrics.getAnnotations())
            }
            if (!metrics.getNested().isEmpty()) {
                val nested: List<Metrics> = ArrayList()
                metrics.getNested().forEach { it -> nested.add(it) }
                m.put(GraphSONTokens.METRICS, nested)
            }
            jsonGenerator.writeObject(m)
        }
    }

    /**
     * Maps in the JVM can have [Object] as a key, but in JSON they must be a [String].
     */
    internal class GraphSONKeySerializer : StdKeySerializer() {
        @Override
        @Throws(IOException::class)
        fun serialize(o: Object, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider) {
            ser(o, jsonGenerator, serializerProvider)
        }

        @Override
        @Throws(IOException::class)
        fun serializeWithType(
            o: Object, jsonGenerator: JsonGenerator,
            serializerProvider: SerializerProvider, typeSerializer: TypeSerializer?
        ) {
            ser(o, jsonGenerator, serializerProvider)
        }

        @Throws(IOException::class)
        private fun ser(
            o: Object, jsonGenerator: JsonGenerator,
            serializerProvider: SerializerProvider
        ) {
            if (Element::class.java.isAssignableFrom(o.getClass())) jsonGenerator.writeFieldName(
                (o as Element).id().toString()
            ) else super.serialize(o, jsonGenerator, serializerProvider)
        }
    }

    //////////////////////////// DESERIALIZERS ///////////////////////////
    internal class VertexJacksonDeserializer : StdDeserializer<Vertex?>(Vertex::class.java) {
        @Throws(IOException::class, JsonProcessingException::class)
        fun deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext): Vertex {
            val v: DetachedVertex.Builder = DetachedVertex.build()
            while (jsonParser.nextToken() !== JsonToken.END_OBJECT) {
                if (jsonParser.getCurrentName().equals(GraphSONTokens.ID)) {
                    jsonParser.nextToken()
                    v.setId(deserializationContext.readValue(jsonParser, Object::class.java))
                } else if (jsonParser.getCurrentName().equals(GraphSONTokens.LABEL)) {
                    jsonParser.nextToken()
                    v.setLabel(jsonParser.getText())
                } else if (jsonParser.getCurrentName().equals(GraphSONTokens.PROPERTIES)) {
                    jsonParser.nextToken()
                    while (jsonParser.nextToken() !== JsonToken.END_OBJECT) {
                        jsonParser.nextToken()
                        while (jsonParser.nextToken() !== JsonToken.END_ARRAY) {
                            v.addProperty(
                                deserializationContext.readValue(
                                    jsonParser,
                                    VertexProperty::class.java
                                ) as DetachedVertexProperty
                            )
                        }
                    }
                }
            }
            return v.create()
        }

        @get:Override
        val isCachable: Boolean
            get() = true
    }

    internal class EdgeJacksonDeserializer : StdDeserializer<Edge?>(Edge::class.java) {
        @Override
        @Throws(IOException::class, JsonProcessingException::class)
        fun deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext): Edge {
            val e: DetachedEdge.Builder = DetachedEdge.build()
            val inV: DetachedVertex.Builder = DetachedVertex.build()
            val outV: DetachedVertex.Builder = DetachedVertex.build()
            while (jsonParser.nextToken() !== JsonToken.END_OBJECT) {
                if (jsonParser.getCurrentName().equals(GraphSONTokens.ID)) {
                    jsonParser.nextToken()
                    e.setId(deserializationContext.readValue(jsonParser, Object::class.java))
                } else if (jsonParser.getCurrentName().equals(GraphSONTokens.LABEL)) {
                    jsonParser.nextToken()
                    e.setLabel(jsonParser.getText())
                } else if (jsonParser.getCurrentName().equals(GraphSONTokens.OUT)) {
                    jsonParser.nextToken()
                    outV.setId(deserializationContext.readValue(jsonParser, Object::class.java))
                } else if (jsonParser.getCurrentName().equals(GraphSONTokens.OUT_LABEL)) {
                    jsonParser.nextToken()
                    outV.setLabel(jsonParser.getText())
                } else if (jsonParser.getCurrentName().equals(GraphSONTokens.IN)) {
                    jsonParser.nextToken()
                    inV.setId(deserializationContext.readValue(jsonParser, Object::class.java))
                } else if (jsonParser.getCurrentName().equals(GraphSONTokens.IN_LABEL)) {
                    jsonParser.nextToken()
                    inV.setLabel(jsonParser.getText())
                } else if (jsonParser.getCurrentName().equals(GraphSONTokens.PROPERTIES)) {
                    jsonParser.nextToken()
                    while (jsonParser.nextToken() !== JsonToken.END_OBJECT) {
                        jsonParser.nextToken()
                        e.addProperty(deserializationContext.readValue(jsonParser, Property::class.java))
                    }
                }
            }
            e.setInV(inV.create())
            e.setOutV(outV.create())
            return e.create()
        }

        @get:Override
        val isCachable: Boolean
            get() = true
    }

    internal class PropertyJacksonDeserializer : StdDeserializer<Property?>(Property::class.java) {
        @Override
        @Throws(IOException::class, JsonProcessingException::class)
        fun deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext): Property {
            var key: String? = null
            var value: Object? = null
            while (jsonParser.nextToken() !== JsonToken.END_OBJECT) {
                if (jsonParser.getCurrentName().equals(GraphSONTokens.KEY)) {
                    jsonParser.nextToken()
                    key = jsonParser.getText()
                } else if (jsonParser.getCurrentName().equals(GraphSONTokens.VALUE)) {
                    jsonParser.nextToken()
                    value = deserializationContext.readValue(jsonParser, Object::class.java)
                }
            }
            return DetachedProperty(key, value)
        }

        @get:Override
        val isCachable: Boolean
            get() = true
    }

    internal class PathJacksonDeserializer : StdDeserializer<Path?>(Path::class.java) {
        @Override
        @Throws(IOException::class, JsonProcessingException::class)
        fun deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext): Path {
            val p: Path = MutablePath.make()
            var labels: List<Object?> = ArrayList()
            var objects: List<Object?> = ArrayList()
            while (jsonParser.nextToken() !== JsonToken.END_OBJECT) {
                if (jsonParser.getCurrentName().equals(GraphSONTokens.LABELS)) {
                    jsonParser.nextToken()
                    labels = deserializationContext.readValue(jsonParser, List::class.java)
                } else if (jsonParser.getCurrentName().equals(GraphSONTokens.OBJECTS)) {
                    jsonParser.nextToken()
                    objects = deserializationContext.readValue(jsonParser, List::class.java)
                }
            }
            for (i in 0 until objects.size()) {
                p.extend(objects[i], labels[i] as Set<String?>?)
            }
            return p
        }

        @get:Override
        val isCachable: Boolean
            get() = true

        companion object {
            private val setType: JavaType =
                TypeFactory.defaultInstance().constructCollectionType(HashSet::class.java, String::class.java)
        }
    }

    internal class VertexPropertyJacksonDeserializer protected constructor() : StdDeserializer<VertexProperty?>(
        VertexProperty::class.java
    ) {
        @Override
        @Throws(IOException::class, JsonProcessingException::class)
        fun deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext): VertexProperty {
            val vp: DetachedVertexProperty.Builder = DetachedVertexProperty.build()
            while (jsonParser.nextToken() !== JsonToken.END_OBJECT) {
                if (jsonParser.getCurrentName().equals(GraphSONTokens.ID)) {
                    jsonParser.nextToken()
                    vp.setId(deserializationContext.readValue(jsonParser, Object::class.java))
                } else if (jsonParser.getCurrentName().equals(GraphSONTokens.LABEL)) {
                    jsonParser.nextToken()
                    vp.setLabel(jsonParser.getText())
                } else if (jsonParser.getCurrentName().equals(GraphSONTokens.VALUE)) {
                    jsonParser.nextToken()
                    vp.setValue(deserializationContext.readValue(jsonParser, Object::class.java))
                } else if (jsonParser.getCurrentName().equals(GraphSONTokens.PROPERTIES)) {
                    jsonParser.nextToken()
                    while (jsonParser.nextToken() !== JsonToken.END_OBJECT) {
                        val key: String = jsonParser.getCurrentName()
                        jsonParser.nextToken()
                        val `val`: Object = deserializationContext.readValue(jsonParser, Object::class.java)
                        vp.addProperty(DetachedProperty(key, `val`))
                    }
                }
            }
            return vp.create()
        }

        @get:Override
        val isCachable: Boolean
            get() = true

        companion object {
            private val propertiesType: JavaType = TypeFactory.defaultInstance()
                .constructMapType(HashMap::class.java, String::class.java, Object::class.java)
        }
    }

    internal class TraversalExplanationJacksonDeserializer : StdDeserializer<TraversalExplanation?>(
        TraversalExplanation::class.java
    ) {
        @Override
        @Throws(IOException::class, JsonProcessingException::class)
        fun deserialize(jsonParser: JsonParser?, deserializationContext: DeserializationContext): TraversalExplanation {
            val explainData: Map<String, Object> = deserializationContext.readValue(jsonParser, Map::class.java)
            val originalTraversal: String = explainData[GraphSONTokens.ORIGINAL].toString()
            val intermediates: List<Triplet<String, String, String>> = ArrayList()
            for (m in explainData[GraphSONTokens.INTERMEDIATE]) {
                intermediates.add(
                    Triplet.with(
                        m[GraphSONTokens.STRATEGY].toString(),
                        m[GraphSONTokens.CATEGORY].toString(),
                        m[GraphSONTokens.TRAVERSAL].toString()
                    )
                )
            }
            return ImmutableExplanation(originalTraversal, intermediates)
        }

        @get:Override
        val isCachable: Boolean
            get() = true
    }

    internal class MetricsJacksonDeserializer : StdDeserializer<Metrics?>(Metrics::class.java) {
        @Override
        @Throws(IOException::class, JsonProcessingException::class)
        fun deserialize(jsonParser: JsonParser?, deserializationContext: DeserializationContext): Metrics {
            val metricsData: Map<String, Object> = deserializationContext.readValue(jsonParser, Map::class.java)
            val m =
                MutableMetrics(metricsData[GraphSONTokens.ID] as String?, metricsData[GraphSONTokens.NAME] as String?)
            m.setDuration(
                Math.round((metricsData[GraphSONTokens.DURATION] as Double?)!! * 1000000),
                TimeUnit.NANOSECONDS
            )
            for (count in (metricsData.getOrDefault(
                GraphSONTokens.COUNTS,
                LinkedHashMap(0)
            ) as Map<String?, Long?>).entrySet()) {
                m.setCount(count.getKey(), count.getValue())
            }
            for (count in (metricsData.getOrDefault(
                GraphSONTokens.ANNOTATIONS,
                LinkedHashMap(0)
            ) as Map<String?, Long?>).entrySet()) {
                m.setAnnotation(count.getKey(), count.getValue())
            }
            for (nested in metricsData.getOrDefault(GraphSONTokens.METRICS, ArrayList(0)) as List<MutableMetrics?>) {
                m.addNested(nested)
            }
            return m
        }

        @get:Override
        val isCachable: Boolean
            get() = true
    }

    internal class TraversalMetricsJacksonDeserializer : StdDeserializer<TraversalMetrics?>(
        TraversalMetrics::class.java
    ) {
        @Override
        @Throws(IOException::class, JsonProcessingException::class)
        fun deserialize(jsonParser: JsonParser?, deserializationContext: DeserializationContext): TraversalMetrics {
            val traversalMetricsData: Map<String, Object> =
                deserializationContext.readValue(jsonParser, Map::class.java)
            return DefaultTraversalMetrics(
                Math.round((traversalMetricsData[GraphSONTokens.DURATION] as Double?)!! * 1000000),
                traversalMetricsData[GraphSONTokens.METRICS] as List<MutableMetrics?>?
            )
        }

        @get:Override
        val isCachable: Boolean
            get() = true
    }

    internal class TreeJacksonDeserializer : StdDeserializer<Tree?>(Tree::class.java) {
        @Override
        @Throws(IOException::class, JsonProcessingException::class)
        fun deserialize(jsonParser: JsonParser?, deserializationContext: DeserializationContext): Tree {
            val data: List<Map> = deserializationContext.readValue(jsonParser, List::class.java)
            val t = Tree()
            for (entry in data) {
                t.put(entry[GraphSONTokens.KEY], entry[GraphSONTokens.VALUE])
            }
            return t
        }

        @get:Override
        val isCachable: Boolean
            get() = true
    }

    internal class IntegerJackonsDeserializer protected constructor() : StdDeserializer<Integer?>(Integer::class.java) {
        @Override
        @Throws(IOException::class, JsonProcessingException::class)
        fun deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext?): Integer {
            return jsonParser.getIntValue()
        }

        @get:Override
        val isCachable: Boolean
            get() = true
    }

    internal class DoubleJacksonDeserializer protected constructor() : StdDeserializer<Double?>(Double::class.java) {
        @Override
        @Throws(IOException::class, JsonProcessingException::class)
        fun deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext?): Double {
            return if (jsonParser.getCurrentToken().isNumeric()) jsonParser.getDoubleValue() else {
                val numberText: String = jsonParser.getValueAsString()
                if ("NaN".equalsIgnoreCase(numberText)) Double.NaN else if ("-Infinity".equals(numberText) || "-INF".equalsIgnoreCase(
                        numberText
                    )
                ) Double.NEGATIVE_INFINITY else if ("Infinity".equals(numberText) || "INF".equals(numberText)) Double.POSITIVE_INFINITY else throw IllegalStateException(
                    "Double value unexpected: $numberText"
                )
            }
        }

        @get:Override
        val isCachable: Boolean
            get() = true
    }
}