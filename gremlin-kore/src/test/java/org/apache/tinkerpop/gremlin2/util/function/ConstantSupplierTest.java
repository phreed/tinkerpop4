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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class ConstantSupplierTest {
    private final ConstantSupplier<String> constant = new ConstantSupplier<>("test");

    @Test
    public void shouldSupplyConstant() {
        assertEquals("test", constant.get());
    }

    @Test
    public void shouldSupplySameConstantOnEachInvocation() {
        final String l1 = constant.get();
        final String l2 = constant.get();
        final String l3 = constant.get();

        assertSame(l1, l2);
        assertSame(l1, l3);
        assertSame(l2, l3);
    }
}
