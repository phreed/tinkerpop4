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
package org.apache.tinkerpop4.gremlin.process.traversal.traverser

import org.apache.commons.collections.map.ReferenceMap

class B_LP_NL_O_P_S_SE_SL_Traverser<T> : B_LP_O_P_S_SE_SL_Traverser<T> {
    protected var nestedLoops: Stack<LabelledCounter>? = null
    protected var loopNames: ReferenceMap? = null

    protected constructor() {}
    constructor(t: T, step: Step<T, *>?, initialBulk: Long) : super(t, step, initialBulk) {
        nestedLoops = Stack()
        loopNames = ReferenceMap(ReferenceMap.HARD, ReferenceMap.WEAK)
    }

    /////////////////
    @Override
    fun loops(): Int {
        return nestedLoops.peek().count()
    }

    @Override
    fun loops(loopName: String?): Int {
        return if (loopName == null) loops() else if (loopNames.containsKey(loopName)) (loopNames.get(loopName) as LabelledCounter).count() else throw IllegalArgumentException(
            "Loop name not defined: $loopName"
        )
    }

    @Override
    fun initialiseLoops(stepLabel: String?, loopName: String?) {
        if (nestedLoops.empty() || !nestedLoops.peek().hasLabel(stepLabel)) {
            val lc = LabelledCounter(stepLabel, 0.toShort())
            nestedLoops.push(lc)
            if (loopName != null) loopNames.put(loopName, lc)
        }
    }

    @Override
    fun incrLoops() {
        nestedLoops.peek().increment()
    }

    @Override
    fun resetLoops() {
        nestedLoops.pop()
    }

    /////////////////
    @Override
    fun <R> split(r: R, step: Step<T, R>?): Admin<R> {
        val clone = super.split(r, step) as B_LP_NL_O_P_S_SE_SL_Traverser<R>
        clone.nestedLoops = Stack()
        for (lc in nestedLoops) clone.nestedLoops.push(lc.clone() as LabelledCounter)
        if (loopNames != null) {
            clone.loopNames = ReferenceMap(ReferenceMap.HARD, ReferenceMap.WEAK)
            val loopNamesIterator: Iterator = loopNames.entrySet().iterator()
            while (loopNamesIterator.hasNext()) {
                val pair: ReferenceMap.Entry = loopNamesIterator.next() as ReferenceMap.Entry
                val idx: Int = nestedLoops.indexOf(pair.getValue())
                if (idx != -1) clone.loopNames.put(pair.getKey(), clone.nestedLoops.get(idx))
            }
        }
        return clone
    }

    @Override
    fun split(): Admin<T> {
        val clone = super.split() as B_LP_NL_O_P_S_SE_SL_Traverser<T>
        clone.nestedLoops = Stack()
        for (lc in nestedLoops) clone.nestedLoops.push(lc.clone() as LabelledCounter)
        if (loopNames != null) {
            clone.loopNames = ReferenceMap(ReferenceMap.HARD, ReferenceMap.WEAK)
            val loopNamesIterator: Iterator = loopNames.entrySet().iterator()
            while (loopNamesIterator.hasNext()) {
                val pair: ReferenceMap.Entry = loopNamesIterator.next() as ReferenceMap.Entry
                val idx: Int = nestedLoops.indexOf(pair.getValue())
                if (idx != -1) clone.loopNames.put(pair.getKey(), clone.nestedLoops.get(idx))
            }
        }
        return clone
    }

    @Override
    fun merge(other: Admin<*>?) {
        super.merge(other)
    }

    /////////////////
    @Override
    override fun equals(o: Object): Boolean {
        if (this === o) return true
        if (o !is B_LP_NL_O_P_S_SE_SL_Traverser<*>) return false
        if (!super.equals(o)) return false
        val that = o as B_LP_NL_O_P_S_SE_SL_Traverser<*>
        if (!nestedLoops.equals(that.nestedLoops)) return false
        return if (loopNames != null) loopNames.equals(that.loopNames) else that.loopNames == null
    }

    @Override
    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + nestedLoops.hashCode()
        result = 31 * result + if (loopNames != null) loopNames.hashCode() else 0
        return result
    }
}