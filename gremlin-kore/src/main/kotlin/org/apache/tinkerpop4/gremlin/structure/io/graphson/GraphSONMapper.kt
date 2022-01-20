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
package org.apache.tinkerpop4.gremlin.structure.io.graphson

import org.apache.tinkerpop4.gremlin.structure.Graph

/**
 * An extension to the standard Jackson `ObjectMapper` which automatically registers the standard
 * [GraphSONModule] for serializing [Graph] elements.  This class
 * can be used for generalized JSON serialization tasks that require meeting GraphSON standards.
 *
 *
 * [Graph] implementations providing an [IoRegistry] should register their `SimpleModule`
 * implementations to it as follows:
 * <pre>
 * `public class MyGraphIoRegistry extends AbstractIoRegistry {
 * public MyGraphIoRegistry() {
 * register(GraphSONIo.class, null, new MyGraphSimpleModule());
 * }
 * }
` *
</pre> *
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class GraphSONMapper private constructor(builder: Builder) : Mapper<ObjectMapper?> {
    private val customModules: List<SimpleModule>
    private val loadCustomSerializers: Boolean
    private val normalize: Boolean
    private val version: GraphSONVersion
    private var typeInfo: TypeInfo? = null

    init {
        customModules = builder.customModules
        loadCustomSerializers = builder.loadCustomModules
        normalize = builder.normalize
        version = builder.version
        if (null == builder.typeInfo) typeInfo =
            if (builder.version === GraphSONVersion.V1_0) TypeInfo.NO_TYPES else TypeInfo.PARTIAL_TYPES else typeInfo =
            builder.typeInfo
    }

    @Override
    fun createMapper(): ObjectMapper {
        val om = ObjectMapper()
        om.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
        val graphSONModule: GraphSONModule = version.getBuilder().create(normalize)
        om.registerModule(graphSONModule)
        customModules.forEach(om::registerModule)

        // plugin external serialization modules
        if (loadCustomSerializers) om.findAndRegisterModules()

        // graphson 3.0 only allows type - there is no option to remove embedded types
        if (version === GraphSONVersion.V3_0 && typeInfo === TypeInfo.NO_TYPES) throw IllegalStateException(
            String.format(
                "GraphSON 3.0 does not support %s",
                TypeInfo.NO_TYPES
            )
        )
        if (version === GraphSONVersion.V3_0 || version === GraphSONVersion.V2_0 && typeInfo !== TypeInfo.NO_TYPES) {
            val graphSONTypeIdResolver = GraphSONTypeIdResolver()
            val typer: TypeResolverBuilder = GraphSONTypeResolverBuilder(version)
                .typesEmbedding(typeInfo)
                .valuePropertyName(GraphSONTokens.VALUEPROP)
                .init(JsonTypeInfo.Id.CUSTOM, graphSONTypeIdResolver)
                .typeProperty(GraphSONTokens.VALUETYPE)

            // Registers native Java types that are supported by Jackson
            registerJavaBaseTypes(graphSONTypeIdResolver)

            // Registers the GraphSON Module's types
            graphSONModule.getTypeDefinitions().forEach { targetClass, typeId ->
                graphSONTypeIdResolver.addCustomType(
                    String.format("%s:%s", graphSONModule.getTypeNamespace(), typeId), targetClass
                )
            }

            // Register types to typeResolver for the Custom modules
            customModules.forEach { e ->
                if (e is TinkerPopJacksonModule) {
                    val mod: TinkerPopJacksonModule = e as TinkerPopJacksonModule
                    val moduleTypeDefinitions: Map<Class, String> = mod.getTypeDefinitions()
                    if (moduleTypeDefinitions != null) {
                        if (mod.getTypeNamespace() == null || mod.getTypeNamespace()
                                .isEmpty()
                        ) throw IllegalStateException(
                            "Cannot specify a module for GraphSON 2.0 with type definitions but without a type Domain. " +
                                    "If no specific type domain is required, use Gremlin's default domain, \"gremlin\" but there may be collisions."
                        )
                        moduleTypeDefinitions.forEach { targetClass, typeId ->
                            graphSONTypeIdResolver.addCustomType(
                                String.format("%s:%s", mod.getTypeNamespace(), typeId), targetClass
                            )
                        }
                    }
                }
            }
            om.setDefaultTyping(typer)
        } else if (version === GraphSONVersion.V1_0 || version === GraphSONVersion.V2_0) {
            if (typeInfo === TypeInfo.PARTIAL_TYPES) {
                val typer: TypeResolverBuilder<*> = StdTypeResolverBuilder()
                    .init(JsonTypeInfo.Id.CLASS, null)
                    .inclusion(JsonTypeInfo.As.PROPERTY)
                    .typeProperty(GraphSONTokens.CLASS)
                om.setDefaultTyping(typer)
            }
        } else {
            throw IllegalStateException("Unknown GraphSONVersion : $version")
        }

        // this provider toStrings all unknown classes and converts keys in Map objects that are Object to String.
        val provider: DefaultSerializerProvider = GraphSONSerializerProvider(version)
        om.setSerializerProvider(provider)
        if (normalize) om.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)

        // keep streams open to accept multiple values (e.g. multiple vertices)
        om.getFactory().disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET)
        return om
    }

    fun getVersion(): GraphSONVersion {
        return version
    }

    fun getTypeInfo(): TypeInfo? {
        return typeInfo
    }

    private fun registerJavaBaseTypes(graphSONTypeIdResolver: GraphSONTypeIdResolver) {
        Arrays.asList(
            UUID::class.java,
            Class::class.java,
            Calendar::class.java,
            Date::class.java,
            TimeZone::class.java,
            Timestamp::class.java
        ).forEach { e ->
            graphSONTypeIdResolver.addCustomType(
                String.format(
                    "%s:%s",
                    GraphSONTokens.GREMLIN_TYPE_NAMESPACE,
                    e.getSimpleName()
                ), e
            )
        }
    }

    class Builder private constructor() : Mapper.Builder<Builder?> {
        private val customModules: List<SimpleModule> = ArrayList()
        private var loadCustomModules = false
        private var normalize = false
        private val registries: List<IoRegistry> = ArrayList()
        private var version: GraphSONVersion = GraphSONVersion.V3_0

        /**
         * GraphSON 2.0/3.0 should have types activated by default (3.0 does not have a typeless option), and 1.0
         * should use no types by default.
         */
        private var typeInfo: TypeInfo? = null

        /**
         * {@inheritDoc}
         */
        @Override
        fun addRegistry(registry: IoRegistry?): Builder {
            registries.add(registry)
            return this
        }

        /**
         * Set the version of GraphSON to use. The default is [GraphSONVersion.V3_0].
         */
        fun version(version: GraphSONVersion): Builder {
            this.version = version
            return this
        }

        /**
         * Set the version of GraphSON to use.
         */
        fun version(version: String?): Builder {
            this.version = GraphSONVersion.valueOf(version)
            return this
        }

        /**
         * Supply a mapper module for serialization/deserialization.
         */
        fun addCustomModule(custom: SimpleModule?): Builder {
            customModules.add(custom)
            return this
        }

        /**
         * Try to load `SimpleModule` instances from the current classpath.  These are loaded in addition to
         * the one supplied to the [.addCustomModule];
         */
        fun loadCustomModules(loadCustomModules: Boolean): Builder {
            this.loadCustomModules = loadCustomModules
            return this
        }

        /**
         * Forces keys to be sorted.
         */
        fun normalize(normalize: Boolean): Builder {
            this.normalize = normalize
            return this
        }

        /**
         * Specify if the values are going to be typed or not, and at which level.
         *
         * The level can be [TypeInfo.NO_TYPES] or [TypeInfo.PARTIAL_TYPES], and could be extended in the
         * future.
         */
        fun typeInfo(typeInfo: TypeInfo?): Builder {
            this.typeInfo = typeInfo
            return this
        }

        fun create(): GraphSONMapper {
            registries.forEach { registry ->
                val simpleModules: List<Pair<Class, SimpleModule>> =
                    registry.find(GraphSONIo::class.java, SimpleModule::class.java)
                simpleModules.stream().map(Pair::getValue1).forEach(customModules::add)
            }
            return GraphSONMapper(this)
        }
    }

    companion object {
        fun build(): Builder {
            return Builder()
        }

        /**
         * Create a new Builder from a given [GraphSONMapper].
         *
         * @return a new builder, with properties taken from the original mapper already applied.
         */
        fun build(mapper: GraphSONMapper): Builder {
            val builder = build()
            builder.customModules = mapper.customModules
            builder.version = mapper.version
            builder.loadCustomModules = mapper.loadCustomSerializers
            builder.normalize = mapper.normalize
            builder.typeInfo = mapper.typeInfo
            return builder
        }
    }
}