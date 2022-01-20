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

/**
 * A [Mapper] implementation for Kryo. This implementation requires that all classes to be serialized by
 * Kryo are registered to it.
 *
 *
 * [Graph] implementations providing an [IoRegistry] should register their custom classes and/or
 * serializers in one of three ways:
 *
 *
 *
 *  1. Register just the custom class with a `null` [Serializer] implementation
 *  1. Register the custom class with a [Serializer] implementation
 *  1.
 * Register the custom class with a `Function<Kryo, Serializer>` for those cases where the
 * [Serializer] requires the [Kryo] instance to get constructed.
 *
 *
 *
 *
 * For example:
 * <pre>
 * `public class MyGraphIoRegistry extends AbstractIoRegistry {
 * public MyGraphIoRegistry() {
 * register(GryoIo.class, MyGraphIdClass.class, new MyGraphIdSerializer());
 * }
 * }
` *
</pre> *
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class GryoMapper private constructor(builder: Builder) : Mapper<Kryo?> {
    private val typeRegistrations: List<TypeRegistration<*>>
    private val registrationRequired: Boolean
    private val referenceTracking: Boolean
    private val classResolver: Supplier<ClassResolver>
    private val version: GryoVersion

    init {
        typeRegistrations = builder.typeRegistrations
        version = builder.version
        validate()
        registrationRequired = builder.registrationRequired
        referenceTracking = builder.referenceTracking
        classResolver = if (null == builder.classResolver) version.getClassResolverMaker() else builder.classResolver
    }

    @Override
    fun createMapper(): Kryo {
        val kryo = Kryo(classResolver.get(), MapReferenceResolver(), DefaultStreamFactory())
        kryo.addDefaultSerializer(Map.Entry::class.java, EntrySerializer())
        kryo.setRegistrationRequired(registrationRequired)
        kryo.setReferences(referenceTracking)
        for (tr in typeRegistrations) tr.registerWith(kryo)
        return kryo
    }

    fun getVersion(): GryoVersion {
        return version
    }

    val registeredClasses: List<Any>
        get() = typeRegistrations.stream().map(TypeRegistration::getTargetClass).collect(Collectors.toList())

    fun getTypeRegistrations(): List<TypeRegistration<*>> {
        return typeRegistrations
    }

    private fun validate() {
        val duplicates: Set<Integer> = HashSet()
        val ids: Set<Integer> = HashSet()
        typeRegistrations.forEach { t -> if (!ids.contains(t.getId())) ids.add(t.getId()) else duplicates.add(t.getId()) }
        if (duplicates.size() > 0) throw IllegalStateException("There are duplicate kryo identifiers in use: $duplicates")
    }

    /**
     * A builder to construct a [GryoMapper] instance.
     */
    class Builder : Mapper.Builder<Builder?> {
        var version: GryoVersion = GryoVersion.V3_0

        /**
         * Note that the following are pre-registered boolean, Boolean, byte, Byte, char, Character, double, Double,
         * int, Integer, float, Float, long, Long, short, Short, String, void.
         */
        var typeRegistrations: List<TypeRegistration<*>> = version.cloneRegistrations()
        private val registries: List<IoRegistry> = ArrayList()

        /**
         * Starts numbering classes for Gryo serialization at 65536 to leave room for future usage by TinkerPop.
         */
        private val currentSerializationId: AtomicInteger = AtomicInteger(65536)
        var registrationRequired = true
        var referenceTracking = true
        var classResolver: Supplier<ClassResolver>? = null

        /**
         * {@inheritDoc}
         */
        @Override
        fun addRegistry(registry: IoRegistry?): Builder {
            if (null == registry) throw IllegalArgumentException("The registry cannot be null")
            registries.add(registry)
            return this
        }

        /**
         * The version of Gryo to use in the mapper. Defaults to 1.0. Calls to this method will reset values specified
         * to [.addCustom] and related overloads.
         */
        fun version(version: GryoVersion): Builder {
            this.version = version
            typeRegistrations = version.cloneRegistrations()
            return this
        }

        /**
         * Provides a custom Kryo `ClassResolver` to be supplied to a `Kryo` instance.  If this value is
         * not supplied then it will default to the `ClassResolver` of the provided [GryoVersion]. To
         * ensure compatibility with Gryo it is highly recommended that objects passed to this method extend that class.
         *
         *
         * If the `ClassResolver` implementation share state, then the [Supplier] should typically create
         * new instances when requested, as the [Supplier] will be called for each [Kryo] instance created.
         */
        fun classResolver(classResolverSupplier: Supplier<ClassResolver?>?): Builder {
            if (null == classResolverSupplier) throw IllegalArgumentException("The classResolverSupplier cannot be null")
            classResolver = classResolverSupplier
            return this
        }

        /**
         * Register custom classes to serializes with gryo using default serialization. Note that calling this method
         * for a class that is already registered will override that registration.
         */
        fun addCustom(vararg custom: Class?): Builder {
            if (custom != null && custom.size > 0) {
                for (c in custom) {
                    addOrOverrideRegistration(c, Function<Integer, TypeRegistration<T>> { id -> GryoTypeReg.of(c, id) })
                }
            }
            return this
        }

        /**
         * Register custom class to serialize with a custom serialization class. Note that calling this method for
         * a class that is already registered will override that registration.
         */
        fun addCustom(clazz: Class, serializer: Serializer?): Builder {
            addOrOverrideRegistration(clazz, Function<Integer, TypeRegistration<T>> { id -> of(clazz, id, serializer) })
            return this
        }

        /**
         * Register custom class to serialize with a custom serialization shim.
         */
        fun addCustom(clazz: Class, serializer: SerializerShim?): Builder {
            addOrOverrideRegistration(clazz, Function<Integer, TypeRegistration<T>> { id -> of(clazz, id, serializer) })
            return this
        }

        /**
         * Register a custom class to serialize with a custom serializer as returned from a [Function]. Note
         * that calling this method for a class that is already registered will override that registration.
         */
        fun addCustom(clazz: Class, functionOfKryo: Function<Kryo?, Serializer?>?): Builder {
            addOrOverrideRegistration(
                clazz,
                Function<Integer, TypeRegistration<T>> { id -> of(clazz, id, functionOfKryo) })
            return this
        }

        /**
         * When set to `true`, all classes serialized by the `Kryo` instances created from this
         * [GryoMapper] must have their classes known up front and registered appropriately through this
         * builder.  By default this value is `true`.  This approach is more efficient than setting the
         * value to `false`.
         *
         * @param registrationRequired set to `true` if the classes should be registered up front or
         * `false` otherwise
         */
        fun registrationRequired(registrationRequired: Boolean): Builder {
            this.registrationRequired = registrationRequired
            return this
        }

        /**
         * By default, each appearance of an object in the graph after the first is stored as an integer ordinal.
         * This allows multiple references to the same object and cyclic graphs to be serialized. This has a small
         * amount of overhead and can be disabled to save space if it is not needed.
         *
         * @param referenceTracking set to `true` to enable and `false` otherwise
         */
        fun referenceTracking(referenceTracking: Boolean): Builder {
            this.referenceTracking = referenceTracking
            return this
        }

        /**
         * Creates a `GryoMapper`.
         */
        fun create(): GryoMapper {
            // consult the registry if provided and inject registry entries as custom classes.
            registries.forEach { registry ->
                val serializers: List<Pair<Class, Object>> = registry.find(GryoIo::class.java)
                serializers.forEach { p ->
                    if (null == p.getValue1()) addCustom(p.getValue0()) else if (p.getValue1() is SerializerShim) addCustom(
                        p.getValue0(),
                        ShadedSerializerAdapter(p.getValue1() as SerializerShim)
                    ) else if (p.getValue1() is Serializer) addCustom(
                        p.getValue0(),
                        p.getValue1() as Serializer
                    ) else if (p.getValue1() is Function) addCustom(
                        p.getValue0(),
                        p.getValue1() as Function<Kryo?, Serializer?>
                    ) else throw IllegalStateException(
                        String.format(
                            "Unexpected value provided by %s for serializable class %s - expected a parameter in [null, %s implementation or Function<%s, %s>], but received %s",
                            registry.getClass().getSimpleName(), p.getValue0().getClass().getCanonicalName(),
                            Serializer::class.java.getName(), Kryo::class.java.getSimpleName(),
                            Serializer::class.java.getSimpleName(), p.getValue1()
                        )
                    )
                }
            }
            return GryoMapper(this)
        }

        private fun <T> addOrOverrideRegistration(
            clazz: Class<*>,
            newRegistrationBuilder: Function<Integer, TypeRegistration<T>>
        ) {
            val iter: Iterator<TypeRegistration<*>> = typeRegistrations.iterator()
            var registrationId: Integer? = null
            while (iter.hasNext()) {
                val existingRegistration: TypeRegistration<*> = iter.next()
                if (existingRegistration.getTargetClass().equals(clazz)) {
                    // when overridding a registration, use the old id
                    registrationId = existingRegistration.getId()
                    // remove the old registration (we install its override below)
                    iter.remove()
                    break
                }
            }
            if (null == registrationId) {
                // when not overridding a registration, get an id from the counter
                registrationId = currentSerializationId.getAndIncrement()
            }
            typeRegistrations.add(newRegistrationBuilder.apply(registrationId))
        }
    }

    private class GryoTypeReg<T> private constructor(
        clazz: Class<T>,
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
            private fun <T> of(clazz: Class<T>, id: Int): GryoTypeReg<T> {
                return GryoTypeReg(clazz, null, null, null, id)
            }

            private fun <T> of(clazz: Class<T>, id: Int, shadedSerializer: Serializer<T>): GryoTypeReg<T> {
                return GryoTypeReg(clazz, shadedSerializer, null, null, id)
            }

            private fun <T> of(clazz: Class<T>, id: Int, serializerShim: SerializerShim<T>): GryoTypeReg<T> {
                return GryoTypeReg(clazz, null, serializerShim, null, id)
            }

            private fun <T> of(clazz: Class, id: Int, fct: Function<Kryo, Serializer>): GryoTypeReg<T> {
                return GryoTypeReg(clazz, null, null, fct, id)
            }
        }
    }

    companion object {
        val GIO: ByteArray = "gio".getBytes()
        val HEADER: ByteArray = Arrays.copyOf(GIO, 16)
        fun build(): Builder {
            return Builder()
        }
    }
}