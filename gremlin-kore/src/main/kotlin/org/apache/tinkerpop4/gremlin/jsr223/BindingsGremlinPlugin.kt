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
 * A plugin that allows `Bindings` to be applied to a [GremlinScriptEngine] at the time of creation.
 * `Bindings` defined with this plugin will always be assigned as `ScriptContext.GLOBAL_SCOPE` and as such
 * will be visible to all [GremlinScriptEngine] instances.
 *
 *
 * Note that bindings are applied in the order in which the `BindingsGremlinPlugin` instances are added to the
 * [GremlinScriptEngineManager]. Therefore if there are two plugins added and both include a variable called "x"
 * then the value of "x" will be the equal to the value provided by the second plugin that overrides the first.
 *
 *
 * This [GremlinPlugin] is not enabled for the `ServiceLoader`. It is designed to be instantiated manually.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class BindingsGremlinPlugin : AbstractGremlinPlugin {
    private constructor(builder: Builder) : super(NAME, DefaultBindingsCustomizer(builder.bindings)) {}
    internal constructor(bindingsSupplier: Supplier<Bindings?>?) : super(
        NAME,
        LazyBindingsCustomizer(bindingsSupplier)
    ) {
    }

    class Builder private constructor() {
        private var bindings: Bindings = SimpleBindings()
        fun bindings(bindings: Map<String?, Object?>?): Builder {
            this.bindings = SimpleBindings(bindings)
            return this
        }

        fun create(): BindingsGremlinPlugin {
            return BindingsGremlinPlugin(this)
        }
    }

    companion object {
        private const val NAME = "tinkerpop.bindings"

        /**
         * Builds a set of static bindings.
         */
        fun build(): Builder {
            return Builder()
        }
    }
}