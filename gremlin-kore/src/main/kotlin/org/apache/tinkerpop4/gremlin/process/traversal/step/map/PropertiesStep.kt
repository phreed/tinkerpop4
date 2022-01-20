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
 */
class PropertiesStep<E>(traversal: Traversal.Admin?, propertyType: PropertyType, vararg propertyKeys: String) :
    FlatMapStep<Element?, E>(traversal), AutoCloseable, Configuring {
    protected var parameters: Parameters = Parameters()
    val propertyKeys: Array<String>
    protected val returnType: PropertyType

    init {
        returnType = propertyType
        this.propertyKeys = propertyKeys
    }

    @Override
    fun getParameters(): Parameters {
        return parameters
    }

    @Override
    fun configure(vararg keyValues: Object?) {
        parameters.set(null, keyValues)
    }

    @Override
    protected fun flatMap(traverser: Traverser.Admin<Element?>): Iterator<E> {
        return if (returnType.equals(PropertyType.VALUE)) traverser.get().values(propertyKeys) else traverser.get()
            .properties(
                propertyKeys
            )
    }

    fun getReturnType(): PropertyType {
        return returnType
    }

    @Override
    override fun toString(): String {
        return StringFactory.stepString(this, Arrays.asList(propertyKeys), returnType.name().toLowerCase())
    }

    @Override
    override fun hashCode(): Int {
        var result = super.hashCode() xor returnType.hashCode()
        for (propertyKey in propertyKeys) {
            result = result xor Objects.hashCode(propertyKey)
        }
        return result
    }

    @get:Override
    val requirements: Set<Any>
        get() = Collections.singleton(TraverserRequirement.OBJECT)

    @Override
    @Throws(Exception::class)
    fun close() {
        closeIterator()
    }
}