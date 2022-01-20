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
package org.apache.tinkerpop4.gremlin.structure

import org.apache.tinkerpop4.gremlin.process.traversal.Path
import java.util.ArrayList
import java.util.HashSet
import java.util.LinkedHashSet
import java.util.Map
import java.util.function.Function

/**
 * Column references a particular type of column in a complex data structure such as a `Map`, a
 * `Map.Entry`, or a [Path].
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
enum class Column : Function<Object?, Object?> {
    /**
     * The keys associated with the data structure.
     */
    keys {
        @Override
        fun apply(`object`: Object): Object {
            return if (`object` is Map) LinkedHashSet((`object` as Map<*, *>).keySet()) else if (`object` is Map.Entry) (`object` as Map.Entry).getKey() else if (`object` is Path) ArrayList(
                (`object` as Path).labels()
            ) else throw IllegalArgumentException("The provided object does not have accessible keys: " + `object`.getClass())
        }
    },

    /**
     * The values associated with the data structure.
     */
    values {
        @Override
        fun apply(`object`: Object): Object {
            return if (`object` is Map) ArrayList((`object` as Map<*, *>).values()) else if (`object` is Map.Entry) (`object` as Map.Entry).getValue() else if (`object` is Path) ArrayList(
                (`object` as Path).objects()
            ) else throw IllegalArgumentException("The provided object does not have accessible keys: " + `object`.getClass())
        }
    }
}