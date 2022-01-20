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

import org.apache.tinkerpop4.gremlin.process.traversal.Bytecode

/**
 * This Translator will translate [Bytecode] into a representation that has been stripped of any user data
 * (anonymized). A default anonymizer is provided, but can be replaced with a custom anonymizer as needed. The
 * default anonymizer replaces any String, Numeric, Date, Timestamp, or UUID with a type-based token. Identical values
 * will receive the same token (e.g. if "foo" is assigned "string0" then all occurrences of "foo" will be replaced
 * with "string0").
 */
class AnonymizingTypeTranslator
/**
 * Default constructor creates a [DefaultAnonymizer] + withParameters=false.
 */ @JvmOverloads constructor(
    private val anonymizer: Anonymizer = DefaultAnonymizer(),
    withParameters: Boolean = false
) : GroovyTranslator.DefaultTypeTranslator(withParameters) {
    /**
     * Customizable anonymizer interface.
     */
    interface Anonymizer {
        /**
         * Return an anonymized token for the supplied object.
         *
         * @param obj a [Traversal] object of one of the following types: String, Long, Double, FLoat, Integer,
         * Class, TImestamp, Date, UUID, [Vertex], [Edge], [VertexProperty]
         * @return    an anonymized version of the supplied object
         */
        fun anonymize(obj: Object?): Object
    }

    /**
     * This default implementation keeps a map from type (Java Class) to another map from instances to anonymized
     * token.
     */
    class DefaultAnonymizer : Anonymizer {
        /*
         * Map<ClassName, Map<Object, AnonymizedValue>>
         */
        private val simpleNameToObjectCache: Map<String, Map<Object, String>> = HashMap()

        /**
         * Return an anonymized token for the supplied object of the form "type:instance#".
         */
        @Override
        override fun anonymize(obj: Object): Object {
            val type: String = obj.getClass().getSimpleName()
            var objectToAnonymizedString: Map<Object, String?>? = simpleNameToObjectCache[type]
            return if (objectToAnonymizedString != null) {
                // this object type has been handled at least once before
                val innerValue = objectToAnonymizedString[obj]
                if (innerValue != null) {
                    innerValue
                } else {
                    val anonymizedValue: String = type.toLowerCase() + objectToAnonymizedString.size()
                    objectToAnonymizedString.put(obj, anonymizedValue)
                    anonymizedValue
                }
            } else {
                objectToAnonymizedString = HashMap()
                simpleNameToObjectCache.put(type, objectToAnonymizedString)
                val anonymizedValue: String = type.toLowerCase() + objectToAnonymizedString!!.size()
                objectToAnonymizedString.put(obj, anonymizedValue)
                anonymizedValue
            }
        }
    }

    constructor(withParameters: Boolean) : this(DefaultAnonymizer(), withParameters) {}

    @Override
    protected fun getSyntax(o: String?): String {
        return anonymizer.anonymize(o).toString()
        //      Original syntax:
//        return (o.contains("\"") ? "\"\"\"" + StringEscapeUtils.escapeJava(o) + "\"\"\"" : "\"" + StringEscapeUtils.escapeJava(o) + "\"")
//                .replace("$", "\\$");
    }

    @Override
    protected fun getSyntax(o: Date?): String {
        return anonymizer.anonymize(o).toString()
        //      Original syntax:
//        return "new Date(" + o.getTime() + "L)";
    }

    @Override
    protected fun getSyntax(o: Timestamp?): String {
        return anonymizer.anonymize(o).toString()
        //      Original syntax:
//        return "new Timestamp(" + o.getTime() + "L)";
    }

    @Override
    protected fun getSyntax(o: UUID?): String {
        return anonymizer.anonymize(o).toString()
        //      Original syntax:
//        return "UUID.fromString('" + o.toString() + "')";
    }

    @Override
    protected fun getSyntax(o: Number?): String {
        return anonymizer.anonymize(o).toString()
        //      Original syntax:
//        if (o instanceof Long)
//            return o + "L";
//        else if (o instanceof Double)
//            return o + "d";
//        else if (o instanceof Float)
//            return o + "f";
//        else if (o instanceof Integer)
//            return "(int) " + o;
//        else if (o instanceof Byte)
//            return "(byte) " + o;
//        if (o instanceof Short)
//            return "(short) " + o;
//        else if (o instanceof BigInteger)
//            return "new BigInteger('" + o.toString() + "')";
//        else if (o instanceof BigDecimal)
//            return "new BigDecimal('" + o.toString() + "')";
//        else
//            return o.toString();
    }

    @Override
    protected fun produceScript(o: Class<*>?): Script {
        return script.append(anonymizer.anonymize(o).toString())
        //      Original syntax:
//        return script.append(CoreImports.getClassImports().contains(o) ? o.getSimpleName() : o.getCanonicalName());
    }
}