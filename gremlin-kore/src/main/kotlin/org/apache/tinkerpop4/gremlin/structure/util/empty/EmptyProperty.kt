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
package org.apache.tinkerpop4.gremlin.structure.util.empty

import org.apache.tinkerpop4.gremlin.structure.Element

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class EmptyProperty<V> private constructor() : Property<V> {
    @Override
    fun key(): String {
        throw Exceptions.propertyDoesNotExist()
    }

    @Override
    @Throws(NoSuchElementException::class)
    fun value(): V {
        throw Exceptions.propertyDoesNotExist()
    }

    @get:Override
    val isPresent: Boolean
        get() = false

    @Override
    fun element(): Element {
        throw Exceptions.propertyDoesNotExist()
    }

    @Override
    fun remove() {
    }

    @Override
    override fun toString(): String {
        return StringFactory.propertyString(this)
    }

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
    override fun equals(`object`: Object?): Boolean {
        return `object` is EmptyProperty<*>
    }

    override fun hashCode(): Int {
        return 1281483122
    }

    companion object {
        private val INSTANCE: EmptyProperty<*> = EmptyProperty<Any?>()
        fun <V> instance(): Property<V> {
            return INSTANCE
        }
    }
}