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

import org.apache.tinkerpop4.gremlin.process.traversal.Step

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class HasNextStep<S>(traversal: Traversal.Admin?) : AbstractStep<S, Boolean?>(traversal) {
    @Override
    @Throws(NoSuchElementException::class)
    protected fun processNextStart(): Traverser.Admin<Boolean> {
        return if (this.starts.hasNext()) this.starts.next().split(Boolean.TRUE, this) else this.getTraversal()
            .getTraverserGenerator().generate(Boolean.FALSE, this as Step, 1L)
    }
}