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
package org.apache.tinkerpop4.gremlin.tinkercat.jsr223

import org.apache.tinkerpop4.gremlin.jsr223.AbstractGremlinPlugin
import org.apache.tinkerpop4.gremlin.jsr223.DefaultImportCustomizer
import org.apache.tinkerpop4.gremlin.jsr223.ImportCustomizer
import org.apache.tinkerpop4.gremlin.tinkercat.jsr223.TinkerCatGremlinPlugin
import org.apache.tinkerpop4.gremlin.tinkercat.process.computer.*
import org.apache.tinkerpop4.gremlin.tinkercat.structure.*

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
object TinkerCatGremlinPlugin : AbstractGremlinPlugin("") {
    private const val NAME = "tinkerpop.tinkercat"
    private val imports: ImportCustomizer = DefaultImportCustomizer.build()
        .addClassImports(
            TinkerEdge::class.java,
            TinkerElement::class.java,
            TinkerFactory::class.java,
            TinkerCat::class.java,
            TinkerCatVariables::class.java,
            TinkerHelper::class.java,
            TinkerProperty::class.java,
            TinkerVertex::class.java,
            TinkerVertexProperty::class.java,
            TinkerCatComputer::class.java,
            TinkerCatComputerView::class.java,
            TinkerMapEmitter::class.java,
            TinkerMemory::class.java,
            TinkerMessenger::class.java,
            TinkerReduceEmitter::class.java,
            TinkerWorkerPool::class.java
        ).create()
    private val instance: TinkerCatGremlinPlugin = TinkerCatGremlinPlugin
    fun instance(): TinkerCatGremlinPlugin {
        return instance
    }
}