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
package org.apache.tinkerpop4.gremlin.process.traversal.step.sideEffect

import org.apache.tinkerpop4.gremlin.process.traversal.Failure

/**
 * Triggers an immediate failure of the traversal by throwing a `RuntimeException`. The exception thrown must
 * implement the [Failure] interface.
 */
class FailStep<S>(traversal: Traversal.Admin?, protected var message: String?, metadata: Map<String, Object>) :
    SideEffectStep<S>(traversal) {
    protected var metadata: Map<String, Object>

    constructor(traversal: Traversal.Admin?) : this(traversal, "fail() step triggered") {}
    constructor(traversal: Traversal.Admin?, message: String?) : this(traversal, message, Collections.emptyMap()) {}

    init {
        this.metadata = metadata
    }

    @Override
    protected fun sideEffect(traverser: Traverser.Admin<S>?) {
        throw FailException(traversal, traverser, message, metadata)
    }

    /**
     * Default [Failure] implementation that is thrown by [FailStep].
     */
    class FailException(
        traversal: Traversal.Admin, traverser: Traverser.Admin?,
        message: String?, metadata: Map<String, Object>
    ) : RuntimeException(message), Failure {
        private val metadata: Map<String, Object>
        private val traversal: Traversal.Admin
        private val traverser: Traverser.Admin?

        init {
            this.metadata = metadata
            this.traversal = traversal
            this.traverser = traverser
        }

        @Override
        fun getMetadata(): Map<String, Object> {
            return metadata
        }

        @Override
        fun getTraverser(): Traverser.Admin? {
            return traverser
        }

        @Override
        fun getTraversal(): Traversal.Admin {
            return traversal
        }
    }
}