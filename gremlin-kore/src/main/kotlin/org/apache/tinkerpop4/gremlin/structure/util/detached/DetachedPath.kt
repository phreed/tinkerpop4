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
package org.apache.tinkerpop4.gremlin.structure.util.detached

import org.apache.tinkerpop4.gremlin.process.traversal.Path

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class DetachedPath : MutablePath, Attachable<Path?> {
    private constructor() {}

    fun get(): Path {
        return this
    }

    protected constructor(path: Path, withProperties: Boolean) {
        path.forEach { `object`, labels ->
            if (`object` is DetachedElement || `object` is DetachedProperty || `object` is DetachedPath) this.objects.add(
                `object`
            ) else this.objects.add(DetachedFactory.detach(`object`, withProperties))

            //Make a copy of the labels as its an UnmodifiableSet which can not be serialized.
            labels.add(LinkedHashSet(labels))
        }
    }

    @Override
    fun attach(method: Function<Attachable<Path?>?, Path?>?): Path {
        val path: Path = MutablePath.make()
        this.forEach { `object`, labels ->
            path.extend(
                if (`object` is Attachable) (`object` as Attachable).attach(
                    method
                ) else `object`, labels
            )
        }
        return path
    }
}