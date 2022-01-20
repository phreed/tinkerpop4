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
package org.apache.tinkerpop4.gremlin.tinkercat.structure.io.graphson

import org.apache.tinkerpop4.gremlin.process.traversal.TraversalSource
import kotlin.jvm.JvmOverloads
import org.apache.tinkerpop4.gremlin.jsr223.JavaTranslator
import org.apache.tinkerpop4.gremlin.process.traversal.Bytecode
import org.apache.tinkerpop4.gremlin.structure.io.graphson.GraphSONVersion
import org.apache.tinkerpop4.gremlin.process.traversal.Translator.StepTranslator
import org.apache.tinkerpop4.gremlin.process.traversal.Traversal
import org.apache.tinkerpop4.gremlin.structure.io.graphson.GraphSONWriter
import org.apache.tinkerpop4.gremlin.structure.io.graphson.GraphSONReader
import org.apache.tinkerpop4.gremlin.structure.io.graphson.GraphSONMapper
import org.apache.tinkerpop4.gremlin.structure.io.graphson.GraphSONXModuleV2d0
import org.apache.tinkerpop4.gremlin.structure.io.graphson.GraphSONXModuleV3d0
import java.lang.IllegalArgumentException
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.lang.Exception
import java.lang.IllegalStateException

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
internal class GraphSONTranslator<S : TraversalSource?, T : Traversal.Admin<*, *>?> @JvmOverloads constructor(
    private val wrappedTranslator: JavaTranslator<S, T>, version: GraphSONVersion = GraphSONVersion.V2_0
) : StepTranslator<S, T> {
    private val writer: GraphSONWriter
    private val reader: GraphSONReader

    init {
        val mapper: GraphSONMapper
        mapper = if (version == GraphSONVersion.V2_0) {
            GraphSONMapper.build()
                .addCustomModule(GraphSONXModuleV2d0.build().create(false)).version(GraphSONVersion.V2_0).create()
        } else if (version == GraphSONVersion.V3_0) {
            GraphSONMapper.build()
                .addCustomModule(GraphSONXModuleV3d0.build().create(false)).version(GraphSONVersion.V3_0).create()
        } else {
            throw IllegalArgumentException("GraphSONVersion." + version.name + " is not supported for testing")
        }
        writer = GraphSONWriter.build().mapper(mapper).create()
        reader = GraphSONReader.build().mapper(mapper).create()
    }

    override fun getTraversalSource(): S {
        return wrappedTranslator.traversalSource
    }

    override fun translate(bytecode: Bytecode): T {
        return try {
            val outputStream = ByteArrayOutputStream()
            writer.writeObject(outputStream, bytecode)
            wrappedTranslator.translate(
                reader.readObject(
                    ByteArrayInputStream(outputStream.toByteArray()),
                    Bytecode::class.java
                )
            )
        } catch (e: Exception) {
            throw IllegalStateException(e.message, e)
        }
    }

    override fun getTargetLanguage(): String {
        return wrappedTranslator.targetLanguage
    }
}