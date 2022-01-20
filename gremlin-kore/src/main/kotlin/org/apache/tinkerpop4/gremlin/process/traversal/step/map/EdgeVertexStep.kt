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

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class EdgeVertexStep(traversal: Traversal.Admin?, direction: Direction) : FlatMapStep<Edge?, Vertex?>(traversal),
    AutoCloseable, Configuring {
    protected var parameters: Parameters = Parameters()
    protected var direction: Direction

    init {
        this.direction = direction
    }

    @Override
    fun getParameters(): Parameters {
        return parameters
    }

    @Override
    fun configure(vararg keyValues: Object?) {
        parameters.set(null, keyValues)
    }

    @Override
    protected fun flatMap(traverser: Traverser.Admin<Edge?>): Iterator<Vertex> {
        return traverser.get().vertices(direction)
    }

    @Override
    override fun toString(): String {
        return StringFactory.stepString(this, direction)
    }

    @Override
    override fun hashCode(): Int {
        return super.hashCode() xor direction.hashCode()
    }

    fun getDirection(): Direction {
        return direction
    }

    fun reverseDirection() {
        direction = direction.opposite()
    }

    @get:Override
    val requirements: Set<Any>
        get() = Collections.singleton(TraverserRequirement.OBJECT)

    @Override
    @Throws(Exception::class)
    fun close() {
        closeIterator()
    }
}