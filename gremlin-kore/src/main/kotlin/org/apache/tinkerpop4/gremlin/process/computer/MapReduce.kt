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
package org.apache.tinkerpop4.gremlin.process.computer

import org.apache.commons.configuration2.Configuration

/**
 * A MapReduce is composed of map(), combine(), and reduce() stages.
 * The [Stage.MAP] stage processes the vertices of the [Graph] in a logically parallel manner.
 * The [Stage.COMBINE] stage aggregates the values of a particular map emitted key prior to sending across the cluster.
 * The [Stage.REDUCE] stage aggregates the values of the combine/map emitted keys for the keys that hash to the current machine in the cluster.
 * The interface presented here is nearly identical to the interface popularized by Hadoop save the map() is over the vertices of the graph.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
interface MapReduce<MK, MV, RK, RV, R> : Cloneable {
    /**
     * MapReduce is composed of three stages: map, combine, and reduce.
     */
    enum class Stage {
        MAP, COMBINE, REDUCE
    }

    /**
     * When it is necessary to store the state of a MapReduce job, this method is called.
     * This is typically required when the MapReduce job needs to be serialized to another machine.
     * Note that what is stored is simply the instance state, not any processed data.
     *
     * @param configuration the configuration to store the state of the MapReduce job in.
     */
    fun storeState(configuration: Configuration) {
        configuration.setProperty(MAP_REDUCE, this.getClass().getName())
    }

    /**
     * When it is necessary to load the state of a MapReduce job, this method is called.
     * This is typically required when the MapReduce job needs to be serialized to another machine.
     * Note that what is loaded is simply the instance state, not any processed data.
     *
     *
     * It is important that the state loaded from loadState() is identical to any state created from a constructor.
     * For those GraphComputers that do not need to use Configurations to migrate state between JVMs, the constructor will only be used.
     *
     * @param graph         the graph the MapReduce job will run against
     * @param configuration the configuration to load the state of the MapReduce job from.
     */
    fun loadState(graph: Graph?, configuration: Configuration?) {}

    /**
     * A MapReduce job can be map-only, map-reduce-only, or map-combine-reduce.
     * Before executing the particular stage, this method is called to determine if the respective stage is defined.
     * This method should return true if the respective stage as a non-default method implementation.
     *
     * @param stage the stage to check for definition.
     * @return whether that stage should be executed.
     */
    fun doStage(stage: Stage?): Boolean

    /**
     * The map() method is logically executed at all vertices in the graph in parallel.
     * The map() method emits key/value pairs given some analysis of the data in the vertices (and/or its incident edges).
     * All [MapReduce] classes must at least provide an implementation of `MapReduce#map(Vertex, MapEmitter)`.
     *
     * @param vertex  the current vertex being map() processed.
     * @param emitter the component that allows for key/value pairs to be emitted to the next stage.
     */
    fun map(vertex: Vertex?, emitter: MapEmitter<MK, MV>?)

    /**
     * The combine() method is logically executed at all "machines" in parallel.
     * The combine() method pre-combines the values for a key prior to propagation over the wire.
     * The combine() method must emit the same key/value pairs as the reduce() method.
     * If there is a combine() implementation, there must be a reduce() implementation.
     * If the MapReduce implementation is single machine, it can skip executing this method as reduce() is sufficient.
     *
     * @param key     the key that has aggregated values
     * @param values  the aggregated values associated with the key
     * @param emitter the component that allows for key/value pairs to be emitted to the reduce stage.
     */
    fun combine(key: MK, values: Iterator<MV>?, emitter: ReduceEmitter<RK, RV>?) {}

    /**
     * The reduce() method is logically on the "machine" the respective key hashes to.
     * The reduce() method combines all the values associated with the key and emits key/value pairs.
     *
     * @param key     the key that has aggregated values
     * @param values  the aggregated values associated with the key
     * @param emitter the component that allows for key/value pairs to be emitted as the final result.
     */
    fun reduce(key: MK, values: Iterator<MV>?, emitter: ReduceEmitter<RK, RV>?) {}

    /**
     * This method is called at the start of the respective [MapReduce.Stage] for a particular "chunk of vertices."
     * The set of vertices in the graph are typically not processed with full parallelism.
     * The vertex set is split into subsets and a worker is assigned to call the MapReduce methods on it method.
     * The default implementation is a no-op.
     *
     * @param stage the stage of the MapReduce computation
     */
    fun workerStart(stage: Stage?) {}

    /**
     * This method is called at the end of the respective [MapReduce.Stage] for a particular "chunk of vertices."
     * The set of vertices in the graph are typically not processed with full parallelism.
     * The vertex set is split into subsets and a worker is assigned to call the MapReduce methods on it method.
     * The default implementation is a no-op.
     *
     * @param stage the stage of the MapReduce computation
     */
    fun workerEnd(stage: Stage?) {}

    /**
     * If a [Comparator] is provided, then all pairs leaving the [MapEmitter] are sorted.
     * The sorted results are either fed sorted to the combine/reduce-stage or as the final output.
     * If sorting is not required, then [Optional.empty] should be returned as sorting is computationally expensive.
     * The default implementation returns [Optional.empty].
     *
     * @return an [Optional] of a comparator for sorting the map output.
     */
    val mapKeySort: Optional<Comparator<MK>?>?
        get() = Optional.empty()

    /**
     * If a [Comparator] is provided, then all pairs leaving the [ReduceEmitter] are sorted.
     * If sorting is not required, then [Optional.empty] should be returned as sorting is computationally expensive.
     * The default implementation returns [Optional.empty].
     *
     * @return an [Optional] of a comparator for sorting the reduce output.
     */
    val reduceKeySort: Optional<Comparator<RK>?>?
        get() = Optional.empty()

    /**
     * The key/value pairs emitted by reduce() (or map() in a map-only job) can be iterated to generate a local JVM Java object.
     *
     * @param keyValues the key/value pairs that were emitted from reduce() (or map() in a map-only job)
     * @return the resultant object formed from the emitted key/values.
     */
    fun generateFinalResult(keyValues: Iterator<KeyValue<RK, RV>?>?): R

    /**
     * The results of the MapReduce job are associated with a memory-key to ultimately be stored in [Memory].
     *
     * @return the memory key of the generated result object.
     */
    val memoryKey: String?

    /**
     * The final result can be generated and added to [Memory] and accessible via [DefaultComputerResult].
     * The default simply takes the object from generateFinalResult() and adds it to the Memory given getMemoryKey().
     *
     * @param memory    the memory of the [GraphComputer]
     * @param keyValues the key/value pairs emitted from reduce() (or map() in a map only job).
     */
    fun addResultToMemory(memory: Memory.Admin, keyValues: Iterator<KeyValue<RK, RV>?>?) {
        memory.set(memoryKey, generateFinalResult(keyValues))
    }

    /**
     * When multiple workers on a single machine need MapReduce instances, it is possible to use clone.
     * This will provide a speedier way of generating instances, over the [MapReduce.storeState] and [MapReduce.loadState] model.
     * The default implementation simply returns the object as it assumes that the MapReduce instance is a stateless singleton.
     *
     * @return A clone of the MapReduce object
     */
    @SuppressWarnings("CloneDoesntDeclareCloneNotSupportedException")
    fun clone(): MapReduce<MK, MV, RK, RV, R>?
    //////////////////
    /**
     * The MapEmitter is used to emit key/value pairs from the map() stage of the MapReduce job.
     * The implementation of MapEmitter is up to the vendor, not the developer.
     *
     * @param <K> the key type
     * @param <V> the value type
    </V></K> */
    interface MapEmitter<K, V> {
        fun emit(key: K, value: V)

        /**
         * A default method that assumes the key is [MapReduce.NullObject].
         *
         * @param value the value to emit.
         */
        fun emit(value: V) {
            this.emit(NullObject.instance() as K, value)
        }
    }

    /**
     * The ReduceEmitter is used to emit key/value pairs from the combine() and reduce() stages of the MapReduce job.
     * The implementation of ReduceEmitter is up to the vendor, not the developer.
     *
     * @param <OK> the key type
     * @param <OV> the value type
    </OV></OK> */
    interface ReduceEmitter<OK, OV> {
        fun emit(key: OK, value: OV)

        /**
         * A default method that assumes the key is [MapReduce.NullObject].
         *
         * @param value the value to emit.
         */
        fun emit(value: OV) {
            this.emit(NullObject.instance() as OK, value)
        }
    }
    //////////////////
    /**
     * A convenience singleton when a single key is needed so that all emitted values converge to the same combiner/reducer.
     */
    class NullObject : Comparable<NullObject?>, Serializable {
        @Override
        override fun hashCode(): Int {
            return -9832049
        }

        @Override
        override fun equals(`object`: Object): Boolean {
            return this === `object` || `object` is NullObject
        }

        @Override
        override fun compareTo(`object`: NullObject?): Int {
            return 0
        }

        @Override
        override fun toString(): String {
            return NULL_OBJECT
        }

        companion object {
            private val INSTANCE = NullObject()
            private const val NULL_OBJECT = ""
            fun instance(): NullObject {
                return INSTANCE
            }
        }
    }

    companion object {
        const val MAP_REDUCE = "gremlin.mapReduce"

        /**
         * A helper method to construct a [MapReduce] given the content of the supplied configuration.
         * The class of the MapReduce is read from the [MapReduce.MAP_REDUCE] static configuration key.
         * Once the MapReduce is constructed, [MapReduce.loadState] method is called with the provided configuration.
         *
         * @param graph         The graph that the MapReduce job will run against
         * @param configuration A configuration with requisite information to build a MapReduce
         * @return the newly constructed MapReduce
         */
        fun <M : MapReduce<*, *, *, *, *>?> createMapReduce(graph: Graph?, configuration: Configuration): M {
            return try {
                val mapReduceClass: Class<M> = Class.forName(configuration.getString(MAP_REDUCE)) as Class
                val constructor: Constructor<M> = mapReduceClass.getDeclaredConstructor()
                constructor.setAccessible(true)
                val mapReduce: M = constructor.newInstance()
                mapReduce!!.loadState(graph, configuration)
                mapReduce
            } catch (e: Exception) {
                throw IllegalStateException(e.getMessage(), e)
            }
        }
    }
}