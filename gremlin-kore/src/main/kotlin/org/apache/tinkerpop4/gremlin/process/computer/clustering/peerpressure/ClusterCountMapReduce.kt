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
package org.apache.tinkerpop4.gremlin.process.computer.clustering.peerpressure

import org.apache.tinkerpop4.gremlin.process.computer.KeyValue

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class ClusterCountMapReduce :
    StaticMapReduce<MapReduce.NullObject?, Serializable?, MapReduce.NullObject?, Integer?, Integer?> {
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
        configuration.setProperty(CLUSTER_COUNT_MEMORY_KEY, memoryKey)
    }

    @Override
    fun loadState(graph: Graph?, configuration: Configuration) {
        memoryKey = configuration.getString(CLUSTER_COUNT_MEMORY_KEY, DEFAULT_MEMORY_KEY)
    }

    @Override
    fun doStage(stage: Stage): Boolean {
        return !stage.equals(Stage.COMBINE)
    }

    @Override
    fun map(vertex: Vertex, emitter: MapEmitter<NullObject?, Serializable?>) {
        val cluster: Property<Serializable> = vertex.property(PeerPressureVertexProgram.CLUSTER)
        if (cluster.isPresent()) {
            emitter.emit(NullObject.instance(), cluster.value())
        }
    }

    @Override
    fun reduce(key: NullObject?, values: Iterator<Serializable?>, emitter: ReduceEmitter<NullObject?, Integer?>) {
        val set: Set<Serializable> = HashSet()
        values.forEachRemaining(set::add)
        emitter.emit(NullObject.instance(), set.size())
    }

    @Override
    fun generateFinalResult(keyValues: Iterator<KeyValue<NullObject?, Integer?>>): Integer {
        return keyValues.next().getValue()
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

        fun create(): ClusterCountMapReduce {
            return ClusterCountMapReduce(memoryKey)
        }
    }

    companion object {
        const val CLUSTER_COUNT_MEMORY_KEY = "gremlin.clusterCountMapReduce.memoryKey"
        const val DEFAULT_MEMORY_KEY = "clusterCount"

        //////////////////////////////
        fun build(): Builder {
            return Builder()
        }
    }
}