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
package org.apache.tinkerpop4.gremlin.process.traversal.step.map

import org.apache.tinkerpop4.gremlin.process.traversal.Path

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class TreeStep<S>(traversal: Traversal.Admin?) : ReducingBarrierStep<S, Tree?>(traversal), TraversalParent,
    ByModulating, PathProcessor {
    private var traversalRing: TraversalRing<Object, Object> = TraversalRing()

    @get:Override
    @set:Override
    var keepLabels: Set<String>? = null
        set(keepLabels) {
            field = HashSet(keepLabels)
        }

    init {
        this.setSeedSupplier(TreeSupplier.instance() as Supplier)
        this.setReducingBiOperator(TreeBiOperator.instance())
    }

    @get:Override
    val localChildren: List<Any>
        get() = traversalRing.getTraversals()

    @Override
    fun modulateBy(treeTraversal: Traversal.Admin<*, *>?) {
        traversalRing.addTraversal(this.integrateChild(treeTraversal))
    }

    @Override
    fun replaceLocalChild(oldTraversal: Traversal.Admin<*, *>?, newTraversal: Traversal.Admin<*, *>?) {
        traversalRing.replaceTraversal(
            oldTraversal as Traversal.Admin<Object?, Object?>?,
            newTraversal as Traversal.Admin<Object?, Object?>?
        )
    }

    @get:Override
    val requirements: Set<Any>
        get() = this.getSelfAndChildRequirements(TraverserRequirement.PATH, TraverserRequirement.SIDE_EFFECTS)

    @Override
    fun projectTraverser(traverser: Traverser.Admin<S>): Tree {
        val topTree = Tree()
        var depth: Tree = topTree
        val path: Path = traverser.path()
        for (i in 0 until path.size()) {
            val product: TraversalProduct =
                TraversalUtil.produce(path.< Object > get < Object ? > i, traversalRing.next())
            if (product.isProductive()) {
                val `object`: Object = product.get()
                if (!depth.containsKey(`object`)) depth.put(`object`, Tree())
                depth = depth.get(`object`) as Tree
            }
        }
        traversalRing.reset()
        return topTree
    }

    @Override
    fun clone(): TreeStep<S> {
        val clone = super.clone() as TreeStep<S>
        clone.traversalRing = traversalRing.clone()
        return clone
    }

    @Override
    fun setTraversal(parentTraversal: Traversal.Admin<*, *>?) {
        super.setTraversal(parentTraversal)
        traversalRing.getTraversals().forEach(this::integrateChild)
    }

    @Override
    override fun hashCode(): Int {
        return super.hashCode() xor traversalRing.hashCode()
    }

    @Override
    override fun toString(): String {
        return StringFactory.stepString(this, traversalRing)
    }

    @Override
    fun reset() {
        super.reset()
        traversalRing.reset()
    }

    ///////////
    class TreeBiOperator : BinaryOperator<Tree?>, Serializable {
        @Override
        fun apply(mutatingSeed: Tree, tree: Tree?): Tree {
            mutatingSeed.addTree(tree)
            return mutatingSeed
        }

        companion object {
            private val INSTANCE = TreeBiOperator()
            fun instance(): TreeBiOperator {
                return INSTANCE
            }
        }
    }
}