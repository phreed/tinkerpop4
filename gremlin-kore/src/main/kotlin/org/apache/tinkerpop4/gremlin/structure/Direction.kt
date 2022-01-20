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
package org.apache.tinkerpop4.gremlin.structure

/**
 * [Direction] is used to denote the direction of an [Edge] or location of a [Vertex] on an
 * [Edge]. For example:
 *
 *
 * <pre>
 * gremlin--knows--&gt;rexster
</pre> *
 * is an [Direction.OUT] [Edge] for Gremlin and an [Direction.IN] edge for Rexster. Moreover, given
 * that [Edge], Gremlin is the [Direction.OUT] [Vertex] and Rexster is the [Direction.IN]
 * [Vertex].
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
enum class Direction {
    /**
     * Refers to an outgoing direction.
     */
    OUT,

    /**
     * Refers to an incoming direction.
     */
    IN,

    /**
     * Refers to either direction ([.IN] or [.OUT]).
     */
    BOTH;

    /**
     * Produce the opposite representation of the current `Direction` enum.
     */
    fun opposite(): Direction {
        return if (this.equals(OUT)) IN else if (this.equals(
                IN
            )
        ) OUT else BOTH
    }

    companion object {
        /**
         * The actual direction of an [Edge] may only be [.IN] or [.OUT], as defined in this array.
         */
        val proper = arrayOf(OUT, IN)
    }
}