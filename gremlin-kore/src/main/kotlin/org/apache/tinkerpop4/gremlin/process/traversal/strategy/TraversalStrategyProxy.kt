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
package org.apache.tinkerpop4.gremlin.process.traversal.strategy

import org.apache.commons.configuration2.Configuration
import org.apache.tinkerpop4.gremlin.process.traversal.Bytecode
import org.apache.tinkerpop4.gremlin.process.traversal.Traversal
import org.apache.tinkerpop4.gremlin.process.traversal.TraversalSource
import org.apache.tinkerpop4.gremlin.process.traversal.TraversalStrategy
import org.apache.tinkerpop4.gremlin.structure.util.StringFactory
import java.io.Serializable

/**
 * This class is for use with [Bytecode] and for serialization purposes. It is not meant for direct use with
 * [TraversalSource.withStrategies].
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class TraversalStrategyProxy<T : TraversalStrategy?>(strategyClass: Class<T>, configuration: Configuration) :
    Serializable, TraversalStrategy {
    private val configuration: Configuration
    private val strategyClass: Class<T>

    constructor(traversalStrategy: T) : this(
        traversalStrategy.getClass() as Class<T>,
        traversalStrategy.getConfiguration()
    ) {
    }

    init {
        this.configuration = configuration
        this.strategyClass = strategyClass
    }

    fun getConfiguration(): Configuration {
        return configuration
    }

    fun getStrategyClass(): Class<T> {
        return strategyClass
    }

    @Override
    fun apply(traversal: Traversal.Admin?) {
        throw UnsupportedOperationException("TraversalStrategyProxy is not meant to be used directly as a TraversalStrategy and is for serialization purposes only")
    }

    @Override
    operator fun compareTo(o: Object?): Int {
        throw UnsupportedOperationException("TraversalStrategyProxy is not meant to be used directly as a TraversalStrategy and is for serialization purposes only")
    }

    @Override
    override fun toString(): String {
        return StringFactory.traversalStrategyProxyString(this)
    }
}