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

import org.apache.tinkerpop4.gremlin.process.computer.traversal.step.map.ConnectedComponent

/**
 * Covers `String` oriented constants used as arguments to [GraphTraversal.with] steps.
 */
class GremlinStringConstantsVisitor private constructor() : GremlinBaseVisitor<Object?>() {
    @Override
    fun visitGremlinStringConstants(ctx: GremlinStringConstantsContext?): Object {
        return visitChildren(ctx)
    }

    @Override
    fun visitConnectedComponentStringConstant(ctx: ConnectedComponentStringConstantContext?): Object {
        return visitChildren(ctx)
    }

    @Override
    fun visitPageRankStringConstants(ctx: PageRankStringConstantsContext?): Object {
        return visitChildren(ctx)
    }

    @Override
    fun visitPeerPressureStringConstants(ctx: PeerPressureStringConstantsContext?): Object {
        return visitChildren(ctx)
    }

    @Override
    fun visitShortestPathStringConstants(ctx: ShortestPathStringConstantsContext?): Object {
        return visitChildren(ctx)
    }

    @Override
    fun visitWithOptionsStringConstants(ctx: WithOptionsStringConstantsContext?): Object {
        return visitChildren(ctx)
    }

    @Override
    fun visitIoOptionsStringConstants(ctx: IoOptionsStringConstantsContext?): Object {
        return visitChildren(ctx)
    }

    @Override
    fun visitGremlinStringConstants_connectedComponentStringConstants_edges(ctx: GremlinStringConstants_connectedComponentStringConstants_edgesContext?): Object {
        return ConnectedComponent.edges
    }

    @Override
    fun visitGremlinStringConstants_connectedComponentStringConstants_component(ctx: GremlinStringConstants_connectedComponentStringConstants_componentContext?): Object {
        return ConnectedComponent.component
    }

    @Override
    fun visitGremlinStringConstants_connectedComponentStringConstants_propertyName(ctx: GremlinStringConstants_connectedComponentStringConstants_propertyNameContext?): Object {
        return ConnectedComponent.propertyName
    }

    @Override
    fun visitGremlinStringConstants_pageRankStringConstants_edges(ctx: GremlinStringConstants_pageRankStringConstants_edgesContext?): Object {
        return PageRank.edges
    }

    @Override
    fun visitGremlinStringConstants_pageRankStringConstants_times(ctx: GremlinStringConstants_pageRankStringConstants_timesContext?): Object {
        return PageRank.times
    }

    @Override
    fun visitGremlinStringConstants_pageRankStringConstants_propertyName(ctx: GremlinStringConstants_pageRankStringConstants_propertyNameContext?): Object {
        return PageRank.propertyName
    }

    @Override
    fun visitGremlinStringConstants_peerPressureStringConstants_edges(ctx: GremlinStringConstants_peerPressureStringConstants_edgesContext?): Object {
        return PeerPressure.edges
    }

    @Override
    fun visitGremlinStringConstants_peerPressureStringConstants_times(ctx: GremlinStringConstants_peerPressureStringConstants_timesContext?): Object {
        return PeerPressure.times
    }

    @Override
    fun visitGremlinStringConstants_peerPressureStringConstants_propertyName(ctx: GremlinStringConstants_peerPressureStringConstants_propertyNameContext?): Object {
        return PeerPressure.propertyName
    }

    @Override
    fun visitGremlinStringConstants_shortestPathStringConstants_target(ctx: GremlinStringConstants_shortestPathStringConstants_targetContext?): Object {
        return ShortestPath.target
    }

    @Override
    fun visitGremlinStringConstants_shortestPathStringConstants_edges(ctx: GremlinStringConstants_shortestPathStringConstants_edgesContext?): Object {
        return ShortestPath.edges
    }

    @Override
    fun visitGremlinStringConstants_shortestPathStringConstants_distance(ctx: GremlinStringConstants_shortestPathStringConstants_distanceContext?): Object {
        return ShortestPath.distance
    }

