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
package org.apache.tinkerpop4.gremlin.process.traversal.step.util

import org.apache.tinkerpop4.gremlin.process.traversal.dsl.graph.GraphTraversal

/**
 * Configuration options to be passed to the [GraphTraversal.with].
 *
 * @author Daniel Kuppitz (http://gremlin.guru)
 */
object WithOptions {
    //
    // PropertyMapStep
    //
    /**
     * Configures the tokens to be included in value maps.
     */
    val tokens: String = Graph.Hidden.hide("tinkerpop.valueMap.tokens")

    /**
     * Include no tokens.
     */
    var none = 0

    /**
     * Include ids (affects all [Element] value maps).
     */
    var ids = 1

    /**
     * Include labels (affects all [Vertex] and [Edge] value maps).
     */
    var labels = 2

    /**
     * Include keys (affects all [VertexProperty] value maps).
     */
    var keys = 4

    /**
     * Include keys (affects all [VertexProperty] value maps).
     */
    var values = 8

    /**
     * Include all tokens.
     */
    var all = ids or labels or keys or values
    //
    // IndexStep
    //
    /**
     * Configures the indexer to be used in [IndexStep].
     */
    val indexer: String = Graph.Hidden.hide("tinkerpop.index.indexer")

    /**
     * Index items using 2-item lists.
     */
    var list = 0

    /**
     * Index items using a `LinkedHashMap`.
     */
    var map = 1
}