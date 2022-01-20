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
internal object GraphSONSerializersV1d0 {
    @Throws(IOException::class)
    private fun serializerVertexProperty(
        property: VertexProperty, jsonGenerator: JsonGenerator,
        serializerProvider: SerializerProvider,
        typeSerializer: TypeSerializer?, normalize: Boolean,
        includeLabel: Boolean
    ) {
        jsonGenerator.writeStartObject()
        if (typeSerializer != null) jsonGenerator.writeStringField(GraphSONTokens.CLASS, HashMap::class.java.getName())
        GraphSONUtil.writeWithType(GraphSONTokens.ID, property.id(), jsonGenerator, serializerProvider, typeSerializer)
        GraphSONUtil.writeWithType(
            GraphSONTokens.VALUE,
            property.value(),
            jsonGenerator,
            serializerProvider,
            typeSerializer
        )
        if (includeLabel) jsonGenerator.writeStringField(GraphSONTokens.LABEL, property.label())
        tryWriteMetaProperties(property, jsonGenerator, serializerProvider, typeSerializer, normalize)
        jsonGenerator.writeEndObject()
    }

    @Throws(IOException::class)
    private fun tryWriteMetaProperties(
        property: VertexProperty, jsonGenerator: JsonGenerator,
        serializerProvider: SerializerProvider,
        typeSerializer: TypeSerializer?, normalize: Boolean
    ) {
        // when "detached" you can't check features of the graph it detached from so it has to be
        // treated differently from a regular VertexProperty implementation.
        if (property is DetachedVertexProperty) {
            // only write meta properties key if they exist
            if (property.properties().hasNext()) {
                writeMetaProperties(property, jsonGenerator, serializerProvider, typeSerializer, normalize)
            }
        } else {
            // still attached - so we can check the features to see if it's worth even trying to write the
            // meta properties key
            if (property.graph().features().vertex().supportsMetaProperties() && property.properties().hasNext()) {
                writeMetaProperties(property, jsonGenerator, serializerProvider, typeSerializer, normalize)
            }
        }
    }

    @Throws(IOException::class)
    private fun writeMetaProperties(
        property: VertexProperty, jsonGenerator: JsonGenerator,
        serializerProvider: SerializerProvider,
        typeSerializer: TypeSerializer?, normalize: Boolean
    ) {
        jsonGenerator.writeObjectFieldStart(GraphSONTokens.PROPERTIES)
        if (typeSerializer != null) jsonGenerator.writeStringField(GraphSONTokens.CLASS, HashMap::class.java.getName())
        val metaProperties: Iterator<Property<Object>> = if (normalize) IteratorUtils.list(
            property.properties() as Iterator<Property<Object?>?>,
            Comparators.PROPERTY_COMPARATOR
        ).iterator() else property.properties()
        while (metaProperties.hasNext()) {
            val metaProperty: Property<Object> = metaProperties.next()
            GraphSONUtil.writeWithType(
                metaProperty.key(),
                metaProperty.value(),
                jsonGenerator,
                serializerProvider,
                typeSerializer
            )
        }
        jsonGenerator.writeEndObject()
    }

    internal class VertexPropertyJacksonSerializer(private val normalize: Boolean) : StdSerializer<VertexProperty?>(
        VertexProperty::class.java
    ) {
        @Override
        @Throws(IOException::class)
        fun serialize(property: VertexProperty, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider) {
            serializerVertexProperty(property, jsonGenerator, serializerProvider, null, normalize, true)
        }

        @Override
        @Throws(IOException::class)
        fun serializeWithType(
            property: VertexProperty, jsonGenerator: JsonGenerator,
            serializerProvider: SerializerProvider, typeSerializer: TypeSerializer?
        ) {
            serializerVertexProperty(property, jsonGenerator, serializerProvider, typeSerializer, normalize, true)
        }
    }

    internal class PropertyJacksonSerializer : StdSerializer<Property?>(Property::class.java) {
        @Override
        @Throws(IOException::class)
        fun serialize(property: Property, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider) {
            ser(property, jsonGenerator, serializerProvider, null)
        }

        @Override
        @Throws(IOException::class)
        fun serializeWithType(
            property: Property, jsonGenerator: JsonGenerator,
            serializerProvider: SerializerProvider, typeSerializer: TypeSerializer?
        ) {
            ser(property, jsonGenerator, serializerProvider, typeSerializer)
        }

