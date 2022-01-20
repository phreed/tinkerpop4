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
package org.apache.tinkerpop4.gremlin.process.traversal.util

import org.apache.tinkerpop4.gremlin.process.traversal.TraversalSideEffects

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
object SideEffectHelper {
    @Throws(IllegalArgumentException::class)
    fun validateSideEffectKey(key: String?) {
        if (null == key) throw TraversalSideEffects.Exceptions.sideEffectKeyCanNotBeNull()
        if (key.isEmpty()) throw TraversalSideEffects.Exceptions.sideEffectKeyCanNotBeEmpty()
    }

    @Deprecated
    @Deprecated(
        """As of release 3.5.3, not replaced as there is really no general validation anymore for values since
      {@code null} is now accepted."""
    )
    @Throws(
        IllegalArgumentException::class
    )
    fun validateSideEffectValue(value: Object?) {
        if (null == value) throw TraversalSideEffects.Exceptions.sideEffectValueCanNotBeNull()
    }

    @Deprecated
    @Deprecated(
        """As of release 3.5.3, not replaced as there is really no general validation anymore for values since
      {@code null} is now accepted."""
    )
    @Throws(
        IllegalArgumentException::class
    )
    fun validateSideEffectKeyValue(key: String?, value: Object?) {
        validateSideEffectKey(key)
        validateSideEffectValue(value)
    }
}