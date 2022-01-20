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
package org.apache.tinkerpop4.gremlin.process.traversal.step.util

import org.apache.tinkerpop4.gremlin.process.traversal.Path
import org.apache.tinkerpop4.gremlin.structure.util.StringFactory
import java.io.Serializable
import java.util.Collections
import java.util.List
import java.util.Set

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class EmptyPath private constructor() : Path, Serializable {
    @Override
    fun size(): Int {
        return 0
    }

    @Override
    fun extend(`object`: Object?, labels: Set<String?>?): Path {
        return this
    }

    @Override
    fun extend(labels: Set<String?>?): Path {
        return this
    }

    @Override
    fun retract(labels: Set<String?>?): Path {
        return this
    }

    @Override
    operator fun <A> get(label: String?): A {
        throw Path.Exceptions.stepWithProvidedLabelDoesNotExist(label)
    }

    @Override
    operator fun <A> get(index: Int): A {
        return Collections.emptyList().get(index)
    }

    @Override
    fun hasLabel(label: String?): Boolean {
        return false
    }

    @Override
    fun objects(): List<Object> {
        return Collections.emptyList()
    }

    @Override
    fun labels(): List<Set<String>> {
        return Collections.emptyList()
    }

    @get:Override
    val isSimple: Boolean
        get() = true

    @Override
    @SuppressWarnings("CloneDoesntCallSuperClone,CloneDoesntDeclareCloneNotSupportedException")
    fun clone(): EmptyPath {
        return this
    }

    @Override
    override fun hashCode(): Int {
        return -1424379551
    }

    @Override
    override fun equals(`object`: Object?): Boolean {
        return `object` is EmptyPath
    }

    @Override
    override fun toString(): String {
        return StringFactory.pathString(this)
    }

    companion object {
        private val INSTANCE = EmptyPath()
        fun instance(): Path {
            return INSTANCE
        }
    }
}