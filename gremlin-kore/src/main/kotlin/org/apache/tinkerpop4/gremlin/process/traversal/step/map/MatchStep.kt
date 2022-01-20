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
package org.apache.tinkerpop4.gremlin.process.traversal.step.map

import org.apache.tinkerpop4.gremlin.process.traversal.Path

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
class MatchStep<S, E>(traversal: Traversal.Admin?, connective: Connective, vararg matchTraversals: Traversal?) :
    ComputerAwareStep<S, Map<String?, E>?>(traversal), TraversalParent, Scoping, PathProcessor {
    enum class TraversalType {
        WHERE_PREDICATE, WHERE_TRAVERSAL, MATCH_TRAVERSAL
    }

    private var matchTraversals: List<Traversal.Admin<Object, Object>>
    private var first = true
    val matchStartLabels: Set<String> = HashSet()
    val matchEndLabels: Set<String> = HashSet()

    @get:Override
    var scopeKeys: Set<String>? = null
        get() {
            if (null == field) {
                field = HashSet()
                matchTraversals.forEach { traversal ->
                    if (traversal.getStartStep() is Scoping) field.addAll((traversal.getStartStep() as Scoping).getScopeKeys())
                    if (traversal.getEndStep() is Scoping) field.addAll((traversal.getEndStep() as Scoping).getScopeKeys())
                }
                field.removeAll(matchEndLabels)
                field.remove(computedStartLabel)
                field = Collections.unmodifiableSet(field)
            }
            return field
        }
        private set
    private val connective: Connective
    private val computedStartLabel: String
    private var matchAlgorithm: MatchAlgorithm? = null
    private var matchAlgorithmClass: Class<out MatchAlgorithm> =
        CountMatchAlgorithm::class.java // default is CountMatchAlgorithm (use MatchAlgorithmStrategy to change)
    private var referencedLabelsMap // memoization of referenced labels for MatchEndSteps (Map<startStepId, referencedLabels>)
            : Map<String, Set<String>>? = null
        private get() {
            if (null == field) {
                field = HashMap()
                for (traversal in matchTraversals) {
                    val referencedLabels: Set<String> = HashSet()
                    for (step in traversal.getSteps()) {
                        referencedLabels.addAll(PathUtil.getReferencedLabels(step))
                    }
                    field.put(traversal.getStartStep().getId(), referencedLabels)
                }
            }
            return field
        }
    private var dedups: Set<List<Object>>? = null
    private var dedupLabels: Set<String>? = null
    private var keepLabels: Set<String>? = null

    //////////////////
    private fun pullOutVariableStartStepToParent(whereStep: WhereTraversalStep<*>): String? {
        return if (this.pullOutVariableStartStepToParent(HashSet(), whereStep.getLocalChildren().get(0), true)
                .size() !== 1
        ) null else pullOutVariableStartStepToParent(HashSet(), whereStep.getLocalChildren().get(0), false).iterator()
            .next()
    }

    private fun pullOutVariableStartStepToParent(
        selectKeys: Set<String>,
        traversal: Traversal.Admin<*, *>,
        testRun: Boolean
    ): Set<String> {
        val startStep: Step<*, *> = traversal.getStartStep()
        if (startStep is WhereStartStep && !(startStep as WhereStartStep).getScopeKeys().isEmpty()) {
            selectKeys.addAll((startStep as WhereStartStep<*>).getScopeKeys())
            if (!testRun) (startStep as WhereStartStep).removeScopeKey()
        } else if (startStep is ConnectiveStep || startStep is NotStep) {
            (startStep as TraversalParent).getLocalChildren()
                .forEach { child -> this.pullOutVariableStartStepToParent(selectKeys, child, testRun) }
        }
        return selectKeys
    }

    //////////////////
    private fun configureStartAndEndSteps(matchTraversal: Traversal.Admin<*, *>) {
        ConnectiveStrategy.instance().apply(matchTraversal)
        // START STEP to MatchStep OR MatchStartStep
        val startStep: Step<*, *> = matchTraversal.getStartStep()
        if (startStep is ConnectiveStep) {
            val matchStep: MatchStep<*, *> = MatchStep<Any?, Any?>(
                matchTraversal,
                if (startStep is AndStep) ConnectiveStep.Connective.AND else ConnectiveStep.Connective.OR,
                (startStep as ConnectiveStep<*>).getLocalChildren()
                    .toArray(arrayOfNulls<Traversal>((startStep as ConnectiveStep<*>).getLocalChildren().size()))
            )
            TraversalHelper.replaceStep(startStep, matchStep, matchTraversal)
            matchStartLabels.addAll(matchStep.matchStartLabels)
            matchEndLabels.addAll(matchStep.matchEndLabels)
        } else if (startStep is NotStep) {
            val notTraversal = DefaultTraversal()
            TraversalHelper.removeToTraversal(startStep, startStep.getNextStep(), notTraversal)
            matchTraversal.addStep(0, WhereTraversalStep(matchTraversal, notTraversal))
            configureStartAndEndSteps(matchTraversal)
        } else if (StartStep.isVariableStartStep(startStep)) {
            val label: String = startStep.getLabels().iterator().next()
            matchStartLabels.add(label)
            TraversalHelper.replaceStep(
                matchTraversal.getStartStep() as Step,
                MatchStartStep(matchTraversal, label),
                matchTraversal
            )
        } else if (startStep is WhereTraversalStep) {  // necessary for GraphComputer so the projection is not select'd from a path
            val whereStep: WhereTraversalStep<*> = startStep as WhereTraversalStep<*>
            TraversalHelper.insertBeforeStep(
                MatchStartStep(
                    matchTraversal,
                    this.pullOutVariableStartStepToParent(whereStep)
                ), whereStep as Step, matchTraversal
            ) // where(as('a').out()) -> as('a').where(out())
        } else if (startStep is WherePredicateStep) {  // necessary for GraphComputer so the projection is not select'd from a path
            val whereStep: WherePredicateStep<*> = startStep as WherePredicateStep<*>
            TraversalHelper.insertBeforeStep(
                MatchStartStep(matchTraversal, whereStep.getStartKey().orElse(null)),
                whereStep as Step,
                matchTraversal
            ) // where('a',eq('b')) --> as('a').where(eq('b'))
            whereStep.removeStartKey()
        } else {
            throw IllegalArgumentException("All match()-traversals must have a single start label (i.e. variable): $matchTraversal")
        }
        // END STEP to MatchEndStep
        val endStep: Step<*, *> = matchTraversal.getEndStep()
        if (endStep.getLabels()
                .size() > 1
        ) throw IllegalArgumentException("The end step of a match()-traversal can have at most one label: $endStep")
        val label: String? = if (endStep.getLabels().size() === 0) null else endStep.getLabels().iterator().next()
        if (null != label) endStep.removeLabel(label)
        val matchEndStep: Step<*, *> = MatchEndStep(matchTraversal, label)
        if (null != label) matchEndLabels.add(label)
        matchTraversal.asAdmin().addStep(matchEndStep)

        // this turns barrier computations into locally computable traversals
        if (TraversalHelper.getStepsOfAssignableClass(Barrier::class.java, matchTraversal).stream()
                .filter { s -> s !is NoOpBarrierStep }
                .findAny().isPresent()
        ) { // exclude NoOpBarrierSteps from the determination as they are optimization barriers
            val newTraversal: Traversal.Admin = DefaultTraversal()
            TraversalHelper.removeToTraversal(
                matchTraversal.getStartStep().getNextStep(),
                matchTraversal.getEndStep(),
                newTraversal
            )
            TraversalHelper.insertAfterStep(
                TraversalFlatMapStep(matchTraversal, newTraversal),
                matchTraversal.getStartStep(),
                matchTraversal
            )
        }
    }

    fun getConnective(): Connective {
        return connective
    }

    fun addGlobalChild(globalChildTraversal: Traversal.Admin<*, *>) {
        configureStartAndEndSteps(globalChildTraversal)
        matchTraversals.add(this.integrateChild(globalChildTraversal))
    }

    @Override
    fun removeGlobalChild(globalChildTraversal: Traversal.Admin<*, *>?) {
        matchTraversals.remove(globalChildTraversal)
    }

    @get:Override
    val globalChildren: List<Any>
        get() = Collections.unmodifiableList(matchTraversals)

    @Override
    fun setKeepLabels(keepLabels: Set<String?>?) {
        this.keepLabels = HashSet(keepLabels)
        if (null != dedupLabels) this.keepLabels.addAll(dedupLabels)
    }

    @Override
    fun getKeepLabels(): Set<String>? {
        return keepLabels
    }

    @Override
    override fun toString(): String {
        return StringFactory.stepString(this, dedupLabels, connective, matchTraversals)
    }

    @Override
    fun reset() {
        super.reset()
        first = true
    }

    fun setMatchAlgorithm(matchAlgorithmClass: Class<out MatchAlgorithm?>) {
        this.matchAlgorithmClass = matchAlgorithmClass
    }

    fun getMatchAlgorithm(): MatchAlgorithm? {
        if (null == matchAlgorithm) initializeMatchAlgorithm(this.traverserStepIdAndLabelsSetByChild)
        return matchAlgorithm
    }

    @Override
    fun clone(): MatchStep<S, E> {
        val clone = super.clone() as MatchStep<S, E>
        clone.matchTraversals = ArrayList()
        for (traversal in matchTraversals) {
            clone.matchTraversals.add(traversal.clone())
        }
        if (dedups != null) clone.dedups = HashSet()
        clone.standardAlgorithmBarrier = this.traversal.getTraverserSetSupplier().get() as TraverserSet<S>
        return clone
    }

    @Override
    fun setTraversal(parentTraversal: Traversal.Admin<*, *>?) {
        super.setTraversal(parentTraversal)
        for (traversal in matchTraversals) {
            this.integrateChild(traversal)
        }
    }

    fun setDedupLabels(labels: Set<String?>) {
        if (!labels.isEmpty()) {
            dedups = HashSet()
            dedupLabels = HashSet(labels)
            if (null != keepLabels) keepLabels.addAll(dedupLabels)
        }
    }

    /*public boolean isDeduping() {
        return this.dedupLabels != null;
    }*/
    private fun isDuplicate(traverser: Traverser<S>): Boolean {
        if (null == dedups) return false
        val path: Path = traverser.path()
        for (label in dedupLabels!!) {
            if (!path.hasLabel(label)) return false
        }
        val objects: List<Object> = ArrayList(dedupLabels!!.size())
        for (label in dedupLabels!!) {
            objects.add(path.get(Pop.last, label))
        }
        return dedups!!.contains(objects)
    }

    private fun hasMatched(connective: Connective, traverser: Traverser.Admin<S>): Boolean {
        var counter = 0
        var matched = false
        for (matchTraversal in matchTraversals) {
            if (traverser.getTags().contains(matchTraversal.getStartStep().getId())) {
                if (connective === ConnectiveStep.Connective.OR) {
                    matched = true
                    break
                }
                counter++
            }
        }
        if (!matched) matched = matchTraversals.size() === counter
        if (matched && dedupLabels != null) {
            val path: Path = traverser.path()
            val objects: List<Object> = ArrayList(dedupLabels!!.size())
            for (label in dedupLabels!!) {
                objects.add(path.get(Pop.last, label))
            }
            dedups.add(objects)
        }
        return matched
    }

    private fun getBindings(traverser: Traverser<S>): Map<String, E> {
        val bindings: Map<String, E> = HashMap()
        traverser.path().forEach { `object`, labels ->
            for (label in labels) {
                if (matchStartLabels.contains(label) || matchEndLabels.contains(label)) bindings.put(
                    label,
                    `object` as E
                )
            }
        }
        return bindings
    }

    private fun initializeMatchAlgorithm(onComputer: Boolean) {
        try {
            matchAlgorithm = matchAlgorithmClass.getConstructor().newInstance()
        } catch (e: NoSuchMethodException) {
            throw IllegalStateException(e.getMessage(), e)
        } catch (e: IllegalAccessException) {
            throw IllegalStateException(e.getMessage(), e)
        } catch (e: InvocationTargetException) {
            throw IllegalStateException(e.getMessage(), e)
        } catch (e: InstantiationException) {
            throw IllegalStateException(e.getMessage(), e)
        }
        matchAlgorithm!!.initialize(onComputer, matchTraversals)
    }

    private fun hasPathLabel(path: Path, labels: Set<String>): Boolean {
        for (label in labels) {
            if (path.hasLabel(label)) return true
        }
        return false
    }

    private var standardAlgorithmBarrier: TraverserSet

    init {
        this.connective = connective
        this.matchTraversals = Stream.of(matchTraversals).map(Traversal::asAdmin).collect(Collectors.toList())
        this.matchTraversals.forEach { matchTraversal: Traversal.Admin<*, *> -> configureStartAndEndSteps(matchTraversal) } // recursively convert to MatchStep, MatchStartStep, or MatchEndStep
        this.matchTraversals.forEach(this::integrateChild)
        standardAlgorithmBarrier = traversal.getTraverserSetSupplier().get() as TraverserSet<S>
        computedStartLabel = Helper.computeStartLabel(this.matchTraversals)
    }

    @Override
    @Throws(NoSuchElementException::class)
    protected fun standardAlgorithm(): Iterator<Traverser.Admin<Map<String, E>>> {
        while (true) {
            if (first) {
                first = false
                initializeMatchAlgorithm(false)
                if (null != keepLabels &&
                    keepLabels!!.containsAll(matchEndLabels) &&
                    keepLabels!!.containsAll(matchStartLabels)
                ) keepLabels = null
            } else { // TODO: if(standardAlgorithmBarrier.isEmpty()) -- leads to consistent counts without retracting paths, but orders of magnitude slower (or make Traverser.tags an equality concept)
                var stop = false
                for (matchTraversal in matchTraversals) {
                    while (matchTraversal.hasNext()) { // TODO: perhaps make MatchStep a LocalBarrierStep ??
                        standardAlgorithmBarrier.add(matchTraversal.nextTraverser())
                        if (null == keepLabels || standardAlgorithmBarrier.size() >= PathRetractionStrategy.MAX_BARRIER_SIZE) {
                            stop = true
                            break
                        }
                    }
                    if (stop) break
                }
            }
            val traverser: Traverser.Admin
            if (standardAlgorithmBarrier.isEmpty()) {
                traverser = this.starts.next()
                if (!traverser.getTags().contains(this.getId())) {
                    traverser.getTags().add(this.getId()) // so the traverser never returns to this branch ever again
                    if (!hasPathLabel(traverser.path(), matchStartLabels)) traverser.addLabels(
                        Collections.singleton(
                            computedStartLabel
                        )
                    ) // if the traverser doesn't have a legal start, then provide it the pre-computed one
                }
            } else traverser = standardAlgorithmBarrier.remove()

            ///
            if (!isDuplicate(traverser)) {
                if (hasMatched(connective, traverser)) return IteratorUtils.of(
                    traverser.split(
                        getBindings(traverser),
                        this
                    )
                )
                if (connective === ConnectiveStep.Connective.AND) {
                    val matchTraversal: Traversal.Admin<Object, Object> = getMatchAlgorithm().apply(traverser)
                    traverser.getTags().add(matchTraversal.getStartStep().getId())
                    matchTraversal.addStart(traverser) // determine which sub-pattern the traverser should try next
                } else {  // OR
                    for (matchTraversal in matchTraversals) {
                        val split: Traverser.Admin = traverser.split()
                        split.getTags().add(matchTraversal.getStartStep().getId())
                        matchTraversal.addStart(split)
                    }
                }
            }
        }
    }

    @Override
    @Throws(NoSuchElementException::class)
    protected fun computerAlgorithm(): Iterator<Traverser.Admin<Map<String, E>>> {
        while (true) {
            if (first) {
                first = false
                initializeMatchAlgorithm(true)
                if (null != keepLabels &&
                    keepLabels!!.containsAll(matchEndLabels) &&
                    keepLabels!!.containsAll(matchStartLabels)
                ) keepLabels = null
            }
            val traverser: Traverser.Admin = this.starts.next()
            if (!traverser.getTags().contains(this.getId())) {
                traverser.getTags().add(this.getId()) // so the traverser never returns to this branch ever again
                if (!hasPathLabel(traverser.path(), matchStartLabels)) traverser.addLabels(
                    Collections.singleton(
                        computedStartLabel
                    )
                ) // if the traverser doesn't have a legal start, then provide it the pre-computed one
            }
            ///
            if (!isDuplicate(traverser)) {
                if (hasMatched(connective, traverser)) {
                    traverser.setStepId(this.getNextStep().getId())
                    traverser.addLabels(this.labels)
                    return IteratorUtils.of(traverser.split(getBindings(traverser), this))
                }
                return if (connective === ConnectiveStep.Connective.AND) {
                    val matchTraversal: Traversal.Admin<Object, Object> =
                        getMatchAlgorithm().apply(traverser) // determine which sub-pattern the traverser should try next
                    traverser.getTags().add(matchTraversal.getStartStep().getId())
                    traverser.setStepId(
                        matchTraversal.getStartStep().getId()
                    ) // go down the traversal match sub-pattern
                    IteratorUtils.of(traverser)
                } else { // OR
                    val traversers: List<Traverser.Admin<Map<String, E>>> = ArrayList(matchTraversals.size())
                    for (matchTraversal in matchTraversals) {
                        val split: Traverser.Admin = traverser.split()
                        split.getTags().add(matchTraversal.getStartStep().getId())
                        split.setStepId(matchTraversal.getStartStep().getId())
                        traversers.add(split)
                    }
                    traversers.iterator()
                }
            }
        }
    }

    @Override
    override fun hashCode(): Int {
        var result = super.hashCode() xor connective.hashCode()
        for (t in matchTraversals) {
            result = result xor t.hashCode()
        }
        return result
    }

    @get:Override
    val requirements: Set<Any>
        get() = this.getSelfAndChildRequirements(TraverserRequirement.LABELED_PATH, TraverserRequirement.SIDE_EFFECTS)

    //////////////////////////////
    class MatchStartStep(traversal: Traversal.Admin?, private val selectKey: String?) :
        AbstractStep<Object?, Object?>(traversal), Scoping {
        // computer the first time and then save resultant keys
        // this is the old way but it only checked for where() as the next step, not arbitrarily throughout traversal
        // just going to keep this in case something pops up in the future and this is needed as an original reference.
        /* if (this.getNextStep() instanceof WhereTraversalStep || this.getNextStep() instanceof WherePredicateStep)
            this.scopeKeys.addAll(((Scoping) this.getNextStep()).getScopeKeys());*/
        @get:Override
        var scopeKeys: Set<String>? = null
            get() {
                if (null == field) { // computer the first time and then save resultant keys
                    field = HashSet()
                    if (null != selectKey) field.add(selectKey)
                    val endLabels = (this.getTraversal().getParent() as MatchStep<*, *>).matchEndLabels
                    TraversalHelper.anyStepRecursively({ step ->
                        if (step is WherePredicateStep || step is WhereTraversalStep) {
                            for (key in (step as Scoping).getScopeKeys()) {
                                if (endLabels.contains(key)) field.add(key)
                            }
                        }
                        false
                    }, this.getTraversal())
                    // this is the old way but it only checked for where() as the next step, not arbitrarily throughout traversal
                    // just going to keep this in case something pops up in the future and this is needed as an original reference.
                    /* if (this.getNextStep() instanceof WhereTraversalStep || this.getNextStep() instanceof WherePredicateStep)
                        this.scopeKeys.addAll(((Scoping) this.getNextStep()).getScopeKeys());*/field =
                        Collections.unmodifiableSet(
                            field
                        )
                }
                return field
            }
            private set
        private var parent: MatchStep<*, *>? = null
        @Override
        @Throws(NoSuchElementException::class)
        protected fun processNextStart(): Traverser.Admin<Object?> {
            if (null == parent) parent = this.getTraversal().getParent()
            val traverser: Traverser.Admin<Object?> = this.starts.next()
            parent!!.getMatchAlgorithm()!!.recordStart(traverser, this.getTraversal())
            // TODO: sideEffect check?
            return if (null == selectKey) traverser else traverser.split(
                traverser.path().get(Pop.last, selectKey),
                this
            )
        }

        @Override
        override fun toString(): String {
            return StringFactory.stepString(this, selectKey)
        }

        @Override
        override fun hashCode(): Int {
            var result = super.hashCode()
            if (null != selectKey) result = result xor selectKey.hashCode()
            return result
        }

        fun getSelectKey(): Optional<String> {
            return Optional.ofNullable(selectKey)
        }
    }

    class MatchEndStep(traversal: Traversal.Admin?, private val matchKey: String?) : EndStep<Object?>(traversal),
        Scoping {
        private val matchKeyCollection: Set<String>
        private var parent: MatchStep<*, *>? = null

        init {
            matchKeyCollection = if (null == matchKey) Collections.emptySet() else Collections.singleton(matchKey)
        }

        private fun <S> retractUnnecessaryLabels(traverser: Traverser.Admin<S>): Traverser.Admin<S> {
            if (null == parent!!.getKeepLabels()) return traverser
            val keepers: Set<String> = HashSet(parent!!.getKeepLabels())
            val tags: Set<String> = traverser.getTags()
            for (matchTraversal in parent!!.matchTraversals) { // get remaining traversal patterns for the traverser
                val startStepId: String = matchTraversal.getStartStep().getId()
                if (!tags.contains(startStepId)) {
                    keepers.addAll(parent!!.referencedLabelsMap!![startStepId]) // get the reference labels required for those remaining traversals
                }
            }
            return PathProcessor.processTraverserPathLabels(
                traverser,
                keepers
            ) // remove all reference labels that are no longer required
        }

        @Override
        @Throws(NoSuchElementException::class)
        protected fun processNextStart(): Traverser.Admin<Object> {
            if (null == parent) parent = this.getTraversal().getParent().asStep()
            while (true) {
                val traverser: Traverser.Admin = this.starts.next()
                // no end label
                if (null == matchKey) {
                    // if (this.traverserStepIdAndLabelsSetByChild) -- traverser equality is based on stepId, lets ensure they are all at the parent
                    traverser.setStepId(parent.getId())
                    parent!!.getMatchAlgorithm()!!.recordEnd(traverser, this.getTraversal())
                    return retractUnnecessaryLabels(traverser)
                }
                // TODO: sideEffect check?
                // path check
                val path: Path = traverser.path()
                if (!path.hasLabel(matchKey) || traverser.get().equals(path.get(Pop.last, matchKey))) {
                    // if (this.traverserStepIdAndLabelsSetByChild) -- traverser equality is based on stepId and thus, lets ensure they are all at the parent
                    traverser.setStepId(parent.getId())
                    traverser.addLabels(matchKeyCollection)
                    parent!!.getMatchAlgorithm()!!.recordEnd(traverser, this.getTraversal())
                    return retractUnnecessaryLabels(traverser)
                }
            }
        }

        fun getMatchKey(): Optional<String> {
            return Optional.ofNullable(matchKey)
        }

        @Override
        override fun toString(): String {
            return StringFactory.stepString(this, matchKey)
        }

        @Override
        override fun hashCode(): Int {
            var result = super.hashCode()
            if (null != matchKey) result = result xor matchKey.hashCode()
            return result
        }

        @Override
        fun getScopeKeys(): Set<String> {
            return matchKeyCollection
        }
    }

    //////////////////////////////
    object Helper {
        fun getEndLabel(traversal: Traversal.Admin<Object?, Object?>): Optional<String> {
            val endStep: Step<*, *> = traversal.getEndStep()
            return if (endStep is ProfileStep) // TOTAL HACK
                (endStep.getPreviousStep() as MatchEndStep).getMatchKey() else (endStep as MatchEndStep).getMatchKey()
        }

        fun getStartLabels(traversal: Traversal.Admin<Object?, Object?>): Set<String> {
            return (traversal.getStartStep() as Scoping).getScopeKeys()
        }

        fun hasStartLabels(traverser: Traverser.Admin<Object?>, traversal: Traversal.Admin<Object?, Object?>): Boolean {
            for (label in getStartLabels(traversal)) {
                if (!traverser.path().hasLabel(label)) return false
            }
            return true
        }

        fun hasEndLabel(traverser: Traverser.Admin<Object?>, traversal: Traversal.Admin<Object?, Object?>): Boolean {
            val endLabel: Optional<String> = getEndLabel(traversal)
            return endLabel.isPresent() && traverser.path().hasLabel(endLabel.get()) // TODO: !isPresent?
        }

        fun hasExecutedTraversal(
            traverser: Traverser.Admin<Object?>,
            traversal: Traversal.Admin<Object?, Object?>
        ): Boolean {
            return traverser.getTags().contains(traversal.getStartStep().getId())
        }

        fun getTraversalType(traversal: Traversal.Admin<Object?, Object?>): TraversalType {
            val nextStep: Step<*, *> = traversal.getStartStep().getNextStep()
            return if (nextStep is WherePredicateStep) TraversalType.WHERE_PREDICATE else if (nextStep is WhereTraversalStep) TraversalType.WHERE_TRAVERSAL else TraversalType.MATCH_TRAVERSAL
        }

        fun computeStartLabel(traversals: List<Traversal.Admin<Object?, Object?>?>): String {
            run {

                // a traversal start label, that's not used as an end label, must be the step's start label
                val startLabels: Set<String> = HashSet()
                val endLabels: Set<String> = HashSet()
                for (traversal in traversals) {
                    getEndLabel(traversal).ifPresent(endLabels::add)
                    startLabels.addAll(getStartLabels(traversal))
                }
                startLabels.removeAll(endLabels)
                if (!startLabels.isEmpty()) return startLabels.iterator().next()
            }
            val sort: List<String> = ArrayList()
            for (traversal in traversals) {
                getStartLabels(traversal).stream().filter { startLabel -> !sort.contains(startLabel) }
                    .forEach(sort::add)
                getEndLabel(traversal).ifPresent { endLabel -> if (!sort.contains(endLabel)) sort.add(endLabel) }
            }
            Collections.sort(sort) { a, b ->
                for (traversal in traversals) {
                    val endLabel: Optional<String> = getEndLabel(traversal)
                    if (endLabel.isPresent()) {
                        val startLabels = getStartLabels(traversal)
                        if (a.equals(endLabel.get()) && startLabels.contains(b)) return@sort 1 else if (b.equals(
                                endLabel.get()
                            ) && startLabels.contains(a)
                        ) return@sort -1
                    }
                }
                0
            }
            return sort[0]
        }
    }

    //////////////////////////////
    interface MatchAlgorithm : Function<Traverser.Admin<Object?>?, Traversal.Admin<Object?, Object?>?>, Serializable {
        fun initialize(onComputer: Boolean, traversals: List<Traversal.Admin<Object?, Object?>?>?)
        fun recordStart(traverser: Traverser.Admin<Object?>?, traversal: Traversal.Admin<Object?, Object?>?) {}
        fun recordEnd(traverser: Traverser.Admin<Object?>?, traversal: Traversal.Admin<Object?, Object?>?) {}

        companion object {
            val UNMATCHABLE_PATTERN: Function<List<Traversal.Admin<Object, Object>>, IllegalStateException> =
                Function<List<Traversal.Admin<Object, Object>>, IllegalStateException> { traversals ->
                    IllegalStateException(
                        "The provided match pattern is unsolvable: $traversals"
                    )
                }
        }
    }

    class GreedyMatchAlgorithm : MatchAlgorithm {
        private var traversals: List<Traversal.Admin<Object, Object>>? = null
        @Override
        override fun initialize(onComputer: Boolean, traversals: List<Traversal.Admin<Object?, Object?>?>) {
            this.traversals = traversals
        }

        @Override
        fun apply(traverser: Traverser.Admin<Object?>): Traversal.Admin<Object?, Object?> {
            for (traversal in traversals) {
                if (!Helper.hasExecutedTraversal(traverser, traversal) && Helper.hasStartLabels(
                        traverser,
                        traversal
                    )
                ) return traversal
            }
            throw MatchAlgorithm.UNMATCHABLE_PATTERN.apply(traversals)
        }
    }

    class CountMatchAlgorithm : MatchAlgorithm {
        protected var bundles: List<Bundle>? = null
        protected var counter = 0
        protected var onComputer = false
        override fun initialize(onComputer: Boolean, traversals: List<Traversal.Admin<Object?, Object?>?>) {
            this.onComputer = onComputer
            bundles = traversals.stream().map { traversal: Traversal.Admin<Object?, Object?> -> Bundle(traversal) }
                .collect(Collectors.toList())
        }

        @Override
        fun apply(traverser: Traverser.Admin<Object?>): Traversal.Admin<Object, Object> {
            // optimization to favor processing StarGraph local objects first to limit message passing (GraphComputer only)
            // TODO: generalize this for future MatchAlgorithms (given that 3.2.0 will focus on RealTimeStrategy, it will probably go there)
            if (onComputer) {
                val labels: List<Set<String>> = traverser.path().labels()
                val lastLabels = labels[labels.size() - 1]
                Collections.sort(
                    bundles,
                    Comparator.< Bundle > comparingLong < Bundle ? > { b ->
                        Helper.getStartLabels(b.traversal).stream()
                            .filter { startLabel -> !lastLabels.contains(startLabel) }
                            .count()
                    }.thenComparingInt { b -> b.traversalType.ordinal() }
                        .thenComparingDouble { b -> b.multiplicity })
            }
            var startLabelsBundle: Bundle? = null
            for (bundle in bundles!!) {
                if (!Helper.hasExecutedTraversal(traverser, bundle.traversal) && Helper.hasStartLabels(
                        traverser,
                        bundle.traversal
                    )
                ) {
                    if (bundle.traversalType != TraversalType.MATCH_TRAVERSAL || Helper.hasEndLabel(
                            traverser,
                            bundle.traversal
                        )
                    ) return bundle.traversal else if (null == startLabelsBundle) startLabelsBundle = bundle
                }
            }
            if (null != startLabelsBundle) return startLabelsBundle.traversal
            throw MatchAlgorithm.UNMATCHABLE_PATTERN.apply(bundles.stream().map { record -> record.traversal }
                .collect(Collectors.toList()))
        }

        @Override
        override fun recordStart(traverser: Traverser.Admin<Object?>?, traversal: Traversal.Admin<Object?, Object?>) {
            getBundle(traversal).startsCount++
        }

        @Override
        override fun recordEnd(traverser: Traverser.Admin<Object?>?, traversal: Traversal.Admin<Object?, Object?>) {
            getBundle(traversal).incrementEndCount()
            if (!onComputer) {  // if on computer, sort on a per traverser-basis with bias towards local star graph
                if (counter < 200 || counter % 250 == 0) // aggressively sort for the first 200 results -- after that, sort every 250
                    Collections.sort(
                        bundles,
                        Comparator.< Bundle > comparingInt < Bundle ? > { b -> b.traversalType.ordinal() }.thenComparingDouble { b -> b.multiplicity })
                counter++
            }
        }

        protected fun getBundle(traversal: Traversal.Admin<Object?, Object?>): Bundle {
            for (bundle in bundles!!) {
                if (bundle.traversal === traversal) return bundle
            }
            throw IllegalStateException(
                "No equivalent traversal could be found in " + CountMatchAlgorithm::class.java.getSimpleName()
                    .toString() + ": " + traversal
            )
        }

        ///////////
        inner class Bundle(traversal: Traversal.Admin<Object?, Object?>) {
            var traversal: Traversal.Admin<Object, Object>
            var traversalType: TraversalType
            var startsCount: Long
            var endsCount: Long
            var multiplicity: Double

            init {
                this.traversal = traversal
                traversalType = Helper.getTraversalType(traversal)
                startsCount = 0L
                endsCount = 0L
                multiplicity = 0.0
            }

            fun incrementEndCount() {
                multiplicity = ++endsCount.toDouble() / startsCount.toDouble()
            }
        }
    }
}