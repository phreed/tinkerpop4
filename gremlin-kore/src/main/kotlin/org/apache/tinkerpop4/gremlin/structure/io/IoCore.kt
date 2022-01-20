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
package org.apache.tinkerpop4.gremlin.structure.io

import org.apache.tinkerpop4.gremlin.process.traversal.dsl.graph.GraphTraversalSource

/**
 * Constructs the core [Io.Builder] implementations enabling a bit of shorthand syntax by importing these
 * methods statically.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
@Deprecated
@Deprecated(
    """As of release 3.4.0, partially replaced by {@link GraphTraversalSource#io(String)}. Notice
  {@link GraphTraversalSource#io(String)} doesn't support read operation from {@code java.io.InputStream} or write
  operation to {@code java.io.OutputStream}. Thus for readers or writers which need this functionality are safe to
  use this deprecated method. There is no intention to remove this method unless all the functionality is replaced
  by the `io` step of {@link GraphTraversalSource}."""
)
object IoCore {
    /**
     * Creates a basic GraphML-based [Io.Builder].
     */
    fun graphml(): Io.Builder<GraphMLIo> {
        return GraphMLIo.build()
    }

    /**
     * Creates a basic GraphSON-based [Io.Builder].
     */
    fun graphson(): Io.Builder<GraphSONIo> {
        return GraphSONIo.build()
    }

    /**
     * Creates a basic Gryo-based [Io.Builder].
     */
    fun gryo(): Io.Builder<GryoIo> {
        return GryoIo.build()
    }

    @Throws(ClassNotFoundException::class, IllegalAccessException::class, InstantiationException::class)
    fun createIoBuilder(graphFormat: String?): Io.Builder {
        val ioBuilderClass: Class<Io.Builder> = Class.forName(graphFormat) as Class<Io.Builder>
        return ioBuilderClass.newInstance()
    }
}