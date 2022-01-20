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

import java.io.Serializable
import java.util.ArrayList
import java.util.Collection
import java.util.Collections
import java.util.HashMap
import java.util.List
import java.util.Map

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class Tree<T>() : HashMap<T, Tree<T>?>(), Serializable {
    @SafeVarargs
    constructor(vararg children: T) : this() {
        for (t in children) {
            this.put(t, Tree<T>())
        }
    }

    @SafeVarargs
    constructor(vararg children: Map.Entry<T, Tree<T>?>) : this() {
        for (entry in children) {
            this.put(entry.getKey(), entry.getValue())
        }
    }

    fun getTreesAtDepth(depth: Int): List<Tree<T>> {
        var currentDepth: List<Tree<T>> = Collections.singletonList(this)
        for (i in 0 until depth) {
            currentDepth = if (i == depth - 1) {
                return currentDepth
            } else {
                val temp: List<Tree<T>> = ArrayList<Tree<T>>()
                for (t in currentDepth) {
                    temp.addAll(t.values())
                }
                temp
            }
        }
        return Collections.emptyList()
    }

    fun getObjectsAtDepth(depth: Int): List<T> {
        val list: List<T> = ArrayList<T>()
        for (t in getTreesAtDepth(depth)) {
            list.addAll(t.keySet())
        }
        return list
    }

    val leafTrees: List<Tree<T>>
        get() {
            val leaves: List<Tree<T>> = ArrayList()
            var currentDepth: List<Tree<T>> = Collections.singletonList(this)
            var allLeaves = false
            while (!allLeaves) {
                allLeaves = true
                val temp: List<Tree<T>> = ArrayList()
                for (t in currentDepth) {
                    if (t.isLeaf) {
                        for (t2 in t.entrySet()) {
                            leaves.add(Tree<T>(t2))
                        }
                    } else {
                        allLeaves = false
                        temp.addAll(t.values())
                    }
                }
                currentDepth = temp
            }
            return leaves
        }
    val leafObjects: List<T>
        get() {
            val leaves: List<T> = ArrayList<T>()
            for (t in leafTrees) {
                leaves.addAll(t.keySet())
            }
            return leaves
        }
    val isLeaf: Boolean
        get() {
            val values: Collection<Tree<T>> = this.values()
            return values.iterator().next().isEmpty()
        }

    fun addTree(tree: Tree<T>) {
        tree.forEach { k, t ->
            if (this.containsKey(k)) {
                this.get(k).addTree(t)
            } else {
                this.put(k, t)
            }
        }
    }

    fun splitParents(): List<Tree<T>> {
        return if (this.keySet().size() === 1) {
            Collections.singletonList(this)
        } else {
            val parents: List<Tree<T>> = ArrayList()
            this.forEach { k, t ->
                val parentTree: Tree<T> =
                    Tree<Any>()
                parentTree.put(k, t)
                parents.add(parentTree)
            }
            parents
        }
    }
}