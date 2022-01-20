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

import org.apache.tinkerpop4.gremlin.process.traversal.P
import org.apache.tinkerpop4.gremlin.process.traversal.Path
import org.apache.tinkerpop4.gremlin.process.traversal.util.ConnectiveP
import org.apache.tinkerpop4.gremlin.structure.Edge
import org.apache.tinkerpop4.gremlin.structure.Property
import org.apache.tinkerpop4.gremlin.structure.Vertex
import org.apache.tinkerpop4.gremlin.structure.VertexProperty
import org.apache.tinkerpop4.gremlin.structure.util.detached.DetachedEdge
import org.apache.tinkerpop4.gremlin.structure.util.detached.DetachedPath
import org.apache.tinkerpop4.gremlin.structure.util.detached.DetachedProperty
import org.apache.tinkerpop4.gremlin.structure.util.detached.DetachedVertex
import org.apache.tinkerpop4.gremlin.structure.util.detached.DetachedVertexProperty
import org.apache.tinkerpop4.gremlin.structure.util.reference.ReferenceEdge
import org.apache.tinkerpop4.gremlin.structure.util.reference.ReferencePath
import org.apache.tinkerpop4.gremlin.structure.util.reference.ReferenceProperty
import org.apache.tinkerpop4.gremlin.structure.util.reference.ReferenceVertex
import org.apache.tinkerpop4.gremlin.structure.util.reference.ReferenceVertexProperty
import org.apache.tinkerpop4.gremlin.util.function.Lambda
import java.net.InetAddress
import java.nio.ByteBuffer

/**
 * [AbstractGryoClassResolver] for Gryo 1.0.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class GryoClassResolverV1d0 : AbstractGryoClassResolver() {
    @Override
    fun coerceType(clazz: Class): Class {
        // force all instances of Vertex, Edge, VertexProperty, etc. to their respective interface
        val type: Class
        type =
            if (!ReferenceVertex::class.java.isAssignableFrom(clazz) && !DetachedVertex::class.java.isAssignableFrom(
                    clazz
                ) && Vertex::class.java.isAssignableFrom(
                    clazz
                )
            ) Vertex::class.java else if (!ReferenceEdge::class.java.isAssignableFrom(clazz) && !DetachedEdge::class.java.isAssignableFrom(
                    clazz
                ) && Edge::class.java.isAssignableFrom(clazz)
            ) Edge::class.java else if (!ReferenceVertexProperty::class.java.isAssignableFrom(clazz) && !DetachedVertexProperty::class.java.isAssignableFrom(
                    clazz
                ) && VertexProperty::class.java.isAssignableFrom(clazz)
            ) VertexProperty::class.java else if (!ReferenceProperty::class.java.isAssignableFrom(clazz) && !DetachedProperty::class.java.isAssignableFrom(
                    clazz
                ) && !DetachedVertexProperty::class.java.isAssignableFrom(clazz) && !ReferenceVertexProperty::class.java.isAssignableFrom(
                    clazz
                ) && Property::class.java.isAssignableFrom(clazz)
            ) Property::class.java else if (!ReferencePath::class.java.isAssignableFrom(clazz) && !DetachedPath::class.java.isAssignableFrom(
                    clazz
                ) && Path::class.java.isAssignableFrom(clazz)
            ) Path::class.java else if (Lambda::class.java.isAssignableFrom(clazz)) Lambda::class.java else if (ByteBuffer::class.java.isAssignableFrom(
                    clazz
                )
            ) ByteBuffer::class.java else if (Class::class.java.isAssignableFrom(clazz)) Class::class.java else if (InetAddress::class.java.isAssignableFrom(
                    clazz
                )
            ) InetAddress::class.java else if (ConnectiveP::class.java.isAssignableFrom(clazz)) P::class.java else clazz
        return type
    }
}