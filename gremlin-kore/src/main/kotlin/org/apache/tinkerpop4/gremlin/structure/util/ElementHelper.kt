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
package org.apache.tinkerpop4.gremlin.structure.util

import org.apache.tinkerpop4.gremlin.structure.Edge

/**
 * Utility class supporting common functions for [Element].
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
object ElementHelper {
    /**
     * Determine whether the [Element] label can be legally set. This is typically used as a pre-condition check.
     *
     * @param label the element label
     * @throws IllegalArgumentException whether the label is legal and if not, a clear reason exception is provided
     */
    @Throws(IllegalArgumentException::class)
    fun validateLabel(label: String?) {
        if (null == label) throw Element.Exceptions.labelCanNotBeNull()
        if (label.isEmpty()) throw Element.Exceptions.labelCanNotBeEmpty()
        if (Graph.Hidden.isHidden(label)) throw Element.Exceptions.labelCanNotBeAHiddenKey(label)
    }

    /**
     * Determines whether the property key/value for the specified thing can be legally set. This is typically used as
     * a pre-condition check prior to setting a property.
     *
     * @param key   the key of the property
     * @param value the value of the property\
     * @throws IllegalArgumentException whether the key/value pair is legal and if not, a clear reason exception
     * message is provided
     */
    @Throws(IllegalArgumentException::class)
    fun validateProperty(key: String?, value: Object?) {
        if (null == key) throw Property.Exceptions.propertyKeyCanNotBeNull()
        if (key.isEmpty()) throw Property.Exceptions.propertyKeyCanNotBeEmpty()
        if (Graph.Hidden.isHidden(key)) throw Property.Exceptions.propertyKeyCanNotBeAHiddenKey(key)
    }

    /**
     * Determines whether a list of key/values are legal, ensuring that there are an even number of values submitted
     * and that the keys in the list of arguments are [String] or [T] objects.
     *
     * @param propertyKeyValues a list of key/value pairs
     * @throws IllegalArgumentException if something in the pairs is illegal
     */
    @Throws(IllegalArgumentException::class)
    fun legalPropertyKeyValueArray(vararg propertyKeyValues: Object?) {
        if (propertyKeyValues.size % 2 != 0) throw Element.Exceptions.providedKeyValuesMustBeAMultipleOfTwo()
        var i = 0
        while (i < propertyKeyValues.size) {
            if (propertyKeyValues[i] !is String && propertyKeyValues[i] !is T) throw Element.Exceptions.providedKeyValuesMustHaveALegalKeyOnEvenIndices()
            i = i + 2
        }
    }

    /**
     * Extracts the value of the [T.id] key from the list of arguments.
     *
     * @param keyValues a list of key/value pairs
     * @return the value associated with [T.id]
     */
    fun getIdValue(vararg keyValues: Object): Optional<Object> {
        var i = 0
        while (i < keyValues.size) {
            if (keyValues[i].equals(T.id)) return Optional.ofNullable(keyValues[i + 1])
            i = i + 2
        }
        return Optional.empty()
    }

    /**
     * Remove a key from the set of key/value pairs. Assumes that validations have already taken place to
     * assure that key positions contain strings and that there are an even number of elements. If after removal
     * there are no values left, the key value list is returned as empty.
     *
     * @param keyToRemove the key to remove
     * @param keyValues   the list to remove the accessor from
     * @return the key/values without the specified accessor or an empty array if no values remain after removal
     */
    fun remove(keyToRemove: String?, vararg keyValues: Object?): Optional<Array<Object>> {
        return remove(keyToRemove as Object?, *keyValues)
    }

    /**
     * Removes an accessor from the set of key/value pairs. Assumes that validations have already taken place to
     * assure that key positions contain strings and that there are an even number of elements. If after removal
     * there are no values left, the key value list is returned as empty.
     *
     * @param accessor  to remove
     * @param keyValues the list to remove the accessor from
     * @return the key/values without the specified accessor or an empty array if no values remain after removal
     */
    fun remove(accessor: T?, vararg keyValues: Object?): Optional<Array<Object>> {
        return remove(accessor as Object?, *keyValues)
    }

    private fun remove(keyToRemove: Object, vararg keyValues: Object): Optional<Array<Object>> {
        val list: List = Arrays.asList(keyValues)
        val revised: List = IntStream.range(0, list.size())
            .filter { i -> i % 2 == 0 }
            .filter { i -> !keyToRemove.equals(list.get(i)) }
            .flatMap { i -> IntStream.of(i, i + 1) }
            .mapToObj { i -> list.get(i) }
            .collect(Collectors.toList())
        return if (revised.size() > 0) Optional.of(revised.toArray()) else Optional.empty()
    }

    /**
     * Append a key/value pair to a list of existing key/values. If the key already exists in the keyValues then
     * that value is overwritten with the provided value.
     */
    fun upsert(keyValues: Array<Object>, key: Object, `val`: Object?): Array<Object?> {
        return if (!getKeys(*keyValues).contains(key)) Stream.concat(
            Stream.of(keyValues),
            Stream.of(key, `val`)
        ).toArray() else {
            val kvs: Array<Object?> = arrayOfNulls<Object>(keyValues.size)
            var i = 0
            while (i < keyValues.size) {
                kvs[i] = keyValues[i]
                if (keyValues[i].equals(key)) kvs[i + 1] = `val` else kvs[i + 1] = keyValues[i + 1]
                i = i + 2
            }
            kvs
        }
    }

    /**
     * Replaces one key with a different key.
     *
     * @param keyValues the list of key/values to alter
     * @param oldKey    the key to replace
     * @param newKey    the new key
     */
    fun replaceKey(keyValues: Array<Object>, oldKey: Object?, newKey: Object?): Array<Object?> {
        val kvs: Array<Object?> = arrayOfNulls<Object>(keyValues.size)
        var i = 0
        while (i < keyValues.size) {
            if (keyValues[i].equals(oldKey)) kvs[i] = newKey else kvs[i] = keyValues[i]
            kvs[i + 1] = keyValues[i + 1]
            i = i + 2
        }
        return kvs
    }

    /**
     * Converts a set of key values to a Map.  Assumes that validations have already taken place to
     * assure that key positions contain strings and that there are an even number of elements.
     */
    fun asMap(vararg keyValues: Object?): Map<String, Object> {
        return asPairs(*keyValues).stream().collect(Collectors.toMap({ p -> p.getValue0() }) { p -> p.getValue1() })
    }

    /**
     * Convert a set of key values to a list of Pair objects.  Assumes that validations have already taken place to
     * assure that key positions contain strings and that there are an even number of elements.
     */
    fun asPairs(vararg keyValues: Object?): List<Pair<String, Object>> {
        val list: List = Arrays.asList(keyValues)
        return IntStream.range(1, list.size())
            .filter { i -> i % 2 != 0 }
            .mapToObj { i -> Pair.with(list.get(i - 1).toString(), list.get(i)) }
            .collect(Collectors.toList())
    }

    /**
     * Gets the list of keys from the key values.
     *
     * @param keyValues a list of key/values pairs
     */
    fun getKeys(vararg keyValues: Object): Set<String> {
        val keys: Set<String> = HashSet()
        var i = 0
        while (i < keyValues.size) {
            keys.add(keyValues[i].toString())
            i = i + 2
        }
        return keys
    }

    /**
     * Extracts the value of the [T.label] key from the list of arguments.
     *
     * @param keyValues a list of key/value pairs
     * @return the value associated with [T.label]
     * @throws ClassCastException   if the value of the label is not a [String]
     * @throws NullPointerException if the value for the [T.label] key is `null`
     */
    fun getLabelValue(vararg keyValues: Object): Optional<String> {
        var i = 0
        while (i < keyValues.size) {
            if (keyValues[i].equals(T.label)) {
                validateLabel(keyValues[i + 1] as String)
                return Optional.of(keyValues[i + 1] as String)
            }
            i = i + 2
        }
        return Optional.empty()
    }

    /**
     * Assign key/value pairs as properties to an [Element].  If the value of [T.id] or
     * [T.label] is in the set of pairs, then they are ignored.
     *
     * @param element           the graph element to assign the `propertyKeyValues`
     * @param propertyKeyValues the key/value pairs to assign to the `element`
     * @throws ClassCastException       if the value of the key is not a [String]
     * @throws IllegalArgumentException if the value of `element` is null
     */
    fun attachProperties(element: Element?, vararg propertyKeyValues: Object?) {
        if (null == element) throw Graph.Exceptions.argumentCanNotBeNull("element")
        val allowNullPropertyValues: Boolean = if (element is Vertex) element.graph().features().vertex()
            .supportsNullPropertyValues() else if (element is Edge) element.graph().features().edge()
            .supportsNullPropertyValues() else element.graph().features().vertex().properties()
            .supportsNullPropertyValues()
        var i = 0
        while (i < propertyKeyValues.size) {
            if (!propertyKeyValues[i].equals(T.id) && !propertyKeyValues[i].equals(T.label)) if (!allowNullPropertyValues && null == propertyKeyValues[i + 1]) element.properties(
                propertyKeyValues[i] as String?
            ).forEachRemaining(Property::remove) else element.property(
                propertyKeyValues[i] as String?,
                propertyKeyValues[i + 1]
            )
            i = i + 2
        }
    }

    /**
     * Assign key/value pairs as properties to an [Vertex].  If the value of [T.id] or [T.label] is
     * in the set of pairs, then they are ignored. The [VertexProperty.Cardinality] of the key is determined from
     * the [Graph.Features.VertexFeatures].
     *
     * @param vertex            the graph vertex to assign the `propertyKeyValues`
     * @param propertyKeyValues the key/value pairs to assign to the `element`
     * @throws ClassCastException       if the value of the key is not a [String]
     * @throws IllegalArgumentException if the value of `element` is null
     */
    fun attachProperties(vertex: Vertex?, vararg propertyKeyValues: Object?) {
        if (null == vertex) throw Graph.Exceptions.argumentCanNotBeNull("vertex")
        val allowNullPropertyValues: Boolean = vertex.graph().features().vertex().supportsNullPropertyValues()
        var i = 0
        while (i < propertyKeyValues.size) {
            if (!propertyKeyValues[i].equals(T.id) && !propertyKeyValues[i].equals(T.label)) if (!allowNullPropertyValues && null == propertyKeyValues[i + 1]) vertex.properties(
                propertyKeyValues[i] as String?
            ).forEachRemaining(VertexProperty::remove) else vertex.property(
                vertex.graph().features().vertex().getCardinality(
                    propertyKeyValues[i] as String?
                ), propertyKeyValues[i] as String?, propertyKeyValues[i + 1]
            )
            i = i + 2
        }
    }

    /**
     * Assign key/value pairs as properties to a [Vertex]. If the value of [T.id] or [T.label] is
     * in the set of pairs, then they are ignored.
     *
     * @param vertex            the vertex to attach the properties to
     * @param cardinality       the cardinality of the key value pair settings
     * @param propertyKeyValues the key/value pairs to assign to the `element`
     * @throws ClassCastException       if the value of the key is not a [String]
     * @throws IllegalArgumentException if the value of `element` is null
     */
    fun attachProperties(
        vertex: Vertex?, cardinality: VertexProperty.Cardinality?,
        vararg propertyKeyValues: Object?
    ) {
        if (null == vertex) throw Graph.Exceptions.argumentCanNotBeNull("vertex")
        val allowNullPropertyValues: Boolean = vertex.graph().features().vertex().supportsNullPropertyValues()
        var i = 0
        while (i < propertyKeyValues.size) {
            if (!propertyKeyValues[i].equals(T.id) && !propertyKeyValues[i].equals(T.label)) if (!allowNullPropertyValues && null == propertyKeyValues[i + 1]) vertex.properties(
                propertyKeyValues[i] as String?
            ).forEachRemaining(VertexProperty::remove) else vertex.property(
                cardinality,
                propertyKeyValues[i] as String?,
                propertyKeyValues[i + 1]
            )
            i = i + 2
        }
    }

    /**
     * This is a helper method for dealing with vertex property cardinality and typically used in [Vertex.property].
     * If the cardinality is list, simply return [Optional.empty].
     * If the cardinality is single, delete all existing properties of the provided key and return [Optional.empty].
     * If the cardinality is set, find one that has the same key/value and attached the properties to it and return it. Else, if no equal value is found, return [Optional.empty].
     *
     * @param vertex      the vertex to stage a vertex property for
     * @param cardinality the cardinality of the vertex property
     * @param key         the key of the vertex property
     * @param value       the value of the vertex property
     * @param keyValues   the properties of vertex property
     * @param <V>         the type of the vertex property value
     * @return a vertex property if it has been found in set with equal value
    </V> */
    fun <V> stageVertexProperty(
        vertex: Vertex,
        cardinality: VertexProperty.Cardinality,
        key: String?, value: V, vararg keyValues: Object?
    ): Optional<VertexProperty<V>> {
        if (cardinality.equals(VertexProperty.Cardinality.single)) vertex.properties(key)
            .forEachRemaining(VertexProperty::remove) else if (cardinality.equals(VertexProperty.Cardinality.set)) {
            val itty: Iterator<VertexProperty<V>> = vertex.properties(key)
            while (itty.hasNext()) {
                val property: VertexProperty<V> = itty.next()
                if (property.value().equals(value)) {
                    attachProperties(property, keyValues)
                    return Optional.of(property)
                }
            }
        } // do nothing on Cardinality.list
        return Optional.empty()
    }

    /**
     * Retrieve the properties associated with a particular element.
     * The result is a Object[] where odd indices are String keys and even indices are the values.
     *
     * @param element          the element to retrieve properties from
     * @param includeId        include Element.ID in the key/value list
     * @param includeLabel     include Element.LABEL in the key/value list
     * @param propertiesToCopy the properties to include with an empty list meaning copy all properties
     * @return a key/value array of properties where odd indices are String keys and even indices are the values.
     */
    fun getProperties(
        element: Element,
        includeId: Boolean,
        includeLabel: Boolean,
        propertiesToCopy: Set<String?>
    ): Array<Object> {
        val keyValues: List<Object> = ArrayList()
        if (includeId) {
            keyValues.add(T.id)
            keyValues.add(element.id())
        }
        if (includeLabel) {
            keyValues.add(T.label)
            keyValues.add(element.label())
        }
        element.keys().forEach { key ->
            if (propertiesToCopy.isEmpty() || propertiesToCopy.contains(key)) {
                keyValues.add(key)
                keyValues.add(element.value(key))
            }
        }
        return keyValues.toArray(arrayOfNulls<Object>(keyValues.size()))
    }

    /**
     * A standard method for determining if two [Element] objects are equal. This method should be used by any
     * [Object.equals] implementation to ensure consistent behavior. This method is used for Vertex,
     * Edge, and VertexProperty.
     *
     * @param a The first [Element]
     * @param b The second [Element] (as an [Object])
     * @return true if elements and equal and false otherwise
     */
    fun areEqual(a: Element?, b: Object?): Boolean {
        if (a === b) return true
        if (null == b || null == a) return false
        return if (!(a is Vertex && b is Vertex ||
                    a is Edge && b is Edge ||
                    a is VertexProperty && b is VertexProperty)
        ) false else haveEqualIds(a, b as Element)
    }

    fun areEqual(a: Vertex?, b: Vertex?): Boolean {
        return null != b && null != a && (a === b || haveEqualIds(a, b))
    }

    fun areEqual(a: Edge?, b: Edge?): Boolean {
        return null != b && null != a && (a === b || haveEqualIds(a, b))
    }

    fun areEqual(a: VertexProperty?, b: VertexProperty?): Boolean {
        return null != b && null != a && (a === b || haveEqualIds(a, b))
    }

    /**
     * A standard method for determining if two [VertexProperty] objects are equal. This method should be used by any
     * [Object.equals] implementation to ensure consistent behavior.
     *
     * @param a the first [VertexProperty]
     * @param b the second [VertexProperty]
     * @return true if equal and false otherwise
     */
    fun areEqual(a: VertexProperty?, b: Object?): Boolean {
        return areEqual(a as Element?, b)
    }

    /**
     * Simply tests if the value returned from [Element.id] are `equal()`.
     *
     * @param a the first [Element]
     * @param b the second [Element]
     * @return true if ids are equal and false otherwise
     */
    fun haveEqualIds(a: Element, b: Element): Boolean {
        return a.id().equals(b.id())
    }

    /**
     * If two [Element] instances are equal, then they must have the same hash codes. This methods ensures consistent hashCode values.
     *
     * @param element the element to get the hashCode for
     * @return the hash code of the element
     */
    fun hashCode(element: Element): Int {
        return element.id().hashCode()
    }

    /**
     * If two [Property] instances are equal, then they must have the same hash codes. This methods ensures consistent hashCode values.
     * For [VertexProperty] use [ElementHelper.hashCode].
     *
     * @param property the property to get the hashCode for
     * @return the hash code of the property
     */
    fun hashCode(property: Property): Int {
        return property.key().hashCode() + Objects.hashCode(property.value())
    }

    /**
     * A standard method for determining if two [Property] objects are equal. This method should be used by any
     * [Object.equals] implementation to ensure consistent behavior.
     *
     * @param a the first [Property]
     * @param b the second [Property]
     * @return true if equal and false otherwise
     */
    fun areEqual(a: Property?, b: Object?): Boolean {
        if (a === b) return true
        if (null == b || null == a) return false
        if (b !is Property) return false
        if (!a.isPresent() && !(b as Property).isPresent()) return true
        return if (!a.isPresent() && (b as Property).isPresent() || a.isPresent() && !(b as Property).isPresent()) false else a.key()
            .equals((b as Property).key()) && a.value().equals((b as Property).value())
    }

    fun propertyValueMap(element: Element, vararg propertyKeys: String?): Map<String, Object> {
        val values: Map<String, Object> = HashMap()
        element.properties(propertyKeys).forEachRemaining { property -> values.put(property.key(), property.value()) }
        return values
    }

    fun propertyMap(element: Element, vararg propertyKeys: String?): Map<String, Property> {
        val propertyMap: Map<String, Property> = HashMap()
        element.properties(propertyKeys).forEachRemaining { property -> propertyMap.put(property.key(), property) }
        return propertyMap
    }

    fun vertexPropertyValueMap(vertex: Vertex, vararg propertyKeys: String?): Map<String, List> {
        val valueMap: Map<String, List> = HashMap()
        vertex.properties(propertyKeys).forEachRemaining { property ->
            if (valueMap.containsKey(property.key())) valueMap[property.key()].add(property.value()) else {
                val list: List = ArrayList()
                list.add(property.value())
                valueMap.put(property.key(), list)
            }
        }
        return valueMap
    }

    fun vertexPropertyMap(vertex: Vertex, vararg propertyKeys: String?): Map<String, List<VertexProperty>> {
        val propertyMap: Map<String, List<VertexProperty>> = HashMap()
        vertex.properties(propertyKeys).forEachRemaining { property ->
            if (propertyMap.containsKey(property.key())) propertyMap[property.key()].add(property) else {
                val list: List<VertexProperty> = ArrayList()
                list.add(property)
                propertyMap.put(property.key(), list)
            }
        }
        return propertyMap
    }

    /**
     * Checks if a key exists within a list of provided keys. Returns `false` if the key is `null` or if
     * the [Graph.Hidden]. Returns `true` if no `providedKeys` are supplied.
     *
     * @param key must not be `null`
     */
    fun keyExists(key: String, vararg providedKeys: String?): Boolean {
        Objects.requireNonNull(key)
        if (Graph.Hidden.isHidden(key)) return false
        if (null == providedKeys || 0 == providedKeys.size) return true
        return if (1 == providedKeys.size) key.equals(providedKeys[0]) else {
            for (temp in providedKeys) {
                if (key.equals(temp)) return true
            }
            false
        }
    }

    fun idExists(id: Object?, vararg providedIds: Object?): Boolean {
        if (0 == providedIds.size) return true

        // it is OK to evaluate equality of ids via toString() now given that the toString() the test suite
        // enforces the value of id.()toString() to be a first class representation of the identifier
        return if (1 == providedIds.size) {
            id != null && providedIds[0] != null && id.toString().equals(providedIds[0].toString())
        } else {
            for (temp in providedIds) {
                if (id != null && temp != null && temp.toString().equals(id.toString())) return true
            }
            false
        }
    }
}