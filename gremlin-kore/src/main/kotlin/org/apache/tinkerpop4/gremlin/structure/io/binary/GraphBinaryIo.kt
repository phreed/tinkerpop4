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
package org.apache.tinkerpop4.gremlin.structure.io.binary

import org.apache.tinkerpop4.gremlin.structure.io.GraphReader

/**
 * This is a dummy implementation of [Io] which is only used in the context of helping to configure a
 * GraphBinary `MessageSerializer` with an [IoRegistry]. It's methods are not implemented.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class GraphBinaryIo : Io {
    @Override
    fun reader(): ReaderBuilder {
        throw UnsupportedOperationException("GraphBinaryIo is only used to support IoRegistry configuration - it's methods are not implemented")
    }

    @Override
    fun writer(): WriterBuilder {
        throw UnsupportedOperationException("GraphBinaryIo is only used to support IoRegistry configuration - it's methods are not implemented")
    }

    @Override
    fun mapper(): Mapper.Builder {
        throw UnsupportedOperationException("GraphBinaryIo is only used to support IoRegistry configuration - it's methods are not implemented")
    }

    @Override
    @Throws(IOException::class)
    fun writeGraph(file: String?) {
        throw UnsupportedOperationException("GraphBinaryIo is only used to support IoRegistry configuration - it's methods are not implemented")
    }

    @Override
    @Throws(IOException::class)
    fun readGraph(file: String?) {
        throw UnsupportedOperationException("GraphBinaryIo is only used to support IoRegistry configuration - it's methods are not implemented")
    }
}