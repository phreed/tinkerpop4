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
package org.apache.tinkerpop4.gremlin.jsr223

import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.Collections
import java.util.LinkedHashSet
import java.util.Set
import java.util.stream.Collectors

/**
 * Provides the list of imports to apply to a [GremlinScriptEngine] instance.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
interface ImportCustomizer : Customizer {
    /**
     * Gets the set of classes to be imported to the [GremlinScriptEngine].
     */
    val classImports: Set<Any?>

    /**
     * Gets the set of static methods to be imported to the [GremlinScriptEngine].
     */
    val methodImports: Set<Any?>

    /**
     * Gets the set of enums to be imported to the [GremlinScriptEngine].
     */
    val enumImports: Set<Any?>

    /**
     * Gets the set of fields to be imported to the [GremlinScriptEngine].
     */
    val fieldImports: Set<Any?>

    /**
     * Gets the set of packages from the [.getClassImports].
     */
    val classPackages: Set<Any?>?
        get() = classImports.stream().map(Class::getPackage).collect(Collectors.toCollection { LinkedHashSet() })

    /**
     * Gets the set of classes from the [.getMethodImports].
     */
    val methodClasses: Set<Any?>?
        get() = methodImports.stream().map(Method::getDeclaringClass)
            .collect(Collectors.toCollection { LinkedHashSet() })

    /**
     * Gets the set of classes from the [.getEnumImports].
     */
    val enumClasses: Set<Any?>?
        get() = enumImports.stream().map(Enum::getDeclaringClass).collect(Collectors.toCollection { LinkedHashSet() })

    /**
     * Gets the set of fields from the [.getFieldImports].
     */
    val fieldClasses: Set<Any?>?
        get() = fieldImports.stream().map(Field::getDeclaringClass).collect(Collectors.toCollection { LinkedHashSet() })
}