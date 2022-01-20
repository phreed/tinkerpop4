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
package org.apache.tinkerpop4.gremlin.process.traversal.step.map

import net.objecthunter.exp4j.Expression

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class MathStep<S>(traversal: Traversal.Admin?, private val equation: String) : MapStep<S, Double?>(traversal),
    ByModulating, TraversalParent, Scoping, PathProcessor {
    private val expression: TinkerExpression
    private var traversalRing: TraversalRing<S, Number> = TraversalRing()

    @get:Override
    @set:Override
    var keepLabels: Set<String>? = null
    @Override
    protected fun processNextStart(): Traverser.Admin<Double> {
        val traverser: Traverser.Admin = this.starts.next()
        val localExpression = Expression(expression.expression)
        var productive = true
        for (`var` in expression.variables) {
            val product: TraversalProduct = if (`var`.equals(CURRENT)) TraversalUtil.produce(
                traverser,
                traversalRing.next()
            ) else TraversalUtil.produce(
                this.getNullableScopeValue(Pop.last, `var`, traverser) as S,
                traversalRing.next()
            )
            if (!product.isProductive()) {
                productive = false
                break
            }
            val o: Object = product.get()

            // it's possible for ElementValueTraversal to return null or something that is possibly not a Number.
            // worth a check to try to return a nice error message. The TraversalRing<S, Number> is a bit optimistic
            // given type erasure. It could easily end up otherwise.
            if (o !is Number) throw IllegalStateException(
                String.format(
                    "The variable %s for math() step must resolve to a Number - it is instead of type %s with value %s",
                    `var`, if (Objects.isNull(o)) "null" else o.getClass().getName(), o
                )
            )
            localExpression.setVariable(`var`, (o as Number).doubleValue())
        }
        traversalRing.reset()

        // if at least one of the traversals wasnt productive it will filter
        return if (productive) PathProcessor.processTraverserPathLabels(
            traverser.split(
                localExpression.evaluate(),
                this
            ), keepLabels
        ) else EmptyTraverser.instance()
    }

    @Override
    fun modulateBy(selectTraversal: Traversal.Admin<*, *>?) {
        traversalRing.addTraversal(this.integrateChild(selectTraversal))
    }

    // this is a trick i saw in DedupGlobalStep that allows ComputerVerificationStrategy to be happy for OLAP.
    // it's a bit more of a hack here. in DedupGlobalStep, the dedup operation really only just needs the ID, but
    // here the true max requirement is PROPERTIES, but because of how map() works in this implementation in
    // relation to CURRENT, if we don't access the path labels, then we really only just operate on the stargraph
    // and are thus OLAP safe. In tracing around the code a bit, I don't see a problem with taking this approach,
    // but I suppose a better way might be make it more clear when this step is dealing with an actual path and
    // when it is not and/or adjust ComputerVerificationStrategy to cope with the situation where math() is only
    // dealing with the local stargraph.
    @get:Override
    val maxRequirement: ElementRequirement
        get() =// this is a trick i saw in DedupGlobalStep that allows ComputerVerificationStrategy to be happy for OLAP.
        // it's a bit more of a hack here. in DedupGlobalStep, the dedup operation really only just needs the ID, but
        // here the true max requirement is PROPERTIES, but because of how map() works in this implementation in
        // relation to CURRENT, if we don't access the path labels, then we really only just operate on the stargraph
        // and are thus OLAP safe. In tracing around the code a bit, I don't see a problem with taking this approach,
        // but I suppose a better way might be make it more clear when this step is dealing with an actual path and
        // when it is not and/or adjust ComputerVerificationStrategy to cope with the situation where math() is only
            // dealing with the local stargraph.
            if (expression.variables.contains(CURRENT) && expression.variables.size() === 1) ElementRequirement.ID else super@PathProcessor.getMaxRequirement()

    @Override
    override fun toString(): String {
        return StringFactory.stepString(this, equation, traversalRing)
    }

    @Override
    override fun hashCode(): Int {
        return super.hashCode() xor equation.hashCode() xor traversalRing.hashCode()
    }

    @get:Override
    val localChildren: List<Any>
        get() = traversalRing.getTraversals()

    @Override
    fun reset() {
        super.reset()
        traversalRing.reset()
    }

    @Override
    fun clone(): MathStep<S> {
        val clone = super.clone() as MathStep<S>
        clone.traversalRing = traversalRing.clone()
        return clone
    }

    @Override
    fun setTraversal(parentTraversal: Traversal.Admin<*, *>?) {
        super.setTraversal(parentTraversal)
        traversalRing.getTraversals().forEach(this::integrateChild)
    }

    @get:Override
    val requirements: Set<Any>
        get() = this.getSelfAndChildRequirements(TraverserRequirement.OBJECT, TraverserRequirement.SIDE_EFFECTS)

    @get:Override
    val scopeKeys: Set<String>
        get() = if (expression.variables.contains(CURRENT)) {
            val temp: Set<String> = HashSet(expression.variables)
            temp.remove(CURRENT)
            temp
        } else expression.variables

    init {
        expression = TinkerExpression(equation, getVariables(equation))
    }

    /**
     * A wrapper for the `Expression` class. That class is not marked `Serializable` and therefore gives
     * problems in OLAP specifically with Spark. This wrapper allows the `Expression` to be serialized in that
     * context with Java serialization.
     */
    class TinkerExpression(private val equation: String, val variables: Set<String>) : Serializable {
        @kotlin.jvm.Transient
        var expression: Expression? = null
            get() {
                if (null == field) {
                    field = ExpressionBuilder(equation)
                        .variables(variables)
                        .implicitMultiplication(false)
                        .build()
                }
                return field
            }
            private set

    }

    companion object {
        private const val CURRENT = "_"

        ///
        private val FUNCTIONS = arrayOf(
            "abs", "acos", "asin", "atan",
            "cbrt", "ceil", "cos", "cosh",
            "exp",
            "floor",
            "log", "log10", "log2",
            "signum", "sin", "sinh", "sqrt",
            "tan", "tanh"
        )
        private val VARIABLE_PATTERN: Pattern = Pattern.compile(
            "\\b(?!" +
                    String.join("|", FUNCTIONS).toString() + "|([0-9]+))([a-zA-Z_][a-zA-Z0-9_]*)\\b"
        )

        protected fun getVariables(equation: String?): Set<String> {
            val matcher: Matcher = VARIABLE_PATTERN.matcher(equation)
            val variables: Set<String> = LinkedHashSet()
            while (matcher.find()) {
                variables.add(matcher.group())
            }
            return variables
        }
    }
}