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
package org.apache.tinkerpop4.gremlin.util.iterator;

import org.apache.tinkerpop4.gremlin.process.traversal.util.FastNoSuchElementException;
import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class DoubleIteratorTest {
    @Test
    public void shouldIterateAnArray() {
        final Iterator<String> itty = new DoubleIterator<>("test1", "test2");
        assertEquals("test1", itty.next());
        assertEquals("test2", itty.next());

        assertFalse(itty.hasNext());
    }

    @Test(expected = FastNoSuchElementException.class)
    public void shouldThrowFastNoSuchElementException() {
        final Iterator<String> itty = new DoubleIterator<>("test1", "test2");
        itty.next();
        itty.next();
        itty.next();
    }
}
