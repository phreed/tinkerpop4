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

import org.apache.tinkerpop4.gremlin.language.grammar.GremlinAntlrToJava

/**
 * A [GremlinScriptEngine] implementation that evaluates Gremlin scripts using `gremlin-language`. As it
 * uses `gremlin-language` and thus the ANTLR parser, it is not capable of process arbitrary scripts as the
 * `GremlinGroovyScriptEngine` can and is therefore a more secure Gremlin evaluator. It is obviously restricted
 * to the capabilities of the ANTLR grammar so therefore syntax that includes things like lambdas are not supported.
 * For bytecode evaluation it simply uses the [JavaTranslator].
 *
 *
 * As an internal note, technically, this is an incomplete implementation of the [GremlinScriptEngine] in the
 * traditional sense as a drop-in replacement for something like the `GremlinGroovyScriptEngine`. As a result,
 * this [GremlinScriptEngine] cannot pass the `GremlinScriptEngineSuite` tests in full. On the other hand,
 * this limitation is precisely what makes this implementation better from a security perspective. Ultimately, this
 * implementation represents the first step to changes in what it means to have a [GremlinScriptEngine]. In some
 * sense, there is question why a [GremlinScriptEngine] approach is necessary at all except for easily plugging
 * into the existing internals of Gremlin Server or more specifically the `GremlinExecutor`.
 */
class GremlinLangScriptEngine(vararg customizers: Customizer?) : AbstractScriptEngine(), GremlinScriptEngine {
    @get:Override
    @Volatile
    var factory: GremlinScriptEngineFactory? = null
        get() {
            if (field == null) {
                synchronized(this) {
                    if (field == null) {
                        field = GremlinLangScriptEngineFactory()
                    }
                }
            }
            return field
        }
        private set

    /**
     * Creates a new instance using no [Customizer].
     */
    constructor() : this(*arrayOfNulls<Customizer>(0)) {}

    /**
     * Bytecode is evaluated by the [JavaTranslator].
     */
    @Override
    @Throws(ScriptException::class)
    fun eval(bytecode: Bytecode?, bindings: Bindings, traversalSource: String): Traversal.Admin {
        if (traversalSource.equals(HIDDEN_G)) throw IllegalArgumentException("The traversalSource cannot have the name " + HIDDEN_G.toString() + " - it is reserved")
        if (bindings.containsKey(HIDDEN_G)) throw IllegalArgumentException("Bindings cannot include " + HIDDEN_G.toString() + " - it is reserved")
        if (!bindings.containsKey(traversalSource)) throw IllegalArgumentException("The bindings available to the ScriptEngine do not contain a traversalSource named: $traversalSource")
        val b: Object = bindings.get(traversalSource)
        if (b !is TraversalSource) throw IllegalArgumentException(
            traversalSource + " is of type " + b.getClass()
                .getSimpleName() + " and is not an instance of TraversalSource"
        )
        return JavaTranslator.of(b as TraversalSource).translate(bytecode)
    }

    /**
     * Gremlin scripts evaluated by the grammar must be bound to "g" and should evaluate to a "g" in the
     * `ScriptContext` that is of type [TraversalSource]
     */
    @Override
    @Throws(ScriptException::class)
    fun eval(script: String?, context: ScriptContext): Object {
        val o: Object = context.getAttribute("g")
        if (o !is GraphTraversalSource) throw IllegalArgumentException(
            "g is of type " + o.getClass().getSimpleName().toString() + " and is not an instance of TraversalSource"
        )
        val antlr = GremlinAntlrToJava(o as GraphTraversalSource)
        return try {
            GremlinQueryParser.parse(script, antlr)
        } catch (ex: Exception) {
            throw ScriptException(ex)
        }
    }

    @Override
    @Throws(ScriptException::class)
    fun eval(reader: Reader, context: ScriptContext?): Object {
        return eval(readFully(reader), context)
    }

    @Override
    fun createBindings(): Bindings {
        return SimpleBindings()
    }

    /**
     * Creates the `ScriptContext` using a [GremlinScriptContext] which avoids a significant amount of
     * additional object creation on script evaluation.
     */
    @Override
    protected fun getScriptContext(nn: Bindings?): ScriptContext {
        val ctxt = GremlinScriptContext(context.getReader(), context.getWriter(), context.getErrorWriter())
        val gs: Bindings = getBindings(ScriptContext.GLOBAL_SCOPE)
        if (gs != null) ctxt.setBindings(gs, ScriptContext.GLOBAL_SCOPE)
        if (nn != null) {
            ctxt.setBindings(nn, ScriptContext.ENGINE_SCOPE)
        } else {
            throw NullPointerException("Engine scope Bindings may not be null.")
        }
        return ctxt
    }

    @Throws(ScriptException::class)
    private fun readFully(reader: Reader): String {
        val arr = CharArray(8192)
        val buf = StringBuilder()
        var numChars: Int
        try {
            while (reader.read(arr, 0, arr.size).also { numChars = it } > 0) {
                buf.append(arr, 0, numChars)
            }
        } catch (exp: IOException) {
            throw ScriptException(exp)
        }
        return buf.toString()
    }
}