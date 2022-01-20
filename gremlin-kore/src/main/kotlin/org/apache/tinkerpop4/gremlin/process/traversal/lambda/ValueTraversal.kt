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
package org.apache.tinkerpop4.gremlin.process.traversal.lambda

import org.apache.tinkerpop4.gremlin.process.traversal.Traversal

/**
 * More efficiently extracts a value from an [Element] or `Map` to avoid strategy application costs.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class ValueTraversal<T, V> : AbstractLambdaTraversal<T, V> {
    val propertyKey: String

    /**
     * Gets the value of the [Traversal] which will be `null` until [.addStart] is
     * called.
     */
    var value: V? = null
        private set

    /**
     * Determines if there is a value to iterate from the [Traversal].
     */
    var isNoStarts = false
        private set

    /**
     * Creates an instance for the specified `propertyKey`.
     */
    constructor(propertyKey: String) {
        this.propertyKey = propertyKey
    }

    /**
     * Creates an instance with the `bypassTraversal` set on construction.
     */
    constructor(propertyKey: String, bypassTraversal: Traversal.Admin<T, V>?) {
        this.propertyKey = propertyKey
        this.setBypassTraversal(bypassTraversal)
    }

    /**
     * Return the `value` of the traverser and will continue to return it on subsequent calls. Calling methods
     * should account for this behavior on their own.
     */
    @Override
    operator fun next(): V {
        if (isNoStarts) throw NoSuchElementException(String.format("%s is empty", this.toString()))
        return value
    }

    /**
     * If there is a "start" traverser this method will return `true` and otherwise `false` and in either
     * case will always return as such, irrespective of calls to [.next]. Calling methods should account for
     * this behavior on their own.
     */
    @Override
    operator fun hasNext(): Boolean {
        // value traversal is a bit of a special case for next/hasNext(). prior to "noStarts" this method would always
        // return true and next() would always return the value. in other words if the ValueTraversal had a successful
        // addStart(Traverser) it would indefinitely return the start. other parts of the processing engine must
        // account for this behavior and only call the ValueTraversal once. With the change to use "noStarts" we rely
        // on that same behavior in the processing engine and assume a single call but now hasNext() will always
        // either always be true or always be false and next() will throw NoSuchElementException
        return !isNoStarts
    }

    @Override
    fun addStart(start: Traverser.Admin<T>) {
        if (null == this.bypassTraversal) {
            val o: T = start.get()
            if (o is Element) {
                // if the property is not present then it has noStarts which means hasNext() return false
                val p: Property<V> = (o as Element).property(propertyKey)
                if (p.isPresent()) value = p.value() else isNoStarts = true
            } else if (o is Map) value = (o as Map).get(propertyKey) as V else throw IllegalStateException(
                String.format(
                    "The by(\"%s\") modulator can only be applied to a traverser that is an Element or a Map - it is being applied to [%s] a %s class instead",
                    propertyKey, o, o.getClass().getSimpleName()
                )
            )
        } else {
            value = TraversalUtil.apply(start, this.bypassTraversal)
        }
    }

    @Override
    fun reset() {
        super.reset()
        isNoStarts = false
    }

    @Override
    override fun toString(): String {
        return "value(" + (if (null == this.bypassTraversal) propertyKey else this.bypassTraversal) + ')'
    }

    @Override
    override fun hashCode(): Int {
        var hc = 19
        hc = 43 * hc + super.hashCode()
        hc = 43 * hc + propertyKey.hashCode()
        return hc
    }

    @Override
    override fun equals(other: Object): Boolean {
        return (other is ValueTraversal<*, *>
                && Objects.equals((other as ValueTraversal<*, *>).propertyKey, propertyKey))
    }
}