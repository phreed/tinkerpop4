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
package org.apache.tinkerpop4.gremlin.structure

import org.apache.tinkerpop4.gremlin.util.iterator.IteratorUtils

/**
 * An [Element] is the base class for both [Vertex] and [Edge]. An [Element] has an identifier
 * that must be unique to its inheriting classes ([Vertex] or [Edge]). An [Element] can maintain a
 * collection of [Property] objects.  Typically, objects are Java primitives (e.g. String, long, int, boolean,
 * etc.)
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
interface Element {
    /**
     * Gets the unique identifier for the graph `Element`.
     *
     * @return The id of the element
     */
    fun id(): Object?

    /**
     * Gets the label for the graph `Element` which helps categorize it.
     *
     * @return The label of the element
     */
    fun label(): String?

    /**
     * Get the graph that this element is within.
     *
     * @return the graph of this element
     */
    fun graph(): Graph?

    /**
     * Get the keys of the properties associated with this element.
     * The default implementation iterators the properties and stores the keys into a [HashSet].
     *
     * @return The property key set
     */
    fun keys(): Set<String?>? {
        val keys: Set<String> = HashSet()
        properties<Any>().forEachRemaining { property -> keys.add(property.key()) }
        return Collections.unmodifiableSet(keys)
    }

    /**
     * Get a [Property] for the `Element` given its key.
     * The default implementation calls the raw [Element.properties].
     */
    fun <V> property(key: String?): Property<V>? {
        val iterator: Iterator<Property<V>?> = properties<Any>(key)!!
        return if (iterator.hasNext()) iterator.next() else Property.< V > empty < V ? > ()
    }

    /**
     * Add or set a property value for the `Element` given its key.
     */
    fun <V> property(key: String?, value: V): Property<V>?

    /**
     * Get the value of a [Property] given it's key.
     * The default implementation calls [Element.property] and then returns the associated value.
     *
     * @throws NoSuchElementException if the property does not exist on the `Element`.
     */
    @Throws(NoSuchElementException::class)
    fun <V> value(key: String?): V {
        return this.property<V>(key).orElseThrow { Property.Exceptions.propertyDoesNotExist(this, key) }
    }

    /**
     * Removes the `Element` from the graph.
     */
    fun remove()

    /**
     * Get the values of properties as an [Iterator].
     */
    fun <V> values(vararg propertyKeys: String?): Iterator<V>? {
        return IteratorUtils.map(properties<V>(*propertyKeys)) { property -> property.value() }
    }

    /**
     * Get an [Iterator] of properties where the `propertyKeys` is meant to be a filter on the available
     * keys. If no keys are provide then return all the properties.
     */
    fun <V> properties(vararg propertyKeys: String?): Iterator<Property<V?>?>?

    /**
     * Common exceptions to use with an element.
     */
    object Exceptions {
        fun providedKeyValuesMustBeAMultipleOfTwo(): IllegalArgumentException {
            return IllegalArgumentException("The provided key/value array length must be a multiple of two")
        }

        fun providedKeyValuesMustHaveALegalKeyOnEvenIndices(): IllegalArgumentException {
            return IllegalArgumentException("The provided key/value array must have a String or T on even array indices")
        }

        fun propertyAdditionNotSupported(): IllegalStateException {
            return IllegalStateException("Property addition is not supported")
        }

        fun labelCanNotBeNull(): IllegalArgumentException {
            return IllegalArgumentException("Label can not be null")
        }

        fun labelCanNotBeEmpty(): IllegalArgumentException {
            return IllegalArgumentException("Label can not be empty")
        }

        fun labelCanNotBeAHiddenKey(label: String): IllegalArgumentException {
            return IllegalArgumentException("Label can not be a hidden key: $label")
        }
    }
}