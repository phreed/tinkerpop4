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

import org.apache.tinkerpop4.gremlin.process.traversal.P

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class HasContainer(var key: String?, predicate: P<*>?) : Serializable, Cloneable, Predicate<Element?> {
    private var predicate: P?
    private val testingIdString: Boolean

    init {
        this.predicate = predicate
        testingIdString = isStringTestable
    }

    fun test(element: Element): Boolean {
        // it is OK to evaluate equality of ids via toString(), given that the test suite enforces the value of
        // id().toString() to be a first class representation of the identifier. a string test is only executed
        // if the predicate value is a String.  this allows stuff like: g.V().has(id,lt(10)) to work properly
        if (key!!.equals(T.id.getAccessor())) return if (testingIdString) testIdAsString(element) else testId(element)
        if (key!!.equals(T.label.getAccessor())) return testLabel(element)
        val itty: Iterator<Property?> = element.properties(key)
        try {
            while (itty.hasNext()) {
                if (testValue(itty.next())) return true
            }
        } finally {
            CloseableIterator.closeIterator(itty)
        }
        return false
    }

    fun test(property: Property): Boolean {
        if (key!!.equals(T.value.getAccessor())) return testValue(property)
        if (key!!.equals(T.key.getAccessor())) return testKey(property)
        return if (property is Element) test(property as Element) else false
    }

    protected fun testId(element: Element): Boolean {
        return predicate.test(element.id())
    }

    protected fun testIdAsString(element: Element): Boolean {
        return predicate.test(element.id().toString())
    }

    protected fun testLabel(element: Element): Boolean {
        return predicate.test(element.label())
    }

    protected fun testValue(property: Property?): Boolean {
        return predicate.test(property.value())
    }

    protected fun testKey(property: Property): Boolean {
        return predicate.test(property.key())
    }

    override fun toString(): String {
        return key + '.' + predicate
    }

    fun clone(): HasContainer {
        return try {
            val clone = super.clone() as HasContainer
            clone.predicate = predicate.clone()
            clone
        } catch (e: CloneNotSupportedException) {
            throw IllegalStateException(e.getMessage(), e)
        }
    }

    @Override
    override fun hashCode(): Int {
        return (if (key != null) key!!.hashCode() else 0) xor if (predicate != null) predicate.hashCode() else 0
    }

    fun getPredicate(): P<*>? {
        return predicate
    }

    val biPredicate: BiPredicate<*, *>
        get() = predicate.getBiPredicate()
    val value: Object
        get() = predicate.getValue()
    ////////////
    /**
     * Determines if the value of the predicate is testable via `toString()` representation of an element which
     * is only relevant if the has relates to an [T.id].
     */
    private val isStringTestable: Boolean
        private get() {
            if (key!!.equals(T.id.getAccessor())) {
                val predicateValue: Object? = if (null == predicate) null else predicate.getValue()
                if (predicateValue is Collection) {
                    if (!predicateValue.isEmpty()) {
                        return (predicateValue as Collection?).stream().allMatch { c -> null == c || c is String }
                    }
                }
                return predicateValue is String
            }
            return false
        }

    companion object {
        fun <V> testAll(property: Property<V>, hasContainers: List<HasContainer>): Boolean {
            return internalTestAll<Any>(property, hasContainers)
        }

        fun testAll(element: Element, hasContainers: List<HasContainer>): Boolean {
            return internalTestAll<Any>(element, hasContainers)
        }

        private fun <S> internalTestAll(element: S, hasContainers: List<HasContainer>): Boolean {
            val isProperty = element is Property
            for (hasContainer in hasContainers) {
                if (isProperty) {
                    if (!hasContainer.test(element as Property)) return false
                } else {
                    if (!hasContainer.test(element as Element)) return false
                }
            }
            return true
        }
    }
}