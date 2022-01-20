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
package org.apache.tinkerpop4.gremlin.process.traversal.util

import java.io.Serializable

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class StepPosition private constructor(// step in traversal length
    var x: Int, // depth in traversal nested tree
    var y: Int, // breadth in traversal siblings
    var z: Int, // the traversal holder id
    var parentId: String
) : Serializable {
    constructor() : this(0, 0, 0, EMPTY_STRING) {}

    fun nextXId(): String {
        return x++ + DOT + y + DOT + z + LEFT_PARENTHESES + parentId + RIGHT_PARENTHESES
    }

    @Override
    override fun toString(): String {
        return x + DOT + y + DOT + z + LEFT_PARENTHESES + parentId + RIGHT_PARENTHESES
    }

    companion object {
        private const val DOT = "."
        private const val LEFT_PARENTHESES = "("
        private const val RIGHT_PARENTHESES = ")"
        private const val EMPTY_STRING = ""
        fun isStepId(maybeAStepId: String): Boolean {
            return maybeAStepId.matches("[0-9]+\\.[0-9]+\\.[0-9]+\\([0-9\\.\\(\\)]*\\)")
        }
    }
}