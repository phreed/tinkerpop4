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

import org.apache.tinkerpop4.gremlin.process.traversal.Path

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
object ReferenceFactory {
    fun detach(vertex: Vertex?): ReferenceVertex? {
        return if (vertex is ReferenceVertex) vertex as ReferenceVertex? else ReferenceVertex(vertex)
    }

    fun detach(edge: Edge?): ReferenceEdge? {
        return if (edge is ReferenceEdge) edge as ReferenceEdge? else ReferenceEdge(edge)
    }

    fun <V> detach(vertexProperty: VertexProperty<V>?): ReferenceVertexProperty? {
        return if (vertexProperty is ReferenceVertexProperty) vertexProperty as ReferenceVertexProperty? else ReferenceVertexProperty(
            vertexProperty
        )
    }

    fun <V> detach(property: Property<V>?): ReferenceProperty<V>? {
        return if (property is ReferenceProperty) property as ReferenceProperty? else ReferenceProperty(property)
    }

    fun detach(path: Path?): ReferencePath? {
        return if (path is ReferencePath) path as ReferencePath? else ReferencePath(path)
    }

    fun detach(element: Element): ReferenceElement {
        return if (element is Vertex) detach(element as Vertex) else if (element is Edge) detach(
            element as Edge
        ) else if (element is VertexProperty) detach(element as VertexProperty) else throw IllegalArgumentException("The provided argument is an unknown element: " + element + ':' + element.getClass())
    }

    fun <D> detach(`object`: Object): D {
        return if (`object` is Element) {
            detach(`object` as Element) as D
        } else if (`object` is Property) {
            detach(`object` as Property) as D
        } else if (`object` is Path) {
            detach(`object` as Path) as D
        } else if (`object` is List) {
            val list: List = ArrayList((`object` as List).size())
            for (item in `object`) {
                list.add(detach<D>(item))
            }
            list
        } else if (`object` is BulkSet) {
            val set = BulkSet()
            for (entry in (`object` as BulkSet<Object?>).asBulk().entrySet()) {
                set.add(detach(entry.getKey()), entry.getValue())
            }
            set
        } else if (`object` is Set) {
            val set: Set =
                if (`object` is LinkedHashSet) LinkedHashSet((`object` as Set).size()) else HashSet((`object` as Set).size())
            for (item in `object`) {
                set.add(detach<D>(item))
            }
            set
        } else if (`object` is Map) {
            val map: Map =
                if (`object` is Tree) Tree() else if (`object` is LinkedHashMap) LinkedHashMap((`object` as Map).size()) else HashMap(
                    (`object` as Map).size()
                )
            for (entry in (`object` as Map<Object?, Object?>).entrySet()) {
                map.put(detach(entry.getKey()), detach(entry.getValue()))
            }
            map
        } else {
            `object`
        }
    }
}