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

import org.apache.commons.configuration2.Configuration

/**
 * `TraversalVertexProgram` enables the evaluation of a [Traversal] on a [GraphComputer].
 * At the start of the computation, each [Vertex] (or [Edge]) is assigned a single [Traverser].
 * For each traverser that is local to the vertex, the vertex looks up its current location in the traversal and
 * processes that step. If the outputted traverser of the step references a local structure on the vertex (e.g. the
 * vertex, an incident edge, its properties, or an arbitrary object), then the vertex continues to compute the next
 * traverser. If the traverser references another location in the graph, then the traverser is sent to that location
 * in the graph via a message. The messages of TraversalVertexProgram are traversers. This continues until all
 * traversers in the computation have halted.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class TraversalVertexProgram private constructor() : VertexProgram<TraverserSet<Object?>?> {
    private var memoryComputeKeys: Set<MemoryComputeKey> = HashSet()
    private var traversal: PureTraversal<*, *>? = null
    private var traversalMatrix: TraversalMatrix<*, *>? = null
    private val mapReducers: Set<MapReduce> = HashSet()
    private var haltedTraversers: TraverserSet<Object>? = null
    private var returnHaltedTraversers = false
    private var haltedTraverserStrategy: HaltedTraverserStrategy? = null
    private var profile = false

    // handle current profile metrics if profile is true
    private var iterationMetrics: MutableMetrics? = null

    /**
     * Get the [PureTraversal] associated with the current instance of the [TraversalVertexProgram].
     *
     * @return the pure traversal of the instantiated program
     */
    fun getTraversal(): PureTraversal<*, *> {
        return traversal
    }

    @Override
    fun loadState(graph: Graph?, configuration: Configuration) {
        if (!configuration.containsKey(TRAVERSAL)) throw IllegalArgumentException("The configuration does not have a traversal: " + TRAVERSAL)
        traversal = PureTraversal.loadState(configuration, TRAVERSAL, graph)
        if (!traversal.get().isLocked()) traversal.get().applyStrategies()
        /// traversal is compiled and ready to be introspected
        traversalMatrix = TraversalMatrix(traversal.get())
        // get any master-traversal halted traversers
        haltedTraversers = loadHaltedTraversers<Any>(configuration)
        // if results will be serialized out, don't save halted traversers across the cluster
        returnHaltedTraversers = traversal.get().getParent().asStep()
            .getNextStep() is ComputerResultStep ||  // if its just going to stream it out, don't distribute
                traversal.get().getParent().asStep()
                    .getNextStep() is EmptyStep ||  // same as above, but if using TraversalVertexProgramStep directly
                traversal.get().getParent().asStep()
                    .getNextStep() is ProfileStep &&  // same as above, but needed for profiling
                traversal.get().getParent().asStep().getNextStep().getNextStep() is ComputerResultStep

        // determine how to store halted traversers
        val itty: Iterator<*> =
            IteratorUtils.filter(traversal.get().getStrategies()) { strategy -> strategy is HaltedTraverserStrategy }
                .iterator()
        haltedTraverserStrategy =
            if (itty.hasNext()) itty.next() as HaltedTraverserStrategy? else HaltedTraverserStrategy.reference()

        // register traversal side-effects in memory
        memoryComputeKeys.addAll(MemoryTraversalSideEffects.getMemoryComputeKeys(traversal.get()))
        // register MapReducer memory compute keys
        for (mapReducer in TraversalHelper.getStepsOfAssignableClassRecursively(
            MapReducer::class.java,
            traversal.get()
        )) {
            mapReducers.add(mapReducer.getMapReduce())
            memoryComputeKeys.add(
                MemoryComputeKey.of(
                    mapReducer.getMapReduce().getMemoryKey(),
                    Operator.assign,
                    false,
                    false
                )
            )
        }
        // register memory computing steps that use memory compute keys
        for (memoryComputing in TraversalHelper.getStepsOfAssignableClassRecursively(
            MemoryComputing::class.java, traversal.get()
        )) {
            memoryComputeKeys.add(memoryComputing.getMemoryComputeKey())
        }
        // register profile steps (TODO: try to hide this)
        for (profileStep in TraversalHelper.getStepsOfAssignableClassRecursively(
            ProfileStep::class.java,
            traversal.get()
        )) {
            traversal.get().getSideEffects().register(
                profileStep.getId(),
                MutableMetricsSupplier(profileStep.getPreviousStep()),
                ProfileStep.ProfileBiOperator.instance()
            )
        }
        // register TraversalVertexProgram specific memory compute keys
        memoryComputeKeys.add(MemoryComputeKey.of(VOTE_TO_HALT, Operator.and, false, true))
        memoryComputeKeys.add(MemoryComputeKey.of(HALTED_TRAVERSERS, Operator.addAll, false, false))
        memoryComputeKeys.add(MemoryComputeKey.of(ACTIVE_TRAVERSERS, Operator.addAll, true, true))
        memoryComputeKeys.add(MemoryComputeKey.of(MUTATED_MEMORY_KEYS, Operator.addAll, false, true))
        memoryComputeKeys.add(MemoryComputeKey.of(COMPLETED_BARRIERS, Operator.addAll, true, true))

        // does the traversal need profile information
        profile =
            !TraversalHelper.getStepsOfAssignableClassRecursively(ProfileStep::class.java, traversal.get()).isEmpty()
    }

    @Override
    fun storeState(configuration: Configuration) {
        super@VertexProgram.storeState(configuration)
        traversal.storeState(configuration, TRAVERSAL)
        storeHaltedTraversers<Any>(configuration, haltedTraversers)
    }

    @Override
    fun setup(memory: Memory) {
        // memory is local
        MemoryTraversalSideEffects.setMemorySideEffects(traversal.get(), memory, ProgramPhase.SETUP)
        (traversal.get().getSideEffects() as MemoryTraversalSideEffects).storeSideEffectsInMemory()
        memory.set(VOTE_TO_HALT, true)
        memory.set(MUTATED_MEMORY_KEYS, HashSet())
        memory.set(COMPLETED_BARRIERS, HashSet())
        // if halted traversers are being sent from a previous VertexProgram in an OLAP chain (non-distributed traversers), get them into the flow
        if (!haltedTraversers.isEmpty()) {
            val toProcessTraversers: TraverserSet<Object> = TraverserSet()
            IteratorUtils.removeOnNext(haltedTraversers.iterator()).forEachRemaining { traverser ->
                traverser.setStepId(traversal.get().getStartStep().getId())
                toProcessTraversers.add(traverser)
            }
            assert(haltedTraversers.isEmpty())
            val remoteActiveTraversers: IndexedTraverserSet<Object, Vertex> = VertexIndexedTraverserSet()
            MasterExecutor.processTraversers(
                traversal,
                traversalMatrix,
                toProcessTraversers,
                remoteActiveTraversers,
                haltedTraversers,
                haltedTraverserStrategy
            )
            memory.set(HALTED_TRAVERSERS, haltedTraversers)
            memory.set(ACTIVE_TRAVERSERS, remoteActiveTraversers)
        } else {
            memory.set(HALTED_TRAVERSERS, TraverserSet())
            memory.set(ACTIVE_TRAVERSERS, VertexIndexedTraverserSet())
        }
        // local variable will no longer be used so null it for GC
        haltedTraversers = null
        // does the traversal need profile information
        profile =
            !TraversalHelper.getStepsOfAssignableClassRecursively(ProfileStep::class.java, traversal.get()).isEmpty()
    }

    @Override
    fun getMessageScopes(memory: Memory?): Set<MessageScope> {
        return MESSAGE_SCOPES
    }

    @Override
    fun execute(vertex: Vertex, messenger: Messenger<TraverserSet<Object?>?>?, memory: Memory) {
        // if any global halted traversers, simply don't use them as they were handled by master setup()
        // these halted traversers are typically from a previous OLAP job that yielded traversers at the master traversal
        if (null != haltedTraversers) haltedTraversers = null
        // memory is distributed
        MemoryTraversalSideEffects.setMemorySideEffects(traversal.get(), memory, ProgramPhase.EXECUTE)
        // if a barrier was completed in another worker, it is also completed here (ensure distributed barriers are synchronized)
        val completedBarriers: Set<String> = memory.get(COMPLETED_BARRIERS)
        for (stepId in completedBarriers) {
            val step: Step<*, *> = traversalMatrix.getStepById(stepId)
            if (step is Barrier) (traversalMatrix.getStepById(stepId) as Barrier).done()
        }
        // define halted traversers
        val property: VertexProperty<TraverserSet<Object>> = vertex.property(HALTED_TRAVERSERS)
        val haltedTraversers: TraverserSet<Object>
        if (property.isPresent()) {
            haltedTraversers = property.value()
        } else {
            haltedTraversers = TraverserSet()
            vertex.property(VertexProperty.Cardinality.single, HALTED_TRAVERSERS, haltedTraversers)
        }
        //////////////////
        if (memory.isInitialIteration()) {    // ITERATION 1
            val activeTraversers: TraverserSet<Object> = TraverserSet()
            // if halted traversers are being sent from a previous VertexProgram in an OLAP chain (distributed traversers), get them into the flow
            IteratorUtils.removeOnNext(haltedTraversers.iterator()).forEachRemaining { traverser ->
                traverser.setStepId(traversal.get().getStartStep().getId())
                activeTraversers.add(traverser)
            }
            assert(haltedTraversers.isEmpty())
            // for g.V()/E()
            if (traversal.get().getStartStep() is GraphStep) {
                val graphStep: GraphStep<Element, Element> =
                    traversal.get().getStartStep() as GraphStep<Element, Element>
                graphStep.reset()
                activeTraversers.forEach { traverser -> graphStep.addStart(traverser as Traverser.Admin) }
                activeTraversers.clear()
                if (graphStep.returnsVertex()) graphStep.setIteratorSupplier {
                    if (ElementHelper.idExists(
                            vertex.id(),
                            graphStep.getIds()
                        )
                    ) IteratorUtils.of(vertex) else EmptyIterator.instance()
                } else graphStep.setIteratorSupplier {
                    IteratorUtils.filter(vertex.edges(Direction.OUT)) { edge ->
                        ElementHelper.idExists(
                            edge.id(),
                            graphStep.getIds()
                        )
                    }
                }
                graphStep.forEachRemaining { traverser ->
                    if (traverser.isHalted()) {
                        if (returnHaltedTraversers) memory.add(
                            HALTED_TRAVERSERS, TraverserSet(
                                haltedTraverserStrategy.halt(traverser)
                            )
                        ) else haltedTraversers.add(traverser.detach() as Traverser.Admin)
                    } else activeTraversers.add(traverser as Traverser.Admin)
                }
            }
            memory.add(
                VOTE_TO_HALT,
                activeTraversers.isEmpty() || WorkerExecutor.execute(
                    vertex,
                    SingleMessenger(messenger, activeTraversers),
                    traversalMatrix,
                    memory,
                    returnHaltedTraversers,
                    haltedTraversers,
                    haltedTraverserStrategy
                )
            )
        } else  // ITERATION 1+
            memory.add(
                VOTE_TO_HALT,
                WorkerExecutor.execute(
                    vertex,
                    messenger,
                    traversalMatrix,
                    memory,
                    returnHaltedTraversers,
                    haltedTraversers,
                    haltedTraverserStrategy
                )
            )
        // save space by not having an empty halted traversers property
        if (returnHaltedTraversers || haltedTraversers.isEmpty()) vertex.< TraverserSet > property < TraverserSet ? > HALTED_TRAVERSERS.remove()
    }

    @Override
    fun terminate(memory: Memory): Boolean {
        // memory is local
        MemoryTraversalSideEffects.setMemorySideEffects(traversal.get(), memory, ProgramPhase.TERMINATE)
        val voteToHalt: Boolean = memory.< Boolean > get < Boolean ? > VOTE_TO_HALT
        memory.set(VOTE_TO_HALT, true)
        memory.set(ACTIVE_TRAVERSERS, VertexIndexedTraverserSet())
        return if (voteToHalt) {
            // local traverser sets to process
            val toProcessTraversers: TraverserSet<Object> = TraverserSet()
            // traversers that need to be sent back to the workers (no longer can be processed locally by the master traversal)
            val remoteActiveTraversers: IndexedTraverserSet<Object, Vertex> = VertexIndexedTraverserSet()
            // halted traversers that have completed their journey
            val haltedTraversers: TraverserSet<Object> = memory.get(HALTED_TRAVERSERS)
            // get all barrier traversers
            val completedBarriers: Set<String> = HashSet()
            MasterExecutor.processMemory(traversalMatrix, memory, toProcessTraversers, completedBarriers)
            // process all results from barriers locally and when elements are touched, put them in remoteActiveTraversers
            MasterExecutor.processTraversers(
                traversal,
                traversalMatrix,
                toProcessTraversers,
                remoteActiveTraversers,
                haltedTraversers,
                haltedTraverserStrategy
            )
            // tell parallel barriers that might not have been active in the last round that they are no longer active
            memory.set(COMPLETED_BARRIERS, completedBarriers)
            if (!remoteActiveTraversers.isEmpty() ||
                completedBarriers.stream().map(traversalMatrix::getStepById).filter { step -> step is LocalBarrier }
                    .findAny().isPresent()
            ) {
                // send active traversers back to workers
                memory.set(ACTIVE_TRAVERSERS, remoteActiveTraversers)
                false
            } else {
                // finalize locally with any last traversers dangling in the local traversal
                val endStep: Step<*, Object> = traversal.get().getEndStep() as Step<*, Object>
                while (endStep.hasNext()) {
                    haltedTraversers.add(haltedTraverserStrategy.halt(endStep.next()))
                }
                // the result of a TraversalVertexProgram are the halted traversers
                memory.set(HALTED_TRAVERSERS, haltedTraversers)
                // finalize profile side-effect steps. (todo: try and hide this)
                for (profileStep in TraversalHelper.getStepsOfAssignableClassRecursively(
                    ProfileSideEffectStep::class.java, traversal.get()
                )) {
                    traversal.get().getSideEffects().set(
                        profileStep.getSideEffectKey(), profileStep.generateFinalResult(
                            traversal.get().getSideEffects().get(profileStep.getSideEffectKey())
                        )
                    )
                }
                true
            }
        } else {
            false
        }
    }

    @Override
    fun workerIterationStart(memory: Memory) {
        // start collecting profile metrics
        if (profile) {
            iterationMetrics =
                MutableMetrics("iteration" + memory.getIteration(), "Worker Iteration " + memory.getIteration())
            iterationMetrics.start()
        }
    }

    @Override
    fun workerIterationEnd(memory: Memory) {
        // store profile metrics in proper ProfileStep metrics
        if (profile) {
            val profileSteps: List<ProfileStep> =
                TraversalHelper.getStepsOfAssignableClassRecursively(ProfileStep::class.java, traversal.get())
            // guess the profile step to store data
            var profileStepIndex: Int = memory.getIteration()
            // if we guess wrongly write timing into last step
            profileStepIndex =
                if (profileStepIndex >= profileSteps.size()) profileSteps.size() - 1 else profileStepIndex
            iterationMetrics.finish(0)
            // reset counts
            iterationMetrics.setCount(TraversalMetrics.TRAVERSER_COUNT_ID, 0)
            if (null != MemoryTraversalSideEffects.getMemorySideEffectsPhase(traversal.get())) {
                traversal.get().getSideEffects().add(profileSteps[profileStepIndex].getId(), iterationMetrics)
            }
            iterationMetrics = null
        }
    }

    @get:Override
    val vertexComputeKeys: Set<Any>
        get() = VERTEX_COMPUTE_KEYS

    @Override
    fun getMemoryComputeKeys(): Set<MemoryComputeKey> {
        return memoryComputeKeys
    }

    @Override
    fun getMapReducers(): Set<MapReduce> {
        return mapReducers
    }

    @get:Override
    val messageCombiner: Optional<MessageCombiner<TraverserSet<Object>>>
        get() = TraversalVertexProgramMessageCombiner.instance() as Optional

    @Override
    fun clone(): TraversalVertexProgram {
        return try {
            val clone = super.clone() as TraversalVertexProgram
            clone.traversal = traversal.clone()
            if (!clone.traversal.get().isLocked()) clone.traversal.get().applyStrategies()
            clone.traversalMatrix = TraversalMatrix(clone.traversal.get())
            clone.memoryComputeKeys = HashSet()
            for (memoryComputeKey in memoryComputeKeys) {
                clone.memoryComputeKeys.add(memoryComputeKey.clone())
            }
            clone
        } catch (e: CloneNotSupportedException) {
            throw IllegalStateException(e.getMessage(), e)
        }
    }

    @get:Override
    val preferredResultGraph: ResultGraph
        get() = GraphComputer.ResultGraph.ORIGINAL

    @get:Override
    val preferredPersist: Persist
        get() = GraphComputer.Persist.NOTHING

    @Override
    override fun toString(): String {
        val traversalString: String = traversal.get().toString().substring(1)
        return StringFactory.vertexProgramString(this, traversalString.substring(0, traversalString.length() - 1))
    }

    @get:Override
    val features: Features
        get() = object : Features() {
            @Override
            fun requiresGlobalMessageScopes(): Boolean {
                return true
            }

            @Override
            fun requiresVertexPropertyAddition(): Boolean {
                return true
            }
        }

    class Builder : AbstractVertexProgramBuilder<Builder?>(
        TraversalVertexProgram::class.java
    ) {
        fun haltedTraversers(haltedTraversers: TraverserSet<Object>?): Builder {
            storeHaltedTraversers<Any>(this.configuration, haltedTraversers)
            return this
        }

        fun traversal(
            traversalSource: TraversalSource?,
            scriptEngine: String?,
            traversalScript: String?,
            vararg bindings: Object?
        ): Builder {
            return this.traversal(ScriptTraversal(traversalSource, scriptEngine, traversalScript, bindings))
        }

        fun traversal(traversal: Traversal.Admin<*, *>): Builder {
            // this is necessary if the job was submitted via TraversalVertexProgram.build() instead of TraversalVertexProgramStep.
            var traversal: Traversal.Admin<*, *> = traversal
            if (traversal.getParent() !is TraversalVertexProgramStep) {
                val memoryTraversalSideEffects = MemoryTraversalSideEffects(traversal.getSideEffects())
                val parentTraversal: Traversal.Admin<*, *> = DefaultTraversal()
                traversal.getGraph().ifPresent(parentTraversal::setGraph)
                val strategies: TraversalStrategies = traversal.getStrategies().clone()
                strategies.addStrategies(
                    ComputerFinalizationStrategy.instance(),
                    ComputerVerificationStrategy.instance(),
                    VertexProgramStrategy(Computer.compute())
                )
                parentTraversal.setStrategies(strategies)
                traversal.setStrategies(strategies)
                parentTraversal.setSideEffects(memoryTraversalSideEffects)
                parentTraversal.addStep(TraversalVertexProgramStep(parentTraversal, traversal))
                traversal = (parentTraversal.getStartStep() as TraversalVertexProgramStep).getGlobalChildren().get(0)
                traversal.setSideEffects(memoryTraversalSideEffects)
            }
            PureTraversal.storeState(this.configuration, TRAVERSAL, traversal)
            return this
        }
    }

    companion object {
        const val TRAVERSAL = "gremlin.traversalVertexProgram.traversal"
        const val HALTED_TRAVERSERS = "gremlin.traversalVertexProgram.haltedTraversers"
        const val ACTIVE_TRAVERSERS = "gremlin.traversalVertexProgram.activeTraversers"
        protected const val MUTATED_MEMORY_KEYS = "gremlin.traversalVertexProgram.mutatedMemoryKeys"
        private const val VOTE_TO_HALT = "gremlin.traversalVertexProgram.voteToHalt"
        private const val COMPLETED_BARRIERS = "gremlin.traversalVertexProgram.completedBarriers"

        // TODO: if not an adjacent traversal, use Local message scope -- a dual messaging system.
        private val MESSAGE_SCOPES: Set<MessageScope> =
            HashSet(Collections.singletonList(MessageScope.Global.instance()))
        private val VERTEX_COMPUTE_KEYS: Set<VertexComputeKey> = HashSet(
            Arrays.asList(
                VertexComputeKey.of(
                    HALTED_TRAVERSERS, false
                ), VertexComputeKey.of(ACTIVE_TRAVERSERS, true)
            )
        )

        fun <R> loadHaltedTraversers(configuration: Configuration): TraverserSet<R> {
            if (!configuration.containsKey(HALTED_TRAVERSERS)) return TraverserSet()
            val `object`: Object =
                if (configuration.getProperty(HALTED_TRAVERSERS) is String) VertexProgramHelper.deserialize(
                    configuration,
                    HALTED_TRAVERSERS
                ) else configuration.getProperty(
                    HALTED_TRAVERSERS
                )
            return if (`object` is Traverser.Admin) TraverserSet(`object` as Traverser.Admin<R>) else {
                val traverserSet: TraverserSet<R> = TraverserSet()
                traverserSet.addAll(`object` as Collection)
                traverserSet
            }
        }

        fun <R> storeHaltedTraversers(configuration: Configuration, haltedTraversers: TraverserSet<R>?) {
            if (null != haltedTraversers && !haltedTraversers.isEmpty()) {
                try {
                    VertexProgramHelper.serialize(haltedTraversers, configuration, HALTED_TRAVERSERS)
                } catch (e: Exception) {
                    configuration.setProperty(HALTED_TRAVERSERS, haltedTraversers)
                }
            }
        }

        //////////////
        fun build(): Builder {
            return Builder()
        }
    }
}