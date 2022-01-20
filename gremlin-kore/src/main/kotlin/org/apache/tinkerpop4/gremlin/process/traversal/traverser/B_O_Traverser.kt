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
package org.apache.tinkerpop4.gremlin.process.traversal.traverser

import org.apache.tinkerpop4.gremlin.process.traversal.Traverser

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class B_O_Traverser<T> : O_Traverser<T> {
    protected var bulk = 1L

    @get:Override
    @set:Override
    var stepId: String = HALT

    protected constructor() {}
    constructor(t: T, initialBulk: Long) : super(t) {
        bulk = initialBulk
    }

    @Override
    fun setBulk(count: Long) {
        bulk = count
    }

    @Override
    fun bulk(): Long {
        return bulk
    }

    @Override
    fun merge(other: Traverser.Admin<*>) {
        super.merge(other)
        bulk = bulk + other.bulk()
    }

    @Override
    override fun hashCode(): Int {
        return super.hashCode()
    }

    protected fun equals(other: B_O_Traverser<*>): Boolean {
        return super.equals(other) && other.stepId.equals(stepId)
    }

    @Override
    override fun equals(`object`: Object?): Boolean {
        return `object` is B_O_Traverser<*> && this.equals(`object` as B_O_Traverser<*>?)
    }
}