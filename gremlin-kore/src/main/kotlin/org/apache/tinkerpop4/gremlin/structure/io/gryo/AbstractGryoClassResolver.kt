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
package org.apache.tinkerpop4.gremlin.structure.io.gryo

import org.apache.tinkerpop.shaded.kryo.ClassResolver

/**
 * This mapper implementation of the `ClassResolver` helps ensure that all Vertex and Edge concrete classes
 * get properly serialized and deserialized by stripping them of their concrete class name so that they are treated
 * generically. See the [.getRegistration] method for the core of this logic.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
abstract class AbstractGryoClassResolver : ClassResolver {
    protected var kryo: Kryo? = null
    protected val idToRegistration: IntMap<Registration> = IntMap()
    protected val classToRegistration: ObjectMap<Class, Registration> = ObjectMap()
    protected var classToNameId: IdentityObjectIntMap<Class>? = null
    protected var nameIdToClass: IntMap<Class>? = null
    protected var nameToClass: ObjectMap<String, Class>? = null
    protected var nextNameId = 0
    private var memoizedClassId = -1
    private var memoizedClassIdValue: Registration? = null
    private var memoizedClass: Class? = null
    private var memoizedClassValue: Registration? = null
    @Override
    fun setKryo(kryo: Kryo?) {
        this.kryo = kryo
    }

    @Override
    fun register(registration: Registration?): Registration {
        if (null == registration) throw IllegalArgumentException("Registration cannot be null.")
        if (registration.getId() !== NAME) idToRegistration.put(registration.getId(), registration)
        classToRegistration.put(registration.getType(), registration)
        if (registration.getType().isPrimitive()) classToRegistration.put(
            getWrapperClass(registration.getType()),
            registration
        )
        return registration
    }

    @Override
    fun registerImplicit(type: Class?): Registration {
        return register(Registration(type, kryo.getDefaultSerializer(type), NAME))
    }

    /**
     * Called from [.getRegistration] to determine the actual type.
     */
    abstract fun coerceType(clazz: Class?): Class
    @Override
    fun getRegistration(clazz: Class?): Registration? {
        val type: Class = coerceType(clazz)
        if (type === memoizedClass) return memoizedClassValue
        val registration: Registration = classToRegistration.get(type)
        if (registration != null) {
            memoizedClass = type
            memoizedClassValue = registration
        }
        return registration
    }

    @Override
    fun getRegistration(classID: Int): Registration {
        return idToRegistration.get(classID)
    }

    @Override
    fun writeClass(output: Output, type: Class?): Registration? {
        if (null == type) {
            output.writeVarInt(Kryo.NULL, true)
            return null
        }
        val registration: Registration = kryo.getRegistration(type)
        if (registration.getId() === NAME) writeName(output, type) else output.writeVarInt(
            registration.getId() + 2,
            true
        )
        return registration
    }

    protected fun writeName(output: Output, type: Class) {
        output.writeVarInt(NAME + 2, true)
        if (classToNameId != null) {
            val nameId: Int = classToNameId.get(type, -1)
            if (nameId != -1) {
                output.writeVarInt(nameId, true)
                return
            }
        }
        // Only write the class name the first time encountered in object graph.
        val nameId = nextNameId++
        if (classToNameId == null) classToNameId = IdentityObjectIntMap()
        classToNameId.put(type, nameId)
        output.writeVarInt(nameId, true)
        output.writeString(type.getName())
    }

    @Override
    fun readClass(input: Input): Registration? {
        val classID: Int = input.readVarInt(true)
        when (classID) {
            Kryo.NULL -> return null
            NAME + 2 -> return readName(input)
        }
        if (classID == memoizedClassId) return memoizedClassIdValue
        val registration: Registration = idToRegistration.get(classID - 2)
            ?: throw KryoException("Encountered unregistered class ID: " + (classID - 2))
        memoizedClassId = classID
        memoizedClassIdValue = registration
        return registration
    }

    protected fun readName(input: Input): Registration {
        val nameId: Int = input.readVarInt(true)
        if (nameIdToClass == null) nameIdToClass = IntMap()
        var type: Class? = nameIdToClass.get(nameId)
        if (type == null) {
            // Only read the class name the first time encountered in object graph.
            val className: String = input.readString()
            type = getTypeByName(className)
            if (type == null) {
                type = try {
                    Class.forName(className, false, kryo.getClassLoader())
                } catch (ex: ClassNotFoundException) {
                    throw KryoException("Unable to find class: $className", ex)
                }
                if (nameToClass == null) nameToClass = ObjectMap()
                nameToClass.put(className, type)
            }
            nameIdToClass.put(nameId, type)
        }
        return kryo.getRegistration(type)
    }

    protected fun getTypeByName(className: String?): Class<*>? {
        return if (nameToClass != null) nameToClass.get(className) else null
    }

    @Override
    fun reset() {
        if (!kryo.isRegistrationRequired()) {
            if (classToNameId != null) classToNameId.clear()
            if (nameIdToClass != null) nameIdToClass.clear()
            nextNameId = 0
        }
    }

    companion object {
        const val NAME: Byte = -1
    }
}