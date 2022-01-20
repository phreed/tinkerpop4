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
package org.apache.tinkerpop4.gremlin.process.traversal

import org.apache.tinkerpop4.gremlin.process.traversal.strategy.TraversalStrategyProxy
import org.apache.tinkerpop4.gremlin.structure.util.StringFactory
import org.apache.tinkerpop4.gremlin.util.iterator.IteratorUtils
import java.io.Serializable
import java.util.ArrayList
import java.util.Arrays
import java.util.Collection
import java.util.HashMap
import java.util.LinkedHashMap
import java.util.LinkedHashSet
import java.util.List
import java.util.Map
import java.util.Objects
import java.util.Set

/**
 * When a [TraversalSource] is manipulated and then a [Traversal] is spawned and mutated, a language
 * agnostic representation of those mutations is recorded in a bytecode instance. Bytecode is simply a list
 * of ordered instructions where an instruction is a string operator and a (flattened) array of arguments.
 * Bytecode is used by [Translator] instances which are able to translate a traversal in one language to another
 * by analyzing the bytecode as opposed to the Java traversal object representation on heap.
 *
 *
 * Bytecode can be serialized between environments and machines by way of a GraphSON representation.
 * Thus, Gremlin-Python can create bytecode in Python and ship it to Gremlin-Java for evaluation in Java.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class Bytecode : Cloneable, Serializable {
    /**
     * Get the [TraversalSource] instructions associated with this bytecode.
     *
     * @return an iterable of instructions
     */
    var sourceInstructions: List<Instruction> = ArrayList()
        private set

    /**
     * Get the [Traversal] instructions associated with this bytecode.
     *
     * @return an iterable of instructions
     */
    var stepInstructions: List<Instruction> = ArrayList()
        private set

    constructor() {}
    internal constructor(sourceName: String, vararg arguments: Object?) {
        sourceInstructions.add(Instruction(sourceName, *flattenArguments(*arguments)))
    }

    /**
     * Add a [TraversalSource] instruction to the bytecode.
     *
     * @param sourceName the traversal source method name (e.g. withSack())
     * @param arguments  the traversal source method arguments
     */
    fun addSource(sourceName: String, vararg arguments: Object) {
        if (sourceName.equals(TraversalSource.Symbols.withoutStrategies)) {
            if (arguments == null) sourceInstructions.add(Instruction(sourceName, null)) else {
                val classes: Array<Class<TraversalStrategy>?> = arrayOfNulls<Class>(arguments.size)
                for (i in arguments.indices) {
                    classes[i] =
                        if (arguments[i] is TraversalStrategyProxy) (arguments[i] as TraversalStrategyProxy).getStrategyClass() else arguments[i] as Class
                }
                sourceInstructions.add(Instruction(sourceName, classes))
            }
        } else sourceInstructions.add(Instruction(sourceName, *flattenArguments(*arguments)))
        Bindings.clear()
    }

    /**
     * Add a [Traversal] instruction to the bytecode.
     *
     * @param stepName  the traversal method name (e.g. out())
     * @param arguments the traversal method arguments
     */
    fun addStep(stepName: String, vararg arguments: Object?) {
        stepInstructions.add(Instruction(stepName, *flattenArguments(*arguments)))
    }

    /**
     * Get both the [TraversalSource] and [Traversal] instructions of this bytecode.
     * The traversal source instructions are provided prior to the traversal instructions.
     *
     * @return an interable of all the instructions in this bytecode
     */
    val instructions: Iterable<Instruction>
        get() = Iterable<Instruction> {
            IteratorUtils.concat(
                sourceInstructions.iterator(), stepInstructions.iterator()
            )
        }

    /**
     * Get all the bindings (in a nested, recursive manner) from all the arguments of all the instructions of this bytecode.
     *
     * @return a map of string variable and object value bindings
     */
    val bindings: Map<String, Any>
        get() {
            val bindingsMap: Map<String, Object> = HashMap()
            for (instruction in sourceInstructions) {
                for (argument in instruction.getArguments()) {
                    addArgumentBinding(bindingsMap, argument)
                }
            }
            for (instruction in stepInstructions) {
                for (argument in instruction.getArguments()) {
                    addArgumentBinding(bindingsMap, argument)
                }
            }
            return bindingsMap
        }
    val isEmpty: Boolean
        get() = sourceInstructions.isEmpty() && stepInstructions.isEmpty()

    @Override
    override fun toString(): String {
        return Arrays.asList(sourceInstructions, stepInstructions).toString()
    }

    @Override
    override fun equals(o: Object?): Boolean {
        if (this == o) return true
        if (o == null || getClass() !== o.getClass()) return false
        val bytecode = o as Bytecode
        return Objects.equals(sourceInstructions, bytecode.sourceInstructions) &&
                Objects.equals(stepInstructions, bytecode.stepInstructions)
    }

    @Override
    override fun hashCode(): Int {
        return Objects.hash(sourceInstructions, stepInstructions)
    }

    @SuppressWarnings("CloneDoesntDeclareCloneNotSupportedException")
    @Override
    fun clone(): Bytecode {
        return try {
            val clone = super.clone() as Bytecode
            clone.sourceInstructions = ArrayList(sourceInstructions)
            clone.stepInstructions = ArrayList(stepInstructions)
            clone
        } catch (e: CloneNotSupportedException) {
            throw IllegalStateException(e.getMessage(), e)
        }
    }

    class Instruction(val operator: String, vararg arguments: Object) : Serializable {
        private val arguments: Array<Object>

        init {
            this.arguments = arguments
        }

        fun getArguments(): Array<Object> {
            return arguments
        }

        @Override
        override fun toString(): String {
            return operator + "(" + StringFactory.removeEndBrackets(Arrays.asList(arguments)) + ")"
        }

        @Override
        override fun equals(o: Object?): Boolean {
            if (this === o) return true
            if (o == null || getClass() !== o.getClass()) return false
            val that = o as Instruction
            return Objects.equals(operator, that.operator) &&
                    Arrays.equals(arguments, that.arguments)
        }

        @Override
        override fun hashCode(): Int {
            var result: Int = Objects.hash(operator)
            result = 31 * result + Arrays.hashCode(arguments)
            return result
        }
    }

    class Binding<V>(val key: String, val value: V) : Serializable {
        fun variable(): String {
            return key
        }

        fun value(): V {
            return value
        }

        @Override
        override fun toString(): String {
            return "binding[" + key + "=" + value + "]"
        }

        @Override
        override fun equals(o: Object?): Boolean {
            if (this === o) return true
            if (o == null || getClass() !== o.getClass()) return false
            val binding = o as Binding<*>
            return Objects.equals(key, binding.key) &&
                    Objects.equals(value, binding.value)
        }

        @Override
        override fun hashCode(): Int {
            return Objects.hash(key, value)
        }
    }

    /////
    private fun flattenArguments(vararg arguments: Object): Array<Object> {
        if (arguments == null || arguments.size == 0) return EMPTY_ARRAY
        val flatArguments: List<Object> = ArrayList(arguments.size)
        for (`object` in arguments) {
            if (`object` is Array<Object>) {
                for (nestObject in `object`) {
                    flatArguments.add(convertArgument(nestObject, true))
                }
            } else flatArguments.add(convertArgument(`object`, true))
        }
        return flatArguments.toArray()
    }

    private fun convertArgument(argument: Object, searchBindings: Boolean): Object {
        if (searchBindings) {
            val variable: String = Bindings.getBoundVariable(argument)
            if (null != variable) return Binding<Any>(variable, convertArgument(argument, false))
        }
        //
        return if (argument is Traversal) {
            // prevent use of "g" to spawn child traversals
            if ((argument as Traversal).asAdmin().getTraversalSource().isPresent()) throw IllegalStateException(
                String.format(
                    "The child traversal of %s was not spawned anonymously - use the __ class rather than a TraversalSource to construct the child traversal",
                    argument
                )
            )
            (argument as Traversal).asAdmin().getBytecode()
        } else if (argument is Map) {
            val map: Map<Object, Object> = LinkedHashMap((argument as Map).size())
            for (entry in (argument as Map<*, *>).entrySet()) {
                map.put(convertArgument(entry.getKey(), true), convertArgument(entry.getValue(), true))
            }
            map
        } else if (argument is List) {
            val list: List<Object> = ArrayList((argument as List).size())
            for (item in argument) {
                list.add(convertArgument(item, true))
            }
            list
        } else if (argument is Set) {
            val set: Set<Object> = LinkedHashSet((argument as Set).size())
            for (item in argument) {
                set.add(convertArgument(item, true))
            }
            set
        } else argument
    }

    companion object {
        private val EMPTY_ARRAY: Array<Object> = arrayOf<Object>()
        private fun addArgumentBinding(bindingsMap: Map<String, Object>, argument: Object) {
            if (argument is Binding<*>) bindingsMap.put(
                (argument as Binding<*>).key,
                (argument as Binding<*>).value
            ) else if (argument is Map) {
                for (entry in (argument as Map<*, *>).entrySet()) {
                    addArgumentBinding(bindingsMap, entry.getKey())
                    addArgumentBinding(bindingsMap, entry.getValue())
                }
            } else if (argument is Collection) {
                for (item in argument) {
                    addArgumentBinding(bindingsMap, item)
                }
            } else if (argument is Bytecode) bindingsMap.putAll((argument as Bytecode).bindings)
        }
    }
}