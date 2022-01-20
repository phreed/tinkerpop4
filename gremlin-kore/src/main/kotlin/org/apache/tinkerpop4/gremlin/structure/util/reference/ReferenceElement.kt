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
package org.apache.tinkerpop4.gremlin.structure.util.reference

import org.apache.tinkerpop4.gremlin.process.computer.util.ComputerGraph

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
abstract class ReferenceElement<E : Element?> : Element, Serializable, Attachable<E> {
    protected var id: Object? = null
    protected var label: String? = null

    protected constructor() {}
    protected constructor(id: Object?, label: String?) {
        this.id = id
        this.label = label
    }

    constructor(element: Element) {
        id = element.id()
        try {
            //Exception creation takes too much time, return default values for known types
            if (element is ComputerAdjacentVertex) {
                label = Vertex.DEFAULT_LABEL
            } else {
                label = element.label()
            }
        } catch (e: UnsupportedOperationException) {
            if (element is Vertex) label = Vertex.DEFAULT_LABEL else if (element is Edge) label =
                Edge.DEFAULT_LABEL else label = VertexProperty.DEFAULT_LABEL
        }
    }

    @Override
    fun id(): Object? {
        return id
    }

    @Override
    fun label(): String? {
        return label
    }

    @Override
    fun graph(): Graph {
        return EmptyGraph.instance()
    }

    @Override
    override fun hashCode(): Int {
        return ElementHelper.hashCode(this)
    }

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
    override fun equals(other: Object?): Boolean {
        return ElementHelper.areEqual(this, other)
    }

    fun get(): E {
        return this as E
    }
}