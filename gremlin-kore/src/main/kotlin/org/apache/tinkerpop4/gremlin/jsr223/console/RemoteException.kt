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

import java.util.Optional

/**
 * A mapper `Exception` to be thrown when there are problems with processing a command given to a
 * [RemoteAcceptor].  The message provided to the exception will be displayed to the user in the Console.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class RemoteException : Exception {
    private var remoteStackTrace: String? = null

    constructor(message: String?) : this(message, null as String?) {}
    constructor(message: String?, remoteStackTrace: String?) : super(message) {
        this.remoteStackTrace = remoteStackTrace
    }

    constructor(message: String?, cause: Throwable?) : this(message, cause, null) {}
    constructor(message: String?, cause: Throwable?, remoteStackTrace: String?) : super(message, cause) {
        this.remoteStackTrace = remoteStackTrace
    }

    constructor(cause: Throwable?) : super(cause) {}

    /**
     * The stacktrace produced by the remote server.
     */
    fun getRemoteStackTrace(): Optional<String> {
        return Optional.ofNullable(remoteStackTrace)
    }
}