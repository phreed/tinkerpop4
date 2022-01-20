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

import org.apache.commons.configuration2.ConfigurationConverter

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class TraversalStrategySerializer : SimpleTypeSerializer<TraversalStrategy?>(DataType.TRAVERSALSTRATEGY) {
    @Override
    @Throws(IOException::class)
    protected fun readValue(buffer: Buffer?, context: GraphBinaryReader): TraversalStrategy {
        val clazz: Class<TraversalStrategy> = context.readValue(buffer, Class::class.java, false)
        val config: Map = context.readValue(buffer, Map::class.java, false)
        return TraversalStrategyProxy(clazz, MapConfiguration(config))
    }

    @Override
    @Throws(IOException::class)
    protected fun writeValue(value: TraversalStrategy, buffer: Buffer?, context: GraphBinaryWriter) {
        context.writeValue(value.getClass(), buffer, false)
        context.writeValue(translateToBytecode(ConfigurationConverter.getMap(value.getConfiguration())), buffer, false)
    }

    companion object {
        private fun translateToBytecode(conf: Map<Object, Object>): Map<Object, Object> {
            val newConf: Map<Object, Object> = LinkedHashMap(conf.size())
            conf.entrySet().forEach { entry ->
                if (entry.getValue() is Traversal) newConf.put(
                    entry.getKey(),
                    (entry.getValue() as Traversal).asAdmin().getBytecode()
                ) else newConf.put(entry.getKey(), entry.getValue())
            }
            return newConf
        }
    }
}