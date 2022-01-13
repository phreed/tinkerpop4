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
package org.apache.tinkerpop.gremlin.tinkercat.process.computer

import org.apache.tinkerpop.gremlin.process.computer.MessageScope
import org.apache.tinkerpop.gremlin.structure.Vertex
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class TinkerMessageBoard<M> {
    @JvmField
    var sendMessages: MutableMap<MessageScope, MutableMap<Vertex, Queue<M>>> = ConcurrentHashMap()
    @JvmField
    var receiveMessages: Map<MessageScope, Map<Vertex, Queue<M>>> = ConcurrentHashMap()
    var previousMessageScopes: Set<MessageScope> = HashSet()
    var currentMessageScopes: Set<MessageScope> = HashSet()
    fun completeIteration() {
        receiveMessages = sendMessages
        sendMessages = ConcurrentHashMap()
        previousMessageScopes = currentMessageScopes
        currentMessageScopes = HashSet()
    }
}