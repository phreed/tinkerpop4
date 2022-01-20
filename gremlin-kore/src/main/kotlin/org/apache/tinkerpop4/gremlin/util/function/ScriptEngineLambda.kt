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
package org.apache.tinkerpop4.gremlin.util.function

import org.apache.tinkerpop4.gremlin.jsr223.SingleGremlinScriptEngineManager

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class ScriptEngineLambda(engineName: String?, script: String) : Function, Supplier, Consumer, Predicate, BiConsumer,
    TriConsumer {
    protected val engine: ScriptEngine
    protected val script: String

    init {
        engine = SingleGremlinScriptEngineManager.get(engineName)
        this.script = script
    }

    fun apply(a: Object?): Object {
        return try {
            val bindings: Bindings = SimpleBindings()
            bindings.put(A, a)
            engine.eval(script, bindings)
        } catch (e: ScriptException) {
            throw IllegalArgumentException(e.getMessage())
        }
    }

    fun get(): Object {
        return try {
            engine.eval(script)
        } catch (e: ScriptException) {
            throw IllegalArgumentException(e.getMessage())
        }
    }

    fun accept(a: Object?) {
        try {
            val bindings: Bindings = SimpleBindings()
            bindings.put(A, a)
            engine.eval(script, bindings)
        } catch (e: ScriptException) {
            throw IllegalArgumentException(e.getMessage())
        }
    }

    fun accept(a: Object?, b: Object?) {
        try {
            val bindings: Bindings = SimpleBindings()
            bindings.put(A, a)
            bindings.put(B, b)
            engine.eval(script, bindings)
        } catch (e: ScriptException) {
            throw IllegalArgumentException(e.getMessage())
        }
    }

    fun accept(a: Object?, b: Object?, c: Object?) {
        try {
            val bindings: Bindings = SimpleBindings()
            bindings.put(A, a)
            bindings.put(B, b)
            bindings.put(C, c)
            engine.eval(script, bindings)
        } catch (e: ScriptException) {
            throw IllegalArgumentException(e.getMessage())
        }
    }

    fun test(a: Object?): Boolean {
        return try {
            val bindings: Bindings = SimpleBindings()
            bindings.put(A, a)
            engine.eval(script, bindings)
        } catch (e: ScriptException) {
            throw IllegalArgumentException(e.getMessage())
        }
    }

    companion object {
        private const val A = "a"
        private const val B = "b"
        private const val C = "c"
    }
}