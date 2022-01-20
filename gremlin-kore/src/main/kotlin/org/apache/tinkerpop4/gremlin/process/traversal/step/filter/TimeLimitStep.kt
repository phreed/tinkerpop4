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
package org.apache.tinkerpop4.gremlin.process.traversal.step.filter

import org.apache.tinkerpop4.gremlin.process.traversal.Traversal

/**
 * @author Randall Barnhart (random pi)
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class TimeLimitStep<S>(traversal: Traversal.Admin?, private val timeLimit: Long) : FilterStep<S>(traversal) {
    private var startTime: AtomicLong = AtomicLong(-1)
    private var timedOut: AtomicBoolean = AtomicBoolean(false)
    @Override
    protected fun filter(traverser: Traverser.Admin<S>?): Boolean {
        if (startTime.get() === -1L) startTime.set(System.currentTimeMillis())
        if (System.currentTimeMillis() - startTime.get() >= timeLimit) {
            timedOut.set(true)
            throw FastNoSuchElementException.instance()
        }
        return true
    }

    @Override
    override fun toString(): String {
        return StringFactory.stepString(this, timeLimit)
    }

    @Override
    fun reset() {
        super.reset()
        startTime.set(-1L)
        timedOut.set(false)
    }

    fun getTimedOut(): Boolean {
        return timedOut.get()
    }

    @Override
    fun clone(): TimeLimitStep<S> {
        val clone = super.clone() as TimeLimitStep<S>
        clone.timedOut = AtomicBoolean(timedOut.get())
        clone.startTime = AtomicLong(startTime.get())
        return clone
    }

    @Override
    override fun hashCode(): Int {
        return super.hashCode() xor Long.hashCode(timeLimit)
    }
}