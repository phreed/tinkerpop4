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
class VertexStep<E : Element?>(
    traversal: Traversal.Admin?,
    returnClass: Class<E>,
    direction: Direction,
    vararg edgeLabels: String
) : FlatMapStep<Vertex?, E>(traversal), AutoCloseable, Configuring {
    protected var parameters: Parameters = Parameters()
    val edgeLabels: Array<String>
    private var direction: Direction
    private val returnClass: Class<E>

    init {
        this.direction = direction
        this.edgeLabels = edgeLabels
        this.returnClass = returnClass
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
    protected fun flatMap(traverser: Traverser.Admin<Vertex?>): Iterator<E> {
        return if (Vertex::class.java.isAssignableFrom(returnClass)) traverser.get()
            .vertices(direction, edgeLabels) else (traverser.get().edges(
            direction, edgeLabels
        ) as Iterator<E>)
    }

    fun getDirection(): Direction {
        return direction
    }

    fun getReturnClass(): Class<E> {
        return returnClass
    }

    fun reverseDirection() {
        direction = direction.opposite()
    }

    fun returnsVertex(): Boolean {
        return returnClass.equals(Vertex::class.java)
    }

    fun returnsEdge(): Boolean {
        return returnClass.equals(Edge::class.java)
    }

    @Override
    override fun toString(): String {
        return StringFactory.stepString(
            this,
            direction,
            Arrays.asList(edgeLabels),
            returnClass.getSimpleName().toLowerCase()
        )
    }

    @Override
    override fun hashCode(): Int {
        var result = super.hashCode() xor direction.hashCode() xor returnClass.hashCode()
        for (edgeLabel in edgeLabels) {
            result = result xor edgeLabel.hashCode()
        }
        return result
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