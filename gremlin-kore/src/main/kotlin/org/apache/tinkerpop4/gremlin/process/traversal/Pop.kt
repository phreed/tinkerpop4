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
package org.apache.tinkerpop4.gremlin.process.traversal

/**
 * A [Path] may have multiple values associated with a single label.
 * [Pop] is used to determine whether the first value, last value, or
 * all values are gathered. Note that [Pop.mixed] will return results
 * as a list if there are multiple (like [Pop.all]) or as a singleton
 * if only one object in the path is labeled (like [Pop.last]).
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
enum class Pop {
    /**
     * The first item in an ordered collection (i.e. `collection[0]`).
     *
     * @since 3.0.0-incubating
     */
    first,

    /**
     * The last item in an ordered collection (i.e. `collection[collection.size()-1]`).
     *
     * @since 3.0.0-incubating
     */
    last,

    /**
     * Get all the items and return them as a list.
     *
     * @since 3.0.0-incubating
     */
    all,

    /**
     * Get the items as either a list (for multiple) or an object (for singles).
     */
    mixed
}