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
package org.apache.tinkerpop4.gremlin.process.computer

import org.apache.tinkerpop4.gremlin.structure.Graph

/**
 * The result of the [GraphComputer]'s computation. This is returned in a `Future` by
 * [GraphComputer.submit]. A GraphComputer computation yields two things: an updated view of the computed on
 * [Graph] and any computational sideEffects called [Memory].
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
interface ComputerResult : AutoCloseable {
    /**
     * Get the [Graph] computed as determined by [GraphComputer.Persist] and [GraphComputer.ResultGraph].
     *
     * @return The computed graph
     */
    fun graph(): Graph?

    /**
     * Get the GraphComputer's computational sideEffects known as [Memory].
     *
     * @return the computed memory
     */
    fun memory(): Memory?

    /**
     * Close the computed [GraphComputer] result. The semantics of "close" differ depending on the underlying implementation.
     * In general, when a [ComputerResult] is closed, the computed values are no longer available to the user.
     *
     * @throws Exception
     */
    @Override
    @Throws(Exception::class)
    fun close()
}