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
package org.apache.tinkerpop4.gremlin.process.traversal.step.sideEffect

import org.apache.tinkerpop4.gremlin.process.traversal.Traversal
import org.apache.tinkerpop4.gremlin.process.traversal.Traverser
import org.apache.tinkerpop4.gremlin.process.traversal.step.LambdaHolder
import org.apache.tinkerpop4.gremlin.structure.util.StringFactory
import java.util.function.Consumer

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class LambdaSideEffectStep<S>(traversal: Traversal.Admin?, consumer: Consumer<Traverser<S>?>) :
    SideEffectStep<S>(traversal), LambdaHolder {
    private val consumer: Consumer<Traverser<S>>

    init {
        this.consumer = consumer
    }

    fun getConsumer(): Consumer<Traverser<S>> {
        return consumer
    }

    @Override
    protected fun sideEffect(traverser: Traverser.Admin<S>?) {
        consumer.accept(traverser)
    }

    @Override
    override fun toString(): String {
        return StringFactory.stepString(this, consumer)
    }

    @Override
    override fun hashCode(): Int {
        return super.hashCode() xor consumer.hashCode()
    }
}