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
package org.apache.tinkerpop4.gremlin.process.traversal.step.sideEffect

import org.apache.tinkerpop4.gremlin.process.traversal.IO

/**
 * Handles read and write operations into the [Graph].
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class IoStep<S>(traversal: Traversal.Admin?, file: String?) : AbstractStep<S, S>(traversal), ReadWriting {
    private var parameters: Parameters = Parameters()
    private var first = true

    @get:Override
    var file: String?
        private set
    private var mode: Mode = Mode.UNSET

    init {
        if (null == file || file.isEmpty()) throw IllegalArgumentException("file cannot be null or empty")
        this.file = file
    }

    @Override
    fun setMode(mode: Mode) {
        this.mode = mode
    }

    @Override
    fun getMode(): Mode {
        return mode
    }

    @Override
    fun getParameters(): Parameters {
        return parameters
    }

    @Override
    fun configure(vararg keyValues: Object?) {
        parameters.set(null, keyValues)
    }

    @Override
    protected fun processNextStart(): Traverser.Admin<S> {
        if (mode === Mode.UNSET) throw IllegalStateException("IO mode was not set to read() or write()")
        if (!first) throw FastNoSuchElementException.instance()
        first = false
        val file = File(file)
        return if (mode === Mode.READING) {
            if (!file.exists()) throw IllegalStateException(this.file.toString() + " does not exist")
            read(file)
        } else if (mode === Mode.WRITING) {
            write(file)
        } else {
            throw IllegalStateException("Invalid ReadWriting.Mode configured in IoStep: " + mode.name())
        }
    }

    protected fun write(file: File?): Traverser.Admin<S> {
        try {
            FileOutputStream(file).use { stream ->
                val graph: Graph = this.traversal.getGraph().get() as Graph
                constructWriter().writeGraph(stream, graph)
                return EmptyTraverser.instance()
            }
        } catch (ioe: IOException) {
            throw IllegalStateException(String.format("Could not write file %s from graph", this.file), ioe)
        }
    }

    protected fun read(file: File?): Traverser.Admin<S> {
        try {
            FileInputStream(file).use { stream ->
                val graph: Graph = this.traversal.getGraph().get() as Graph
                constructReader().readGraph(stream, graph)
                return EmptyTraverser.instance()
            }
        } catch (ioe: IOException) {
            throw IllegalStateException(String.format("Could not read file %s into graph", this.file), ioe)
        }
    }

    /**
     * Builds a [GraphReader] instance to use. Attempts to detect the file format to be read using the file
     * extension or simply uses configurations provided by the user on the parameters given to the step.
     */
    private fun constructReader(): GraphReader {
        val objectOrClass: Object = parameters.get(IO.reader) { detectFileType() }.get(0)
        return if (objectOrClass is GraphReader) objectOrClass as GraphReader else if (objectOrClass is String) {
            if (objectOrClass.equals(IO.graphson)) {
                val builder: GraphSONMapper.Builder = GraphSONMapper.build()
                detectRegistries().forEach(builder::addRegistry)
                GraphSONReader.build().mapper(builder.create()).create()
            } else if (objectOrClass.equals(IO.gryo)) {
                val builder: GryoMapper.Builder = GryoMapper.build()
                detectRegistries().forEach(builder::addRegistry)
                GryoReader.build().mapper(builder.create()).create()
            } else if (objectOrClass.equals(IO.graphml)) GraphMLReader.build().create() else {
                try {
                    val graphReaderClazz: Class<*> = Class.forName(objectOrClass as String)
                    val build: Method = graphReaderClazz.getMethod("build")
                    val builder: ReaderBuilder = build.invoke(null) as ReaderBuilder
                    builder.create()
                } catch (ex: Exception) {
                    throw IllegalStateException(
                        String.format(
                            "Could not construct the specified GraphReader of %s",
                            objectOrClass
                        ), ex
                    )
                }
            }
        } else {
            throw IllegalStateException("GraphReader could not be determined")
        }
    }

    /**
     * Builds a [GraphWriter] instance to use. Attempts to detect the file format to be write using the file
     * extension or simply uses configurations provided by the user on the parameters given to the step.
     */
    private fun constructWriter(): GraphWriter {
        val objectOrClass: Object = parameters.get(IO.writer) { detectFileType() }.get(0)
        return if (objectOrClass is GraphWriter) objectOrClass as GraphWriter else if (objectOrClass is String) {
            if (objectOrClass.equals(IO.graphson)) {
                val builder: GraphSONMapper.Builder = GraphSONMapper.build()
                detectRegistries().forEach(builder::addRegistry)
                GraphSONWriter.build().mapper(builder.create()).create()
            } else if (objectOrClass.equals(IO.gryo)) {
                val builder: GryoMapper.Builder = GryoMapper.build()
                detectRegistries().forEach(builder::addRegistry)
                GryoWriter.build().mapper(builder.create()).create()
            } else if (objectOrClass.equals(IO.graphml)) GraphMLWriter.build().create() else {
                try {
                    val graphWriterClazz: Class<*> = Class.forName(objectOrClass as String)
                    val build: Method = graphWriterClazz.getMethod("build")
                    val builder: WriterBuilder = build.invoke(null) as WriterBuilder
                    builder.create()
                } catch (ex: Exception) {
                    throw IllegalStateException(
                        String.format(
                            "Could not construct the specified GraphWriter of %s",
                            objectOrClass
                        ), ex
                    )
                }
            }
        } else {
            throw IllegalStateException("GraphWriter could not be determined")
        }
    }

    protected fun detectFileType(): String {
        return if (file.endsWith(".kryo")) IO.gryo else if (file.endsWith(".json")) IO.graphson else if (file.endsWith(".xml")) IO.graphml else throw IllegalStateException(
            "Could not detect the file format - specify the writer explicitly or rename file with a standard extension"
        )
    }

    protected fun detectRegistries(): List<IoRegistry> {
        val k: List<Object> = parameters.get(IO.registry, null)
        return k.stream().map { cn ->
            try {
                if (cn is IoRegistry) return@map cn as IoRegistry else {
                    val clazz: Class<*> = Class.forName(cn.toString())
                    return@map clazz.getMethod("instance").invoke(null) as IoRegistry
                }
            } catch (ex: Exception) {
                throw IllegalStateException(ex)
            }
        }.collect(Collectors.toList())
    }

    @Override
    override fun hashCode(): Int {
        val hash = super.hashCode() xor parameters.hashCode()
        return if (null != file) hash xor file!!.hashCode() else hash
    }

    @Override
    override fun toString(): String {
        return StringFactory.stepString(this, file, parameters)
    }

    @Override
    fun clone(): IoStep<*> {
        val clone = super.clone() as IoStep<*>
        clone.parameters = parameters.clone()
        clone.file = file
        clone.mode = mode
        return clone
    }
}