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

import org.apache.tinkerpop4.gremlin.structure.io.gryo.kryoshim.InputShim
import org.apache.tinkerpop4.gremlin.structure.io.gryo.kryoshim.KryoShim
import org.apache.tinkerpop4.gremlin.structure.io.gryo.kryoshim.OutputShim
import org.apache.tinkerpop4.gremlin.structure.io.gryo.kryoshim.SerializerShim
import org.apache.tinkerpop4.gremlin.util.function.HashSetSupplier
import org.apache.tinkerpop.shaded.kryo.Kryo
import org.apache.tinkerpop.shaded.kryo.Serializer
import org.apache.tinkerpop.shaded.kryo.io.Input
import org.apache.tinkerpop.shaded.kryo.io.Output
import org.javatuples.Pair
import org.javatuples.Triplet
import java.net.InetAddress
import java.net.URI
import java.nio.ByteBuffer
import java.util.AbstractMap
import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.LinkedHashMap
import java.util.List
import java.util.Map
import java.util.UUID

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 * @author Daniel Kuppitz (http://gremlin.guru)
 */
internal class UtilSerializers private constructor() {
    /**
     * Serializer for `List` instances produced by `Arrays.asList()`.
     */
    class ArraysAsListSerializer : SerializerShim<List?> {
        @Override
        fun <O : OutputShim?> write(kryo: KryoShim<*, O>, output: O, list: List?) {
            val l: List = ArrayList(list)
            kryo.writeObject(output, l)
        }

        @Override
        fun <I : InputShim?> read(kryo: KryoShim<I, *>, input: I, clazz: Class<List?>?): List {
            return kryo.readObject(input, ArrayList::class.java)
        }
    }

    class ByteBufferSerializer : SerializerShim<ByteBuffer?> {
        @Override
        fun <O : OutputShim?> write(kryo: KryoShim<*, O>?, output: O, bb: ByteBuffer) {
            val b: ByteArray = bb.array()
            val arrayOffset: Int = bb.arrayOffset()
            Arrays.copyOfRange(b, arrayOffset + bb.position(), arrayOffset + bb.limit())
            output.writeInt(b.size)
            output.writeBytes(b, 0, b.size)
        }

        @Override
        fun <I : InputShim?> read(kryo: KryoShim<I, *>?, input: I, clazz: Class<ByteBuffer?>?): ByteBuffer {
            val len: Int = input.readInt()
            val b: ByteArray = input.readBytes(len)
            val bb: ByteBuffer = ByteBuffer.allocate(len)
            bb.put(b)
            return bb
        }
    }

    class ClassSerializer : SerializerShim<Class?> {
        @Override
        fun <O : OutputShim?> write(kryo: KryoShim<*, O>?, output: O, `object`: Class) {
            output.writeString(`object`.getName())
        }

        @Override
        fun <I : InputShim?> read(kryo: KryoShim<I, *>?, input: I, clazz: Class<Class?>?): Class {
            val name: String = input.readString()
            return try {
                Class.forName(name)
            } catch (ex: Exception) {
                throw RuntimeException(ex)
            }
        }
    }

    class ClassArraySerializer : SerializerShim<Array<Class?>?> {
        @Override
        fun <O : OutputShim?> write(kryo: KryoShim<*, O>?, output: O, `object`: Array<Class?>) {
            output.writeInt(`object`.size)
            for (clazz in `object`) {
                output.writeString(clazz.getName())
            }
        }

        @Override
        fun <I : InputShim?> read(kryo: KryoShim<I, *>?, input: I, clazz: Class<Array<Class?>?>?): Array<Class?> {
            val size: Int = input.readInt()
            val clazzes: Array<Class?> = arrayOfNulls<Class>(size)
            for (i in 0 until size) {
                try {
                    clazzes[i] = Class.forName(input.readString())
                } catch (ex: Exception) {
                    throw RuntimeException(ex)
                }
            }
            return clazzes
        }
    }

    class InetAddressSerializer : SerializerShim<InetAddress?> {
        @Override
        fun <O : OutputShim?> write(kryo: KryoShim<*, O>?, output: O, addy: InetAddress) {
            val str: String = addy.toString().trim()
            val slash: Int = str.indexOf('/')
            if (slash >= 0) {
                if (slash == 0) {
                    output.writeString(str.substring(1))
                } else {
                    output.writeString(str.substring(0, slash))
                }
            }
        }

        @Override
        fun <I : InputShim?> read(kryo: KryoShim<I, *>?, input: I, clazz: Class<InetAddress?>?): InetAddress {
            return try {
                InetAddress.getByName(input.readString())
            } catch (ex: Exception) {
                throw RuntimeException(ex)
            }
        }
    }

    class HashSetSupplierSerializer : SerializerShim<HashSetSupplier?> {
        @Override
        fun <O : OutputShim?> write(kryo: KryoShim<*, O>?, output: O, hashSetSupplier: HashSetSupplier?) {
        }

        @Override
        fun <I : InputShim?> read(kryo: KryoShim<I, *>?, input: I, clazz: Class<HashSetSupplier?>?): HashSetSupplier {
            return HashSetSupplier.instance()
        }
    }

    internal class UUIDSerializer : SerializerShim<UUID?> {
        @Override
        fun <O : OutputShim?> write(kryo: KryoShim<*, O>?, output: O, uuid: UUID) {
            output.writeLong(uuid.getMostSignificantBits())
            output.writeLong(uuid.getLeastSignificantBits())
        }

        @Override
        fun <I : InputShim?> read(kryo: KryoShim<I, *>?, input: I, uuidClass: Class<UUID?>?): UUID {
            return UUID(input.readLong(), input.readLong())
        }

        @get:Override
        val isImmutable: Boolean
            get() = true
    }

    internal class URISerializer : SerializerShim<URI?> {
        @Override
        fun <O : OutputShim?> write(kryo: KryoShim<*, O>?, output: O, uri: URI) {
            output.writeString(uri.toString())
        }

        @Override
        fun <I : InputShim?> read(kryo: KryoShim<I, *>?, input: I, uriClass: Class<URI?>?): URI {
            return URI.create(input.readString())
        }

        @get:Override
        val isImmutable: Boolean
            get() = true
    }

    internal class PairSerializer : SerializerShim<Pair?> {
        @Override
        fun <O : OutputShim?> write(kryo: KryoShim<*, O>, output: O, pair: Pair) {
            kryo.writeClassAndObject(output, pair.getValue0())
            kryo.writeClassAndObject(output, pair.getValue1())
        }

        @Override
        fun <I : InputShim?> read(kryo: KryoShim<I, *>, input: I, pairClass: Class<Pair?>?): Pair {
            return Pair.with(kryo.readClassAndObject(input), kryo.readClassAndObject(input))
        }
    }

    internal class TripletSerializer : SerializerShim<Triplet?> {
        @Override
        fun <O : OutputShim?> write(kryo: KryoShim<*, O>, output: O, triplet: Triplet) {
            kryo.writeClassAndObject(output, triplet.getValue0())
            kryo.writeClassAndObject(output, triplet.getValue1())
            kryo.writeClassAndObject(output, triplet.getValue2())
        }

        @Override
        fun <I : InputShim?> read(kryo: KryoShim<I, *>, input: I, tripletClass: Class<Triplet?>?): Triplet {
            return Triplet.with(
                kryo.readClassAndObject(input),
                kryo.readClassAndObject(input),
                kryo.readClassAndObject(input)
            )
        }
    }

    internal class EntrySerializer : Serializer<Map.Entry?>() {
        @Override
        fun write(kryo: Kryo, output: Output?, entry: Map.Entry) {
            kryo.writeClassAndObject(output, entry.getKey())
            kryo.writeClassAndObject(output, entry.getValue())
        }

        @Override
        fun read(kryo: Kryo, input: Input?, entryClass: Class<Map.Entry?>?): Map.Entry {
            return SimpleEntry(kryo.readClassAndObject(input), kryo.readClassAndObject(input))
        }
    }

    /**
     * Serializer for `List` instances produced by `Arrays.asList()`.
     */
    internal class SynchronizedMapSerializer : SerializerShim<Map?> {
        @Override
        fun <O : OutputShim?> write(kryo: KryoShim<*, O>, output: O, map: Map) {
            val m: Map = LinkedHashMap()
            map.forEach(m::put)
            kryo.writeObject(output, m)
        }

        @Override
        fun <I : InputShim?> read(kryo: KryoShim<I, *>, input: I, clazz: Class<Map?>?): Map {
            return Collections.synchronizedMap(kryo.readObject(input, LinkedHashMap::class.java))
        }
    }
}