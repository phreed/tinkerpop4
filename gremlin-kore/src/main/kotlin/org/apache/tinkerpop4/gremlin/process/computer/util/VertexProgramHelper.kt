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
package org.apache.tinkerpop4.gremlin.process.computer.util

import org.apache.commons.configuration2.AbstractConfiguration

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
object VertexProgramHelper {
    fun vertexComputeKeysAsSet(vertexComputeKeySet: Set<VertexComputeKey?>): Set<String> {
        val set: Set<String> = HashSet(vertexComputeKeySet.size())
        for (key in vertexComputeKeySet) {
            set.add(key.getKey())
        }
        return set
    }

    fun isTransientVertexComputeKey(key: String, vertexComputeKeySet: Set<VertexComputeKey?>): Boolean {
        for (vertexComputeKey in vertexComputeKeySet) {
            if (vertexComputeKey.getKey().equals(key)) return vertexComputeKey.isTransient()
        }
        throw IllegalArgumentException("Could not find key in vertex compute key set: $key")
    }

    fun vertexComputeKeysAsArray(vertexComputeKeySet: Set<VertexComputeKey?>): Array<String> {
        return vertexComputeKeysAsSet(vertexComputeKeySet).toArray(arrayOfNulls<String>(vertexComputeKeySet.size()))
    }

    fun serialize(`object`: Object?, configuration: Configuration, key: String?) {
        try {
            configuration.setProperty(key, Base64.getEncoder().encodeToString(Serializer.serializeObject(`object`)))
        } catch (e: IOException) {
            throw IllegalArgumentException(e.getMessage(), e)
        }
    }

    fun <T> deserialize(configuration: Configuration, key: String?): T {
        return try {
            // a bit of a weird double try-catch here. Base64 can throw an IllegalArgumentException if given some
            // bad data to deserialize. that needs to be caught and then re-cast as a IOException so that downstream
            // systems can better catch and react to the error. giraph is the big hassle here it seems - see
            // GiraphGraphComputer.run() for more related notes on this specifically where
            // VertexProgram.createVertexProgram() is called as it has special handling for errors related to
            // deserialization. if not handled properly, giraph will hang in tests. i don't want to over-tweak this
            // code too much for two reasons (1) dont want to alter method signatures too much or mess with existing
            // logic within 3.2.x (2) giraph is dead in 3.4.x so no point to trying to make this a ton more elegant.
            try {
                Serializer.deserializeObject(Base64.getDecoder().decode(configuration.getString(key).getBytes()))
            } catch (iae: IllegalArgumentException) {
                throw IOException(iae.getMessage())
            }
        } catch (e: IOException) {
            throw IllegalArgumentException(e.getMessage(), e)
        } catch (e: ClassNotFoundException) {
            throw IllegalArgumentException(e.getMessage(), e)
        }
    }

    fun <S, E> reverse(traversal: Traversal.Admin<S, E>): Traversal.Admin<S, E> {
        for (step in traversal.getSteps()) {
            if (step is VertexStep) (step as VertexStep).reverseDirection()
            if (step is EdgeVertexStep) (step as EdgeVertexStep).reverseDirection()
        }
        return traversal
    }

    @Throws(IllegalArgumentException::class)
    fun legalConfigurationKeyValueArray(vararg configurationKeyValues: Object?) {
        if (configurationKeyValues.size % 2 != 0) throw IllegalArgumentException("The provided arguments must have a size that is a factor of 2")
        var i = 0
        while (i < configurationKeyValues.size) {
            if (configurationKeyValues[i] !is String) throw IllegalArgumentException("The provided key/value array must have a String key on even array indices")
            i = i + 2
        }
    }
}