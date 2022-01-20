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

import org.apache.tinkerpop4.gremlin.process.traversal.step.util.Tree

/**
 * Provides quick lookup for Type deserialization extracted from the JSON payload. As well as the Java Object to types
 * compatible for the version 2.0 of GraphSON.
 *
 * @author Kevin Gallardo (https://kgdo.me)
 */
class GraphSONTypeIdResolver : TypeIdResolver {
    private val idToType: Map<String, JavaType> = HashMap()
    private val typeToId: Map<Class, String> = HashMap()

    // Override manually a type definition.
    fun addCustomType(name: String?, clasz: Class?): GraphSONTypeIdResolver {
        if (Tree::class.java.isAssignableFrom(clasz)) {
            // there is a special case for Tree which extends a Map, but has only 1 parametrized type,
            // and for which creating a default type is failing because it may fall into a
            // a self-referencing never-ending loop. Temporarily we force Tree<Element>
            // which should cover all the usage TinkerPop would do of the Trees anyway.
            idToType.put(
                name,
                TypeFactory.defaultInstance().constructType(object : TypeReference<Tree<out Element?>?>() {})
            )
        } else {
            idToType.put(name, TypeFactory.defaultInstance().constructType(clasz))
        }
        typeToId.put(clasz, name)
        return this
    }

    fun getIdToType(): Map<String, JavaType> {
        return idToType
    }

    fun getTypeToId(): Map<Class, String> {
        return typeToId
    }

    @Override
    fun init(javaType: JavaType?) {
    }

    @Override
    fun idFromValue(o: Object): String? {
        return idFromValueAndType(o, o.getClass())
    }

    @Override
    fun idFromValueAndType(o: Object?, aClass: Class<*>): String? {
        return if (!typeToId.containsKey(aClass)) {
            // If one wants to serialize an object with a type, but hasn't registered
            // a typeID for that class, fail.
            throw IllegalArgumentException(
                String.format(
                    "Could not find a type identifier for the class : %s. " +
                            "Make sure the value to serialize has a type identifier registered for its class.", aClass
                )
            )
        } else {
            typeToId[aClass]
        }
    }

    @Override
    fun idFromBaseType(): String? {
        return null
    }

    @Override
    fun typeFromId(databindContext: DatabindContext, s: String): JavaType? {
        // Get the type from the string from the stored Map. If not found, default to deserialize as a String.
        return if (idToType.containsKey(s)) idToType[s] else databindContext.constructType(String::class.java)
    }

    @get:Override
    val descForKnownTypeIds: String
        get() = "GraphSON advanced typing system"

    @get:Override
    val mechanism: JsonTypeInfo.Id
        get() = JsonTypeInfo.Id.CUSTOM
}