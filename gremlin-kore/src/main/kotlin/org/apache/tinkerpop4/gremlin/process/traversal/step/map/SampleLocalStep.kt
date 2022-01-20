/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.tinkerpop4.gremlin.process.traversal.step.map

import org.apache.tinkerpop4.gremlin.process.traversal.Traversal
import org.apache.tinkerpop4.gremlin.process.traversal.Traverser
import org.apache.tinkerpop4.gremlin.process.traversal.step.Seedable
import org.apache.tinkerpop4.gremlin.process.traversal.traverser.TraverserRequirement
import java.util.ArrayList
import java.util.Collection
import java.util.Collections
import java.util.LinkedHashMap
import java.util.List
import java.util.Map
import java.util.Random
import java.util.Set

/**
 * @author Daniel Kuppitz (http://gremlin.guru)
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class SampleLocalStep<S>(traversal: Traversal.Admin?, private val amountToSample: Int) : ScalarMapStep<S, S>(traversal),
    Seedable {
    private val random: Random = Random()
    @Override
    fun resetSeed(seed: Long) {
        random.setSeed(seed)
    }

    @Override
    protected fun map(traverser: Traverser.Admin<S>): S {
        val start: S = traverser.get()
        return if (start is Map) {
            mapMap(start as Map)
        } else if (start is Collection) {
            mapCollection(start as Collection)
        } else {
            start
        }
    }

    private fun mapCollection(collection: Collection): S {
        if (collection.size() <= amountToSample) return collection
        val original: List<S> = ArrayList(collection)
        val target: List<S> = ArrayList()
        while (target.size() < amountToSample) {
            target.add(original.remove(random.nextInt(original.size())))
        }
        return target as S
    }

    private fun mapMap(map: Map): S {
        if (map.size() <= amountToSample) return map
        val original: List<Map.Entry> = ArrayList(map.entrySet())
        val target: Map = LinkedHashMap(amountToSample)
        while (target.size() < amountToSample) {
            val entry: Map.Entry = original.remove(random.nextInt(original.size()))
            target.put(entry.getKey(), entry.getValue())
        }
        return target
    }

    @get:Override
    val requirements: Set<Any>
        get() = Collections.singleton(TraverserRequirement.OBJECT)

    @Override
    override fun hashCode(): Int {
        return super.hashCode() xor amountToSample
    }
}