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
internal object WorkerExecutor {
    internal fun execute(
        vertex: Vertex,
        messenger: Messenger<TraverserSet<Object?>?>,
        traversalMatrix: TraversalMatrix<*, *>,
        memory: Memory,
        returnHaltedTraversers: Boolean,
        haltedTraversers: TraverserSet<Object>,
        haltedTraverserStrategy: HaltedTraverserStrategy
    ): Boolean {
        val traversalSideEffects: TraversalSideEffects = traversalMatrix.getTraversal().getSideEffects()
        val voteToHalt = AtomicBoolean(true)
        val activeTraversers: TraverserSet<Object> = TraverserSet()
        val toProcessTraversers: TraverserSet<Object> = TraverserSet()

        ////////////////////////////////
        // GENERATE LOCAL TRAVERSERS //
        ///////////////////////////////

        // MASTER ACTIVE
        // these are traversers that are going from OLTP (master) to OLAP (workers)
        // these traversers were broadcasted from the master traversal to the workers for attachment
        val maybeActiveTraversers: IndexedTraverserSet<Object, Vertex> =
            memory.get(TraversalVertexProgram.ACTIVE_TRAVERSERS)
        // some memory systems are interacted with by multiple threads and thus, concurrent modification can happen at iterator.remove().
        // its better to reduce the memory footprint and shorten the active traverser list so synchronization is worth it.
        // most distributed OLAP systems have the memory partitioned and thus, this synchronization does nothing.
        synchronized(maybeActiveTraversers) {
            if (!maybeActiveTraversers.isEmpty()) {
                val traversers: Collection<Traverser.Admin<Object>> = maybeActiveTraversers.get(vertex)
                if (traversers != null) {
                    val iterator: Iterator<Traverser.Admin<Object>> = traversers.iterator()
                    while (iterator.hasNext()) {
                        val traverser: Traverser.Admin<Object> = iterator.next()
                        iterator.remove()
                        maybeActiveTraversers.remove(traverser)
                        traverser.attach(Attachable.Method.get(vertex))
                        traverser.setSideEffects(traversalSideEffects)
                        toProcessTraversers.add(traverser)
                    }
                }
            }
        }

        // WORKER ACTIVE
        // these are traversers that exist from from a local barrier
        // these traversers will simply saved at the local vertex while the master traversal synchronized the barrier
        vertex.< TraverserSet < Object > > property<TraverserSet<Object>>(TraversalVertexProgram.ACTIVE_TRAVERSERS).ifPresent { previousActiveTraversers ->
            IteratorUtils.removeOnNext(previousActiveTraversers.iterator()).forEachRemaining { traverser ->
                traverser.attach(Attachable.Method.get(vertex))
                traverser.setSideEffects(traversalSideEffects)
                toProcessTraversers.add(traverser)
            }
            assert(previousActiveTraversers.isEmpty())
            // remove the property to save space
            vertex.property(TraversalVertexProgram.ACTIVE_TRAVERSERS).remove()
        }

        // TRAVERSER MESSAGES (WORKER -> WORKER)
        // these are traversers that have been messaged to the vertex from another vertex
        val messages: Iterator<TraverserSet<Object>> = messenger.receiveMessages()
        while (messages.hasNext()) {
            IteratorUtils.removeOnNext(messages.next().iterator()).forEachRemaining { traverser ->
                if (traverser.isHalted()) {
                    if (returnHaltedTraversers) memory.add(
                        TraversalVertexProgram.HALTED_TRAVERSERS,
                        TraverserSet(haltedTraverserStrategy.halt(traverser))
                    ) else haltedTraversers.add(traverser) // the traverser has already been detached so no need to detach it again
                } else {
                    // traverser is not halted and thus, should be processed locally
                    // attach it and process
                    traverser.attach(Attachable.Method.get(vertex))
                    traverser.setSideEffects(traversalSideEffects)
                    toProcessTraversers.add(traverser)
                }
            }
        }

        ///////////////////////////////
        // PROCESS LOCAL TRAVERSERS //
        //////////////////////////////

        // while there are still local traversers, process them until they leave the vertex (message pass) or halt (store).
        while (!toProcessTraversers.isEmpty()) {
            var previousStep: Step<Object, Object> = EmptyStep.instance()
            var traversers: Iterator<Traverser.Admin<Object?>> = toProcessTraversers.iterator()
            while (traversers.hasNext()) {
                val traverser: Traverser.Admin<Object?> = traversers.next()
                traversers.remove()
                val currentStep: Step<Object, Object> = traversalMatrix.getStepById(traverser.getStepId())
                // try and fill up the current step as much as possible with traversers to get a bulking optimization
                if (!currentStep.getId().equals(previousStep.getId()) && previousStep !is EmptyStep) drainStep(
                    vertex,
                    previousStep,
                    activeTraversers,
                    haltedTraversers,
                    memory,
                    returnHaltedTraversers,
                    haltedTraverserStrategy
                )
                currentStep.addStart(traverser)
                previousStep = currentStep
            }
            drainStep(
                vertex,
                previousStep,
                activeTraversers,
                haltedTraversers,
                memory,
                returnHaltedTraversers,
                haltedTraverserStrategy
            )
            assert(toProcessTraversers.isEmpty())
            // process all the local objects and send messages or store locally again
            if (!activeTraversers.isEmpty()) {
                traversers = activeTraversers.iterator()
                while (traversers.hasNext()) {
                    val traverser: Traverser.Admin<Object?> = traversers.next()
                    traversers.remove()
                    // decide whether to message the traverser or to process it locally
                    if (traverser.get() is Element || traverser.get() is Property) {      // GRAPH OBJECT
                        // if the element is remote, then message, else store it locally for re-processing
                        val hostingVertex: Vertex = Host.getHostingVertex(traverser.get())
                        if (!vertex.equals(hostingVertex)) { // if its host is not the current vertex, then send the traverser to the hosting vertex
                            voteToHalt.set(false) // if message is passed, then don't vote to halt
                            messenger.sendMessage(
                                MessageScope.Global.of(hostingVertex),
                                TraverserSet(traverser.detach())
                            )
                        } else {
                            traverser.attach(Attachable.Method.get(vertex)) // necessary for select() steps that reference the current object
                            toProcessTraversers.add(traverser)
                        }
                    } else  // STANDARD OBJECT
                        toProcessTraversers.add(traverser)
                }
                assert(activeTraversers.isEmpty())
            }
        }
        return voteToHalt.get()
    }

