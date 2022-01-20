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

import org.apache.tinkerpop4.gremlin.structure.io.IoRegistry

/**
 * Gryo objects are somewhat expensive to construct (given the dependency on Kryo), therefore this pool helps re-use
 * those objects.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class GryoPool
/**
 * Used by `GryoPool.Builder`.
 */
private constructor() {
    enum class Type {
        READER, WRITER, READER_WRITER
    }

    private var gryoReaders: Queue<GryoReader>? = null
    private var gryoWriters: Queue<GryoWriter>? = null
    private var kryos: Queue<Kryo>? = null
    private var mapper: GryoMapper? = null
    fun getMapper(): GryoMapper? {
        return mapper
    }

    fun takeKryo(): Kryo {
        val kryo: Kryo = kryos.poll()
        return if (null == kryo) mapper.createMapper() else kryo
    }

    fun takeReader(): GryoReader {
        val reader: GryoReader = gryoReaders.poll()
        return if (null == reader) GryoReader.build().mapper(mapper).create() else reader
    }

    fun takeWriter(): GryoWriter {
        val writer: GryoWriter = gryoWriters.poll()
        return if (null == writer) GryoWriter.build().mapper(mapper).create() else writer
    }

    fun offerKryo(kryo: Kryo?) {
        kryos.offer(kryo)
    }

    fun offerReader(gryoReader: GryoReader?) {
        gryoReaders.offer(gryoReader)
    }

    fun offerWriter(gryoWriter: GryoWriter?) {
        gryoWriters.offer(gryoWriter)
    }

    fun <A> readWithKryo(kryoFunction: Function<Kryo?, A>): A {
        val kryo: Kryo = takeKryo()
        val a: A = kryoFunction.apply(kryo)
        offerKryo(kryo)
        return a
    }

    fun writeWithKryo(kryoConsumer: Consumer<Kryo?>) {
        val kryo: Kryo = takeKryo()
        kryoConsumer.accept(kryo)
        offerKryo(kryo)
    }

    fun <A> doWithReader(readerFunction: Function<GryoReader?, A>): A {
        val gryoReader: GryoReader = takeReader()
        val a: A = readerFunction.apply(gryoReader)
        offerReader(gryoReader)
        return a
    }

    fun doWithWriter(writerFunction: Consumer<GryoWriter?>) {
        val gryoWriter: GryoWriter = takeWriter()
        writerFunction.accept(gryoWriter)
        offerWriter(gryoWriter)
    }

    private fun createPool(poolSize: Int, type: Type, gryoMapper: GryoMapper) {
        mapper = gryoMapper
        if (type.equals(Type.READER) || type.equals(Type.READER_WRITER)) {
            gryoReaders = LinkedBlockingQueue(poolSize)
            for (i in 0 until poolSize) {
                gryoReaders.add(GryoReader.build().mapper(gryoMapper).create())
            }
        }
        if (type.equals(Type.WRITER) || type.equals(Type.READER_WRITER)) {
            gryoWriters = LinkedBlockingQueue(poolSize)
            for (i in 0 until poolSize) {
                gryoWriters.add(GryoWriter.build().mapper(gryoMapper).create())
            }
        }
        kryos = LinkedBlockingQueue(poolSize)
        for (i in 0 until poolSize) {
            kryos.add(gryoMapper.createMapper())
        }
    }

    ////
    class Builder {
        private var poolSize = 256
        private val ioRegistries: List<IoRegistry>? = ArrayList()
        private var type = Type.READER_WRITER
        private var gryoMapperConsumer: Consumer<GryoMapper.Builder>? = null
        private var version: GryoVersion = GryoVersion.V1_0

        /**
         * Set the version of Gryo to use for this pool.
         */
        fun version(version: GryoVersion): Builder {
            this.version = version
            return this
        }

        /**
         * The `IoRegistry` class names to use for the `GryoPool`
         *
         * @param ioRegistryClassNames a list of class names
         * @return the update builder
         */
        fun ioRegistries(ioRegistryClassNames: List<Object?>?): Builder {
            ioRegistries.addAll(IoRegistryHelper.createRegistries(ioRegistryClassNames))
            return this
        }

        /**
         * The `IoRegistry` class name to use for the `GryoPool`
         *
         * @param ioRegistryClassName a class name
         * @return the update builder
         */
        fun ioRegistry(ioRegistryClassName: Object?): Builder {
            ioRegistries.addAll(IoRegistryHelper.createRegistries(Collections.singletonList(ioRegistryClassName)))
            return this
        }

        /**
         * The size of the `GryoPool`. The size can not be changed once created.
         *
         * @param poolSize the pool size
         * @return the updated builder
         */
        fun poolSize(poolSize: Int): Builder {
            this.poolSize = poolSize
            return this
        }

        /**
         * The type of `GryoPool` to support -- see `Type`
         *
         * @param type the pool type
         * @return the updated builder
         */
        fun type(type: Type): Builder {
            this.type = type
            return this
        }

        /**
         * A consumer to update the `GryoMapper.Builder` once constructed.
         *
         * @param gryoMapperConsumer the `GryoMapper.Builder` consumer
         * @return the updated builder
         */
        fun initializeMapper(gryoMapperConsumer: Consumer<GryoMapper.Builder?>): Builder {
            this.gryoMapperConsumer = gryoMapperConsumer
            return this
        }

        /**
         * Create the `GryoPool` from this builder.
         *
         * @return the new pool
         */
        fun create(): GryoPool {
            val mapper: GryoMapper.Builder = GryoMapper.build().version(version)
            val gryoPool = GryoPool()
            if (null != ioRegistries) ioRegistries.forEach(mapper::addRegistry)
            if (null != gryoMapperConsumer) gryoMapperConsumer.accept(mapper)
            gryoPool.createPool(poolSize, type, mapper.create())
            return gryoPool
        }
    }

    companion object {
        const val CONFIG_IO_GRYO_POOL_SIZE = "gremlin.io.gryo.poolSize"
        const val CONFIG_IO_GRYO_VERSION = "gremlin.io.gryo.version"
        const val CONFIG_IO_GRYO_POOL_SIZE_DEFAULT = 256
        val CONFIG_IO_GRYO_POOL_VERSION_DEFAULT: GryoVersion = GryoVersion.V3_0
        fun build(): Builder {
            return Builder()
        }
    }
}