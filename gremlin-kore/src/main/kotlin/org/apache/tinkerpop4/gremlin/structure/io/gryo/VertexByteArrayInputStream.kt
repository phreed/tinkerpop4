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
package org.apache.tinkerpop4.gremlin.structure.io.gryo

import org.apache.tinkerpop4.gremlin.structure.Vertex

/**
 * An [InputStream] implementation that can independently process a Gryo file written with
 * [GryoWriter.writeVertices].
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class VertexByteArrayInputStream(inputStream: InputStream?) : FilterInputStream(inputStream) {
    /**
     * Read the bytes of the next [Vertex] in the stream. The returned
     * stream can then be passed to [GryoReader.readVertex].
     */
    @Throws(IOException::class)
    fun readVertexBytes(): ByteArrayOutputStream {
        val stream = ByteArrayOutputStream()
        val buffer: LinkedList<Byte> = LinkedList()
        var current: Int = read()
        while (current > -1 && (buffer.size() < 12 || !isMatch(buffer))) {
            stream.write(current)
            current = read()
            if (buffer.size() > 11) buffer.removeFirst()
            buffer.addLast(current.toByte())
        }
        stream.write(current)
        return stream
    }

    companion object {
        private val vertexTerminatorClass = byteArrayOf(15, 1, 1, 9)
        private val pattern: ByteArray =
            ByteBuffer.allocate(vertexTerminatorClass.size + 8).put(vertexTerminatorClass).putLong(4185403236219066774L)
                .array()

        private fun isMatch(input: List<Byte>): Boolean {
            for (i in pattern.indices) {
                if (pattern[i] != input[i]) {
                    return false
                }
            }
            return true
        }
    }
}