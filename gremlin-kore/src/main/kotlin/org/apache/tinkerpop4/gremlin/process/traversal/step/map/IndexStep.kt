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
package org.apache.tinkerpop4.gremlin.process.traversal.step.map

import org.apache.tinkerpop4.gremlin.process.traversal.Traversal

/**
 * @author Daniel Kuppitz (http://gremlin.guru)
 */
class IndexStep<S, E>(traversal: Traversal.Admin?) : ScalarMapStep<S, E>(traversal), TraversalParent, Configuring {
    private val parameters: Parameters = Parameters()
    private var indexer: Function<Iterator<*>, Object>? = null

    /**
     * Gets the type of indexer that is configured for this step.
     */
    var indexerType: IndexerType? = null
        private set

    init {
        configure(WithOptions.indexer, WithOptions.list)
    }

    fun getIndexer(): Function<Iterator<*>, Object>? {
        return indexer
    }

    @Override
    protected fun map(traverser: Traverser.Admin<S>): E {
        return indexer.apply(IteratorUtils.asIterator(traverser.get()))
    }

    @Override
    override fun hashCode(): Int {
        return super.hashCode() xor indexer.hashCode()
    }

    @get:Override
    val requirements: Set<Any>
        get() = Collections.singleton(TraverserRequirement.OBJECT)

    @Override
    fun configure(vararg keyValues: Object) {
        val indexer: Int
        if (keyValues[0].equals(WithOptions.indexer)) {
            indexer = if (keyValues.size == 2 && keyValues[1] is Integer) {
                keyValues[1] as Integer
            } else {
                throw INVALID_CONFIGURATION_EXCEPTION
            }
            if (indexer == WithOptions.list) {
                indexerType = IndexerType.LIST
                this.indexer = Function<Iterator<*>, Object> { iterator: Iterator<*> -> indexedList(iterator) }
            } else if (indexer == WithOptions.map) {
                indexerType = IndexerType.MAP
                this.indexer = Function<Iterator<*>, Object> { iterator: Iterator<*> -> indexedMap(iterator) }
            } else {
                throw INVALID_CONFIGURATION_EXCEPTION
            }
        } else {
            parameters.set(this, keyValues)
        }
    }

    @Override
    fun getParameters(): Parameters {
        return parameters
    }

    /**
     * Type of the index as it corresponds to the associated [WithOptions.list] and [WithOptions.map].
     */
    enum class IndexerType(val type: Int) {
        LIST(WithOptions.list), MAP(WithOptions.map);

    }

    companion object {
        private val INVALID_CONFIGURATION_EXCEPTION: IllegalArgumentException = IllegalArgumentException(
            "WithOptions.indexer requires a single Integer argument (possible " + "" +
                    "values are: WithOptions.[list|map])"
        )

        private fun indexedList(iterator: Iterator<*>): List<List<Object>> {
            val list: List<List<Object>> = ArrayList()
            var i = 0
            while (iterator.hasNext()) {
                list.add(Arrays.asList(iterator.next(), i++))
            }
            return Collections.unmodifiableList(list)
        }

        private fun indexedMap(iterator: Iterator<*>): Map<Integer?, Object> {
            val map: Map<Integer, Object> = LinkedHashMap()
            var i = 0
            while (iterator.hasNext()) {
                map.put(i++, iterator.next())
            }
            return Collections.unmodifiableMap(map)
        }
    }
}