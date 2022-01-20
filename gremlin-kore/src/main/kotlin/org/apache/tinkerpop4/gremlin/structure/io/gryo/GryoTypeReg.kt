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

import org.apache.commons.lang3.builder.ToStringBuilder
import org.apache.tinkerpop4.gremlin.structure.io.gryo.kryoshim.SerializerShim
import org.apache.tinkerpop4.gremlin.structure.io.gryo.kryoshim.shaded.ShadedSerializerAdapter
import org.apache.tinkerpop.shaded.kryo.Kryo
import org.apache.tinkerpop.shaded.kryo.Serializer
import java.util.function.Function

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
internal class GryoTypeReg<T> private constructor(
    clazz: Class<T>?,
    shadedSerializer: Serializer<T>,
    serializerShim: SerializerShim<T>,
    functionOfShadedKryo: Function<Kryo, Serializer>,
    id: Int
) : TypeRegistration<T> {
    private val clazz: Class<T>
    private val shadedSerializer: Serializer<T>?
    private val serializerShim: SerializerShim<T>?
    private val functionOfShadedKryo: Function<Kryo, Serializer>?

    @get:Override
    val id: Int

    init {
        if (null == clazz) throw IllegalArgumentException("clazz cannot be null")
        this.clazz = clazz
        this.shadedSerializer = shadedSerializer
        this.serializerShim = serializerShim
        this.functionOfShadedKryo = functionOfShadedKryo
        this.id = id
        var serializerCount = 0
        if (null != this.shadedSerializer) serializerCount++
        if (null != this.serializerShim) serializerCount++
        if (null != this.functionOfShadedKryo) serializerCount++
        if (1 < serializerCount) {
            val msg: String = String.format(
                "GryoTypeReg accepts at most one kind of serializer, but multiple " +
                        "serializers were supplied for class %s (id %s).  " +
                        "Shaded serializer: %s.  Shim serializer: %s.  Shaded serializer function: %s.",
                this.clazz.getCanonicalName(), id,
                this.shadedSerializer, this.serializerShim, this.functionOfShadedKryo
            )
            throw IllegalArgumentException(msg)
        }
    }

    @Override
    fun getShadedSerializer(): Serializer<T>? {
        return shadedSerializer
    }

    @Override
    fun getSerializerShim(): SerializerShim<T>? {
        return serializerShim
    }

    @Override
    fun getFunctionOfShadedKryo(): Function<Kryo, Serializer>? {
        return functionOfShadedKryo
    }

    @get:Override
    val targetClass: Class<T>
        get() = clazz

    @Override
    fun registerWith(kryo: Kryo): Kryo {
        if (null != functionOfShadedKryo) kryo.register(
            clazz,
            functionOfShadedKryo.apply(kryo),
            id
        ) else if (null != shadedSerializer) kryo.register(
            clazz,
            shadedSerializer,
            id
        ) else if (null != serializerShim) kryo.register(clazz, ShadedSerializerAdapter(serializerShim), id) else {
            kryo.register(clazz, kryo.getDefaultSerializer(clazz), id)
            // Suprisingly, the preceding call is not equivalent to
            //   kryo.register(clazz, id);
        }
        return kryo
    }

    @Override
    override fun toString(): String {
        return ToStringBuilder(this)
            .append("targetClass", clazz)
            .append("id", id)
            .append("shadedSerializer", shadedSerializer)
            .append("serializerShim", serializerShim)
            .append("functionOfShadedKryo", functionOfShadedKryo)
            .toString()
    }

    companion object {
        fun <T> of(clazz: Class<T>?, id: Int): GryoTypeReg<T> {
            return GryoTypeReg<Any>(clazz, null, null, null, id)
        }

        fun <T> of(clazz: Class<T>?, id: Int, shadedSerializer: Serializer<T>): GryoTypeReg<T> {
            return GryoTypeReg<Any>(clazz, shadedSerializer, null, null, id)
        }

        fun <T> of(clazz: Class<T>?, id: Int, serializerShim: SerializerShim<T>): GryoTypeReg<T> {
            return GryoTypeReg<Any>(clazz, null, serializerShim, null, id)
        }

        fun <T> of(clazz: Class?, id: Int, fct: Function<Kryo, Serializer>): GryoTypeReg<T> {
            return GryoTypeReg(clazz, null, null, fct, id)
        }
    }
}