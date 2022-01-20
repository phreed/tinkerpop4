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

/**
 * Simple implementation of the [ImportCustomizer] which allows direct setting of all the different import types.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class DefaultImportCustomizer private constructor(builder: Builder) : ImportCustomizer {
    private val classImports: Set<Class>
    private val methodImports: Set<Method>
    private val enumImports: Set<Enum>
    private val fieldImports: Set<Field>

    init {
        classImports = builder.classImports
        methodImports = builder.methodImports
        enumImports = builder.enumImports
        fieldImports = builder.fieldImports
    }

    @Override
    fun getClassImports(): Set<Class> {
        return Collections.unmodifiableSet(classImports)
    }

    @Override
    fun getMethodImports(): Set<Method> {
        return Collections.unmodifiableSet(methodImports)
    }

    @Override
    fun getEnumImports(): Set<Enum> {
        return Collections.unmodifiableSet(enumImports)
    }

    @Override
    fun getFieldImports(): Set<Field> {
        return Collections.unmodifiableSet(fieldImports)
    }

    class Builder private constructor() {
        private val classImports: Set<Class> = LinkedHashSet()
        private val methodImports: Set<Method> = LinkedHashSet()
        private val enumImports: Set<Enum> = LinkedHashSet()
        private val fieldImports: Set<Field> = LinkedHashSet()

        /**
         * Adds classes that will be imported to the `ScriptEngine`.
         */
        fun addClassImports(vararg clazz: Class?): Builder {
            classImports.addAll(Arrays.asList(clazz))
            return this
        }

        /**
         * Overload to [.addClassImports].
         */
        fun addClassImports(classes: Collection<Class?>?): Builder {
            classImports.addAll(classes)
            return this
        }

        /**
         * Adds methods that are meant to be imported statically to the engine. When adding methods be sure that
         * the classes of those methods are added to the [.addClassImports] or
         * [.addClassImports]. If they are not added then the certain `ScriptEngine` instances
         * may have problems importing the methods (e.g. gremlin-python).
         */
        fun addMethodImports(vararg method: Method?): Builder {
            methodImports.addAll(Arrays.asList(method))
            return this
        }

        /**
         * Overload to [.addMethodImports].
         */
        fun addMethodImports(methods: Collection<Method?>?): Builder {
            methodImports.addAll(methods)
            return this
        }

        /**
         * Adds fields that are meant to be imported statically to the engine. When adding fields be sure that
         * the classes of those fields are added to the [.addClassImports] or
         * [.addClassImports]. If they are not added then the certain `ScriptEngine` instances
         * may have problems importing the methods (e.g. gremlin-python).
         */
        fun addFieldImports(vararg field: Field?): Builder {
            fieldImports.addAll(Arrays.asList(field))
            return this
        }

        /**
         * Overload to [.addFieldImports].
         */
        fun addFieldImports(fields: Collection<Field?>?): Builder {
            fieldImports.addAll(fields)
            return this
        }

        /**
         * Adds methods that are meant to be imported statically to the engine. When adding methods be sure that
         * the classes of those methods are added to the [.addClassImports] or
         * [.addClassImports]. If they are not added then the certain `ScriptEngine` instances
         * may have problems importing the methods (e.g. gremlin-python).
         */
        fun addEnumImports(vararg e: Enum?): Builder {
            enumImports.addAll(Arrays.asList(e))
            return this
        }

        /**
         * Overload to [.addEnumImports].
         */
        fun addEnumImports(enums: Collection<Enum?>?): Builder {
            enumImports.addAll(enums)
            return this
        }

        fun create(): DefaultImportCustomizer {
            return DefaultImportCustomizer(this)
        }
    }

    companion object {
        fun build(): Builder {
            return Builder()
        }
    }
}