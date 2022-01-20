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
package org.apache.tinkerpop4.gremlin.structure.io.graphson

import org.apache.tinkerpop4.gremlin.process.traversal.Operator

/**
 * GraphSON 2.0 `TypeSerializer`.
 *
 * @author Kevin Gallardo (https://kgdo.me)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class GraphSONTypeSerializerV3d0 internal constructor(
    idRes: TypeIdResolver?, propertyName: String?, typeInfo: TypeInfo?,
    valuePropertyName: String?
) : AbstractGraphSONTypeSerializer(idRes, propertyName, typeInfo, valuePropertyName) {
    @Override
    @Throws(IOException::class)
    fun writeTypePrefix(jsonGenerator: JsonGenerator, writableTypeId: WritableTypeId): WritableTypeId {
        if (writableTypeId.valueShape === JsonToken.START_OBJECT) {
            if (writableTypeId.forValue is Map) {
                writeTypePrefix(
                    jsonGenerator,
                    getTypeIdResolver().idFromValueAndType(
                        writableTypeId.forValue,
                        getClassFromObject(writableTypeId.forValue)
                    )
                )
                jsonGenerator.writeStartArray()
            } else {
                jsonGenerator.writeStartObject()
            }
        } else if (writableTypeId.valueShape === JsonToken.START_ARRAY) {
            if (writableTypeId.forValue is List || writableTypeId.forValue is Set) {
                writeTypePrefix(
                    jsonGenerator,
                    getTypeIdResolver().idFromValueAndType(
                        writableTypeId.forValue,
                        getClassFromObject(writableTypeId.forValue)
                    )
                )
                jsonGenerator.writeStartArray()
            } else {
                jsonGenerator.writeStartArray()
            }
        } else if (canWriteTypeId()) {
            writeTypePrefix(
                jsonGenerator,
                getTypeIdResolver().idFromValueAndType(
                    writableTypeId.forValue,
                    getClassFromObject(writableTypeId.forValue)
                )
            )
        } else {
            throw IllegalStateException("Could not write prefix: shape[" + writableTypeId.valueShape.toString() + "] value[" + writableTypeId.forValue.toString() + "]")
        }
        return writableTypeId
    }

    @Override
    @Throws(IOException::class)
    fun writeTypeSuffix(jsonGenerator: JsonGenerator, writableTypeId: WritableTypeId): WritableTypeId {
        if (writableTypeId.valueShape === JsonToken.START_OBJECT) {
            if (writableTypeId.forValue is Map) {
                jsonGenerator.writeEndArray()
                writeTypeSuffix(jsonGenerator)
            } else {
                jsonGenerator.writeEndObject()
            }
        } else if (writableTypeId.valueShape === JsonToken.START_ARRAY) {
            if (writableTypeId.forValue is List || writableTypeId.forValue is Set) {
                jsonGenerator.writeEndArray()
                writeTypeSuffix(jsonGenerator)
            } else {
                jsonGenerator.writeEndArray()
            }
        } else if (canWriteTypeId()) {
            writeTypeSuffix(jsonGenerator)
        } else {
            throw IllegalStateException("Could not write suffix: shape[" + writableTypeId.valueShape.toString() + "] value[" + writableTypeId.forValue.toString() + "]")
        }
        return writableTypeId
    }

    @Override
    protected fun getClassFromObject(o: Object): Class {
        val c: Class = o.getClass()
        if (classMap.containsKey(c)) return classMap.get(c)
        val mapped: Class
        if (Map::class.java.isAssignableFrom(c)) {
            if (Tree::class.java.isAssignableFrom(c)) mapped = Tree::class.java else mapped = Map::class.java
        } else if (List::class.java.isAssignableFrom(c)) mapped =
            List::class.java else if (Set::class.java.isAssignableFrom(c) && !BulkSet::class.java.isAssignableFrom(c)) // we have special handling of BulkSet
            mapped = Set::class.java else if (Vertex::class.java.isAssignableFrom(c)) mapped =
            Vertex::class.java else if (Edge::class.java.isAssignableFrom(c)) mapped =
            Edge::class.java else if (Path::class.java.isAssignableFrom(c)) mapped =
            Path::class.java else if (VertexProperty::class.java.isAssignableFrom(c)) mapped =
            VertexProperty::class.java else if (Metrics::class.java.isAssignableFrom(c)) mapped =
            Metrics::class.java else if (TraversalMetrics::class.java.isAssignableFrom(c)) mapped =
            TraversalMetrics::class.java else if (Property::class.java.isAssignableFrom(c)) mapped =
            Property::class.java else if (ByteBuffer::class.java.isAssignableFrom(c)) mapped =
            ByteBuffer::class.java else if (InetAddress::class.java.isAssignableFrom(c)) mapped =
            InetAddress::class.java else if (Traverser::class.java.isAssignableFrom(c)) mapped =
            Traverser::class.java else if (Lambda::class.java.isAssignableFrom(c)) mapped =
            Lambda::class.java else if (VertexProperty.Cardinality::class.java.isAssignableFrom(c)) mapped =
            VertexProperty.Cardinality::class.java else if (Column::class.java.isAssignableFrom(c)) mapped =
            Column::class.java else if (Direction::class.java.isAssignableFrom(c)) mapped =
            Direction::class.java else if (Operator::class.java.isAssignableFrom(c)) mapped =
            Operator::class.java else if (Order::class.java.isAssignableFrom(c)) mapped =
            Order::class.java else if (Pop::class.java.isAssignableFrom(c)) mapped =
            Pop::class.java else if (SackFunctions.Barrier::class.java.isAssignableFrom(c)) mapped =
            SackFunctions.Barrier::class.java else if (Pick::class.java.isAssignableFrom(c)) mapped =
            Pick::class.java else if (Scope::class.java.isAssignableFrom(c)) mapped =
            Scope::class.java else if (T::class.java.isAssignableFrom(c)) mapped = T::class.java else mapped = c
        classMap.put(c, mapped)
        return mapped
    }
}