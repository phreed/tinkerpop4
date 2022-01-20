/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.tinkerpop4.gremlin.process.traversal

import java.util.HashMap
import java.util.List
import java.util.Map
import java.util.Optional
import java.util.stream.Collectors
import java.util.stream.Stream

/**
 * General representation of script
 *
 * @author Stark Arya (sandszhou.zj@alibaba-inc.com)
 */
class Script {
    private val scriptBuilder: StringBuilder
    private val parameters: Map<Object, String>

    init {
        scriptBuilder = StringBuilder()
        parameters = HashMap()
    }

    fun init() {
        scriptBuilder.setLength(0)
        parameters.clear()
    }

    fun append(script: String?): Script {
        scriptBuilder.append(script)
        return this
    }

    fun setCharAtEnd(ch: Char): Script {
        scriptBuilder.setCharAt(scriptBuilder.length() - 1, ch)
        return this
    }

    fun <V> getBoundKeyOrAssign(withParameters: Boolean, value: V): Script {
        if (withParameters) {
            if (!parameters.containsKey(value)) {
                parameters.put(value, nextBoundKey)
            }
            append(parameters[value])
        } else {
            append(value.toString())
        }
        return this
    }

    val script: String
        get() = scriptBuilder.toString()

    fun getParameters(): Optional<Map<String, Object>> {
        return Optional.ofNullable(
            if (parameters.isEmpty()) null else parameters.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey))
        )
    }

    /**
     * @return  a monotonically increasing key
     */
    private val nextBoundKey: String
        private get() = KEY_PREFIX + parameters.size()

    @Override
    override fun toString(): String {
        val builder = StringBuilder()
        val strings: List<String> = Stream.of(arrayOf(script, getParameters()))
            .filter { o -> null != o }
            .filter { o ->
                if (o is Map) {
                    return@filter !(o as Map).isEmpty()
                } else {
                    return@filter !o.toString().isEmpty()
                }
            }
            .map(Object::toString).collect(Collectors.toList())
        if (!strings.isEmpty()) {
            builder.append('(')
            builder.append(String.join(",", strings))
            builder.append(')')
        }
        return "Script[" + builder.toString().toString() + "]"
    }

    companion object {
        private const val KEY_PREFIX = "_args_"
    }
}