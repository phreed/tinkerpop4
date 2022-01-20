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
package org.apache.tinkerpop4.gremlin.process.traversal.step.filter

import org.apache.tinkerpop4.gremlin.process.traversal.Traversal
import org.apache.tinkerpop4.gremlin.process.traversal.Traverser
import org.apache.tinkerpop4.gremlin.process.traversal.step.util.AbstractStep

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
abstract class FilterStep<S>(traversal: Traversal.Admin?) : AbstractStep<S, S>(traversal) {
    @Override
    protected fun processNextStart(): Traverser.Admin<S> {
        while (true) {
            val traverser: Traverser.Admin<S> = this.starts.next()
            if (filter(traverser)) return traverser
        }
    }

    protected abstract fun filter(traverser: Traverser.Admin<S>?): Boolean
}