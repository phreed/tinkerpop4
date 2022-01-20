/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.tinkerpop4.gremlin.structure.io.graphson

import org.apache.commons.configuration2.ConfigurationConverter

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
internal class TraversalSerializersV3d0 private constructor() {
    /////////////////
    // SERIALIZERS //
    ////////////////
    internal class TraversalJacksonSerializer : StdSerializer<Traversal?>(Traversal::class.java) {
        @Override
        @Throws(IOException::class)
        fun serialize(traversal: Traversal, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider?) {
            jsonGenerator.writeObject(traversal.asAdmin().getBytecode())
        }

        @Override
        @Throws(IOException::class)
        fun serializeWithType(
            traversal: Traversal,
            jsonGenerator: JsonGenerator,
            serializerProvider: SerializerProvider?,
            typeSerializer: TypeSerializer?
        ) {
            serialize(traversal, jsonGenerator, serializerProvider)
        }
    }

    internal class BytecodeJacksonSerializer : StdScalarSerializer<Bytecode?>(Bytecode::class.java) {
        @Override
        @Throws(IOException::class)
        fun serialize(bytecode: Bytecode, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider?) {
            jsonGenerator.writeStartObject()
            if (bytecode.getSourceInstructions().iterator().hasNext()) {
                jsonGenerator.writeArrayFieldStart(GraphSONTokens.SOURCE)
                for (instruction in bytecode.getSourceInstructions()) {
                    jsonGenerator.writeStartArray()
                    jsonGenerator.writeString(instruction.getOperator())
                    for (argument in instruction.getArguments()) {
                        jsonGenerator.writeObject(argument)
                    }
                    jsonGenerator.writeEndArray()
                }
                jsonGenerator.writeEndArray()
            }
            if (bytecode.getStepInstructions().iterator().hasNext()) {
                jsonGenerator.writeArrayFieldStart(GraphSONTokens.STEP)
                for (instruction in bytecode.getStepInstructions()) {
                    jsonGenerator.writeStartArray()
                    jsonGenerator.writeString(instruction.getOperator())
                    for (argument in instruction.getArguments()) {
                        jsonGenerator.writeObject(argument)
                    }
                    jsonGenerator.writeEndArray()
                }
                jsonGenerator.writeEndArray()
            }
            jsonGenerator.writeEndObject()
        }
    }

    internal class EnumJacksonSerializer : StdScalarSerializer<Enum?>(Enum::class.java) {
        @Override
        @Throws(IOException::class)
        fun serialize(enumInstance: Enum, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider?) {
            jsonGenerator.writeString(enumInstance.name())
        }
    }

    internal class PJacksonSerializer : StdScalarSerializer<P?>(P::class.java) {
        @Override
        @Throws(IOException::class)
        fun serialize(p: P, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider?) {
            jsonGenerator.writeStartObject()
            jsonGenerator.writeStringField(
                GraphSONTokens.PREDICATE,
                if (p is ConnectiveP) if (p is AndP) GraphSONTokens.AND else GraphSONTokens.OR else p.getBiPredicate()
                    .toString()
            )
            if (p is ConnectiveP) {
                jsonGenerator.writeArrayFieldStart(GraphSONTokens.VALUE)
                for (predicate in (p as ConnectiveP<*>).getPredicates()) {
                    jsonGenerator.writeObject(predicate)
                }
                jsonGenerator.writeEndArray()
            } else jsonGenerator.writeObjectField(GraphSONTokens.VALUE, p.getValue())
            jsonGenerator.writeEndObject()
        }
    }

    internal class LambdaJacksonSerializer : StdScalarSerializer<Lambda?>(Lambda::class.java) {
        @Override
        @Throws(IOException::class)
        fun serialize(lambda: Lambda, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider?) {
            jsonGenerator.writeStartObject()
            jsonGenerator.writeStringField(GraphSONTokens.SCRIPT, lambda.getLambdaScript())
            jsonGenerator.writeStringField(GraphSONTokens.LANGUAGE, lambda.getLambdaLanguage())
            jsonGenerator.writeNumberField(GraphSONTokens.ARGUMENTS, lambda.getLambdaArguments())
            jsonGenerator.writeEndObject()
        }
    }

    internal class BulkSetJacksonSerializer : StdScalarSerializer<BulkSet?>(BulkSet::class.java) {
        @Override
        @Throws(IOException::class)
        fun serialize(bulkSet: BulkSet, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider?) {
            jsonGenerator.writeStartArray()
            for (entry in bulkSet.asBulk().entrySet()) {
                jsonGenerator.writeObject(entry.getKey())
                jsonGenerator.writeObject(entry.getValue())
            }
            jsonGenerator.writeEndArray()
        }
    }

