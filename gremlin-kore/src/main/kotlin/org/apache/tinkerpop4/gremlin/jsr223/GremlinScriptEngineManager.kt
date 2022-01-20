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
import java.util.List

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
 * This interface is based quite heavily on the workings of the `ScriptEngineManager` supplied in the
 * `javax.script` packages, but adds some additional features that are specific to Gremlin and TinkerPop.
 * Unfortunately, it's not easily possible to extend `ScriptEngineManager` directly as there certain behaviors
 * don't appear to be be straightforward to implement and member variables are all private. It is important to note
 * that this interface is designed to provide support for "Gremlin-enabled" `ScriptEngine` instances (i.e. those
 * that extend from [GremlinScriptEngine]) and is not meant to manage just any `ScriptEngine` instance
 * that may be on the path.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
interface GremlinScriptEngineManager {
    /**
     * Stores the specified `Bindings` as a global for all [GremlinScriptEngine] objects created by it.
     *
     * @throws IllegalArgumentException if bindings is null.
     */
    fun setBindings(bindings: Bindings?)

    /**
     * Gets the bindings of the `Bindings` in global scope.
     */
    fun getBindings(): Bindings?

    /**
     * Sets the specified key/value pair in the global scope. The key may not be null or empty.
     *
     * @throws IllegalArgumentException if key is null or empty.
     */
    fun put(key: String?, value: Object?)

    /**
     * Gets the value for the specified key in the global scope.
     */
    operator fun get(key: String?): Object?

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
    fun getEngineByName(shortName: String?): GremlinScriptEngine?

    /**
     * Look up and create a [GremlinScriptEngine] for a given extension.  The algorithm
     * used by [.getEngineByName] is used except that the search starts by looking for a
     * [GremlinScriptEngineFactory] registered to handle the given extension using
     * [.registerEngineExtension].
     *
     * @return The engine to handle scripts with this extension.  Returns `null` if not found.
     * @throws NullPointerException if extension is `null`.
     */
    fun getEngineByExtension(extension: String?): GremlinScriptEngine?

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
    fun getEngineByMimeType(mimeType: String?): GremlinScriptEngine?

    /**
     * Returns a list whose elements are instances of all the [GremlinScriptEngineFactory] classes
     * found by the discovery mechanism.
     *
     * @return List of all discovered [GremlinScriptEngineFactory] objects.
     */
    val engineFactories: List<Any?>?

    /**
     * Add [GremlinPlugin] instances to customize newly created [GremlinScriptEngine] instances.
     */
    fun addPlugin(plugin: GremlinPlugin?)

    /**
     * Registers a [GremlinScriptEngineFactory] to handle a language name.  Overrides any such association found
     * using the discovery mechanism.
     *
     * @param name The name to be associated with the [GremlinScriptEngineFactory]
     * @param factory The class to associate with the given name.
     * @throws NullPointerException if any of the parameters is null.
     */
    fun registerEngineName(name: String?, factory: GremlinScriptEngineFactory?)

    /**
     * Registers a [GremlinScriptEngineFactory] to handle a mime type. Overrides any such association found using
     * the discovery mechanism.
     *
     * @param type The mime type  to be associated with the [GremlinScriptEngineFactory].
     * @param factory The class to associate with the given mime type.
     * @throws NullPointerException if any of the parameters is null.
     */
    fun registerEngineMimeType(type: String?, factory: GremlinScriptEngineFactory?)

    /**
     * Registers a [GremlinScriptEngineFactory] to handle an extension. Overrides any such association found
     * using the discovery mechanism.
     *
     * @param extension The extension type to be associated with the [GremlinScriptEngineFactory]
     * @param factory The class to associate with the given extension.
     * @throws NullPointerException if any of the parameters is null.
     */
    fun registerEngineExtension(extension: String?, factory: GremlinScriptEngineFactory?)

    /**
     * Get the list of [Customizer] instances filtered by the `scriptEngineName`.
     */
    fun getCustomizers(scriptEngineName: String?): List<Customizer?>?
}