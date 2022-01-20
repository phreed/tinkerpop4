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
package org.apache.tinkerpop4.gremlin.structure

/**
 * This enumeration allows for the specification of the type of a [Property].
 * Properties can either be their standard form or value form. Note that this is different than a property
 * class like [Property] or [VertexProperty]. This enumeration is used to denote those aspects of a
 * property that can not be realized by class alone.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
enum class PropertyType {
    PROPERTY {
        @Override
        override fun forProperties(): Boolean {
            return true
        }

        @Override
        override fun forValues(): Boolean {
            return false
        }
    },
    VALUE {
        @Override
        override fun forProperties(): Boolean {
            return false
        }

        @Override
        override fun forValues(): Boolean {
            return true
        }
    };

    abstract fun forProperties(): Boolean
    abstract fun forValues(): Boolean
}