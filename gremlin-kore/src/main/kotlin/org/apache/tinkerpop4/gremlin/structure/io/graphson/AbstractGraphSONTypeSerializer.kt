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
package org.apache.tinkerpop4.gremlin.structure.io.graphson

import org.apache.tinkerpop.shaded.jackson.annotation.JsonTypeInfo

/**
 * Extension of the Jackson's default TypeSerializer. An instance of this object will be passed to the serializers
 * on which they can safely call the utility methods to serialize types and making it compatible with the version
 * 2.0+ of GraphSON.
 *
 * @author Kevin Gallardo (https://kgdo.me)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
abstract class AbstractGraphSONTypeSerializer internal constructor(
    idRes: TypeIdResolver, propertyName: String, typeInfo: TypeInfo?,
    valuePropertyName: String
) : TypeSerializer() {
    protected val idRes: TypeIdResolver

    @get:Override
    val propertyName: String
    protected val typeInfo: TypeInfo?
    protected val valuePropertyName: String
    protected val classMap: Map<Class, Class> = HashMap()

    init {
        this.idRes = idRes
        this.propertyName = propertyName
        this.typeInfo = typeInfo
        this.valuePropertyName = valuePropertyName
    }

    @Override
    fun forProperty(beanProperty: BeanProperty?): TypeSerializer {
        return this
    }

    @get:Override
    val typeInclusion: JsonTypeInfo.As
        get() = JsonTypeInfo.As.WRAPPER_OBJECT

    @get:Override
    val typeIdResolver: TypeIdResolver
        get() = idRes

    protected fun canWriteTypeId(): Boolean {
        return (typeInfo != null
                && typeInfo === TypeInfo.PARTIAL_TYPES)
    }

    @Throws(IOException::class)
    protected fun writeTypePrefix(jsonGenerator: JsonGenerator, s: String?) {
        jsonGenerator.writeStartObject()
        jsonGenerator.writeStringField(propertyName, s)
        jsonGenerator.writeFieldName(valuePropertyName)
    }

    @Throws(IOException::class)
    protected fun writeTypeSuffix(jsonGenerator: JsonGenerator) {
        jsonGenerator.writeEndObject()
    }

    /**
     * We force only **one** translation of a Java object to a domain specific object. i.e. users register typeIDs
     * and serializers/deserializers for the predefined types we have in the spec. Graph, Vertex, Edge,
     * VertexProperty, etc... And **not** their implementations (TinkerGraph, DetachedVertex, TinkerEdge, etc..)
     */
    protected abstract fun getClassFromObject(o: Object?): Class?
}