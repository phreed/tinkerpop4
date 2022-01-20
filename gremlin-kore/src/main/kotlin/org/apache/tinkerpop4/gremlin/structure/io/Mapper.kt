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
package org.apache.tinkerpop4.gremlin.structure.io

import org.apache.tinkerpop4.gremlin.structure.Graph
import java.util.List

/**
 * Represents a low-level serialization class that can be used to map classes to serializers.  These implementation
 * create instances of serializers from other libraries (e.g. creating a `Kryo` instance).
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
interface Mapper<T> {
    /**
     * Create a new instance of the internal object mapper that an implementation represents.
     */
    fun createMapper(): T

    /**
     * Largely a marker interface for builders that construct [Mapper] instances.
     */
    interface Builder<B : Builder<*>?> {
        /**
         * Adds a vendor supplied [IoRegistry] to the `Mapper.Builder` which enables it to check for
         * vendor custom serializers to add to the [Mapper].  All [Io] implementations should expose
         * this method via this [Builder] so that it is compatible with [Graph.io]. Successive calls
         * to this method will add multiple registries.  Registry order must be respected when doing so.  In
         * other words, data written with [IoRegistry] `A` added first and `B` second must be read
         * by a `Mapper` with that same registry ordering.  Attempting to add `B` before `A` will
         * result in errors.
         */
        fun addRegistry(registry: IoRegistry?): B

        /**
         * Adds a vendor supplied [IoRegistry] to the `Mapper.Builder` which enables it to check for
         * vendor custom serializers to add to the [Mapper].  All [Io] implementations should expose
         * this method via this [Builder] so that it is compatible with [Graph.io]. Successive calls
         * to this method will add multiple registries.  Registry order must be respected when doing so.  In
         * other words, data written with [IoRegistry] `A` added first and `B` second must be read
         * by a `Mapper` with that same registry ordering.  Attempting to add `B` before `A` will
         * result in errors.
         */
        fun addRegistries(registries: List<IoRegistry?>): B {
            var b = this as B
            for (registry in registries) {
                b = addRegistry(registry)
            }
            return b
        }
    }
}