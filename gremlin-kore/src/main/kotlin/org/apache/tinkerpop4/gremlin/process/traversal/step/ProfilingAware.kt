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
package org.apache.tinkerpop4.gremlin.process.traversal.step

import org.apache.tinkerpop4.gremlin.process.computer.MemoryComputeKey

/**
 * Marks a [Step] as one that is aware of profiling. A good example of where this is important is with
 * [GroupStep] which needs to track and hold a [Barrier], which is important for the
 * [ProfileStrategy] to know about. Once the [ProfileStep] is injected the [Barrier] needs to be
 * recalculated so that the timer can be properly started on the associated [ProfileStep]. Without that indirect
 * start of the timer, the operation related to the [Barrier] will not be properly accounted for and when
 * metrics are normalized it is possible to end up with a negative timing.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
interface ProfilingAware {
    /**
     * Prepares the step for any internal changes that might help ensure that profiling will work as expected.
     */
    fun prepareForProfiling()

    /**
     * A helper class which holds a [Barrier] and it's related [ProfileStep] so that the latter can have
     * its timer started and stopped appropriately.
     */
    class ProfiledBarrier(barrier: Barrier, profileStep: ProfileStep) : Barrier {
        private val barrier: Barrier
        private val profileStep: ProfileStep

        init {
            this.barrier = barrier
            this.profileStep = profileStep
        }

        @Override
        fun processAllStarts() {
            profileStep.start()
            barrier.processAllStarts()
            profileStep.stop()
        }

        @Override
        fun hasNextBarrier(): Boolean {
            profileStep.start()
            val b: Boolean = barrier.hasNextBarrier()
            profileStep.stop()
            return b
        }

        @Override
        @Throws(NoSuchElementException::class)
        fun nextBarrier(): Object {
            profileStep.start()
            val o: Object = barrier.nextBarrier()
            profileStep.stop()
            return o
        }

        @Override
        fun addBarrier(barrier: Object?) {
            this.barrier.addBarrier(barrier)
        }

        @get:Override
        val memoryComputeKey: MemoryComputeKey
            get() = barrier.getMemoryComputeKey()

        @Override
        fun done() {
            barrier.done()
        }
    }
}