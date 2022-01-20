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

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class ImmutablePath private constructor(
    private val previousPath: ImmutablePath?,
    currentObject: Object?,
    currentLabels: Set<String>?
) : Path, Serializable, Cloneable {
    private val currentObject: Object?
    private val currentLabels: Set<String>?
    @SuppressWarnings("CloneDoesntCallSuperClone,CloneDoesntDeclareCloneNotSupportedException")
    @Override
    fun clone(): ImmutablePath {
        return this
    }

    init {
        this.currentObject = currentObject
        this.currentLabels = currentLabels
    }

    private val isTail: Boolean
        private get() = END.equals(currentObject)

    @get:Override
    val isEmpty: Boolean
        get() = isTail

    @Override
    fun size(): Int {
        var counter = 0
        var currentPath: ImmutablePath? = this
        while (true) {
            if (currentPath!!.isTail) return counter
            counter++
            currentPath = currentPath.previousPath
        }
    }

    @Override
    fun <A> head(): A? {
        return currentObject
    }

    @Override
    fun extend(`object`: Object?, labels: Set<String>?): Path {
        return ImmutablePath(this, `object`, labels)
    }

    @Override
    fun extend(labels: Set<String>): Path {
        return if (labels.isEmpty() || currentLabels!!.containsAll(labels)) this else {
            val newLabels: Set<String> = LinkedHashSet()
            newLabels.addAll(currentLabels)
            newLabels.addAll(labels)
            ImmutablePath(previousPath, currentObject, newLabels)
        }
    }

    @Override
    fun retract(labels: Set<String?>): Path {
        if (labels.isEmpty()) return this

        // get all the immutable path sections
        val immutablePaths: List<ImmutablePath> = ArrayList()
        var currentPath: ImmutablePath? = this
        while (true) {
            if (currentPath!!.isTail) break
            immutablePaths.add(0, currentPath)
            currentPath = currentPath.previousPath
        }
        // build a new immutable path using the respective path sections that are not to be retracted
        var newPath: Path = TAIL_PATH
        for (immutablePath in immutablePaths) {
            val temp: Set<String> = LinkedHashSet(immutablePath.currentLabels)
            temp.removeAll(labels)
            if (!temp.isEmpty()) newPath = newPath.extend(immutablePath.currentObject, temp)
        }
        return newPath
    }

    @Override
    operator fun <A> get(index: Int): A? {
        var counter = size()
        var currentPath: ImmutablePath? = this
        while (true) {
            if (index == --counter) return currentPath!!.currentObject
            currentPath = currentPath!!.previousPath
        }
    }

    @Override
    operator fun <A> get(pop: Pop, label: String): A? {
        if (Pop.mixed === pop) {
            return this.get(label)
        } else if (Pop.all === pop) {
            // Recursively build the list to avoid building objects/labels collections.
            val list: List<Object> = ArrayList()
            var currentPath: ImmutablePath? = this
            while (true) {
                if (currentPath!!.isTail) break else if (currentPath.currentLabels!!.contains(label)) list.add(
                    0,
                    currentPath.currentObject
                )
                currentPath = currentPath.previousPath
            }
            return list as A
        } else if (Pop.last === pop) {
            var currentPath: ImmutablePath? = this
            while (true) {
                currentPath =
                    if (currentPath!!.isTail) throw Path.Exceptions.stepWithProvidedLabelDoesNotExist(label) else if (currentPath.currentLabels!!.contains(
                            label
                        )
                    ) return currentPath.currentObject else currentPath.previousPath
            }
        } else { // Pop.first
            var found: A? = null
            var currentPath: ImmutablePath? = this
            while (true) {
                if (currentPath!!.isTail) break else if (currentPath.currentLabels!!.contains(label)) found =
                    currentPath.currentObject
                currentPath = currentPath.previousPath
            }
            return found
        }
    }

    @Override
    fun hasLabel(label: String): Boolean {
        var currentPath: ImmutablePath? = this
        while (true) {
            currentPath =
                if (currentPath!!.isTail) return false else if (currentPath.currentLabels!!.contains(label)) return true else currentPath.previousPath
        }
    }

    @Override
    fun objects(): List<Object> {
        val objects: List<Object> = ArrayList()
        var currentPath: ImmutablePath? = this
        while (true) {
            if (currentPath!!.isTail) break
            objects.add(0, currentPath.currentObject)
            currentPath = currentPath.previousPath
        }
        return Collections.unmodifiableList(objects)
    }

    @Override
    fun labels(): List<Set<String>> {
        val labels: List<Set<String>> = ArrayList()
        var currentPath: ImmutablePath? = this
        while (true) {
            if (currentPath!!.isTail) break
            labels.add(0, currentPath.currentLabels)
            currentPath = currentPath.previousPath
        }
        return Collections.unmodifiableList(labels)
    }

    @Override
    override fun toString(): String {
        return StringFactory.pathString(this)
    }

    @Override
    override fun hashCode(): Int {
        // hashCode algorithm from AbstractList
        val hashCodes = IntArray(size())
        var index = hashCodes.size - 1
        var currentPath: ImmutablePath? = this
        while (true) {
            if (currentPath!!.isTail) break
            hashCodes[index] = Objects.hashCode(currentPath.currentObject)
            currentPath = currentPath.previousPath
            index--
        }
        var hashCode = 1
        for (hash in hashCodes) {
            hashCode = hashCode * 31 + hash
        }
        return hashCode
    }

    @Override
    override fun equals(other: Object): Boolean {
        if (other !is Path) return false
        val otherPath: Path = other as Path
        val size = size()
        if (otherPath.size() !== size) return false
        if (size > 0) {
            var currentPath: ImmutablePath? = this
            val otherObjects: List<Object?> = otherPath.objects()
            val otherLabels: List<Set<String>> = otherPath.labels()
            for (i in otherLabels.size() - 1 downTo 0) {
                currentPath =
                    if (currentPath!!.isTail) return true else if (!(currentPath.currentObject == null && otherObjects[i] == null) && currentPath.currentObject != null && !currentPath.currentObject.equals(
                            otherObjects[i]
                        ) ||
                        !currentPath.currentLabels!!.equals(otherLabels[i])
                    ) return false else currentPath.previousPath
            }
        }
        return true
    }

    @Override
    fun popEquals(pop: Pop, other: Object): Boolean {
        if (other !is Path) return false
        val otherPath: Path = other as Path
        var currentPath: ImmutablePath? = this
        while (true) {
            if (currentPath!!.isTail) break
            for (label in currentPath.currentLabels!!) {
                if (!otherPath.hasLabel(label)) return false
                val o1: Object? = this.get<Any>(pop, label)
                val o2: Object = otherPath.get(pop, label)
                if (o1 != null && !o1.equals(o2)) return false
            }
            currentPath = currentPath.previousPath
        }
        return true
    }

    @get:Override
    val isSimple: Boolean
        get() {
            val objects: Set<Object?> = HashSet()
            var currentPath: ImmutablePath? = this
            while (true) {
                currentPath =
                    if (currentPath!!.isTail) return true else if (objects.contains(currentPath.currentObject)) return false else {
                        objects.add(currentPath.currentObject)
                        currentPath.previousPath
                    }
            }
        }

    companion object {
        private val END: Object = EmptyPath.instance()
        private val TAIL_PATH = ImmutablePath(null, END, null)
        fun make(): Path {
            return TAIL_PATH
        }
    }
}