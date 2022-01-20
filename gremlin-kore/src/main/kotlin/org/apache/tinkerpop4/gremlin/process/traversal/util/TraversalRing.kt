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

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class TraversalRing<A, B>(vararg traversals: Traversal.Admin<A, B>?) : Serializable, Cloneable {
    private var traversals: List<Traversal.Admin<A, B>> = ArrayList()
    private var currentTraversal = -1

    init {
        Collections.addAll(this.traversals, traversals)
    }

    operator fun next(): Traversal.Admin<A, B>? {
        return if (traversals.isEmpty()) {
            null
        } else {
            currentTraversal = (currentTraversal + 1) % traversals.size()
            traversals[currentTraversal]
        }
    }

    val isEmpty: Boolean
        get() = traversals.isEmpty()

    fun reset() {
        currentTraversal = -1
    }

    fun size(): Int {
        return traversals.size()
    }

    fun addTraversal(traversal: Traversal.Admin<A, B>?) {
        traversals.add(traversal)
    }

    fun replaceTraversal(oldTraversal: Traversal.Admin<A, B>?, newTraversal: Traversal.Admin<A, B>?) {
        var i = 0
        val j: Int = traversals.size()
        while (i < j) {
            if (Objects.equals(oldTraversal, traversals[i])) {
                traversals.set(i, newTraversal)
                break
            }
            i++
        }
    }

    fun getTraversals(): List<Traversal.Admin<A, B>> {
        return Collections.unmodifiableList(traversals)
    }

    @Override
    override fun toString(): String {
        return traversals.toString()
    }

    @Override
    fun clone(): TraversalRing<A, B> {
        return try {
            val clone = super.clone() as TraversalRing<A, B>
            clone.traversals = ArrayList()
            for (traversal in traversals) {
                clone.addTraversal(traversal.clone())
            }
            clone
        } catch (e: CloneNotSupportedException) {
            throw IllegalStateException(e.getMessage(), e)
        }
    }

    @Override
    override fun hashCode(): Int {
        var result: Int = this.getClass().hashCode()
        var i = 0
        for (traversal in traversals) {
            result = result xor Integer.rotateLeft(traversal.hashCode(), i++)
        }
        return result
    }
}