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
package org.apache.tinkerpop4.gremlin.process.traversal.util

import org.apache.commons.configuration2.Configuration

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class PureTraversal<S, E>(pureTraversal: Traversal.Admin<S, E>) : Serializable, Cloneable {
    private var pureTraversal: Traversal.Admin<S, E>

    @kotlin.jvm.Transient
    private var cachedTraversal: Traversal.Admin<S, E>? = null

    init {
        this.pureTraversal = pureTraversal
    }

    val pure: Traversal.Admin<S, E>
        get() = pureTraversal.clone()

    fun get(): Traversal.Admin<S, E> {
        if (null == cachedTraversal) cachedTraversal = pureTraversal.clone()
        return cachedTraversal
    }

    fun storeState(configuration: Configuration, configurationKey: String?) {
        try {
            VertexProgramHelper.serialize(
                this,
                configuration,
                configurationKey
            ) // the traversal can not be serialized (probably because of lambdas). As such, try direct reference.
        } catch (e: IllegalArgumentException) {
            configuration.setProperty(configurationKey, this)
        }
    }

    @Override
    override fun hashCode(): Int {
        return pureTraversal.hashCode()
    }

    @Override
    override fun equals(other: Object): Boolean {
        return other is PureTraversal<*, *> && pureTraversal.equals((other as PureTraversal<*, *>).pureTraversal)
    }

    ///////////
    @Override
    override fun toString(): String {
        return get().toString()
    }

    @Override
    fun clone(): PureTraversal<S, E> {
        return try {
            val clone = super.clone() as PureTraversal<S, E>
            clone.pureTraversal = pureTraversal.clone()
            clone.cachedTraversal = null
            clone
        } catch (e: CloneNotSupportedException) {
            throw IllegalStateException(e.getMessage(), e)
        }
    }

    companion object {
        ////////////
        fun <S, E> storeState(
            configuration: Configuration?,
            configurationKey: String?,
            traversal: Traversal.Admin<S, E>
        ) {
            PureTraversal<Any, Any>(traversal).storeState(configuration, configurationKey)
        }

        fun <S, E> loadState(
            configuration: Configuration,
            configurationKey: String?,
            graph: Graph?
        ): PureTraversal<S, E> {
            val configValue: Object = configuration.getProperty(configurationKey)
            val pureTraversal = if (configValue is String) VertexProgramHelper.deserialize(
                configuration,
                configurationKey
            ) else (configValue as PureTraversal<S, E>)
            pureTraversal.pureTraversal.setGraph(graph)
            return pureTraversal
        }
    }
}