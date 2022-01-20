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
package org.apache.tinkerpop4.gremlin.process.traversal.util

import org.apache.tinkerpop4.gremlin.jsr223.SingleGremlinScriptEngineManager

/**
 * ScriptTraversal encapsulates a [ScriptEngine] and a script which is compiled into a [Traversal] at [Admin.applyStrategies].
 * This is useful for serializing traversals as the compilation can happen on the remote end where the traversal will ultimately be processed.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class ScriptTraversal<S, E>(
    traversalSource: TraversalSource,
    scriptEngine: String,
    script: String,
    vararg bindings: Object
) : DefaultTraversal<S, E>() {
    private val alias = "g"
    private val factory: TraversalSourceFactory
    private val script: String
    private val scriptEngine: String
    private val bindings: Array<Object>

    init {
        this.graph = traversalSource.getGraph()
        factory = TraversalSourceFactory(traversalSource)
        this.scriptEngine = scriptEngine
        this.script = script
        this.bindings = bindings
        if (this.bindings.size % 2 != 0) throw IllegalArgumentException("The provided key/value bindings array length must be a multiple of two")
    }

    @Override
    @Throws(IllegalStateException::class)
    fun applyStrategies() {
        try {
            assert(0 == this.getSteps().size())
            val engine: ScriptEngine = SingleGremlinScriptEngineManager.get(scriptEngine)
            val engineBindings: Bindings = engine.createBindings()
            val strategyList: List<TraversalStrategy<*>> = this.getStrategies().toList()
            engineBindings.put(
                alias,
                factory.createTraversalSource(this.graph)
                    .withStrategies(strategyList.toArray(arrayOfNulls<TraversalStrategy>(strategyList.size())))
            )
            engineBindings.put(
                "graph",
                this.graph
            ) // TODO: we don't need this as the traversalSource.getGraph() exists, but its now here and people might be using it (remove in 3.3.0)
            var i = 0
            while (i < bindings.size) {
                engineBindings.put(bindings[i] as String, bindings[i + 1])
                i = i + 2
            }
            val traversal: Traversal.Admin<S, E> = engine.eval(script, engineBindings) as Traversal.Admin<S, E>
            traversal.getSideEffects().mergeInto(this.sideEffects)
            traversal.getSteps().forEach(this::addStep)
            this.strategies = traversal.getStrategies()
            super.applyStrategies()
        } catch (e: ScriptException) {
            throw IllegalStateException(e.getMessage(), e)
        }
    }
}