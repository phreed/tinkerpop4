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
package org.apache.tinkerpop4.gremlin.process.computer.util

import org.apache.commons.configuration2.Configuration
import org.apache.tinkerpop4.gremlin.process.computer.MapReduce
import org.apache.tinkerpop4.gremlin.structure.util.StringFactory

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
abstract class StaticMapReduce<MK, MV, RK, RV, R> : MapReduce<MK, MV, RK, RV, R> {
    @Override
    @SuppressWarnings("CloneDoesntCallSuperClone,CloneDoesntDeclareCloneNotSupportedException")
    fun clone(): MapReduce<MK, MV, RK, RV, R> {
        return this
    }

    @Override
    fun storeState(configuration: Configuration?) {
        super@MapReduce.storeState(configuration)
    }

    @Override
    override fun toString(): String {
        return StringFactory.mapReduceString(this, this.getMemoryKey())
    }

    @Override
    override fun equals(`object`: Object?): Boolean {
        return GraphComputerHelper.areEqual(this, `object`)
    }

    @Override
    override fun hashCode(): Int {
        return (this.getClass().getCanonicalName() + this.getMemoryKey()).hashCode()
    }
}