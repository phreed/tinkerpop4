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
package org.apache.tinkerpop4.gremlin.process.traversal.traverser

import org.apache.tinkerpop4.gremlin.process.traversal.Step
import org.apache.tinkerpop4.gremlin.process.traversal.Traversal
import org.apache.tinkerpop4.gremlin.process.traversal.Traverser

/**
 * A [TraverserRequirement] is a list of requirements that a [Traversal] requires of a [Traverser].
 * The less requirements, the simpler the traverser can be (both in terms of space and time constraints).
 * Every [Step] provides its specific requirements via [Step.getRequirements].
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
enum class TraverserRequirement {
    BULK, LABELED_PATH, NESTED_LOOP, OBJECT, ONE_BULK, PATH, SACK, SIDE_EFFECTS, SINGLE_LOOP
}