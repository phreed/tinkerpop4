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
package org.apache.tinkerpop4.gremlin.process.computer.traversal

import org.apache.tinkerpop4.gremlin.process.computer.Memory

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
internal object MasterExecutor {
    internal fun processMemory(
        traversalMatrix: TraversalMatrix<*, *>,
        memory: Memory,
        toProcessTraversers: TraverserSet<Object?>?,
        completedBarriers: Set<String?>
    ) {
        // handle traversers and data that were sent from the workers to the master traversal via memory
        if (memory.exists(TraversalVertexProgram.MUTATED_MEMORY_KEYS)) {
            for (key in memory.< Set < String > > get<Set<String>>(TraversalVertexProgram.MUTATED_MEMORY_KEYS)) {
                val step: Step<Object?, Object> = traversalMatrix.getStepById(key)
                assert(step is Barrier)
                completedBarriers.add(step.getId())
                if (step !is LocalBarrier) {  // local barriers don't do any processing on the master traversal (they just lock on the workers)
                    val barrier: Barrier<Object?> = step as Barrier<Object?>
                    barrier.addBarrier(memory.get(key))
                    step.forEachRemaining(toProcessTraversers::add)
                    // if it was a reducing barrier step, reset the barrier to its seed value
                    if (step is ReducingBarrierStep) memory.set(
                        step.getId(),
                        (step as ReducingBarrierStep).getSeedSupplier().get()
                    )
                }
            }
        }
        memory.set(TraversalVertexProgram.MUTATED_MEMORY_KEYS, HashSet())
    }

    internal fun processTraversers(
        traversal: PureTraversal<*, *>,
        traversalMatrix: TraversalMatrix<*, *>,
        toProcessTraversers: TraverserSet<Object?>,
        remoteActiveTraversers: TraverserSet<Object?>,
        haltedTraversers: TraverserSet<Object?>,
        haltedTraverserStrategy: HaltedTraverserStrategy
    ) {
        var toProcessTraversers: TraverserSet<Object?> = toProcessTraversers
        while (!toProcessTraversers.isEmpty()) {
            val localActiveTraversers: TraverserSet<Object?> = TraverserSet()
            var previousStep: Step<Object?, Object?> = EmptyStep.instance()
            var currentStep: Step<Object?, Object?> = EmptyStep.instance()

            // these are traversers that are at the master traversal and will either halt here or be distributed back to the workers as needed
            val traversers: Iterator<Traverser.Admin<Object>> = toProcessTraversers.iterator()
            while (traversers.hasNext()) {
                val traverser: Traverser.Admin<Object> = traversers.next()
                traversers.remove()
                traverser.set(DetachedFactory.detach(traverser.get(), true)) // why?
                traverser.setSideEffects(traversal.get().getSideEffects())
                if (traverser.isHalted()) haltedTraversers.add(haltedTraverserStrategy.halt(traverser)) else if (isRemoteTraverser(
                        traverser,
                        traversalMatrix
                    )
                ) // this is so that patterns like order().name work as expected. try and stay local as long as possible
                    remoteActiveTraversers.add(traverser.detach()) else {
                    currentStep = traversalMatrix.getStepById(traverser.getStepId())
                    if (!currentStep.getId().equals(previousStep.getId()) && previousStep !is EmptyStep) {
                        GraphComputing.atMaster(previousStep, true)
                        while (previousStep.hasNext()) {
                            val result: Traverser.Admin<Object> = previousStep.next()
                            if (result.isHalted()) haltedTraversers.add(haltedTraverserStrategy.halt(result)) else if (isRemoteTraverser(
                                    result,
                                    traversalMatrix
                                )
                            ) remoteActiveTraversers.add(result.detach()) else localActiveTraversers.add(result)
                        }
                    }
                    currentStep.addStart(traverser)
                    previousStep = currentStep
                }
            }
            if (currentStep !is EmptyStep) {
                GraphComputing.atMaster(currentStep, true)
                while (currentStep.hasNext()) {
                    val traverser: Traverser.Admin<Object> = currentStep.next()
                    if (traverser.isHalted()) haltedTraversers.add(haltedTraverserStrategy.halt(traverser)) else if (isRemoteTraverser(
                            traverser,
                            traversalMatrix
                        )
                    ) remoteActiveTraversers.add(traverser.detach()) else localActiveTraversers.add(traverser)
                }
            }
            assert(toProcessTraversers.isEmpty())
            toProcessTraversers = localActiveTraversers
        }
    }

    private fun isRemoteTraverser(traverser: Traverser.Admin, traversalMatrix: TraversalMatrix<*, *>): Boolean {
        return traverser.get() is Attachable &&
                traverser.get() !is Path &&
                !isLocalElement(traversalMatrix.getStepById(traverser.getStepId()))
    }

    // TODO: once this is complete (fully known), move to TraversalHelper
    private fun isLocalElement(step: Step<*, *>): Boolean {
        return step is PropertiesStep || step is PropertyMapStep ||
                step is IdStep || step is LabelStep || step is SackStep ||
                step is PropertyKeyStep || step is PropertyValueStep ||
                step is TailGlobalStep || step is RangeGlobalStep || step is HasStep ||
                step is ConnectiveStep
    }
}