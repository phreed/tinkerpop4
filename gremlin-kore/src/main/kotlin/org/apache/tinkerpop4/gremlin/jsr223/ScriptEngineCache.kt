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

import javax.script.ScriptEngine

/**
 * A cache of standard `ScriptEngine` instances, instantiated by the standard `ScriptEngineManager`.
 * These instances are NOT "Gremlin-enabled". See [SingleGremlinScriptEngineManager] for the analogous class
 * that loads [GremlinScriptEngine] instances.
 *
 * @author Daniel Kuppitz (http://gremlin.guru)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
object ScriptEngineCache {
    const val DEFAULT_SCRIPT_ENGINE = "gremlin-groovy"
    private val SCRIPT_ENGINE_MANAGER: ScriptEngineManager = ScriptEngineManager()
    private val CACHED_ENGINES: Map<String, ScriptEngine> = ConcurrentHashMap()
    operator fun get(engineName: String): ScriptEngine {
        return CACHED_ENGINES.compute(engineName) { key, engine ->
            if (null == engine) {
                engine = SCRIPT_ENGINE_MANAGER.getEngineByName(engineName)
                if (null == engine) {
                    throw IllegalArgumentException("There is no script engine with provided name: $engineName")
                }
            }
            engine
        }
    }
}