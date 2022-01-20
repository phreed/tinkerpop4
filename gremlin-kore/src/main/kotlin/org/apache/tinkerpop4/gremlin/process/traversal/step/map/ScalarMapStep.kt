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
package org.apache.tinkerpop4.gremlin.process.traversal.step.map

import org.apache.tinkerpop4.gremlin.process.traversal.Traversal
import org.apache.tinkerpop4.gremlin.process.traversal.Traverser

/**
 * A type of [MapStep] class which will transform the object of one [Traverser] into another. This class
 * simply requires the implementation of the [.map] method to extract the object of the given
 * [Traverser] and return the transformation of that object as `E`.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
abstract class ScalarMapStep<S, E>(traversal: Traversal.Admin?) : MapStep<S, E>(traversal) {
    @Override
    protected fun processNextStart(): Traverser.Admin<E> {
        val traverser: Traverser.Admin<S> = this.starts.next()
        return traverser.split(map(traverser), this)
    }

    protected abstract fun map(traverser: Traverser.Admin<S>?): E
}