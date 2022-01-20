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

import org.apache.tinkerpop4.gremlin.process.traversal.Traversal
import org.apache.tinkerpop4.gremlin.structure.Graph
import java.io.Serializable
import java.util.function.Function
import java.util.function.Supplier

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class TraversalClassFunction<S, E>(traversalSupplierClass: Class<out Supplier<Traversal.Admin<S, E>?>?>) :
    Function<Graph?, Traversal.Admin<S, E>?>, Serializable {
    private val traversalSupplierClass: Class<out Supplier<Traversal.Admin<S, E>?>?>

    init {
        this.traversalSupplierClass = traversalSupplierClass
    }

    fun apply(graph: Graph?): Traversal.Admin<S, E> {
        return try {
            val traversal: Traversal.Admin<S, E> = traversalSupplierClass.getConstructor().newInstance().get()
            if (!traversal.isLocked()) {
                traversal.setGraph(graph)
                traversal.applyStrategies()
            }
            traversal
        } catch (e: Exception) {
            throw IllegalStateException(e.getMessage(), e)
        }
    }

    override fun toString(): String {
        return traversalSupplierClass.getCanonicalName()
    }
}