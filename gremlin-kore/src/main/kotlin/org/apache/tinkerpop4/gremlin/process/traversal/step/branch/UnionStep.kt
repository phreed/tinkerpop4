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
package org.apache.tinkerpop4.gremlin.process.traversal.step.branch

import org.apache.tinkerpop4.gremlin.process.traversal.Traversal

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class UnionStep<S, E>(traversal: Traversal.Admin?, vararg unionTraversals: Traversal.Admin<*, E>?) :
    BranchStep<S, E, Pick?>(traversal) {
    init {
        this.setBranchTraversal(ConstantTraversal(Pick.any))
        for (union in unionTraversals) {
            addGlobalChildOption(Pick.any, union as Traversal.Admin?)
        }
    }

    @Override
    fun addGlobalChildOption(pickToken: Pick, traversalOption: Traversal.Admin<S, E>?) {
        if (Pick.any !== pickToken) throw IllegalArgumentException("Union step only supports the any token: $pickToken")
        super.addGlobalChildOption(pickToken, traversalOption)
    }

    @Override
    override fun toString(): String {
        return StringFactory.stepString(this, this.traversalPickOptions.getOrDefault(Pick.any, Collections.emptyList()))
    }
}