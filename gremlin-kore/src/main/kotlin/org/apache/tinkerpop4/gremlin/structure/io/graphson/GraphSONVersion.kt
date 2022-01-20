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

import GraphSONModule.GraphSONModuleBuilder

/**
 * The set of available GraphSON versions.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
enum class GraphSONVersion(builder: GraphSONModuleBuilder, versionNumber: String) {
    V1_0(GraphSONModule.GraphSONModuleV1d0.build(), "1.0"), V2_0(
        GraphSONModule.GraphSONModuleV2d0.build(),
        "2.0"
    ),
    V3_0(GraphSONModule.GraphSONModuleV3d0.build(), "3.0");

    private val builder: GraphSONModuleBuilder
    val version: String

    init {
        this.builder = builder
        version = versionNumber
    }

    fun getBuilder(): GraphSONModuleBuilder {
        return builder
    }
}