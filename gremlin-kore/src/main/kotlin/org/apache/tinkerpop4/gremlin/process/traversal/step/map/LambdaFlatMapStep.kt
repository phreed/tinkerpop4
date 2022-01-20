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

import org.apache.tinkerpop4.gremlin.process.traversal.Traversal
import org.apache.tinkerpop4.gremlin.process.traversal.Traverser
import org.apache.tinkerpop4.gremlin.process.traversal.step.LambdaHolder
import org.apache.tinkerpop4.gremlin.structure.util.StringFactory
import java.util.Iterator
import java.util.function.Function

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class LambdaFlatMapStep<S, E>(traversal: Traversal.Admin?, function: Function<Traverser<S>?, Iterator<E>?>) :
    FlatMapStep<S, E>(traversal), LambdaHolder {
    private val function: Function<Traverser<S>, Iterator<E>>

    init {
        this.function = function
    }

    fun getFunction(): Function<Traverser<S>, Iterator<E>> {
        return function
    }

    @Override
    protected fun flatMap(traverser: Traverser.Admin<S>?): Iterator<E> {
        return function.apply(traverser)
    }

    @Override
    override fun toString(): String {
        return StringFactory.stepString(this, function)
    }

    @Override
    override fun hashCode(): Int {
        return super.hashCode() xor function.hashCode()
    }
}