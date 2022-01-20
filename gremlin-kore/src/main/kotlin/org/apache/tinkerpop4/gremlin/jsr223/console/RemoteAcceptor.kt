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

import java.io.Closeable

/**
 * The Gremlin Console supports the `:remote` and `:submit` commands which provide standardized ways
 * for plugins to provide "remote connections" to resources and a way to "submit" a command to those resources.
 * A "remote connection" does not necessarily have to be a remote server.  It simply refers to a resource that is
 * external to the console.
 *
 *
 * By implementing this interface and returning an instance of it through
 * [ConsoleCustomizer.getRemoteAcceptor] a plugin can hook into those commands and
 * provide remoting features.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
interface RemoteAcceptor : Closeable {
    /**
     * Gets called when `:remote` is used in conjunction with the "connect" option.  It is up to the
     * implementation to decide how additional arguments on the line should be treated after "connect".
     *
     * @return an object to display as output to the user
     * @throws RemoteException if there is a problem with connecting
     */
    @Throws(RemoteException::class)
    fun connect(args: List<String?>?): Object?

    /**
     * Gets called when `:remote` is used in conjunction with the `config` option.  It is up to the
     * implementation to decide how additional arguments on the line should be treated after `config`.
     *
     * @return an object to display as output to the user
     * @throws RemoteException if there is a problem with configuration
     */
    @Throws(RemoteException::class)
    fun configure(args: List<String?>?): Object?

    /**
     * Gets called when `:submit` is executed.  It is up to the implementation to decide how additional
     * arguments on the line should be treated after `:submit`.
     *
     * @return an object to display as output to the user
     * @throws RemoteException if there is a problem with submission
     */
    @Throws(RemoteException::class)
    fun submit(args: List<String?>?): Object?

    /**
     * If the `RemoteAcceptor` is used in the Gremlin Console, then this method might be called to determine
     * if it can be used in a fashion that supports the `:remote console` command.  By default, this value is
     * set to `false`.
     *
     *
     * A `RemoteAcceptor` should only return `true` for this method if it expects that all activities it
     * supports are executed through the `:submit` command. If the users interaction with the remote requires
     * working with both local and remote evaluation at the same time, it is likely best to keep this method return
     * `false`. A good example of this type of plugin would be the Gephi Plugin which uses `:remote config`
     * to configure a local `TraversalSource` to be used and expects calls to `:submit` for the same body
     * of analysis.
     */
    fun allowRemoteConsole(): Boolean {
        return false
    }

    companion object {
        const val RESULT = "result"
    }
}