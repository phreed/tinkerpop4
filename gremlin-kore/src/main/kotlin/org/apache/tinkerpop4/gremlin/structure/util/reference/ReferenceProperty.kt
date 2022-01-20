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
package org.apache.tinkerpop4.gremlin.structure.util.reference

import org.apache.tinkerpop4.gremlin.structure.Element

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class ReferenceProperty<V> : Attachable<Property<V>?>, Serializable, Property<V> {
    private var element: ReferenceElement<*>? = null
    private var key: String? = null
    private var value: V? = null

    private constructor() {}

    fun get(): Property<V> {
        return this
    }

    constructor(property: Property<V>) {
        element = if (null == property.element()) null else ReferenceFactory.detach(property.element())
        key = property.key()
        value = property.value()
    }

    constructor(key: String?, value: V) {
        element = null
        this.key = key
        this.value = value
    }

    @Override
    override fun hashCode(): Int {
        return ElementHelper.hashCode(this)
    }

    @Override
    override fun toString(): String {
        return StringFactory.propertyString(this)
    }

    @Override
    override fun equals(`object`: Object?): Boolean {
        return ElementHelper.areEqual(this, `object`)
    }

    @Override
    fun key(): String? {
        return key
    }

    @Override
    @Throws(NoSuchElementException::class)
    fun value(): V {
        return value
    }

    @get:Override
    val isPresent: Boolean
        get() = true

    @Override
    fun element(): Element {
        return element
    }

    @Override
    fun remove() {
        throw Property.Exceptions.propertyRemovalNotSupported()
    }
}