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
package org.apache.tinkerpop4.gremlin.structure.util.detached

import org.apache.tinkerpop4.gremlin.process.traversal.Path

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
object DetachedFactory {
    fun detach(vertex: Vertex?, withProperties: Boolean): DetachedVertex? {
        return if (vertex is DetachedVertex) vertex as DetachedVertex? else DetachedVertex(vertex, withProperties)
    }

    fun detach(edge: Edge?, withProperties: Boolean): DetachedEdge? {
        return if (edge is DetachedEdge) edge as DetachedEdge? else DetachedEdge(edge, withProperties)
    }

    fun <V> detach(vertexProperty: VertexProperty<V>?, withProperties: Boolean): DetachedVertexProperty? {
        return if (vertexProperty is DetachedVertexProperty) vertexProperty as DetachedVertexProperty? else DetachedVertexProperty(
            vertexProperty,
            withProperties
        )
    }

    fun <V> detach(property: Property<V>?): DetachedProperty<V>? {
        return if (property is DetachedProperty) property as DetachedProperty<V>? else DetachedProperty(property)
    }

    fun detach(path: Path?, withProperties: Boolean): DetachedPath? {
        return if (path is DetachedPath) path as DetachedPath? else DetachedPath(path, withProperties)
    }

    fun detach(element: Element, withProperties: Boolean): DetachedElement {
        return if (element is Vertex) detach(
            element as Vertex,
            withProperties
        ) else if (element is Edge) detach(
            element as Edge,
            withProperties
        ) else if (element is VertexProperty) detach(
            element as VertexProperty,
            withProperties
        ) else throw IllegalArgumentException("The provided argument is an unknown element: " + element + ':' + element.getClass())
    }

    fun <D> detach(`object`: Object, withProperties: Boolean): D {
        return if (`object` is Element) {
            detach(`object` as Element, withProperties) as D
        } else if (`object` is Property) {
            detach(`object` as Property)
        } else if (`object` is Path) {
            detach(`object` as Path, withProperties) as D
        } else if (`object` is List) {
            val list: List = ArrayList((`object` as List).size())
            for (item in `object`) {
                list.add(detach<D>(item, withProperties))
            }
            list
        } else if (`object` is BulkSet) {
            val set = BulkSet()
            for (entry in (`object` as BulkSet<Object?>).asBulk().entrySet()) {
                set.add(detach(entry.getKey(), withProperties), entry.getValue())
            }
            set
        } else if (`object` is Set) {
            val set: Set =
                if (`object` is LinkedHashSet) LinkedHashSet((`object` as Set).size()) else HashSet((`object` as Set).size())
            for (item in `object`) {
                set.add(detach<D>(item, withProperties))
            }
            set
        } else if (`object` is Map) {
            val map: Map =
                if (`object` is Tree) Tree() else if (`object` is LinkedHashMap) LinkedHashMap((`object` as Map).size()) else HashMap(
                    (`object` as Map).size()
                )
            for (entry in (`object` as Map<Object?, Object?>).entrySet()) {
                map.put(detach(entry.getKey(), withProperties), detach(entry.getValue(), withProperties))
            }
            map
        } else {
            `object`
        }
    }
}