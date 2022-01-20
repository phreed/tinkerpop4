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
class BigDecimalSerializer : SimpleTypeSerializer<BigDecimal?>(DataType.BIGDECIMAL) {
    @Override
    @Throws(IOException::class)
    protected fun readValue(buffer: Buffer?, context: GraphBinaryReader): BigDecimal {
        val scale: Int = context.readValue(buffer, Integer::class.java, false)
        val unscaled: BigInteger = context.readValue(buffer, BigInteger::class.java, false)
        return BigDecimal(unscaled, scale)
    }

    @Override
    @Throws(IOException::class)
    protected fun writeValue(value: BigDecimal, buffer: Buffer?, context: GraphBinaryWriter) {
        context.writeValue(value.scale(), buffer, false)
        context.writeValue(value.unscaledValue(), buffer, false)
    }
}