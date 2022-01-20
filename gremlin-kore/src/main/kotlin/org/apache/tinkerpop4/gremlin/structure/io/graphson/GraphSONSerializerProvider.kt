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

import org.apache.tinkerpop.shaded.jackson.databind.JsonSerializer

/**
 * Implementation of the `DefaultSerializerProvider` for Jackson that uses the `ToStringSerializer` for
 * unknown types.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
internal class GraphSONSerializerProvider : DefaultSerializerProvider {
    private val unknownTypeSerializer: JsonSerializer<Object>

    constructor(version: GraphSONVersion) : super() {
        if (version === GraphSONVersion.V1_0) {
            setDefaultKeySerializer(GraphSONKeySerializer())
            unknownTypeSerializer = ToStringSerializer()
        } else if (version === GraphSONVersion.V2_0) {
            setDefaultKeySerializer(GraphSONKeySerializer())
            unknownTypeSerializer = ToStringGraphSONSerializer()
        } else {
            unknownTypeSerializer = ToStringGraphSONSerializer()
        }
    }

    protected constructor(
        src: SerializerProvider?,
        config: SerializationConfig?, f: SerializerFactory?,
        unknownTypeSerializer: JsonSerializer<Object>
    ) : super(src, config, f) {
        this.unknownTypeSerializer = unknownTypeSerializer
    }

    @Override
    fun getUnknownTypeSerializer(aClass: Class<*>?): JsonSerializer<Object> {
        return unknownTypeSerializer
    }

    @Override
    fun createInstance(
        config: SerializationConfig?,
        jsf: SerializerFactory?
    ): GraphSONSerializerProvider {
        // createInstance is called pretty often to create a new SerializerProvider
        // we give it the unknownTypeSerializer that we had in the first place,
        // when the object was first constructed through the public constructor
        // that has a GraphSONVersion.
        return GraphSONSerializerProvider(this, config, jsf, unknownTypeSerializer)
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}