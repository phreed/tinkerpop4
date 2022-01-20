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

import org.apache.tinkerpop.shaded.jackson.core.JsonParseException

/**
 * Utility class to easily concatenate multiple JsonParsers. This class had to be implemented because the class it is
 * extending, [JsonParserSequence], inevitably skips a token when switching from one empty parser to a new one.
 * I.e. it is automatically calling [JsonParser.nextToken] when switching to the new parser, ignoring
 * the current token.
 *
 * This class is used for high performance in GraphSON when trying to detect types.
 *
 * @author Kevin Gallardo (https://kgdo.me)
 */
class JsonParserConcat protected constructor(parsers: Array<JsonParser>?) : JsonParserSequence(parsers) {
    @Override
    @Throws(IOException::class, JsonParseException::class)
    fun nextToken(): JsonToken? {
        var t: JsonToken = this.delegate.nextToken()
        return if (t != null) {
            t
        } else {
            do {
                if (!this.switchToNext()) {
                    return null
                }
                // call getCurrentToken() instead of nextToken() in JsonParserSequence.
                t = if (this.delegate.getCurrentToken() == null) nextToken() else this.getCurrentToken()
            } while (t == null)
            t
        }
    }

    companion object {
        fun createFlattened(first: JsonParser, second: JsonParser): JsonParserConcat {
            return if (first !is JsonParserConcat && second !is JsonParserConcat) {
                JsonParserConcat(arrayOf<JsonParser>(first, second))
            } else {
                val p = ArrayList()
                if (first is JsonParserConcat) {
                    (first as JsonParserConcat).addFlattenedActiveParsers(p)
                } else {
                    p.add(first)
                }
                if (second is JsonParserConcat) {
                    (second as JsonParserConcat).addFlattenedActiveParsers(p)
                } else {
                    p.add(second)
                }
                JsonParserConcat(p.toArray(arrayOfNulls<JsonParser>(p.size())) as Array<JsonParser>)
            }
        }
    }
}