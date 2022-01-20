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
package org.apache.tinkerpop4.gremlin.structure.io.graphml

/**
 * A collection of tokens used for GraphML related data.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
internal object GraphMLTokens {
    const val XML_SCHEMA_NAMESPACE_TAG = "xsi"
    const val DEFAULT_GRAPHML_SCHEMA_LOCATION = "http://graphml.graphdrawing.org/xmlns/1.1/graphml.xsd"
    const val XML_SCHEMA_LOCATION_ATTRIBUTE = "schemaLocation"
    const val GRAPHML = "graphml"
    const val XMLNS = "xmlns"
    const val GRAPHML_XMLNS = "http://graphml.graphdrawing.org/xmlns"
    const val G = "G"
    const val EDGEDEFAULT = "edgedefault"
    const val DIRECTED = "directed"
    const val KEY = "key"
    const val FOR = "for"
    const val ID = "id"
    const val ATTR_NAME = "attr.name"
    const val ATTR_TYPE = "attr.type"
    const val GRAPH = "graph"
    const val NODE = "node"
    const val EDGE = "edge"
    const val SOURCE = "source"
    const val TARGET = "target"
    const val DATA = "data"
    const val LABEL_E = "labelE"
    const val LABEL_V = "labelV"
    const val STRING = "string"
    const val FLOAT = "float"
    const val DOUBLE = "double"
    const val LONG = "long"
    const val BOOLEAN = "boolean"
    const val INT = "int"
    const val VERTEX_SUFFIX = "V"
    const val EDGE_SUFFIX = "E"
}