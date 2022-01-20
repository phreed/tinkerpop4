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
 * This module is required for a `ScriptEngine` to be Gremlin-enabled. This [GremlinPlugin] is not enabled
 * for the `ServiceLoader`. It is designed to be instantiated manually and compliant [GremlinScriptEngine]
 * instances will automatically install it by default when created.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class CoreGremlinPlugin private constructor() : GremlinPlugin {
    @Override
    fun getCustomizers(scriptEngineName: String?): Optional<Array<Customizer>> {
        return Optional.of(customizers)
    }

    companion object {
        @get:Override
        val name = "tinkerpop.core"
            get() = Companion.field
        private val gremlinCore: ImportCustomizer = DefaultImportCustomizer.build()
            .addClassImports(CoreImports.getClassImports())
            .addFieldImports(CoreImports.getFieldImports())
            .addEnumImports(CoreImports.getEnumImports())
            .addMethodImports(CoreImports.getMethodImports()).create()
        private val customizers: Array<Customizer> = arrayOf<Customizer>(gremlinCore)
        private val INSTANCE = CoreGremlinPlugin()
        fun instance(): CoreGremlinPlugin {
            return INSTANCE
        }
    }
}