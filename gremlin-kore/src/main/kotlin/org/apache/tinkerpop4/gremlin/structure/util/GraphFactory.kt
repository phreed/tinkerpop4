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
package org.apache.tinkerpop4.gremlin.structure.util

import org.apache.commons.configuration2.ConfigurationUtils
import org.apache.commons.configuration2.FileBasedConfiguration
import org.apache.commons.configuration2.YAMLConfiguration
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder
import org.apache.commons.configuration2.builder.fluent.Configurations
import org.apache.commons.configuration2.builder.fluent.Parameters
import org.apache.tinkerpop4.gremlin.structure.Graph
import org.apache.commons.configuration2.Configuration
import org.apache.commons.configuration2.ex.ConfigurationException
import org.apache.commons.configuration2.MapConfiguration
import java.io.File
import java.util.Map

/**
 * Factory to construct new [Graph] instances from a `Configuration` object or properties file.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
object GraphFactory {
    /**
     * Open a graph.  See each [Graph] instance for its configuration options.
     *
     * @param configuration A configuration object that specifies the minimally required properties for a
     * [Graph] instance. This minimum is determined by the
     * [Graph] instance itself.
     * @return A [Graph] instance.
     * @throws IllegalArgumentException if `configuration`
     */
    fun open(configuration: Configuration?): Graph {
        if (null == configuration) throw Graph.Exceptions.argumentCanNotBeNull("configuration")
        val clazz: String = configuration.getString(Graph.GRAPH, null)
            ?: throw RuntimeException(String.format("Configuration must contain a valid '%s' setting", Graph.GRAPH))
        val graphClass: Class<*>
        graphClass = try {
            Class.forName(clazz)
        } catch (e: ClassNotFoundException) {
            throw RuntimeException(
                String.format(
                    "GraphFactory could not find [%s] - Ensure that the jar is in the classpath",
                    clazz
                )
            )
        }

        // If the graph class specifies a factory class then use that instead of the specified class.
        val factoryAnnotation: GraphFactoryClass = graphClass.getAnnotation(GraphFactoryClass::class.java)
        val factoryClass: Class<*> = if (factoryAnnotation != null) factoryAnnotation.value() else graphClass
        return open(configuration, factoryClass)
    }

    private fun open(configuration: Configuration?, graphFactoryClass: Class<*>): Graph {
        val g: Graph
        g = try {
            // will use open(Configuration c) to instantiate
            graphFactoryClass.getMethod("open", Configuration::class.java).invoke(null, configuration) as Graph
        } catch (e1: NoSuchMethodException) {
            throw RuntimeException(
                String.format(
                    "GraphFactory can only instantiate Graph implementations from classes that have a static open() method that takes a single Apache Commons Configuration argument - [%s] does not seem to have one",
                    graphFactoryClass
                )
            )
        } catch (e2: Exception) {
            throw RuntimeException(
                String.format(
                    "GraphFactory could not instantiate this Graph implementation [%s]",
                    graphFactoryClass
                ), e2
            )
        }
        return g
    }

    /**
     * Open a graph.  See each [Graph] instance for its configuration options. This file may be XML, YAML,
     * or a standard properties file. How the configuration is used (and which kind is required) is dependent on
     * the implementation.
     *
     *
     * If using XML, ensure that the appropriate version of Apache `commons-collections` is available on the
     * classpath as it is an optional dependency of Apache `commons-configuration`, the library that
     * `GraphFactory` depends on.
     *
     * @param configurationFile The location of a configuration file that specifies the minimally required properties
     * for a [Graph] instance. This minimum is determined by the [Graph] instance
     * itself.
     * @return A [Graph] instance.
     * @throws IllegalArgumentException if `configurationFile` is null
     */
    fun open(configurationFile: String?): Graph {
        if (null == configurationFile) throw Graph.Exceptions.argumentCanNotBeNull("configurationFile")
        return open(getConfiguration(File(configurationFile)))
    }

    /**
     * Open a graph. See each [Graph] instance for its configuration options.
     *
     * @param configuration A `Map` based configuration that will be converted to an
     * `Configuration` object via `MapConfiguration` and passed to the appropriate
     * overload.
     * @return A Graph instance.
     */
    fun open(configuration: Map?): Graph {
        if (null == configuration) throw Graph.Exceptions.argumentCanNotBeNull("configuration")
        return open(MapConfiguration(configuration))
    }

    private fun getConfiguration(configurationFile: File): org.apache.commons.configuration2.Configuration {
        if (!configurationFile.isFile()) throw IllegalArgumentException(
            String.format(
                "The location configuration must resolve to a file and [%s] does not",
                configurationFile
            )
        )
        return try {
            val fileName: String = configurationFile.getName()
            val fileExtension: String = fileName.substring(fileName.lastIndexOf('.') + 1)
            val conf: Configuration
            val configs = Configurations()
            conf = when (fileExtension) {
                "yml", "yaml" -> {
                    val params = Parameters()
                    val builder: FileBasedConfigurationBuilder<FileBasedConfiguration> =
                        FileBasedConfigurationBuilder<FileBasedConfiguration>(
                            YAMLConfiguration::class.java
                        ).configure(params.fileBased().setFile(configurationFile))
                    val copy: org.apache.commons.configuration2.Configuration = BaseConfiguration()
                    ConfigurationUtils.copy(
                        builder.configure(params.fileBased().setFile(configurationFile)).getConfiguration(), copy
                    )
                    copy
                }
                "xml" -> configs.xml(configurationFile)
                else -> configs.properties(configurationFile)
            }
            conf
        } catch (e: ConfigurationException) {
            throw IllegalArgumentException(String.format("Could not load configuration at: %s", configurationFile), e)
        }
    }
}