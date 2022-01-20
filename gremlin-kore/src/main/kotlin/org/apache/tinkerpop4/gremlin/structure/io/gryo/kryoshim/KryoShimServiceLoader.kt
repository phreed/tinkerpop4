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
package org.apache.tinkerpop4.gremlin.structure.io.gryo.kryoshim

import org.apache.commons.configuration2.BaseConfiguration

/**
 * Loads the highest-priority or user-selected [KryoShimService].
 */
object KryoShimServiceLoader {
    @Volatile
    private var cachedShimService: KryoShimService? = null

    @Volatile
    private var configuration: Configuration? = null
    private const val maskedProperties = ".+\\.(password|keyStorePassword|trustStorePassword)|spark.authenticate.secret"
    private val log: Logger = LoggerFactory.getLogger(KryoShimServiceLoader::class.java)

    /**
     * Set this system property to the fully-qualified name of a [KryoShimService]
     * package-and-classname to force it into service.  Setting this property causes the
     * priority-selection mechanism ([KryoShimService.getPriority]) to be ignored.
     */
    const val KRYO_SHIM_SERVICE = "gremlin.io.kryoShimService"
    fun applyConfiguration(configuration: Configuration?) {
        if (null == KryoShimServiceLoader.configuration || null == cachedShimService ||
            !KryoShimServiceLoader.configuration.getKeys().hasNext()
        ) {
            KryoShimServiceLoader.configuration = configuration
            load(true)
        }
    }

    fun close() {
        if (null != cachedShimService) cachedShimService.close()
        cachedShimService = null
        configuration = null
    }

    /**
     * Return a reference to the shim service.  This method may return a cached shim service
     * unless `forceReload` is true.  Calls to this method need not be externally
     * synchonized.
     *
     * @param forceReload if false, this method may use its internal service cache; if true,
     * this method must ignore cache, and it must invoke [ServiceLoader.reload]
     * before selecting a new service to return
     * @return the shim service
     */
    private fun load(forceReload: Boolean): KryoShimService? {
        // if the service is loaded and doesn't need reloading, simply return in
        if (null != cachedShimService && !forceReload) return cachedShimService

        // if a service is already loaded, close it
        if (null != cachedShimService) cachedShimService.close()

        // if the configuration is null, try and load the configuration from System.properties
        if (null == configuration) configuration = SystemUtil.getSystemPropertiesConfiguration("tinkerpop", true)

        // get all of the shim services
        val services: ArrayList<KryoShimService> = ArrayList()
        val serviceLoader: ServiceLoader<KryoShimService> = ServiceLoader.load(KryoShimService::class.java)
        synchronized(KryoShimServiceLoader::class.java) {
            if (forceReload) serviceLoader.reload()
            for (kss in serviceLoader) {
                services.add(kss)
            }
        }
        // if a shim service class is specified in the configuration, use it -- else, priority-based
        if (configuration.containsKey(KRYO_SHIM_SERVICE)) {
            for (kss in services) {
                if (kss.getClass().getCanonicalName().equals(configuration.getString(KRYO_SHIM_SERVICE))) {
                    log.info(
                        "Set KryoShimService to {} because of configuration {}={}",
                        kss.getClass().getSimpleName(),
                        KRYO_SHIM_SERVICE,
                        configuration.getString(KRYO_SHIM_SERVICE)
                    )
                    cachedShimService = kss
                    break
                }
            }
        } else {
            services.sort(KryoShimServiceComparator.INSTANCE)
            for (kss in services) {
                log.debug(
                    "Found KryoShimService: {} (priority {})",
                    kss.getClass().getCanonicalName(),
                    kss.getPriority()
                )
            }
            if (0 != services.size()) {
                cachedShimService = services.get(services.size() - 1)
                log.info(
                    "Set KryoShimService to {} because its priority value ({}) is the best available",
                    cachedShimService.getClass().getSimpleName(), cachedShimService.getPriority()
                )
            }
        }

        // no shim service was available
        if (null == cachedShimService) throw IllegalStateException("Unable to load KryoShimService")

        // once the shim service is defined, configure it
        log.info(
            "Configuring KryoShimService {} with the following configuration:\n#######START########\n{}\n########END#########",
            cachedShimService.getClass().getCanonicalName(),
            ConfigurationUtils.toString(maskedConfiguration(configuration))
        )
        cachedShimService.applyConfiguration(configuration)
        return cachedShimService
    }

