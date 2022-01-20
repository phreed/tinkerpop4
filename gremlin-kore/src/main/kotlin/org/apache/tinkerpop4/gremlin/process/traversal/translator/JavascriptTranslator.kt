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
package org.apache.tinkerpop4.gremlin.process.traversal.translator

import org.apache.commons.configuration2.ConfigurationConverter

/**
 * Converts bytecode to a Javascript string of Gremlin.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class JavascriptTranslator private constructor(
    @get:Override val traversalSource: String,
    typeTranslator: TypeTranslator
) : ScriptTranslator {
    private val typeTranslator: TypeTranslator

    init {
        this.typeTranslator = typeTranslator
    }

    @Override
    fun translate(bytecode: Bytecode?): Script {
        return typeTranslator.apply(traversalSource, bytecode)
    }

    @get:Override
    val targetLanguage: String
        get() = "gremlin-javascript"

    @Override
    override fun toString(): String {
        return StringFactory.translatorString(this)
    }

    /**
     * Performs standard type translation for the TinkerPop types to Javascript.
     */
    class DefaultTypeTranslator(withParameters: Boolean) : AbstractTypeTranslator(withParameters) {
        @get:Override
        protected val nullSyntax: String
            protected get() = "null"

        @Override
        protected fun getSyntax(o: String): String {
            return (if (o.contains("\"")) "\"\"\"" + StringEscapeUtils.escapeJava(o)
                .toString() + "\"\"\"" else "\"" + StringEscapeUtils.escapeJava(o).toString() + "\"")
                .replace("$", "\\$")
        }

        @Override
        protected fun getSyntax(o: Boolean): String {
            return o.toString()
        }

        @Override
        protected fun getSyntax(o: Date): String {
            return "new Date(" + o.getTime().toString() + ")"
        }

        @Override
        protected fun getSyntax(o: Timestamp): String {
            return "new Date(" + o.getTime().toString() + ")"
        }

        @Override
        protected fun getSyntax(o: UUID): String {
            return "'" + o.toString().toString() + "'"
        }

        @Override
        protected fun getSyntax(o: Lambda): String {
            return "() => \"" + StringEscapeUtils.escapeEcmaScript(o.getLambdaScript().trim()).toString() + "\""
        }

        @Override
        protected fun getSyntax(o: SackFunctions.Barrier): String {
            return "Barrier." + o.toString()
        }

        @Override
        protected fun getSyntax(o: VertexProperty.Cardinality): String {
            return "Cardinality." + o.toString()
        }

        @Override
        protected fun getSyntax(o: Pick): String {
            return "Pick." + o.toString()
        }

        @Override
        protected fun getSyntax(o: Number): String {
            return o.toString()
        }

        @Override
        protected fun produceScript(o: Set<*>?): Script {
            return produceScript(ArrayList(o))
        }

        @Override
        protected fun produceScript(o: List<*>): Script {
            val iterator = o.iterator()
            script.append("[")
            while (iterator.hasNext()) {
                val nextItem: Object = iterator.next()
                convertToScript(nextItem)
                if (iterator.hasNext()) script.append(",").append(" ")
            }
            return script.append("]")
        }

        @Override
        protected fun produceScript(o: Map<*, *>): Script {
            script.append("new Map([")
            val itty: Iterator<Map.Entry<*, *>?> = o.entrySet().iterator()
            while (itty.hasNext()) {
                val entry = itty.next()
                script.append("[")
                convertToScript(entry.getKey())
                script.append(",")
                convertToScript(entry.getValue())
                script.append("]")
                if (itty.hasNext()) script.append(",")
            }
            return script.append("])")
        }

        @Override
        protected fun produceScript(o: Class<*>): Script {
            return script.append(o.getCanonicalName())
        }

        @Override
        protected fun produceScript(o: Enum<*>): Script {
            return script.append(o.getDeclaringClass().getSimpleName() + "." + o.toString())
        }

        @Override
        protected fun produceScript(o: Vertex): Script {
            script.append("new Vertex(")
            convertToScript(o.id())
            script.append(",")
            convertToScript(o.label())
            return script.append(", null)")
        }

        @Override
        protected fun produceScript(o: Edge): Script {
            script.append("new Edge(")
            convertToScript(o.id())
            script.append(", new Vertex(")
            convertToScript(o.outVertex().id())
            script.append(",")
            convertToScript(o.outVertex().label())
            script.append(", null),")
            convertToScript(o.label())
            script.append(", new Vertex(")
            convertToScript(o.inVertex().id())
            script.append(",")
            convertToScript(o.inVertex().label())
            return script.append(",null),null)")
        }

        @Override
        protected fun produceScript(o: VertexProperty<*>): Script {
            script.append("new Property(")
            convertToScript(o.id())
            script.append(",")
            convertToScript(o.label())
            script.append(",")
            convertToScript(o.value())
            script.append(",")
            return script.append("null)")
        }

        @Override
        protected fun produceScript(o: TraversalStrategyProxy<*>): Script {
            return if (o.getConfiguration().isEmpty()) {
                script.append("new " + o.getStrategyClass().getSimpleName().toString() + "()")
            } else {
                script.append("new " + o.getStrategyClass().getSimpleName().toString() + "(")
                val conf: Map<Object, Object> = ConfigurationConverter.getMap(o.getConfiguration())
                script.append("{")
                conf.entrySet().stream().filter { entry -> !entry.getKey().equals(TraversalStrategy.STRATEGY) }
                    .forEach { entry ->
                        script.append(entry.getKey().toString())
                        script.append(":")
                        convertToScript(entry.getValue()).getScript()
                        script.append(",")
                    }
                script.setCharAtEnd('}')
                script.append(")")
            }
        }

        @Override
        protected fun produceScript(traversalSource: String?, o: Bytecode): Script {
            script.append(traversalSource)
            for (instruction in o.getInstructions()) {
                val methodName: String = instruction.getOperator()
                if (0 == instruction.getArguments().length) {
                    script.append(".").append(resolveSymbol(methodName)).append("()")
                } else {
                    script.append(".").append(resolveSymbol(methodName)).append("(")

                    // have to special case withSack() for Groovy because UnaryOperator and BinaryOperator signatures
                    // make it impossible for the interpreter to figure out which function to call. specifically we need
                    // to discern between:
                    //     withSack(A initialValue, UnaryOperator<A> splitOperator)
                    //     withSack(A initialValue, BinaryOperator<A> splitOperator)
                    // and:
                    //     withSack(Supplier<A> initialValue, UnaryOperator<A> mergeOperator)
                    //     withSack(Supplier<A> initialValue, BinaryOperator<A> mergeOperator)
                    if (methodName.equals(TraversalSource.Symbols.withSack) && instruction.getArguments().length === 2 && instruction.getArguments()
                            .get(1) is Lambda
                    ) {
                        val castFirstArgTo =
                            if (instruction.getArguments().get(0) is Lambda) Supplier::class.java.getName() else ""
                        val secondArg: Lambda = instruction.getArguments().get(1) as Lambda
                        val castSecondArgTo: String =
                            if (secondArg.getLambdaArguments() === 1) UnaryOperator::class.java.getName() else BinaryOperator::class.java.getName()
                        if (!castFirstArgTo.isEmpty()) script.append(String.format("(%s) ", castFirstArgTo))
                        convertToScript(instruction.getArguments().get(0))
                        script.append(", (").append(castSecondArgTo).append(") ")
                        convertToScript(instruction.getArguments().get(1))
                        script.append(",")
                    } else {
                        for (`object` in instruction.getArguments()) {
                            convertToScript(`object`)
                            script.append(",")
                        }
                    }
                    script.setCharAtEnd(')')
                }
            }
            return script
        }

        @Override
        protected fun produceScript(p: P<*>): Script {
            if (p is TextP) {
                script.append("TextP.").append(p.getBiPredicate().toString()).append("(")
                convertToScript(p.getValue())
            } else if (p is ConnectiveP) {
                // ConnectiveP gets some special handling because it's reduced to and(P, P, P) and we want it
                // generated the way it was written which was P.and(P).and(P)
                val list: List<P<*>> = (p as ConnectiveP).getPredicates()
                val connector = if (p is OrP) "or" else "and"
                for (i in 0 until list.size()) {
                    produceScript(list[i])

                    // for the first/last P there is no parent to close
                    if (i > 0 && i < list.size() - 1) script.append(")")

                    // add teh connector for all but last P
                    if (i < list.size() - 1) {
                        script.append(".").append(connector).append("(")
                    }
                }
            } else {
                script.append("P.").append(p.getBiPredicate().toString()).append("(")
                convertToScript(p.getValue())
            }
            script.append(")")
            return script
        }

        protected fun resolveSymbol(methodName: String?): String {
            return SymbolHelper.toJavascript(methodName)
        }
    }

    internal object SymbolHelper {
        private val TO_JS_MAP: Map<String, String> = HashMap()
        private val FROM_JS_MAP: Map<String, String> = HashMap()

        init {
            TO_JS_MAP.put("from", "from_")
            TO_JS_MAP.put("in", "in_")
            TO_JS_MAP.put("with", "with_")
            //
            TO_JS_MAP.forEach { k, v ->
                FROM_JS_MAP.put(
                    org.apache.tinkerpop4.gremlin.process.traversal.translator.v,
                    org.apache.tinkerpop4.gremlin.process.traversal.translator.k
                )
            }
        }

        fun toJavascript(symbol: String?): String {
            return TO_JS_MAP.getOrDefault(symbol, symbol)
        }

        fun toJava(symbol: String?): String {
            return FROM_JS_MAP.getOrDefault(symbol, symbol)
        }
    }

    companion object {
        /**
         * Creates the translator with a `false` argument to `withParameters` using
         * [.of].
         */
        fun of(traversalSource: String?): JavascriptTranslator {
            return of(traversalSource, false)
        }

        /**
         * Creates the translator with the [DefaultTypeTranslator] passing the `withParameters` option to it
         * which will handle type translation in a fashion that should typically increase cache hits and reduce
         * compilation times if enabled at the sacrifice to rewriting of the script that could reduce readability.
         */
        fun of(traversalSource: String, withParameters: Boolean): JavascriptTranslator {
            return of(traversalSource, DefaultTypeTranslator(withParameters))
        }

        /**
         * Creates the translator with a custom [TypeTranslator] instance.
         */
        fun of(traversalSource: String, typeTranslator: TypeTranslator): JavascriptTranslator {
            return JavascriptTranslator(traversalSource, typeTranslator)
        }
    }
}