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
package org.apache.tinkerpop4.gremlin.structure.util.keyed

import org.apache.tinkerpop4.gremlin.structure.Graph

/**
 * A utility implementation of a [Property] that only has a key but no value and no meta-properties.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class KeyedVertexProperty<V>(key: String?) : VertexProperty<V> {
    private val key: String

    init {
        if (null == key || key.isEmpty()) throw IllegalArgumentException("key cannot be null")
        this.key = key
    }

    @Override
    fun element(): Vertex {
        throw Property.Exceptions.propertyDoesNotExist()
    }

    @Override
    fun id(): Object {
        throw Property.Exceptions.propertyDoesNotExist()
    }

    @Override
    fun graph(): Graph {
        throw Property.Exceptions.propertyDoesNotExist()
    }

    @Override
    fun <U> property(key: String?): Property<U> {
        return Property.< U > empty < U ? > ()
    }

    @Override
    fun <U> property(key: String?, value: U): Property<U> {
        return Property.< U > empty < U ? > ()
    }

    @Override
    fun key(): String {
        return key
    }

    @Override
    @Throws(NoSuchElementException::class)
    fun value(): V {
        throw Property.Exceptions.propertyDoesNotExist()
    }

    @get:Override
    val isPresent: Boolean
        get() = false

    @Override
    fun remove() {
    }

    @Override
    override fun toString(): String {
        return StringFactory.propertyString(this)
    }

    @Override
    fun <U> properties(vararg propertyKeys: String?): Iterator<Property<U>> {
        return Collections.emptyIterator()
    }

    @Override
    override fun equals(o: Object?): Boolean {
        if (this == o) return true
        if (o == null || getClass() !== o.getClass()) return false
        val that = o as KeyedVertexProperty<*>
        return key.equals(that.key)
    }

    @Override
    override fun hashCode(): Int {
        return key.hashCode()
    }
}