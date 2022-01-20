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
package org.apache.tinkerpop4.gremlin.process.traversal.step.branch;

import org.apache.tinkerpop4.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop4.gremlin.process.traversal.dsl.graph.Anon;
import org.apache.tinkerpop4.gremlin.process.traversal.step.StepTest;

import java.util.Arrays;
import java.util.List;

import static org.apache.tinkerpop4.gremlin.process.traversal.dsl.graph.Anon.hasLabel;
import static org.apache.tinkerpop4.gremlin.process.traversal.dsl.graph.Anon.out;

/**
 * @author Daniel Kuppitz (http://gremlin.guru)
 */
public class RepeatStepTest extends StepTest {

    @Override
    protected List<Traversal> getTraversals() {
        return Arrays.asList(
                __.repeat(out()).times(3),
                __.repeat(out().as("x")).times(3),
                __.out().emit().repeat(out()).times(3),
                __.repeat(out()).until(hasLabel("x")),
                __.repeat("a", __.out()).times(3),
                __.repeat(out().repeat(out()).times(1)).times(1).limit(1)
        );
    }
}
