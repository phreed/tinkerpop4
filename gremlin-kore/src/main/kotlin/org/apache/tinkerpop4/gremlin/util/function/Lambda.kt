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
package org.apache.tinkerpop4.gremlin.util.function

import org.apache.tinkerpop4.gremlin.jsr223.GremlinScriptEngine

/**
 * Provides a way to serialize string lambdas as scripts which can be evaluated by a [GremlinScriptEngine].
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
interface Lambda : Serializable {
    val lambdaScript: String
    val lambdaLanguage: String
    val lambdaArguments: Int

    abstract class AbstractLambda private constructor(
        private val lambdaSource: String,
        @get:Override override val lambdaLanguage: String,
        @get:Override override val lambdaArguments: Int
    ) : Lambda {

        @Override
        override fun getLambdaScript(): String {
            return lambdaSource
        }

        @Override
        override fun toString(): String {
            return "lambda[" + lambdaSource + "]"
        }

        @Override
        override fun hashCode(): Int {
            return lambdaSource.hashCode() + lambdaLanguage.hashCode() + lambdaArguments
        }

        @Override
        override fun equals(`object`: Object): Boolean {
            return `object` is Lambda && (`object` as Lambda).lambdaArguments == lambdaArguments &&
                    (`object` as Lambda).lambdaScript.equals(lambdaSource) &&
                    (`object` as Lambda).lambdaLanguage.equals(lambdaLanguage)
        }
    }

    class UnknownArgLambda(lambdaSource: String, lambdaLanguage: String, lambdaArguments: Int) :
        AbstractLambda(lambdaSource, lambdaLanguage, lambdaArguments)

    class ZeroArgLambda<A>(lambdaSource: String, lambdaLanguage: String) :
        AbstractLambda(lambdaSource, lambdaLanguage, 0), Supplier<A> {
        @Override
        fun get(): A? {
            return null
        }
    }

    class UnaryLambda<A>(lambdaSource: String, lambdaLanguage: String) :
        OneArgLambda<A, A>(lambdaSource, lambdaLanguage), UnaryOperator<A>

    class OneArgLambda<A, B>(lambdaSource: String, lambdaLanguage: String) :
        AbstractLambda(lambdaSource, lambdaLanguage, 1), Function<A, B>, Predicate<A>, Consumer<A> {
        @Override
        fun apply(a: A): B? {
            return null
        }

        @Override
        fun test(a: A): Boolean {
            return false
        }

        @Override
        fun accept(a: A) {
        }
    }

    class BinaryLambda<A>(lambdaSource: String, lambdaLanguage: String) :
        TwoArgLambda<A, A, A>(lambdaSource, lambdaLanguage), BinaryOperator<A>

    class TwoArgLambda<A, B, C>(lambdaSource: String, lambdaLanguage: String) :
        AbstractLambda(lambdaSource, lambdaLanguage, 2), BiFunction<A, B, C>, Comparator<A> {
        @Override
        fun apply(a: A, b: B): C? {
            return null
        }

        @Override
        fun compare(first: A, second: A): Int {
            return 0
        }
    }

    companion object {
        ///
        const val DEFAULT_LAMBDA_LANGUAGE = "gremlin-groovy"
        fun <A> unaryOperator(lambdaSource: String, lambdaLanguage: String): UnaryOperator<A>? {
            return UnaryLambda<Any>(lambdaSource, lambdaLanguage)
        }

        fun <A, B> function(lambdaSource: String, lambdaLanguage: String): Function<A, B>? {
            return OneArgLambda<Any, Any>(lambdaSource, lambdaLanguage)
        }

        fun <A> predicate(lambdaSource: String, lambdaLanguage: String): Predicate<A>? {
            return OneArgLambda<Any, Any>(lambdaSource, lambdaLanguage)
        }

        fun <A> consumer(lambdaSource: String, lambdaLanguage: String): Consumer<A>? {
            return OneArgLambda<Any, Any>(lambdaSource, lambdaLanguage)
        }

        fun <A> supplier(lambdaSource: String, lambdaLanguage: String): Supplier<A>? {
            return ZeroArgLambda<Any>(lambdaSource, lambdaLanguage)
        }

        fun <A> comparator(lambdaSource: String, lambdaLanguage: String): Comparator<A>? {
            return TwoArgLambda<Any, Any, Any>(lambdaSource, lambdaLanguage)
        }

        fun <A, B, C> biFunction(lambdaSource: String, lambdaLanguage: String): BiFunction<A, B, C>? {
            return TwoArgLambda<Any, Any, Any>(lambdaSource, lambdaLanguage)
        }

        //
        fun <A> unaryOperator(lambdaSource: String): UnaryOperator<A>? {
            return UnaryLambda<Any>(lambdaSource, DEFAULT_LAMBDA_LANGUAGE)
        }

        fun <A, B> function(lambdaSource: String): Function<A, B>? {
            return OneArgLambda<Any, Any>(lambdaSource, DEFAULT_LAMBDA_LANGUAGE)
        }

        fun <A> predicate(lambdaSource: String): Predicate<A>? {
            return OneArgLambda<Any, Any>(lambdaSource, DEFAULT_LAMBDA_LANGUAGE)
        }

        fun <A> consumer(lambdaSource: String): Consumer<A>? {
            return OneArgLambda<Any, Any>(lambdaSource, DEFAULT_LAMBDA_LANGUAGE)
        }

        fun <A> supplier(lambdaSource: String): Supplier<A>? {
            return ZeroArgLambda<Any>(lambdaSource, DEFAULT_LAMBDA_LANGUAGE)
        }

        fun <A> comparator(lambdaSource: String): Comparator<A>? {
            return TwoArgLambda<Any, Any, Any>(lambdaSource, DEFAULT_LAMBDA_LANGUAGE)
        }

        fun <A, B, C> biFunction(lambdaSource: String): BiFunction<A, B, C>? {
            return TwoArgLambda<Any, Any, Any>(lambdaSource, DEFAULT_LAMBDA_LANGUAGE)
        }

        fun <A> binaryOperator(lambdaSource: String): BinaryOperator<A>? {
            return BinaryLambda<Any>(lambdaSource, DEFAULT_LAMBDA_LANGUAGE)
        }
    }
}