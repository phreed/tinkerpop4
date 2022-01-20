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
package org.apache.tinkerpop4.gremlin.structure.io

import org.apache.tinkerpop4.gremlin.structure.Graph

/**
 * `GraphMigrator` takes the data in one graph and pipes it to another graph.  Uses the [GryoReader]
 * and [GryoWriter] by default.  Note that this utility is meant as a convenience for "small" graph migrations.
 *
 * @author Alex Averbuch (alex.averbuch@gmail.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
object GraphMigrator {
    /**
     * Use Gryo to pipe the data from one graph to another graph.  Uses readers and writers generated from each
     * [Graph] via the [Graph.io] method.
     */
    @Throws(IOException::class)
    fun migrateGraph(fromGraph: Graph, toGraph: Graph) {
        migrateGraph(fromGraph, toGraph, fromGraph.io(gryo()).reader().create(), toGraph.io(gryo()).writer().create())
    }

    /**
     * Pipe the data from one graph to another graph.  It is important that the reader and writer utilize the
     * same format.
     *
     * @param fromGraph the graph to take data from.
     * @param toGraph   the graph to take data to.
     * @param reader    reads from the graph written by the writer.
     * @param writer    writes the graph to be read by the reader.
     * @throws IOException thrown if there is an error in steam between the two graphs.
     */
    @Throws(IOException::class)
    fun migrateGraph(
        fromGraph: Graph, toGraph: Graph,
        reader: GraphReader, writer: GraphWriter
    ) {
        val inPipe = PipedInputStream(1024)
        val outPipe: PipedOutputStream = object : PipedOutputStream(inPipe) {
            @Override
            @Throws(IOException::class)
            fun close() {
                while (inPipe.available() > 0) {
                    try {
                        Thread.sleep(500)
                    } catch (e: InterruptedException) {
                        // do nothing
                    }
                }
                super.close()
            }
        }
        Thread {
            try {
                writer.writeGraph(outPipe, fromGraph)
                outPipe.flush()
                outPipe.close()
            } catch (e: IOException) {
                throw RuntimeException(e)
            } finally {
                if (fromGraph.features().graph().supportsTransactions()) fromGraph.tx().rollback()
                if (toGraph.features().graph().supportsTransactions()) toGraph.tx().rollback()
            }
        }.start()
        reader.readGraph(inPipe, toGraph)
    }
}