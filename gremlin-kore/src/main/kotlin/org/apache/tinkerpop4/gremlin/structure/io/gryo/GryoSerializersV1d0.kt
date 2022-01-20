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
import org.apache.tinkerpop4.gremlin.process.traversal.Bytecode
import org.apache.tinkerpop4.gremlin.process.traversal.P
import org.apache.tinkerpop4.gremlin.process.traversal.Path
import org.apache.tinkerpop4.gremlin.process.traversal.TextP
import org.apache.tinkerpop4.gremlin.process.traversal.TraversalSource
import org.apache.tinkerpop4.gremlin.process.traversal.util.AndP
import org.apache.tinkerpop4.gremlin.process.traversal.util.ConnectiveP
import org.apache.tinkerpop4.gremlin.process.traversal.util.OrP
import org.apache.tinkerpop4.gremlin.structure.Edge
import org.apache.tinkerpop4.gremlin.structure.Property
import org.apache.tinkerpop4.gremlin.structure.Vertex
import org.apache.tinkerpop4.gremlin.structure.VertexProperty
import org.apache.tinkerpop4.gremlin.structure.io.gryo.kryoshim.InputShim
import org.apache.tinkerpop4.gremlin.structure.io.gryo.kryoshim.KryoShim
import org.apache.tinkerpop4.gremlin.structure.io.gryo.kryoshim.OutputShim
import org.apache.tinkerpop4.gremlin.structure.io.gryo.kryoshim.SerializerShim
import org.apache.tinkerpop4.gremlin.structure.util.detached.DetachedEdge
import org.apache.tinkerpop4.gremlin.structure.util.detached.DetachedFactory
import org.apache.tinkerpop4.gremlin.structure.util.detached.DetachedPath
import org.apache.tinkerpop4.gremlin.structure.util.detached.DetachedProperty
import org.apache.tinkerpop4.gremlin.structure.util.detached.DetachedVertex
import org.apache.tinkerpop4.gremlin.structure.util.detached.DetachedVertexProperty
import org.apache.tinkerpop4.gremlin.util.function.Lambda
import java.util.ArrayList
import java.util.Collection
import java.util.List

/**
 * This class holds serializers for graph-based objects such as vertices, edges, properties, and paths. These objects
 * are "detached" using [DetachedFactory] before serialization. These serializers present a generalized way to
 * serialize the implementations of core interfaces. These are serializers for Gryo 1.0.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class GryoSerializersV1d0 {
    /**
     * Serializes any [Edge] implementation encountered to a [DetachedEdge].
     */
    class EdgeSerializer : SerializerShim<Edge?> {
        @Override
        fun <O : OutputShim?> write(kryo: KryoShim<*, O>, output: O, edge: Edge?) {
            kryo.writeClassAndObject(output, DetachedFactory.detach(edge, true))
        }

        @Override
        fun <I : InputShim?> read(kryo: KryoShim<I, *>, input: I, edgeClass: Class<Edge?>?): Edge {
            val o: Object = kryo.readClassAndObject(input)
            return o as Edge
        }
    }

    /**
     * Serializes any [Vertex] implementation encountered to an [DetachedVertex].
     */
    class VertexSerializer : SerializerShim<Vertex?> {
        @Override
        fun <O : OutputShim?> write(kryo: KryoShim<*, O>, output: O, vertex: Vertex?) {
            kryo.writeClassAndObject(output, DetachedFactory.detach(vertex, true))
        }

        @Override
        fun <I : InputShim?> read(kryo: KryoShim<I, *>, input: I, vertexClass: Class<Vertex?>?): Vertex {
            return kryo.readClassAndObject(input) as Vertex
        }
    }

    /**
     * Serializes any [Property] implementation encountered to an [DetachedProperty].
     */
    class PropertySerializer : SerializerShim<Property?> {
        @Override
        fun <O : OutputShim?> write(kryo: KryoShim<*, O>, output: O, property: Property?) {
            kryo.writeClassAndObject(
                output,
                if (property is VertexProperty) DetachedFactory.detach(
                    property as VertexProperty?,
                    true
                ) else DetachedFactory.detach(property)
            )
        }

        @Override
        fun <I : InputShim?> read(kryo: KryoShim<I, *>, input: I, propertyClass: Class<Property?>?): Property {
            return kryo.readClassAndObject(input) as Property
        }
    }

    /**
     * Serializes any [VertexProperty] implementation encountered to an [DetachedVertexProperty].
     */
    class VertexPropertySerializer : SerializerShim<VertexProperty?> {
        @Override
        fun <O : OutputShim?> write(kryo: KryoShim<*, O>, output: O, vertexProperty: VertexProperty?) {
            kryo.writeClassAndObject(output, DetachedFactory.detach(vertexProperty, true))
        }

        @Override
        fun <I : InputShim?> read(
            kryo: KryoShim<I, *>,
            input: I,
            vertexPropertyClass: Class<VertexProperty?>?
        ): VertexProperty {
            return kryo.readClassAndObject(input) as VertexProperty
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
}