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

import javax.script.ScriptEngineManager
import java.util.Collection
import java.util.List

/**
 * A [Customizer] that executes scripts in a [GremlinScriptEngine] instance for purpose of initialization.
 * Implementors of a [GremlinScriptEngine] do not need to be concerned with supporting this [Customizer].
 * This is work for the [ScriptEngineManager] implementation since scripts typically require access to global
 * bindings and those are not applied to the [GremlinScriptEngine] until after construction.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
interface ScriptCustomizer : Customizer {
    /**
     * Gets a collection of scripts where each is represented as a list of script lines.
     */
    val scripts: Collection<List<String?>?>?
}