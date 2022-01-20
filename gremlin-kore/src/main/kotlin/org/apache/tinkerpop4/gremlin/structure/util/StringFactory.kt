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
package org.apache.tinkerpop4.gremlin.structure.util

import org.apache.commons.lang3.StringUtils

/**
 * A collection of helpful methods for creating standard [Object.toString] representations of graph-related
 * objects.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
object StringFactory {
    private const val V = "v"
    private const val E = "e"
    private const val P = "p"
    private const val VP = "vp"
    private const val PATH = "path"
    private const val L_BRACKET = "["
    private const val R_BRACKET = "]"
    private const val COMMA_SPACE = ", "
    private const val DASH = "-"
    private const val ARROW = "->"
    private const val EMPTY_PROPERTY = "p[empty]"
    private const val EMPTY_VERTEX_PROPERTY = "vp[empty]"
    private val LINE_SEPARATOR: String = System.getProperty("line.separator")
    private const val STORAGE = "storage"
    private const val featuresStartWith = "supports"
    private const val prefixLength: Int = featuresStartWith.length()

    /**
     * Construct the representation for a [Vertex].
     */
    fun vertexString(vertex: Vertex): String {
        return V + L_BRACKET + vertex.id() + R_BRACKET
    }

    /**
     * Construct the representation for a [Edge].
     */
    fun edgeString(edge: Edge): String {
        return E + L_BRACKET + edge.id() + R_BRACKET + L_BRACKET + edge.outVertex()
            .id() + DASH + edge.label() + ARROW + edge.inVertex().id() + R_BRACKET
    }

    /**
     * Construct the representation for a [Property] or [VertexProperty].
     */
    fun propertyString(property: Property): String {
        return if (property is VertexProperty) {
            if (!property.isPresent()) return EMPTY_VERTEX_PROPERTY
            val valueString: String = String.valueOf(property.value())
            VP + L_BRACKET + property.key() + ARROW + StringUtils.abbreviate(
                valueString,
                20
            ) + R_BRACKET
        } else {
            if (!property.isPresent()) return EMPTY_PROPERTY
            val valueString: String = String.valueOf(property.value())
            P + L_BRACKET + property.key() + ARROW + StringUtils.abbreviate(
                valueString,
                20
            ) + R_BRACKET
        }
    }

    /**
     * Construct the representation for a [Graph].
     *
     * @param internalString a mapper [String] that appends to the end of the standard representation
     */
    fun graphString(graph: Graph, internalString: String): String {
        return graph.getClass().getSimpleName().toLowerCase() + L_BRACKET + internalString + R_BRACKET
    }

    fun graphVariablesString(variables: Graph.Variables): String {
        return "variables" + L_BRACKET + "size:" + variables.keys().size() + R_BRACKET
    }

    fun memoryString(memory: Memory): String {
        return "memory" + L_BRACKET + "size:" + memory.keys().size() + R_BRACKET
    }

    fun computeResultString(computerResult: ComputerResult): String {
        return "result" + L_BRACKET + computerResult.graph() + ',' + computerResult.memory() + R_BRACKET
    }

    fun graphComputerString(graphComputer: GraphComputer): String {
        return graphComputer.getClass().getSimpleName().toLowerCase()
    }

    fun traversalSourceString(traversalSource: TraversalSource): String {
        val graphString: String = traversalSource.getGraph().toString()
        val optional: Optional<Computer> = VertexProgramStrategy.getComputer(traversalSource.getStrategies())
        return traversalSource.getClass().getSimpleName()
            .toLowerCase() + L_BRACKET + graphString + COMMA_SPACE + (if (optional.isPresent()) optional.get()
            .toString() else "standard") + R_BRACKET
    }

    fun featureString(features: Graph.Features): String {
        val sb = StringBuilder("FEATURES")
        val supportMethods: Predicate<Method> = Predicate<Method> { m ->
            m.getModifiers() === Modifier.PUBLIC && m.getName().startsWith(
                featuresStartWith
            ) && !m.getName().equals(featuresStartWith)
        }
        sb.append(LINE_SEPARATOR)
        Stream.of(
            Pair.with(GraphFeatures::class.java, features.graph()),
            Pair.with(VariableFeatures::class.java, features.graph().variables()),
            Pair.with(VertexFeatures::class.java, features.vertex()),
            Pair.with(VertexPropertyFeatures::class.java, features.vertex().properties()),
            Pair.with(EdgeFeatures::class.java, features.edge()),
            Pair.with(EdgePropertyFeatures::class.java, features.edge().properties())
        ).forEach { p ->
            printFeatureTitle(p.getValue0(), sb)
            Stream.of(p.getValue0().getMethods())
                .filter(supportMethods)
                .map(createTransform(p.getValue1()))
                .forEach(sb::append)
        }
        return sb.toString()
    }

    fun traversalSideEffectsString(traversalSideEffects: TraversalSideEffects): String {
        return "sideEffects" + L_BRACKET + "size:" + traversalSideEffects.keys().size() + R_BRACKET
    }

    fun traversalStrategiesString(traversalStrategies: TraversalStrategies): String {
        return "strategies" + traversalStrategies.toList()
    }

    fun traversalStrategyString(traversalStrategy: TraversalStrategy): String {
        return traversalStrategy.getClass().getSimpleName()
    }

    fun traversalStrategyProxyString(traversalStrategyProxy: TraversalStrategyProxy): String {
        return traversalStrategyProxy.getStrategyClass().getSimpleName()
    }

    fun translatorString(translator: Translator): String {
        return "translator" + L_BRACKET + translator.getTraversalSource()
            .toString() + ":" + translator.getTargetLanguage() + R_BRACKET
    }

    fun vertexProgramString(vertexProgram: VertexProgram, internalString: String): String {
        return vertexProgram.getClass().getSimpleName() + L_BRACKET + internalString + R_BRACKET
    }

    fun vertexProgramString(vertexProgram: VertexProgram): String {
        return vertexProgram.getClass().getSimpleName()
    }

    fun mapReduceString(mapReduce: MapReduce, internalString: String): String {
        return mapReduce.getClass().getSimpleName() + L_BRACKET + internalString + R_BRACKET
    }

    fun mapReduceString(mapReduce: MapReduce): String {
        return mapReduce.getClass().getSimpleName()
    }

    private fun createTransform(features: FeatureSet): Function<Method, String> {
        return FunctionUtils.wrapFunction { m ->
            ">-- " + m.getName().substring(prefixLength).toString() + ": " + m.invoke(features)
                .toString() + LINE_SEPARATOR
        }
    }

    private fun printFeatureTitle(featureClass: Class<out FeatureSet?>, sb: StringBuilder) {
        sb.append("> ")
        sb.append(featureClass.getSimpleName())
        sb.append(LINE_SEPARATOR)
    }

    fun stepString(step: Step<*, *>, vararg arguments: Object?): String {
        val builder = StringBuilder(step.getClass().getSimpleName())
        val strings: List<String> = Stream.of(arguments)
            .filter { o ->
                if (o is TraversalRing) return@filter !(o as TraversalRing).isEmpty() else if (o is Collection) return@filter !(o as Collection).isEmpty() else if (o is Map) return@filter !(o as Map).isEmpty() else return@filter !Objects.toString(
                    o
                ).isEmpty()
            }
            .map { o ->
                val string: String = Objects.toString(o)
                if (hasLambda(string)) "lambda" else string
            }.collect(Collectors.toList())
        if (!strings.isEmpty()) {
            builder.append('(')
            builder.append(String.join(",", strings))
            builder.append(')')
        }
        if (!step.getLabels().isEmpty()) builder.append('@').append(step.getLabels())
        return builder.toString()
    }

    private fun hasLambda(objectString: String): Boolean {
        return objectString.contains("\$Lambda$") ||  // JAVA (org.apache.tinkerpop4.gremlin.tinkergraph.structure.TinkerGraphPlayTest$$Lambda$1/1711574013@61a52fb)
                objectString.contains("$_run_closure") ||  // GROOVY (groovysh_evaluate$_run_closure1@db44aa2)
                objectString.contains("<lambda>") // PYTHON (<function <lambda> at 0x10dfaec80>)
    }

    fun traversalString(traversal: Traversal.Admin<*, *>): String {
        // generally speaking the need to show the profile steps in a toString() is limited to TinkerPop developers.
        // most users would be confused by the output to profile as these steps are proliferated about the traversal
        // and are internal collectors of data not the actual steps the user specified. the traversal profile output
        // looks cleaner without this cruft and hopefully the lack of it's existence should not confuse TinkerPop
        // developers trying to debug things. when You, yes, you future Stephen, find this comment....sorry?
        return traversal.getSteps().stream().filter { s -> s !is ProfileStep && s !is ProfileSideEffectStep }
            .collect(Collectors.toList()).toString()
    }

    fun storageString(internalString: String): String {
        return STORAGE + L_BRACKET + internalString + R_BRACKET
    }

    fun removeEndBrackets(collection: Collection): String {
        val string: String = collection.toString()
        return string.substring(1, string.length() - 1)
    }

    fun pathString(path: Path?): String {
        return PATH + L_BRACKET + String.join(", ", IteratorUtils.map(path, Objects::toString)) + R_BRACKET
    }
}