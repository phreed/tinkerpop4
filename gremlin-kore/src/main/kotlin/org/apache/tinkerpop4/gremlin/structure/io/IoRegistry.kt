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
import org.apache.tinkerpop4.gremlin.structure.io.graphson.GraphSONIo
import org.apache.tinkerpop4.gremlin.structure.io.gryo.GryoIo
import org.javatuples.Pair
import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.HashMap
import java.util.List
import java.util.Map
import java.util.stream.Collectors

/**
 * A generalized custom serializer registry for providers implementing a [Graph].  Providers should develop an
 * implementation of this interface if their implementation requires custom serialization of identifiers or other
 * such content housed in their graph.  Consider extending from [AbstractIoRegistry] and ensure that the
 * implementation has a zero-arg constructor or a static "instance" method that returns an `IoRegistry`
 * instance, as implementations may need to be constructed from reflection given different parts of the TinkerPop
 * stack.
 *
 *
 * The serializers to register depend on the [Io] implementations that are expected to be supported.  There
 * are currently two core implementations in [GryoIo] and [GraphSONIo].  Both of these should be supported
 * for full compliance with the test suite.
 *
 *
 * There is no need to use this class if the [Graph] does not have custom classes that require serialization.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
interface IoRegistry {
    /**
     * Find a list of all the serializers registered to an [Io] class by the [Graph].
     */
    fun find(builderClass: Class<out Io?>?): List<Pair<Class?, Object?>?>?

    /**
     * Find a list of all the serializers, of a particular type, registered to an [Io] class by the
     * [Graph].
     */
    fun <S> find(builderClass: Class<out Io?>?, serializerType: Class<S>?): List<Pair<Class?, S>?>?

    companion object {
        const val IO_REGISTRY = "gremlin.io.registry"
    }
}