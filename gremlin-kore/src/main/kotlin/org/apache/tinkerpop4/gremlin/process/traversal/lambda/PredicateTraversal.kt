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
package org.apache.tinkerpop4.gremlin.process.traversal.lambda

import org.apache.tinkerpop4.gremlin.process.traversal.P
import org.apache.tinkerpop4.gremlin.process.traversal.Traverser
import org.apache.tinkerpop4.gremlin.process.traversal.util.FastNoSuchElementException
import java.util.function.Predicate

/**
 * @author Daniel Kuppitz (http://gremlin.guru)
 */
class PredicateTraversal<S>(value: Object) : AbstractLambdaTraversal<S, S>() {
    private val predicate: Predicate
    private var s: S? = null
    private var pass = false

    init {
        predicate = if (value is Predicate) value as Predicate else P.eq(value)
    }

    @Override
    operator fun next(): S {
        if (pass) return s
        throw FastNoSuchElementException.instance()
    }

    @Override
    operator fun hasNext(): Boolean {
        return pass
    }

    @Override
    fun addStart(start: Traverser.Admin<S>) {
        pass = predicate.test(start.get().also { s = it })
    }

    @Override
    override fun toString(): String {
        return "(" + predicate.toString().toString() + ")"
    }

    @Override
    override fun hashCode(): Int {
        return this.getClass().hashCode() xor predicate.hashCode()
    }
}