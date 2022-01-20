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

import java.util.Optional

/**
 * A plugin interface that is used by the [GremlinScriptEngineManager] to configure special [Customizer]
 * instances that will alter the features of any [GremlinScriptEngine] created by the manager itself.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
interface GremlinPlugin {
    /**
     * The name of the module.  This name should be unique (use a namespaced approach) as naming clashes will
     * prevent proper module operations. Modules developed by TinkerPop will be prefixed with "tinkerpop."
     * For example, TinkerPop's implementation of Spark would be named "tinkerpop.spark".  If Facebook were
     * to do their own implementation the implementation might be called "facebook.spark".
     */
    val name: String?

    /**
     * Some modules may require a restart of the plugin host for the classloader to pick up the features.  This is
     * typically true of modules that rely on `Class.forName()` to dynamically instantiate classes from the
     * root classloader (e.g. JDBC drivers that instantiate via @{code DriverManager}).
     */
    fun requireRestart(): Boolean {
        return false
    }

    /**
     * Gets the list of all [Customizer] implementations to assign to a new [GremlinScriptEngine]. This is
     * the same as doing `getCustomizers(null)`.
     */
    val customizers: Optional<Array<Customizer?>?>?
        get() = getCustomizers(null)

    /**
     * Gets the list of [Customizer] implementations to assign to a new [GremlinScriptEngine]. The
     * implementation should filter the returned `Customizers` according to the supplied name of the
     * Gremlin-enabled `ScriptEngine`. By providing a filter, `GremlinModule` developers can have the
     * ability to target specific `ScriptEngines`.
     *
     * @param scriptEngineName The name of the `ScriptEngine` or null to get all the available `Customizers`
     */
    fun getCustomizers(scriptEngineName: String?): Optional<Array<Customizer?>?>?
}