        companion object {
            @Throws(IOException::class)
            private fun ser(
                property: Property, jsonGenerator: JsonGenerator,
                serializerProvider: SerializerProvider, typeSerializer: TypeSerializer?
            ) {
                jsonGenerator.writeStartObject()
                if (typeSerializer != null) jsonGenerator.writeStringField(
                    GraphSONTokens.CLASS,
                    HashMap::class.java.getName()
                )
                serializerProvider.defaultSerializeField(GraphSONTokens.KEY, property.key(), jsonGenerator)
                serializerProvider.defaultSerializeField(GraphSONTokens.VALUE, property.value(), jsonGenerator)
                jsonGenerator.writeEndObject()
            }
        }
    }

    internal class EdgeJacksonSerializer(private val normalize: Boolean) : StdSerializer<Edge?>(Edge::class.java) {
        @Override
        @Throws(IOException::class)
        fun serialize(edge: Edge, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider) {
            ser(edge, jsonGenerator, serializerProvider, null)
        }

        @Override
        @Throws(IOException::class)
        fun serializeWithType(
            edge: Edge, jsonGenerator: JsonGenerator,
            serializerProvider: SerializerProvider, typeSerializer: TypeSerializer?
        ) {
            ser(edge, jsonGenerator, serializerProvider, typeSerializer)
        }

        @Throws(IOException::class)
        private fun ser(
            edge: Edge, jsonGenerator: JsonGenerator,
            serializerProvider: SerializerProvider, typeSerializer: TypeSerializer?
        ) {
            jsonGenerator.writeStartObject()
            if (typeSerializer != null) jsonGenerator.writeStringField(
                GraphSONTokens.CLASS,
                HashMap::class.java.getName()
            )
            GraphSONUtil.writeWithType(GraphSONTokens.ID, edge.id(), jsonGenerator, serializerProvider, typeSerializer)
            jsonGenerator.writeStringField(GraphSONTokens.LABEL, edge.label())
            jsonGenerator.writeStringField(GraphSONTokens.TYPE, GraphSONTokens.EDGE)
            jsonGenerator.writeStringField(GraphSONTokens.IN_LABEL, edge.inVertex().label())
            jsonGenerator.writeStringField(GraphSONTokens.OUT_LABEL, edge.outVertex().label())
            GraphSONUtil.writeWithType(
                GraphSONTokens.IN,
                edge.inVertex().id(),
                jsonGenerator,
                serializerProvider,
                typeSerializer
            )
            GraphSONUtil.writeWithType(
                GraphSONTokens.OUT,
                edge.outVertex().id(),
                jsonGenerator,
                serializerProvider,
                typeSerializer
            )
            writeProperties(edge, jsonGenerator, serializerProvider, typeSerializer)
            jsonGenerator.writeEndObject()
        }

        @Throws(IOException::class)
        private fun writeProperties(
            edge: Edge, jsonGenerator: JsonGenerator,
            serializerProvider: SerializerProvider,
            typeSerializer: TypeSerializer?
        ) {
            val elementProperties: Iterator<Property<Object>> =
                if (normalize) IteratorUtils.list(edge.properties(), Comparators.PROPERTY_COMPARATOR)
                    .iterator() else edge.properties()
            if (elementProperties.hasNext()) {
                jsonGenerator.writeObjectFieldStart(GraphSONTokens.PROPERTIES)
                if (typeSerializer != null) jsonGenerator.writeStringField(
                    GraphSONTokens.CLASS,
                    HashMap::class.java.getName()
                )
                while (elementProperties.hasNext()) {
                    val elementProperty: Property<Object> = elementProperties.next()
                    GraphSONUtil.writeWithType(
                        elementProperty.key(),
                        elementProperty.value(),
                        jsonGenerator,
                        serializerProvider,
                        typeSerializer
                    )
                }
                jsonGenerator.writeEndObject()
            }
        }
    }

