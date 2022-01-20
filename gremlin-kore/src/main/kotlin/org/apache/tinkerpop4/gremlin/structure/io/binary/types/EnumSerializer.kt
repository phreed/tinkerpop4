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
 * Generalized serializer for {#code Enum} value types.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class EnumSerializer<E : Enum?> private constructor(dataType: DataType, readFunc: Function<String, E>) :
    SimpleTypeSerializer<E>(dataType) {
    private val readFunc: Function<String, E>

    init {
        this.readFunc = readFunc
    }

    @Override
    @Throws(IOException::class)
    protected fun readValue(buffer: Buffer?, context: GraphBinaryReader): E {
        return readFunc.apply(context.read(buffer))
    }

    @Override
    @Throws(IOException::class)
    protected fun writeValue(value: E, buffer: Buffer?, context: GraphBinaryWriter) {
        context.write(value.name(), buffer)
    }

    companion object {
        val BarrierSerializer: EnumSerializer<SackFunctions.Barrier> =
            EnumSerializer<Enum>(DataType.BARRIER, SackFunctions.Barrier::valueOf)
        val CardinalitySerializer: EnumSerializer<VertexProperty.Cardinality> =
            EnumSerializer<Enum>(DataType.CARDINALITY, VertexProperty.Cardinality::valueOf)
        val ColumnSerializer: EnumSerializer<Column> = EnumSerializer<Enum>(DataType.COLUMN, Column::valueOf)
        val DirectionSerializer: EnumSerializer<Direction> =
            EnumSerializer<Enum>(DataType.DIRECTION, Direction::valueOf)
        val OperatorSerializer: EnumSerializer<Operator> = EnumSerializer<Enum>(DataType.OPERATOR, Operator::valueOf)
        val OrderSerializer: EnumSerializer<Order> = EnumSerializer<Enum>(DataType.ORDER, Order::valueOf)
        val PickSerializer: EnumSerializer<Pick> =
            EnumSerializer<Enum>(DataType.PICK, TraversalOptionParent.Pick::valueOf)
        val PopSerializer: EnumSerializer<Pop> = EnumSerializer<Enum>(DataType.POP, Pop::valueOf)
        val ScopeSerializer: EnumSerializer<Scope> = EnumSerializer<Enum>(DataType.SCOPE, Scope::valueOf)
        val TSerializer: EnumSerializer<T> = EnumSerializer<Enum>(DataType.T, T::valueOf)
    }
}