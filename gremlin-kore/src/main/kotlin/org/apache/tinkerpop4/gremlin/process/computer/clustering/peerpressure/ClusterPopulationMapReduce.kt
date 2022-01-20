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
class ClusterPopulationMapReduce :
    StaticMapReduce<Serializable?, Long?, Serializable?, Long?, Map<Serializable?, Long?>?> {
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
        configuration.setProperty(CLUSTER_POPULATION_MEMORY_KEY, memoryKey)
    }

    @Override
    fun loadState(graph: Graph?, configuration: Configuration) {
        memoryKey = configuration.getString(CLUSTER_POPULATION_MEMORY_KEY, DEFAULT_MEMORY_KEY)
    }

    @Override
    fun doStage(stage: Stage?): Boolean {
        return true
    }

    @Override
    fun map(vertex: Vertex, emitter: MapEmitter<Serializable?, Long?>) {
        val cluster: Property<Serializable> = vertex.property(PeerPressureVertexProgram.CLUSTER)
        if (cluster.isPresent()) {
            emitter.emit(cluster.value(), 1L)
        }
    }

    @Override
    fun combine(key: Serializable?, values: Iterator<Long>, emitter: ReduceEmitter<Serializable?, Long?>) {
        reduce(key, values, emitter)
    }

    @Override
    fun reduce(key: Serializable?, values: Iterator<Long>, emitter: ReduceEmitter<Serializable?, Long?>) {
        var count = 0L
        while (values.hasNext()) {
            count = count + values.next()
        }
        emitter.emit(key, count)
    }

    @Override
    fun generateFinalResult(keyValues: Iterator<KeyValue<Serializable?, Long?>?>): Map<Serializable, Long> {
        val clusterPopulation: Map<Serializable, Long> = HashMap()
        keyValues.forEachRemaining { pair -> clusterPopulation.put(pair.getKey(), pair.getValue()) }
        return clusterPopulation
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

        fun create(): ClusterPopulationMapReduce {
            return ClusterPopulationMapReduce(memoryKey)
        }
    }

    companion object {
        const val CLUSTER_POPULATION_MEMORY_KEY = "gremlin.clusterPopulationMapReduce.memoryKey"
        const val DEFAULT_MEMORY_KEY = "clusterPopulation"

        //////////////////////////////
        fun build(): Builder {
            return Builder()
        }
    }
}