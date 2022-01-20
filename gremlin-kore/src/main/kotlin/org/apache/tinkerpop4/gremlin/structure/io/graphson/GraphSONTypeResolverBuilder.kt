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

import org.apache.tinkerpop.shaded.jackson.databind.DeserializationConfig

/**
 * Creates the Type serializers as well as the Typed deserializers that will be provided to the serializers and
 * deserializers. Contains the typeInfo level that should be provided by the GraphSONMapper.
 *
 * @author Kevin Gallardo (https://kgdo.me)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class GraphSONTypeResolverBuilder(version: GraphSONVersion) : StdTypeResolverBuilder() {
    private var typeInfo: TypeInfo? = null
    private var valuePropertyName: String? = null
    private val version: GraphSONVersion
    private val typeValidator: PolymorphicTypeValidator = BasicPolymorphicTypeValidator.builder().build()

    init {
        this.version = version
    }

    @Override
    fun buildTypeDeserializer(
        config: DeserializationConfig?, baseType: JavaType?,
        subtypes: Collection<NamedType?>?
    ): TypeDeserializer {
        val idRes: TypeIdResolver = this.idResolver(config, baseType, typeValidator, subtypes, false, true)
        return GraphSONTypeDeserializer(baseType, idRes, this.getTypeProperty(), typeInfo, valuePropertyName)
    }

    @Override
    fun buildTypeSerializer(
        config: SerializationConfig?, baseType: JavaType?,
        subtypes: Collection<NamedType?>?
    ): TypeSerializer {
        val idRes: TypeIdResolver = this.idResolver(config, baseType, typeValidator, subtypes, true, false)
        return if (version === GraphSONVersion.V2_0) GraphSONTypeSerializerV2d0(
            idRes,
            this.getTypeProperty(),
            typeInfo,
            valuePropertyName
        ) else GraphSONTypeSerializerV3d0(idRes, this.getTypeProperty(), typeInfo, valuePropertyName)
    }

    fun valuePropertyName(valuePropertyName: String?): GraphSONTypeResolverBuilder {
        this.valuePropertyName = valuePropertyName
        return this
    }

    fun typesEmbedding(typeInfo: TypeInfo?): GraphSONTypeResolverBuilder {
        this.typeInfo = typeInfo
        return this
    }
}