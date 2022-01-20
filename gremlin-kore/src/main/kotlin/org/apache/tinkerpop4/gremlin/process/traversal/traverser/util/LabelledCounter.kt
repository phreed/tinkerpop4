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
package org.apache.tinkerpop4.gremlin.process.traversal.traverser.util

import java.io.Serializable

/**
 * Class to track a count associated with a Label
 */
class LabelledCounter : Serializable, Cloneable, Cloneable {
    private val label: String?
    private var count: Short = 0

    protected constructor() {
        label = ""
    }

    constructor(label: String?, initialCount: Short) {
        if (label == null) {
            throw NullPointerException("Label is null")
        }
        this.label = label
        count = initialCount
    }

    fun hasLabel(label: String?): Boolean {
        return this.label!!.equals(label)
    }

    fun count(): Int {
        return count.toInt()
    }

    fun increment() {
        count++
    }

    @Override
    fun clone(): Object {
        return LabelledCounter(label, count)
    }

    @Override
    override fun toString(): String {
        return "Step Label: " + label + " Counter: " + Short.toString(count)
    }

    @Override
    override fun equals(o: Object): Boolean {
        if (this === o) return true
        if (o !is LabelledCounter) return false
        val that = o as LabelledCounter
        return if (count != that.count) false else label?.equals(that.label) ?: (that.label == null)
    }

    @Override
    override fun hashCode(): Int {
        var result = label?.hashCode() ?: 0
        result = 31 * result + count.toInt()
        return result
    }
}