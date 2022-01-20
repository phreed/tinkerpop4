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

import org.apache.tinkerpop.shaded.jackson.core.JsonGenerator

/**
 * Utility methods for GraphSON serialization. Functions in here might be used by external serializers.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
object GraphSONUtil {
    @Throws(IOException::class)
    fun writeWithType(
        `object`: Object, jsonGenerator: JsonGenerator,
        serializerProvider: SerializerProvider,
        typeSerializer: TypeSerializer?
    ) {
        writeWithType(null, `object`, jsonGenerator, serializerProvider, typeSerializer)
    }

    @Throws(IOException::class)
    fun writeWithType(
        key: String?, `object`: Object, jsonGenerator: JsonGenerator,
        serializerProvider: SerializerProvider,
        typeSerializer: TypeSerializer?
    ) {
        val serializer: JsonSerializer<Object> = serializerProvider.findValueSerializer(`object`.getClass(), null)
        if (typeSerializer != null) {
            // serialize with types embedded
            if (key != null && !key.isEmpty()) jsonGenerator.writeFieldName(key)
            serializer.serializeWithType(`object`, jsonGenerator, serializerProvider, typeSerializer)
        } else {
            // types are not embedded, but use the serializer when possible or else custom serializers will get
            // bypassed and you end up with the default jackson serializer when you don't want it.
            if (key != null && !key.isEmpty()) jsonGenerator.writeFieldName(key)
            serializer.serialize(`object`, jsonGenerator, serializerProvider)
        }
    }

    @Throws(IOException::class)
    fun writeStartObject(o: Object?, jsonGenerator: JsonGenerator, typeSerializer: TypeSerializer?) {
        if (typeSerializer != null) typeSerializer.writeTypePrefixForObject(
            o,
            jsonGenerator
        ) else jsonGenerator.writeStartObject()
    }

    @Throws(IOException::class)
    fun writeEndObject(o: Object?, jsonGenerator: JsonGenerator, typeSerializer: TypeSerializer?) {
        if (typeSerializer != null) typeSerializer.writeTypeSuffixForObject(
            o,
            jsonGenerator
        ) else jsonGenerator.writeEndObject()
    }

    @Throws(IOException::class)
    fun writeStartArray(o: Object?, jsonGenerator: JsonGenerator, typeSerializer: TypeSerializer?) {
        if (typeSerializer != null) typeSerializer.writeTypePrefixForArray(
            o,
            jsonGenerator
        ) else jsonGenerator.writeStartArray()
    }

    @Throws(IOException::class)
    fun writeEndArray(o: Object?, jsonGenerator: JsonGenerator, typeSerializer: TypeSerializer?) {
        if (typeSerializer != null) typeSerializer.writeTypeSuffixForArray(
            o,
            jsonGenerator
        ) else jsonGenerator.writeEndArray()
    }

    fun safeWriteObjectField(jsonGenerator: JsonGenerator, key: String?, value: Object?) {
        try {
            jsonGenerator.writeObjectField(key, value)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
}