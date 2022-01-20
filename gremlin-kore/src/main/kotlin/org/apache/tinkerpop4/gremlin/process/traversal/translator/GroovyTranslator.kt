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
 * Converts bytecode to a Groovy string of Gremlin.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 * @author Stark Arya (sandszhou.zj@alibaba-inc.com)
 */
class GroovyTranslator private constructor(@get:Override val traversalSource: String, typeTranslator: TypeTranslator) :
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
        get() = "gremlin-groovy"

    @Override
    override fun toString(): String {
        return StringFactory.translatorString(this)
    }

    /**
     * Performs standard type translation for the TinkerPop types to Groovy.
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
            return "new Date(" + o.getTime().toString() + "L)"
        }

        @Override
        protected fun getSyntax(o: Timestamp): String {
            return "new Timestamp(" + o.getTime().toString() + "L)"
        }

        @Override
        protected fun getSyntax(o: UUID): String {
            return "UUID.fromString('" + o.toString().toString() + "')"
        }

        @Override
        protected fun getSyntax(o: Lambda): String {
            val lambdaString: String = o.getLambdaScript().trim()
            return if (lambdaString.startsWith("{")) lambdaString else "{$lambdaString}"
        }

        @Override
        protected fun getSyntax(o: SackFunctions.Barrier): String {
            return "SackFunctions.Barrier." + o.toString()
        }

        @Override
        protected fun getSyntax(o: VertexProperty.Cardinality): String {
            return "VertexProperty.Cardinality." + o.toString()
        }

        @Override
        protected fun getSyntax(o: Pick): String {
            return "TraversalOptionParent.Pick." + o.toString()
        }

        @Override
        protected fun getSyntax(o: Number): String {
            if (o is Long) return o.toString() + "L" else if (o is Double) return o.toString() + "d" else if (o is Float) return o.toString() + "f" else if (o is Integer) return "(int) $o" else if (o is Byte) return "(byte) $o"
            return if (o is Short) "(short) $o" else if (o is BigInteger) "new BigInteger('$o')" else if (o is BigDecimal) "new BigDecimal('$o')" else o.toString()
        }

        @Override
        protected fun produceScript(o: Set<*>?): Script {
            return produceScript(ArrayList(o)).append(" as Set")
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
            script.append("[")
            val itty: Iterator<Map.Entry<*, *>?> = o.entrySet().iterator()
            while (itty.hasNext()) {
                val entry = itty.next()
                script.append("(")
                convertToScript(entry.getKey())
                script.append("):(")
                convertToScript(entry.getValue())
                script.append(")")
                if (itty.hasNext()) script.append(",")
            }
            return script.append("]")
        }

        /**
         * Gets the string representation of a class with the default implementation simply checking to see if the
         * `Class` is in [CoreImports] or not. If it is present that means it can be referenced using the
         * simple name otherwise it uses the canonical name.
         *
         *
         * Those building custom [ScriptTranslator] instances might override this if they have other classes
         * that are not in [CoreImports] by default.
         */
        @Override
        protected fun produceScript(o: Class<*>): Script {
            return script.append(
                if (CoreImports.getClassImports().contains(o)) o.getSimpleName() else o.getCanonicalName()
            )
        }

        @Override
        protected fun produceScript(o: Enum<*>): Script {
            return script.append(o.getDeclaringClass().getSimpleName() + "." + o.toString())
        }

        @Override
        protected fun produceScript(o: Vertex): Script {
            script.append("new ReferenceVertex(")
            convertToScript(o.id())
            script.append(",")
            convertToScript(o.label())
            return script.append(")")
        }

        @Override
        protected fun produceScript(o: Edge): Script {
            script.append("new ReferenceEdge(")
            convertToScript(o.id())
            script.append(",")
            convertToScript(o.label())
            script.append(",new ReferenceVertex(")
            convertToScript(o.inVertex().id())
            script.append(",")
            convertToScript(o.inVertex().label())
            script.append("),new ReferenceVertex(")
            convertToScript(o.outVertex().id())
            script.append(",")
            convertToScript(o.outVertex().label())
            return script.append("))")
        }

        @Override
        protected fun produceScript(o: VertexProperty<*>): Script {
            script.append("new ReferenceVertexProperty(")
            convertToScript(o.id())
            script.append(",")
            convertToScript(o.label())
            script.append(",")
            convertToScript(o.value())
            return script.append(")")
        }

        @Override
        protected fun produceScript(o: TraversalStrategyProxy<*>): Script {
            return if (o.getConfiguration().isEmpty()) {
                produceScript(o.getStrategyClass())
            } else {
                script.append("new ")
                produceScript(o.getStrategyClass())
                script.append("(")
                val itty: Iterator<Map.Entry<Object, Object>> = ConfigurationConverter.getMap(
                    o.getConfiguration()
                ).entrySet().iterator()
                while (itty.hasNext()) {
                    val entry: Map.Entry<Object, Object> = itty.next()
                    script.append(entry.getKey().toString())
                    script.append(": ")
                    convertToScript(entry.getValue())
                    if (itty.hasNext()) script.append(", ")
                }
                script.append(")")
            }
        }

        @Override
        protected fun produceScript(traversalSource: String?, bytecode: Bytecode): Script {
            script.append(traversalSource)
            for (instruction in bytecode.getInstructions()) {
                val methodName: String = instruction.getOperator()
                if (0 == instruction.getArguments().length) {
                    script.append(".").append(methodName).append("()")
                } else {
                    script.append(".").append(methodName).append("(")

                    // special case inject(null, null) or else groovy might guess the JDK collection extension form
                    if (methodName.equals(GraphTraversal.Symbols.inject)) {
                        val itty: Iterator<Object> = Arrays.stream(instruction.getArguments()).iterator()
                        while (itty.hasNext()) {
                            val o: Object = itty.next()
                            if (null == o) script.append("(Object)")
                            convertToScript(o)
                            if (itty.hasNext()) script.append(",")
                        }
                    } else {
                        val itty: Iterator<Object> = Arrays.stream(instruction.getArguments()).iterator()
                        while (itty.hasNext()) {
                            convertToScript(itty.next())
                            if (itty.hasNext()) script.append(",")
                        }
                    }
                    script.append(")")
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

        private fun convertMapToArguments(map: Map<Object, Object>): String {
            return map.entrySet().stream()
                .map { entry -> String.format("%s: %s", entry.getKey().toString(), convertToScript(entry.getValue())) }
                .collect(Collectors.joining(", "))
        }
    }

    /**
     * An extension of the [DefaultTypeTranslator] that generates Gremlin that is compliant with
     * `gremlin-language` scripts. Specifically, it will convert `Date` and `Timestamp` to use the
     * `datetime()` function. Time zone offsets are resolved to where `2018-03-22T00:35:44.741+1600`
     * would be converted to `datetime('2018-03-21T08:35:44.741Z')`. More commonly `2018-03-22` would simply
     * generate `datetime('2018-03-22T00:00:00Z')`.
     *
     *
     * In addition, it prefers use of `Vertex` when producing a [ReferenceVertex].
     */
    class LanguageTypeTranslator(withParameters: Boolean) : DefaultTypeTranslator(withParameters) {
        @Override
        override fun getSyntax(o: Date): String {
            return getDatetimeSyntax(o.toInstant())
        }

        @Override
        override fun getSyntax(o: Timestamp): String {
            return getDatetimeSyntax(o.toInstant())
        }

        @Override
        override fun produceScript(o: Vertex): Script {
            script.append("new Vertex(")
            convertToScript(o.id())
            script.append(",")
            convertToScript(o.label())
            return script.append(")")
        }

        @Override
        override fun getSyntax(o: Number): String {
            if (o is Long) return o.toString() + "L" else if (o is Double) return o.toString() + "D" else if (o is Float) return o.toString() + "F" else if (o is Integer) return o.toString() + "I" else if (o is Byte) return o.toString() + "B"
            return if (o is Short) o.toString() + "S" else if (o is BigInteger) o.toString() + "N" else if (o is BigDecimal) o.toString() + "D" else o.toString()
        }

        companion object {
            private fun getDatetimeSyntax(i: Instant): String {
                return String.format("datetime('%s')", DatetimeHelper.format(i))
            }
        }
    }

    companion object {
        /**
         * Creates the translator with a `false` argument to `withParameters` using
         * [.of].
         */
        fun of(traversalSource: String?): GroovyTranslator {
            return of(traversalSource, false)
        }

        /**
         * Creates the translator with the [DefaultTypeTranslator] passing the `withParameters` option to it
         * which will handle type translation in a fashion that should typically increase cache hits and reduce
         * compilation times if enabled at the sacrifice to rewriting of the script that could reduce readability.
         */
        fun of(traversalSource: String, withParameters: Boolean): GroovyTranslator {
            return of(traversalSource, DefaultTypeTranslator(withParameters))
        }

        /**
         * Creates the translator with a custom [TypeTranslator] instance.
         */
        fun of(traversalSource: String, typeTranslator: TypeTranslator): GroovyTranslator {
            return GroovyTranslator(traversalSource, typeTranslator)
        }
    }
}