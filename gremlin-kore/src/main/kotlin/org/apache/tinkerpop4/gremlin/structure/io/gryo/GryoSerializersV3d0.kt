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
package org.apache.tinkerpop4.gremlin.structure.io.gryo

import org.apache.tinkerpop4.gremlin.process.remote.traversal.DefaultRemoteTraverser

/**
 * This class holds serializers for graph-based objects such as vertices, edges, properties, and paths. These objects
 * are "detached" using [DetachedFactory] before serialization. These serializers present a generalized way to
 * serialize the implementations of core interfaces. These serializers are versioned and not compatible with Gryo 1.0.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
object GryoSerializersV3d0 {
    private fun writeElementProperties(kryo: KryoShim, output: OutputShim, element: Element?) {
        val properties: Iterator<Property?> = element.properties()
        output.writeBoolean(properties.hasNext())
        while (properties.hasNext()) {
            val p: Property? = properties.next()
            output.writeString(p.key())
            kryo.writeClassAndObject(output, p.value())
            output.writeBoolean(properties.hasNext())
        }
    }

    /**
     * Serializes any [Edge] implementation encountered to a [DetachedEdge].
     */
    class EdgeSerializer : SerializerShim<Edge?> {
        @Override
        fun <O : OutputShim?> write(kryo: KryoShim<*, O>, output: O, edge: Edge) {
            kryo.writeClassAndObject(output, edge.id())
            output.writeString(edge.label())
            kryo.writeClassAndObject(output, edge.inVertex().id())

            // temporary try/catch perhaps? need this to get SparkSingleIterationStrategyTest to work. Trying to grab
            // the label of the adjacent vertex ends in error if there's a StarEdge in the ComputerGraph$ComputerEdge.
            // apparently this is how we handle things in DetachedElement. i'll write here in the comments what was
            // written there:
            //
            // ghetto
            try {
                output.writeString(edge.inVertex().label())
            } catch (ex: Exception) {
                output.writeString(Vertex.DEFAULT_LABEL)
            }
            kryo.writeClassAndObject(output, edge.outVertex().id())

            // same nonsense as above for a default label
            try {
                output.writeString(edge.outVertex().label())
            } catch (ex: Exception) {
                output.writeString(Vertex.DEFAULT_LABEL)
            }
            writeElementProperties(kryo, output, edge)
        }

        @Override
        fun <I : InputShim?> read(kryo: KryoShim<I, *>, input: I, edgeClass: Class<Edge?>?): Edge {
            val builder: DetachedEdge.Builder = DetachedEdge.build()
            builder.setId(kryo.readClassAndObject(input))
            builder.setLabel(input.readString())
            val inV: DetachedVertex.Builder = DetachedVertex.build()
            inV.setId(kryo.readClassAndObject(input))
            inV.setLabel(input.readString())
            builder.setInV(inV.create())
            val outV: DetachedVertex.Builder = DetachedVertex.build()
            outV.setId(kryo.readClassAndObject(input))
            outV.setLabel(input.readString())
            builder.setOutV(outV.create())
            while (input.readBoolean()) {
                builder.addProperty(DetachedProperty(input.readString(), kryo.readClassAndObject(input)))
            }
            return builder.create()
        }
    }

    /**
     * Serializes any [Vertex] implementation encountered to an [DetachedVertex].
     */
    class VertexSerializer : SerializerShim<Vertex?> {
        @Override
        fun <O : OutputShim?> write(kryo: KryoShim<*, O>, output: O, vertex: Vertex) {
            kryo.writeClassAndObject(output, vertex.id())
            output.writeString(vertex.label())
            val properties: Iterator<VertexProperty?> = vertex.properties()
            output.writeBoolean(properties.hasNext())
            while (properties.hasNext()) {
                val vp: VertexProperty? = properties.next()
                kryo.writeClassAndObject(output, vp.id())
                output.writeString(vp.label())
                kryo.writeClassAndObject(output, vp.value())
                if (vp is DetachedVertexProperty || vertex.graph().features().vertex().supportsMetaProperties()) {
                    writeElementProperties(kryo, output, vp)
                } else {
                    output.writeBoolean(false)
                }
                output.writeBoolean(properties.hasNext())
            }
        }

        @Override
        fun <I : InputShim?> read(kryo: KryoShim<I, *>, input: I, vertexClass: Class<Vertex?>?): Vertex {
            val builder: DetachedVertex.Builder = DetachedVertex.build()
            builder.setId(kryo.readClassAndObject(input))
            builder.setLabel(input.readString())
            while (input.readBoolean()) {
                val vpBuilder: DetachedVertexProperty.Builder = DetachedVertexProperty.build()
                vpBuilder.setId(kryo.readClassAndObject(input))
                vpBuilder.setLabel(input.readString())
                vpBuilder.setValue(kryo.readClassAndObject(input))
                while (input.readBoolean()) {
                    vpBuilder.addProperty(DetachedProperty(input.readString(), kryo.readClassAndObject(input)))
                }
                builder.addProperty(vpBuilder.create())
            }
            return builder.create()
        }
    }

    /**
     * Serializes any [Property] implementation encountered to an [DetachedProperty].
     */
    class PropertySerializer : SerializerShim<Property?> {
        @Override
        fun <O : OutputShim?> write(kryo: KryoShim<*, O>, output: O, property: Property) {
            output.writeString(property.key())
            kryo.writeClassAndObject(output, property.value())
            kryo.writeClassAndObject(output, property.element().id())
            output.writeString(property.element().label())
        }

        @Override
        fun <I : InputShim?> read(kryo: KryoShim<I, *>, input: I, propertyClass: Class<Property?>?): Property {
            return DetachedProperty(
                input.readString(), kryo.readClassAndObject(input),
                DetachedVertex.build().setId(kryo.readClassAndObject(input)).setLabel(input.readString()).create()
            )
        }
    }

    /**
     * Serializes any [VertexProperty] implementation encountered to an [DetachedVertexProperty].
     */
    class VertexPropertySerializer : SerializerShim<VertexProperty?> {
        @Override
        fun <O : OutputShim?> write(kryo: KryoShim<*, O>, output: O, vertexProperty: VertexProperty) {
            kryo.writeClassAndObject(output, vertexProperty.id())
            output.writeString(vertexProperty.label())
            kryo.writeClassAndObject(output, vertexProperty.value())
            kryo.writeClassAndObject(output, vertexProperty.element().id())
            output.writeString(vertexProperty.element().label())
            if (vertexProperty is DetachedVertexProperty || vertexProperty.graph().features().vertex()
                    .supportsMetaProperties()
            ) {
                writeElementProperties(kryo, output, vertexProperty)
            } else {
                output.writeBoolean(false)
            }
        }

        @Override
        fun <I : InputShim?> read(
            kryo: KryoShim<I, *>,
            input: I,
            vertexPropertyClass: Class<VertexProperty?>?
        ): VertexProperty {
            val vpBuilder: DetachedVertexProperty.Builder = DetachedVertexProperty.build()
            vpBuilder.setId(kryo.readClassAndObject(input))
            vpBuilder.setLabel(input.readString())
            vpBuilder.setValue(kryo.readClassAndObject(input))
            val host: DetachedVertex.Builder = DetachedVertex.build()
            host.setId(kryo.readClassAndObject(input))
            host.setLabel(input.readString())
            vpBuilder.setV(host.create())
            while (input.readBoolean()) {
                vpBuilder.addProperty(DetachedProperty(input.readString(), kryo.readClassAndObject(input)))
            }
            return vpBuilder.create()
        }
    }

    /**
     * Serializes any [Path] implementation encountered to an [DetachedPath].
     */
    class PathSerializer : SerializerShim<Path?> {
        @Override
        fun <O : OutputShim?> write(kryo: KryoShim<*, O>, output: O, path: Path?) {
            kryo.writeClassAndObject(output, DetachedFactory.detach(path, false))
        }

        @Override
        fun <I : InputShim?> read(kryo: KryoShim<I, *>, input: I, pathClass: Class<Path?>?): Path {
            return kryo.readClassAndObject(input) as Path
        }
    }

    class BytecodeSerializer : SerializerShim<Bytecode?> {
        @Override
        fun <O : OutputShim?> write(kryo: KryoShim<*, O>, output: O, bytecode: Bytecode) {
            writeInstructions(kryo, output, bytecode.getSourceInstructions())
            writeInstructions(kryo, output, bytecode.getStepInstructions())
        }

        @Override
        fun <I : InputShim?> read(kryo: KryoShim<I, *>, input: I, clazz: Class<Bytecode?>?): Bytecode {
            val bytecode = Bytecode()
            val sourceInstructionCount: Int = input.readInt()
            for (ix in 0 until sourceInstructionCount) {
                val operator: String = input.readString()
                val args: Array<Object> =
                    if (operator.equals(TraversalSource.Symbols.withoutStrategies)) kryo.readObject(
                        input,
                        Array<Class>::class.java
                    ) else kryo.readObject(input, Array<Object>::class.java)
                bytecode.addSource(operator, args)
            }
            val stepInstructionCount: Int = input.readInt()
            for (ix in 0 until stepInstructionCount) {
                val operator: String = input.readString()
                val args: Array<Object> = kryo.readObject(input, Array<Object>::class.java)
                bytecode.addStep(operator, args)
            }
            return bytecode
        }

        companion object {
            private fun <O : OutputShim?> writeInstructions(
                kryo: KryoShim<*, O>, output: O,
                instructions: List<Bytecode.Instruction>
            ) {
                output.writeInt(instructions.size())
                for (inst in instructions) {
                    output.writeString(inst.getOperator())
                    kryo.writeObject(output, inst.getArguments())
                }
            }
        }
    }

    class PSerializer : SerializerShim<P?> {
        @Override
        fun <O : OutputShim?> write(kryo: KryoShim<*, O>, output: O, p: P) {
            output.writeString(if (p is ConnectiveP) if (p is AndP) "and" else "or" else p.getBiPredicate().toString())
            if (p is ConnectiveP || p.getValue() is Collection) {
                output.writeByte(0.toByte())
                val coll = if (p is ConnectiveP) (p as ConnectiveP<*>).getPredicates() else (p.getValue() as Collection)
                output.writeInt(coll.size())
                coll.forEach { v -> kryo.writeClassAndObject(output, v) }
            } else {
                output.writeByte(1.toByte())
                kryo.writeClassAndObject(output, p.getValue())
            }
        }

        @Override
        fun <I : InputShim?> read(kryo: KryoShim<I, *>, input: I, clazz: Class<P?>?): P {
            val predicate: String = input.readString()
            val isCollection = input.readByte() === 0.toByte()
            val value: Object
            if (isCollection) {
                value = ArrayList()
                val size: Int = input.readInt()
                for (ix in 0 until size) {
                    (value as List).add(kryo.readClassAndObject(input))
                }
            } else {
                value = kryo.readClassAndObject(input)
            }
            return try {
                if (predicate.equals("and") || predicate.equals("or")) if (predicate.equals("and")) AndP(value as List<P?>) else OrP(
                    value as List<P?>
                ) else if (value is Collection) {
                    if (predicate.equals("between")) P.between(
                        value[0],
                        value[1]
                    ) else if (predicate.equals("inside")) P.inside(
                        value[0], value[1]
                    ) else if (predicate.equals("outside")) P.outside(
                        value[0],
                        value[1]
                    ) else if (predicate.equals("within")) P.within(value as Collection) else if (predicate.equals("without")) P.without(
                        value as Collection
                    ) else P::class.java.getMethod(predicate, Collection::class.java)
                        .invoke(null, value as Collection) as P
                } else P::class.java.getMethod(predicate, Object::class.java).invoke(null, value) as P
            } catch (e: Exception) {
                throw IllegalStateException(e.getMessage(), e)
            }
        }
    }

    class TextPSerializer : SerializerShim<TextP?> {
        @Override
        fun <O : OutputShim?> write(kryo: KryoShim<*, O>, output: O, p: TextP) {
            output.writeString(p.getBiPredicate().toString())
            kryo.writeObject(output, p.getValue())
        }

        @Override
        fun <I : InputShim?> read(kryo: KryoShim<I, *>, input: I, clazz: Class<TextP?>?): TextP {
            val predicate: String = input.readString()
            val value: String = kryo.readObject(input, String::class.java)
            return try {
                TextP::class.java.getMethod(predicate, String::class.java).invoke(null, value) as TextP
            } catch (e: Exception) {
                throw IllegalStateException(e.getMessage(), e)
            }
        }
    }

    class LambdaSerializer : SerializerShim<Lambda?> {
        @Override
        fun <O : OutputShim?> write(kryo: KryoShim<*, O>?, output: O, lambda: Lambda) {
            output.writeString(lambda.getLambdaScript())
            output.writeString(lambda.getLambdaLanguage())
            output.writeInt(lambda.getLambdaArguments())
        }

        @Override
        fun <I : InputShim?> read(kryo: KryoShim<I, *>?, input: I, clazz: Class<Lambda?>?): Lambda {
            val script: String = input.readString()
            val language: String = input.readString()
            val arguments: Int = input.readInt()
            //
            return if (-1 == arguments || arguments > 2) UnknownArgLambda(
                script,
                language,
                arguments
            ) else if (0 == arguments) ZeroArgLambda(script, language) else if (1 == arguments) OneArgLambda(
                script,
                language
            ) else TwoArgLambda(script, language)
        }
    }

    class BindingSerializer : SerializerShim<Bytecode.Binding?> {
        @Override
        fun <O : OutputShim?> write(kryo: KryoShim<*, O>, output: O, binding: Bytecode.Binding) {
            output.writeString(binding.variable())
            kryo.writeClassAndObject(output, binding.value())
        }

        @Override
        fun <I : InputShim?> read(kryo: KryoShim<I, *>, input: I, clazz: Class<Bytecode.Binding?>?): Bytecode.Binding {
            val `var`: String = input.readString()
            val `val`: Object = kryo.readClassAndObject(input)
            return Binding(`var`, `val`)
        }
    }

    class DefaultRemoteTraverserSerializer : SerializerShim<DefaultRemoteTraverser?> {
        @Override
        fun <O : OutputShim?> write(kryo: KryoShim<*, O>, output: O, remoteTraverser: DefaultRemoteTraverser) {
            kryo.writeClassAndObject(output, remoteTraverser.get())
            output.writeLong(remoteTraverser.bulk())
        }

        @Override
        fun <I : InputShim?> read(
            kryo: KryoShim<I, *>,
            input: I,
            remoteTraverserClass: Class<DefaultRemoteTraverser?>?
        ): DefaultRemoteTraverser {
            val o: Object = kryo.readClassAndObject(input)
            return DefaultRemoteTraverser(o, input.readLong())
        }
    }

    class TraversalMetricsSerializer : SerializerShim<TraversalMetrics?> {
        @Override
        fun <O : OutputShim?> write(kryo: KryoShim<*, O>, output: O, `object`: TraversalMetrics) {
            output.writeDouble(`object`.getDuration(TimeUnit.NANOSECONDS) / 1000000.0)
            val metrics: Collection<Metrics?> = `object`.getMetrics()
            output.writeInt(metrics.size())
            metrics.forEach { m -> kryo.writeObject(output, m) }
        }

        @Override
        fun <I : InputShim?> read(kryo: KryoShim<I, *>, input: I, clazz: Class<TraversalMetrics?>?): TraversalMetrics {
            val duration: Double = input.readDouble()
            val size: Int = input.readInt()
            val orderedMetrics: List<MutableMetrics> = ArrayList()
            for (ix in 0 until size) {
                orderedMetrics.add(kryo.readObject(input, MutableMetrics::class.java))
            }
            return DefaultTraversalMetrics(Math.round(duration * 1000000), orderedMetrics)
        }
    }

    class MetricsSerializer : SerializerShim<Metrics?> {
        @Override
        fun <O : OutputShim?> write(kryo: KryoShim<*, O>, output: O, `object`: Metrics) {
            output.writeString(`object`.getId())
            output.writeString(`object`.getName())
            output.writeDouble(`object`.getDuration(TimeUnit.NANOSECONDS) / 1000000.0)
            kryo.writeObject(output, `object`.getCounts())

            // annotations is a synchronized LinkedHashMap - get rid of the "synch" for serialization as gryo
            // doesn't know how to deserialize that well and LinkedHashMap should work with 3.3.x and previous
            val annotations: Map<String, Object> = LinkedHashMap()
            `object`.getAnnotations().forEach(annotations::put)
            kryo.writeObject(output, annotations)

            // kryo might have a problem with LinkedHashMap value collections. can't recreate it independently but
            // it gets fixed with standard collections for some reason.
            val nested: List<Metrics> = ArrayList(`object`.getNested())
            kryo.writeObject(output, nested)
        }

        @Override
        fun <I : InputShim?> read(kryo: KryoShim<I, *>, input: I, clazz: Class<Metrics?>?): Metrics {
            val m = MutableMetrics(input.readString(), input.readString())
            m.setDuration(Math.round(input.readDouble() * 1000000), TimeUnit.NANOSECONDS)
            val counts = kryo.readObject(input, HashMap::class.java) as Map<String, Long>
            for (count in counts.entrySet()) {
                m.setCount(count.getKey(), count.getValue())
            }
            for (count in kryo.readObject(input, HashMap::class.java).entrySet()) {
                m.setAnnotation(count.getKey(), count.getValue())
            }
            for (nested in kryo.readObject(input, ArrayList::class.java)) {
                m.addNested(nested)
            }
            return m
        }
    }
}