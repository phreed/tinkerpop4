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

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class EdgeSerializer : SimpleTypeSerializer<Edge?>(DataType.EDGE) {
    @Override
    @Throws(IOException::class)
    protected fun readValue(buffer: Buffer?, context: GraphBinaryReader): Edge {
        val id: Object = context.read(buffer)
        val label: String = context.readValue(buffer, String::class.java, false)
        val inV = ReferenceVertex(
            context.read(buffer),
            context.readValue(buffer, String::class.java, false)
        )
        val outV = ReferenceVertex(
            context.read(buffer),
            context.readValue(buffer, String::class.java, false)
        )

        // discard the parent vertex - we only send "references so this should always be null, but will we change our
        // minds someday????
        context.read(buffer)

        // discard the properties - as we only send "references" this should always be null, but will we change our
        // minds some day????
        context.read(buffer)
        return ReferenceEdge(id, label, inV, outV)
    }

    @Override
    @Throws(IOException::class)
    protected fun writeValue(value: Edge, buffer: Buffer?, context: GraphBinaryWriter) {
        context.write(value.id(), buffer)
        context.writeValue(value.label(), buffer, false)
        context.write(value.inVertex().id(), buffer)
        context.writeValue(value.inVertex().label(), buffer, false)
        context.write(value.outVertex().id(), buffer)
        context.writeValue(value.outVertex().label(), buffer, false)

        // we don't serialize the parent Vertex for edges. they are "references", but we leave a place holder
        // here as an option for the future as we've waffled this soooooooooo many times now
        context.write(null, buffer)
        // we don't serialize properties for graph vertices/edges. they are "references", but we leave a place holder
        // here as an option for the future as we've waffled this soooooooooo many times now
        context.write(null, buffer)
    }
}