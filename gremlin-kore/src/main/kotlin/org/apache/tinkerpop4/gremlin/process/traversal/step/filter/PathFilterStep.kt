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

import org.apache.tinkerpop4.gremlin.process.traversal.Path

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class PathFilterStep<S>(traversal: Traversal.Admin?, isSimple: Boolean) : FilterStep<S>(traversal), FromToModulating,
    ByModulating, TraversalParent, PathProcessor {
    var fromLabel: String? = null
        protected set
    var toLabel: String? = null
        protected set
    val isSimple: Boolean
    private var traversalRing: TraversalRing<Object, Object>
    private var keepLabels: Set<String>? = null

    init {
        traversalRing = TraversalRing()
        this.isSimple = isSimple
    }

    @Override
    protected fun filter(traverser: Traverser.Admin<S>): Boolean {
        val path: Path = traverser.path().subPath(fromLabel, toLabel)
        return if (traversalRing.isEmpty()) path.isSimple() === isSimple else {
            traversalRing.reset()
            val byPath: Path = MutablePath.make()
            val labels: List<Set<String>> = path.labels()
            val objects: List<Object> = path.objects()
            for (ix in 0 until objects.size()) {
                val t: Traversal.Admin = traversalRing.next()
                val p: TraversalProduct = TraversalUtil.produce(objects[ix], t)

                // if not productive we can quit coz this path is getting filtered
                if (!p.isProductive()) break
                byPath.extend(p.get(), labels[ix])
            }

            // the path sizes must be equal or else it means a by() wasn't productive and that path will be filtered
            path.size() === byPath.size() && byPath.isSimple() === isSimple
        }
    }

    @get:Override
    val requirements: Set<Any>
        get() = Collections.singleton(TraverserRequirement.PATH)

    fun addFrom(fromLabel: String?) {
        this.fromLabel = fromLabel
    }

    fun addTo(toLabel: String?) {
        this.toLabel = toLabel
    }

    @Override
    override fun toString(): String {
        return StringFactory.stepString(this, if (isSimple) "simple" else "cyclic", fromLabel, toLabel, traversalRing)
    }

    @Override
    fun clone(): PathFilterStep<S> {
        val clone = super.clone() as PathFilterStep<S>
        clone.traversalRing = traversalRing.clone()
        return clone
    }

    @Override
    fun setTraversal(parentTraversal: Traversal.Admin<*, *>?) {
        super.setTraversal(parentTraversal)
        traversalRing.getTraversals().forEach(this::integrateChild)
    }

    @get:Override
    val localChildren: List<Any>
        get() = traversalRing.getTraversals()

    @Override
    fun modulateBy(pathTraversal: Traversal.Admin<*, *>?) {
        traversalRing.addTraversal(this.integrateChild(pathTraversal))
    }

    @Override
    fun replaceLocalChild(oldTraversal: Traversal.Admin<*, *>?, newTraversal: Traversal.Admin<*, *>?) {
        traversalRing.replaceTraversal(
            oldTraversal as Traversal.Admin<Object?, Object?>?,
            newTraversal as Traversal.Admin<Object?, Object?>?
        )
    }

    @Override
    fun reset() {
        super.reset()
        traversalRing.reset()
    }

    @Override
    override fun hashCode(): Int {
        return super.hashCode() xor
                traversalRing.hashCode() xor
                Boolean.hashCode(isSimple) xor
                (if (null == fromLabel) "null".hashCode() else fromLabel!!.hashCode()) xor
                if (null == toLabel) "null".hashCode() else toLabel!!.hashCode()
    }

    @Override
    fun setKeepLabels(keepLabels: Set<String?>?) {
        this.keepLabels = HashSet(keepLabels)
    }

    @Override
    protected fun processNextStart(): Traverser.Admin<S> {
        return PathProcessor.processTraverserPathLabels(super.processNextStart(), keepLabels)
    }

    @Override
    fun getKeepLabels(): Set<String>? {
        return keepLabels
    }

    fun getTraversalRing(): TraversalRing<Object, Object> {
        return traversalRing
    }
}