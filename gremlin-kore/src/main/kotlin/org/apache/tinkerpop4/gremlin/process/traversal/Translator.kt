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

import org.apache.tinkerpop4.gremlin.process.traversal.step.TraversalOptionParent

/**
 * A Translator will translate [Bytecode] into another representation. That representation may be a
 * Java instance via [StepTranslator] or a String script in some language via [ScriptTranslator].
 * The parameterization of Translator is S (traversal source) and T (full translation).
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stark Arya (sandszhou.zj@alibaba-inc.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
interface Translator<S, T> {
    /**
     * Get the [TraversalSource] representation rooting this translator.
     * For string-based translators ([ScriptTranslator]), this is typically a "g".
     * For java-based translators ([StepTranslator]), this is typically the [TraversalSource] instance
     * which the [Traversal] will be built from.
     *
     * @return the traversal source representation
     */
    val traversalSource: S

    /**
     * Translate [Bytecode] into a new representation. Typically, for language translations, the translation is
     * to a string representing the traversal in the respective scripting language.
     *
     * @param bytecode the bytecode representing traversal source and traversal manipulations.
     * @return the translated object
     */
    fun translate(bytecode: Bytecode?): T

    /**
     * Translates a [Traversal] into the specified form
     */
    fun translate(t: Traversal<*, *>): T {
        return translate(t.asAdmin().getBytecode())
    }

    /**
     * Get the language that the translator is converting the traversal byte code to.
     *
     * @return the language of the translation
     */
    val targetLanguage: String?
    ///
    /**
     * Translates bytecode to a Script representation.
     */
    interface ScriptTranslator : Translator<String?, Script?> {
        /**
         * Provides a way for the [ScriptTranslator] to convert various data types to their string
         * representations in their target language.
         */
        interface TypeTranslator : BiFunction<String?, Object?, Script?>
        abstract class AbstractTypeTranslator protected constructor(protected val withParameters: Boolean) :
            TypeTranslator {
            protected val script: Script

            init {
                script = Script()
            }

            @Override
            fun apply(traversalSource: String?, o: Object?): Script {
                script.init()
                return if (o is Bytecode) {
                    produceScript(traversalSource, o as Bytecode?)
                } else {
                    convertToScript(o)
                }
            }

            /**
             * Gets the syntax for a `null` value as a string representation.
             */
            protected abstract val nullSyntax: String?

            /**
             * Take the string argument and convert it to a string representation in the target language (i.e. escape,
             * enclose in appropriate quotes, etc.)
             */
            protected abstract fun getSyntax(o: String?): String?

            /**
             * Take the boolean argument and convert it to a string representation in the target language.
             */
            protected abstract fun getSyntax(o: Boolean?): String?

            /**
             * Take the `Date` argument and convert it to a string representation in the target language.
             */
            protected abstract fun getSyntax(o: Date?): String?

            /**
             * Take the `Timestamp` argument and convert it to a string representation in the target language.
             */
            protected abstract fun getSyntax(o: Timestamp?): String?

            /**
             * Take the `UUID` argument and convert it to a string representation in the target language.
             */
            protected abstract fun getSyntax(o: UUID?): String?

            /**
             * Take the [Lambda] argument and convert it to a string representation in the target language.
             */
            protected abstract fun getSyntax(o: Lambda?): String?

            /**
             * Take the [SackFunctions.Barrier] argument and convert it to a string representation in the target language.
             */
            protected abstract fun getSyntax(o: SackFunctions.Barrier?): String?

            /**
             * Take the [VertexProperty.Cardinality] argument and convert it to a string representation in the target language.
             */
            protected abstract fun getSyntax(o: VertexProperty.Cardinality?): String?

            /**
             * Take the [TraversalOptionParent.Pick] argument and convert it to a string representation in the target language.
             */
            protected abstract fun getSyntax(o: Pick?): String?

            /**
             * Take the numeric argument and convert it to a string representation in the target language. Languages
             * that can discern differences in types of numbers will wish to further check the type of the
             * `Number` instance itself (i.e. `Double`, `Integer`, etc.)
             */
            protected abstract fun getSyntax(o: Number?): String?

            /**
             * Take the `Set` and writes the syntax directly to the member [.script] variable.
             */
            protected abstract fun produceScript(o: Set<*>?): Script?

            /**
             * Take the `List` and writes the syntax directly to the member [.script] variable.
             */
            protected abstract fun produceScript(o: List<*>?): Script?

            /**
             * Take the `Map` and writes the syntax directly to the member [.script] variable.
             */
            protected abstract fun produceScript(o: Map<*, *>?): Script?

            /**
             * Take the `Class` and writes the syntax directly to the member [.script] variable.
             */
            protected abstract fun produceScript(o: Class<*>?): Script?

            /**
             * Take the `Enum` and writes the syntax directly to the member [.script] variable.
             */
            protected abstract fun produceScript(o: Enum<*>?): Script?

            /**
             * Take the [Vertex] and writes the syntax directly to the member [.script] variable.
             */
            protected abstract fun produceScript(o: Vertex?): Script?

            /**
             * Take the [Edge] and writes the syntax directly to the member [.script] variable.
             */
            protected abstract fun produceScript(o: Edge?): Script?

            /**
             * Take the [VertexProperty] and writes the syntax directly to the member [.script] variable.
             */
            protected abstract fun produceScript(o: VertexProperty<*>?): Script?

            /**
             * Take the [TraversalStrategyProxy] and writes the syntax directly to the member [.script] variable.
             */
            protected abstract fun produceScript(o: TraversalStrategyProxy<*>?): Script?

            /**
             * Take the [Bytecode] and writes the syntax directly to the member [.script] variable.
             */
            protected abstract fun produceScript(traversalSource: String?, o: Bytecode?): Script

            /**
             * Take the [P] and writes the syntax directly to the member [.script] variable. This
             * implementation should also consider [TextP].
             */
            protected abstract fun produceScript(p: P<*>?): Script?

            /**
             * For each operator argument, if withParameters set true, try parametrization as follows:
             *
             * -----------------------------------------------
             * if unpack, why ?      ObjectType
             * -----------------------------------------------
             * (Yes)                 Bytecode.Binding
             * (Recursion, No)       Bytecode
             * (Recursion, No)       Traversal
             * (Yes)                 String
             * (Recursion, No)       Set
             * (Recursion, No)       List
             * (Recursion, No)       Map
             * (Yes)                 Long
             * (Yes)                 Double
             * (Yes)                 Float
             * (Yes)                 Integer
             * (Yes)                 Timestamp
             * (Yes)                 Date
             * (Yes)                 Uuid
             * (Recursion, No)       P
             * (Enumeration, No)     SackFunctions.Barrier
             * (Enumeration, No)     VertexProperty.Cardinality
             * (Enumeration, No)     TraversalOptionParent.Pick
             * (Enumeration, No)     Enum
             * (Recursion, No)       Vertex
             * (Recursion, No)       Edge
             * (Recursion, No)       VertexProperty
             * (Yes)                 Lambda
             * (Recursion, No)       TraversalStrategyProxy
             * (Enumeration, No)     TraversalStrategy
             * (Yes)                 Other
             * -------------------------------------------------
             *
             * @param object
             * @return String Repres
             */
            protected fun convertToScript(`object`: Object?): Script {
                return if (`object` is Bytecode.Binding) {
                    script.getBoundKeyOrAssign(withParameters, (`object` as Bytecode.Binding?).variable())
                } else if (`object` is Bytecode) {
                    produceScript(anonymousTraversalPrefix, `object` as Bytecode?)
                } else if (`object` is Traversal) {
                    convertToScript((`object` as Traversal?).asAdmin().getBytecode())
                } else if (`object` is String) {
                    val objectOrWrapper: Object = if (withParameters) `object` else getSyntax(`object` as String?)
                    script.getBoundKeyOrAssign(withParameters, objectOrWrapper)
                } else if (`object` is Boolean) {
                    val objectOrWrapper: Object = if (withParameters) `object` else getSyntax(`object` as Boolean?)
                    script.getBoundKeyOrAssign(withParameters, objectOrWrapper)
                } else if (`object` is Set) {
                    produceScript(`object` as Set<*>?)
                } else if (`object` is List) {
                    produceScript(`object` as List<*>?)
                } else if (`object` is Map) {
                    produceScript(`object` as Map<*, *>?)
                } else if (`object` is Number) {
                    val objectOrWrapper: Object = if (withParameters) `object` else getSyntax(`object` as Number?)
                    script.getBoundKeyOrAssign(withParameters, objectOrWrapper)
                } else if (`object` is Class) {
                    produceScript(`object` as Class<*>?)
                } else if (`object` is Timestamp) {
                    val objectOrWrapper: Object = if (withParameters) `object` else getSyntax(`object` as Timestamp?)
                    script.getBoundKeyOrAssign(withParameters, objectOrWrapper)
                } else if (`object` is Date) {
                    val objectOrWrapper: Object = if (withParameters) `object` else getSyntax(`object` as Date?)
                    script.getBoundKeyOrAssign(withParameters, objectOrWrapper)
                } else if (`object` is UUID) {
                    val objectOrWrapper: Object = if (withParameters) `object` else getSyntax(`object` as UUID?)
                    script.getBoundKeyOrAssign(withParameters, objectOrWrapper)
                } else if (`object` is P) {
                    produceScript(`object` as P<*>?)
                } else if (`object` is SackFunctions.Barrier) {
                    script.append(getSyntax(`object` as SackFunctions.Barrier?))
                } else if (`object` is VertexProperty.Cardinality) {
                    script.append(getSyntax(`object` as VertexProperty.Cardinality?))
                } else if (`object` is Pick) {
                    script.append(getSyntax(`object` as Pick?))
                } else if (`object` is Enum) {
                    produceScript(`object` as Enum<*>?)
                } else if (`object` is Vertex) {
                    produceScript(`object` as Vertex?)
                } else if (`object` is Edge) {
                    produceScript(`object` as Edge?)
                } else if (`object` is VertexProperty) {
                    produceScript(`object` as VertexProperty<*>?)
                } else if (`object` is Lambda) {
                    val objectOrWrapper: Object = if (withParameters) `object` else getSyntax(`object` as Lambda?)
                    script.getBoundKeyOrAssign(withParameters, objectOrWrapper)
                } else if (`object` is TraversalStrategyProxy) {
                    produceScript(`object` as TraversalStrategyProxy<*>?)
                } else if (`object` is TraversalStrategy) {
                    convertToScript(TraversalStrategyProxy(`object` as TraversalStrategy?))
                } else {
                    if (null == `object`) script.append(nullSyntax) else script.getBoundKeyOrAssign(
                        withParameters,
                        `object`
                    )
                }
            }

            companion object {
                /**
                 * Gets the syntax for the spawn of an anonymous traversal which is traditionally the double underscore.
                 */
                protected val anonymousTraversalPrefix = "__"
                    protected get() = Companion.field
            }
        }
    }

    /**
     * Translates bytecode to actual steps.
     */
    interface StepTranslator<S : TraversalSource?, T : Traversal.Admin<*, *>?> : Translator<S, T>
}