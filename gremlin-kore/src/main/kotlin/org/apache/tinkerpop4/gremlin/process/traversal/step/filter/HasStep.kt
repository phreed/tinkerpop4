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
package org.apache.tinkerpop4.gremlin.process.traversal.step.filter

import org.apache.tinkerpop4.gremlin.process.traversal.Traversal

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class HasStep<S : Element?>(traversal: Traversal.Admin?, vararg hasContainers: HasContainer?) :
    FilterStep<S>(traversal), HasContainerHolder, Configuring {
    private val parameters: Parameters = Parameters()
    private var hasContainers: List<HasContainer>

    init {
        this.hasContainers = ArrayList()
        Collections.addAll(this.hasContainers, hasContainers)
    }

    @Override
    fun getParameters(): Parameters {
        return parameters
    }

    @Override
    fun configure(vararg keyValues: Object?) {
        parameters.set(null, keyValues)
    }

    @Override
    protected fun filter(traverser: Traverser.Admin<S>): Boolean {
        // the generic S is defined as Element but Property can also be used with HasStep so this seems to cause
        // problems with some jdk versions.
        return if (traverser.get() is Element) HasContainer.testAll(
            traverser.get(),
            hasContainers
        ) else if (traverser.get() is Property) HasContainer.testAll(
            traverser.get() as Property,
            hasContainers
        ) else throw IllegalStateException(
            String.format(
                "Traverser to has() must be of type Property or Element, not %s",
                traverser.get().getClass().getName()
            )
        )
    }

    @Override
    override fun toString(): String {
        return StringFactory.stepString(this, hasContainers)
    }

    @Override
    fun getHasContainers(): List<HasContainer> {
        return Collections.unmodifiableList(hasContainers)
    }

    @Override
    fun removeHasContainer(hasContainer: HasContainer?) {
        hasContainers.remove(hasContainer)
    }

    @Override
    fun addHasContainer(hasContainer: HasContainer?) {
        hasContainers.add(hasContainer)
    }

    @get:Override
    val requirements: Set<Any>
        get() = EnumSet.of(TraverserRequirement.OBJECT)

    @Override
    fun clone(): HasStep<S> {
        val clone = super.clone() as HasStep<S>
        clone.hasContainers = ArrayList()
        for (hasContainer in hasContainers) {
            clone.addHasContainer(hasContainer.clone())
        }
        return clone
    }

    @Override
    override fun hashCode(): Int {
        var result = super.hashCode()
        for (hasContainer in hasContainers) {
            result = result xor hasContainer.hashCode()
        }
        return result
    }
}