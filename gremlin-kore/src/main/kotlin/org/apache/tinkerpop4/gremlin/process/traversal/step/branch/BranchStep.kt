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
package org.apache.tinkerpop4.gremlin.process.traversal.step.branch

import org.apache.tinkerpop4.gremlin.process.traversal.Traversal

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Daniel Kuppitz (http://gremlin.guru)
 */
class BranchStep<S, E, M>(traversal: Traversal.Admin?) : ComputerAwareStep<S, E>(traversal),
    TraversalOptionParent<M, S, E> {
    protected var branchTraversal: Traversal.Admin<S, M>? = null
    protected var traversalPickOptions: Map<Pick, List<Traversal.Admin<S, E>>> = HashMap()
    protected var traversalOptions: List<Pair<Traversal.Admin, Traversal.Admin<S, E>>> = ArrayList()
    private var first = true
    private var hasBarrier = false
    fun setBranchTraversal(branchTraversal: Traversal.Admin<S, M>?) {
        this.branchTraversal = this.integrateChild(branchTraversal)
    }

    @Override
    fun addGlobalChildOption(pickToken: M, traversalOption: Traversal.Admin<S, E>) {
        if (pickToken is Pick) {
            if (traversalPickOptions.containsKey(pickToken)) traversalPickOptions[pickToken].add(traversalOption) else traversalPickOptions.put(
                pickToken as Pick,
                ArrayList(Collections.singletonList(traversalOption))
            )
        } else {
            val pickOptionTraversal: Traversal.Admin
            if (pickToken is Traversal) {
                pickOptionTraversal = (pickToken as Traversal).asAdmin()
            } else {
                pickOptionTraversal = PredicateTraversal(pickToken)
            }
            traversalOptions.add(Pair.with(pickOptionTraversal, traversalOption))
        }
        traversalOption.addStep(EndStep(traversalOption))
        if (!hasBarrier && !TraversalHelper.getStepsOfAssignableClassRecursively(Barrier::class.java, traversalOption)
                .isEmpty()
        ) hasBarrier = true
        this.integrateChild(traversalOption)
    }

    @get:Override
    val requirements: Set<Any>
        get() = this.getSelfAndChildRequirements()

    @get:Override
    val globalChildren: List<Any>
        get() = Collections.unmodifiableList(
            Stream.concat(
                traversalPickOptions.values().stream().flatMap(List::stream),
                traversalOptions.stream().map(Pair::getValue1)
            ).collect(Collectors.toList())
        )

    @get:Override
    val localChildren: List<Any>
        get() = Collections.singletonList(branchTraversal)

    @Override
    protected fun standardAlgorithm(): Iterator<Traverser.Admin<E>> {
        while (true) {
            if (!first) {
                // this block is ignored on the first pass through the while(true) giving the opportunity for
                // the traversalOptions to be prepared. Iterate all of them and simply return the ones that yield
                // results. applyCurrentTraverser() will have only injected the current traverser into the options
                // that met the choice requirements.
                for (option in globalChildren) {
                    // checking hasStarts() first on the step in case there is a ReducingBarrierStep which will
                    // always return true for hasNext()
                    if (option.getStartStep().hasStarts() && option.hasNext()) return option.getEndStep()
                }
            }
            first = false

            // pass the current traverser to applyCurrentTraverser() which will make the "choice" of traversal to
            // apply with the given traverser. as this is in a while(true) this phase essentially prepares the options
            // for execution above
            if (hasBarrier) {
                if (!this.starts.hasNext()) throw FastNoSuchElementException.instance()
                while (this.starts.hasNext()) {
                    applyCurrentTraverser(this.starts.next())
                }
            } else {
                applyCurrentTraverser(this.starts.next())
            }
        }
    }

    /**
     * Choose the right traversal option to apply and seed those options with this traverser.
     */
    private fun applyCurrentTraverser(start: Traverser.Admin<S>) {
        // first get the value of the choice based on the current traverser and use that to select the right traversal
        // option to which that traverser should be routed
        val choice: Object = TraversalUtil.apply(start, branchTraversal)
        val branches: List<Traversal.Admin<S, E>>? = pickBranches(choice)

        // if a branch is identified, then split the traverser and add it to the start of the option so that when
        // that option is iterated (in the calling method) that value can be applied.
        branches?.forEach { traversal -> traversal.addStart(start.split()) }
        if (choice !== Pick.any) {
            val anyBranch: List<Traversal.Admin<S, E>>? = traversalPickOptions[Pick.any]
            anyBranch?.forEach { traversal -> traversal.addStart(start.split()) }
        }
    }

    @Override
    protected fun computerAlgorithm(): Iterator<Traverser.Admin<E>> {
        val ends: List<Traverser.Admin<E>> = ArrayList()
        val start: Traverser.Admin<S> = this.starts.next()
        val choice: Object = TraversalUtil.apply(start, branchTraversal)
        val branches: List<Traversal.Admin<S, E>>? = pickBranches(choice)
        branches?.forEach { traversal ->
            val split: Traverser.Admin<E> = start.split() as Traverser.Admin<E>
            split.setStepId(traversal.getStartStep().getId())
            //split.addLabels(this.labels);
            ends.add(split)
        }
        if (choice !== Pick.any) {
            val anyBranch: List<Traversal.Admin<S, E>>? = traversalPickOptions[Pick.any]
            anyBranch?.forEach { traversal ->
                val split: Traverser.Admin<E> = start.split() as Traverser.Admin<E>
                split.setStepId(traversal.getStartStep().getId())
                //split.addLabels(this.labels);
                ends.add(split)
            }
        }
        return ends.iterator()
    }

    private fun pickBranches(choice: Object): List<Traversal.Admin<S, E>>? {
        val branches: List<Traversal.Admin<S, E>> = ArrayList()
        if (choice is Pick) {
            if (traversalPickOptions.containsKey(choice)) {
                branches.addAll(traversalPickOptions[choice])
            }
        }
        for (p in traversalOptions) {
            if (TraversalUtil.test(choice, p.getValue0())) {
                branches.add(p.getValue1())
            }
        }
        return if (branches.isEmpty()) traversalPickOptions[Pick.none] else branches
    }

    @Override
    fun clone(): BranchStep<S, E, M> {
        val clone = super.clone() as BranchStep<S, E, M>
        clone.traversalPickOptions = HashMap(traversalPickOptions.size())
        clone.traversalOptions = ArrayList(traversalOptions.size())
        for (entry in traversalPickOptions.entrySet()) {
            val traversals: List<Traversal.Admin<S, E>> = entry.getValue()
            if (traversals.size() > 0) {
                val clonedTraversals: List<Traversal.Admin<S, E>> =
                    clone.traversalPickOptions.compute(entry.getKey()) { k, v -> if (v == null) ArrayList(traversals.size()) else v }
                for (traversal in traversals) {
                    clonedTraversals.add(traversal.clone())
                }
            }
        }
        for (pair in traversalOptions) {
            clone.traversalOptions.add(Pair.with(pair.getValue0().clone(), pair.getValue1().clone()))
        }
        clone.branchTraversal = branchTraversal.clone()
        return clone
    }

    @Override
    fun setTraversal(parentTraversal: Traversal.Admin<*, *>?) {
        super.setTraversal(parentTraversal)
        this.integrateChild(branchTraversal)
        globalChildren.forEach(this::integrateChild)
    }

    @Override
    override fun hashCode(): Int {
        var result = super.hashCode()
        if (traversalPickOptions != null) result = result xor traversalPickOptions.hashCode()
        if (traversalOptions != null) result = result xor traversalOptions.hashCode()
        if (branchTraversal != null) result = result xor branchTraversal.hashCode()
        return result
    }

    @Override
    override fun toString(): String {
        val combinedOptions: List<Pair> = Stream.concat(
            traversalPickOptions.entrySet().stream().map { e -> Pair.with(e.getKey(), e.getValue()) },
            traversalOptions.stream()
        ).collect(Collectors.toList())
        return StringFactory.stepString(this, branchTraversal, combinedOptions)
    }

    @Override
    fun reset() {
        super.reset()
        globalChildren.forEach(Traversal.Admin::reset)
        first = true
    }
}