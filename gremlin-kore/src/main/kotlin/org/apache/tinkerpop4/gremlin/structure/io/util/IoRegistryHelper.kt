/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.tinkerpop4.gremlin.structure.io.util

import org.apache.commons.configuration2.Configuration
import org.apache.tinkerpop4.gremlin.structure.io.IoRegistry
import java.lang.reflect.Method
import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.List

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
object IoRegistryHelper {
    fun createRegistries(registryNamesClassesOrInstances: List<Object?>): List<IoRegistry> {
        if (registryNamesClassesOrInstances.isEmpty()) return Collections.emptyList()
        val registries: List<IoRegistry> = ArrayList()
        for (`object` in registryNamesClassesOrInstances) {
            if (`object` is IoRegistry) registries.add(`object` as IoRegistry) else if (`object` is String || `object` is Class) {
                try {
                    val clazz: Class<*> =
                        if (`object` is String) Class.forName(`object` as String) else `object` as Class
                    var instanceMethod: Method? = null
                    try {
                        instanceMethod = clazz.getDeclaredMethod("instance") // try for getInstance() ??
                    } catch (e: NoSuchMethodException) {
                        try {
                            // even though use of "getInstance" is no longer a thing in tinkerpop as of 3.3.0, perhaps
                            // others are using this style of naming. Doesn't seem to harm anything to continue to
                            // check for that method.
                            instanceMethod = clazz.getDeclaredMethod("getInstance") // try for getInstance() ??
                        } catch (e2: NoSuchMethodException) {
                            // no instance() or getInstance() methods
                        }
                    }
                    if (null != instanceMethod && IoRegistry::class.java.isAssignableFrom(instanceMethod.getReturnType())) registries.add(
                        instanceMethod.invoke(null) as IoRegistry
                    ) else registries.add(clazz.newInstance() as IoRegistry) // no instance() or getInstance() methods, try instantiate class
                } catch (e: Exception) {
                    throw IllegalStateException(e.getMessage(), e)
                }
            } else {
                throw IllegalArgumentException("The provided registry object can not be resolved to an instance: $`object`")
            }
        }
        return registries
    }

    fun createRegistries(configuration: Configuration): List<IoRegistry> {
        return if (configuration.containsKey(IoRegistry.IO_REGISTRY)) {
            val property: Object = configuration.getProperty(IoRegistry.IO_REGISTRY)
            if (property is IoRegistry) Collections.singletonList(property as IoRegistry) else if (property is List) createRegistries(
                property as List
            ) else if (property is String) createRegistries(Arrays.asList((property as String).split(","))) else throw IllegalArgumentException(
                "The provided registry object can not be resolved to an instance: $property"
            )
        } else Collections.emptyList()
    }
}