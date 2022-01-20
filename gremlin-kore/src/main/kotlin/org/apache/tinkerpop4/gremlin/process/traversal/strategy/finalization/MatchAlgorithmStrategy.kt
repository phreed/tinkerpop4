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
package org.apache.tinkerpop4.gremlin.process.traversal.strategy.finalization

import org.apache.commons.configuration2.Configuration

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class MatchAlgorithmStrategy : AbstractTraversalStrategy<FinalizationStrategy?>, FinalizationStrategy {
    private var matchAlgorithmClass: Class<out MatchAlgorithm?>? = null

    private constructor() {
        // for serialization
    }

    private constructor(matchAlgorithmClass: Class<out MatchAlgorithm?>) {
        this.matchAlgorithmClass = matchAlgorithmClass
    }

    @Override
    fun apply(traversal: Traversal.Admin<*, *>) {
        for (step in traversal.getSteps()) {
            if (step is MatchStep) {
                (step as MatchStep).setMatchAlgorithm(matchAlgorithmClass)
            }
        }
    }

    @get:Override
    val configuration: Configuration
        get() = MapConfiguration(
            Collections.singletonMap(
                MATCH_ALGORITHM,
                if (null != matchAlgorithmClass.getDeclaringClass()) matchAlgorithmClass.getCanonicalName().replace(
                    "." + matchAlgorithmClass.getSimpleName(),
                    "$" + matchAlgorithmClass.getSimpleName()
                ) else matchAlgorithmClass.getCanonicalName()
            )
        )

    @Override
    override fun toString(): String {
        return StringFactory.traversalStrategyString(this)
    }

    class Builder {
        private var matchAlgorithmClass: Class<out MatchAlgorithm?> = CountMatchAlgorithm::class.java
        fun algorithm(matchAlgorithmClass: Class<out MatchAlgorithm?>): Builder {
            this.matchAlgorithmClass = matchAlgorithmClass
            return this
        }

        fun create(): MatchAlgorithmStrategy {
            return MatchAlgorithmStrategy(matchAlgorithmClass)
        }
    }

    companion object {
        private const val MATCH_ALGORITHM = "matchAlgorithm"
        fun create(configuration: Configuration): MatchAlgorithmStrategy {
            return try {
                MatchAlgorithmStrategy(Class.forName(configuration.getString(MATCH_ALGORITHM)) as Class)
            } catch (e: ClassNotFoundException) {
                throw IllegalArgumentException(e.getMessage(), e)
            }
        }

        fun build(): Builder {
            return Builder()
        }
    }
}