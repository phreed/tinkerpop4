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
package org.apache.tinkerpop4.gremlin.language.grammar

import org.antlr.v4.runtime.tree.AbstractParseTreeVisitor

/**
 * This class provides implementation of [GremlinVisitor], where each method will throw
 * `UnsupportedOperationException`. All the visitor class will extends this class, so that if there is method
 * that are not manually implemented, and called, an exception will be thrown to help us catch bugs.
 *
 * @param <T> The return type of the visit operation. Use [Void] for
 * operations with no return type.
</T> */
class GremlinBaseVisitor<T> : AbstractParseTreeVisitor<T>(), GremlinVisitor<T> {
    protected fun notImplemented(ctx: ParseTree?) {
        val className = if (ctx != null) ctx.getClass().getName() else ""
        throw UnsupportedOperationException("Method not implemented for context class $className")
    }

    @Override
    fun visitQueryList(ctx: QueryListContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitQuery(ctx: QueryContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitEmptyQuery(ctx: EmptyQueryContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalSource(ctx: TraversalSourceContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTransactionPart(ctx: TransactionPartContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitRootTraversal(ctx: RootTraversalContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalSourceSelfMethod(ctx: TraversalSourceSelfMethodContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalSourceSelfMethod_withBulk(ctx: TraversalSourceSelfMethod_withBulkContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalSourceSelfMethod_withPath(ctx: TraversalSourceSelfMethod_withPathContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalSourceSelfMethod_withSack(ctx: TraversalSourceSelfMethod_withSackContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalSourceSelfMethod_withSideEffect(ctx: TraversalSourceSelfMethod_withSideEffectContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalSourceSelfMethod_withStrategies(ctx: TraversalSourceSelfMethod_withStrategiesContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalSourceSelfMethod_with(ctx: TraversalSourceSelfMethod_withContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalSourceSpawnMethod(ctx: TraversalSourceSpawnMethodContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalSourceSpawnMethod_addE(ctx: TraversalSourceSpawnMethod_addEContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalSourceSpawnMethod_addV(ctx: TraversalSourceSpawnMethod_addVContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalSourceSpawnMethod_E(ctx: TraversalSourceSpawnMethod_EContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalSourceSpawnMethod_V(ctx: TraversalSourceSpawnMethod_VContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalSourceSpawnMethod_inject(ctx: TraversalSourceSpawnMethod_injectContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalSourceSpawnMethod_io(ctx: TraversalSourceSpawnMethod_ioContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitChainedTraversal(ctx: ChainedTraversalContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitChainedParentOfGraphTraversal(ctx: ChainedParentOfGraphTraversalContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitNestedTraversal(ctx: NestedTraversalContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTerminatedTraversal(ctx: TerminatedTraversalContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod(ctx: TraversalMethodContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_V(ctx: TraversalMethod_VContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_addE_String(ctx: TraversalMethod_addE_StringContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_addE_Traversal(ctx: TraversalMethod_addE_TraversalContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_addV_Empty(ctx: TraversalMethod_addV_EmptyContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_addV_String(ctx: TraversalMethod_addV_StringContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_addV_Traversal(ctx: TraversalMethod_addV_TraversalContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_aggregate_String(ctx: TraversalMethod_aggregate_StringContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_aggregate_Scope_String(ctx: TraversalMethod_aggregate_Scope_StringContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_and(ctx: TraversalMethod_andContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_as(ctx: TraversalMethod_asContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_barrier_Consumer(ctx: TraversalMethod_barrier_ConsumerContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_barrier_Empty(ctx: TraversalMethod_barrier_EmptyContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_barrier_int(ctx: TraversalMethod_barrier_intContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_both(ctx: TraversalMethod_bothContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_bothE(ctx: TraversalMethod_bothEContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_bothV(ctx: TraversalMethod_bothVContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_branch(ctx: TraversalMethod_branchContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_by_Comparator(ctx: TraversalMethod_by_ComparatorContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_by_Empty(ctx: TraversalMethod_by_EmptyContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_by_Function(ctx: TraversalMethod_by_FunctionContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_by_Function_Comparator(ctx: TraversalMethod_by_Function_ComparatorContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_by_Order(ctx: TraversalMethod_by_OrderContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_by_String(ctx: TraversalMethod_by_StringContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_by_String_Comparator(ctx: TraversalMethod_by_String_ComparatorContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_by_T(ctx: TraversalMethod_by_TContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_by_Traversal(ctx: TraversalMethod_by_TraversalContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_by_Traversal_Comparator(ctx: TraversalMethod_by_Traversal_ComparatorContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_cap(ctx: TraversalMethod_capContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_choose_Function(ctx: TraversalMethod_choose_FunctionContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_choose_Predicate_Traversal(ctx: TraversalMethod_choose_Predicate_TraversalContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_choose_Predicate_Traversal_Traversal(ctx: TraversalMethod_choose_Predicate_Traversal_TraversalContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_choose_Traversal(ctx: TraversalMethod_choose_TraversalContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_choose_Traversal_Traversal(ctx: TraversalMethod_choose_Traversal_TraversalContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_choose_Traversal_Traversal_Traversal(ctx: TraversalMethod_choose_Traversal_Traversal_TraversalContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_coalesce(ctx: TraversalMethod_coalesceContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_coin(ctx: TraversalMethod_coinContext?): T? {
        notImplemented(ctx)
        return null
    }

    @Override
    fun visitTraversalMethod_connectedComponent(ctx: TraversalMethod_connectedComponentContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_constant(ctx: TraversalMethod_constantContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_count_Empty(ctx: TraversalMethod_count_EmptyContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_count_Scope(ctx: TraversalMethod_count_ScopeContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_cyclicPath(ctx: TraversalMethod_cyclicPathContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_dedup_Scope_String(ctx: TraversalMethod_dedup_Scope_StringContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_dedup_String(ctx: TraversalMethod_dedup_StringContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_drop(ctx: TraversalMethod_dropContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_elementMap(ctx: TraversalMethod_elementMapContext?): T? {
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_emit_Empty(ctx: TraversalMethod_emit_EmptyContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_emit_Predicate(ctx: TraversalMethod_emit_PredicateContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_emit_Traversal(ctx: TraversalMethod_emit_TraversalContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_fail_Empty(ctx: TraversalMethod_fail_EmptyContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_fail_String(ctx: TraversalMethod_fail_StringContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_filter_Predicate(ctx: TraversalMethod_filter_PredicateContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_filter_Traversal(ctx: TraversalMethod_filter_TraversalContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_flatMap(ctx: TraversalMethod_flatMapContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_fold_Empty(ctx: TraversalMethod_fold_EmptyContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_fold_Object_BiFunction(ctx: TraversalMethod_fold_Object_BiFunctionContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_from_String(ctx: TraversalMethod_from_StringContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_from_Traversal(ctx: TraversalMethod_from_TraversalContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_from_Vertex(ctx: TraversalMethod_from_VertexContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_group_Empty(ctx: TraversalMethod_group_EmptyContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_group_String(ctx: TraversalMethod_group_StringContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_groupCount_Empty(ctx: TraversalMethod_groupCount_EmptyContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_groupCount_String(ctx: TraversalMethod_groupCount_StringContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_has_String(ctx: TraversalMethod_has_StringContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_has_String_Object(ctx: TraversalMethod_has_String_ObjectContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_has_String_P(ctx: TraversalMethod_has_String_PContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_has_String_String_Object(ctx: TraversalMethod_has_String_String_ObjectContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_has_String_String_P(ctx: TraversalMethod_has_String_String_PContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_has_String_Traversal(ctx: TraversalMethod_has_String_TraversalContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_has_T_Object(ctx: TraversalMethod_has_T_ObjectContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_has_T_P(ctx: TraversalMethod_has_T_PContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_has_T_Traversal(ctx: TraversalMethod_has_T_TraversalContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_hasId_Object_Object(ctx: TraversalMethod_hasId_Object_ObjectContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_hasId_P(ctx: TraversalMethod_hasId_PContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_hasKey_P(ctx: TraversalMethod_hasKey_PContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_hasKey_String_String(ctx: TraversalMethod_hasKey_String_StringContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_hasLabel_P(ctx: TraversalMethod_hasLabel_PContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_hasLabel_String_String(ctx: TraversalMethod_hasLabel_String_StringContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_hasNot(ctx: TraversalMethod_hasNotContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_hasValue_Object_Object(ctx: TraversalMethod_hasValue_Object_ObjectContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_hasValue_P(ctx: TraversalMethod_hasValue_PContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_id(ctx: TraversalMethod_idContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_identity(ctx: TraversalMethod_identityContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_in(ctx: TraversalMethod_inContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_inE(ctx: TraversalMethod_inEContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_inV(ctx: TraversalMethod_inVContext?): T? {
        notImplemented(ctx)
        return null
    }

    @Override
    fun visitTraversalMethod_index(ctx: TraversalMethod_indexContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_inject(ctx: TraversalMethod_injectContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_is_Object(ctx: TraversalMethod_is_ObjectContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_is_P(ctx: TraversalMethod_is_PContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_key(ctx: TraversalMethod_keyContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_label(ctx: TraversalMethod_labelContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_limit_Scope_long(ctx: TraversalMethod_limit_Scope_longContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_limit_long(ctx: TraversalMethod_limit_longContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_local(ctx: TraversalMethod_localContext?): T? {
        notImplemented(ctx)
        return null
    }

    @Override
    fun visitTraversalMethod_loops_Empty(ctx: TraversalMethod_loops_EmptyContext?): T? {
        notImplemented(ctx)
        return null
    }

    @Override
    fun visitTraversalMethod_loops_String(ctx: TraversalMethod_loops_StringContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_map(ctx: TraversalMethod_mapContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_match(ctx: TraversalMethod_matchContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_math(ctx: TraversalMethod_mathContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_max_Empty(ctx: TraversalMethod_max_EmptyContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_max_Scope(ctx: TraversalMethod_max_ScopeContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_mean_Empty(ctx: TraversalMethod_mean_EmptyContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_mean_Scope(ctx: TraversalMethod_mean_ScopeContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_min_Empty(ctx: TraversalMethod_min_EmptyContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_min_Scope(ctx: TraversalMethod_min_ScopeContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_not(ctx: TraversalMethod_notContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_option_Object_Traversal(ctx: TraversalMethod_option_Object_TraversalContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_option_Traversal(ctx: TraversalMethod_option_TraversalContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_optional(ctx: TraversalMethod_optionalContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_or(ctx: TraversalMethod_orContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_order_Empty(ctx: TraversalMethod_order_EmptyContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_order_Scope(ctx: TraversalMethod_order_ScopeContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_otherV(ctx: TraversalMethod_otherVContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_out(ctx: TraversalMethod_outContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_outE(ctx: TraversalMethod_outEContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_outV(ctx: TraversalMethod_outVContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_pageRank_Empty(ctx: TraversalMethod_pageRank_EmptyContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_pageRank_double(ctx: TraversalMethod_pageRank_doubleContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_path(ctx: TraversalMethod_pathContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_peerPressure(ctx: TraversalMethod_peerPressureContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_profile_Empty(ctx: TraversalMethod_profile_EmptyContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_profile_String(ctx: TraversalMethod_profile_StringContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_project(ctx: TraversalMethod_projectContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_properties(ctx: TraversalMethod_propertiesContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_property_Cardinality_Object_Object_Object(ctx: TraversalMethod_property_Cardinality_Object_Object_ObjectContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_property_Object_Object_Object(ctx: TraversalMethod_property_Object_Object_ObjectContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_propertyMap(ctx: TraversalMethod_propertyMapContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_range_Scope_long_long(ctx: TraversalMethod_range_Scope_long_longContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_range_long_long(ctx: TraversalMethod_range_long_longContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_read(ctx: TraversalMethod_readContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_repeat_String_Traversal(ctx: TraversalMethod_repeat_String_TraversalContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_repeat_Traversal(ctx: TraversalMethod_repeat_TraversalContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_sack_BiFunction(ctx: TraversalMethod_sack_BiFunctionContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_sack_Empty(ctx: TraversalMethod_sack_EmptyContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_sample_Scope_int(ctx: TraversalMethod_sample_Scope_intContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_sample_int(ctx: TraversalMethod_sample_intContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_select_Column(ctx: TraversalMethod_select_ColumnContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_select_Pop_String(ctx: TraversalMethod_select_Pop_StringContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_select_Pop_String_String_String(ctx: TraversalMethod_select_Pop_String_String_StringContext?): T? {
        notImplemented(ctx)
        return null
    }

    @Override
    fun visitTraversalMethod_select_Pop_Traversal(ctx: TraversalMethod_select_Pop_TraversalContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_select_String(ctx: TraversalMethod_select_StringContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_select_String_String_String(ctx: TraversalMethod_select_String_String_StringContext?): T? {
        notImplemented(ctx)
        return null
    }

    @Override
    fun visitTraversalMethod_select_Traversal(ctx: TraversalMethod_select_TraversalContext?): T? {
        notImplemented(ctx)
        return null
    }

    @Override
    fun visitTraversalMethod_shortestPath(ctx: TraversalMethod_shortestPathContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_sideEffect(ctx: TraversalMethod_sideEffectContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_simplePath(ctx: TraversalMethod_simplePathContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_skip_Scope_long(ctx: TraversalMethod_skip_Scope_longContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_skip_long(ctx: TraversalMethod_skip_longContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_store(ctx: TraversalMethod_storeContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_subgraph(ctx: TraversalMethod_subgraphContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_sum_Empty(ctx: TraversalMethod_sum_EmptyContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_sum_Scope(ctx: TraversalMethod_sum_ScopeContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_tail_Empty(ctx: TraversalMethod_tail_EmptyContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_tail_Scope(ctx: TraversalMethod_tail_ScopeContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_tail_Scope_long(ctx: TraversalMethod_tail_Scope_longContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_tail_long(ctx: TraversalMethod_tail_longContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_timeLimit(ctx: TraversalMethod_timeLimitContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_times(ctx: TraversalMethod_timesContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_to_Direction_String(ctx: TraversalMethod_to_Direction_StringContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_to_String(ctx: TraversalMethod_to_StringContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_to_Traversal(ctx: TraversalMethod_to_TraversalContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_to_Vertex(ctx: TraversalMethod_to_VertexContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_toE(ctx: TraversalMethod_toEContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_toV(ctx: TraversalMethod_toVContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_tree_Empty(ctx: TraversalMethod_tree_EmptyContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_tree_String(ctx: TraversalMethod_tree_StringContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_unfold(ctx: TraversalMethod_unfoldContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_union(ctx: TraversalMethod_unionContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_until_Predicate(ctx: TraversalMethod_until_PredicateContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_until_Traversal(ctx: TraversalMethod_until_TraversalContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_value(ctx: TraversalMethod_valueContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_valueMap_String(ctx: TraversalMethod_valueMap_StringContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_valueMap_boolean_String(ctx: TraversalMethod_valueMap_boolean_StringContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_values(ctx: TraversalMethod_valuesContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_where_P(ctx: TraversalMethod_where_PContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_where_String_P(ctx: TraversalMethod_where_String_PContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_where_Traversal(ctx: TraversalMethod_where_TraversalContext?): T? {
        notImplemented(ctx)
        return null
    }

    @Override
    fun visitTraversalMethod_with_String(ctx: TraversalMethod_with_StringContext?): T? {
        notImplemented(ctx)
        return null
    }

    @Override
    fun visitTraversalMethod_with_String_Object(ctx: TraversalMethod_with_String_ObjectContext?): T? {
        notImplemented(ctx)
        return null
    }

    @Override
    fun visitTraversalMethod_write(ctx: TraversalMethod_writeContext?): T? {
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalScope(ctx: TraversalScopeContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalToken(ctx: TraversalTokenContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalOrder(ctx: TraversalOrderContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalDirection(ctx: TraversalDirectionContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalCardinality(ctx: TraversalCardinalityContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalColumn(ctx: TraversalColumnContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalPop(ctx: TraversalPopContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalOperator(ctx: TraversalOperatorContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalOptionParent(ctx: TraversalOptionParentContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalPredicate(ctx: TraversalPredicateContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalTerminalMethod(ctx: TraversalTerminalMethodContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalSackMethod(ctx: TraversalSackMethodContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalSelfMethod(ctx: TraversalSelfMethodContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalComparator(ctx: TraversalComparatorContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalFunction(ctx: TraversalFunctionContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalBiFunction(ctx: TraversalBiFunctionContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalPredicate_eq(ctx: TraversalPredicate_eqContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalPredicate_neq(ctx: TraversalPredicate_neqContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalPredicate_lt(ctx: TraversalPredicate_ltContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalPredicate_lte(ctx: TraversalPredicate_lteContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalPredicate_gt(ctx: TraversalPredicate_gtContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalPredicate_gte(ctx: TraversalPredicate_gteContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalPredicate_inside(ctx: TraversalPredicate_insideContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalPredicate_outside(ctx: TraversalPredicate_outsideContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalPredicate_between(ctx: TraversalPredicate_betweenContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalPredicate_within(ctx: TraversalPredicate_withinContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalPredicate_without(ctx: TraversalPredicate_withoutContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalPredicate_not(ctx: TraversalPredicate_notContext?): T? {
        notImplemented(ctx)
        return null
    }

    @Override
    fun visitTraversalPredicate_containing(ctx: TraversalPredicate_containingContext?): T? {
        notImplemented(ctx)
        return null
    }

    @Override
    fun visitTraversalPredicate_notContaining(ctx: TraversalPredicate_notContainingContext?): T? {
        notImplemented(ctx)
        return null
    }

    @Override
    fun visitTraversalPredicate_startingWith(ctx: TraversalPredicate_startingWithContext?): T? {
        notImplemented(ctx)
        return null
    }

    @Override
    fun visitTraversalPredicate_notStartingWith(ctx: TraversalPredicate_notStartingWithContext?): T? {
        notImplemented(ctx)
        return null
    }

    @Override
    fun visitTraversalPredicate_endingWith(ctx: TraversalPredicate_endingWithContext?): T? {
        notImplemented(ctx)
        return null
    }

    @Override
    fun visitTraversalPredicate_notEndingWith(ctx: TraversalPredicate_notEndingWithContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalTerminalMethod_iterate(ctx: TraversalTerminalMethod_iterateContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalTerminalMethod_explain(ctx: TraversalTerminalMethod_explainContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalTerminalMethod_hasNext(ctx: TraversalTerminalMethod_hasNextContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalTerminalMethod_tryNext(ctx: TraversalTerminalMethod_tryNextContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalTerminalMethod_next(ctx: TraversalTerminalMethod_nextContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalTerminalMethod_toList(ctx: TraversalTerminalMethod_toListContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalTerminalMethod_toSet(ctx: TraversalTerminalMethod_toSetContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalTerminalMethod_toBulkSet(ctx: TraversalTerminalMethod_toBulkSetContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalSelfMethod_none(ctx: TraversalSelfMethod_noneContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalStrategy(ctx: TraversalStrategyContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalStrategyList(ctx: TraversalStrategyListContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalStrategyExpr(ctx: TraversalStrategyExprContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalStrategyArgs_PartitionStrategy(ctx: TraversalStrategyArgs_PartitionStrategyContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalStrategyArgs_EdgeLabelVerificationStrategy(ctx: TraversalStrategyArgs_EdgeLabelVerificationStrategyContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalStrategyArgs_ReservedKeysVerificationStrategy(ctx: TraversalStrategyArgs_ReservedKeysVerificationStrategyContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalStrategyArgs_SubgraphStrategy(ctx: TraversalStrategyArgs_SubgraphStrategyContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalStrategyArgs_ProductiveByStrategy(ctx: TraversalStrategyArgs_ProductiveByStrategyContext?): T? {
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitNestedTraversalList(ctx: NestedTraversalListContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitNestedTraversalExpr(ctx: NestedTraversalExprContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitGenericLiteralList(ctx: GenericLiteralListContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitGenericLiteralExpr(ctx: GenericLiteralExprContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitGenericLiteralRange(ctx: GenericLiteralRangeContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitGenericLiteralCollection(ctx: GenericLiteralCollectionContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitStringLiteralList(ctx: StringLiteralListContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitStringLiteralExpr(ctx: StringLiteralExprContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitGenericLiteral(ctx: GenericLiteralContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitGenericLiteralMap(ctx: GenericLiteralMapContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitIntegerLiteral(ctx: IntegerLiteralContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitFloatLiteral(ctx: FloatLiteralContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitBooleanLiteral(ctx: BooleanLiteralContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitStringLiteral(ctx: StringLiteralContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitDateLiteral(ctx: DateLiteralContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitNullLiteral(ctx: NullLiteralContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitGremlinStringConstants(ctx: GremlinStringConstantsContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitPageRankStringConstants(ctx: PageRankStringConstantsContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitPeerPressureStringConstants(ctx: PeerPressureStringConstantsContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitShortestPathStringConstants(ctx: ShortestPathStringConstantsContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitWithOptionsStringConstants(ctx: WithOptionsStringConstantsContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitGremlinStringConstants_pageRankStringConstants_edges(ctx: GremlinStringConstants_pageRankStringConstants_edgesContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitGremlinStringConstants_pageRankStringConstants_times(ctx: GremlinStringConstants_pageRankStringConstants_timesContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitGremlinStringConstants_pageRankStringConstants_propertyName(ctx: GremlinStringConstants_pageRankStringConstants_propertyNameContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitGremlinStringConstants_peerPressureStringConstants_edges(ctx: GremlinStringConstants_peerPressureStringConstants_edgesContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitGremlinStringConstants_peerPressureStringConstants_times(ctx: GremlinStringConstants_peerPressureStringConstants_timesContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitGremlinStringConstants_peerPressureStringConstants_propertyName(ctx: GremlinStringConstants_peerPressureStringConstants_propertyNameContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitGremlinStringConstants_shortestPathStringConstants_target(ctx: GremlinStringConstants_shortestPathStringConstants_targetContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitGremlinStringConstants_shortestPathStringConstants_edges(ctx: GremlinStringConstants_shortestPathStringConstants_edgesContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitGremlinStringConstants_shortestPathStringConstants_distance(ctx: GremlinStringConstants_shortestPathStringConstants_distanceContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitGremlinStringConstants_shortestPathStringConstants_maxDistance(ctx: GremlinStringConstants_shortestPathStringConstants_maxDistanceContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitGremlinStringConstants_shortestPathStringConstants_includeEdges(ctx: GremlinStringConstants_shortestPathStringConstants_includeEdgesContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitGremlinStringConstants_withOptionsStringConstants_tokens(ctx: GremlinStringConstants_withOptionsStringConstants_tokensContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitGremlinStringConstants_withOptionsStringConstants_none(ctx: GremlinStringConstants_withOptionsStringConstants_noneContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitGremlinStringConstants_withOptionsStringConstants_ids(ctx: GremlinStringConstants_withOptionsStringConstants_idsContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitGremlinStringConstants_withOptionsStringConstants_labels(ctx: GremlinStringConstants_withOptionsStringConstants_labelsContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitGremlinStringConstants_withOptionsStringConstants_keys(ctx: GremlinStringConstants_withOptionsStringConstants_keysContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitGremlinStringConstants_withOptionsStringConstants_values(ctx: GremlinStringConstants_withOptionsStringConstants_valuesContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitGremlinStringConstants_withOptionsStringConstants_all(ctx: GremlinStringConstants_withOptionsStringConstants_allContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitGremlinStringConstants_withOptionsStringConstants_indexer(ctx: GremlinStringConstants_withOptionsStringConstants_indexerContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitGremlinStringConstants_withOptionsStringConstants_list(ctx: GremlinStringConstants_withOptionsStringConstants_listContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitGremlinStringConstants_withOptionsStringConstants_map(ctx: GremlinStringConstants_withOptionsStringConstants_mapContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitPageRankStringConstant(ctx: PageRankStringConstantContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitPeerPressureStringConstant(ctx: PeerPressureStringConstantContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitShortestPathStringConstant(ctx: ShortestPathStringConstantContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitWithOptionsStringConstant(ctx: WithOptionsStringConstantContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_option_Predicate_Traversal(ctx: TraversalMethod_option_Predicate_TraversalContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitIoOptionsStringConstants(ctx: IoOptionsStringConstantsContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitGremlinStringConstants_ioOptionsStringConstants_reader(ctx: GremlinStringConstants_ioOptionsStringConstants_readerContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitGremlinStringConstants_ioOptionsStringConstants_writer(ctx: GremlinStringConstants_ioOptionsStringConstants_writerContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitGremlinStringConstants_ioOptionsStringConstants_gryo(ctx: GremlinStringConstants_ioOptionsStringConstants_gryoContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitGremlinStringConstants_ioOptionsStringConstants_graphson(ctx: GremlinStringConstants_ioOptionsStringConstants_graphsonContext?): T? {
        notImplemented(ctx)
        return null
    }

    @Override
    fun visitGremlinStringConstants_ioOptionsStringConstants_graphml(ctx: GremlinStringConstants_ioOptionsStringConstants_graphmlContext?): T? {
        notImplemented(ctx)
        return null
    }

    @Override
    fun visitConnectedComponentConstants(ctx: ConnectedComponentConstantsContext?): T? {
        notImplemented(ctx)
        return null
    }

    @Override
    fun visitGremlinStringConstants_connectedComponentStringConstants_component(ctx: GremlinStringConstants_connectedComponentStringConstants_componentContext?): T? {
        notImplemented(ctx)
        return null
    }

    @Override
    fun visitGremlinStringConstants_connectedComponentStringConstants_edges(ctx: GremlinStringConstants_connectedComponentStringConstants_edgesContext?): T? {
        notImplemented(ctx)
        return null
    }

    @Override
    fun visitGremlinStringConstants_connectedComponentStringConstants_propertyName(ctx: GremlinStringConstants_connectedComponentStringConstants_propertyNameContext?): T? {
        notImplemented(ctx)
        return null
    }

    @Override
    fun visitConnectedComponentStringConstant(ctx: ConnectedComponentStringConstantContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitIoOptionsStringConstant(ctx: IoOptionsStringConstantContext?): T? {
        notImplemented(ctx)
        return null
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitStructureVertex(ctx: StructureVertexContext?): T? {
        notImplemented(ctx)
        return null
    }
}