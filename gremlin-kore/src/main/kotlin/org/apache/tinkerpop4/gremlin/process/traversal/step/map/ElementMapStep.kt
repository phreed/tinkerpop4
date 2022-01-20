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
package org.apache.tinkerpop4.gremlin.process.traversal.step.map

import org.apache.tinkerpop4.gremlin.process.traversal.Traversal
import org.apache.tinkerpop4.gremlin.process.traversal.Traverser
import org.apache.tinkerpop4.gremlin.process.traversal.step.GraphComputing
import org.apache.tinkerpop4.gremlin.process.traversal.step.TraversalParent
import org.apache.tinkerpop4.gremlin.process.traversal.traverser.TraverserRequirement
import org.apache.tinkerpop4.gremlin.structure.Direction
import org.apache.tinkerpop4.gremlin.structure.Edge
import org.apache.tinkerpop4.gremlin.structure.Element
import org.apache.tinkerpop4.gremlin.structure.Property
import org.apache.tinkerpop4.gremlin.structure.T
import org.apache.tinkerpop4.gremlin.structure.Vertex
import org.apache.tinkerpop4.gremlin.structure.VertexProperty
import org.apache.tinkerpop4.gremlin.structure.util.StringFactory
import java.util.Arrays
import java.util.Iterator
import java.util.LinkedHashMap
import java.util.Map
import java.util.Objects
import java.util.Set

/**
 * Converts a [Element] to a `Map`.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Daniel Kuppitz (http://gremlin.guru)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class ElementMapStep<K, E>(traversal: Traversal.Admin?, vararg propertyKeys: String) :
    ScalarMapStep<Element?, Map<K, E>?>(traversal), TraversalParent, GraphComputing {
    val propertyKeys: Array<String>
    var isOnGraphComputer = false
        private set

    init {
        this.propertyKeys = propertyKeys
    }

    @Override
    protected fun map(traverser: Traverser.Admin<Element?>): Map<K, E> {
        val map: Map<Object, Object> = LinkedHashMap()
        val element: Element = traverser.get()
        map.put(T.id, element.id())
        if (element is VertexProperty) {
            map.put(T.key, (element as VertexProperty<*>).key())
            map.put(T.value, (element as VertexProperty<*>).value())
        } else {
            map.put(T.label, element.label())
        }
        if (element is Edge) {
            val e: Edge = element as Edge
            map.put(Direction.IN, getVertexStructure(e.inVertex()))
            map.put(Direction.OUT, getVertexStructure(e.outVertex()))
        }
        val properties: Iterator<Property?> = element.properties(propertyKeys)
        while (properties.hasNext()) {
            val property: Property<*>? = properties.next()
            map.put(property.key(), property.value())
        }
        return map
    }

    protected fun getVertexStructure(v: Vertex): Map<Object, Object> {
        val m: Map<Object, Object> = LinkedHashMap()
        m.put(T.id, v.id())

        // can't add label if doing GraphComputer stuff as there is no access to the label of the adjacent vertex
        if (!isOnGraphComputer) m.put(T.label, v.label())
        return m
    }

    @Override
    fun onGraphComputer() {
        isOnGraphComputer = true
    }

    override fun toString(): String {
        return StringFactory.stepString(this, Arrays.asList(propertyKeys))
    }

    @Override
    override fun hashCode(): Int {
        var result = super.hashCode()
        for (propertyKey in propertyKeys) {
            result = result xor Objects.hashCode(propertyKey)
        }
        return result
    }

    @get:Override
    val requirements: Set<Any>
        get() = this.getSelfAndChildRequirements(TraverserRequirement.OBJECT)
}