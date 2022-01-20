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
package org.apache.tinkerpop4.gremlin.structure.io.binary.types

import org.apache.tinkerpop4.gremlin.structure.io.binary.DataType

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class PSerializer<T : P?>(typeOfP: DataType?, classOfP: Class<T>) : SimpleTypeSerializer<T>(typeOfP) {
    private val classOfP: Class<T>
    private val methods: ConcurrentHashMap<PFunctionId, CheckedFunction<*, *>> = ConcurrentHashMap()

    init {
        this.classOfP = classOfP
    }

    @Override
    @Throws(IOException::class)
    protected fun readValue(buffer: Buffer?, context: GraphBinaryReader): T {
        val predicateName: String = context.readValue(buffer, String::class.java, false)
        val length: Int = context.readValue(buffer, Integer::class.java, false)
        val args: Array<Object?> = arrayOfNulls<Object>(length)
        val argumentClasses: Array<Class<*>?> = arrayOfNulls<Class>(length)
        for (i in 0 until length) {
            args[i] = context.read(buffer)
            argumentClasses[i] = if (null == args[i]) Object::class.java else args[i].getClass()
        }
        if ("and".equals(predicateName)) return (args[0] as P?).and(args[1] as P?) else if ("or".equals(predicateName)) return (args[0] as P?).or(
            args[1] as P?
        ) else if ("not".equals(predicateName)) return P.not(args[0] as P?)
        val f: CheckedFunction<Array<Object?>, T> = getMethod(predicateName, argumentClasses)
        return try {
            f.apply(args)
        } catch (ex: Exception) {
            throw IOException(String.format("Can't deserialize value into the predicate: '%s'", predicateName), ex)
        }
    }

    @Throws(IOException::class)
    private fun getMethod(predicateName: String, argumentClasses: Array<Class<*>?>): CheckedFunction<*, *> {
        val id = PFunctionId(predicateName, argumentClasses)
        var result: CheckedFunction<Array<Object?>?, T>? = methods.get(id)
        if (result == null) {
            var collectionType = false
            var m: Method
            try {
                // do a direct lookup
                m = classOfP.getMethod(predicateName, argumentClasses)
            } catch (ex0: NoSuchMethodException) {
                // then try collection types
                try {
                    m = classOfP.getMethod(predicateName, Collection::class.java)
                    collectionType = true
                } catch (ex1: NoSuchMethodException) {
                    // finally go for the generics
                    m = try {
                        classOfP.getMethod(predicateName, Object::class.java)
                    } catch (ex2: NoSuchMethodException) {
                        // finally go for the generics
                        try {
                            classOfP.getMethod(predicateName, Object::class.java, Object::class.java)
                        } catch (ex3: NoSuchMethodException) {
                            throw IOException(String.format("Can't find predicate method: '%s'", predicateName), ex2)
                        }
                    }
                }
            }
            val finalMethod: Method = m
            try {
                if (Modifier.isStatic(m.getModifiers())) {
                    if (collectionType) {
                        result = CheckedFunction<Array<Object>, T> { args: Array<Object?>? ->
                            finalMethod.invoke(
                                null,
                                Arrays.asList(args)
                            )
                        }
                    } else {
                        result = CheckedFunction<Array<Object>, T> { args: Array<Object?>? ->
                            finalMethod.invoke(
                                null,
                                args
                            )
                        }
                    }
                } else {
                    // try an instance method as it might be a form of ConnectiveP which means there is a P as an
                    // argument that should be used as the object of an instance method
                    if (argumentClasses.size != 2) {
                        throw IllegalStateException(
                            String.format(
                                "Could not determine the form of P for %s and %s",
                                predicateName, Arrays.asList(argumentClasses)
                            )
                        )
                    }
                    result = CheckedFunction<Array<Object>, T> { args: Array<Object?> ->
                        if (args[0] !is P || args[1] !is P) throw IllegalStateException(
                            String.format(
                                "Could not determine the form of P for %s and %s",
                                predicateName, Arrays.asList(args)
                            )
                        )
                        val firstP: P? = args[0] as P?
                        val secondP: P? = args[1] as P?
                        finalMethod.invoke(firstP, secondP)
                    }
                }
                methods.put(id, result)
            } catch (ex: Exception) {
                throw IOException(ex)
            }
        }
        return result
    }

    @Override
    @Throws(IOException::class)
    protected fun writeValue(value: T, buffer: Buffer?, context: GraphBinaryWriter) {
        // the predicate name is either a static method of P or an instance method when a type ConnectiveP
        val isConnectedP = value is ConnectiveP
        val predicateName = if (isConnectedP) if (value is AndP) "and" else "or" else value.getBiPredicate().toString()
        val args: Object = if (isConnectedP) (value as ConnectiveP<*>).getPredicates() else value.getValue()
        val argsAsList: List<Object> =
            if (args is Collection) ArrayList(args as Collection) else Collections.singletonList(args)
        val length: Int = argsAsList.size()
        context.writeValue(predicateName, buffer, false)
        context.writeValue(length, buffer, false)
        for (o in argsAsList) {
            context.write(o, buffer)
        }
    }

    @FunctionalInterface
    internal interface CheckedFunction<A, R> {
        @Throws(InvocationTargetException::class, IllegalAccessException::class)
        fun apply(t: A): R
    }

    internal inner class PFunctionId(private val predicateName: String, argumentClasses: Array<Class<*>?>) {
        private val argumentClasses: Array<Class<*>>

        init {
            this.argumentClasses = argumentClasses
        }

        @Override
        override fun equals(o: Object?): Boolean {
            if (this === o) return true
            if (o == null || getClass() !== o.getClass()) return false
            val that: PFunctionId = o
            return predicateName.equals(that.predicateName) &&
                    Arrays.equals(argumentClasses, that.argumentClasses)
        }

        @Override
        override fun hashCode(): Int {
            var result: Int = Objects.hash(predicateName)
            result = 31 * result + Arrays.hashCode(argumentClasses)
            return result
        }
    }
}