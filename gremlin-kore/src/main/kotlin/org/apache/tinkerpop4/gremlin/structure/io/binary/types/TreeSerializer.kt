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
package org.apache.tinkerpop4.gremlin.structure.io.binary.types

import org.apache.tinkerpop4.gremlin.structure.io.binary.DataType

class TreeSerializer : SimpleTypeSerializer<Tree?>(DataType.TREE) {
    @Override
    @Throws(IOException::class)
    protected fun readValue(buffer: Buffer, context: GraphBinaryReader): Tree {
        val length: Int = buffer.readInt()
        val result = Tree()
        for (i in 0 until length) {
            result.put(context.read(buffer), context.readValue(buffer, Tree::class.java, false))
        }
        return result
    }

    @Override
    @Throws(IOException::class)
    protected fun writeValue(value: Tree, buffer: Buffer, context: GraphBinaryWriter) {
        buffer.writeInt(value.size())
        for (key in value.keySet()) {
            context.write(key, buffer)
            context.writeValue(value.get(key), buffer, false)
        }
    }
}