    internal class BindingJacksonSerializer : StdScalarSerializer<Bytecode.Binding?>(Bytecode.Binding::class.java) {
        @Override
        @Throws(IOException::class)
        fun serialize(
            binding: Bytecode.Binding,
            jsonGenerator: JsonGenerator,
            serializerProvider: SerializerProvider?
        ) {
            jsonGenerator.writeStartObject()
            jsonGenerator.writeStringField(GraphSONTokens.KEY, binding.variable())
            jsonGenerator.writeObjectField(GraphSONTokens.VALUE, binding.value())
            jsonGenerator.writeEndObject()
        }
    }

    internal class TraverserJacksonSerializer : StdScalarSerializer<Traverser?>(Traverser::class.java) {
        @Override
        @Throws(IOException::class)
        fun serialize(
            traverserInstance: Traverser,
            jsonGenerator: JsonGenerator,
            serializerProvider: SerializerProvider?
        ) {
            jsonGenerator.writeStartObject()
            jsonGenerator.writeObjectField(GraphSONTokens.BULK, traverserInstance.bulk())
            jsonGenerator.writeObjectField(GraphSONTokens.VALUE, traverserInstance.get())
            jsonGenerator.writeEndObject()
        }
    }

    internal class TraversalStrategyJacksonSerializer : StdScalarSerializer<TraversalStrategy?>(
        TraversalStrategy::class.java
    ) {
        @Override
        @Throws(IOException::class)
        fun serialize(
            traversalStrategy: TraversalStrategy,
            jsonGenerator: JsonGenerator,
            serializerProvider: SerializerProvider?
        ) {
            jsonGenerator.writeStartObject()
            for (entry in ConfigurationConverter.getMap(traversalStrategy.getConfiguration()).entrySet()) {
                jsonGenerator.writeObjectField(entry.getKey() as String, entry.getValue())
            }
            jsonGenerator.writeEndObject()
        }
    }

    ///////////////////
    // DESERIALIZERS //
    //////////////////
    internal class BytecodeJacksonDeserializer : StdDeserializer<Bytecode?>(Bytecode::class.java) {
        @Override
        @Throws(IOException::class, JsonProcessingException::class)
        fun deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext): Bytecode {
            val bytecode = Bytecode()
            while (jsonParser.nextToken() !== JsonToken.END_OBJECT) {
                val current: String = jsonParser.getCurrentName()
                if (current.equals(GraphSONTokens.SOURCE) || current.equals(GraphSONTokens.STEP)) {
                    jsonParser.nextToken()
                    while (jsonParser.nextToken() !== JsonToken.END_ARRAY) {

                        // there should be a list now and the first item in the list is always string and is the step name
                        // skip the start array
                        jsonParser.nextToken()
                        val stepName: String = jsonParser.getText()

                        // iterate through the rest of the list for arguments until it gets to the end
                        val arguments: List<Object> = ArrayList()
                        while (jsonParser.nextToken() !== JsonToken.END_ARRAY) {
                            // we don't know the types here, so let the deserializer figure that business out
                            arguments.add(deserializationContext.readValue(jsonParser, Object::class.java))
                        }

                        // if it's not a "source" then it must be a "step"
                        if (current.equals(GraphSONTokens.SOURCE)) bytecode.addSource(
                            stepName,
                            arguments.toArray()
                        ) else bytecode.addStep(stepName, arguments.toArray())
                    }
                }
            }
            return bytecode
        }

        @get:Override
        val isCachable: Boolean
            get() = true

