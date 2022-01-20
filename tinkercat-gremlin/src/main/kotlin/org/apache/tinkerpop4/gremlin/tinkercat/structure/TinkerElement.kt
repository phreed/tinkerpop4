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
package org.apache.tinkerpop4.gremlin.tinkercat.structure

import org.apache.tinkerpop4.gremlin.structure.Element
import org.apache.tinkerpop4.gremlin.structure.util.ElementHelper
import java.lang.IllegalStateException

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
abstract class TinkerElement protected constructor(protected val id: Any, protected val label: String) : Element {
    @JvmField
    var removed = false
    override fun hashCode(): Int {
        return ElementHelper.hashCode(this)
    }

    override fun id(): Any {
        return id
    }

    override fun label(): String {
        return label
    }

    override fun equals(`object`: Any?): Boolean {
        return ElementHelper.areEqual(this, `object`)
    }

    companion object {
        @JvmStatic
        protected fun elementAlreadyRemoved(clazz: Class<out Element?>, id: Any?): IllegalStateException {
            return IllegalStateException(String.format("%s with id %s was removed.", clazz.simpleName, id))
        }
    }
}