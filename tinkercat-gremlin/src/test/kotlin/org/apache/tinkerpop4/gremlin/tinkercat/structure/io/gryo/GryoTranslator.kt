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
package org.apache.tinkerpop4.gremlin.tinkercat.structure.io.gryo

import org.apache.tinkerpop4.gremlin.process.traversal.TraversalSource
import org.apache.tinkerpop4.gremlin.jsr223.JavaTranslator
import org.apache.tinkerpop4.gremlin.process.traversal.Bytecode
import org.apache.tinkerpop4.gremlin.process.traversal.Translator.StepTranslator
import org.apache.tinkerpop4.gremlin.process.traversal.Traversal
import org.apache.tinkerpop4.gremlin.structure.io.gryo.GryoMapper
import org.apache.tinkerpop4.gremlin.structure.io.gryo.GryoWriter
import org.apache.tinkerpop4.gremlin.structure.io.gryo.GryoReader
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.lang.Exception
import java.lang.IllegalStateException

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
internal class GryoTranslator<S : TraversalSource?, T : Traversal.Admin<*, *>?>(
    private val wrappedTranslator: JavaTranslator<S, T>
) : StepTranslator<S, T> {
    private val mapper = GryoMapper.build().create()
    private val writer = GryoWriter.build().mapper(mapper).create()
    private val reader = GryoReader.build().mapper(mapper).create()
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