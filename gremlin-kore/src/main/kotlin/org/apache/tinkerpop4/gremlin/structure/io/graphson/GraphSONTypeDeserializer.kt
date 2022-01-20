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

import org.apache.tinkerpop.shaded.jackson.annotation.JsonTypeInfo

/**
 * Contains main logic for the whole JSON to Java deserialization. Handles types embedded with the version 2.0 of GraphSON.
 *
 * @author Kevin Gallardo (https://kgdo.me)
 */
class GraphSONTypeDeserializer internal constructor(
    baseType: JavaType, idRes: TypeIdResolver, typePropertyName: String,
    typeInfo: TypeInfo, valuePropertyName: String
) : TypeDeserializerBase(baseType, idRes, typePropertyName, false, null) {
    private val idRes: TypeIdResolver
    private val propertyName: String
    private val valuePropertyName: String
    private val baseType: JavaType
    private val typeInfo: TypeInfo

    init {
        this.baseType = baseType
        this.idRes = idRes
        propertyName = typePropertyName
        this.typeInfo = typeInfo
        this.valuePropertyName = valuePropertyName
    }

    @Override
    fun forProperty(beanProperty: BeanProperty?): TypeDeserializer {
        return this
    }

    @get:Override
    val typeInclusion: JsonTypeInfo.As
        get() = JsonTypeInfo.As.WRAPPER_ARRAY

    @get:Override
    val typeIdResolver: TypeIdResolver
        get() = idRes

    @get:Override
    val defaultImpl: Class<*>?
        get() = null

    @Override
    @Throws(IOException::class)
    fun deserializeTypedFromObject(jsonParser: JsonParser, deserializationContext: DeserializationContext): Object {
        return deserialize(jsonParser, deserializationContext)
    }

    @Override
    @Throws(IOException::class)
    fun deserializeTypedFromArray(jsonParser: JsonParser, deserializationContext: DeserializationContext): Object {
        return deserialize(jsonParser, deserializationContext)
    }

    @Override
    @Throws(IOException::class)
    fun deserializeTypedFromScalar(jsonParser: JsonParser, deserializationContext: DeserializationContext): Object {
        return deserialize(jsonParser, deserializationContext)
    }

    @Override
    @Throws(IOException::class)
    fun deserializeTypedFromAny(jsonParser: JsonParser, deserializationContext: DeserializationContext): Object {
        return deserialize(jsonParser, deserializationContext)
    }

    /**
     * Main logic for the deserialization.
     */
    @Throws(IOException::class)
    private fun deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext): Object {
        val buf = TokenBuffer(jsonParser.getCodec(), false)
        val localCopy = TokenBuffer(jsonParser.getCodec(), false)

        // Detect type
        try {
            // The Type pattern is START_OBJECT -> TEXT_FIELD(propertyName) && TEXT_FIELD(valueProp).
            if (jsonParser.getCurrentToken() === JsonToken.START_OBJECT) {
                buf.writeStartObject()
                var typeName: String? = null
                var valueDetected = false
                var valueDetectedFirst = false
                for (i in 0..1) {
                    val nextFieldName: String = jsonParser.nextFieldName()
                        ?: // empty map or less than 2 fields, go out.
                        break
                    if (!nextFieldName.equals(propertyName) && !nextFieldName.equals(valuePropertyName)) {
                        // no type, go out.
                        break
                    }
                    if (nextFieldName.equals(propertyName)) {
                        // detected "@type" field.
                        typeName = jsonParser.nextTextValue()
                        // keeping the spare buffer up to date in case it's a false detection (only the "@type" property)
                        buf.writeStringField(propertyName, typeName)
                        continue
                    }
                    if (nextFieldName.equals(valuePropertyName)) {
                        // detected "@value" field.
                        jsonParser.nextValue()
                        if (typeName == null) {
                            // keeping the spare buffer up to date in case it's a false detection (only the "@value" property)
                            // the problem is that the fields "@value" and "@type" could be in any order
                            buf.writeFieldName(valuePropertyName)
                            valueDetectedFirst = true
                            localCopy.copyCurrentStructure(jsonParser)
                        }
                        valueDetected = true
                        continue
                    }
                }
                if (typeName != null && valueDetected) {
                    // Type has been detected pattern detected.
                    val typeFromId: JavaType = idRes.typeFromId(deserializationContext, typeName)
                    if (!baseType.isJavaLangObject() && !baseType.equals(typeFromId)) {
                        throw InstantiationException(
                            String.format(
                                "Cannot deserialize the value with the detected type contained in the JSON ('%s') " +
                                        "to the type specified in parameter to the object mapper (%s). " +
                                        "Those types are incompatible.", typeName, baseType.getRawClass().toString()
                            )
                        )
                    }
                    val jsonDeserializer: JsonDeserializer =
                        deserializationContext.findContextualValueDeserializer(typeFromId, null)
                    val tokenParser: JsonParser
                    if (valueDetectedFirst) {
                        tokenParser = localCopy.asParser()
                        tokenParser.nextToken()
                    } else {
                        tokenParser = jsonParser
                    }
                    val value: Object = jsonDeserializer.deserialize(tokenParser, deserializationContext)
                    val t: JsonToken = jsonParser.nextToken()
                    return if (t === JsonToken.END_OBJECT) {
                        // we're good to go
                        value
                    } else {
                        // detected the type pattern entirely but the Map contained other properties
                        // For now we error out because we assume that pattern is *only* reserved to
                        // typed values.
                        throw deserializationContext.mappingException(
                            "Detected the type pattern in the JSON payload " +
                                    "but the map containing the types and values contains other fields. This is not " +
                                    "allowed by the deserializer."
                        )
                    }
                }
            }
        } catch (e: Exception) {
            throw deserializationContext.mappingException("Could not deserialize the JSON value as required. Nested exception: " + e.toString())
        }

        // Type pattern wasn't detected, however,
        // while searching for the type pattern, we may have moved the cursor of the original JsonParser in param.
        // To compensate, we have filled consistently a TokenBuffer that should contain the equivalent of
        // what we skipped while searching for the pattern.
        // This has a huge positive impact on performances, since JsonParser does not have a 'rewind()',
        // the only other solution would have been to copy the whole original JsonParser. Which we avoid here and use
        // an efficient structure made of TokenBuffer + JsonParserSequence/Concat.
        // Concatenate buf + localCopy + end of original content(jsonParser).
        val concatenatedArray: Array<JsonParser> = arrayOf<JsonParser>(buf.asParser(), localCopy.asParser(), jsonParser)
        val parserToUse: JsonParser = JsonParserConcat(concatenatedArray)
        parserToUse.nextToken()

        // If a type has been specified in parameter, use it to find a deserializer and deserialize:
        return if (!baseType.isJavaLangObject()) {
            val jsonDeserializer: JsonDeserializer =
                deserializationContext.findContextualValueDeserializer(baseType, null)
            jsonDeserializer.deserialize(parserToUse, deserializationContext)
        } else {
            if (parserToUse.isExpectedStartArrayToken()) {
                deserializationContext.findContextualValueDeserializer(arrayJavaType, null)
                    .deserialize(parserToUse, deserializationContext)
            } else if (parserToUse.isExpectedStartObjectToken()) {
                deserializationContext.findContextualValueDeserializer(mapJavaType, null)
                    .deserialize(parserToUse, deserializationContext)
            } else {
                // There's "java.lang.Object" in param, there's no type detected in the payload, the payload isn't a JSON Map or JSON List
                // then consider it a simple type, even though we shouldn't be here if it was a simple type.
                // TODO : maybe throw an error instead?
                // throw deserializationContext.mappingException("Roger, we have a problem deserializing");
                val jsonDeserializer: JsonDeserializer =
                    deserializationContext.findContextualValueDeserializer(baseType, null)
                jsonDeserializer.deserialize(parserToUse, deserializationContext)
            }
        }
    }

    private fun canReadTypeId(): Boolean {
        return typeInfo === TypeInfo.PARTIAL_TYPES
    }

    companion object {
        private val mapJavaType: JavaType = TypeFactory.defaultInstance().constructType(LinkedHashMap::class.java)
        private val arrayJavaType: JavaType = TypeFactory.defaultInstance().constructType(ArrayList::class.java)
    }
}