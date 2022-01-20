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
package org.apache.tinkerpop4.gremlin.process.traversal.step.filter

import org.apache.tinkerpop4.gremlin.process.traversal.Traversal
import org.apache.tinkerpop4.gremlin.process.traversal.Traverser
import org.apache.tinkerpop4.gremlin.process.traversal.step.Seedable
import org.apache.tinkerpop4.gremlin.process.traversal.traverser.TraverserRequirement
import org.apache.tinkerpop4.gremlin.structure.util.StringFactory
import java.util.Collections
import java.util.Random
import java.util.Set

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class CoinStep<S>(traversal: Traversal.Admin?, private val probability: Double) : FilterStep<S>(traversal), Seedable {
    private val random: Random = Random()
    @Override
    fun resetSeed(seed: Long) {
        random.setSeed(seed)
    }

    @Override
    protected fun filter(traverser: Traverser.Admin<S>): Boolean {
        var newBulk = 0L
        if (traverser.bulk() < 100) {
            for (i in 0 until traverser.bulk()) {
                if (probability >= random.nextDouble()) newBulk++
            }
        } else {
            newBulk = Math.round(probability * traverser.bulk())
        }
        if (0L == newBulk) return false
        traverser.setBulk(newBulk)
        return true
    }

    @Override
    override fun toString(): String {
        return StringFactory.stepString(this, probability)
    }

    @Override
    override fun hashCode(): Int {
        return super.hashCode() xor Double.hashCode(probability)
    }

    @get:Override
    val requirements: Set<Any>
        get() = Collections.singleton(TraverserRequirement.BULK)
}