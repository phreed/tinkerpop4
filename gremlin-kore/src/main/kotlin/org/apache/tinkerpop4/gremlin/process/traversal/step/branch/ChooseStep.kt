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
import org.apache.tinkerpop4.gremlin.process.traversal.step.map.HasNextStep

/**
 * A step which offers a choice of two or more Traversals to take.
 *
 * @author Joshua Shinavier (http://fortytwo.net)
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class ChooseStep<S, E, M>(traversal: Traversal.Admin?, choiceTraversal: Traversal.Admin<S, M>?) :
    BranchStep<S, E, M>(traversal) {
    init {
        this.setBranchTraversal(choiceTraversal)
    }

    constructor(
        traversal: Traversal.Admin?,
        predicateTraversal: Traversal.Admin<S, *>,
        trueChoice: Traversal.Admin<S, E>?,
        falseChoice: Traversal.Admin<S, E>?
    ) : this(traversal, predicateTraversal.addStep(HasNextStep(predicateTraversal)) as Traversal.Admin<S, M>) {
        addGlobalChildOption(Boolean.TRUE as M, trueChoice)
        addGlobalChildOption(Boolean.FALSE as M, falseChoice)
    }

    @Override
    fun addGlobalChildOption(pickToken: M, traversalOption: Traversal.Admin<S, E>?) {
        if (pickToken is Pick) {
            if (Pick.any.equals(pickToken)) throw IllegalArgumentException("Choose step can not have an any-option as only one option per traverser is allowed")
            if (this.traversalPickOptions.containsKey(pickToken)) throw IllegalArgumentException("Choose step can only have one traversal per pick token: $pickToken")
        }
        super.addGlobalChildOption(pickToken, traversalOption)
    }
}