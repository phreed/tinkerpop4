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

import org.apache.tinkerpop4.gremlin.structure.Edge
import org.apache.tinkerpop4.gremlin.structure.Element
import org.apache.tinkerpop4.gremlin.structure.Property
import org.apache.tinkerpop4.gremlin.tinkercat.structure.TinkerHelper.removeIndex
import org.apache.tinkerpop4.gremlin.structure.util.StringFactory
import org.apache.tinkerpop4.gremlin.structure.util.ElementHelper

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class TinkerProperty<V>(protected val element: Element, protected val key: String, protected var value: V) :
    Property<V> {
    override fun element(): Element {
        return element
    }

    override fun key(): String {
        return key
    }

    override fun value(): V {
        return value
    }

    /**
     * The existence of this object implies the property is present, thus even a `null` value means "present".
     */
    override fun isPresent(): Boolean {
        return true
    }

    override fun toString(): String {
        return StringFactory.propertyString(this)
    }

    override fun equals(`object`: Any?): Boolean {
        return ElementHelper.areEqual(this, `object`)
    }

    override fun hashCode(): Int {
        return ElementHelper.hashCode(this)
    }

    override fun remove() {
        if (element is Edge) {
            (element as TinkerEdge).properties!!.remove(key)
            removeIndex(element, key, value)
        } else {
            (element as TinkerVertexProperty<*>).properties?.remove(key)
        }
    }
}