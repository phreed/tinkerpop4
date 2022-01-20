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
package org.apache.tinkerpop4.gremlin.process.traversal.step

import org.apache.tinkerpop4.gremlin.process.traversal.Path

/**
 * This interface is implemented by [Step] implementations that access labeled path steps, side-effects or
 * `Map` values by key, such as `select('a')` step. Note that a step like `project()` is non-scoping
 * because while it creates a `Map` it does not introspect them.
 *
 *
 * There are four types of scopes:
 *
 *  1. Current scope — the current data referenced by the traverser (“path head”).
 *  1. Path scope — a particular piece of data in the path of the traverser (“path history”).
 *  1. Side-effect scope — a particular piece of data in the global traversal blackboard.
 *  1. Map scope — a particular piece of data in the current scope map (“map value by key”).
 *
 *
 * The current scope refers to the current object referenced by the traverser. That is, the `traverser.get()`
 * object. Another way to think about the current scope is to think in terms of the path of the traverser where the
 * current scope is the head of the path. With the math()-step, the variable `_` refers to the current scope.
 *
 * <pre>
 * `gremlin> g.V().values("age").math("sin _")
 * ==>-0.6636338842129675
 * ==>0.956375928404503
 * ==>0.5514266812416906
 * ==>-0.428182669496151
` *
</pre> *
 *
 * The path scope refers to data previously seen by the traverser. That is, data in the traverser’s path history.
 * Paths can be accessed by `path()`, however, individual parts of the path can be labeled using `as()`
 * and accessed later via the path label name. Thus, in the traversal below, “a” and “b” refer to objects previously
 * traversed by the traverser.
 *
 * <pre>
 * `gremlin> g.V().as("a").out("knows").as("b”).
 * math("a / b").by("age")
 * ==>1.0740740740740742
 * ==>0.90625
` *
</pre> *
 *
 * The side-effect scope refers objects in the global side-effects of the traversal. Side-effects are not local to the
 * traverser, but instead, global to the traversal. In the traversal below you can see how “x” is being referenced in
 * the math()-step and thus, the side-effect data is being used.
 *
 * <pre>
 * `gremlin> g.withSideEffect("x",100).V().values("age").math("_ / x")
 * ==>0.29
 * ==>0.27
 * ==>0.32
 * ==>0.35
` *
</pre> *
 *
 * Map scope refers to objects within the current map object. Thus, its like current scope, but a bit “deeper.” In the
 * traversal below the `project()`-step generates a map with keys “a” and “b”. The subsequent `math()`-step
 * is then able to access the “a” and “b” values in the respective map and use them for the division operation.
 *
 * <pre>
 * `gremlin>
 * g.V().hasLabel("person”).
 * project("a","b”).
 * by("age”).
 * by(bothE().count()).
 * math("a / b")
 * ==>9.666666666666666
 * ==>27.0
 * ==>10.666666666666666
 * ==>35.0
` *
</pre> *
 *
 * Scoping is all about variable data access and forms the fundamental interface for access to the memory structures
 * of Gremlin.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
interface Scoping {
    enum class Variable {
        START, END
    }

    /**
     * Finds the object with the specified key for the current traverser and throws an exception if the key cannot
     * be found.
     *
     * @throws KeyNotFoundException if the key does not exist
     */
    @Throws(KeyNotFoundException::class)
    fun <S> getScopeValue(pop: Pop?, key: Object, traverser: Traverser.Admin<*>): S {
        val `object`: Object = traverser.get()
        if (`object` is Map && (`object` as Map).containsKey(key)) return `object`[key] as S
        if (key is String) {
            val k = key as String
            if (traverser.getSideEffects().exists(k)) return traverser.getSideEffects().get(k)
            val path: Path = traverser.path()
            if (path.hasLabel(k)) return if (null == pop) path.get(k) else path.get(pop, k)
        }
        throw KeyNotFoundException(key, this)
    }

    /**
     * Calls [.getScopeValue] but throws an unchecked `IllegalStateException`
     * if the key cannot be found.
     */
    fun <S> getSafeScopeValue(pop: Pop?, key: Object, traverser: Traverser.Admin<*>): S {
        return try {
            getScopeValue(pop, key, traverser)
        } catch (nfe: KeyNotFoundException) {
            throw IllegalArgumentException(nfe.getMessage(), nfe)
        }
    }

    /**
     * Calls [.getScopeValue] and returns `null` if the key is not found.
     * Use this method with caution as `null` has one of two meanings as a return value. It could be that the
     * key was found and its value was `null` or it might mean that the key was not found and `null` was
     * simply returned.
     */
    fun <S> getNullableScopeValue(pop: Pop?, key: String, traverser: Traverser.Admin<*>): S? {
        return try {
            getScopeValue(pop, key, traverser)
        } catch (nfe: KeyNotFoundException) {
            null
        }
    }

    /**
     * Get the labels that this scoping step will access during the traversal
     *
     * @return the accessed labels of the scoping step
     */
    val scopeKeys: Set<String?>?

    class KeyNotFoundException(key: Object, current: Scoping) : Exception(
        "Neither the map, sideEffects, nor path has a $key-key: $current"
    ) {
        private val key: Object
        val step: Scoping

        init {
            this.key = key
            step = current
        }

        fun getKey(): Object {
            return key
        }
    }
}