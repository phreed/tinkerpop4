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

import org.apache.tinkerpop4.gremlin.process.traversal.Traversal

class TraversalStrategyVisitor(tvisitor: GremlinBaseVisitor<Traversal?>) : GremlinBaseVisitor<TraversalStrategy?>() {
    protected val tvisitor: GremlinBaseVisitor<Traversal>

    init {
        this.tvisitor = tvisitor
    }

    @Override
    fun visitTraversalStrategy(ctx: TraversalStrategyContext): TraversalStrategy {
        // child count of one implies init syntax for the singleton constructed strategies. otherwise, it will
        // fall back to the Builder methods for construction
        if (ctx.getChildCount() === 1) {
            val strategyName: String = ctx.getChild(0).getText()
            if (strategyName.equals(ReadOnlyStrategy::class.java.getSimpleName())) return ReadOnlyStrategy.instance() else if (strategyName.equals(
                    ProductiveByStrategy::class.java.getSimpleName()
                )
            ) return ProductiveByStrategy.instance()
        } else if (ctx.getChild(0).getText().equals("new")) {
            val strategyName: String = ctx.getChild(1).getText()
            if (strategyName.equals(PartitionStrategy::class.java.getSimpleName())) return getPartitionStrategy(ctx.traversalStrategyArgs_PartitionStrategy()) else if (strategyName.equals(
                    ReservedKeysVerificationStrategy::class.java.getSimpleName()
                )
            ) return getReservedKeysVerificationStrategy(ctx.traversalStrategyArgs_ReservedKeysVerificationStrategy()) else if (strategyName.equals(
                    EdgeLabelVerificationStrategy::class.java.getSimpleName()
                )
            ) return getEdgeLabelVerificationStrategy(ctx.traversalStrategyArgs_EdgeLabelVerificationStrategy()) else if (strategyName.equals(
                    SubgraphStrategy::class.java.getSimpleName()
                )
            ) return getSubgraphStrategy(ctx.traversalStrategyArgs_SubgraphStrategy()) else if (strategyName.equals(
                    SeedStrategy::class.java.getSimpleName()
                )
            ) return SeedStrategy(Long.parseLong(ctx.integerLiteral().getText())) else if (strategyName.equals(
                    ProductiveByStrategy::class.java.getSimpleName()
                )
            ) return getProductiveByStrategy(ctx.traversalStrategyArgs_ProductiveByStrategy())
        }
        throw IllegalStateException("Unexpected TraversalStrategy specification - " + ctx.getText())
    }

    private fun getSubgraphStrategy(ctxs: List<TraversalStrategyArgs_SubgraphStrategyContext>): SubgraphStrategy {
        val builder: SubgraphStrategy.Builder = SubgraphStrategy.build()
        ctxs.forEach { ctx ->
            when (ctx.getChild(0).getText()) {
                SubgraphStrategy.VERTICES -> builder.vertices(tvisitor.visitNestedTraversal(ctx.nestedTraversal()))
                SubgraphStrategy.EDGES -> builder.edges(tvisitor.visitNestedTraversal(ctx.nestedTraversal()))
                SubgraphStrategy.VERTEX_PROPERTIES -> builder.vertexProperties(tvisitor.visitNestedTraversal(ctx.nestedTraversal()))
                SubgraphStrategy.CHECK_ADJACENT_VERTICES -> builder.checkAdjacentVertices(
                    GenericLiteralVisitor.getBooleanLiteral(
                        ctx.booleanLiteral()
                    )
                )
            }
        }
        return builder.create()
    }

    companion object {
        private fun getEdgeLabelVerificationStrategy(ctxs: List<TraversalStrategyArgs_EdgeLabelVerificationStrategyContext>?): EdgeLabelVerificationStrategy {
            if (null == ctxs || ctxs.isEmpty()) return EdgeLabelVerificationStrategy.build().create()
            val builder: EdgeLabelVerificationStrategy.Builder = EdgeLabelVerificationStrategy.build()
            ctxs.forEach { ctx ->
                when (ctx.getChild(0).getText()) {
                    AbstractWarningVerificationStrategy.LOG_WARNING -> builder.logWarning(
                        GenericLiteralVisitor.getBooleanLiteral(
                            ctx.booleanLiteral()
                        )
                    )
                    AbstractWarningVerificationStrategy.THROW_EXCEPTION -> builder.throwException(
                        GenericLiteralVisitor.getBooleanLiteral(
                            ctx.booleanLiteral()
                        )
                    )
                }
            }
            return builder.create()
        }

        private fun getReservedKeysVerificationStrategy(ctxs: List<TraversalStrategyArgs_ReservedKeysVerificationStrategyContext>?): ReservedKeysVerificationStrategy {
            if (null == ctxs || ctxs.isEmpty()) return ReservedKeysVerificationStrategy.build().create()
            val builder: ReservedKeysVerificationStrategy.Builder = ReservedKeysVerificationStrategy.build()
            ctxs.forEach { ctx ->
                when (ctx.getChild(0).getText()) {
                    AbstractWarningVerificationStrategy.LOG_WARNING -> builder.logWarning(
                        GenericLiteralVisitor.getBooleanLiteral(
                            ctx.booleanLiteral()
                        )
                    )
                    AbstractWarningVerificationStrategy.THROW_EXCEPTION -> builder.throwException(
                        GenericLiteralVisitor.getBooleanLiteral(
                            ctx.booleanLiteral()
                        )
                    )
                    ReservedKeysVerificationStrategy.KEYS -> builder.reservedKeys(
                        HashSet(
                            Arrays.asList(
                                GenericLiteralVisitor.getStringLiteralList(ctx.stringLiteralList())
                            )
                        )
                    )
                }
            }
            return builder.create()
        }

        private fun getPartitionStrategy(ctxs: List<TraversalStrategyArgs_PartitionStrategyContext>): PartitionStrategy {
            val builder: PartitionStrategy.Builder = PartitionStrategy.build()
            ctxs.forEach { ctx ->
                when (ctx.getChild(0).getText()) {
                    PartitionStrategy.INCLUDE_META_PROPERTIES -> builder.includeMetaProperties(
                        GenericLiteralVisitor.getBooleanLiteral(
                            ctx.booleanLiteral()
                        )
                    )
                    PartitionStrategy.READ_PARTITIONS -> builder.readPartitions(
                        Arrays.asList(
                            GenericLiteralVisitor.getStringLiteralList(
                                ctx.stringLiteralList()
                            )
                        )
                    )
                    PartitionStrategy.WRITE_PARTITION -> builder.writePartition(
                        GenericLiteralVisitor.getStringLiteral(
                            ctx.stringLiteral()
                        )
                    )
                    PartitionStrategy.PARTITION_KEY -> builder.partitionKey(GenericLiteralVisitor.getStringLiteral(ctx.stringLiteral()))
                }
            }
            return builder.create()
        }

        private fun getProductiveByStrategy(ctx: TraversalStrategyArgs_ProductiveByStrategyContext): ProductiveByStrategy {
            val builder: ProductiveByStrategy.Builder = ProductiveByStrategy.build()
            builder.productiveKeys(Arrays.asList(GenericLiteralVisitor.getStringLiteralList(ctx.stringLiteralList())))
            return builder.create()
        }
    }
}