    private fun drainStep(
        vertex: Vertex,
        step: Step<Object, Object>,
        activeTraversers: TraverserSet<Object>,
        haltedTraversers: TraverserSet<Object>,
        memory: Memory,
        returnHaltedTraversers: Boolean,
        haltedTraverserStrategy: HaltedTraverserStrategy
    ) {
        GraphComputing.atMaster(step, false)
        if (step is Barrier) {
            if (step is Bypassing) (step as Bypassing).setBypass(true)
            if (step is LocalBarrier) {
                // local barrier traversers are stored on the vertex until the master traversal synchronizes the system
                val barrier: LocalBarrier<Object> = step as LocalBarrier<Object>
                val localBarrierTraversers: TraverserSet<Object> =
                    vertex.< TraverserSet < Object > > property<TraverserSet<Object>>(TraversalVertexProgram.ACTIVE_TRAVERSERS).orElse(
                        TraverserSet()
                    )
                vertex.property(TraversalVertexProgram.ACTIVE_TRAVERSERS, localBarrierTraversers)
                while (barrier.hasNextBarrier()) {
                    val barrierSet: TraverserSet<Object> = barrier.nextBarrier()
                    IteratorUtils.removeOnNext(barrierSet.iterator()).forEachRemaining { traverser ->
                        traverser.addLabels(step.getLabels()) // this might need to be generalized for working with global barriers too
                        if (traverser.isHalted() &&
                            (returnHaltedTraversers ||
                                    traverser.get() !is Element && traverser.get() !is Property ||
                                    Host.getHostingVertex(traverser.get()).equals(vertex))
                        ) {
                            if (returnHaltedTraversers) memory.add(
                                TraversalVertexProgram.HALTED_TRAVERSERS,
                                TraverserSet(haltedTraverserStrategy.halt(traverser))
                            ) else haltedTraversers.add(traverser.detach())
                        } else localBarrierTraversers.add(traverser.detach())
                    }
                }
                memory.add(TraversalVertexProgram.MUTATED_MEMORY_KEYS, HashSet(Collections.singleton(step.getId())))
            } else {
                val barrier: Barrier = step as Barrier
                while (barrier.hasNextBarrier()) {
                    memory.add(step.getId(), barrier.nextBarrier())
                }
                memory.add(TraversalVertexProgram.MUTATED_MEMORY_KEYS, HashSet(Collections.singleton(step.getId())))
            }
        } else { // LOCAL PROCESSING
            step.forEachRemaining { traverser ->
                if (traverser.isHalted() &&  // if its a ReferenceFactory (one less iteration required)
                    ((returnHaltedTraversers || ReferenceFactory::class.java === haltedTraverserStrategy.getHaltedTraverserFactory()) &&
                            traverser.get() !is Element && traverser.get() !is Property ||
                            Host.getHostingVertex(traverser.get()).equals(vertex))
                ) {
                    if (returnHaltedTraversers) memory.add(
                        TraversalVertexProgram.HALTED_TRAVERSERS,
                        TraverserSet(haltedTraverserStrategy.halt(traverser))
                    ) else haltedTraversers.add(traverser.detach())
                } else {
                    activeTraversers.add(traverser)
                }
            }
        }
    }
}