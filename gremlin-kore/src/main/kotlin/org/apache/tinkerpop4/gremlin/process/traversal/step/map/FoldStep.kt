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

import org.apache.tinkerpop4.gremlin.process.traversal.Operator

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class FoldStep<S, E>(traversal: Traversal.Admin?, seed: Supplier<E>, foldFunction: BiFunction<E, S, E>?) :
    ReducingBarrierStep<S, E>(traversal) {
    val isListFold: Boolean

    constructor(traversal: Traversal.Admin?) : this(
        traversal,
        ArrayListSupplier.instance() as Supplier,
        Operator.addAll as BiFunction
    ) {
    }

    init {
        isListFold = seed.get() is Collection
        this.setSeedSupplier(seed)
        this.setReducingBiOperator(FoldBiOperator<E>(foldFunction))
    }

    @Override
    fun projectTraverser(traverser: Traverser.Admin<S>): E {
        return if (isListFold) {
            val list: List<S> = ArrayList()
            for (i in 0 until traverser.bulk()) {
                list.add(traverser.get())
            }
            list as E
        } else {
            traverser.get()
        }
    }

    @get:Override
    val requirements: Set<Any>
        get() = REQUIREMENTS

    class FoldBiOperator<E> : BinaryOperator<E>, Serializable {
        private var biFunction: BiFunction? = null

        private constructor() {
            // for serialization purposes
        }

        constructor(biFunction: BiFunction?) {
            this.biFunction = biFunction
        }

        @Override
        fun apply(seed: E, other: E): E {
            return biFunction.apply(seed, other)
        }
    }

    companion object {
        private val REQUIREMENTS: Set<TraverserRequirement> = EnumSet.of(TraverserRequirement.OBJECT)
    }
}