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
package org.apache.tinkerpop4.gremlin.language.grammar

import org.antlr.v4.runtime.tree.ParseTree
import org.apache.tinkerpop4.gremlin.process.traversal.Scope

/**
 * Traversal enum parser parses all the enums like (e.g. [Scope] in graph traversal.
 */
object TraversalEnumParser {
    /**
     * Parse enum text from a parse tree context into a enum object
     * @param enumType : class of enum
     * @param context : parse tree context
     * @return enum object
     */
    fun <E : Enum<E>?, C : ParseTree?> parseTraversalEnumFromContext(enumType: Class<E>, context: C): E {
        val text: String = context.getText()
        val className: String = enumType.getSimpleName()

        // Support qualified class names like (ex: T.id or Scope.local)
        return if (text.startsWith(className)) {
            val strippedText: String = text.substring(className.length() + 1)
            E?.valueOf(enumType, strippedText)
        } else {
            E?.valueOf(enumType, text)
        }
    }
}