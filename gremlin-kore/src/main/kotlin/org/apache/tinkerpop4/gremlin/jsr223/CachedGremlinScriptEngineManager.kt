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

import java.util.concurrent.ConcurrentHashMap

/**
 * An implementation of the [GremlinScriptEngineManager] that caches the instances of the
 * [GremlinScriptEngine] instances that are created by it. Note that the cache is relevant to the instance
 * of the [CachedGremlinScriptEngineManager] and is not global to the JVM.
 *
 *
 * {@inheritDoc}
 */
class CachedGremlinScriptEngineManager : DefaultGremlinScriptEngineManager {
    private val cache: ConcurrentHashMap<String, GremlinScriptEngine> = ConcurrentHashMap()
    private val extensionToName: ConcurrentHashMap<String, String> = ConcurrentHashMap()
    private val mimeToName: ConcurrentHashMap<String, String> = ConcurrentHashMap()

    /**
     * @see DefaultGremlinScriptEngineManager.DefaultGremlinScriptEngineManager
     */
    constructor() : super() {}

    /**
     * @see DefaultGremlinScriptEngineManager.DefaultGremlinScriptEngineManager
     */
    constructor(loader: ClassLoader?) : super(loader) {}

    /**
     * Gets a [GremlinScriptEngine] from cache or creates a new one from the [GremlinScriptEngineFactory].
     *
     *
     * {@inheritDoc}
     */
    @Override
    fun getEngineByName(shortName: String): GremlinScriptEngine {
        val engine: GremlinScriptEngine = cache.computeIfAbsent(shortName, super::getEngineByName)
        registerLookUpInfo(engine, shortName)
        return engine
    }

    /**
     * Gets a [GremlinScriptEngine] from cache or creates a new one from the [GremlinScriptEngineFactory].
     *
     *
     * {@inheritDoc}
     */
    @Override
    fun getEngineByExtension(extension: String?): GremlinScriptEngine {
        if (!extensionToName.containsKey(extension)) {
            val engine: GremlinScriptEngine = super.getEngineByExtension(extension)
            registerLookUpInfo(engine, engine.getFactory().getEngineName())
            return engine
        }
        return cache.get(extensionToName.get(extension))
    }

    /**
     * Gets a [GremlinScriptEngine] from cache or creates a new one from the [GremlinScriptEngineFactory].
     *
     *
     * {@inheritDoc}
     */
    @Override
    fun getEngineByMimeType(mimeType: String?): GremlinScriptEngine {
        if (!mimeToName.containsKey(mimeType)) {
            val engine: GremlinScriptEngine = super.getEngineByMimeType(mimeType)
            registerLookUpInfo(engine, engine.getFactory().getEngineName())
            return engine
        }
        return cache.get(mimeToName.get(mimeType))
    }

    private fun registerLookUpInfo(engine: GremlinScriptEngine?, shortName: String) {
        if (null == engine) throw IllegalArgumentException(
            String.format(
                "%s is not an available GremlinScriptEngine",
                shortName
            )
        )
        cache.putIfAbsent(shortName, engine)
        engine.getFactory().getExtensions().forEach { ext -> extensionToName.putIfAbsent(ext, shortName) }
        engine.getFactory().getMimeTypes().forEach { mime -> mimeToName.putIfAbsent(mime, shortName) }
    }
}