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
package org.apache.tinkerpop4.gremlin.jsr223;

import static org.junit.Assert.assertSame;

import org.apache.tinkerpop4.gremlin.AssertHelper;
import org.junit.Test;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class ScriptEngineCacheTest {

    @Test
    public void shouldBeUtilityClass() throws Exception {
        AssertHelper.assertIsUtilityClass(ScriptEngineCache.class);
    }

    @Test
    public void shouldGetEngineFromCache() {
        assertSame(ScriptEngineCache.get("nashorn"), ScriptEngineCache.get("nashorn"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowWhenScripEngineDoesNotExist() {
        ScriptEngineCache.get("junk-that-no-one-would-ever-call-a-script-engine-83939473298432");
    }

}
