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

import org.apache.tinkerpop4.gremlin.process.traversal.Operator

/**
 * Specific case of TraversalRootVisitor where all TraversalMethods returns
 * a GraphTraversal object.
 */
class TraversalMethodVisitor(antlr: GremlinAntlrToJava?, graphTraversal: GraphTraversal) :
    TraversalRootVisitor<GraphTraversal?>(antlr, graphTraversal) {
    /**
     * This object is used to append the traversal methods.
     */
    private val graphTraversal: GraphTraversal

    init {
        this.graphTraversal = graphTraversal
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod(ctx: TraversalMethodContext?): Traversal {
        return visitChildren(ctx)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_V(ctx: TraversalMethod_VContext): GraphTraversal {
        return if (ctx.genericLiteralList().getChildCount() !== 0) {
            graphTraversal.V(GenericLiteralVisitor.getGenericLiteralList(ctx.genericLiteralList()))
        } else {
            graphTraversal.V()
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_addV_Empty(ctx: TraversalMethod_addV_EmptyContext?): GraphTraversal {
        return graphTraversal.addV()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_addV_String(ctx: TraversalMethod_addV_StringContext): GraphTraversal {
        return graphTraversal.addV(GenericLiteralVisitor.getStringLiteral(ctx.stringLiteral()))
    }

    @Override
    fun visitTraversalMethod_addE_Traversal(ctx: TraversalMethod_addE_TraversalContext): GraphTraversal {
        return graphTraversal.addE(antlr.tvisitor.visitNestedTraversal(ctx.nestedTraversal()))
    }

    @Override
    fun visitTraversalMethod_addV_Traversal(ctx: TraversalMethod_addV_TraversalContext): GraphTraversal {
        return graphTraversal.addV(antlr.tvisitor.visitNestedTraversal(ctx.nestedTraversal()))
    }

    @Override
    fun visitTraversalMethod_addE_String(ctx: TraversalMethod_addE_StringContext): GraphTraversal {
        val childIndexOfParameterEdgeLabel = 2
        val stringLiteralContext: StringLiteralContext =
            ctx.getChild(childIndexOfParameterEdgeLabel) as StringLiteralContext
        return graphTraversal.addE(GenericLiteralVisitor.getStringLiteral(stringLiteralContext))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_aggregate_String(ctx: TraversalMethod_aggregate_StringContext): GraphTraversal {
        return graphTraversal.aggregate(GenericLiteralVisitor.getStringLiteral(ctx.stringLiteral()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_aggregate_Scope_String(ctx: TraversalMethod_aggregate_Scope_StringContext): GraphTraversal {
        return graphTraversal.aggregate(
            TraversalEnumParser.parseTraversalEnumFromContext(Scope::class.java, ctx.getChild(2)),
            GenericLiteralVisitor.getStringLiteral(ctx.stringLiteral())
        )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_and(ctx: TraversalMethod_andContext): GraphTraversal {
        return graphTraversal.and(
            antlr.tListVisitor.visitNestedTraversalList(ctx.nestedTraversalList())
        )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_as(ctx: TraversalMethod_asContext): GraphTraversal {
        return if (ctx.getChildCount() === 4) {
            graphTraversal.`as`(GenericLiteralVisitor.getStringLiteral(ctx.stringLiteral()))
        } else {
            graphTraversal.`as`(
                GenericLiteralVisitor.getStringLiteral(ctx.stringLiteral()),
                GenericLiteralVisitor.getStringLiteralList(ctx.stringLiteralList())
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_barrier_Consumer(ctx: TraversalMethod_barrier_ConsumerContext?): GraphTraversal {
        // normSack is a special consumer enum type defined in org.apache.tinkerpop4.gremlin.process.traversal.SackFunctions.Barrier
        // it is not used in any other traversal methods.
        return graphTraversal.barrier(normSack)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_barrier_Empty(ctx: TraversalMethod_barrier_EmptyContext?): GraphTraversal {
        return graphTraversal.barrier()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_barrier_int(ctx: TraversalMethod_barrier_intContext): GraphTraversal {
        return graphTraversal.barrier(Integer.valueOf(ctx.integerLiteral().getText()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_both(ctx: TraversalMethod_bothContext): GraphTraversal {
        return graphTraversal.both(GenericLiteralVisitor.getStringLiteralList(ctx.stringLiteralList()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_bothE(ctx: TraversalMethod_bothEContext): GraphTraversal {
        return graphTraversal.bothE(GenericLiteralVisitor.getStringLiteralList(ctx.stringLiteralList()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_bothV(ctx: TraversalMethod_bothVContext?): GraphTraversal {
        return graphTraversal.bothV()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_branch(ctx: TraversalMethod_branchContext): GraphTraversal {
        return graphTraversal.branch(antlr.tvisitor.visitNestedTraversal(ctx.nestedTraversal()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_by_Comparator(ctx: TraversalMethod_by_ComparatorContext): GraphTraversal {
        return graphTraversal.by(TraversalEnumParser.parseTraversalEnumFromContext(Order::class.java, ctx.getChild(2)))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_by_Empty(ctx: TraversalMethod_by_EmptyContext?): GraphTraversal {
        return graphTraversal.by()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_by_Function(ctx: TraversalMethod_by_FunctionContext): GraphTraversal {
        return graphTraversal.by(TraversalFunctionVisitor.instance().visitTraversalFunction(ctx.traversalFunction()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_by_Function_Comparator(ctx: TraversalMethod_by_Function_ComparatorContext): GraphTraversal {
        return graphTraversal.by(
            TraversalFunctionVisitor.instance().visitTraversalFunction(ctx.traversalFunction()),
            TraversalEnumParser.parseTraversalEnumFromContext(Order::class.java, ctx.getChild(4))
        )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_by_Order(ctx: TraversalMethod_by_OrderContext): GraphTraversal {
        return graphTraversal.by(TraversalEnumParser.parseTraversalEnumFromContext(Order::class.java, ctx.getChild(2)))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_by_String(ctx: TraversalMethod_by_StringContext): GraphTraversal {
        return graphTraversal.by(GenericLiteralVisitor.getStringLiteral(ctx.stringLiteral()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_by_String_Comparator(ctx: TraversalMethod_by_String_ComparatorContext): GraphTraversal {
        return graphTraversal.by(
            GenericLiteralVisitor.getStringLiteral(ctx.stringLiteral()),
            TraversalEnumParser.parseTraversalEnumFromContext(Order::class.java, ctx.getChild(4))
        )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_by_T(ctx: TraversalMethod_by_TContext): GraphTraversal {
        return graphTraversal.by(TraversalEnumParser.parseTraversalEnumFromContext(T::class.java, ctx.getChild(2)))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_by_Traversal(ctx: TraversalMethod_by_TraversalContext): GraphTraversal {
        return graphTraversal.by(antlr.tvisitor.visitNestedTraversal(ctx.nestedTraversal()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_by_Traversal_Comparator(ctx: TraversalMethod_by_Traversal_ComparatorContext): GraphTraversal {
        return graphTraversal.by(
            antlr.tvisitor.visitNestedTraversal(ctx.nestedTraversal()),
            TraversalEnumParser.parseTraversalEnumFromContext(Order::class.java, ctx.getChild(4))
        )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_cap(ctx: TraversalMethod_capContext): GraphTraversal {
        return if (ctx.getChildCount() === 4) {
            graphTraversal.cap(GenericLiteralVisitor.getStringLiteral(ctx.stringLiteral()))
        } else {
            graphTraversal.cap(
                GenericLiteralVisitor.getStringLiteral(ctx.stringLiteral()),
                GenericLiteralVisitor.getStringLiteralList(ctx.stringLiteralList())
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_choose_Function(ctx: TraversalMethod_choose_FunctionContext): GraphTraversal {
        return graphTraversal.choose(
            TraversalFunctionVisitor.instance().visitTraversalFunction(ctx.traversalFunction())
        )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_choose_Predicate_Traversal(ctx: TraversalMethod_choose_Predicate_TraversalContext): GraphTraversal {
        return graphTraversal.choose(
            TraversalPredicateVisitor.instance().visitTraversalPredicate(ctx.traversalPredicate()),
            antlr.tvisitor.visitNestedTraversal(ctx.nestedTraversal())
        )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_choose_Predicate_Traversal_Traversal(ctx: TraversalMethod_choose_Predicate_Traversal_TraversalContext): GraphTraversal {
        return graphTraversal.choose(
            TraversalPredicateVisitor.instance().visitTraversalPredicate(ctx.traversalPredicate()),
            antlr.tvisitor.visitNestedTraversal(ctx.nestedTraversal(0)),
            antlr.tvisitor.visitNestedTraversal(ctx.nestedTraversal(1))
        )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_choose_Traversal(ctx: TraversalMethod_choose_TraversalContext): GraphTraversal {
        return graphTraversal.choose(antlr.tvisitor.visitNestedTraversal(ctx.nestedTraversal()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_choose_Traversal_Traversal(ctx: TraversalMethod_choose_Traversal_TraversalContext): GraphTraversal {
        return graphTraversal.choose(
            antlr.tvisitor.visitNestedTraversal(ctx.nestedTraversal(0)),
            antlr.tvisitor.visitNestedTraversal(ctx.nestedTraversal(1))
        )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_choose_Traversal_Traversal_Traversal(ctx: TraversalMethod_choose_Traversal_Traversal_TraversalContext): GraphTraversal {
        return graphTraversal.choose(
            antlr.tvisitor.visitNestedTraversal(ctx.nestedTraversal(0)),
            antlr.tvisitor.visitNestedTraversal(ctx.nestedTraversal(1)),
            antlr.tvisitor.visitNestedTraversal(ctx.nestedTraversal(2))
        )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_coalesce(ctx: TraversalMethod_coalesceContext): GraphTraversal {
        return graphTraversal.coalesce(
            antlr.tListVisitor.visitNestedTraversalList(ctx.nestedTraversalList())
        )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_coin(ctx: TraversalMethod_coinContext): GraphTraversal {
        return graphTraversal.coin(Double.valueOf(ctx.floatLiteral().getText()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_constant(ctx: TraversalMethod_constantContext): GraphTraversal {
        return graphTraversal
            .constant(GenericLiteralVisitor.instance().visitGenericLiteral(ctx.genericLiteral()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_count_Empty(ctx: TraversalMethod_count_EmptyContext?): GraphTraversal {
        return graphTraversal.count()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_count_Scope(ctx: TraversalMethod_count_ScopeContext): GraphTraversal {
        return graphTraversal.count(
            TraversalEnumParser.parseTraversalEnumFromContext(
                Scope::class.java,
                ctx.getChild(2)
            )
        )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_cyclicPath(ctx: TraversalMethod_cyclicPathContext?): GraphTraversal {
        return graphTraversal.cyclicPath()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_dedup_Scope_String(ctx: TraversalMethod_dedup_Scope_StringContext): GraphTraversal {
        return graphTraversal.dedup(
            TraversalEnumParser.parseTraversalEnumFromContext(Scope::class.java, ctx.getChild(2)),
            GenericLiteralVisitor.getStringLiteralList(ctx.stringLiteralList())
        )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_dedup_String(ctx: TraversalMethod_dedup_StringContext): GraphTraversal {
        return graphTraversal.dedup(GenericLiteralVisitor.getStringLiteralList(ctx.stringLiteralList()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_drop(ctx: TraversalMethod_dropContext?): GraphTraversal {
        return graphTraversal.drop()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_emit_Empty(ctx: TraversalMethod_emit_EmptyContext?): GraphTraversal {
        return graphTraversal.emit()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_emit_Predicate(ctx: TraversalMethod_emit_PredicateContext): GraphTraversal {
        return graphTraversal.emit(
            TraversalPredicateVisitor.instance().visitTraversalPredicate(ctx.traversalPredicate())
        )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_emit_Traversal(ctx: TraversalMethod_emit_TraversalContext): GraphTraversal {
        return graphTraversal.emit(antlr.tvisitor.visitNestedTraversal(ctx.nestedTraversal()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_fail_Empty(ctx: TraversalMethod_fail_EmptyContext?): Traversal {
        return graphTraversal.fail()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_fail_String(ctx: TraversalMethod_fail_StringContext): Traversal {
        return graphTraversal.fail(GenericLiteralVisitor.getStringLiteral(ctx.stringLiteral()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_filter_Predicate(ctx: TraversalMethod_filter_PredicateContext): GraphTraversal {
        return graphTraversal.filter(
            TraversalPredicateVisitor.instance().visitTraversalPredicate(ctx.traversalPredicate())
        )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_filter_Traversal(ctx: TraversalMethod_filter_TraversalContext): GraphTraversal {
        return graphTraversal.filter(antlr.tvisitor.visitNestedTraversal(ctx.nestedTraversal()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_flatMap(ctx: TraversalMethod_flatMapContext): GraphTraversal {
        return graphTraversal.flatMap(antlr.tvisitor.visitNestedTraversal(ctx.nestedTraversal()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_fold_Empty(ctx: TraversalMethod_fold_EmptyContext?): GraphTraversal {
        return graphTraversal.fold()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_fold_Object_BiFunction(ctx: TraversalMethod_fold_Object_BiFunctionContext): GraphTraversal {
        return graphTraversal.fold(
            GenericLiteralVisitor.instance().visitGenericLiteral(ctx.genericLiteral()),
            TraversalEnumParser.parseTraversalEnumFromContext(Operator::class.java, ctx.getChild(4)) as BiFunction
        )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_from_String(ctx: TraversalMethod_from_StringContext): GraphTraversal {
        return graphTraversal.from(GenericLiteralVisitor.getStringLiteral(ctx.stringLiteral()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_from_Traversal(ctx: TraversalMethod_from_TraversalContext): GraphTraversal {
        return graphTraversal.from(antlr.tvisitor.visitNestedTraversal(ctx.nestedTraversal()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_groupCount_Empty(ctx: TraversalMethod_groupCount_EmptyContext?): GraphTraversal {
        return graphTraversal.groupCount()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_groupCount_String(ctx: TraversalMethod_groupCount_StringContext): GraphTraversal {
        return graphTraversal.groupCount(GenericLiteralVisitor.getStringLiteral(ctx.stringLiteral()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_group_Empty(ctx: TraversalMethod_group_EmptyContext?): GraphTraversal {
        return graphTraversal.group()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_group_String(ctx: TraversalMethod_group_StringContext): GraphTraversal {
        return graphTraversal.group(GenericLiteralVisitor.getStringLiteral(ctx.stringLiteral()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_hasId_Object_Object(ctx: TraversalMethod_hasId_Object_ObjectContext): GraphTraversal {
        return graphTraversal.hasId(
            GenericLiteralVisitor.instance().visitGenericLiteral(ctx.genericLiteral()),
            GenericLiteralVisitor.getGenericLiteralList(ctx.genericLiteralList())
        )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_hasId_P(ctx: TraversalMethod_hasId_PContext): GraphTraversal {
        return graphTraversal.hasId(
            TraversalPredicateVisitor.instance().visitTraversalPredicate(ctx.traversalPredicate())
        )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_hasKey_P(ctx: TraversalMethod_hasKey_PContext): GraphTraversal {
        return graphTraversal.hasKey(
            TraversalPredicateVisitor.instance().visitTraversalPredicate(ctx.traversalPredicate())
        )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_hasKey_String_String(ctx: TraversalMethod_hasKey_String_StringContext): GraphTraversal {
        return if (ctx.getChildCount() === 4) {
            graphTraversal.hasKey(GenericLiteralVisitor.getStringLiteral(ctx.stringLiteral()))
        } else {
            graphTraversal.hasKey(
                GenericLiteralVisitor.getStringLiteral(ctx.stringLiteral()),
                GenericLiteralVisitor.getStringLiteralList(ctx.stringLiteralList())
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_hasLabel_P(ctx: TraversalMethod_hasLabel_PContext): GraphTraversal {
        return graphTraversal.hasLabel(
            TraversalPredicateVisitor.instance().visitTraversalPredicate(ctx.traversalPredicate())
        )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_hasLabel_String_String(ctx: TraversalMethod_hasLabel_String_StringContext): GraphTraversal {
        return if (ctx.getChildCount() === 4) {
            graphTraversal.hasLabel(GenericLiteralVisitor.getStringLiteral(ctx.stringLiteral()))
        } else {
            graphTraversal.hasLabel(
                GenericLiteralVisitor.getStringLiteral(ctx.stringLiteral()),
                GenericLiteralVisitor.getStringLiteralList(ctx.stringLiteralList())
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_hasNot(ctx: TraversalMethod_hasNotContext): GraphTraversal {
        return graphTraversal.hasNot(GenericLiteralVisitor.getStringLiteral(ctx.stringLiteral()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_hasValue_Object_Object(ctx: TraversalMethod_hasValue_Object_ObjectContext): GraphTraversal {
        return graphTraversal.hasValue(
            GenericLiteralVisitor.instance().visitGenericLiteral(ctx.genericLiteral()),
            GenericLiteralVisitor.getGenericLiteralList(ctx.genericLiteralList())
        )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_hasValue_P(ctx: TraversalMethod_hasValue_PContext): GraphTraversal {
        return graphTraversal.hasValue(
            TraversalPredicateVisitor.instance().visitTraversalPredicate(ctx.traversalPredicate())
        )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_has_String(ctx: TraversalMethod_has_StringContext): GraphTraversal {
        return graphTraversal.has(GenericLiteralVisitor.getStringLiteral(ctx.stringLiteral()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_has_String_Object(ctx: TraversalMethod_has_String_ObjectContext): GraphTraversal {
        return graphTraversal.has(
            GenericLiteralVisitor.getStringLiteral(ctx.stringLiteral()),
            GenericLiteralVisitor(antlr).visitGenericLiteral(ctx.genericLiteral())
        )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_has_String_P(ctx: TraversalMethod_has_String_PContext): GraphTraversal {
        return graphTraversal.has(
            GenericLiteralVisitor.getStringLiteral(ctx.stringLiteral()),
            TraversalPredicateVisitor.instance().visitTraversalPredicate(ctx.traversalPredicate())
        )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_has_String_String_Object(ctx: TraversalMethod_has_String_String_ObjectContext): GraphTraversal {
        return graphTraversal.has(
            GenericLiteralVisitor.getStringLiteral(ctx.stringLiteral(0)),
            GenericLiteralVisitor.getStringLiteral(ctx.stringLiteral(1)),
            GenericLiteralVisitor.instance().visitGenericLiteral(ctx.genericLiteral())
        )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_has_String_String_P(ctx: TraversalMethod_has_String_String_PContext): GraphTraversal {
        return graphTraversal.has(
            GenericLiteralVisitor.getStringLiteral(ctx.stringLiteral(0)),
            GenericLiteralVisitor.getStringLiteral(ctx.stringLiteral(1)),
            TraversalPredicateVisitor.instance().visitTraversalPredicate(ctx.traversalPredicate())
        )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_has_String_Traversal(ctx: TraversalMethod_has_String_TraversalContext): GraphTraversal {
        return graphTraversal.has(
            GenericLiteralVisitor.getStringLiteral(ctx.stringLiteral()),
            antlr.tvisitor.visitNestedTraversal(ctx.nestedTraversal())
        )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_has_T_Object(ctx: TraversalMethod_has_T_ObjectContext): GraphTraversal {
        return graphTraversal.has(
            TraversalEnumParser.parseTraversalEnumFromContext(T::class.java, ctx.getChild(2)),
            GenericLiteralVisitor(antlr).visitGenericLiteral(ctx.genericLiteral())
        )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_has_T_P(ctx: TraversalMethod_has_T_PContext): GraphTraversal {
        return graphTraversal.has(
            TraversalEnumParser.parseTraversalEnumFromContext(T::class.java, ctx.getChild(2)),
            TraversalPredicateVisitor.instance().visitTraversalPredicate(ctx.traversalPredicate())
        )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_has_T_Traversal(ctx: TraversalMethod_has_T_TraversalContext): GraphTraversal {
        return graphTraversal.has(
            TraversalEnumParser.parseTraversalEnumFromContext(T::class.java, ctx.getChild(2)),
            antlr.tvisitor.visitNestedTraversal(ctx.nestedTraversal())
        )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_id(ctx: TraversalMethod_idContext?): GraphTraversal {
        return graphTraversal.id()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_identity(ctx: TraversalMethod_identityContext?): GraphTraversal {
        return graphTraversal.identity()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_in(ctx: TraversalMethod_inContext): GraphTraversal {
        return graphTraversal.`in`(GenericLiteralVisitor.getStringLiteralList(ctx.stringLiteralList()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_inE(ctx: TraversalMethod_inEContext): GraphTraversal {
        return graphTraversal.inE(GenericLiteralVisitor.getStringLiteralList(ctx.stringLiteralList()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_inV(ctx: TraversalMethod_inVContext?): GraphTraversal {
        return graphTraversal.inV()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_inject(ctx: TraversalMethod_injectContext): GraphTraversal {
        return graphTraversal.inject(GenericLiteralVisitor.getGenericLiteralList(ctx.genericLiteralList()))
    }

    @Override
    fun visitTraversalMethod_index(ctx: TraversalMethod_indexContext?): GraphTraversal {
        return graphTraversal.index()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_is_Object(ctx: TraversalMethod_is_ObjectContext): GraphTraversal {
        return graphTraversal.`is`(GenericLiteralVisitor.instance().visitGenericLiteral(ctx.genericLiteral()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_is_P(ctx: TraversalMethod_is_PContext): GraphTraversal {
        return graphTraversal.`is`(
            TraversalPredicateVisitor.instance().visitTraversalPredicate(ctx.traversalPredicate())
        )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_key(ctx: TraversalMethod_keyContext?): GraphTraversal {
        return graphTraversal.key()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_label(ctx: TraversalMethod_labelContext?): GraphTraversal {
        return graphTraversal.label()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_limit_Scope_long(ctx: TraversalMethod_limit_Scope_longContext): GraphTraversal {
        return graphTraversal.limit(
            TraversalEnumParser.parseTraversalEnumFromContext(Scope::class.java, ctx.getChild(2)),
            Integer.valueOf(ctx.integerLiteral().getText())
        )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_limit_long(ctx: TraversalMethod_limit_longContext): GraphTraversal {
        return graphTraversal.limit(Integer.valueOf(ctx.integerLiteral().getText()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_local(ctx: TraversalMethod_localContext): GraphTraversal {
        return graphTraversal.local(antlr.tvisitor.visitNestedTraversal(ctx.nestedTraversal()))
    }

    @Override
    fun visitTraversalMethod_loops_Empty(ctx: TraversalMethod_loops_EmptyContext?): GraphTraversal {
        return graphTraversal.loops()
    }

    @Override
    fun visitTraversalMethod_loops_String(ctx: TraversalMethod_loops_StringContext): GraphTraversal {
        return graphTraversal.loops(GenericLiteralVisitor.getStringLiteral(ctx.stringLiteral()))
    }

    @Override
    fun visitTraversalMethod_repeat_String_Traversal(ctx: TraversalMethod_repeat_String_TraversalContext): GraphTraversal {
        return graphTraversal.repeat(
            GenericLiteralVisitor.getStringLiteral(ctx.stringLiteral()),
            antlr.tvisitor.visitNestedTraversal(ctx.nestedTraversal())
        )
    }

    @Override
    fun visitTraversalMethod_repeat_Traversal(ctx: TraversalMethod_repeat_TraversalContext): GraphTraversal {
        return graphTraversal.repeat(antlr.tvisitor.visitNestedTraversal(ctx.nestedTraversal()))
    }

    @Override
    fun visitTraversalMethod_read(ctx: TraversalMethod_readContext?): GraphTraversal {
        return graphTraversal.read()
    }

    @Override
    fun visitTraversalMethod_write(ctx: TraversalMethod_writeContext?): GraphTraversal {
        return graphTraversal.write()
    }

    @Override
    fun visitTraversalMethod_with_String(ctx: TraversalMethod_with_StringContext): GraphTraversal {
        return graphTraversal.with(GenericLiteralVisitor.getStringLiteral(ctx.stringLiteral()))
    }

    @Override
    fun visitTraversalMethod_with_String_Object(ctx: TraversalMethod_with_String_ObjectContext): GraphTraversal {
        return graphTraversal.with(
            GenericLiteralVisitor.getStringLiteral(ctx.stringLiteral()),
            GenericLiteralVisitor(antlr).visitGenericLiteral(ctx.genericLiteral())
        )
    }

    @Override
    fun visitTraversalMethod_shortestPath(ctx: TraversalMethod_shortestPathContext?): GraphTraversal {
        return graphTraversal.shortestPath()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_map(ctx: TraversalMethod_mapContext): GraphTraversal {
        return graphTraversal.map(antlr.tvisitor.visitNestedTraversal(ctx.nestedTraversal()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_match(ctx: TraversalMethod_matchContext): GraphTraversal {
        return graphTraversal.match(
            antlr.tListVisitor.visitNestedTraversalList(ctx.nestedTraversalList())
        )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_max_Empty(ctx: TraversalMethod_max_EmptyContext?): GraphTraversal {
        return graphTraversal.max()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_max_Scope(ctx: TraversalMethod_max_ScopeContext): GraphTraversal {
        return graphTraversal.max(TraversalEnumParser.parseTraversalEnumFromContext(Scope::class.java, ctx.getChild(2)))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_mean_Empty(ctx: TraversalMethod_mean_EmptyContext?): GraphTraversal {
        return graphTraversal.mean()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_mean_Scope(ctx: TraversalMethod_mean_ScopeContext): GraphTraversal {
        return graphTraversal.mean(
            TraversalEnumParser.parseTraversalEnumFromContext(
                Scope::class.java,
                ctx.getChild(2)
            )
        )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_min_Empty(ctx: TraversalMethod_min_EmptyContext?): GraphTraversal {
        return graphTraversal.min()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_min_Scope(ctx: TraversalMethod_min_ScopeContext): GraphTraversal {
        return graphTraversal.min(TraversalEnumParser.parseTraversalEnumFromContext(Scope::class.java, ctx.getChild(2)))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_not(ctx: TraversalMethod_notContext): GraphTraversal {
        return graphTraversal.not(antlr.tvisitor.visitNestedTraversal(ctx.nestedTraversal()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_option_Object_Traversal(ctx: TraversalMethod_option_Object_TraversalContext): GraphTraversal {
        return graphTraversal.option(
            GenericLiteralVisitor(antlr).visitGenericLiteral(ctx.genericLiteral()),
            antlr.tvisitor.visitNestedTraversal(ctx.nestedTraversal())
        )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_option_Traversal(ctx: TraversalMethod_option_TraversalContext): GraphTraversal {
        return graphTraversal.option(antlr.tvisitor.visitNestedTraversal(ctx.nestedTraversal()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_optional(ctx: TraversalMethod_optionalContext): GraphTraversal {
        return graphTraversal.optional(antlr.tvisitor.visitNestedTraversal(ctx.nestedTraversal()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_or(ctx: TraversalMethod_orContext): GraphTraversal {
        return graphTraversal.or(
            antlr.tListVisitor.visitNestedTraversalList(ctx.nestedTraversalList())
        )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_order_Empty(ctx: TraversalMethod_order_EmptyContext?): GraphTraversal {
        return graphTraversal.order()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_order_Scope(ctx: TraversalMethod_order_ScopeContext): GraphTraversal {
        return graphTraversal.order(
            TraversalEnumParser.parseTraversalEnumFromContext(
                Scope::class.java,
                ctx.getChild(2)
            )
        )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_otherV(ctx: TraversalMethod_otherVContext?): GraphTraversal {
        return graphTraversal.otherV()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_out(ctx: TraversalMethod_outContext): GraphTraversal {
        return graphTraversal.out(GenericLiteralVisitor.getStringLiteralList(ctx.stringLiteralList()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_outE(ctx: TraversalMethod_outEContext): GraphTraversal {
        return graphTraversal.outE(GenericLiteralVisitor.getStringLiteralList(ctx.stringLiteralList()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_outV(ctx: TraversalMethod_outVContext?): GraphTraversal {
        return graphTraversal.outV()
    }

    @Override
    fun visitTraversalMethod_connectedComponent(ctx: TraversalMethod_connectedComponentContext?): Traversal {
        return graphTraversal.connectedComponent()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_pageRank_Empty(ctx: TraversalMethod_pageRank_EmptyContext?): GraphTraversal {
        return graphTraversal.pageRank()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_pageRank_double(ctx: TraversalMethod_pageRank_doubleContext): GraphTraversal {
        return graphTraversal.pageRank(Double.valueOf(ctx.floatLiteral().getText()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_path(ctx: TraversalMethod_pathContext?): GraphTraversal {
        return graphTraversal.path()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_peerPressure(ctx: TraversalMethod_peerPressureContext?): GraphTraversal {
        return graphTraversal.peerPressure()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_profile_Empty(ctx: TraversalMethod_profile_EmptyContext?): GraphTraversal {
        return graphTraversal.profile()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_profile_String(ctx: TraversalMethod_profile_StringContext): GraphTraversal {
        return graphTraversal.profile(GenericLiteralVisitor.getStringLiteral(ctx.stringLiteral()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_project(ctx: TraversalMethod_projectContext): GraphTraversal {
        return if (ctx.getChildCount() === 4) {
            graphTraversal.project(GenericLiteralVisitor.getStringLiteral(ctx.stringLiteral()))
        } else {
            graphTraversal.project(
                GenericLiteralVisitor.getStringLiteral(ctx.stringLiteral()),
                GenericLiteralVisitor.getStringLiteralList(ctx.stringLiteralList())
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_properties(ctx: TraversalMethod_propertiesContext): GraphTraversal {
        return graphTraversal.properties(GenericLiteralVisitor.getStringLiteralList(ctx.stringLiteralList()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_elementMap(ctx: TraversalMethod_elementMapContext): GraphTraversal {
        return graphTraversal.elementMap(GenericLiteralVisitor.getStringLiteralList(ctx.stringLiteralList()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_propertyMap(ctx: TraversalMethod_propertyMapContext): GraphTraversal {
        return graphTraversal.propertyMap(GenericLiteralVisitor.getStringLiteralList(ctx.stringLiteralList()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_property_Cardinality_Object_Object_Object(ctx: TraversalMethod_property_Cardinality_Object_Object_ObjectContext): GraphTraversal {
        return graphTraversal.property(
            TraversalEnumParser.parseTraversalEnumFromContext(VertexProperty.Cardinality::class.java, ctx.getChild(2)),
            GenericLiteralVisitor.instance().visitGenericLiteral(ctx.genericLiteral(0)),
            GenericLiteralVisitor.instance().visitGenericLiteral(ctx.genericLiteral(1)),
            GenericLiteralVisitor.getGenericLiteralList(ctx.genericLiteralList())
        )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_property_Object_Object_Object(ctx: TraversalMethod_property_Object_Object_ObjectContext): GraphTraversal {
        return graphTraversal.property(
            GenericLiteralVisitor(antlr).visitGenericLiteral(ctx.genericLiteral(0)),
            GenericLiteralVisitor(antlr).visitGenericLiteral(ctx.genericLiteral(1)),
            GenericLiteralVisitor.getGenericLiteralList(ctx.genericLiteralList())
        )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_range_Scope_long_long(ctx: TraversalMethod_range_Scope_long_longContext): GraphTraversal {
        return graphTraversal.range(
            TraversalEnumParser.parseTraversalEnumFromContext(Scope::class.java, ctx.getChild(2)),
            Integer.valueOf(ctx.integerLiteral(0).getText()),
            Integer.valueOf(ctx.integerLiteral(1).getText())
        )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_range_long_long(ctx: TraversalMethod_range_long_longContext): GraphTraversal {
        return graphTraversal.range(
            Integer.valueOf(ctx.integerLiteral(0).getText()),
            Integer.valueOf(ctx.integerLiteral(1).getText())
        )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_sack_BiFunction(ctx: TraversalMethod_sack_BiFunctionContext): GraphTraversal {
        return graphTraversal.sack(
            TraversalEnumParser.parseTraversalEnumFromContext(
                Operator::class.java,
                ctx.getChild(2)
            )
        )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_sack_Empty(ctx: TraversalMethod_sack_EmptyContext?): GraphTraversal {
        return graphTraversal.sack()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_sample_Scope_int(ctx: TraversalMethod_sample_Scope_intContext): GraphTraversal {
        return graphTraversal.sample(
            TraversalEnumParser.parseTraversalEnumFromContext(Scope::class.java, ctx.getChild(2)),
            Integer.valueOf(ctx.integerLiteral().getText())
        )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_sample_int(ctx: TraversalMethod_sample_intContext): GraphTraversal {
        return graphTraversal.sample(Integer.valueOf(ctx.integerLiteral().getText()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_select_Column(ctx: TraversalMethod_select_ColumnContext): GraphTraversal {
        return graphTraversal.select(
            TraversalEnumParser.parseTraversalEnumFromContext(
                Column::class.java,
                ctx.getChild(2)
            )
        )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_select_Pop_String(ctx: TraversalMethod_select_Pop_StringContext): GraphTraversal {
        return graphTraversal.select(
            TraversalEnumParser.parseTraversalEnumFromContext(Pop::class.java, ctx.getChild(2)),
            GenericLiteralVisitor.getStringLiteral(ctx.stringLiteral())
        )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_select_Pop_String_String_String(ctx: TraversalMethod_select_Pop_String_String_StringContext): GraphTraversal {
        return graphTraversal.select(
            TraversalEnumParser.parseTraversalEnumFromContext(Pop::class.java, ctx.getChild(2)),
            GenericLiteralVisitor.getStringLiteral(ctx.stringLiteral(0)),
            GenericLiteralVisitor.getStringLiteral(ctx.stringLiteral(1)),
            GenericLiteralVisitor.getStringLiteralList(ctx.stringLiteralList())
        )
    }

    @Override
    fun visitTraversalMethod_select_Pop_Traversal(ctx: TraversalMethod_select_Pop_TraversalContext): GraphTraversal {
        return graphTraversal.select(
            TraversalEnumParser.parseTraversalEnumFromContext(Pop::class.java, ctx.getChild(2)),
            antlr.tvisitor.visitNestedTraversal(ctx.nestedTraversal())
        )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_select_String(ctx: TraversalMethod_select_StringContext): GraphTraversal {
        return graphTraversal.select(GenericLiteralVisitor.getStringLiteral(ctx.stringLiteral()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_select_String_String_String(ctx: TraversalMethod_select_String_String_StringContext): GraphTraversal {
        return graphTraversal.select(
            GenericLiteralVisitor.getStringLiteral(ctx.stringLiteral(0)),
            GenericLiteralVisitor.getStringLiteral(ctx.stringLiteral(1)),
            GenericLiteralVisitor.getStringLiteralList(ctx.stringLiteralList())
        )
    }

    @Override
    fun visitTraversalMethod_select_Traversal(ctx: TraversalMethod_select_TraversalContext): GraphTraversal {
        return graphTraversal.select(antlr.tvisitor.visitNestedTraversal(ctx.nestedTraversal()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_sideEffect(ctx: TraversalMethod_sideEffectContext): GraphTraversal {
        return graphTraversal.sideEffect(antlr.tvisitor.visitNestedTraversal(ctx.nestedTraversal()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_simplePath(ctx: TraversalMethod_simplePathContext?): GraphTraversal {
        return graphTraversal.simplePath()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_skip_Scope_long(ctx: TraversalMethod_skip_Scope_longContext): GraphTraversal {
        return graphTraversal.skip(
            TraversalEnumParser.parseTraversalEnumFromContext(Scope::class.java, ctx.getChild(2)),
            Integer.valueOf(ctx.integerLiteral().getText())
        )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_skip_long(ctx: TraversalMethod_skip_longContext): GraphTraversal {
        return graphTraversal.skip(Integer.valueOf(ctx.integerLiteral().getText()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_store(ctx: TraversalMethod_storeContext): GraphTraversal {
        return graphTraversal.store(GenericLiteralVisitor.getStringLiteral(ctx.stringLiteral()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_subgraph(ctx: TraversalMethod_subgraphContext): GraphTraversal {
        return graphTraversal.subgraph(GenericLiteralVisitor.getStringLiteral(ctx.stringLiteral()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_sum_Empty(ctx: TraversalMethod_sum_EmptyContext?): GraphTraversal {
        return graphTraversal.sum()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_sum_Scope(ctx: TraversalMethod_sum_ScopeContext): GraphTraversal {
        return graphTraversal.sum(TraversalEnumParser.parseTraversalEnumFromContext(Scope::class.java, ctx.getChild(2)))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_tail_Empty(ctx: TraversalMethod_tail_EmptyContext?): GraphTraversal {
        return graphTraversal.tail()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_tail_Scope(ctx: TraversalMethod_tail_ScopeContext): GraphTraversal {
        return graphTraversal.tail(
            TraversalEnumParser.parseTraversalEnumFromContext(
                Scope::class.java,
                ctx.getChild(2)
            )
        )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_tail_Scope_long(ctx: TraversalMethod_tail_Scope_longContext): GraphTraversal {
        return graphTraversal.tail(
            TraversalEnumParser.parseTraversalEnumFromContext(Scope::class.java, ctx.getChild(2)),
            Integer.valueOf(ctx.integerLiteral().getText())
        )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_tail_long(ctx: TraversalMethod_tail_longContext): GraphTraversal {
        return graphTraversal.tail(Integer.valueOf(ctx.integerLiteral().getText()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_timeLimit(ctx: TraversalMethod_timeLimitContext): GraphTraversal {
        return graphTraversal.timeLimit(Integer.valueOf(ctx.integerLiteral().getText()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_times(ctx: TraversalMethod_timesContext): GraphTraversal {
        return graphTraversal.times(Integer.valueOf(ctx.integerLiteral().getText()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_toE(ctx: TraversalMethod_toEContext): GraphTraversal {
        return graphTraversal.toE(
            TraversalEnumParser.parseTraversalEnumFromContext(Direction::class.java, ctx.getChild(2)),
            GenericLiteralVisitor.getStringLiteralList(ctx.stringLiteralList())
        )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_toV(ctx: TraversalMethod_toVContext): GraphTraversal {
        return graphTraversal.toV(
            TraversalEnumParser.parseTraversalEnumFromContext(
                Direction::class.java,
                ctx.getChild(2)
            )
        )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_to_Direction_String(ctx: TraversalMethod_to_Direction_StringContext): GraphTraversal {
        return graphTraversal.to(
            TraversalEnumParser.parseTraversalEnumFromContext(Direction::class.java, ctx.getChild(2)),
            GenericLiteralVisitor.getStringLiteralList(ctx.stringLiteralList())
        )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_to_String(ctx: TraversalMethod_to_StringContext): GraphTraversal {
        return graphTraversal.to(GenericLiteralVisitor.getStringLiteral(ctx.stringLiteral()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_to_Traversal(ctx: TraversalMethod_to_TraversalContext): GraphTraversal {
        return graphTraversal.to(antlr.tvisitor.visitNestedTraversal(ctx.nestedTraversal()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_tree_Empty(ctx: TraversalMethod_tree_EmptyContext?): GraphTraversal {
        return graphTraversal.tree()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_tree_String(ctx: TraversalMethod_tree_StringContext): GraphTraversal {
        return graphTraversal.tree(GenericLiteralVisitor.getStringLiteral(ctx.stringLiteral()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_unfold(ctx: TraversalMethod_unfoldContext?): GraphTraversal {
        return graphTraversal.unfold()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_union(ctx: TraversalMethod_unionContext): GraphTraversal {
        return graphTraversal.union(
            antlr.tListVisitor.visitNestedTraversalList(ctx.nestedTraversalList())
        )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_until_Predicate(ctx: TraversalMethod_until_PredicateContext): GraphTraversal {
        return graphTraversal.until(
            TraversalPredicateVisitor.instance().visitTraversalPredicate(ctx.traversalPredicate())
        )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_until_Traversal(ctx: TraversalMethod_until_TraversalContext): GraphTraversal {
        return graphTraversal.until(antlr.tvisitor.visitNestedTraversal(ctx.nestedTraversal()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_value(ctx: TraversalMethod_valueContext?): GraphTraversal {
        return graphTraversal.value()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_valueMap_String(ctx: TraversalMethod_valueMap_StringContext): GraphTraversal {
        return graphTraversal.valueMap(GenericLiteralVisitor.getStringLiteralList(ctx.stringLiteralList()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_valueMap_boolean_String(ctx: TraversalMethod_valueMap_boolean_StringContext): GraphTraversal {
        return if (ctx.getChildCount() === 4) {
            graphTraversal.valueMap(Boolean.valueOf(ctx.booleanLiteral().getText()))
        } else {
            graphTraversal.valueMap(
                Boolean.valueOf(ctx.booleanLiteral().getText()),
                GenericLiteralVisitor.getStringLiteralList(ctx.stringLiteralList())
            )
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_values(ctx: TraversalMethod_valuesContext): GraphTraversal {
        return graphTraversal.values(GenericLiteralVisitor.getStringLiteralList(ctx.stringLiteralList()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_where_P(ctx: TraversalMethod_where_PContext): GraphTraversal {
        return graphTraversal.where(
            TraversalPredicateVisitor.instance().visitTraversalPredicate(ctx.traversalPredicate())
        )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_where_String_P(ctx: TraversalMethod_where_String_PContext): GraphTraversal {
        return graphTraversal.where(
            GenericLiteralVisitor.getStringLiteral(ctx.stringLiteral()),
            TraversalPredicateVisitor.instance().visitTraversalPredicate(ctx.traversalPredicate())
        )
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_where_Traversal(ctx: TraversalMethod_where_TraversalContext): GraphTraversal {
        return graphTraversal.where(antlr.tvisitor.visitNestedTraversal(ctx.nestedTraversal()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_math(ctx: TraversalMethod_mathContext): GraphTraversal {
        return graphTraversal.math(GenericLiteralVisitor.getStringLiteral(ctx.stringLiteral()))
    }

    /**
     * {@inheritDoc}
     */
    @Override
    fun visitTraversalMethod_option_Predicate_Traversal(ctx: TraversalMethod_option_Predicate_TraversalContext): Traversal {
        return graphTraversal.option(
            TraversalPredicateVisitor.instance().visitTraversalPredicate(ctx.traversalPredicate()),
            antlr.tvisitor.visitNestedTraversal(ctx.nestedTraversal())
        )
    }

    @Override
    fun visitTraversalMethod_from_Vertex(ctx: TraversalMethod_from_VertexContext): Traversal {
        return graphTraversal.from(StructureElementVisitor.instance().visitStructureVertex(ctx.structureVertex()))
    }

    @Override
    fun visitTraversalMethod_to_Vertex(ctx: TraversalMethod_to_VertexContext): Traversal {
        return graphTraversal.to(StructureElementVisitor.instance().visitStructureVertex(ctx.structureVertex()))
    }

    fun getNestedTraversalList(ctx: NestedTraversalListContext): Array<GraphTraversal> {
        return ctx.nestedTraversalExpr().nestedTraversal()
            .stream()
            .map(this::visitNestedTraversal)
            .toArray { _Dummy_.__Array__() }
    }
}