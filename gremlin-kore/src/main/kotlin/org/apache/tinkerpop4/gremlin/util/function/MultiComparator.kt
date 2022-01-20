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
package org.apache.tinkerpop4.gremlin.util.function

import org.apache.tinkerpop4.gremlin.process.traversal.Order
import org.apache.tinkerpop4.gremlin.process.traversal.traverser.ProjectedTraverser
import java.io.Serializable
import java.util.Comparator
import java.util.List

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class MultiComparator<C> : Comparator<C>, Serializable {
    private var comparators: List<Comparator>? = null
    var isShuffle = false
        private set
    var startIndex = 0

    private constructor() {
        // for serialization purposes
    }

    constructor(comparators: List<Comparator<C>?>?) {
        this.comparators = comparators
        isShuffle = !this.comparators!!.isEmpty() && Order.shuffle === this.comparators!![this.comparators!!.size() - 1]
        for (i in 0 until this.comparators!!.size()) {
            if (this.comparators!![i] === Order.shuffle) startIndex = i + 1
        }
    }

    @Override
    fun compare(objectA: C, objectB: C): Int {
        return if (comparators!!.isEmpty()) {
            Order.asc.compare(objectA, objectB)
        } else {
            for (i in startIndex until comparators!!.size()) {
                val comparison: Int =
                    comparators!![i].compare(getObject(objectA, i), getObject(objectB, i))
                if (comparison != 0) return comparison
            }
            0
        }
    }

    private fun getObject(`object`: C, index: Int): Object {
        return if (`object` is ProjectedTraverser) (`object` as ProjectedTraverser).getProjections()
            .get(index) else `object`
    }
}