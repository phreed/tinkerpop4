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
package org.apache.tinkerpop4.gremlin.structure.io.graphson;

import org.apache.tinkerpop.shaded.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public abstract class AbstractGraphSONTest {

    public static <T> T serializeDeserialize(final ObjectMapper mapper, final Object o, final Class<T> clazz) throws Exception {
        try (final ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            mapper.writeValue(stream, o);
            try (final InputStream inputStream = new ByteArrayInputStream(stream.toByteArray())) {
                return mapper.readValue(inputStream, clazz);
            }
        }
    }

    public static <T> T serializeDeserializeAuto(final ObjectMapper mapper, final Object o) throws Exception {
        try (final ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            mapper.writeValue(stream, o);

            try (final InputStream inputStream = new ByteArrayInputStream(stream.toByteArray())) {
                // Object.class is the wildcard that triggers the auto discovery.
                return (T)mapper.readValue(inputStream, Object.class);
            }
        }
    }
}