    private fun maskedConfiguration(configuration: Configuration?): Configuration {
        val maskedConfiguration: Configuration = BaseConfiguration()
        val keys: Iterator = configuration.getKeys()
        while (keys.hasNext()) {
            val key = keys.next() as String
            if (key.matches(maskedProperties)) maskedConfiguration.setProperty(
                key,
                "******"
            ) else maskedConfiguration.setProperty(key, configuration.getProperty(key))
        }
        return maskedConfiguration
    }

    /**
     * A loose abstraction of [org.apache.tinkerpop.shaded.kryo.Kryo.writeClassAndObject],
     * where the `output` parameter is an internally-created [ByteArrayOutputStream].  Returns
     * the byte array underlying that stream.
     *
     * @param object an object for which the instance and class are serialized
     * @return the serialized form
     */
    fun writeClassAndObjectToBytes(`object`: Object?): ByteArray {
        val shimService: KryoShimService? = load(false)
        val baos = ByteArrayOutputStream()
        shimService.writeClassAndObject(`object`, baos)
        return baos.toByteArray()
    }

    /**
     * A loose abstraction of [org.apache.tinkerpop.shaded.kryo.Kryo.readClassAndObject],
     * where the `input` parameter is `source`.  Returns the deserialized object.
     *
     * @param inputStream an input stream containing data for a serialized object class and instance
     * @param <T>         the type to which the deserialized object is cast as it is returned
     * @return the deserialized object
    </T> */
    fun <T> readClassAndObject(inputStream: InputStream?): T {
        val shimService: KryoShimService? = load(false)
        return shimService.readClassAndObject(inputStream)
    }

    /**
     * Selects the service with greatest [KryoShimService.getPriority]
     * (not absolute value).
     *
     *
     * Breaks ties with lexicographical comparison of classnames where the
     * name that sorts last is considered to have highest priority.  Ideally
     * nothing should rely on that tiebreaking behavior, but it beats random
     * selection in case a user ever gets into that situation by accident and
     * tries to figure out what's going on.
     */
    private enum class KryoShimServiceComparator : Comparator<KryoShimService?> {
        INSTANCE;

        @Override
        fun compare(a: KryoShimService, b: KryoShimService): Int {
            val ap: Int = a.getPriority()
            val bp: Int = b.getPriority()
            return if (ap < bp) {
                -1
            } else if (bp < ap) {
                1
            } else {
                val result: Int = a.getClass().getCanonicalName().compareTo(b.getClass().getCanonicalName())
                if (0 == result) {
                    log.warn(
                        "Found two {} implementations with the same canonical classname: {}.  " +
                                "This may indicate a problem with the classpath/classloader such as " +
                                "duplicate or conflicting copies of the file " +
                                "META-INF/services/org.apache.tinkerpop4.gremlin.structure.io.gryo.kryoshim.KryoShimService.",
                        a.getClass().getCanonicalName()
                    )
                } else {
                    val winner: String =
                        if (0 < result) a.getClass().getCanonicalName() else b.getClass().getCanonicalName()
                    log.warn(
                        "{} implementations {} and {} are tied with priority value {}.  " +
                                "Preferring {} to the other because it has a lexicographically greater classname.  " +
                                "Consider setting the system property \"{}\" instead of relying on priority tie-breaking.",
                        KryoShimService::class.java.getSimpleName(), a, b, ap, winner, KRYO_SHIM_SERVICE
                    )
                }
                result
            }
        }
    }
}