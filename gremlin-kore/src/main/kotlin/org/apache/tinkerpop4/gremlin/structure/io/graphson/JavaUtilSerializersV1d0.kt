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

import org.apache.tinkerpop4.gremlin.structure.Element

/**
 * GraphSON serializers for classes in `java.util.*`.
 */
internal class JavaUtilSerializersV1d0 private constructor() {
    internal class MapEntryJacksonSerializer : StdSerializer<Map.Entry?>(Map.Entry::class.java) {
        @Override
        @Throws(IOException::class)
        fun serialize(entry: Map.Entry, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider) {
            ser(entry, jsonGenerator, serializerProvider, null)
        }

        @Override
        @Throws(IOException::class)
        fun serializeWithType(
            entry: Map.Entry, jsonGenerator: JsonGenerator,
            serializerProvider: SerializerProvider, typeSerializer: TypeSerializer?
        ) {
            ser(entry, jsonGenerator, serializerProvider, typeSerializer)
        }

        companion object {
            @Throws(IOException::class)
            private fun ser(
                entry: Map.Entry, jsonGenerator: JsonGenerator,
                serializerProvider: SerializerProvider, typeSerializer: TypeSerializer?
            ) {
                jsonGenerator.writeStartObject()
                if (typeSerializer != null) jsonGenerator.writeStringField(
                    GraphSONTokens.CLASS,
                    HashMap::class.java.getName()
                )

                // this treatment of keys is consistent with the current GraphSONKeySerializer which extends the
                // StdKeySerializer
                val key: Object = entry.getKey()
                val cls: Class = key.getClass()
                val k: String
                if (cls === String::class.java) k = key else if (Element::class.java.isAssignableFrom(cls)) k =
                    (key as Element).id().toString() else if (Date::class.java.isAssignableFrom(cls)) {
                    if (serializerProvider.isEnabled(SerializationFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS)) k =
                        String.valueOf((key as Date).getTime()) else k =
                        serializerProvider.getConfig().getDateFormat().format(key as Date)
                } else if (cls === Class::class.java) k = (key as Class).getName() else k = key.toString()
                serializerProvider.defaultSerializeField(k, entry.getValue(), jsonGenerator)
                jsonGenerator.writeEndObject()
            }
        }
    }
}