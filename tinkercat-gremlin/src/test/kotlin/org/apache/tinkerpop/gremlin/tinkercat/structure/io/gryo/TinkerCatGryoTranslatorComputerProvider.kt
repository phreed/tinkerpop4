/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.tinkerpop.gremlin.tinkercat.structure.io.gryo

import org.apache.tinkerpop.gremlin.structure.Graph.OptOut
import org.apache.tinkerpop.gremlin.GraphProvider
import org.apache.tinkerpop.gremlin.tinkercat.process.computer.TinkerCatComputer
import org.apache.tinkerpop.gremlin.tinkercat.structure.io.gryo.TinkerCatGryoTranslatorProvider
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.Graph

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
@OptOut(
    test = "org.apache.tinkerpop.gremlin.process.computer.GraphComputerTest",
    method = "shouldSucceedWithProperTraverserRequirements",
    reason = "Reason requires investigation"
)
@GraphProvider.Descriptor(computer = TinkerCatComputer::class)
class TinkerCatGryoTranslatorComputerProvider : TinkerCatGryoTranslatorProvider() {
    override fun traversal(graph: Graph): GraphTraversalSource {
        return super.traversal(graph).withComputer()
    }
}