        companion object {
            private val listJavaType: JavaType =
                TypeFactory.defaultInstance().constructCollectionType(ArrayList::class.java, Object::class.java)
            private val listListJavaType: JavaType =
                TypeFactory.defaultInstance().constructCollectionType(ArrayList::class.java, listJavaType)
        }
    }

    internal class EnumJacksonDeserializer<A : Enum?>(enumClass: Class<A>?) : StdDeserializer<A>(enumClass) {
        @Override
        @Throws(IOException::class, JsonProcessingException::class)
        fun deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext?): A {
            val enumClass: Class<A> = this._valueClass as Class<A>
            val enumName: String = jsonParser.getText()
            for (a in enumClass.getEnumConstants()) {
                if (a.name().equals(enumName)) return a
            }
            throw IOException("Unknown enum type: $enumClass")
        }

        @get:Override
        val isCachable: Boolean
            get() = true
    }

    internal class PJacksonDeserializer : AbstractReflectJacksonDeserializer<P?>(P::class.java) {
        @Override
        @Throws(IOException::class, JsonProcessingException::class)
        fun deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext): P {
            var predicate: String? = null
            var value: Object? = null
            while (jsonParser.nextToken() !== JsonToken.END_OBJECT) {
                if (jsonParser.getCurrentName().equals(GraphSONTokens.PREDICATE)) {
                    jsonParser.nextToken()
                    predicate = jsonParser.getText()
                } else if (jsonParser.getCurrentName().equals(GraphSONTokens.VALUE)) {
                    jsonParser.nextToken()
                    value = deserializationContext.readValue(jsonParser, Object::class.java)
                }
            }
            return if (predicate!!.equals(GraphSONTokens.AND) || predicate.equals(GraphSONTokens.OR)) {
                if (predicate.equals(GraphSONTokens.AND)) AndP(value as List<P?>?) else OrP(value as List<P?>?)
            } else if (predicate.equals(GraphSONTokens.NOT) && value is P) {
                P.not(value as P<*>?)
            } else {
                try {
                    if (value is Collection) {
                        if (predicate.equals("between")) P.between(
                            (value as List?)!![0],
                            (value as List?)!![1]
                        ) else if (predicate.equals("inside")) P.between(
                            (value as List?)!![0], (value as List?)!![1]
                        ) else if (predicate.equals("outside")) P.outside(
                            (value as List?)!![0], (value as List?)!![1]
                        ) else if (predicate.equals("within")) P.within(value as Collection?) else if (predicate.equals(
                                "without"
                            )
                        ) P.without(value as Collection?) else tryFindMethod(
                            P::class.java, predicate, Collection::class.java
                        ).invoke(null, value as Collection?) as P
                    } else {
                        try {
                            tryFindMethod(P::class.java, predicate, Object::class.java).invoke(null, value) as P
                        } catch (e: NoSuchMethodException) {
                            tryFindMethod(P::class.java, predicate, Array<Object>::class.java).invoke(
                                null,
                                arrayOf<Object?>(value) as Object?
                            ) as P
                        }
                    }
                } catch (e: Exception) {
                    throw IllegalStateException(e.getMessage(), e)
                }
            }
        }

        @get:Override
        val isCachable: Boolean
            get() = true
    }

    /**
     * Deserializers that make reflection calls can use this class as a base and thus cache reflected methods to avoid
     * future lookups.
     */
    internal abstract class AbstractReflectJacksonDeserializer<T>(clazz: Class<out T>?) : StdDeserializer<T>(clazz) {
        private val CACHE: Map<CacheKey, Method> = ConcurrentHashMap()
        @Throws(Exception::class)
        protected fun tryFindMethod(base: Class<*>, methodName: String?, parameterType: Class<*>?): Method {
            return CACHE.computeIfAbsent(
                CacheKey(methodName, parameterType)
            ) { cacheKey ->
                try {
                    return@computeIfAbsent base.getMethod(methodName, parameterType)
                } catch (e: Exception) {
                    throw IllegalStateException(e.getMessage(), e)
                }
            }
        }

        private class CacheKey(predicate: String?, parameterType: Class<*>?) {
            private val predicate: String
            private val parameterType: Class<*>

            init {
                this.predicate = Objects.requireNonNull(predicate)
                this.parameterType = Objects.requireNonNull(parameterType)
            }

            @Override
            override fun equals(o: Object?): Boolean {
                if (this === o) return true
                if (o == null || getClass() !== o.getClass()) return false
                val cacheKey = o as CacheKey
                return predicate.equals(cacheKey.predicate) &&
                        parameterType.equals(cacheKey.parameterType)
            }

            @Override
            override fun hashCode(): Int {
                return Objects.hash(predicate, parameterType)
            }
        }
    }

    internal class TextPJacksonDeserializer : AbstractReflectJacksonDeserializer<TextP?>(TextP::class.java) {
        @Override
        @Throws(IOException::class, JsonProcessingException::class)
        fun deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext): TextP {
            var predicate: String? = null
            var value: String? = null
            while (jsonParser.nextToken() !== JsonToken.END_OBJECT) {
                if (jsonParser.getCurrentName().equals(GraphSONTokens.PREDICATE)) {
                    jsonParser.nextToken()
                    predicate = jsonParser.getText()
                } else if (jsonParser.getCurrentName().equals(GraphSONTokens.VALUE)) {
                    jsonParser.nextToken()
                    value = deserializationContext.readValue(jsonParser, String::class.java)
                }
            }
            return try {
                tryFindMethod(TextP::class.java, predicate, String::class.java).invoke(null, value) as TextP
            } catch (e: Exception) {
                throw IllegalStateException(e.getMessage(), e)
            }
        }

        @get:Override
        val isCachable: Boolean
            get() = true
    }

    internal class LambdaJacksonDeserializer : StdDeserializer<Lambda?>(Lambda::class.java) {
        @Override
        @Throws(IOException::class, JsonProcessingException::class)
        fun deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext?): Lambda {
            var script: String? = null
            var language: String? = null
            var arguments = -1
            while (jsonParser.nextToken() !== JsonToken.END_OBJECT) {
                if (jsonParser.getCurrentName().equals(GraphSONTokens.SCRIPT)) {
                    jsonParser.nextToken()
                    script = jsonParser.getText()
                } else if (jsonParser.getCurrentName().equals(GraphSONTokens.LANGUAGE)) {
                    jsonParser.nextToken()
                    language = jsonParser.getText()
                } else if (jsonParser.getCurrentName().equals(GraphSONTokens.ARGUMENTS)) {
                    jsonParser.nextToken()
                    arguments = jsonParser.getIntValue()
                }
            }
            return if (-1 == arguments || arguments > 2) UnknownArgLambda(
                script,
                language,
                arguments
            ) else if (0 == arguments) ZeroArgLambda(script, language) else if (1 == arguments) OneArgLambda(
                script,
                language
            ) else TwoArgLambda(script, language)
        }

        @get:Override
        val isCachable: Boolean
            get() = true
    }

    internal class BulkSetJacksonDeserializer : StdDeserializer<BulkSet?>(BulkSet::class.java) {
        @Override
        @Throws(IOException::class, JsonProcessingException::class)
        fun deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext): BulkSet {
            val bulkSet: BulkSet<Object> = BulkSet()
            while (jsonParser.nextToken() !== JsonToken.END_ARRAY) {
                val key: Object = deserializationContext.readValue(jsonParser, Object::class.java)
                jsonParser.nextToken()
                val `val`: Long = deserializationContext.readValue(jsonParser, Long::class.java)
                bulkSet.add(key, `val`)
            }
            return bulkSet
        }

        @get:Override
        val isCachable: Boolean
            get() = true
    }

    internal class BindingJacksonDeserializer : StdDeserializer<Bytecode.Binding?>(Bytecode.Binding::class.java) {
        @Override
        @Throws(IOException::class, JsonProcessingException::class)
        fun deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext): Bytecode.Binding {
            var k: String? = null
            var v: Object? = null
            while (jsonParser.nextToken() !== JsonToken.END_OBJECT) {
                if (jsonParser.getCurrentName().equals(GraphSONTokens.KEY)) {
                    jsonParser.nextToken()
                    k = jsonParser.getText()
                } else if (jsonParser.getCurrentName().equals(GraphSONTokens.VALUE)) {
                    jsonParser.nextToken()
                    v = deserializationContext.readValue(jsonParser, Object::class.java)
                }
            }
            return Binding(k, v)
        }

        @get:Override
        val isCachable: Boolean
            get() = true
    }

    internal class TraverserJacksonDeserializer : StdDeserializer<Traverser?>(Traverser::class.java) {
        @Override
        @Throws(IOException::class, JsonProcessingException::class)
        fun deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext): Traverser {
            var bulk: Long = 1
            var v: Object? = null
            while (jsonParser.nextToken() !== JsonToken.END_OBJECT) {
                if (jsonParser.getCurrentName().equals(GraphSONTokens.BULK)) {
                    jsonParser.nextToken()
                    bulk = deserializationContext.readValue(jsonParser, Long::class.java)
                } else if (jsonParser.getCurrentName().equals(GraphSONTokens.VALUE)) {
                    jsonParser.nextToken()
                    v = deserializationContext.readValue(jsonParser, Object::class.java)
                }
            }
            return DefaultRemoteTraverser(v, bulk)
        }

        @get:Override
        val isCachable: Boolean
            get() = true
    }

    internal class TraversalStrategyProxyJacksonDeserializer<T : TraversalStrategy?>(clazz: Class<T>) :
        AbstractObjectDeserializer<TraversalStrategyProxy?>(
            TraversalStrategyProxy::class.java
        ) {
        private val clazz: Class<T>

        init {
            this.clazz = clazz
        }

        @Override
        fun createObject(data: Map<String?, Object?>?): TraversalStrategyProxy<T> {
            return TraversalStrategyProxy(clazz, MapConfiguration(data))
        }
    }
}