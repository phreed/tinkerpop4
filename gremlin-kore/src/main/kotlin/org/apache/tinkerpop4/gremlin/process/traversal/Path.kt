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
package org.apache.tinkerpop4.gremlin.process.traversal

import org.apache.tinkerpop4.gremlin.process.traversal.step.util.MutablePath

/**
 * A Path denotes a particular walk through a [Graph] as defined by a [Traversal].
 * In abstraction, any Path implementation maintains two lists: a list of sets of labels and a list of objects.
 * The list of labels are the labels of the steps traversed. The list of objects are the objects traversed.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
interface Path : Cloneable, Iterable<Object?> {
    /**
     * Get the number of step in the path.
     *
     * @return the size of the path
     */
    fun size(): Int {
        return objects().size()
    }

    /**
     * Determine if the path is empty or not.
     *
     * @return whether the path is empty or not.
     */
    val isEmpty: Boolean
        get() = size() == 0

    /**
     * Get the head of the path.
     *
     * @param <A> the type of the head of the path
     * @return the head of the path
    </A> */
    fun <A> head(): A? {
        return objects()[size() - 1]
    }

    /**
     * Add a new step to the path with an object and any number of associated labels.
     *
     * @param object the new head of the path
     * @param labels the labels at the head of the path
     * @return the extended path
     */
    fun extend(`object`: Object?, labels: Set<String?>?): Path?

    /**
     * Add labels to the head of the path.
     *
     * @param labels the labels at the head of the path
     * @return the path with added labels
     */
    fun extend(labels: Set<String?>?): Path?

    /**
     * Remove labels from path.
     *
     * @param labels the labels to remove
     * @return the path with removed labels
     */
    fun retract(labels: Set<String?>?): Path?

    /**
     * Get the object associated with the particular label of the path.
     * If the path as multiple labels of the type, then return a [List] of those objects.
     *
     * @param label the label of the path
     * @param <A>   the type of the object associated with the label
     * @return the object associated with the label of the path
     * @throws IllegalArgumentException if the path does not contain the label
    </A> */
    @Throws(IllegalArgumentException::class)
    operator fun <A> get(label: String): A {
        val objects: List<Object?> = objects()
        val labels = labels()
        var `object`: Optional<Object?>? = null
        for (i in 0 until labels.size()) {
            if (labels[i].contains(label)) {
                if (null == `object`) {
                    `object` = Optional.ofNullable(objects[i])
                } else if (`object`.get() is List) {
                    (`object`.get() as List).add(objects[i])
                } else {
                    val list: List<Object> = ArrayList(2)
                    list.add(`object`.get())
                    list.add(objects[i])
                    `object` = Optional.of(list)
                }
            }
        }
        if (null == `object`) throw Exceptions.stepWithProvidedLabelDoesNotExist(label)
        return `object`.orElse(null)
    }

    /**
     * Pop the object(s) associated with the label of the path.
     *
     * @param pop   first for least recent, last for most recent, and all for all in a list
     * @param label the label of the path
     * @param <A>   the type of the object associated with the label
     * @return the object associated with the label of the path
     * @throws IllegalArgumentException if the path does not contain the label
    </A> */
    @Throws(IllegalArgumentException::class)
    operator fun <A> get(pop: Pop, label: String): A {
        return if (Pop.mixed === pop) {
            this.get(label)
        } else if (Pop.all === pop) {
            if (hasLabel(label)) {
                val `object`: Object = this.get<Any>(label)
                if (`object` is List) `object` else Collections.singletonList(`object`)
            } else {
                Collections.emptyList()
            }
        } else {
            val `object`: Object = this.get<Any>(label)
            if (`object` is List) {
                if (Pop.last === pop) `object`[(`object` as List).size() - 1] else `object`[0]
            } else `object`
        }
    }

    /**
     * Get the object associated with the specified index into the path.
     *
     * @param index the index of the path
     * @param <A>   the type of the object associated with the index
     * @return the object associated with the index of the path
    </A> */
    operator fun <A> get(index: Int): A? {
        return objects()[index]
    }

    /**
     * Return true if the path has the specified label, else return false.
     *
     * @param label the label to search for
     * @return true if the label exists in the path
     */
    fun hasLabel(label: String?): Boolean {
        return labels().stream().filter { labels -> labels.contains(label) }.findAny().isPresent()
    }

    /**
     * An ordered list of the objects in the path.
     *
     * @return the objects of the path
     */
    fun objects(): List<Object?>

    /**
     * An ordered list of the labels associated with the path
     * The set of labels for a particular step are ordered by the order in which [Path.extend] was called.
     *
     * @return the labels of the path
     */
    fun labels(): List<Set<String>>

    @SuppressWarnings("CloneDoesntDeclareCloneNotSupportedException")
    fun clone(): Path?

    /**
     * Determines whether the path is a simple or not. A simple path has no cycles and thus, no repeated objects.
     *
     * @return Whether the path is simple or not
     */
    val isSimple: Boolean
        get() {
            val objects: List<Object?> = objects()
            for (i in 0 until objects.size() - 1) {
                for (j in i + 1 until objects.size()) {
                    if (Objects.equals(objects[i], objects[j])) return false
                }
            }
            return true
        }

    override fun iterator(): Iterator<Object?>? {
        return objects().iterator()
    }

    fun forEach(consumer: BiConsumer<Object?, Set<String?>?>) {
        val objects: List<Object?> = objects()
        val labels = labels()
        for (i in 0 until objects.size()) {
            consumer.accept(objects[i], labels[i])
        }
    }

    fun stream(): Stream<Pair<Object?, Set<String?>?>?>? {
        val labels = labels()
        val objects: List<Object?> = objects()
        return IntStream.range(0, size()).mapToObj { i -> Pair.with(objects[i], labels[i]) }
    }

    fun popEquals(pop: Pop, other: Object): Boolean {
        if (other !is Path) return false
        val otherPath = other as Path
        return labels().stream().flatMap(Set::stream).noneMatch { label ->
            if (!otherPath.hasLabel(label)) return@noneMatch true
            val o1: Object = otherPath.get(pop, label)
            val o2: Object = this.get(pop, label)
            !(null == o1 && null == o2 || o1 != null && o1.equals(o2))
        }
    }

    /**
     * Isolate a sub-path from the path object. The isolation is based solely on the path labels.
     * The to-label is inclusive. Thus, from "b" to "c" would isolate the example path as follows `a,[b,c],d`.
     * Note that if there are multiple path segments with the same label, then its the last occurrence that is isolated.
     * For instance, from "b" to "c" would be `a,b,[b,c,d,c]`.
     *
     * @param fromLabel The label to start recording the sub-path from.
     * @param toLabel   The label to end recording the sub-path to.
     * @return the isolated sub-path.
     */
    fun subPath(fromLabel: String?, toLabel: String?): Path? {
        return if (null == fromLabel && null == toLabel) this else {
            val subPath: Path = MutablePath.make()
            val size = size()
            var fromIndex = -1
            var toIndex = -1
            for (i in size - 1 downTo 0) {
                val labels: Set<String?> = labels()[i]
                if (-1 == fromIndex && labels.contains(fromLabel)) fromIndex = i
                if (-1 == toIndex && labels.contains(toLabel)) toIndex = i
            }
            if (null != fromLabel && -1 == fromIndex) throw Exceptions.couldNotLocatePathFromLabel(
                fromLabel
            )
            if (null != toLabel && -1 == toIndex) throw Exceptions.couldNotLocatePathToLabel(
                toLabel
            )
            if (fromIndex == -1) fromIndex = 0
            if (toIndex == -1) toIndex = size - 1
            if (fromIndex > toIndex) throw Exceptions.couldNotIsolatedSubPath(
                fromLabel,
                toLabel
            )
            for (i in fromIndex..toIndex) {
                val labels = labels()[i]
                subPath.extend(this.get<Any>(i), labels)
            }
            subPath
        }
    }

    object Exceptions {
        fun stepWithProvidedLabelDoesNotExist(label: String): IllegalArgumentException {
            return IllegalArgumentException("The step with label $label does not exist")
        }

        fun couldNotLocatePathFromLabel(fromLabel: String): IllegalArgumentException {
            return IllegalArgumentException("Could not locate path from-label: $fromLabel")
        }

        fun couldNotLocatePathToLabel(toLabel: String): IllegalArgumentException {
            return IllegalArgumentException("Could not locate path to-label: $toLabel")
        }

        fun couldNotIsolatedSubPath(fromLabel: String?, toLabel: String?): IllegalArgumentException {
            return IllegalArgumentException("Could not isolate path because from comes after to: $fromLabel->$toLabel")
        }
    }
}