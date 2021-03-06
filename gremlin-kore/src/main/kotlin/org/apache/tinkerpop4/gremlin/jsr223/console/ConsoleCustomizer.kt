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
package org.apache.tinkerpop4.gremlin.jsr223.console

import org.apache.tinkerpop4.gremlin.jsr223.Customizer
import org.apache.tinkerpop4.gremlin.jsr223.GremlinPlugin

/**
 * A [Customizer] that allows a [RemoteAcceptor] to be used with a console implementation. Only one
 * `ConsoleCustomizer` can be added to a particular [GremlinPlugin].
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
interface ConsoleCustomizer : Customizer {
    /**
     * Allows a plugin to utilize features of the `:remote` and `:submit` commands of the Gremlin Console.
     * This method does not need to be implemented if the plugin is not meant for the Console for some reason or
     * if it does not intend to take advantage of those commands.
     */
    fun getRemoteAcceptor(environment: GremlinShellEnvironment?): RemoteAcceptor?
}