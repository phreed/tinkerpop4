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
package org.apache.tinkerpop4.gremlin.process.traversal.strategy.decoration

import org.apache.commons.configuration2.Configuration

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class HaltedTraverserStrategy private constructor(haltedTraverserFactory: Class) :
    AbstractTraversalStrategy<DecorationStrategy?>(), DecorationStrategy {
    private val haltedTraverserFactory: Class? = null
    private var useReference = false
    fun apply(traversal: Traversal.Admin<*, *>?) {
        // do nothing as this is simply a metadata strategy
    }

    fun getHaltedTraverserFactory(): Class? {
        return haltedTraverserFactory
    }

    fun <R> halt(traverser: Traverser.Admin<R>): Traverser.Admin<R> {
        if (useReference) traverser.set(ReferenceFactory.detach(traverser.get())) else traverser.set(
            DetachedFactory.detach(
                traverser.get(),
                true
            )
        )
        return traverser
    }

    init {
        if (haltedTraverserFactory.equals(DetachedFactory::class.java) || haltedTraverserFactory.equals(ReferenceFactory::class.java)) {
            this.haltedTraverserFactory = haltedTraverserFactory
            useReference = ReferenceFactory::class.java.equals(this.haltedTraverserFactory)
        } else throw IllegalArgumentException("The provided traverser detachment factory is unknown: $haltedTraverserFactory")
    }

    @get:Override
    val configuration: Configuration
        get() {
            val map: Map<String, Object> = HashMap()
            map.put(STRATEGY, HaltedTraverserStrategy::class.java.getCanonicalName())
            map.put(HALTED_TRAVERSER_FACTORY, haltedTraverserFactory.getCanonicalName())
            return MapConfiguration(map)
        }

    companion object {
        const val HALTED_TRAVERSER_FACTORY = "haltedTraverserFactory"
        fun create(configuration: Configuration): HaltedTraverserStrategy {
            return try {
                HaltedTraverserStrategy(Class.forName(configuration.getString(HALTED_TRAVERSER_FACTORY)))
            } catch (e: ClassNotFoundException) {
                throw IllegalArgumentException(e.getMessage(), e)
            }
        }

        ////////////
        fun detached(): HaltedTraverserStrategy {
            return HaltedTraverserStrategy(DetachedFactory::class.java)
        }

        fun reference(): HaltedTraverserStrategy {
            return HaltedTraverserStrategy(ReferenceFactory::class.java)
        }
    }
}