    @Override
    fun visitGremlinStringConstants_shortestPathStringConstants_maxDistance(ctx: GremlinStringConstants_shortestPathStringConstants_maxDistanceContext?): Object {
        return ShortestPath.maxDistance
    }

    @Override
    fun visitGremlinStringConstants_shortestPathStringConstants_includeEdges(ctx: GremlinStringConstants_shortestPathStringConstants_includeEdgesContext?): Object {
        return ShortestPath.includeEdges
    }

    @Override
    fun visitGremlinStringConstants_withOptionsStringConstants_tokens(ctx: GremlinStringConstants_withOptionsStringConstants_tokensContext?): Object {
        return WithOptions.tokens
    }

    @Override
    fun visitGremlinStringConstants_withOptionsStringConstants_none(ctx: GremlinStringConstants_withOptionsStringConstants_noneContext?): Object {
        return WithOptions.none
    }

    @Override
    fun visitGremlinStringConstants_withOptionsStringConstants_ids(ctx: GremlinStringConstants_withOptionsStringConstants_idsContext?): Object {
        return WithOptions.ids
    }

    @Override
    fun visitGremlinStringConstants_withOptionsStringConstants_labels(ctx: GremlinStringConstants_withOptionsStringConstants_labelsContext?): Object {
        return WithOptions.labels
    }

    @Override
    fun visitGremlinStringConstants_withOptionsStringConstants_keys(ctx: GremlinStringConstants_withOptionsStringConstants_keysContext?): Object {
        return WithOptions.keys
    }

    @Override
    fun visitGremlinStringConstants_withOptionsStringConstants_values(ctx: GremlinStringConstants_withOptionsStringConstants_valuesContext?): Object {
        return WithOptions.values
    }

    @Override
    fun visitGremlinStringConstants_withOptionsStringConstants_all(ctx: GremlinStringConstants_withOptionsStringConstants_allContext?): Object {
        return WithOptions.all
    }

    @Override
    fun visitGremlinStringConstants_withOptionsStringConstants_indexer(ctx: GremlinStringConstants_withOptionsStringConstants_indexerContext?): Object {
        return WithOptions.indexer
    }

    @Override
    fun visitGremlinStringConstants_withOptionsStringConstants_list(ctx: GremlinStringConstants_withOptionsStringConstants_listContext?): Object {
        return WithOptions.list
    }

    @Override
    fun visitGremlinStringConstants_withOptionsStringConstants_map(ctx: GremlinStringConstants_withOptionsStringConstants_mapContext?): Object {
        return WithOptions.map
    }

    @Override
    fun visitGremlinStringConstants_ioOptionsStringConstants_reader(ctx: GremlinStringConstants_ioOptionsStringConstants_readerContext?): Object {
        return IO.reader
    }

    @Override
    fun visitGremlinStringConstants_ioOptionsStringConstants_writer(ctx: GremlinStringConstants_ioOptionsStringConstants_writerContext?): Object {
        return IO.writer
    }

    @Override
    fun visitGremlinStringConstants_ioOptionsStringConstants_gryo(ctx: GremlinStringConstants_ioOptionsStringConstants_gryoContext?): Object {
        return IO.gryo
    }

    @Override
    fun visitGremlinStringConstants_ioOptionsStringConstants_graphson(ctx: GremlinStringConstants_ioOptionsStringConstants_graphsonContext?): Object {
        return IO.graphson
    }

    @Override
    fun visitGremlinStringConstants_ioOptionsStringConstants_graphml(ctx: GremlinStringConstants_ioOptionsStringConstants_graphmlContext?): Object {
        return IO.graphml
    }

    companion object {
        private var instance: GremlinStringConstantsVisitor? = null
        fun instance(): GremlinStringConstantsVisitor? {
            if (instance == null) {
                instance = GremlinStringConstantsVisitor()
            }
            return instance
        }

        @Deprecated("As of release 3.5.2, replaced by {@link #instance()}")
        fun getInstance(): GremlinStringConstantsVisitor? {
            return instance()
        }
    }
}