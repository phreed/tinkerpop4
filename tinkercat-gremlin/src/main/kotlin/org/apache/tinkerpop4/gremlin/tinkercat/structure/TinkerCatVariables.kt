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
package org.apache.tinkerpop4.gremlin.tinkercat.structure

import org.apache.tinkerpop4.gremlin.structure.Graph
import java.util.concurrent.ConcurrentHashMap
import org.apache.tinkerpop4.gremlin.structure.util.GraphVariableHelper
import org.apache.tinkerpop4.gremlin.structure.util.StringFactory
import java.util.*

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class TinkerCatVariables : Graph.Variables {
    private val variables: MutableMap<String, Any> = ConcurrentHashMap()
    override fun keys(): Set<String> {
        return variables.keys
    }

    override fun <R> get(key: String): Optional<R> {
        return Optional.ofNullable(variables[key] as R?)
    }

    override fun remove(key: String) {
        variables.remove(key)
    }

    override fun set(key: String, value: Any) {
        GraphVariableHelper.validateVariable(key, value)
        variables[key] = value
    }

    override fun toString(): String {
        return StringFactory.graphVariablesString(this)
    }
}