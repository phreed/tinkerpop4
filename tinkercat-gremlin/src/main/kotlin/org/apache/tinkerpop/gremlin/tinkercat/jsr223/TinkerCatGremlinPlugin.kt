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
package org.apache.tinkerpop.gremlin.tinkercat.jsr223;

import org.apache.tinkerpop.gremlin.jsr223.AbstractGremlinPlugin;
import org.apache.tinkerpop.gremlin.jsr223.DefaultImportCustomizer;
import org.apache.tinkerpop.gremlin.jsr223.ImportCustomizer;
import org.apache.tinkerpop.gremlin.tinkercat.process.computer.TinkerCatComputer;
import org.apache.tinkerpop.gremlin.tinkercat.process.computer.TinkerCatComputerView;
import org.apache.tinkerpop.gremlin.tinkercat.process.computer.TinkerMapEmitter;
import org.apache.tinkerpop.gremlin.tinkercat.process.computer.TinkerMemory;
import org.apache.tinkerpop.gremlin.tinkercat.process.computer.TinkerMessenger;
import org.apache.tinkerpop.gremlin.tinkercat.process.computer.TinkerReduceEmitter;
import org.apache.tinkerpop.gremlin.tinkercat.process.computer.TinkerWorkerPool;
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerEdge;
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerElement;
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerFactory;
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerCat;
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerCatVariables;
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerHelper;
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerIoRegistryV1d0;
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerIoRegistryV2d0;
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerIoRegistryV3d0;
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerProperty;
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerVertex;
import org.apache.tinkerpop.gremlin.tinkercat.structure.TinkerVertexProperty;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public final class TinkerCatGremlinPlugin extends AbstractGremlinPlugin {
    private static final String NAME = "tinkerpop.tinkercat";

    private static final ImportCustomizer imports = DefaultImportCustomizer.build()
            .addClassImports(TinkerEdge.class,
                    TinkerElement.class,
                    TinkerFactory.class,
                    TinkerCat.class,
                    TinkerCatVariables.class,
                    TinkerHelper.class,
                    TinkerIoRegistryV1d0.class,
                    TinkerIoRegistryV2d0.class,
                    TinkerIoRegistryV3d0.class,
                    TinkerProperty.class,
                    TinkerVertex.class,
                    TinkerVertexProperty.class,
                    TinkerCatComputer.class,
                    TinkerCatComputerView.class,
                    TinkerMapEmitter.class,
                    TinkerMemory.class,
                    TinkerMessenger.class,
                    TinkerReduceEmitter.class,
                    TinkerWorkerPool.class).create();

    private static final TinkerCatGremlinPlugin instance = new TinkerCatGremlinPlugin();

    public TinkerCatGremlinPlugin() {
        super(NAME, imports);
    }

    public static TinkerCatGremlinPlugin instance() {
        return instance;
    }
}