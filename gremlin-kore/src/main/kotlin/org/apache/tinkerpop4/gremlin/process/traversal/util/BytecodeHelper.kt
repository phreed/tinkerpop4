/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.tinkerpop4.gremlin.process.traversal.util

import org.apache.tinkerpop4.gremlin.process.traversal.Bytecode
import org.apache.tinkerpop4.gremlin.process.traversal.GraphOp
import org.apache.tinkerpop4.gremlin.process.traversal.TraversalSource
import org.apache.tinkerpop4.gremlin.process.traversal.TraversalStrategy
import org.apache.tinkerpop4.gremlin.structure.util.detached.DetachedFactory
import org.apache.tinkerpop4.gremlin.util.function.Lambda
import org.apache.tinkerpop4.gremlin.util.iterator.IteratorUtils
import java.util.ArrayList
import java.util.Iterator
import java.util.List
import java.util.Optional
import java.util.function.Predicate
import java.util.stream.Stream

/**
 * Utility class for parsing [Bytecode].
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
object BytecodeHelper {
    /**
     * Parses [Bytecode] to find [TraversalStrategy] objects added in the source instructions.
     */
    fun <A : TraversalStrategy?> findStrategies(bytecode: Bytecode, clazz: Class<A>): Iterator<A> {
        return IteratorUtils.map(
            IteratorUtils.filter(
                bytecode.getSourceInstructions().iterator()
            ) { s ->
                s.getOperator().equals(TraversalSource.Symbols.withStrategies) && clazz.isAssignableFrom(
                    s.getArguments().get(0).getClass()
                )
            }
        ) { os -> os.getArguments().get(0) }
    }

    fun filterInstructions(bytecode: Bytecode, predicate: Predicate<Bytecode.Instruction?>): Bytecode {
        val clone = Bytecode()
        for (instruction in bytecode.getSourceInstructions()) {
            if (predicate.test(instruction)) clone.addSource(instruction.getOperator(), instruction.getArguments())
        }
        for (instruction in bytecode.getStepInstructions()) {
            if (predicate.test(instruction)) clone.addStep(instruction.getOperator(), instruction.getArguments())
        }
        return clone
    }

    /**
     * Checks if the bytecode is one of the standard [GraphOp] options.
     */
    fun isGraphOperation(bytecode: Bytecode?): Boolean {
        return Stream.of(GraphOp.values()).anyMatch { op -> op.equals(bytecode) }
    }

    fun getLambdaLanguage(bytecode: Bytecode): Optional<String> {
        for (instruction in bytecode.getInstructions()) {
            for (`object` in instruction.getArguments()) {
                if (`object` is Lambda) return Optional.of((`object` as Lambda).getLambdaLanguage()) else if (`object` is Bytecode) {
                    val temp: Optional<String> = getLambdaLanguage(`object` as Bytecode)
                    if (temp.isPresent()) return temp
                }
            }
        }
        return Optional.empty()
    }

    fun removeBindings(bytecode: Bytecode) {
        for (instruction in bytecode.getInstructions()) {
            val arguments: Array<Object> = instruction.getArguments()
            for (i in arguments.indices) {
                if (arguments[i] is Bytecode.Binding) arguments[i] =
                    (arguments[i] as Bytecode.Binding).value() else if (arguments[i] is Bytecode) removeBindings(
                    arguments[i] as Bytecode
                )
            }
        }
    }

    fun detachElements(bytecode: Bytecode) {
        for (instruction in bytecode.getInstructions()) {
            val arguments: Array<Object> = instruction.getArguments()
            for (i in arguments.indices) {
                if (arguments[i] is Bytecode) detachElements(
                    arguments[i] as Bytecode
                ) else if (arguments[i] is List) {
                    val list: List<Object> = ArrayList()
                    for (`object` in arguments[i]) {
                        list.add(DetachedFactory.detach(`object`, false))
                    }
                    arguments[i] = list
                } else arguments[i] = DetachedFactory.detach(arguments[i], false)
            }
        }
    }
}