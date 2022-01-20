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

import org.apache.tinkerpop4.gremlin.util.Gremlin
import javax.script.ScriptEngine
import java.util.Collections
import java.util.List

/**
 * A simple base implementation of the [GremlinScriptEngineFactory].
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
abstract class AbstractGremlinScriptEngineFactory(
    @get:Override val engineName: String, @get:Override val languageName: String,
    extensions: List<String?>?, mimeTypes: List<String?>?
) : GremlinScriptEngineFactory {

    @get:Override
    val extensions: List<String>

    @get:Override
    val mimeTypes: List<String>
    protected var manager: GremlinScriptEngineManager? = null

    init {
        this.extensions = Collections.unmodifiableList(extensions)
        this.mimeTypes = Collections.unmodifiableList(mimeTypes)
    }

    @Override
    fun setCustomizerManager(manager: GremlinScriptEngineManager?) {
        this.manager = manager
    }

    @get:Override
    val engineVersion: String
        get() = Gremlin.version()

    @get:Override
    val languageVersion: String
        get() = Gremlin.version()

    @get:Override
    val names: List<String>
        get() = Collections.singletonList(languageName)

    @Override
    fun getParameter(key: String): Object? {
        return if (key.equals(ScriptEngine.ENGINE)) {
            engineName
        } else if (key.equals(ScriptEngine.ENGINE_VERSION)) {
            engineVersion
        } else if (key.equals(ScriptEngine.NAME)) {
            engineName
        } else if (key.equals(ScriptEngine.LANGUAGE)) {
            languageName
        } else if (key.equals(ScriptEngine.LANGUAGE_VERSION)) {
            languageVersion
        } else null
    }

    /**
     * Statements are concatenated together by a line feed.
     */
    @Override
    fun getProgram(vararg statements: String?): String {
        val program = StringBuilder()
        for (statement in statements) {
            program.append(statement).append(System.lineSeparator())
        }
        return program.toString()
    }
}