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
class SampleGlobalStep<S>(traversal: Traversal.Admin?, private val amountToSample: Int) :
    CollectingBarrierStep<S>(traversal), TraversalParent, ByModulating, Seedable {
    private var probabilityTraversal: Traversal.Admin<S, Number> = ConstantTraversal(1.0)
    private val random: Random = Random()
    @Override
    fun resetSeed(seed: Long) {
        random.setSeed(seed)
    }

    @get:Override
    val localChildren: List<Any>
        get() = Collections.singletonList(probabilityTraversal)

    @Override
    fun modulateBy(probabilityTraversal: Traversal.Admin<*, *>?) {
        this.probabilityTraversal = this.integrateChild(probabilityTraversal)
    }

    @Override
    fun replaceLocalChild(oldTraversal: Traversal.Admin<*, *>?, newTraversal: Traversal.Admin<*, *>?) {
        if (null != probabilityTraversal && probabilityTraversal.equals(oldTraversal)) probabilityTraversal =
            this.integrateChild(newTraversal)
    }

    @Override
    override fun toString(): String {
        return StringFactory.stepString(this, amountToSample, probabilityTraversal)
    }

    @Override
    fun processAllStarts() {
        while (this.starts.hasNext()) {
            createProjectedTraverser(this.starts.next()).ifPresent(traverserSet::add)
        }
    }

    @Override
    fun barrierConsumer(traverserSet: TraverserSet<S>) {
        // return the entire traverser set if the set is smaller than the amount to sample
        if (traverserSet.bulkSize() <= amountToSample) return
        //////////////// else sample the set
        var totalWeight = 0.0
        for (s in traverserSet) {
            totalWeight =
                totalWeight + (s as ProjectedTraverser<S, Number?>).getProjections().get(0).doubleValue() * s.bulk()
        }
        ///////
        val sampledSet: TraverserSet<S> = this.traversal.getTraverserSetSupplier().get() as TraverserSet<S>
        var runningAmountToSample = 0
        while (runningAmountToSample < amountToSample) {
            var reSample = false
            var runningTotalWeight = totalWeight
            for (s in traverserSet) {
                val sampleBulk: Long = if (sampledSet.contains(s)) sampledSet.get(s).bulk() else 0
                if (sampleBulk < s.bulk()) {
                    val currentWeight: Double =
                        (s as ProjectedTraverser<S, Number?>).getProjections().get(0).doubleValue()
                    for (i in 0 until s.bulk() - sampleBulk) {
                        if (random.nextDouble() <= currentWeight / runningTotalWeight) {
                            val split: Traverser.Admin<S> = s.split()
                            split.setBulk(1L)
                            sampledSet.add(split)
                            runningAmountToSample++
                            reSample = true
                            break
                        }
                        runningTotalWeight = runningTotalWeight - currentWeight
                    }
                    if (reSample || runningAmountToSample >= amountToSample) break
                }
            }
        }
        traverserSet.clear()
        traverserSet.addAll(sampledSet)
    }

    private fun createProjectedTraverser(traverser: Traverser.Admin<S>): Optional<ProjectedTraverser<S, Number>> {
        val product: TraversalProduct = TraversalUtil.produce(traverser, probabilityTraversal)
        return if (product.isProductive()) {
            val o: Object = product.get() as? Number
                ?: throw IllegalStateException(
                    String.format(
                        "Traverser %s does not evaluate to a number with %s", traverser, probabilityTraversal
                    )
                )
            Optional.of(ProjectedTraverser(traverser, Collections.singletonList(product.get() as Number)))
        } else {
            Optional.empty()
        }
    }

    @get:Override
    val requirements: Set<Any>
        get() = this.getSelfAndChildRequirements(TraverserRequirement.BULK)

    @Override
    fun clone(): SampleGlobalStep<S> {
        val clone = super.clone() as SampleGlobalStep<S>
        clone.probabilityTraversal = probabilityTraversal.clone()
        return clone
    }

    @Override
    fun setTraversal(parentTraversal: Traversal.Admin<*, *>?) {
        super.setTraversal(parentTraversal)
        integrateChild(probabilityTraversal)
    }

    @Override
    override fun hashCode(): Int {
        return super.hashCode() xor amountToSample xor probabilityTraversal.hashCode()
    }
}