    internal class VertexJacksonSerializer(private val normalize: Boolean) : StdSerializer<Vertex?>(
        Vertex::class.java
    ) {
        @Override
        @Throws(IOException::class)
        fun serialize(vertex: Vertex, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider) {
            ser(vertex, jsonGenerator, serializerProvider, null)
        }

        @Override
        @Throws(IOException::class)
        fun serializeWithType(
            vertex: Vertex, jsonGenerator: JsonGenerator,
            serializerProvider: SerializerProvider, typeSerializer: TypeSerializer?
        ) {
            ser(vertex, jsonGenerator, serializerProvider, typeSerializer)
        }

        @Throws(IOException::class)
        private fun ser(
            vertex: Vertex, jsonGenerator: JsonGenerator,
            serializerProvider: SerializerProvider, typeSerializer: TypeSerializer?
        ) {
            jsonGenerator.writeStartObject()
            if (typeSerializer != null) jsonGenerator.writeStringField(
                GraphSONTokens.CLASS,
                HashMap::class.java.getName()
            )
            GraphSONUtil.writeWithType(
                GraphSONTokens.ID,
                vertex.id(),
                jsonGenerator,
                serializerProvider,
                typeSerializer
            )
            jsonGenerator.writeStringField(GraphSONTokens.LABEL, vertex.label())
            jsonGenerator.writeStringField(GraphSONTokens.TYPE, GraphSONTokens.VERTEX)
            writeProperties(vertex, jsonGenerator, serializerProvider, typeSerializer)
            jsonGenerator.writeEndObject()
        }

        @Throws(IOException::class)
        private fun writeProperties(
            vertex: Vertex, jsonGenerator: JsonGenerator,
            serializerProvider: SerializerProvider, typeSerializer: TypeSerializer?
        ) {
            jsonGenerator.writeObjectFieldStart(GraphSONTokens.PROPERTIES)
            if (typeSerializer != null) jsonGenerator.writeStringField(
                GraphSONTokens.CLASS,
                HashMap::class.java.getName()
            )
            val keys: List<String> =
                if (normalize) IteratorUtils.list(vertex.keys().iterator(), Comparator.naturalOrder()) else ArrayList(
                    vertex.keys()
                )
            for (key in keys) {
                val vertexProperties: Iterator<VertexProperty<Object>> =
                    if (normalize) IteratorUtils.list(vertex.properties(key), Comparators.PROPERTY_COMPARATOR)
                        .iterator() else vertex.properties(key)
                if (vertexProperties.hasNext()) {
                    jsonGenerator.writeArrayFieldStart(key)
                    if (typeSerializer != null) {
                        jsonGenerator.writeString(ArrayList::class.java.getName())
                        jsonGenerator.writeStartArray()
                    }
                    while (vertexProperties.hasNext()) {
                        serializerVertexProperty(
                            vertexProperties.next(),
                            jsonGenerator,
                            serializerProvider,
                            typeSerializer,
                            normalize,
                            false
                        )
                    }
                    jsonGenerator.writeEndArray()
                    if (typeSerializer != null) jsonGenerator.writeEndArray()
                }
            }
            jsonGenerator.writeEndObject()
        }
    }

    internal class PathJacksonSerializer : StdSerializer<Path?>(Path::class.java) {
        @Override
        @Throws(IOException::class, JsonGenerationException::class)
        fun serialize(path: Path, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider?) {
            ser(path, jsonGenerator, null)
        }

        @Override
        @Throws(IOException::class, JsonProcessingException::class)
        fun serializeWithType(
            path: Path,
            jsonGenerator: JsonGenerator,
            serializerProvider: SerializerProvider?,
            typeSerializer: TypeSerializer?
        ) {
            ser(path, jsonGenerator, typeSerializer)
        }

        companion object {
            @Throws(IOException::class)
            private fun ser(path: Path, jsonGenerator: JsonGenerator, typeSerializer: TypeSerializer?) {
                jsonGenerator.writeStartObject()
                if (typeSerializer != null) jsonGenerator.writeStringField(
                    GraphSONTokens.CLASS,
                    HashMap::class.java.getName()
                )
                jsonGenerator.writeObjectField(GraphSONTokens.LABELS, path.labels())
                jsonGenerator.writeObjectField(GraphSONTokens.OBJECTS, path.objects())
                jsonGenerator.writeEndObject()
            }
        }
    }

    internal class TreeJacksonSerializer : StdSerializer<Tree?>(Tree::class.java) {
        @Override
        @Throws(IOException::class, JsonGenerationException::class)
        fun serialize(tree: Tree, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider?) {
            ser(tree, jsonGenerator, null)
        }

        @Override
        @Throws(IOException::class, JsonProcessingException::class)
        fun serializeWithType(
            tree: Tree,
            jsonGenerator: JsonGenerator,
            serializerProvider: SerializerProvider?,
            typeSerializer: TypeSerializer?
        ) {
            ser(tree, jsonGenerator, typeSerializer)
        }

