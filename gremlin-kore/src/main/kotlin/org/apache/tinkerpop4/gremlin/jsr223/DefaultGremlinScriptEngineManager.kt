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

import org.slf4j.Logger

/**
 * The `ScriptEngineManager` implements a discovery, instantiation and configuration mechanism for
 * [GremlinScriptEngine] classes and also maintains a collection of key/value pairs storing state shared by all
 * engines created by it. This class uses the `ServiceProvider` mechanism to enumerate all the
 * implementations of `GremlinScriptEngineFactory`. The `ScriptEngineManager` provides a method
 * to return a list of all these factories as well as utility methods which look up factories on the basis of language
 * name, file extension and mime type.
 *
 *
 * The `Bindings` of key/value pairs, referred to as the "Global Scope" maintained by the manager is available
 * to all instances of @code ScriptEngine} created by the `GremlinScriptEngineManager`. The values
 * in the `Bindings` are generally exposed in all scripts.
 *
 *
 * This class is based quite heavily on the workings of the `ScriptEngineManager` supplied in the
 * `javax.script` packages, but adds some additional features that are specific to Gremlin and TinkerPop.
 * Unfortunately, it's not easily possible to extend `ScriptEngineManager` directly as there certain behaviors
 * don't appear to be be straightforward to implement and member variables are all private. It is important to note
 * that this class is designed to provide support for "Gremlin-enabled" `ScriptEngine` instances (i.e. those
 * that extend from [GremlinScriptEngine]) and is not meant to manage just any `ScriptEngine` instance
 * that may be on the path.
 *
 *
 * As this is a "Gremlin" `ScriptEngine`, certain common imports are automatically applied when a
 * [GremlinScriptEngine] is instantiated via the [GremlinScriptEngineFactory].. Initial imports from
 * gremlin-core come from the [CoreImports].
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class DefaultGremlinScriptEngineManager : GremlinScriptEngineManager {
    /**
     * Set of script engine factories discovered.
     */
    private val engineSpis: HashSet<GremlinScriptEngineFactory> = HashSet()

    /**
     * Map of engine name to script engine factory.
     */
    private val nameAssociations: HashMap<String, GremlinScriptEngineFactory> = HashMap()

    /**
     * Map of script file extension to script engine factory.
     */
    private val extensionAssociations: HashMap<String, GremlinScriptEngineFactory> = HashMap()

    /**
     * Map of script script MIME type to script engine factory.
     */
    private val mimeTypeAssociations: HashMap<String, GremlinScriptEngineFactory> = HashMap()

    /**
     * Global bindings associated with script engines created by this manager.
     */
    private var globalScope: Bindings = ConcurrentBindings()

    /**
     * List of extensions for the [GremlinScriptEngineManager] which will be used to supply
     * [Customizer] instances to [GremlinScriptEngineFactory] that are instantiated.
     */
    private val plugins: List<GremlinPlugin> = ArrayList()

    /**
     * The effect of calling this constructor is the same as calling
     * `DefaultGremlinScriptEngineManager(Thread.currentThread().getContextClassLoader())`.
     */
    constructor() {
        val ctxtLoader: ClassLoader = Thread.currentThread().getContextClassLoader()
        initEngines(ctxtLoader)
    }

    /**
     * This constructor loads the implementations of [GremlinScriptEngineFactory] visible to the given
     * `ClassLoader` using the `ServiceLoader` mechanism. If loader is `null`, the script
     * engine factories that are bundled with the platform and that are in the usual extension directories
     * (installed extensions) are loaded.
     */
    constructor(loader: ClassLoader) {
        initEngines(loader)
    }

    @Override
    fun getCustomizers(scriptEngineName: String?): List<Customizer> {
        return plugins.stream().flatMap { plugin ->
            val customizers: Optional<Array<Customizer>> = plugin.getCustomizers(scriptEngineName)
            Stream.of(customizers.orElse(arrayOfNulls<Customizer>(0)))
        }.collect(Collectors.toList())
    }

    @Override
    fun addPlugin(plugin: GremlinPlugin?) {
        // TODO: should modules be a set based on "name" to ensure uniqueness? not sure what bad stuff can happen with dupes
        if (plugin != null) plugins.add(plugin)
    }
    /**
     * Gets the bindings of the `Bindings` in global scope.
     */
    /**
     * Stores the specified `Bindings` as a global for all [GremlinScriptEngine] objects created by it.
     * If the bindings are to be updated by multiple threads it is recommended that a [ConcurrentBindings]
     * instance is supplied.
     *
     * @throws IllegalArgumentException if bindings is null.
     */
    @get:Override
    @set:kotlin.jvm.Synchronized
    @set:Override
    var bindings: Bindings
        get() = globalScope
        set(bindings) {
            if (null == bindings) throw IllegalArgumentException("Global scope cannot be null.")
            globalScope = bindings
        }

    /**
     * Sets the specified key/value pair in the global scope. The key may not be null or empty.
     *
     * @throws IllegalArgumentException if key is null or empty.
     */
    @Override
    fun put(key: String?, value: Object?) {
        if (null == key) throw IllegalArgumentException("key may not be null")
        if (key.isEmpty()) throw IllegalArgumentException("key may not be empty")
        globalScope.put(key, value)
    }

    /**
     * Gets the value for the specified key in the global scope.
     */
    @Override
    operator fun get(key: String?): Object {
        return globalScope.get(key)
    }

    /**
     * Looks up and creates a [GremlinScriptEngine] for a given name. The algorithm first searches for a
     * [GremlinScriptEngineFactory] that has been registered as a handler for the specified name using the
     * [.registerEngineExtension] method. If one is not found, it searches
     * the set of `GremlinScriptEngineFactory` instances stored by the constructor for one with the specified
     * name.  If a `ScriptEngineFactory` is found by either method, it is used to create instance of
     * [GremlinScriptEngine].
     *
     * @param shortName The short name of the [GremlinScriptEngine] implementation returned by the
     * [GremlinScriptEngineFactory.getNames] method.
     * @return A [GremlinScriptEngine] created by the factory located in the search.  Returns `null`
     * if no such factory was found.  The global scope of this manager is applied to the newly created
     * [GremlinScriptEngine]
     * @throws NullPointerException if shortName is `null`.
     */
    @Override
    fun getEngineByName(shortName: String?): GremlinScriptEngine? {
        if (null == shortName) throw NullPointerException()
        //look for registered name first
        var obj: Object
        if (null != nameAssociations.get(shortName).also { obj = it }) {
            val spi: GremlinScriptEngineFactory = obj as GremlinScriptEngineFactory
            try {
                return createGremlinScriptEngine(spi)
            } catch (exp: Exception) {
                logger.error(String.format("Could not create GremlinScriptEngine for %s", shortName), exp)
            }
        }
        for (spi in engineSpis) {
            var names: List<String?>? = null
            try {
                names = spi.getNames()
            } catch (exp: Exception) {
                logger.error("Could not get GremlinScriptEngine names", exp)
            }
            if (names != null) {
                for (name in names) {
                    if (shortName.equals(name)) {
                        try {
                            return createGremlinScriptEngine(spi)
                        } catch (exp: Exception) {
                            logger.error(String.format("Could not create GremlinScriptEngine for %s", shortName), exp)
                        }
                    }
                }
            }
        }
        return null
    }

    /**
     * Look up and create a [GremlinScriptEngine] for a given extension.  The algorithm
     * used by [.getEngineByName] is used except that the search starts by looking for a
     * [GremlinScriptEngineFactory] registered to handle the given extension using
     * [.registerEngineExtension].
     *
     * @return The engine to handle scripts with this extension.  Returns `null` if not found.
     * @throws NullPointerException if extension is `null`.
     */
    @Override
    fun getEngineByExtension(extension: String?): GremlinScriptEngine? {
        if (null == extension) throw NullPointerException()
        //look for registered extension first
        var obj: Object
        if (null != extensionAssociations.get(extension).also { obj = it }) {
            val spi: GremlinScriptEngineFactory = obj as GremlinScriptEngineFactory
            try {
                return createGremlinScriptEngine(spi)
            } catch (exp: Exception) {
                logger.error(String.format("Could not create GremlinScriptEngine for %s", extension), exp)
            }
        }
        for (spi in engineSpis) {
            var exts: List<String?>? = null
            try {
                exts = spi.getExtensions()
            } catch (exp: Exception) {
                logger.error("Could not get GremlinScriptEngine extensions", exp)
            }
            if (exts == null) continue
            for (ext in exts) {
                if (extension.equals(ext)) {
                    try {
                        return createGremlinScriptEngine(spi)
                    } catch (exp: Exception) {
                        logger.error(String.format("Could not create GremlinScriptEngine for %s", extension), exp)
                    }
                }
            }
        }
        return null
    }

    /**
     * Look up and create a [GremlinScriptEngine] for a given mime type.  The algorithm used by
     * [.getEngineByName] is used except that the search starts by looking for a
     * [GremlinScriptEngineFactory] registered to handle the given mime type using
     * [.registerEngineMimeType].
     *
     * @param mimeType The given mime type
     * @return The engine to handle scripts with this mime type.  Returns `null` if not found.
     * @throws NullPointerException if mime-type is `null`.
     */
    @Override
    fun getEngineByMimeType(mimeType: String?): GremlinScriptEngine? {
        if (null == mimeType) throw NullPointerException()
        //look for registered types first
        var obj: Object
        if (null != mimeTypeAssociations.get(mimeType).also { obj = it }) {
            val spi: GremlinScriptEngineFactory = obj as GremlinScriptEngineFactory
            try {
                return createGremlinScriptEngine(spi)
            } catch (exp: Exception) {
                logger.error(String.format("Could not create GremlinScriptEngine for %s", mimeType), exp)
            }
        }
        for (spi in engineSpis) {
            var types: List<String?>? = null
            try {
                types = spi.getMimeTypes()
            } catch (exp: Exception) {
                logger.error("Could not get GremlinScriptEngine mimetypes", exp)
            }
            if (types == null) continue
            for (type in types) {
                if (mimeType.equals(type)) {
                    try {
                        return createGremlinScriptEngine(spi)
                    } catch (exp: Exception) {
                        logger.error(String.format("Could not create GremlinScriptEngine for %s", mimeType), exp)
                    }
                }
            }
        }
        return null
    }

    /**
     * Returns a list whose elements are instances of all the [GremlinScriptEngineFactory] classes
     * found by the discovery mechanism.
     *
     * @return List of all discovered [GremlinScriptEngineFactory] objects.
     */
    @get:Override
    val engineFactories: List<Any>
        get() {
            val res: List<GremlinScriptEngineFactory> = ArrayList(engineSpis.size())
            res.addAll(engineSpis.stream().collect(Collectors.toList()))
            return Collections.unmodifiableList(res)
        }

    /**
     * Registers a [GremlinScriptEngineFactory] to handle a language name.  Overrides any such association found
     * using the discovery mechanism.
     *
     * @param name The name to be associated with the [GremlinScriptEngineFactory]
     * @param factory The class to associate with the given name.
     * @throws NullPointerException if any of the parameters is null.
     */
    @Override
    fun registerEngineName(name: String?, factory: GremlinScriptEngineFactory?) {
        if (null == name || null == factory) throw NullPointerException()
        nameAssociations.put(name, factory)
    }

    /**
     * Registers a [GremlinScriptEngineFactory] to handle a mime type. Overrides any such association found using
     * the discovery mechanism.
     *
     * @param type The mime type  to be associated with the [GremlinScriptEngineFactory].
     * @param factory The class to associate with the given mime type.
     * @throws NullPointerException if any of the parameters is null.
     */
    @Override
    fun registerEngineMimeType(type: String?, factory: GremlinScriptEngineFactory?) {
        if (null == type || null == factory) throw NullPointerException()
        mimeTypeAssociations.put(type, factory)
    }

    /**
     * Registers a [GremlinScriptEngineFactory] to handle an extension. Overrides any such association found
     * using the discovery mechanism.
     *
     * @param extension The extension type to be associated with the [GremlinScriptEngineFactory]
     * @param factory The class to associate with the given extension.
     * @throws NullPointerException if any of the parameters is null.
     */
    @Override
    fun registerEngineExtension(extension: String?, factory: GremlinScriptEngineFactory?) {
        if (null == extension || null == factory) throw NullPointerException()
        extensionAssociations.put(extension, factory)
    }

    private fun getServiceLoader(loader: ClassLoader?): ServiceLoader<GremlinScriptEngineFactory> {
        return if (loader != null) {
            ServiceLoader.load(GremlinScriptEngineFactory::class.java, loader)
        } else {
            ServiceLoader.loadInstalled(GremlinScriptEngineFactory::class.java)
        }
    }

    private fun initEngines(loader: ClassLoader) {
        val itty: Iterator<GremlinScriptEngineFactory>
        itty = try {
            val sl: ServiceLoader<GremlinScriptEngineFactory> = AccessController.doPrivileged(
                PrivilegedAction<ServiceLoader<GremlinScriptEngineFactory>> { getServiceLoader(loader) } as PrivilegedAction<ServiceLoader<GremlinScriptEngineFactory?>?>?)
            sl.iterator()
        } catch (err: ServiceConfigurationError) {
            logger.error("Can't find GremlinScriptEngineFactory providers: " + err.getMessage(), err)

            // do not throw any exception here. user may want to manager their own factories using this manager
            // by explicit registration (by registerXXX) methods.
            return
        }
        try {
            while (itty.hasNext()) {
                try {
                    val factory: GremlinScriptEngineFactory = itty.next()
                    factory.setCustomizerManager(this)
                    engineSpis.add(factory)
                } catch (err: ServiceConfigurationError) {
                    logger.error("GremlinScriptEngineManager providers.next(): " + err.getMessage(), err)
                }
            }
        } catch (err: ServiceConfigurationError) {
            logger.error("GremlinScriptEngineManager providers.hasNext(): " + err.getMessage(), err)
            // do not throw any exception here. user may want to manage their own factories using this manager
            // by explicit registration (by registerXXX) methods.
        }
    }

    private fun createGremlinScriptEngine(spi: GremlinScriptEngineFactory): GremlinScriptEngine {
        val engine: GremlinScriptEngine = spi.getScriptEngine()

        // merge in bindings that are marked with global scope. these get applied to all GremlinScriptEngine instances
        getCustomizers(spi.getEngineName()).stream()
            .filter { p -> p is BindingsCustomizer }
            .map { p -> p as BindingsCustomizer }
            .filter { bc -> bc.getScope() === ScriptContext.GLOBAL_SCOPE }
            .flatMap { bc -> bc.getBindings().entrySet().stream() }
            .forEach { kv ->
                if (globalScope.containsKey(kv.getKey())) {
                    logger.warn(
                        "Overriding the global binding [{}] - was [{}] and is now [{}]",
                        kv.getKey(), globalScope.get(kv.getKey()), kv.getValue()
                    )
                }
                globalScope.put(kv.getKey(), kv.getValue())
            }
        engine.setBindings(bindings, ScriptContext.GLOBAL_SCOPE)

        // merge in bindings that are marked with engine scope. there typically won't be any of these but it's just
        // here for completeness. bindings will typically apply with global scope only as engine scope will generally
        // be overridden at the time of eval() with the bindings that are supplied to it
        getCustomizers(spi.getEngineName()).stream()
            .filter { p -> p is BindingsCustomizer }
            .map { p -> p as BindingsCustomizer }
            .filter { bc -> bc.getScope() === ScriptContext.ENGINE_SCOPE }
            .forEach { bc -> engine.getBindings(ScriptContext.ENGINE_SCOPE).putAll(bc.getBindings()) }
        val scriptCustomizers: List<ScriptCustomizer> = getCustomizers(spi.getEngineName()).stream()
            .filter { p -> p is ScriptCustomizer }
            .map { p -> p as ScriptCustomizer }
            .collect(Collectors.toList())

        // since the bindings aren't added until after the ScriptEngine is constructed, running init scripts that
        // require bindings creates a problem. as a result, init scripts are applied here
        scriptCustomizers.stream().flatMap { sc -> sc.getScripts().stream() }
            .map { l -> String.join(System.lineSeparator(), l) }.forEach { initScript ->
                try {
                    // need to apply global bindings here as part of the engine or else they don't get their binding types
                    // registered by certain GremlinScriptEngine instances (pretty much talking about gremlin-groovy here)
                    // where type checking is made important. this may not be a good generic way to handled this in the
                    // long run, but for now we only have two GremlinScriptEngines to be concerned about so thus far it
                    // presents no real pains. global bindings are applied automatically to the context via
                    // AbstractScriptEngine.getScriptContext() - passing them again here to eval() will just make the
                    // global bindings behave as engine bindings and then you get weird things happening (like local vars
                    // becoming global).
                    val initializedBindings: Object = engine.eval(initScript)
                    if (initializedBindings != null && initializedBindings is Map) (initializedBindings as Map<String?, Object?>).forEach { k, v ->
                        put(
                            k,
                            v
                        )
                    }
                } catch (ex: Exception) {
                    throw IllegalStateException(ex)
                }
            }
        return engine
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(DefaultGremlinScriptEngineManager::class.java)
    }
}