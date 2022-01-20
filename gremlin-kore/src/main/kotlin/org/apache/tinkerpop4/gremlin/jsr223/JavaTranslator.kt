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
package org.apache.tinkerpop4.gremlin.jsr223

import org.apache.commons.configuration2.Configuration

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class JavaTranslator<S : TraversalSource?, T : Traversal.Admin<*, *>?> private constructor(@get:Override val traversalSource: S) :
    StepTranslator<S, T> {
    private val anonymousTraversal: Class<*>
    private val localMethodCache: Map<Class<*>, Map<String, Method>> = ConcurrentHashMap()
    private val anonymousTraversalStart: Method?

    init {
        anonymousTraversal = traversalSource.getAnonymousTraversalClass().orElse(null)
        anonymousTraversalStart = startMethodFromAnonymousTraversal
    }

    @Override
    fun translate(bytecode: Bytecode): T? {
        if (BytecodeHelper.isGraphOperation(bytecode)) throw IllegalArgumentException("JavaTranslator cannot translate traversal operations")
        var dynamicSource: TraversalSource? = traversalSource
        var traversal: Traversal.Admin<*, *>? = null
        for (instruction in bytecode.getSourceInstructions()) {
            dynamicSource = invokeMethod(
                dynamicSource,
                TraversalSource::class.java,
                instruction.getOperator(),
                instruction.getArguments()
            ) as TraversalSource
        }
        var spawned = false
        for (instruction in bytecode.getStepInstructions()) {
            if (!spawned) {
                traversal = invokeMethod(
                    dynamicSource,
                    Traversal::class.java,
                    instruction.getOperator(),
                    instruction.getArguments()
                ) as Traversal.Admin
                spawned = true
            } else invokeMethod(traversal, Traversal::class.java, instruction.getOperator(), instruction.getArguments())
        }
        return traversal
    }

    @get:Override
    val targetLanguage: String
        get() = "gremlin-java"

    @Override
    override fun toString(): String {
        return StringFactory.translatorString(this)
    }

    ////
    private fun translateObject(`object`: Object): Object {
        return if (`object` is Bytecode.Binding) translateObject((`object` as Bytecode.Binding).value()) else if (`object` is Bytecode) {
            try {
                val traversal: Traversal.Admin<*, *> = anonymousTraversalStart.invoke(null) as Traversal.Admin
                for (instruction in (`object` as Bytecode).getStepInstructions()) {
                    invokeMethod(
                        traversal,
                        Traversal::class.java,
                        instruction.getOperator(),
                        instruction.getArguments()
                    )
                }
                traversal
            } catch (e: Throwable) {
                throw IllegalStateException(e.getMessage())
            }
        } else if (`object` is TraversalStrategyProxy) {
            val map: Map<String, Object> = HashMap()
            val configuration: Configuration = (`object` as TraversalStrategyProxy).getConfiguration()
            configuration.getKeys()
                .forEachRemaining { key -> map.put(key, translateObject(configuration.getProperty(key))) }
            invokeStrategyCreationMethod(`object`, map)
        } else if (`object` is Map) {
            val map: Map<Object, Object> =
                if (`object` is Tree) Tree() else if (`object` is LinkedHashMap) LinkedHashMap((`object` as Map).size()) else HashMap(
                    (`object` as Map).size()
                )
            for (entry in (`object` as Map<*, *>).entrySet()) {
                map.put(translateObject(entry.getKey()), translateObject(entry.getValue()))
            }
            map
        } else if (`object` is List) {
            val list: List<Object> = ArrayList((`object` as List).size())
            for (o in `object`) {
                list.add(translateObject(o))
            }
            list
        } else if (`object` is BulkSet) {
            val bulkSet: BulkSet<Object> = BulkSet()
            for (entry in (`object` as BulkSet<*>).asBulk().entrySet()) {
                bulkSet.add(translateObject(entry.getKey()), entry.getValue())
            }
            bulkSet
        } else if (`object` is Set) {
            val set: Set<Object> =
                if (`object` is LinkedHashSet) LinkedHashSet((`object` as Set).size()) else HashSet((`object` as Set).size())
            for (o in `object`) {
                set.add(translateObject(o))
            }
            set
        } else `object`
    }

    private fun invokeStrategyCreationMethod(delegate: Object, map: Map<String, Object>): Object {
        val strategyClass: Class<*> = (delegate as TraversalStrategyProxy).getStrategyClass()
        val methodCache: Map<String, Method> = localMethodCache.computeIfAbsent(strategyClass) { k ->
            val cacheEntry: Map<String, Method> = HashMap()
            try {
                cacheEntry.put("instance", strategyClass.getMethod("instance"))
            } catch (ignored: NoSuchMethodException) {
                // nothing - the strategy may not be constructed this way
            }
            try {
                cacheEntry.put("create", strategyClass.getMethod("create", Configuration::class.java))
            } catch (ignored: NoSuchMethodException) {
                // nothing - the strategy may not be constructed this way
            }
            if (cacheEntry.isEmpty()) throw IllegalStateException(
                String.format(
                    "%s does can only be constructed with instance() or create(Configuration)",
                    strategyClass.getSimpleName()
                )
            )
            cacheEntry
        }
        return try {
            if (map.isEmpty()) methodCache["instance"].invoke(null) else methodCache["create"].invoke(
                null,
                MapConfiguration(map)
            )
        } catch (e: InvocationTargetException) {
            throw IllegalStateException(e.getMessage(), e)
        } catch (e: IllegalAccessException) {
            throw IllegalStateException(e.getMessage(), e)
        }
    }

    private fun invokeMethod(
        delegate: Object?,
        returnType: Class<*>,
        methodName: String,
        vararg arguments: Object
    ): Object {
        // populate method cache for fast access to methods in subsequent calls
        val methodCache: Map<String, List<ReflectedMethod>> =
            GLOBAL_METHOD_CACHE.getOrDefault(delegate.getClass(), HashMap())
        if (methodCache.isEmpty()) buildMethodCache(delegate, methodCache)

        // create a copy of the argument array so as not to mutate the original bytecode - no need to create a new
        // object if there are no arguments.
        val argumentsCopy: Array<Object?> = if (arguments.size > 0) arrayOfNulls<Object>(arguments.size) else arguments
        for (i in arguments.indices) {
            argumentsCopy[i] = translateObject(arguments[i])
        }

        // without this initial check iterating an invalid methodName will lead to a null pointer and a less than
        // great error message for the user. 
        if (!methodCache.containsKey(methodName)) {
            throw IllegalStateException(
                generateMethodNotFoundMessage(
                    "Could not locate method", delegate, methodName, argumentsCopy
                )
            )
        }
        try {
            for (methodx in methodCache[methodName]!!) {
                val method: Method = methodx.method
                if (returnType.isAssignableFrom(method.getReturnType())) {
                    val parameters: Array<Parameter> = methodx.parameters
                    if (parameters.size == argumentsCopy.size || methodx.hasVarArgs) {
                        val newArguments: Array<Object?> = arrayOfNulls<Object>(parameters.size)
                        var found = true
                        for (i in parameters.indices) {
                            if (parameters[i].isVarArgs()) {
                                val parameterClass: Class<*> = parameters[i].getType().getComponentType()
                                if (argumentsCopy.size > i && argumentsCopy[i] != null && !parameterClass.isAssignableFrom(
                                        argumentsCopy[i].getClass()
                                    )
                                ) {
                                    found = false
                                    break
                                }
                                val varArgs: Array<Object?> =
                                    Array.newInstance(parameterClass, argumentsCopy.size - i) as Array<Object?>
                                var counter = 0
                                for (j in i until argumentsCopy.size) {
                                    varArgs[counter++] = argumentsCopy[j]
                                }
                                newArguments[i] = varArgs
                                break
                            } else {
                                // try to detect the right method by comparing the type of the parameter to the type
                                // of the argument. doesn't always work so well because of null arguments which don't
                                // bring their type in bytecode and rely on position. this doesn't seem to happen often
                                // ...luckily...because method signatures tend to be sufficiently unique and do not
                                // encourage whacky use - like g.V().has(null, null) is clearly invalid so we don't
                                // even need to try to sort that out. on the other hand g.V().has('name',null) which
                                // is valid hits like four different possible overloads, but we can rely on the most
                                // generic one which takes Object as the second parameter. that seems to work in this
                                // case, but it's a shame this isn't nicer. seems like nicer would mean a heavy
                                // overhaul to Gremlin or to GLVs/bytecode and/or to serialization mechanisms.
                                //
                                // the check where argumentsCopy[i] is null could be accompanied by a type check for
                                // allowable signatures like:
                                // null == argumentsCopy[i] && parameters[i].getType() == Object.class
                                // but that doesn't seem helpful. perhaps this approach is fine as long as we ensure
                                // consistency of null calls to all overloads. in other words addV(String) must behave
                                // the same as addV(Traversal) if null is used as the argument. so far, that seems to
                                // be the case. if we find that is not happening we either fix that specific
                                // inconsistency, start special casing those method finds here, or as mentioned above
                                // do something far more drastic that doesn't involve reflection.
                                if (i < argumentsCopy.size && (null == argumentsCopy[i] ||
                                            argumentsCopy[i] != null && (parameters[i].getType()
                                        .isAssignableFrom(argumentsCopy[i].getClass()) ||
                                            parameters[i].getType().isPrimitive() &&
                                            (Number::class.java.isAssignableFrom(argumentsCopy[i].getClass()) ||
                                                    argumentsCopy[i].getClass().equals(Boolean::class.java) ||
                                                    argumentsCopy[i].getClass().equals(Byte::class.java) ||
                                                    argumentsCopy[i].getClass().equals(Character::class.java))))
                                ) {
                                    newArguments[i] = argumentsCopy[i]
                                } else {
                                    found = false
                                    break
                                }
                            }
                        }
                        if (found) {
                            return if (0 == newArguments.size) method.invoke(delegate) else method.invoke(
                                delegate,
                                newArguments
                            )
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            throw IllegalStateException(
                generateMethodNotFoundMessage(
                    e.getMessage(), null, methodName, argumentsCopy
                ), e
            )
        }
        throw IllegalStateException(
            generateMethodNotFoundMessage(
                "Could not locate method", delegate, methodName, argumentsCopy
            )
        )
    }

    /**
     * Generates the message used when a method cannot be located in the translation. Arguments are converted to
     * classes to avoid exposing data that might be sensitive.
     */
    private fun generateMethodNotFoundMessage(
        message: String, delegate: Object?,
        methodNameNotFound: String, args: Array<Object?>?
    ): String {
        val arguments: Array<Object?> = args ?: arrayOfNulls<Object>(0)
        val delegateClassName = if (delegate != null) delegate.getClass().getSimpleName() else ""
        return message + ": " + delegateClassName + "." + methodNameNotFound + "(" +
                Stream.of(arguments).map { a -> if (null == a) "null" else a.getClass().getSimpleName() }
                    .collect(Collectors.joining(", ")) + ")"
    }

    private val startMethodFromAnonymousTraversal: Method?
        private get() {
            if (anonymousTraversal != null) {
                try {
                    return anonymousTraversal.getMethod("start")
                } catch (ignored: NoSuchMethodException) {
                }
            }
            return null
        }

    private class ReflectedMethod(m: Method) {
        val method: Method
        val parameters: Array<Parameter>
        val hasVarArgs: Boolean

        init {
            method = m

            // the reflection getParameters() method calls clone() every time to get the Parameter array. caching it
            // saves a lot of extra processing
            parameters = m.getParameters()
            hasVarArgs = parameters.size > 0 && parameters[parameters.size - 1].isVarArgs()
        }
    }

    companion object {
        private val GLOBAL_METHOD_CACHE: Map<Class<*>, Map<String, List<ReflectedMethod>>> = ConcurrentHashMap()
        fun <S : TraversalSource?, T : Traversal.Admin<*, *>?> of(traversalSource: S): JavaTranslator<S, T> {
            return JavaTranslator(traversalSource)
        }

        @kotlin.jvm.Synchronized
        private fun buildMethodCache(delegate: Object?, methodCache: Map<String, List<ReflectedMethod>>) {
            if (methodCache.isEmpty()) {
                for (method in delegate.getClass().getMethods()) {
                    val list: List<ReflectedMethod> = methodCache.computeIfAbsent(method.getName()) { k -> ArrayList() }
                    list.add(ReflectedMethod(method))
                }
                GLOBAL_METHOD_CACHE.put(delegate.getClass(), methodCache)
            }
        }
    }
}