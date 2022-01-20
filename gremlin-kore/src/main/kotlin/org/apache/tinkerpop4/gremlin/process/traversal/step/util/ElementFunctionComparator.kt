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
package org.apache.tinkerpop4.gremlin.process.traversal.step.util

import org.apache.tinkerpop4.gremlin.structure.Element
import java.io.Serializable
import java.util.Comparator
import java.util.function.Function

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class ElementFunctionComparator<V>(elementFunction: Function<Element?, V>, valueComparator: Comparator<V>) :
    Comparator<Element?>, Serializable {
    private val elementFunction: Function<Element, V>
    private val valueComparator: Comparator<V>

    init {
        this.elementFunction = elementFunction
        this.valueComparator = valueComparator
    }

    fun getElementFunction(): Function<Element, V> {
        return elementFunction
    }

    fun getValueComparator(): Comparator<V> {
        return valueComparator
    }

    @Override
    fun compare(elementA: Element?, elementB: Element?): Int {
        return valueComparator.compare(elementFunction.apply(elementA), elementFunction.apply(elementB))
    }

    @Override
    override fun toString(): String {
        return valueComparator.toString() + "(" + elementFunction + ')'
    }
}