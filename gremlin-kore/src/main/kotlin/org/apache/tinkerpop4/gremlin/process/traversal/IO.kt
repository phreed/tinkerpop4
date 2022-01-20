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
package org.apache.tinkerpop4.gremlin.process.traversal

import org.apache.tinkerpop4.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop4.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop4.gremlin.structure.Graph
import org.apache.tinkerpop4.gremlin.structure.io.GraphReader
import org.apache.tinkerpop4.gremlin.structure.io.GraphWriter
import org.apache.tinkerpop4.gremlin.structure.io.IoRegistry
import org.apache.tinkerpop4.gremlin.structure.io.graphml.GraphMLReader
import org.apache.tinkerpop4.gremlin.structure.io.graphml.GraphMLWriter
import org.apache.tinkerpop4.gremlin.structure.io.graphson.GraphSONReader
import org.apache.tinkerpop4.gremlin.structure.io.graphson.GraphSONWriter
import org.apache.tinkerpop4.gremlin.structure.io.gryo.GryoReader
import org.apache.tinkerpop4.gremlin.structure.io.gryo.GryoWriter

/**
 * Fields that can be provided to the [GraphTraversalSource.io] using the
 * [GraphTraversal.with] step modulator to provide additional configurations.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
object IO {
    /**
     * A value to supply to [IO.reader] or [IO.writer] to indicate the format to use. Using this shorthand
     * will configure a default [GraphSONReader] or [GraphSONWriter] respectively,
     */
    const val graphson = "graphson"

    /**
     * A value to supply to [IO.reader] or [IO.writer] to indicate the format to use. Using this shorthand
     * will configure a default [GryoReader] or [GryoWriter] respectively,
     */
    const val gryo = "gryo"

    /**
     * A value to supply to [IO.reader] or [IO.writer] to indicate the format to use. Using this shorthand
     * will configure a default [GraphMLReader] or [GraphMLWriter] respectively,
     */
    const val graphml = "graphml"

    /**
     * The specific [GraphReader] instance to use, the name of the fully qualified classname of such an
     * instance or one of [IO.graphson], [IO.gryo] or [IO.graphml]. If this value is not specified
     * then [GraphTraversalSource.io] will attempt to construct a default [GraphReader] based on
     * the file extension provided to it.
     */
    val reader: String = Graph.Hidden.hide("tinkerpop.io.reader")

    /**
     * The specific [GraphWriter] instance to use, the name of the fully qualified classname of such an
     * instance or one of [IO.graphson], [IO.gryo] or [IO.graphml]. If this value is not specified
     * then [GraphTraversalSource.io] will attempt to construct a default [GraphWriter] based on
     * the file extension provided to it.
     */
    val writer: String = Graph.Hidden.hide("tinkerpop.io.writer")

    /**
     * A key that identifies the fully qualified class names of [IoRegistry] instances to use. May be specified
     * multiple times (i.e. once for each registry) using the [GraphTraversal.with] modulator.
     */
    val registry: String = Graph.Hidden.hide("tinkerpop.io.registry")
}