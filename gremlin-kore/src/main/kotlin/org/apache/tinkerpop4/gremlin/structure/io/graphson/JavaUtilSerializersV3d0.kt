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
 * GraphSON serializers for classes in `java.util.*` for the version 3.0 of GraphSON.
 */
internal class JavaUtilSerializersV3d0 private constructor() {
    ////////////////////////////// SERIALIZERS /////////////////////////////////
    internal class MapJacksonSerializer : StdSerializer<Map?>(Map::class.java) {
        @Override
        @Throws(IOException::class)
        fun serialize(map: Map, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider?) {
            for (entry in map.entrySet()) {
                jsonGenerator.writeObject(entry.getKey())
                jsonGenerator.writeObject(entry.getValue())
            }
        }

        @Override
        @Throws(IOException::class)
        fun serializeWithType(
            map: Map, jsonGenerator: JsonGenerator,
            serializerProvider: SerializerProvider?, typeSerializer: TypeSerializer
        ) {
            typeSerializer.writeTypePrefixForObject(map, jsonGenerator)
            serialize(map, jsonGenerator, serializerProvider)
            typeSerializer.writeTypeSuffixForObject(map, jsonGenerator)
        }
    }

    /**
     * Coerces `Map.Entry` to a `Map` with a single entry in it.
     */
    internal class MapEntryJacksonSerializer : StdSerializer<Map.Entry?>(Map.Entry::class.java) {
        @Override
        @Throws(IOException::class)
        fun serialize(entry: Map.Entry?, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider?) {
            val m: Map<Object, Object> = HashMap()
            if (entry != null) m.put(entry.getKey(), entry.getValue())
            jsonGenerator.writeObject(m)
        }

        @Override
        @Throws(IOException::class)
        fun serializeWithType(
            entry: Map.Entry?, jsonGenerator: JsonGenerator,
            serializerProvider: SerializerProvider?, typeSerializer: TypeSerializer?
        ) {
            serialize(entry, jsonGenerator, serializerProvider)
        }
    }

    internal class SetJacksonSerializer : StdSerializer<Set?>(Set::class.java) {
        @Override
        @Throws(IOException::class)
        fun serialize(set: Set, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider?) {
            for (o in set) {
                jsonGenerator.writeObject(o)
            }
        }

        @Override
        @Throws(IOException::class)
        fun serializeWithType(
            set: Set, jsonGenerator: JsonGenerator,
            serializerProvider: SerializerProvider?, typeSerializer: TypeSerializer
        ) {
            typeSerializer.writeTypePrefixForArray(set, jsonGenerator)
            serialize(set, jsonGenerator, serializerProvider)
            typeSerializer.writeTypeSuffixForArray(set, jsonGenerator)
        }
    }

    internal class ListJacksonSerializer : StdSerializer<List?>(List::class.java) {
        @Override
        @Throws(IOException::class)
        fun serialize(list: List, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider?) {
            for (o in list) {
                jsonGenerator.writeObject(o)
            }
        }

        @Override
        @Throws(IOException::class)
        fun serializeWithType(
            list: List, jsonGenerator: JsonGenerator,
            serializerProvider: SerializerProvider?, typeSerializer: TypeSerializer
        ) {
            typeSerializer.writeTypePrefixForArray(list, jsonGenerator)
            serialize(list, jsonGenerator, serializerProvider)
            typeSerializer.writeTypeSuffixForArray(list, jsonGenerator)
        }
    }

    ////////////////////////////// DESERIALIZERS /////////////////////////////////
    internal class MapJacksonDeserializer protected constructor() : StdDeserializer<Map?>(Map::class.java) {
        @Override
        @Throws(IOException::class, JsonProcessingException::class)
        fun deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext): Map {
            val m: Map<Object, Object> = LinkedHashMap()
            while (jsonParser.nextToken() !== JsonToken.END_ARRAY) {
                val key: Object = deserializationContext.readValue(jsonParser, Object::class.java)
                jsonParser.nextToken()
                val `val`: Object = deserializationContext.readValue(jsonParser, Object::class.java)
                m.put(key, `val`)
            }
            return m
        }
    }

    internal class SetJacksonDeserializer protected constructor() : StdDeserializer<Set?>(Set::class.java) {
        @Override
        @Throws(IOException::class, JsonProcessingException::class)
        fun deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext): Set {
            val s: Set<Object> = LinkedHashSet()
            while (jsonParser.nextToken() !== JsonToken.END_ARRAY) {
                s.add(deserializationContext.readValue(jsonParser, Object::class.java))
            }
            return s
        }
    }

    internal class ListJacksonDeserializer protected constructor() : StdDeserializer<List?>(List::class.java) {
        @Override
        @Throws(IOException::class, JsonProcessingException::class)
        fun deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext): List {
            val s: List<Object> = LinkedList()
            while (jsonParser.nextToken() !== JsonToken.END_ARRAY) {
                s.add(deserializationContext.readValue(jsonParser, Object::class.java))
            }
            return s
        }
    }
}