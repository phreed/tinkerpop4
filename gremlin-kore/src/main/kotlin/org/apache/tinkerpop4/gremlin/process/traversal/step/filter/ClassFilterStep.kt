/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.tinkerpop4.gremlin.process.traversal.step.filter

import org.apache.tinkerpop4.gremlin.process.traversal.Traversal
import org.apache.tinkerpop4.gremlin.process.traversal.Traverser
import org.apache.tinkerpop4.gremlin.structure.util.StringFactory

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class ClassFilterStep<S, T>(traversal: Traversal.Admin?, classFilter: Class<T>, allowClasses: Boolean) :
    FilterStep<S>(traversal) {
    private val classFilter: Class<T>
    private val allowClasses: Boolean

    init {
        this.classFilter = classFilter
        this.allowClasses = allowClasses
    }

    fun filter(traverser: Traverser.Admin<S>): Boolean {
        return allowClasses == classFilter.isInstance(traverser.get())
    }

    override fun hashCode(): Int {
        return super.hashCode() xor classFilter.hashCode() xor Boolean.hashCode(allowClasses)
    }

    override fun toString(): String {
        return StringFactory.stepString(this, classFilter.getSimpleName())
    }
}