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

import org.apache.tinkerpop4.gremlin.process.traversal.Traversal

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
interface PathProcessor {
    enum class ElementRequirement {
        ID, LABEL, PROPERTIES, EDGES
    }

    val maxRequirement: ElementRequirement?
        get() {
            var max = ElementRequirement.ID
            if (this is TraversalParent) {
                for (traversal in (this as TraversalParent).getLocalChildren()) {
                    if (traversal is IdentityTraversal) {
                        if (max.compareTo(ElementRequirement.ID) < 0) max = ElementRequirement.ID
                    } else if (traversal is TokenTraversal && (traversal as TokenTraversal).getToken().equals(T.id)) {
                        if (max.compareTo(ElementRequirement.ID) < 0) max = ElementRequirement.ID
                    } else if (traversal is TokenTraversal && (traversal as TokenTraversal).getToken()
                            .equals(T.label)
                    ) {
                        if (max.compareTo(ElementRequirement.LABEL) < 0) max = ElementRequirement.LABEL
                    } else if (traversal is ValueTraversal) {
                        if (max.compareTo(ElementRequirement.PROPERTIES) < 0) max = ElementRequirement.PROPERTIES
                    } else {
                        max = ElementRequirement.EDGES
                    }
                }
            }
            return max
        }
    var keepLabels: Set<String?>?

    companion object {
        fun <S> processTraverserPathLabels(traverser: Traverser.Admin<S>, labels: Set<String?>?): Traverser.Admin<S>? {
            if (null != labels) traverser.keepLabels(labels)
            return traverser
        }
    }
}