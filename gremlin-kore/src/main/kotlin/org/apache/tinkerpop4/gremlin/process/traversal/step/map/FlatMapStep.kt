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
package org.apache.tinkerpop4.gremlin.process.traversal.step.map

import org.apache.tinkerpop4.gremlin.process.traversal.Traversal
import org.apache.tinkerpop4.gremlin.process.traversal.Traverser
import org.apache.tinkerpop4.gremlin.process.traversal.step.util.AbstractStep
import org.apache.tinkerpop4.gremlin.structure.util.CloseableIterator
import org.apache.tinkerpop4.gremlin.util.iterator.EmptyIterator
import java.util.Iterator

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
abstract class FlatMapStep<S, E>(traversal: Traversal.Admin?) : AbstractStep<S, E>(traversal) {
    private var head: Traverser.Admin<S>? = null
    private var iterator: Iterator<E> = EmptyIterator.instance()
    @Override
    protected fun processNextStart(): Traverser.Admin<E> {
        while (true) {
            if (iterator.hasNext()) {
                return head.split(iterator.next(), this)
            } else {
                closeIterator()
                head = this.starts.next()
                iterator = flatMap(head)
            }
        }
    }

    protected abstract fun flatMap(traverser: Traverser.Admin<S>?): Iterator<E>
    @Override
    fun reset() {
        super.reset()
        closeIterator()
        iterator = EmptyIterator.instance()
    }

    protected fun closeIterator() {
        CloseableIterator.closeIterator(iterator)
    }
}