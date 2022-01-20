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
 * Converts bytecode to a C# string of Gremlin.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class DotNetTranslator private constructor(@get:Override val traversalSource: String, typeTranslator: TypeTranslator) :
    ScriptTranslator {
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
        get() = "gremlin-dotnet"

    @Override
    override fun toString(): String {
        return StringFactory.translatorString(this)
    }

    /**
     * Performs standard type translation for the TinkerPop types to C#.
     */
    class DefaultTypeTranslator(withParameters: Boolean) : AbstractTypeTranslator(withParameters) {
        @get:Override
        protected val nullSyntax: String
            protected get() = "null"

        @Override
        protected fun getSyntax(o: String?): String {
            return "\"" + StringEscapeUtils.escapeJava(o).toString() + "\""
        }

        @Override
        protected fun getSyntax(o: Boolean): String {
            return o.toString()
        }

        @Override
        protected fun getSyntax(o: Date): String {
            return "DateTimeOffset.FromUnixTimeMilliseconds(" + o.getTime().toString() + ")"
        }

        @Override
        protected fun getSyntax(o: Timestamp): String {
            return "DateTimeOffset.FromUnixTimeMilliseconds(" + o.getTime().toString() + ")"
        }

        @Override
        protected fun getSyntax(o: UUID): String {
            return "new Guid(\"" + o.toString().toString() + "\")"
        }

        @Override
        protected fun getSyntax(o: Lambda): String {
            return "Lambda.Groovy(\"" + StringEscapeUtils.escapeEcmaScript(o.getLambdaScript().trim())
                .toString() + "\")"
        }

        @Override
        protected fun getSyntax(o: SackFunctions.Barrier): String {
            return "Barrier." + SymbolHelper.toCSharp(o.toString())
        }

        @Override
        protected fun getSyntax(o: VertexProperty.Cardinality): String {
            return "Cardinality." + SymbolHelper.toCSharp(o.toString())
        }

        @Override
        protected fun getSyntax(o: Pick): String {
            return "Pick." + SymbolHelper.toCSharp(o.toString())
        }

        @Override
        protected fun getSyntax(o: Number): String {
            return o.toString()
        }

        @Override
        protected fun produceScript(o: Set<*>): Script {
            val iterator = o.iterator()
            script.append("new HashSet<object> {")
            while (iterator.hasNext()) {
                val nextItem: Object = iterator.next()
                convertToScript(nextItem)
                if (iterator.hasNext()) script.append(", ")
            }
            return script.append("}")
        }

        @Override
        protected fun produceScript(o: List<*>): Script {
            val iterator = o.iterator()
            script.append("new List<object> {")
            while (iterator.hasNext()) {
                val nextItem: Object = iterator.next()
                convertToScript(nextItem)
                if (iterator.hasNext()) script.append(", ")
            }
            return script.append("}")
        }

        @Override
        protected fun produceScript(o: Map<*, *>): Script {
            script.append("new Dictionary<object,object> {")
            produceKeyValuesForMap(o)
            return script.append("}")
        }

        @Override
        protected fun produceScript(o: Class<*>): Script {
            return script.append(o.getCanonicalName())
        }

        @Override
        protected fun produceScript(o: Enum<*>): Script {
            val e: String = if (o is Direction) o.name().substring(0, 1).toUpperCase() + o.name().substring(1)
                .toLowerCase() else o.name().substring(0, 1).toUpperCase() + o.name().substring(1)
            return script.append(o.getDeclaringClass().getSimpleName() + "." + e)
        }

        @Override
        protected fun produceScript(o: Vertex): Script {
            script.append("new Vertex(")
            convertToScript(o.id())
            script.append(", ")
            convertToScript(o.label())
            return script.append(")")
        }

        @Override
        protected fun produceScript(o: Edge): Script {
            script.append("new Edge(")
            convertToScript(o.id())
            script.append(", new Vertex(")
            convertToScript(o.outVertex().id())
            script.append(", ")
            convertToScript(o.outVertex().label())
            script.append("), ")
            convertToScript(o.label())
            script.append(", new Vertex(")
            convertToScript(o.inVertex().id())
            script.append(", ")
            convertToScript(o.inVertex().label())
            return script.append("))")
        }

        @Override
        protected fun produceScript(o: VertexProperty<*>): Script {
            script.append("new VertexProperty(")
            convertToScript(o.id())
            script.append(", ")
            convertToScript(o.label())
            script.append(", ")
            convertToScript(o.value())
            script.append(", ")
            return script.append("null)")
        }

        @Override
        protected fun produceScript(o: TraversalStrategyProxy<*>): Script {
            return if (o.getConfiguration().isEmpty()) {
                script.append("new " + o.getStrategyClass().getSimpleName().toString() + "()")
            } else {
                script.append("new " + o.getStrategyClass().getSimpleName().toString() + "(")
                val keys: Iterator<String> = IteratorUtils.stream(o.getConfiguration().getKeys())
                    .filter { e -> !e.equals(TraversalStrategy.STRATEGY) }
                    .iterator()
                while (keys.hasNext()) {
                    val k = keys.next()
                    script.append(k)
                    script.append(": ")
                    convertToScript(o.getConfiguration().getProperty(k))
                    if (keys.hasNext()) script.append(", ")
                }
                script.append(")")
            }
        }

        private fun produceKeyValuesForMap(m: Map<*, *>): Script {
            val itty: Iterator<Map.Entry<*, *>?> = m.entrySet().iterator()
            while (itty.hasNext()) {
                val entry = itty.next()
                script.append("{")
                convertToScript(entry.getKey())
                script.append(", ")
                convertToScript(entry.getValue())
                script.append("}")
                if (itty.hasNext()) script.append(", ")
            }
            return script
        }

        @Override
        protected fun produceScript(traversalSource: String?, o: Bytecode): Script {
            script.append(traversalSource)
            var instructionPosition = 0
            for (instruction in o.getInstructions()) {
                val methodName: String = instruction.getOperator()
                // perhaps too many if/then conditions for specifying generics. doesnt' seem like there is a clear
                // way to refactor this more nicely though.
                //
                // inject() only has types when called with it when its used as a start step
                if (0 == instruction.getArguments().length) {
                    if (methodName.equals(GraphTraversal.Symbols.fold) && o.getSourceInstructions()
                            .size() + o.getStepInstructions().size() > 1 ||
                        methodName.equals(GraphTraversal.Symbols.inject) && instructionPosition > 0
                    ) script.append(".").append(resolveSymbol(methodName).replace("<object>", ""))
                        .append("()") else script.append(".").append(resolveSymbol(methodName)).append("()")
                } else {
                    if (methodsWithArgsNotNeedingGeneric.contains(methodName) ||
                        methodName.equals(GraphTraversal.Symbols.inject) && (Arrays.stream(instruction.getArguments())
                            .noneMatch(Objects::isNull) || instructionPosition > 0)
                    ) script.append(".")
                        .append(resolveSymbol(methodName).replace("<object>", "").replace("<object,object>", ""))
                        .append("(") else script.append(".").append(resolveSymbol(methodName)).append("(")

                    // have to special case withSack() because UnaryOperator and BinaryOperator signatures
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
                        val castFirstArgTo = if (instruction.getArguments().get(0) is Lambda) "ISupplier" else ""
                        val secondArg: Lambda = instruction.getArguments().get(1) as Lambda
                        val castSecondArgTo =
                            if (secondArg.getLambdaArguments() === 1) "IUnaryOperator" else "IBinaryOperator"
                        if (!castFirstArgTo.isEmpty()) script.append(String.format("(%s) ", castFirstArgTo))
                        convertToScript(instruction.getArguments().get(0))
                        script.append(", (").append(castSecondArgTo).append(") ")
                        convertToScript(instruction.getArguments().get(1))
                        script.append(",")
                    } else {
                        val instArgs: Array<Object> = instruction.getArguments()
                        for (idx in instArgs.indices) {
                            val instArg: Object = instArgs[idx]
                            // overloads might have trouble with null in calling the right one. add more as we find
                            // them i guess
                            if (null == instArg) {
                                if (methodName.equals(GraphTraversal.Symbols.addV) && idx % 2 == 0 ||
                                    methodName.equals(GraphTraversal.Symbols.hasLabel) ||
                                    methodName.equals(GraphTraversal.Symbols.hasKey)
                                ) {
                                    script.append("(string) ")
                                } else if (methodName.equals(GraphTraversal.Symbols.hasValue)) {
                                    script.append("(object) ")
                                } else if (methodName.equals(GraphTraversal.Symbols.has)) {
                                    if (instArgs.size == 2) {
                                        if ((instArgs[0] is T || instArgs[0] is String) && idx == 1) {
                                            script.append("(object) ")
                                        }
                                    } else if (instArgs.size == 3) {
                                        if ((instArgs[0] is T || instArgs[0] is String) && idx == 1) {
                                            script.append("(string) ")
                                        } else if ((instArgs[0] is T || instArgs[0] is String) && idx == 2) {
                                            script.append("(object) ")
                                        }
                                    }
                                }
                            }
                            convertToScript(instArg)
                            script.append(",")
                        }
                    }
                    script.setCharAtEnd(')')
                }
                instructionPosition++
            }
            return script
        }

        @Override
        protected fun produceScript(p: P<*>): Script {
            if (p is TextP) {
                script.append("TextP.").append(SymbolHelper.toCSharp(p.getBiPredicate().toString())).append("(")
                convertToScript(p.getValue())
            } else if (p is ConnectiveP) {
                // ConnectiveP gets some special handling because it's reduced to and(P, P, P) and we want it
                // generated the way it was written which was P.and(P).and(P)
                val list: List<P<*>> = (p as ConnectiveP).getPredicates()
                val connector = if (p is OrP) "Or" else "And"
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
                script.append("P.").append(SymbolHelper.toCSharp(p.getBiPredicate().toString())).append("(")
                convertToScript(p.getValue())
            }
            script.append(")")
            return script
        }

        protected fun resolveSymbol(methodName: String): String {
            return SymbolHelper.toCSharp(methodName)
        }
    }

    internal object SymbolHelper {
        private val TO_CS_MAP: Map<String, String> = HashMap()
        private val FROM_CS_MAP: Map<String, String> = HashMap()

        init {
            TO_CS_MAP.put(GraphTraversal.Symbols.branch, "Branch<object>")
            TO_CS_MAP.put(GraphTraversal.Symbols.cap, "Cap<object>")
            TO_CS_MAP.put(GraphTraversal.Symbols.choose, "Choose<object>")
            TO_CS_MAP.put(GraphTraversal.Symbols.coalesce, "Coalesce<object>")
            TO_CS_MAP.put(GraphTraversal.Symbols.constant, "Constant<object>")
            TO_CS_MAP.put(GraphTraversal.Symbols.elementMap, "ElementMap<object>")
            TO_CS_MAP.put(GraphTraversal.Symbols.flatMap, "FlatMap<object>")
            TO_CS_MAP.put(GraphTraversal.Symbols.fold, "Fold<object>")
            TO_CS_MAP.put(GraphTraversal.Symbols.group, "Group<object,object>")
            TO_CS_MAP.put(GraphTraversal.Symbols.groupCount, "GroupCount<object>")
            TO_CS_MAP.put(GraphTraversal.Symbols.index, "Index<object>")
            TO_CS_MAP.put(GraphTraversal.Symbols.inject, "Inject<object>")
            TO_CS_MAP.put(GraphTraversal.Symbols.io, "Io<object>")
            TO_CS_MAP.put(GraphTraversal.Symbols.limit, "Limit<object>")
            TO_CS_MAP.put(GraphTraversal.Symbols.local, "Local<object>")
            TO_CS_MAP.put(GraphTraversal.Symbols.match, "Match<object>")
            TO_CS_MAP.put(GraphTraversal.Symbols.map, "Map<object>")
            TO_CS_MAP.put(GraphTraversal.Symbols.max, "Max<object>")
            TO_CS_MAP.put(GraphTraversal.Symbols.min, "Min<object>")
            TO_CS_MAP.put(GraphTraversal.Symbols.mean, "Mean<object>")
            TO_CS_MAP.put(GraphTraversal.Symbols.optional, "Optional<object>")
            TO_CS_MAP.put(GraphTraversal.Symbols.project, "Project<object>")
            TO_CS_MAP.put(GraphTraversal.Symbols.properties, "Properties<object>")
            TO_CS_MAP.put(GraphTraversal.Symbols.range, "Range<object>")
            TO_CS_MAP.put(GraphTraversal.Symbols.sack, "Sack<object>")
            TO_CS_MAP.put(GraphTraversal.Symbols.select, "Select<object>")
            TO_CS_MAP.put(GraphTraversal.Symbols.skip, "Skip<object>")
            TO_CS_MAP.put(GraphTraversal.Symbols.sum, "Sum<object>")
            TO_CS_MAP.put(GraphTraversal.Symbols.tail, "Tail<object>")
            TO_CS_MAP.put(GraphTraversal.Symbols.unfold, "Unfold<object>")
            TO_CS_MAP.put(GraphTraversal.Symbols.union, "Union<object>")
            TO_CS_MAP.put(GraphTraversal.Symbols.value, "Value<object>")
            TO_CS_MAP.put(GraphTraversal.Symbols.valueMap, "ValueMap<object,object>")
            TO_CS_MAP.put(GraphTraversal.Symbols.values, "Values<object>")
            //
            TO_CS_MAP.forEach { k, v ->
                FROM_CS_MAP.put(
                    org.apache.tinkerpop4.gremlin.process.traversal.translator.v,
                    org.apache.tinkerpop4.gremlin.process.traversal.translator.k
                )
            }
        }

        fun toCSharp(symbol: String): String {
            return TO_CS_MAP.getOrDefault(symbol, symbol.substring(0, 1).toUpperCase() + symbol.substring(1))
        }

        fun toJava(symbol: String): String {
            return FROM_CS_MAP.getOrDefault(symbol, symbol.substring(0, 1).toLowerCase() + symbol.substring(1))
        }
    }

    companion object {
        private val methodsWithArgsNotNeedingGeneric: List<String> = Arrays.asList(
            GraphTraversal.Symbols.group,
            GraphTraversal.Symbols.groupCount, GraphTraversal.Symbols.sack
        )

        /**
         * Creates the translator with a `false` argument to `withParameters` using
         * [.of].
         */
        fun of(traversalSource: String?): DotNetTranslator {
            return of(traversalSource, false)
        }

        /**
         * Creates the translator with the [DefaultTypeTranslator] passing the `withParameters` option to it
         * which will handle type translation in a fashion that should typically increase cache hits and reduce
         * compilation times if enabled at the sacrifice to rewriting of the script that could reduce readability.
         */
        fun of(traversalSource: String, withParameters: Boolean): DotNetTranslator {
            return of(traversalSource, DefaultTypeTranslator(withParameters))
        }

        /**
         * Creates the translator with a custom [TypeTranslator] instance.
         */
        fun of(traversalSource: String, typeTranslator: TypeTranslator): DotNetTranslator {
            return DotNetTranslator(traversalSource, typeTranslator)
        }
    }
}