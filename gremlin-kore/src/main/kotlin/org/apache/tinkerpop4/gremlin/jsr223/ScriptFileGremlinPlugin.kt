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

import java.io.File

/**
 * Loads scripts from one or more files into the [GremlinScriptEngine] at startup. This [GremlinPlugin] is
 * not enabled for the `ServiceLoader`. It is designed to be instantiated manually. Those implementing
 * [GremlinScriptEngine] instances need to be concerned with accounting for this [Customizer]. It is
 * handled automatically by the [DefaultGremlinScriptEngineManager].
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class ScriptFileGremlinPlugin private constructor(builder: Builder) :
    AbstractGremlinPlugin(NAME, builder.appliesTo, DefaultScriptCustomizer(builder.files)) {
    class Builder private constructor() {
        private val appliesTo: Set<String> = HashSet()
        private val files: List<File> = ArrayList()

        /**
         * The name of the [GremlinScriptEngine] that this module will apply to. Setting no values here will
         * make the module available to all the engines. Typically, this value should be set as a script's syntax will
         * be bound to the [GremlinScriptEngine] language.
         */
        fun appliesTo(scriptEngineNames: Collection<String?>?): Builder {
            appliesTo.addAll(scriptEngineNames)
            return this
        }

        fun files(files: List<String?>): Builder {
            for (f in files) {
                val file = File(f)
                if (!file.exists()) throw IllegalArgumentException(FileNotFoundException(f))
                this.files.add(file)
            }
            return this
        }

        fun create(): ScriptFileGremlinPlugin {
            return ScriptFileGremlinPlugin(this)
        }
    }

    companion object {
        private const val NAME = "tinkerpop.script"
        fun build(): Builder {
            return Builder()
        }
    }
}