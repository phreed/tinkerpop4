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

/**
 * A `ScriptContext` that doesn't create new instances of `Reader` and `Writer` classes on
 * initialization.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class GremlinScriptContext(`in`: Reader, out: Writer, error: Writer) : ScriptContext {
    private var writer: Writer
    private var errorWriter: Writer
    private var reader: Reader
    private var engineScope: Bindings
    private var globalScope: Bindings?

    /**
     * Create a `GremlinScriptContext`.
     */
    init {
        engineScope = SimpleBindings()
        globalScope = null
        reader = `in`
        writer = out
        errorWriter = error
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun setBindings(bindings: Bindings?, scope: Int) {
        when (scope) {
            ENGINE_SCOPE -> {
                if (null == bindings) throw NullPointerException("Engine scope cannot be null.")
                engineScope = bindings
            }
            GLOBAL_SCOPE -> globalScope = bindings
            else -> throw IllegalArgumentException("Invalid scope value.")
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun getAttribute(name: String): Object? {
        checkName(name)
        if (engineScope.containsKey(name)) {
            return getAttribute(name, ENGINE_SCOPE)
        } else if (globalScope != null && globalScope.containsKey(name)) {
            return getAttribute(name, GLOBAL_SCOPE)
        }
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun getAttribute(name: String, scope: Int): Object? {
        checkName(name)
        return when (scope) {
            ENGINE_SCOPE -> engineScope.get(name)
            GLOBAL_SCOPE -> if (globalScope != null) globalScope.get(name) else null
            else -> throw IllegalArgumentException("Illegal scope value.")
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun removeAttribute(name: String, scope: Int): Object? {
        checkName(name)
        return when (scope) {
            ENGINE_SCOPE -> if (getBindings(ENGINE_SCOPE) != null) getBindings(ENGINE_SCOPE).remove(name) else null
            GLOBAL_SCOPE -> if (getBindings(GLOBAL_SCOPE) != null) getBindings(GLOBAL_SCOPE).remove(name) else null
            else -> throw IllegalArgumentException("Illegal scope value.")
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun setAttribute(name: String, value: Object?, scope: Int) {
        checkName(name)
        when (scope) {
            ENGINE_SCOPE -> {
                engineScope.put(name, value)
                return
            }
            GLOBAL_SCOPE -> {
                if (globalScope != null) globalScope.put(name, value)
                return
            }
            else -> throw IllegalArgumentException("Illegal scope value.")
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun getAttributesScope(name: String): Int {
        checkName(name)
        return if (engineScope.containsKey(name)) ENGINE_SCOPE else if (globalScope != null && globalScope.containsKey(
                name
            )
        ) GLOBAL_SCOPE else -1
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun getBindings(scope: Int): Bindings? {
        return if (scope == ENGINE_SCOPE) engineScope else if (scope == GLOBAL_SCOPE) globalScope else throw IllegalArgumentException(
            "Illegal scope value."
        )
    }

    /**
     * {@inheritDoc}
     */
    @get:Override
    val scopes: List<Any>?
        get() = Companion.scopes

    /**
     * {@inheritDoc}
     */
    @Override
    fun getWriter(): Writer {
        return writer
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun getReader(): Reader {
        return reader
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun setReader(reader: Reader) {
        this.reader = reader
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun setWriter(writer: Writer) {
        this.writer = writer
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun getErrorWriter(): Writer {
        return errorWriter
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun setErrorWriter(writer: Writer) {
        errorWriter = writer
    }

    private fun checkName(name: String) {
        Objects.requireNonNull(name)
        if (name.isEmpty()) throw IllegalArgumentException("name cannot be empty")
    }

    companion object {
        private var scopes: List<Integer>? = null

        init {
            scopes = ArrayList(2)
            scopes.add(ENGINE_SCOPE)
            scopes.add(GLOBAL_SCOPE)
            scopes = Collections.unmodifiableList(scopes)
        }
    }
}