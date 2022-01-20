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
package org.apache.tinkerpop4.gremlin.process.traversal.step.util

import org.apache.commons.lang3.ArrayUtils
import org.apache.tinkerpop4.gremlin.process.traversal.Traversal
import org.apache.tinkerpop4.gremlin.process.traversal.Traverser
import org.apache.tinkerpop4.gremlin.process.traversal.step.Scoping
import org.apache.tinkerpop4.gremlin.process.traversal.step.TraversalParent
import org.apache.tinkerpop4.gremlin.process.traversal.util.TraversalUtil
import org.apache.tinkerpop4.gremlin.structure.Element
import org.apache.tinkerpop4.gremlin.structure.T
import org.apache.tinkerpop4.gremlin.util.iterator.IteratorUtils
import java.io.Serializable
import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.HashMap
import java.util.HashSet
import java.util.List
import java.util.Map
import java.util.Objects
import java.util.Set
import java.util.function.Supplier

/**
 * The parameters held by a [Traversal].
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class Parameters : Cloneable, Serializable {
    protected var parameters: Map<Object, List<Object>> = HashMap()

    /**
     * Gets a list of all labels held in parameters that have a traversal as a value.
     */
    var referencedLabels: Set<String> = HashSet()
        protected set

    /**
     * A cached list of traversals that serve as parameter values. The list is cached on calls to
     * [.set] because when the parameter map is large the cost of iterating it repeatedly on the
     * high number of calls to [.getTraversals] is great.
     */
    protected var traversals: List<Traversal.Admin<*, *>> = ArrayList()

    /**
     * Checks for existence of key in parameter set.
     *
     * @param key the key to check
     * @return `true` if the key is present and `false` otherwise
     */
    operator fun contains(key: Object): Boolean {
        return parameters.containsKey(key)
    }

    /**
     * Checks for existence of a key and value in a parameter set.
     *
     * @param key the key to check
     * @param value the value to check
     * @return `true` if the key and value are present and `false` otherwise
     */
    fun contains(key: Object, value: Object): Boolean {
        return this.contains(key) && parameters[key]!!.contains(value)
    }

    /**
     * Renames a key in the parameter set.
     *
     * @param oldKey the key to rename
     * @param newKey the new name of the key
     */
    fun rename(oldKey: Object?, newKey: Object?) {
        parameters.put(newKey, parameters.remove(oldKey))
    }

    /**
     * Gets the list of values for a key, while resolving the values of any parameters that are [Traversal]
     * objects.
     */
    operator fun <S, E> get(traverser: Traverser.Admin<S>?, key: Object, defaultValue: Supplier<E>): List<E> {
        val values = parameters[key] as List<E>? ?: return Collections.singletonList(defaultValue.get())
        val result: List<E> = ArrayList()
        for (value in values) {
            result.add(
                if (value is Traversal.Admin) TraversalUtil.apply(
                    traverser,
                    value as Traversal.Admin<S, E>
                ) else value
            )
        }
        return result
    }

    /**
     * Gets the value of a key and if that key isn't present returns the default value from the [Supplier].
     *
     * @param key          the key to retrieve
     * @param defaultValue the default value generator which if null will return an empty list
     */
    operator fun <E> get(key: Object, defaultValue: Supplier<E>?): List<E> {
        val list = parameters[key] as List<E>?
        return list
            ?: if (null == defaultValue) Collections.emptyList() else Collections.singletonList(defaultValue.get())
    }

    /**
     * Remove a key from the parameter set.
     *
     * @param key the key to remove
     * @return the value of the removed key
     */
    fun remove(key: Object?): Object {
        val o: List<Object> = parameters.remove(key)

        // once a key is removed, it's possible that the traversal/label cache will need to be regenerated
        if (IteratorUtils.anyMatch(o.iterator()) { p -> p is Traversal.Admin }) {
            traversals.clear()
            traversals = ArrayList()
            for (list in parameters.values()) {
                for (`object` in list) {
                    if (`object` is Traversal.Admin) {
                        val t: Traversal.Admin = `object` as Traversal.Admin
                        addTraversal(t)
                    }
                }
            }
        }
        return o
    }

    /**
     * Gets the array of keys/values of the parameters while resolving parameter values that contain
     * [Traversal] instances.
     */
    fun <S> getKeyValues(traverser: Traverser.Admin<S>?, vararg exceptKeys: Object?): Array<Object?> {
        if (parameters.isEmpty()) return EMPTY_ARRAY
        val keyValues: List<Object> = ArrayList()
        for (entry in parameters.entrySet()) {
            if (!ArrayUtils.contains(exceptKeys, entry.getKey())) {
                for (value in entry.getValue()) {
                    keyValues.add(
                        if (entry.getKey() is Traversal.Admin) TraversalUtil.apply(
                            traverser,
                            entry.getKey() as Traversal.Admin<S, *>
                        ) else entry.getKey()
                    )
                    keyValues.add(
                        if (value is Traversal.Admin) TraversalUtil.apply(
                            traverser,
                            value as Traversal.Admin<S, *>
                        ) else value
                    )
                }
            }
        }
        return keyValues.toArray(arrayOfNulls<Object>(keyValues.size()))
    }

    /**
     * Gets an immutable set of the parameters without evaluating them in the context of a [Traverser] as
     * is done in [.getKeyValues].
     *
     * @param exceptKeys keys to not include in the returned [Map]
     */
    fun getRaw(vararg exceptKeys: Object?): Map<Object, List<Object>> {
        if (parameters.isEmpty()) return Collections.emptyMap()
        val exceptions: List<Object> = Arrays.asList(exceptKeys)
        val raw: Map<Object, List<Object>> = HashMap()
        for (entry in parameters.entrySet()) {
            if (!exceptions.contains(entry.getKey())) raw.put(entry.getKey(), entry.getValue())
        }
        return Collections.unmodifiableMap(raw)
    }

    /**
     * Set parameters given key/value pairs.
     */
    fun set(parent: TraversalParent?, vararg keyValues: Object) {
        if (keyValues.size % 2 != 0) throw Element.Exceptions.providedKeyValuesMustBeAMultipleOfTwo()
        var ix = 0
        while (ix < keyValues.size) {
            if (keyValues[ix] !is String && keyValues[ix] !is T && keyValues[ix] !is Traversal) throw IllegalArgumentException(
                "The provided key/value array must have a String, T, or Traversal on even array indices"
            )

            // check both key and value for traversal instances. track the list of traversals that are present so
            // that elsewhere in Parameters there is no need to iterate all values to not find any. also grab
            // available labels in traversal values
            for (iy in 0..1) {
                if (keyValues[ix + iy] is Traversal.Admin) {
                    val t: Traversal.Admin = keyValues[ix + iy] as Traversal.Admin
                    addTraversal(t)
                    if (parent != null) parent.integrateChild(t)
                }
            }
            var values: List<Object?>? = parameters[keyValues[ix]]
            if (null == values) {
                values = ArrayList()
                values.add(keyValues[ix + 1])
                parameters.put(keyValues[ix], values)
            } else {
                values.add(keyValues[ix + 1])
            }
            ix = ix + 2
        }
    }

    /**
     * Gets all the [Traversal.Admin] objects in the map of parameters.
     */
    fun <S, E> getTraversals(): List<Traversal.Admin<S, E>>? {
        // stupid generics - just need to return "traversals"
        return traversals as Object
    }

    fun clone(): Parameters {
        return try {
            val clone = super.clone() as Parameters
            clone.parameters = HashMap()
            clone.traversals = ArrayList()
            for (entry in parameters.entrySet()) {
                val values: List<Object> = ArrayList()
                for (value in entry.getValue()) {
                    if (value is Traversal.Admin) {
                        val traversalClone: Traversal.Admin<*, *> = (value as Traversal.Admin).clone()
                        clone.traversals.add(traversalClone)
                        values.add(traversalClone)
                    } else values.add(value)
                }
                if (entry.getKey() is Traversal.Admin) {
                    val traversalClone: Traversal.Admin<*, *> = (entry.getKey() as Traversal.Admin).clone()
                    clone.traversals.add(traversalClone)
                    clone.parameters.put(traversalClone, values)
                } else clone.parameters.put(entry.getKey(), values)
            }
            clone.referencedLabels = HashSet(referencedLabels)
            clone
        } catch (e: CloneNotSupportedException) {
            throw IllegalStateException(e.getMessage(), e)
        }
    }

    override fun hashCode(): Int {
        var result = 1
        for (entry in parameters.entrySet()) {
            result = result xor entry.getKey().hashCode()
            for (value in entry.getValue()) {
                result = result xor Integer.rotateLeft(Objects.hashCode(value), entry.getKey().hashCode())
            }
        }
        return result
    }

    override fun toString(): String {
        return parameters.toString()
    }

    private fun addTraversal(t: Traversal.Admin) {
        traversals.add(t)
        for (ss in t.getSteps()) {
            if (ss is Scoping) {
                for (label in (ss as Scoping).getScopeKeys()) {
                    referencedLabels.add(label)
                }
            }
        }
    }

    companion object {
        val EMPTY = Parameters()
        private val EMPTY_ARRAY: Array<Object?> = arrayOfNulls<Object>(0)
    }
}