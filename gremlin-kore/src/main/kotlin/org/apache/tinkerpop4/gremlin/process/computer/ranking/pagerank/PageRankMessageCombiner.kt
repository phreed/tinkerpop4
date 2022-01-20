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

import org.apache.tinkerpop4.gremlin.process.computer.MessageCombiner

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class PageRankMessageCombiner private constructor() : MessageCombiner<Double?> {
    @Override
    fun combine(messageA: Double, messageB: Double): Double {
        return messageA + messageB
    }

    companion object {
        private val INSTANCE: Optional<PageRankMessageCombiner> = Optional.of(PageRankMessageCombiner())
        fun instance(): Optional<PageRankMessageCombiner> {
            return INSTANCE
        }
    }
}