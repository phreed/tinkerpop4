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
package org.apache.tinkerpop4.gremlin.util.function;

import org.apache.tinkerpop4.gremlin.process.traversal.step.map.MeanGlobalStep;
import org.junit.Test;

import java.util.HashSet;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class MeanNumberSupplierTest {
    @Test
    public void shouldSupplyMeanNumberInstance() {
        assertThat(MeanNumberSupplier.instance().get(), instanceOf(MeanGlobalStep.MeanNumber.class));
    }

    @Test
    public void shouldSupplyNewMeanNumberOnEachInvocation() {
        final MeanGlobalStep.MeanNumber l1 = MeanNumberSupplier.instance().get();
        final MeanGlobalStep.MeanNumber l2 = MeanNumberSupplier.instance().get();
        final MeanGlobalStep.MeanNumber l3 = MeanNumberSupplier.instance().get();

        assertNotSame(l1, l2);
        assertNotSame(l1, l3);
        assertNotSame(l2, l3);
    }
}
