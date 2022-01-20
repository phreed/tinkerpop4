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
package org.apache.tinkerpop4.gremlin.jsr223

import javax.script.Bindings
import javax.script.ScriptContext

/**
 * Default implementation of the [BindingsCustomizer] which adds bindings to the `ScriptContext.GLOBAL_SCOPE`.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class DefaultBindingsCustomizer internal constructor(bindings: Bindings?, scope: Int) : BindingsCustomizer {
    private val bindings: Bindings?

    @get:Override
    val scope: Int

    /**
     * Creates a new object with `ScriptContext.GLOBAL_SCOPE`.
     */
    constructor(bindings: Bindings?) : this(bindings, ScriptContext.GLOBAL_SCOPE) {}

    /**
     * Creates a new object with a specified scope. There really can't be anything other than a `GLOBAL_SCOPE`
     * specification so this constructor isn't public at the moment. Assigning to `ENGINE_SCOPE` is useless
     * because it is the nature of the `ScriptEngine` to override that scope with `Bindings` supplied at
     * the time of execution.
     */
    init {
        this.bindings = bindings
        this.scope = scope
    }

    @Override
    fun getBindings(): Bindings? {
        return bindings
    }
}