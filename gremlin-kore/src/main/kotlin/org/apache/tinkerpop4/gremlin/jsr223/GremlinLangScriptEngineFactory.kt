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

import java.util.Collections

/**
 * A [GremlinScriptEngineFactory] implementation that creates [GremlinLangScriptEngine] instances.
 */
class GremlinLangScriptEngineFactory : AbstractGremlinScriptEngineFactory(
    ENGINE_NAME, LANGUAGE_NAME, EXTENSIONS, Collections.singletonList(
        PLAIN
    )
) {
    @Override
    fun getMethodCallSyntax(obj: String?, m: String?, vararg args: String?): String? {
        return null
    }

    @Override
    fun getOutputStatement(toDisplay: String?): String? {
        return null
    }

    @get:Override
    val scriptEngine: GremlinScriptEngine
        get() {
            val customizers: List<Customizer> = manager.getCustomizers(ENGINE_NAME)
            return if (customizers.isEmpty()) GremlinLangScriptEngine() else GremlinLangScriptEngine(
                customizers.toArray(
                    arrayOfNulls<Customizer>(customizers.size())
                )
            )
        }

    companion object {
        private const val ENGINE_NAME = "gremlin-lang"
        private const val LANGUAGE_NAME = "gremlin-lang"
        private const val PLAIN = "plain"
        private val EXTENSIONS: List<String> = Collections.singletonList("gremlin")
    }
}