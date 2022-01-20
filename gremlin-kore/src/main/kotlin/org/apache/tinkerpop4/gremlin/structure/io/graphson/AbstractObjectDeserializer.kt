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

import org.apache.tinkerpop.shaded.jackson.core.JsonParser

/**
 * Base class for creating deserializers which parses JSON to a `Map` to more easily reconstruct an object.
 * Generally speaking greater performance can be attained with deserializer development that directly uses the
 * `JsonParser`.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
abstract class AbstractObjectDeserializer<T> protected constructor(clazz: Class<T>?) : StdDeserializer<T>(clazz) {
    @Override
    @Throws(IOException::class, JsonProcessingException::class)
    fun deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext): T {
        jsonParser.nextToken()

        // This will automatically parse all typed stuff.
        val mapData: Map<String?, Object> = deserializationContext.readValue(jsonParser, LinkedHashMap::class.java)
        return createObject(mapData)
    }

    @get:Override
    val isCachable: Boolean
        get() = true

    abstract fun createObject(data: Map<String?, Object?>?): T
}