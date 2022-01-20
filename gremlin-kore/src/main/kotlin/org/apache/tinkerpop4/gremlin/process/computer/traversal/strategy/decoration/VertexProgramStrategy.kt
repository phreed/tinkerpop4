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
package org.apache.tinkerpop4.gremlin.process.computer.traversal.strategy.decoration

import org.apache.commons.configuration2.Configuration

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class VertexProgramStrategy(computer: Computer?) : AbstractTraversalStrategy<DecorationStrategy?>(),
    DecorationStrategy {
    private val computer: Computer?

    private constructor() : this(null) {}

    @Override
    fun apply(traversal: Traversal.Admin<*, *>) {
        // VertexPrograms can only execute at the root level of a Traversal and should not be applied locally prior to RemoteStrategy
        if (!traversal.isRoot()
            || traversal is AbstractLambdaTraversal
            || traversal.getStrategies().getStrategy(RemoteStrategy::class.java).isPresent()
        ) return

        // back propagate as()-labels off of vertex computing steps
        var currentStep: Step<*, *> = traversal.getEndStep()
        val currentLabels: Set<String> = HashSet()
        while (currentStep !is EmptyStep) {
            if (currentStep is VertexComputing && currentStep !is ProgramVertexProgramStep) {  // todo: is there a general solution?
                currentLabels.addAll(currentStep.getLabels())
                currentStep.getLabels().forEach(currentStep::removeLabel)
            } else {
                currentLabels.forEach(currentStep::addLabel)
                currentLabels.clear()
            }
            currentStep = currentStep.getPreviousStep()
        }

        // push GraphStep forward in the chain to reduce the number of TraversalVertexProgram compilations
        currentStep = traversal.getStartStep()
        while (currentStep !is EmptyStep) {
            if (currentStep is GraphStep && currentStep.getNextStep() is VertexComputing) {
                val index: Int = TraversalHelper.stepIndex(currentStep.getNextStep(), traversal)
                traversal.removeStep(currentStep)
                traversal.addStep(index, currentStep)
            } else currentStep = currentStep.getNextStep()
        }

        // wrap all non-VertexComputing steps into a TraversalVertexProgramStep
        currentStep = traversal.getStartStep()
        while (currentStep !is EmptyStep) {
            val computerTraversal: Traversal.Admin<*, *> = DefaultTraversal()
            val firstLegalOLAPStep: Step<*, *> = getFirstLegalOLAPStep(currentStep)
            val lastLegalOLAPStep: Step<*, *> = getLastLegalOLAPStep(currentStep)
            if (firstLegalOLAPStep !is EmptyStep) {
                val index: Int = TraversalHelper.stepIndex(firstLegalOLAPStep, traversal)
                TraversalHelper.removeToTraversal(
                    firstLegalOLAPStep,
                    lastLegalOLAPStep.getNextStep(),
                    computerTraversal as Traversal.Admin
                )
                val traversalVertexProgramStep = TraversalVertexProgramStep(traversal, computerTraversal)
                traversal.addStep(index, traversalVertexProgramStep)
            }
            currentStep = traversal.getStartStep()
            while (currentStep !is EmptyStep) {
                if (currentStep !is VertexComputing) break
                currentStep = currentStep.getNextStep()
            }
        }

        // if the last vertex computing step is a TraversalVertexProgramStep convert to OLTP with ComputerResultStep
        TraversalHelper.getLastStepOfAssignableClass(VertexComputing::class.java, traversal).ifPresent { step ->
            if (step is TraversalVertexProgramStep) {
                val computerResultStep = ComputerResultStep(traversal)
                (step as TraversalVertexProgramStep).getGlobalChildren().get(0).getEndStep().getLabels()
                    .forEach(computerResultStep::addLabel)
                // labeling should happen in TraversalVertexProgram (perhaps MapReduce)
                TraversalHelper.insertAfterStep(computerResultStep, step as Step, traversal)
            }
        }

        // if there is a dangling vertex computing step, add an identity traversal (solve this in the future with a specialized MapReduce)
        if (traversal.getEndStep() is VertexComputing && traversal.getEndStep() !is TraversalVertexProgramStep) {
            val traversalVertexProgramStep = TraversalVertexProgramStep(traversal, __.identity().asAdmin())
            traversal.addStep(traversalVertexProgramStep)
            traversal.addStep(ComputerResultStep(traversal))
        }
        // all vertex computing steps needs the graph computer function
        traversal.getSteps().stream().filter { step -> step is VertexComputing }.forEach { step ->
            (step as VertexComputing).setComputer(
                computer
            )
        }
    }

    fun addGraphComputerStrategies(traversalSource: TraversalSource) {
        val graphComputerClass: Class<out GraphComputer?>
        graphComputerClass = if (computer.getGraphComputerClass().equals(GraphComputer::class.java)) {
            try {
                computer.apply(traversalSource.getGraph()).getClass()
            } catch (e: Exception) {
                GraphComputer::class.java
            }
        } else computer.getGraphComputerClass()
        val graphComputerStrategies: List<TraversalStrategy<*>> =
            TraversalStrategies.GlobalCache.getStrategies(graphComputerClass).toList()
        traversalSource.getStrategies()
            .addStrategies(graphComputerStrategies.toArray(arrayOfNulls<TraversalStrategy>(graphComputerStrategies.size())))
    }

    init {
        this.computer = computer
    }

    @get:Override
    val configuration: Configuration
        get() {
            val map: Map<String, Object> = HashMap()
            map.put(GRAPH_COMPUTER, computer.getGraphComputerClass().getCanonicalName())
            if (-1 != computer.getWorkers()) map.put(WORKERS, computer.getWorkers())
            if (null != computer.getPersist()) map.put(PERSIST, computer.getPersist().name())
            if (null != computer.getResultGraph()) map.put(RESULT, computer.getResultGraph().name())
            if (null != computer.getVertices()) map.put(VERTICES, computer.getVertices())
            if (null != computer.getEdges()) map.put(EDGES, computer.getEdges())
            map.putAll(computer.getConfiguration())
            return MapConfiguration(map)
        }

    class Builder private constructor() {
        private var computer: Computer = Computer.compute()
        fun computer(computer: Computer): Builder {
            this.computer = computer
            return this
        }

        fun graphComputer(graphComputerClass: Class<out GraphComputer?>?): Builder {
            computer = computer.graphComputer(graphComputerClass)
            return this
        }

        fun configure(key: String?, value: Object?): Builder {
            computer = computer.configure(key, value)
            return this
        }

        fun configure(configurations: Map<String?, Object?>?): Builder {
            computer = computer.configure(configurations)
            return this
        }

        fun workers(workers: Int): Builder {
            computer = computer.workers(workers)
            return this
        }

        fun persist(persist: Persist?): Builder {
            computer = computer.persist(persist)
            return this
        }

        fun result(resultGraph: ResultGraph?): Builder {
            computer = computer.result(resultGraph)
            return this
        }

        fun vertices(vertices: Traversal<Vertex?, Vertex?>?): Builder {
            computer = computer.vertices(vertices)
            return this
        }

        fun edges(edges: Traversal<Vertex?, Edge?>?): Builder {
            computer = computer.edges(edges)
            return this
        }

        fun create(): VertexProgramStrategy {
            return VertexProgramStrategy(computer)
        }
    }

    companion object {
        private val INSTANCE = VertexProgramStrategy(Computer.compute())
        private fun getFirstLegalOLAPStep(currentStep: Step<*, *>): Step<*, *> {
            var currentStep: Step<*, *> = currentStep
            while (currentStep !is EmptyStep) {
                if (currentStep !is VertexComputing) return currentStep
                currentStep = currentStep.getNextStep()
            }
            return EmptyStep.instance()
        }

        private fun getLastLegalOLAPStep(currentStep: Step<*, *>): Step<*, *> {
            var currentStep: Step<*, *> = currentStep
            while (currentStep is VertexComputing) currentStep = currentStep.getNextStep()
            while (currentStep !is EmptyStep) {
                if (currentStep is VertexComputing) return currentStep.getPreviousStep()
                currentStep = currentStep.getNextStep()
            }
            return EmptyStep.instance()
        }

        fun getComputer(strategies: TraversalStrategies): Optional<Computer> {
            val optional: Optional<VertexProgramStrategy> = strategies.getStrategy(VertexProgramStrategy::class.java)
            return if (optional.isPresent()) Optional.of(optional.get().computer) else Optional.empty()
        }

        fun instance(): VertexProgramStrategy {
            return INSTANCE
        }

        ////////////////////////////////////////////////////////////
        const val GRAPH_COMPUTER = "graphComputer"
        const val WORKERS = "workers"
        const val PERSIST = "persist"
        const val RESULT = "result"
        const val VERTICES = "vertices"
        const val EDGES = "edges"
        fun create(configuration: Configuration): VertexProgramStrategy {
            return try {
                val builder = build()
                for (key in IteratorUtils.asList(configuration.getKeys())) {
                    if (key.equals(GRAPH_COMPUTER)) builder.graphComputer(Class.forName(configuration.getString(key)) as Class) else if (key.equals(
                            WORKERS
                        )
                    ) builder.workers(configuration.getInt(key)) else if (key.equals(PERSIST)) builder.persist(
                        GraphComputer.Persist.valueOf(configuration.getString(key))
                    ) else if (key.equals(
                            RESULT
                        )
                    ) builder.result(GraphComputer.ResultGraph.valueOf(configuration.getString(key))) else if (key.equals(
                            VERTICES
                        )
                    ) builder.vertices(configuration.getProperty(key) as Traversal) else if (key.equals(EDGES)) builder.edges(
                        configuration.getProperty(key) as Traversal
                    ) else builder.configure(key, configuration.getProperty(key))
                }
                builder.create()
            } catch (e: ClassNotFoundException) {
                throw IllegalArgumentException(e.getMessage(), e)
            }
        }

        fun build(): Builder {
            return Builder()
        }
    }
}