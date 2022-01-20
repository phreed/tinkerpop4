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
class MutablePath private constructor(capacity: Int) : Path, Serializable {
    protected val objects: List<Object>
    protected val labels: List<Set<String?>>

    protected constructor() : this(10) {}

    init {
        objects = ArrayList(capacity)
        labels = ArrayList(capacity)
    }

    @Override
    @SuppressWarnings("CloneDoesntCallSuperClone,CloneDoesntDeclareCloneNotSupportedException")
    fun clone(): MutablePath {
        val clone = MutablePath(objects.size())
        // TODO: Why is this not working Hadoop serialization-wise?... Its cause DetachedPath's clone needs to detach on clone.
        /*final MutablePath clone = (MutablePath) super.clone();
        clone.objects = new ArrayList<>();
        clone.labels = new ArrayList<>();*/clone.objects.addAll(objects)
        for (labels in labels) {
            clone.labels.add(LinkedHashSet(labels))
        }
        return clone
    }

    @get:Override
    val isEmpty: Boolean
        get() = objects.isEmpty()

    @Override
    fun <A> head(): A {
        return objects[objects.size() - 1]
    }

    @Override
    fun size(): Int {
        return objects.size()
    }

    @Override
    fun extend(`object`: Object?, labels: Set<String?>?): Path {
        objects.add(`object`)
        this.labels.add(LinkedHashSet(labels))
        return this
    }

    @Override
    fun extend(labels: Set<String?>): Path {
        if (!labels.isEmpty() && !this.labels[this.labels.size() - 1].containsAll(labels)) this.labels[this.labels.size() - 1].addAll(
            labels
        )
        return this
    }

    @Override
    fun retract(removeLabels: Set<String?>?): Path {
        for (i in labels.size() - 1 downTo 0) {
            labels[i].removeAll(removeLabels)
            if (labels[i].isEmpty()) {
                labels.remove(i)
                objects.remove(i)
            }
        }
        return this
    }

    @Override
    operator fun <A> get(index: Int): A {
        return objects[index]
    }

    @Override
    operator fun <A> get(pop: Pop, label: String): A {
        return if (Pop.mixed === pop) {
            this.get(label)
        } else if (Pop.all === pop) {
            if (hasLabel(label)) {
                val `object`: Object = this.get(label)
                if (`object` is List) `object` else Collections.singletonList(`object`)
            } else {
                Collections.emptyList()
            }
        } else {
            // Override default to avoid building temporary list, and to stop looking when we find the label.
            if (Pop.last === pop) {
                for (i in labels.size() - 1 downTo 0) {
                    if (labels[i].contains(label)) return objects[i]
                }
            } else {
                for (i in 0 until labels.size()) {
                    if (labels[i].contains(label)) return objects[i]
                }
            }
            throw Path.Exceptions.stepWithProvidedLabelDoesNotExist(label)
        }
    }

    @Override
    fun hasLabel(label: String?): Boolean {
        for (set in labels) {
            if (set.contains(label)) return true
        }
        return false
    }

    @Override
    fun objects(): List<Object> {
        return Collections.unmodifiableList(objects)
    }

    @Override
    fun labels(): List<Set<String>> {
        return Collections.unmodifiableList(labels)
    }

    @Override
    operator fun iterator(): Iterator<Object> {
        return objects.iterator()
    }

    @Override
    override fun toString(): String {
        return StringFactory.pathString(this)
    }

    @Override
    override fun hashCode(): Int {
        return objects.hashCode()
    }

    @Override
    override fun equals(other: Object): Boolean {
        if (other !is Path) return false
        val otherPath: Path = other as Path
        if (otherPath.size() !== objects.size()) return false
        val otherPathObjects: List<Object> = otherPath.objects()
        val otherPathLabels: List<Set<String>> = otherPath.labels()
        for (i in objects.size() - 1 downTo 0) {
            val o1: Object = objects[i]
            val o2: Object = otherPathObjects[i]
            if (!(o1 == null && o2 == null) && o1 != null && !o1.equals(o2)) return false
            if (!labels[i].equals(otherPathLabels[i])) return false
        }
        return true
    }

    companion object {
        fun make(): Path {
            return MutablePath()
        }
    }
}