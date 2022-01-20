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

import org.javatuples.Triplet

/**
 * Base class for "TraversalExplanation" instances and centralizes the key functionality which is the job of doing
 * [.prettyPrint].
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
abstract class AbstractExplanation {
    protected abstract val strategyTraversalsAsString: Stream<String?>?
    protected val traversalStepsAsString: Stream<String>
        protected get() = Stream.concat(Stream.of(originalTraversalAsString), intermediates.map(Triplet::getValue2))
    protected abstract val originalTraversalAsString: String

    /**
     * First string is the traversal strategy, the second is the category and the third is the traversal
     * representation at that point.
     */
    protected abstract val intermediates: Stream<Triplet<String?, String?, String?>?>?

    @Override
    override fun toString(): String {
        return this.prettyPrint(Integer.MAX_VALUE)
    }

    fun prettyPrint(): String {
        return this.prettyPrint(100)
    }

    /**
     * A pretty-print representation of the traversal explanation.
     *
     * @return a [String] representation of the traversal explanation
     */
    fun prettyPrint(maxLineLength: Int): String {
        val originalTraversal = "Original Traversal"
        val finalTraversal = "Final Traversal"
        val maxStrategyColumnLength: Int = strategyTraversalsAsString
            .map(String::length)
            .max(Comparator.naturalOrder())
            .orElse(15)
        val newLineIndent = maxStrategyColumnLength + 10
        val maxTraversalColumn = maxLineLength - newLineIndent
        if (maxTraversalColumn < 1) throw IllegalArgumentException(
            "The maximum line length is too small to present the " + TraversalExplanation::class.java.getSimpleName()
                .toString() + ": " + maxLineLength
        )
        val largestTraversalColumn: Int = traversalStepsAsString
            .map { s -> wordWrap(s, maxTraversalColumn, newLineIndent) }
            .flatMap { s -> Stream.of(s.split("\n")) }
            .map(String::trim)
            .map { s -> if (s.trim().startsWith("[")) s else "   $s" } // 3 indent on new lines
            .map(String::length)
            .max(Comparator.naturalOrder())
            .get()
        val builder = StringBuilder("Traversal Explanation\n")
        for (i in 0 until maxStrategyColumnLength + 7 + largestTraversalColumn) {
            builder.append("=")
        }
        spacing(originalTraversal, maxStrategyColumnLength, builder)
        builder.append(wordWrap(originalTraversalAsString, maxTraversalColumn, newLineIndent))
        builder.append("\n\n")
        val intermediates: List<Triplet<String, String, String>> = intermediates.collect(Collectors.toList())
        for (t in intermediates) {
            builder.append(t.getValue0())
            val spacesToAdd: Int = maxStrategyColumnLength - t.getValue0().length() + 1
            for (i in 0 until spacesToAdd) {
                builder.append(" ")
            }
            builder.append("[").append(t.getValue1().substring(0, 1)).append("]")
            for (i in 0..2) {
                builder.append(" ")
            }
            builder.append(wordWrap(t.getValue2(), maxTraversalColumn, newLineIndent)).append("\n")
        }
        spacing(finalTraversal, maxStrategyColumnLength, builder)
        builder.append(
            wordWrap(
                if (intermediates.size() > 0) intermediates[intermediates.size() - 1].getValue2() else originalTraversalAsString,
                maxTraversalColumn,
                newLineIndent
            )
        )
        return builder.toString()
    }

    private fun wordWrap(longString: String, maxLengthPerLine: Int, newLineIndent: Int): String {
        if (longString.length() <= maxLengthPerLine) return longString
        val builder = StringBuilder()
        var counter = 0
        for (i in 0 until longString.length()) {
            if (0 == counter) {
                builder.append(longString.charAt(i))
            } else if (counter < maxLengthPerLine) {
                builder.append(longString.charAt(i))
            } else {
                builder.append("\n")
                for (j in 0 until newLineIndent) {
                    builder.append(" ")
                }
                builder.append(longString.charAt(i))
                counter = 0
            }
            counter++
        }
        return builder.toString()
    }

    companion object {
        fun spacing(finalTraversal: String, maxStrategyColumnLength: Int, builder: StringBuilder) {
            builder.append("\n")
            builder.append(finalTraversal)
            for (i in 0 until maxStrategyColumnLength - finalTraversal.length() + 7) {
                builder.append(" ")
            }
        }
    }
}