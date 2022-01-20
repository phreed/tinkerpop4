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
package org.apache.tinkerpop4.gremlin.process.computer.traversal.step.map

import org.apache.tinkerpop4.gremlin.process.computer.Computer

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
abstract class VertexProgramStep(traversal: Traversal.Admin?) :
    AbstractStep<ComputerResult?, ComputerResult?>(traversal), VertexComputing {
    protected var computer: Computer = Computer.compute()
    protected var first = true
    @Override
    @Throws(NoSuchElementException::class)
    protected fun processNextStart(): Traverser.Admin<ComputerResult> {
        var future: Future<ComputerResult?>? = null
        return try {
            if (first && this.getPreviousStep() is EmptyStep) {
                first = false
                val graph: Graph = this.getTraversal().getGraph().get()
                future =
                    getComputer().apply(graph).program(this.generateProgram(graph, EmptyMemory.instance())).submit()
                val result: ComputerResult = future.get()
                processMemorySideEffects(result.memory())
                this.getTraversal().getTraverserGenerator().generate(result, this, 1L)
            } else {
                val traverser: Traverser.Admin<ComputerResult> = this.starts.next()
                val graph: Graph = traverser.get().graph()
                val memory: Memory = traverser.get().memory()
                future = getComputer().apply(graph).program(this.generateProgram(graph, memory)).submit()
                val result: ComputerResult = future.get()
                processMemorySideEffects(result.memory())
                traverser.split(result, this)
            }
        } catch (ie: InterruptedException) {
            // the thread running the traversal took an interruption while waiting on the call the future.get().
            // the future should then be cancelled with interruption so that the GraphComputer that created
            // the future knows we don't care about it anymore. The GraphComputer should attempt to respect this
            // cancellation request.
            if (future != null) future.cancel(true)
            throw TraversalInterruptedException()
        } catch (e: ExecutionException) {
            throw IllegalStateException(e.getMessage(), e)
        }
    }

    @Override
    fun getComputer(): Computer {
        var tempComputer: Computer = computer
        if (!isEndStep) {
            if (null == tempComputer.getPersist()) tempComputer = tempComputer.persist(GraphComputer.Persist.EDGES)
            if (null == tempComputer.getResultGraph()) tempComputer = tempComputer.result(GraphComputer.ResultGraph.NEW)
        }
        return tempComputer
    }

    @Override
    fun setComputer(computer: Computer) {
        this.computer = computer
    }

    protected fun previousTraversalVertexProgram(): Boolean {
        var currentStep: Step<*, *> = this
        while (currentStep !is EmptyStep) {
            if (currentStep is TraversalVertexProgramStep) return true
            currentStep = currentStep.getPreviousStep()
        }
        return false
    }

    private fun processMemorySideEffects(memory: Memory) {
        // update the traversal side-effects with the state of the memory after the OLAP job execution
        val sideEffects: TraversalSideEffects = this.getTraversal().getSideEffects()
        for (key in memory.keys()) {
            if (sideEffects.exists(key)) {
                // halted traversers should never be propagated through sideEffects
                assert(!key.equals(TraversalVertexProgram.HALTED_TRAVERSERS))
                sideEffects.set(key, memory.get(key))
            }
        }
    }

    protected val isEndStep: Boolean
        protected get() = this.getNextStep() is ComputerResultStep || this.getNextStep() is ProfileStep && this.getNextStep()
            .getNextStep() is ComputerResultStep

    companion object {
        const val ROOT_TRAVERSAL = "gremlin.vertexProgramStep.rootTraversal"
        const val STEP_ID = "gremlin.vertexProgramStep.stepId"
    }
}