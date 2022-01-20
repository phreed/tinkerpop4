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

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Daniel Kuppitz (http://gremlin.guru)
 */
class PropertyMapStep<K, E>(traversal: Traversal.Admin?, propertyType: PropertyType, vararg propertyKeys: String) :
    ScalarMapStep<Element?, Map<K, E>?>(traversal), TraversalParent, ByModulating, Configuring {
    val propertyKeys: Array<String>
    protected val returnType: PropertyType
    var includedTokens = 0
        protected set
    protected var propertyTraversal: Traversal.Admin<Element, out Property?>
    private val parameters: Parameters = Parameters()
    private var traversalRing: TraversalRing<K, E>

    init {
        this.propertyKeys = propertyKeys
        returnType = propertyType
        propertyTraversal = null
        traversalRing = TraversalRing()
    }

    constructor(
        traversal: Traversal.Admin?,
        options: Int,
        propertyType: PropertyType,
        vararg propertyKeys: String?
    ) : this(traversal, propertyType, *propertyKeys) {
        configure(WithOptions.tokens, options)
    }

    @Override
    protected fun map(traverser: Traverser.Admin<Element?>): Map<K, E> {
        val map: Map<Object, Object> = LinkedHashMap()
        val element: Element = traverser.get()
        val isVertex = element is Vertex
        if (returnType === PropertyType.VALUE) {
            if (includeToken(WithOptions.ids)) map.put(T.id, element.id())
            if (element is VertexProperty) {
                if (includeToken(WithOptions.keys)) map.put(T.key, (element as VertexProperty<*>).key())
                if (includeToken(WithOptions.values)) map.put(T.value, (element as VertexProperty<*>).value())
            } else {
                if (includeToken(WithOptions.labels)) map.put(T.label, element.label())
            }
        }
        val properties: Iterator<Property?> =
            if (null == propertyTraversal) element.properties(propertyKeys) else TraversalUtil.applyAll(
                traverser,
                propertyTraversal
            )
        while (properties.hasNext()) {
            val property: Property<*>? = properties.next()
            val value: Object = if (returnType === PropertyType.VALUE) property.value() else property
            if (isVertex) {
                map.compute(property.key()) { k, v ->
                    val values: List<Object> = if (v != null) v else ArrayList()
                    values.add(value)
                    values
                }
            } else {
                map.put(property.key(), value)
            }
        }
        if (!traversalRing.isEmpty()) {
            // will cop a ConcurrentModification if a key is dropped so need this little copy here
            val keys: Set<Object> = HashSet(map.keySet())
            for (key in keys) {
                map.compute(key) { k, v ->
                    val product: TraversalProduct = TraversalUtil.produce(v, traversalRing.next() as Traversal.Admin)
                    if (product.isProductive()) product.get() else null
                }
            }
            traversalRing.reset()
        }
        return map
    }

    @Override
    fun configure(vararg keyValues: Object) {
        if (keyValues[0].equals(WithOptions.tokens)) {
            if (keyValues.size == 2 && keyValues[1] is Boolean) {
                includedTokens = if (keyValues[1]) WithOptions.all else WithOptions.none
            } else {
                for (i in 1 until keyValues.size) {
                    if (keyValues[i] !is Integer) throw IllegalArgumentException(
                        "WithOptions.tokens requires Integer arguments (possible " + "" +
                                "values are: WithOptions.[none|ids|labels|keys|values|all])"
                    )
                    includedTokens = includedTokens or keyValues[i] as Int
                }
            }
        } else {
            parameters.set(this, keyValues)
        }
    }

    @Override
    fun getParameters(): Parameters {
        return parameters
    }

    @get:Override
    val localChildren: List<Any>
        get() {
            val result: List<Traversal.Admin<K, E>> = ArrayList()
            if (null != propertyTraversal) result.add(propertyTraversal as Traversal.Admin)
            result.addAll(traversalRing.getTraversals())
            return Collections.unmodifiableList(result)
        }

    @Override
    fun modulateBy(selectTraversal: Traversal.Admin<*, *>?) {
        traversalRing.addTraversal(this.integrateChild(selectTraversal))
    }

    fun setPropertyTraversal(propertyTraversal: Traversal.Admin<Element?, out Property?>?) {
        this.propertyTraversal = this.integrateChild(propertyTraversal)
    }

    fun getReturnType(): PropertyType {
        return returnType
    }

    override fun toString(): String {
        return StringFactory.stepString(
            this, Arrays.asList(propertyKeys),
            traversalRing, returnType.name().toLowerCase()
        )
    }

    @Override
    fun clone(): PropertyMapStep<K, E> {
        val clone = super.clone() as PropertyMapStep<K, E>
        if (null != propertyTraversal) clone.propertyTraversal = propertyTraversal.clone()
        clone.traversalRing = traversalRing.clone()
        return clone
    }

    @Override
    override fun hashCode(): Int {
        var result = super.hashCode() xor returnType.hashCode() xor Integer.hashCode(includedTokens)
        if (null != propertyTraversal) result = result xor propertyTraversal.hashCode()
        for (propertyKey in propertyKeys) {
            result = result xor Objects.hashCode(propertyKey)
        }
        return result xor traversalRing.hashCode()
    }

    @Override
    fun setTraversal(parentTraversal: Traversal.Admin<*, *>?) {
        super.setTraversal(parentTraversal)
        if (null != propertyTraversal) this.integrateChild(propertyTraversal)
        traversalRing.getTraversals().forEach(this::integrateChild)
    }

    @get:Override
    val requirements: Set<Any>
        get() = this.getSelfAndChildRequirements(TraverserRequirement.OBJECT)

    private fun includeToken(token: Int): Boolean {
        return 0 != includedTokens and token
    }
}