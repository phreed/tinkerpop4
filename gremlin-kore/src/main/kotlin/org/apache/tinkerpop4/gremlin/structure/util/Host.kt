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
package org.apache.tinkerpop4.gremlin.structure.util

import org.apache.tinkerpop4.gremlin.structure.Edge
import org.apache.tinkerpop4.gremlin.structure.Property
import org.apache.tinkerpop4.gremlin.structure.Vertex

/**
 * A marker interface that identifies an object as something that an [Attachable] can connect to.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
interface Host {
    companion object {
        /**
         * Extracts the [Vertex] that is holding the specified object.
         *
         * @throws IllegalStateException if the object is not a graph element type
         */
        fun getHostingVertex(`object`: Object): Vertex? {
            var obj: Object = `object`
            while (true) {
                obj =
                    if (obj is Vertex) return obj as Vertex else if (obj is Edge) return (obj as Edge).outVertex() else if (obj is Property) (obj as Property).element() else throw IllegalStateException(
                        "The host of the object is unknown: " + obj.toString() + ':' + obj.getClass().getCanonicalName()
                    )
            }
        }
    }
}