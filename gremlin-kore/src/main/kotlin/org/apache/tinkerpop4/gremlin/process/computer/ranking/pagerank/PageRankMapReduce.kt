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
package org.apache.tinkerpop4.gremlin.process.computer.ranking.pagerank

import org.apache.tinkerpop4.gremlin.process.computer.KeyValue

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class PageRankMapReduce : StaticMapReduce<Object?, Double?, Object?, Double?, Iterator<KeyValue<Object?, Double?>?>?> {
    @get:Override
    var memoryKey = DEFAULT_MEMORY_KEY
        private set

    private constructor() {}
    private constructor(memoryKey: String) {
        this.memoryKey = memoryKey
    }

    @Override
    fun storeState(configuration: Configuration) {
        super.storeState(configuration)
        configuration.setProperty(PAGE_RANK_MEMORY_KEY, memoryKey)
    }

    @Override
    fun loadState(graph: Graph?, configuration: Configuration) {
        memoryKey = configuration.getString(PAGE_RANK_MEMORY_KEY, DEFAULT_MEMORY_KEY)
    }

    @Override
    fun doStage(stage: Stage): Boolean {
        return stage.equals(Stage.MAP)
    }

    @Override
    fun map(vertex: Vertex, emitter: MapEmitter<Object?, Double?>) {
        val pageRank: Property = vertex.property(PageRankVertexProgram.PAGE_RANK)
        if (pageRank.isPresent()) {
            emitter.emit(vertex.id(), pageRank.value() as Double)
        }
    }

    @Override
    fun generateFinalResult(keyValues: Iterator<KeyValue<Object, Double>>): Iterator<KeyValue<Object, Double>> {
        return keyValues
    }

    @Override
    override fun toString(): String {
        return StringFactory.mapReduceString(this, memoryKey)
    }

    class Builder {
        private var memoryKey = DEFAULT_MEMORY_KEY
        fun memoryKey(memoryKey: String): Builder {
            this.memoryKey = memoryKey
            return this
        }

        fun create(): PageRankMapReduce {
            return PageRankMapReduce(memoryKey)
        }
    }

    companion object {
        const val PAGE_RANK_MEMORY_KEY = "gremlin.pageRankMapReduce.memoryKey"
        const val DEFAULT_MEMORY_KEY = "pageRank"

        //////////////////////////////
        fun build(): Builder {
            return Builder()
        }
    }
}