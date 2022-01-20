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
package org.apache.tinkerpop4.gremlin.structure.util.star

import java.util.HashMap

/**
 * A wrapper for [StarGraphSerializer] that makes it compatible with TinkerPop's
 * shaded Kryo.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class StarGraphGryoSerializer private constructor(edgeDirectionToSerialize: Direction?, graphFilter: GraphFilter) :
    ShadedSerializerAdapter<StarGraph?>(StarGraphSerializer(edgeDirectionToSerialize, graphFilter)) {
    private constructor(edgeDirectionToSerialize: Direction?) : this(edgeDirectionToSerialize, GraphFilter()) {}

    companion object {
        private val CACHE: Map<Direction, StarGraphGryoSerializer> = HashMap()

        init {
            CACHE.put(Direction.BOTH, StarGraphGryoSerializer(Direction.BOTH))
            CACHE.put(Direction.IN, StarGraphGryoSerializer(Direction.IN))
            CACHE.put(Direction.OUT, StarGraphGryoSerializer(Direction.OUT))
            CACHE.put(null, StarGraphGryoSerializer(null))
        }

        /**
         * Gets a serializer from the cache.  Use `null` for the direction when requiring a serializer that
         * doesn't serialize the edges of a vertex.
         */
        fun with(direction: Direction): StarGraphGryoSerializer? {
            return CACHE[direction]
        }

        fun withGraphFilter(graphFilter: GraphFilter): StarGraphGryoSerializer {
            return StarGraphGryoSerializer(Direction.BOTH, graphFilter.clone())
        }
    }
}