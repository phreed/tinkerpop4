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
 * Translates Gremlin [Bytecode] into a Python string representation.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class PythonTranslator private constructor(@get:Override val traversalSource: String, typeTranslator: TypeTranslator) :
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
        get() = "gremlin-python"

    @Override
    override fun toString(): String {
        return StringFactory.translatorString(this)
    }
    ///////
    /**
     * Performs standard type translation for the TinkerPop types to Python.
     */
    class DefaultTypeTranslator(withParameters: Boolean) : AbstractTypeTranslator(withParameters) {
        @get:Override
        protected val nullSyntax: String
            protected get() = "None"

        @Override
        protected fun getSyntax(o: String): String {
            return if (o.contains("'") || o.contains(System.lineSeparator())) "\"\"\"" + o + "\"\"\"" else "'$o'"
        }

        @Override
        protected fun getSyntax(o: Boolean): String {
            return if (o) "True" else "False"
        }

        @Override
        protected fun getSyntax(o: Date): String {
            return "datetime.datetime.utcfromtimestamp(" + o.getTime().toString() + " / 1000.0)"
        }

        @Override
        protected fun getSyntax(o: Timestamp): String {
            return "timestamp(" + o.getTime().toString() + " / 1000.0)"
        }

        @Override
        protected fun getSyntax(o: UUID): String {
            return "UUID('" + o.toString().toString() + "')"
        }

        @Override
        protected fun getSyntax(o: Lambda): String {
            val lambdaString: String = o.getLambdaScript().trim()
            return "lambda: \"" + StringEscapeUtils.escapeJava(lambdaString).toString() + "\""
        }

        @Override
        protected fun getSyntax(o: Number): String {
            // todo: nan/inf
            // all int/short/BigInteger/long are just python int/bignum
            return if (o is Double || o is Float || o is BigDecimal) "float($o)" else if (o is Byte) "SingleByte($o)" else o.toString()
        }

        @Override
        protected fun getSyntax(o: SackFunctions.Barrier): String {
            return "Barrier." + resolveSymbol(o.toString())
        }

        @Override
        protected fun getSyntax(o: VertexProperty.Cardinality): String {
            return "Cardinality." + resolveSymbol(o.toString())
        }

        @Override
        protected fun getSyntax(o: Pick): String {
            return "Pick." + resolveSymbol(o.toString())
        }

        @Override
        protected fun produceScript(o: Set<*>): Script {
            val iterator = o.iterator()
            script.append("set(")
            while (iterator.hasNext()) {
                convertToScript(iterator.next())
                if (iterator.hasNext()) script.append(",")
            }
            return script.append(")")
        }

        @Override
        protected fun produceScript(o: List<*>): Script {
            val iterator = o.iterator()
            script.append("[")
            while (iterator.hasNext()) {
                convertToScript(iterator.next())
                if (iterator.hasNext()) script.append(",")
            }
            return script.append("]")
        }

        @Override
        protected fun produceScript(o: Map<*, *>): Script {
            script.append("{")
            val itty: Iterator<Map.Entry<*, *>?> = o.entrySet().iterator()
            while (itty.hasNext()) {
                val entry = itty.next()
                convertToScript(entry.getKey()).append(":")
                convertToScript(entry.getValue())
                if (itty.hasNext()) script.append(",")
            }
            return script.append("}")
        }

        @Override
        protected fun produceScript(o: Class<*>): Script {
            return script.append("GremlinType(" + o.getCanonicalName().toString() + ")")
        }

        @Override
        protected fun produceScript(o: Enum<*>): Script {
            return script.append(o.getDeclaringClass().getSimpleName() + "." + resolveSymbol(o.toString()))
        }

        @Override
        protected fun produceScript(o: Vertex): Script {
            script.append("Vertex(")
            convertToScript(o.id()).append(",")
            return convertToScript(o.label()).append(")")
        }

        @Override
        protected fun produceScript(o: Edge): Script {
            script.append("Edge(")
            convertToScript(o.id()).append(",")
            convertToScript(o.outVertex()).append(",")
            convertToScript(o.label()).append(",")
            return convertToScript(o.inVertex()).append(")")
        }

        @Override
        protected fun produceScript(o: VertexProperty<*>): Script {
            script.append("VertexProperty(")
            convertToScript(o.id()).append(",")
            convertToScript(o.label()).append(",")
            return convertToScript(o.value()).append(")")
        }

        @Override
        protected fun produceScript(o: TraversalStrategyProxy<*>): Script {
            return if (o.getConfiguration().isEmpty()) script.append(
                "TraversalStrategy('" + o.getStrategyClass().getSimpleName()
                    .toString() + "', None, '" + o.getStrategyClass()
                    .getName().toString() + "')"
            ) else {
                script.append("TraversalStrategy('").append(o.getStrategyClass().getSimpleName()).append("',")
                convertToScript(ConfigurationConverter.getMap(o.getConfiguration()))
                script.append(", '")
                script.append(o.getStrategyClass().getName())
                script.append("')")
            }
        }

        @Override
        protected fun produceScript(traversalSource: String?, o: Bytecode): Script {
            script.append(traversalSource)
            for (instruction in o.getInstructions()) {
                val methodName: String = instruction.getOperator()
                val arguments: Array<Object> = instruction.getArguments()
                if (0 == arguments.size) script.append(".").append(resolveSymbol(methodName))
                    .append("()") else if (methodName.equals("range") && 2 == arguments.size) if ((arguments[0] as Number).longValue() + 1 === (arguments[1] as Number).longValue()) script.append(
                    "["
                ).append(
                    arguments[0].toString()
                ).append("]") else script.append("[").append(arguments[0].toString()).append(":").append(
                    arguments[1].toString()
                ).append("]") else if (methodName.equals("limit") && 1 == arguments.size) script.append("[0:").append(
                    arguments[0].toString()
                ).append("]") else if (methodName.equals("values") && 1 == arguments.size && script.getScript()
                        .length() > 3 && !STEP_NAMES.contains(
                        arguments[0].toString()
                    )
                ) script.append(".").append(arguments[0].toString()) else {
                    script.append(".").append(resolveSymbol(methodName)).append("(")

                    // python has trouble with java varargs...wrapping in collection seems to solve the problem
                    val varargsBeware = (instruction.getOperator().equals(TraversalSource.Symbols.withStrategies)
                            || instruction.getOperator().equals(TraversalSource.Symbols.withoutStrategies))
                    if (varargsBeware) script.append("*[")
                    val itty: Iterator<*> = Stream.of(arguments).iterator()
                    while (itty.hasNext()) {
                        convertToScript(itty.next())
                        if (itty.hasNext()) script.append(",")
                    }
                    if (varargsBeware) script.append("]")
                    script.append(")")
                }
            }
            return script
        }

        @Override
        protected fun produceScript(p: P<*>): Script {
            if (p is TextP) {
                script.append("TextP.").append(resolveSymbol(p.getBiPredicate().toString())).append("(")
                convertToScript(p.getValue())
            } else if (p is ConnectiveP) {
                // ConnectiveP gets some special handling because it's reduced to and(P, P, P) and we want it
                // generated the way it was written which was P.and(P).and(P)
                val list: List<P<*>> = (p as ConnectiveP).getPredicates()
                val connector = if (p is OrP) "or_" else "and_"
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
                script.append("P.").append(resolveSymbol(p.getBiPredicate().toString())).append("(")
                convertToScript(p.getValue())
            }
            script.append(")")
            return script
        }

        protected fun resolveSymbol(methodName: String?): String {
            return SymbolHelper.toPython(methodName)
        }
    }

    /**
     * Performs translation without for the syntax sugar to Python.
     */
    class NoSugarTranslator(withParameters: Boolean) : DefaultTypeTranslator(withParameters) {
        @Override
        override fun produceScript(traversalSource: String?, o: Bytecode): Script {
            script.append(traversalSource)
            for (instruction in o.getInstructions()) {
                val methodName: String = instruction.getOperator()
                val arguments: Array<Object> = instruction.getArguments()
                if (0 == arguments.size) script.append(".").append(resolveSymbol(methodName)).append("()") else {
                    script.append(".").append(resolveSymbol(methodName)).append("(")

                    // python has trouble with java varargs...wrapping in collection seems to solve
                    // the problem
                    val varargsBeware = (instruction.getOperator().equals(TraversalSource.Symbols.withStrategies)
                            || instruction.getOperator().equals(TraversalSource.Symbols.withoutStrategies))
                    if (varargsBeware) script.append("*[")
                    val itty: Iterator<*> = Stream.of(arguments).iterator()
                    while (itty.hasNext()) {
                        convertToScript(itty.next())
                        if (itty.hasNext()) script.append(",")
                    }
                    if (varargsBeware) script.append("]")
                    script.append(")")
                }
            }
            return script
        }
    }

    internal object SymbolHelper {
        private val TO_PYTHON_MAP: Map<String, String> = HashMap()
        private val FROM_PYTHON_MAP: Map<String, String> = HashMap()

        init {
            TO_PYTHON_MAP.put("global", "global_")
            TO_PYTHON_MAP.put("all", "all_")
            TO_PYTHON_MAP.put("and", "and_")
            TO_PYTHON_MAP.put("as", "as_")
            TO_PYTHON_MAP.put("filter", "filter_")
            TO_PYTHON_MAP.put("from", "from_")
            TO_PYTHON_MAP.put("id", "id_")
            TO_PYTHON_MAP.put("in", "in_")
            TO_PYTHON_MAP.put("is", "is_")
            TO_PYTHON_MAP.put("list", "list_")
            TO_PYTHON_MAP.put("max", "max_")
            TO_PYTHON_MAP.put("min", "min_")
            TO_PYTHON_MAP.put("or", "or_")
            TO_PYTHON_MAP.put("not", "not_")
            TO_PYTHON_MAP.put("range", "range_")
            TO_PYTHON_MAP.put("set", "set_")
            TO_PYTHON_MAP.put("sum", "sum_")
            TO_PYTHON_MAP.put("with", "with_")
            TO_PYTHON_MAP.put("range", "range_")
            TO_PYTHON_MAP.put("filter", "filter_")
            TO_PYTHON_MAP.put("id", "id_")
            TO_PYTHON_MAP.put("max", "max_")
            TO_PYTHON_MAP.put("min", "min_")
            TO_PYTHON_MAP.put("sum", "sum_")
            //
            TO_PYTHON_MAP.forEach { k, v ->
                FROM_PYTHON_MAP.put(
                    org.apache.tinkerpop4.gremlin.process.traversal.translator.v,
                    org.apache.tinkerpop4.gremlin.process.traversal.translator.k
                )
            }
        }

        fun toPython(symbol: String?): String {
            return TO_PYTHON_MAP.getOrDefault(symbol, symbol)
        }

        fun toJava(symbol: String?): String {
            return FROM_PYTHON_MAP.getOrDefault(symbol, symbol)
        }
    }

    companion object {
        private val STEP_NAMES: Set<String> = Stream.of(GraphTraversal::class.java.getMethods())
            .filter { method -> Traversal::class.java.isAssignableFrom(method.getReturnType()) }
            .map(Method::getName).collect(Collectors.toSet())

        /**
         * Creates the translator with a `false` argument to `withParameters` using
         * [.of].
         */
        fun of(traversalSource: String?): PythonTranslator {
            return of(traversalSource, false)
        }

        /**
         * Creates the translator with the [DefaultTypeTranslator] passing the `withParameters` option to it
         * which will handle type translation in a fashion that should typically increase cache hits and reduce
         * compilation times if enabled at the sacrifice to rewriting of the script that could reduce readability.
         */
        fun of(traversalSource: String, withParameters: Boolean): PythonTranslator {
            return of(traversalSource, DefaultTypeTranslator(withParameters))
        }

        /**
         * Creates the translator with a custom [TypeTranslator] instance.
         */
        fun of(traversalSource: String, typeTranslator: TypeTranslator): PythonTranslator {
            return PythonTranslator(traversalSource, typeTranslator)
        }
    }
}