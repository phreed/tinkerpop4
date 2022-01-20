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
 * A module that allows custom class, static method and enum imports (i.e. those that are statically defined by a
 * module within itself). A user might utilize this class to supply their own imports. This module is not specific
 * to any [GremlinScriptEngine] - the imports are supplied to all engines. This [GremlinPlugin] is not
 * enabled for the `ServiceLoader`. It is designed to be instantiated manually.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class ImportGremlinPlugin private constructor(builder: Builder?) : AbstractGremlinPlugin(
    NAME, builder!!.appliesTo, DefaultImportCustomizer.build()
        .addClassImports(builder!!.classImports)
        .addEnumImports(builder.enumImports)
        .addMethodImports(builder.methodImports)
        .addFieldImports(builder.fieldImports).create()
) {
    class Builder {
        val fieldImports: Set<Field?>? = HashSet()
        val classImports: Set<Class?>? = HashSet()
        val methodImports: Set<Method?>? = HashSet()
        val enumImports: Set<Enum?>? = HashSet()
        private val appliesTo: Set<String?>? = HashSet()

        /**
         * The name of the [GremlinScriptEngine] that this module will apply to. Setting no values here will
         * make the module available to all the engines.
         */
        fun appliesTo(scriptEngineName: Collection<String?>?): Builder? {
            appliesTo.addAll(scriptEngineName)
            return this
        }

        fun classImports(vararg classes: Class<*>?): Builder? {
            classImports.addAll(Arrays.asList(classes))
            return this
        }

        fun classImports(classes: Collection<String?>?): Builder? {
            for (clazz in classes!!) {
                try {
                    classImports.add(Class.forName(clazz))
                } catch (ex: Exception) {
                    throw IllegalStateException(ex)
                }
            }
            return this
        }

        fun methodImports(methods: Collection<String?>?): Builder? {
            for (method in methods!!) {
                try {
                    if (method.endsWith("#*")) {
                        val classString: String = method.substring(0, method!!.length() - 2)
                        val clazz: Class<*> = Class.forName(classString)
                        methodImports.addAll(allStaticMethods(clazz))
                    } else {
                        val matcher: Matcher = METHOD_PATTERN.matcher(method)
                        if (!matcher.matches()) throw IllegalArgumentException(
                            String.format(
                                "Could not read method descriptor - check format of: %s",
                                method
                            )
                        )
                        val classString: String = matcher.group(1)
                        val methodString: String = matcher.group(2)
                        val argString: String = matcher.group(3)
                        val clazz: Class<*> = Class.forName(classString)
                        methodImports.add(clazz.getMethod(methodString, parse(argString)))
                    }
                } catch (iae: IllegalArgumentException) {
                    throw iae
                } catch (ex: Exception) {
                    throw IllegalStateException(ex)
                }
            }
            return this
        }

        fun methodImports(vararg methods: Method?): Builder? {
            methodImports.addAll(Arrays.asList(methods))
            return this
        }

        fun enumImports(enums: Collection<String?>?): Builder? {
            for (enumItem in enums!!) {
                try {
                    if (enumItem.endsWith("#*")) {
                        val classString: String = enumItem.substring(0, enumItem!!.length() - 2)
                        val clazz: Class<*> = Class.forName(classString)
                        enumImports.addAll(allEnums(clazz))
                    } else {
                        val matcher: Matcher = ENUM_PATTERN.matcher(enumItem)
                        if (!matcher.matches()) throw IllegalArgumentException(
                            String.format(
                                "Could not read enum descriptor - check format of: %s",
                                enumItem
                            )
                        )
                        val classString: String = matcher.group(1)
                        val enumValString: String = matcher.group(2)
                        val clazz: Class<*> = Class.forName(classString)
                        Stream.of(clazz.getEnumConstants())
                            .filter { e -> (e as Enum).name().equals(enumValString) }
                            .findFirst().ifPresent { e -> enumImports.add(e as Enum) }
                    }
                } catch (iae: IllegalArgumentException) {
                    throw iae
                } catch (ex: Exception) {
                    throw IllegalStateException(ex)
                }
            }
            return this
        }

        fun enumImports(vararg enums: Enum?): Builder? {
            enumImports.addAll(Arrays.asList(enums))
            return this
        }

        fun fieldsImports(fields: Collection<String?>?): Builder? {
            for (fieldItem in fields!!) {
                try {
                    if (fieldItem.endsWith("#*")) {
                        val classString: String = fieldItem.substring(0, fieldItem!!.length() - 2)
                        val clazz: Class<*> = Class.forName(classString)
                        fieldImports.addAll(allStaticFields(clazz))
                    } else {
                        val matcher: Matcher = ENUM_PATTERN.matcher(fieldItem)
                        if (!matcher.matches()) throw IllegalArgumentException(
                            String.format(
                                "Could not read field descriptor - check format of: %s",
                                fieldItem
                            )
                        )
                        val classString: String = matcher.group(1)
                        val fieldName: String = matcher.group(2)
                        val clazz: Class<*> = Class.forName(classString)
                        fieldImports.add(clazz.getField(fieldName))
                    }
                } catch (iae: IllegalArgumentException) {
                    throw iae
                } catch (ex: Exception) {
                    throw IllegalStateException(ex)
                }
            }
            return this
        }

        fun fieldsImports(vararg fields: Field?): Builder? {
            fieldImports.addAll(Arrays.asList(fields))
            return this
        }

        fun create(): ImportGremlinPlugin? {
            if (enumImports!!.isEmpty() && classImports!!.isEmpty() && methodImports!!.isEmpty()
                && fieldImports!!.isEmpty()
            ) throw IllegalStateException("At least one import must be specified")
            return ImportGremlinPlugin(this)
        }

        companion object {
            private val METHOD_PATTERN: Pattern? = Pattern.compile("(.*)#(.*)\\((.*)\\)")
            private val ENUM_PATTERN: Pattern? = Pattern.compile("(.*)#(.*)")
            private fun allEnums(clazz: Class<*>?): List<Enum?>? {
                return Stream.of(clazz.getEnumConstants()).map { e -> e }.collect(Collectors.toList())
            }

            private fun allStaticMethods(clazz: Class<*>?): List<Method?>? {
                return Stream.of(clazz.getMethods()).filter { m -> Modifier.isStatic(m.getModifiers()) }
                    .collect(Collectors.toList())
            }

            private fun allStaticFields(clazz: Class<*>?): List<Field?>? {
                return Stream.of(clazz.getFields()).filter { f -> Modifier.isStatic(f.getModifiers()) }
                    .collect(Collectors.toList())
            }

            private fun parse(argString: String?): Array<Class<*>?>? {
                if (null == argString || argString.isEmpty()) return arrayOfNulls<Class<*>?>(0)
                val args: List<String?> = Stream.of(argString.split(",")).map(String::trim).collect(Collectors.toList())
                val classes: Array<Class<*>?> = arrayOfNulls<Class<*>?>(args.size())
                for (ix in 0 until args.size()) {
                    try {
                        classes[ix] = Class.forName(args[ix])
                    } catch (ex: Exception) {
                        throw IllegalStateException(ex)
                    }
                }
                return classes
            }
        }
    }

    companion object {
        private val NAME: String? = "tinkerpop.import"
        fun build(): Builder? {
            return Builder()
        }
    }
}