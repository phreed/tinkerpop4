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

import org.apache.tinkerpop4.gremlin.process.traversal.Bytecode

/**
 * A `GremlinScriptEngine` is an extension of the standard `ScriptEngine` and provides some specific
 * methods that are important to the TinkerPop environment.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
interface GremlinScriptEngine : ScriptEngine {
    @get:Override
    val factory: GremlinScriptEngineFactory?

    /**
     * Evaluates [Traversal] [Bytecode] against a traversal source in the global bindings of the
     * `ScriptEngine`.
     *
     * @param bytecode of the traversal to execute
     * @param traversalSource to execute the bytecode against which should be in the available bindings.
     */
    @Throws(ScriptException::class)
    fun eval(bytecode: Bytecode, traversalSource: String?): Traversal.Admin? {
        val bindings: Bindings = this.createBindings()
        val ctx: ScriptContext = this.getContext()
        val gbindings: Bindings = ctx.getBindings(ScriptContext.GLOBAL_SCOPE)
        if (gbindings != null) bindings.putAll(gbindings)
        val ebindings: Bindings = ctx.getBindings(ScriptContext.ENGINE_SCOPE)
        if (ebindings != null) bindings.putAll(ebindings)
        bindings.putAll(bytecode.getBindings())
        return eval(bytecode, bindings, traversalSource)
    }

    /**
     * Evaluates [Traversal] [Bytecode] with the specified `Bindings`. These `Bindings`
     * supplied to this method will be merged with global engine bindings and override them where keys match.
     */
    @Throws(ScriptException::class)
    fun eval(bytecode: Bytecode?, bindings: Bindings?, traversalSource: String?): Traversal.Admin?

    companion object {
        const val HIDDEN_G = "gremlinscriptengine__g"
    }
}