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

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
object GraphSONTokens {
    const val CLASS = "@class"
    const val VALUETYPE = "@type"
    const val VALUEPROP = "@value"
    const val ID = "id"
    const val TYPE = "type"
    const val VALUE = "value"
    const val PROPERTIES = "properties"
    const val KEY = "key"
    const val EDGE = "edge"
    const val EDGES = "edges"
    const val ELEMENT = "ELEMENT"
    const val VERTEX = "vertex"
    const val VERTEX_PROPERTY = "vertexProperty"
    const val VERTICES = "vertices"
    const val IN = "inV"
    const val OUT = "outV"
    const val IN_E = "inE"
    const val OUT_E = "outE"
    const val LABEL = "label"
    const val LABELS = "labels"
    const val OBJECTS = "objects"
    const val IN_LABEL = "inVLabel"
    const val OUT_LABEL = "outVLabel"
    const val GREMLIN_TYPE_NAMESPACE = "g"
    const val GREMLINX_TYPE_NAMESPACE = "gx"

    // TraversalExplanation Tokens
    const val ORIGINAL = "original"
    const val FINAL = "final"
    const val INTERMEDIATE = "intermediate"
    const val CATEGORY = "category"
    const val TRAVERSAL = "traversal"
    const val STRATEGY = "strategy"

    // TraversalMetrics Tokens
    const val METRICS = "metrics"
    const val DURATION = "dur"
    const val NAME = "name"
    const val COUNTS = "counts"
    const val ANNOTATIONS = "annotations"
    const val BULK = "bulk"
    const val SCRIPT = "script"
    const val LANGUAGE = "language"
    const val ARGUMENTS = "arguments"
    const val PREDICATE = "predicate"
    const val AND = "and"
    const val NOT = "not"
    const val OR = "or"
    const val SOURCE = "source"
    const val STEP = "step"
}