        companion object {
            @Throws(IOException::class)
            private fun ser(tree: Tree, jsonGenerator: JsonGenerator, typeSerializer: TypeSerializer?) {
                jsonGenerator.writeStartObject()
                if (typeSerializer != null) jsonGenerator.writeStringField(
                    GraphSONTokens.CLASS,
                    HashMap::class.java.getName()
                )
                val set: Set<Map.Entry<Element, Tree>> = tree.entrySet()
                for (entry in set) {
                    jsonGenerator.writeObjectFieldStart(entry.getKey().id().toString())
                    if (typeSerializer != null) jsonGenerator.writeStringField(
                        GraphSONTokens.CLASS,
                        HashMap::class.java.getName()
                    )
                    jsonGenerator.writeObjectField(GraphSONTokens.KEY, entry.getKey())
                    jsonGenerator.writeObjectField(GraphSONTokens.VALUE, entry.getValue())
                    jsonGenerator.writeEndObject()
                }
                jsonGenerator.writeEndObject()
            }
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

    internal class TraversalExplanationJacksonSerializer : StdSerializer<TraversalExplanation?>(
        TraversalExplanation::class.java
    ) {
        @Override
        @Throws(IOException::class)
        fun serialize(
            traversalExplanation: TraversalExplanation, jsonGenerator: JsonGenerator,
            serializerProvider: SerializerProvider?
        ) {
            ser(traversalExplanation, jsonGenerator)
        }

        @Override
        @Throws(IOException::class)
        fun serializeWithType(
            value: TraversalExplanation, gen: JsonGenerator,
            serializers: SerializerProvider?, typeSer: TypeSerializer?
        ) {
            ser(value, gen)
        }

        @Throws(IOException::class)
        fun ser(te: TraversalExplanation, jsonGenerator: JsonGenerator) {
            val m: Map<String, Object> = HashMap()
            m.put(GraphSONTokens.ORIGINAL, getStepsAsList(te.getOriginalTraversal()))
            val strategyTraversals: List<Pair<TraversalStrategy, Traversal.Admin<*, *>>> = te.getStrategyTraversals()
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
                getStepsAsList(te.getOriginalTraversal())
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

    internal class TraversalMetricsJacksonSerializer : StdSerializer<TraversalMetrics?>(
        TraversalMetrics::class.java
    ) {
        @Override
        @Throws(IOException::class)
        fun serialize(
            property: TraversalMetrics,
            jsonGenerator: JsonGenerator,
            serializerProvider: SerializerProvider?
        ) {
            serializeInternal(property, jsonGenerator)
        }

        @Override
        @Throws(IOException::class)
        fun serializeWithType(
            property: TraversalMetrics, jsonGenerator: JsonGenerator,
            serializerProvider: SerializerProvider?, typeSerializer: TypeSerializer?
        ) {
            serializeInternal(property, jsonGenerator)
        }

        companion object {
            @Throws(IOException::class)
            private fun serializeInternal(traversalMetrics: TraversalMetrics, jsonGenerator: JsonGenerator) {
                // creation of the map enables all the fields to be properly written with their type if required
                val m: Map<String, Object> = HashMap()
                m.put(GraphSONTokens.DURATION, traversalMetrics.getDuration(TimeUnit.NANOSECONDS) / 1000000.0)
                val metrics: List<Map<String, Object>> = ArrayList()
                traversalMetrics.getMetrics().forEach { it -> metrics.add(metricsToMap(it)) }
                m.put(GraphSONTokens.METRICS, metrics)
                jsonGenerator.writeObject(m)
            }

            private fun metricsToMap(metrics: Metrics): Map<String, Object> {
                val m: Map<String, Object> = HashMap()
                m.put(GraphSONTokens.ID, metrics.getId())
                m.put(GraphSONTokens.NAME, metrics.getName())
                m.put(GraphSONTokens.COUNTS, metrics.getCounts())
                m.put(GraphSONTokens.DURATION, metrics.getDuration(TimeUnit.NANOSECONDS) / 1000000.0)
                if (!metrics.getAnnotations().isEmpty()) {
                    m.put(GraphSONTokens.ANNOTATIONS, metrics.getAnnotations())
                }
                if (!metrics.getNested().isEmpty()) {
                    val nested: List<Map<String, Object>> = ArrayList()
                    metrics.getNested().forEach { it -> nested.add(metricsToMap(it)) }
                    m.put(GraphSONTokens.METRICS, nested)
                }
                return m
            }
        }
    }
}