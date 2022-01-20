/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.tinkerpop4.gremlin.process.traversal

import java.util.HashMap
import java.util.Map

/**
 * Bindings are used to associate a variable with a value. They enable the creation of [Bytecode.Binding]
 * arguments in [Bytecode]. Use the Bindings instance when defining a binding via [Bindings.of].
 * For instance:
 *
 *
 * `
 * b = Bindings.instance()
 * g = graph.traversal()
 * g.V().out(b.of("a","knows"))
 * // bindings can be reused over and over
 * g.V().out("knows").in(b.of("a","created"))
` *
 *
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class Bindings private constructor() {
    fun <V> of(variable: String?, value: V): V {
        var map: Map<Object?, String?>? = MAP.get()
        if (null == map) {
            map = HashMap()
            MAP.set(map)
        }
        map.put(value, variable)
        return value
    }

    @Override
    override fun toString(): String {
        return "bindings[" + Thread.currentThread().getName().toString() + "]"
    }

    companion object {
        private val INSTANCE = Bindings()
        private val MAP: ThreadLocal<Map<Object, String>> = ThreadLocal()
        protected fun <V> getBoundVariable(value: V): String? {
            val map: Map<Object, String> = MAP.get()

            // once retrieved by bytecode, the binding should be removed. all this should occur during the addStep()
            // and related bytecode construction. using remove() solves the problem of TINKERPOP-2458
            return if (null == map) null else map.remove(value)
        }

        protected fun clear() {
            val map: Map<Object, String> = MAP.get()
            if (null != map) map.clear()
        }

        fun instance(): Bindings {
            return INSTANCE
        }
    }
}