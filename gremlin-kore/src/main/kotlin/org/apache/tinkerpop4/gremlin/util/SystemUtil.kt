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
package org.apache.tinkerpop4.gremlin.util

import org.apache.commons.configuration2.BaseConfiguration
import org.apache.commons.configuration2.Configuration
import java.util.Map

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
object SystemUtil {
    /**
     * Generate a [Configuration] from the [System.getProperties].
     * Only those properties with specified prefix key are aggregated.
     * If the prefix and a . should be removed, then trim prefix.
     *
     * @param prefix     the prefix of the keys to include in the configuration
     * @param trimPrefix whether to trim the prefix + . from the key
     * @return a configuration generated from the System properties
     */
    fun getSystemPropertiesConfiguration(prefix: String, trimPrefix: Boolean): Configuration {
        val apacheConfiguration = BaseConfiguration()
        for (entry in System.getProperties().entrySet()) {
            val key: String = entry.getKey().toString()
            val value: Object = entry.getValue()
            if (key.startsWith("$prefix.")) apacheConfiguration.setProperty(
                if (trimPrefix) key.substring(prefix.length() + 1) else key,
                value
            )
        }
        return apacheConfiguration
    }
}