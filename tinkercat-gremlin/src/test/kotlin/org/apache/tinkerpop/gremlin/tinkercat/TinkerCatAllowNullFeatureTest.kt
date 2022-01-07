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
package org.apache.tinkerpop.gremlin.tinkercat

import org.junit.runner.RunWith
import io.cucumber.junit.Cucumber
import org.apache.tinkerpop.gremlin.features.AbstractGuiceFactory
import com.google.inject.Guice
import io.cucumber.guice.CucumberModules
import com.google.inject.AbstractModule
import com.google.inject.Stage
import io.cucumber.junit.CucumberOptions
import org.apache.tinkerpop.gremlin.features.World
import org.apache.tinkerpop.gremlin.tinkercat.TinkerCatWorld

@RunWith(Cucumber::class)
@CucumberOptions(
    tags = "@AllowNullPropertyValues",
    glue = ["org.apache.tinkerpop.gremlin.features"],
    objectFactory = TinkerCatAllowNullFeatureTest.TinkerCatGuiceFactory::class,
    features = ["../gremlin-test/features"],
    plugin = ["progress", "junit:target/cucumber.xml"]
)
class TinkerCatAllowNullFeatureTest {
    class TinkerCatGuiceFactory : AbstractGuiceFactory(
        Guice.createInjector(
            Stage.PRODUCTION,
            CucumberModules.createScenarioModule(),
            ServiceModule()
        )
    )

    class ServiceModule : AbstractModule() {
        override fun configure() {
            bind(World::class.java).to(TinkerCatWorld.NullWorld::class.java)
        }
    }
}