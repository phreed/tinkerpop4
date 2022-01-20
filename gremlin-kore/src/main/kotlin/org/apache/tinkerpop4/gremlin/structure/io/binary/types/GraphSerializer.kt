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

import org.apache.commons.configuration2.BaseConfiguration

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class GraphSerializer : SimpleTypeSerializer<Graph?>(DataType.GRAPH) {
    @Override
    @Throws(IOException::class)
    protected fun readValue(buffer: Buffer?, context: GraphBinaryReader): Graph {
        if (null == openMethod) throw IOException("TinkerGraph is an optional dependency to gremlin-driver - if deserializing Graph instances it must be explicitly added as a dependency")
        val conf: Configuration = BaseConfiguration()
        conf.setProperty("gremlin.tinkergraph.defaultVertexPropertyCardinality", "list")
        return try {
            val graph: Graph = openMethod.invoke(null, conf) as Graph
            val vertexCount: Int = context.readValue(buffer, Integer::class.java, false)
            for (ix in 0 until vertexCount) {
                val v: Vertex = graph.addVertex(
                    T.id,
                    context.read(buffer),
                    T.label,
                    context.readValue(buffer, String::class.java, false)
                )
                val vertexPropertyCount: Int = context.readValue(buffer, Integer::class.java, false)
                for (iy in 0 until vertexPropertyCount) {
                    val id: Object = context.read(buffer)
                    val label: String = context.readValue(buffer, String::class.java, false)
                    val `val`: Object = context.read(buffer)
                    context.read(buffer) // toss parent as it's always null
                    val vp: VertexProperty<Object> = v.property(VertexProperty.Cardinality.list, label, `val`, T.id, id)
                    val edgeProperties: List<Property> = context.readValue(buffer, ArrayList::class.java, false)
                    for (p in edgeProperties) {
                        vp.property(p.key(), p.value())
                    }
                }
            }
            val edgeCount: Int = context.readValue(buffer, Integer::class.java, false)
            for (ix in 0 until edgeCount) {
                val id: Object = context.read(buffer)
                val label: String = context.readValue(buffer, String::class.java, false)
                val inId: Object = context.read(buffer)
                val inV: Vertex = graph.vertices(inId).next()
                context.read(buffer) // toss in label - always null in this context
                val outId: Object = context.read(buffer)
                val outV: Vertex = graph.vertices(outId).next()
                context.read(buffer) // toss in label - always null in this context
                context.read(buffer) // toss parent - never present as it's just a placeholder
                val e: Edge = outV.addEdge(label, inV, T.id, id)
                val edgeProperties: List<Property> = context.readValue(buffer, ArrayList::class.java, false)
                for (p in edgeProperties) {
                    e.property(p.key(), p.value())
                }
            }
            graph
        } catch (ex: Exception) {
            // famous last words - can't happen
            throw IOException(ex)
        }
    }

    @Override
    @Throws(IOException::class)
    protected fun writeValue(value: Graph, buffer: Buffer, context: GraphBinaryWriter) {
        // this kinda looks scary memory-wise, but GraphBinary is about network derser so we are dealing with a
        // graph instance that should live in memory already - not expecting "big" stuff here.
        val vertexList: List<Vertex> = IteratorUtils.list(value.vertices())
        val edgeList: List<Edge> = IteratorUtils.list(value.edges())
        context.writeValue(vertexList.size(), buffer, false)
        for (v in vertexList) {
            writeVertex(buffer, context, v)
        }
        context.writeValue(edgeList.size(), buffer, false)
        for (e in edgeList) {
            writeEdge(buffer, context, e)
        }
    }

    @Throws(IOException::class)
    private fun writeVertex(buffer: Buffer, context: GraphBinaryWriter, vertex: Vertex) {
        val vertexProperties: List<VertexProperty<Object>> = IteratorUtils.list(vertex.properties())
        context.write(vertex.id(), buffer)
        context.writeValue(vertex.label(), buffer, false)
        context.writeValue(vertexProperties.size(), buffer, false)
        for (vp in vertexProperties) {
            context.write(vp.id(), buffer)
            context.writeValue(vp.label(), buffer, false)
            context.write(vp.value(), buffer)

            // maintain the VertexProperty format we have with this empty parent.........
            context.write(null, buffer)

            // write those properties out using the standard Property serializer
            context.writeValue(IteratorUtils.list(vp.properties()), buffer, false)
        }
    }

    @Throws(IOException::class)
    private fun writeEdge(buffer: Buffer, context: GraphBinaryWriter, edge: Edge) {
        context.write(edge.id(), buffer)
        context.writeValue(edge.label(), buffer, false)
        context.write(edge.inVertex().id(), buffer)

        // vertex labels aren't needed but maintaining the Edge form that we have
        context.write(null, buffer)
        context.write(edge.outVertex().id(), buffer)

        // vertex labels aren't needed but maintaining the Edge form that we have
        context.write(null, buffer)

        // maintain the Edge format we have with this empty parent..................
        context.write(null, buffer)

        // write those properties out using the standard Property serializer
        context.writeValue(IteratorUtils.list(edge.properties()), buffer, false)
    }

    companion object {
        private val openMethod: Method? = detectGraphOpenMethod()
        private fun indexedVertexProperties(v: Vertex): Map<String, List<VertexProperty>> {
            val index: Map<String, List<VertexProperty>> = HashMap()
            v.properties().forEachRemaining { vp ->
                if (!index.containsKey(vp.key())) {
                    index.put(vp.key(), ArrayList())
                }
                index[vp.key()].add(vp)
            }
            return index
        }

        private fun detectGraphOpenMethod(): Method? {
            val graphClazz: Class<*> = detectTinkerGraph() ?: return null

            // if no class then no method to lookup
            return try {
                graphClazz.getMethod("open", Configuration::class.java)
            } catch (nsme: NoSuchMethodException) {
                // famous last words - can't happen
                throw IllegalStateException(nsme)
            }
        }

        private fun detectTinkerGraph(): Class<*>? {
            // the java driver defaults to using TinkerGraph to deserialize Graph instances. if TinkerGraph isn't present
            // on the path, that's cool, users just won't be able to deserialize that
            return try {
                Class.forName("org.apache.tinkerpop4.gremlin.tinkergraph.structure.TinkerGraph")
            } catch (cnfe: ClassNotFoundException) {
                null
            }
        }
    }
}