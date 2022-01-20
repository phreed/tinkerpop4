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
package org.apache.tinkerpop4.gremlin.structure.io

import org.apache.tinkerpop4.gremlin.structure.Graph

/**
 * Ties together the core interfaces of an IO format: [GraphReader], [GraphWriter] and [Mapper].
 * The [Builder] of an `Io` instance is supplied to [Graph.io] and the [Graph]
 * implementation can then chose to supply an [IoRegistry] to it before returning it.  An [Io]
 * implementation should use that [IoRegistry] to lookup custom serializers to use and register them to the
 * internal [Mapper] (if the format has such capability).
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
interface Io<R : ReaderBuilder?, W : WriterBuilder?, M : Mapper.Builder?> {
    /**
     * Creates a [GraphReader.ReaderBuilder] implementation . Implementers should call the
     * [.mapper] function to feed its result to the builder.  In this way, custom class serializers
     * registered to the [Mapper.Builder] by [Graph] implementations will end up being used for
     * the serialization process.
     */
    fun reader(): R

    /**
     * Creates a [GraphWriter.WriterBuilder] implementation . Implementers should call the
     * [.mapper] function to feed its result to the builder.  In this way, custom class serializers
     * registered to the [Mapper.Builder] by [Graph] implementations will end up being used for
     * the serialization process.
     */
    fun writer(): W

    /**
     * Constructs a [Mapper.Builder] which is responsible for constructing the abstraction over different
     * serialization methods.  Implementations should set defaults as required, but most importantly need to
     * make the appropriate call to [Mapper.Builder.addRegistry] which will provide the
     * builder with any required custom serializers of the [Graph].
     */
    fun mapper(): M

    /**
     * Write a [Graph] to file using the default configuration of the [.writer] and its supplied
     * [.mapper].
     */
    @Throws(IOException::class)
    fun writeGraph(file: String?)

    /**
     * Read a [Graph] from file using the default configuration of the [.reader] and its supplied
     * [.mapper].
     */
    @Throws(IOException::class)
    fun readGraph(file: String?)

    object Exceptions {
        fun readerFormatIsForFullGraphSerializationOnly(clazz: Class<out GraphReader?>?): UnsupportedOperationException {
            return UnsupportedOperationException(String.format("%s only reads an entire Graph", clazz))
        }

        fun writerFormatIsForFullGraphSerializationOnly(clazz: Class<out GraphWriter?>?): UnsupportedOperationException {
            return UnsupportedOperationException(String.format("%s only writes an entire Graph", clazz))
        }
    }

    /**
     * Helps to construct an [Io] implementation and should be implemented by every such implementation as
     * that class will be passed to [Graph.io] by the user.
     */
    interface Builder<I : Io<*, *, *>?> {
        /**
         * Allows a [Graph] implementation to have full control over the [Mapper.Builder] instance.
         * Typically, the implementation will just pass in its [IoRegistry] implementation so that the
         * [Mapper] that gets built will have knowledge of any custom classes and serializers it may have.
         *
         *
         * End-users should not use this method directly.  If a user wants to register custom serializers, then such
         * things can be done via calls to [Io.mapper] after the [Io] is constructed via
         * [Graph.io].
         */
        fun onMapper(onMapper: Consumer<Mapper.Builder?>?): Builder<out Io<*, *, *>?>?

        /**
         * Providers use this method to supply the current instance of their [Graph] to the builder.  End-users
         * should not call this method directly.
         */
        fun graph(g: Graph?): Builder<out Io<*, *, *>?>?

        /**
         * Determines if the version matches the one configured for this builder. Graph providers can use this in
         * calls to [Graph.io] to figure out the correct versions of registries to add.
         */
        fun <V> requiresVersion(version: V): Boolean

        /**
         * Providers call this method in the [Graph.io] method to construct the [Io] instance
         * and return the value.  End-users will typically not call this method.
         */
        fun create(): I
    }
}