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
package org.apache.tinkerpop4.gremlin.util

import org.apache.tinkerpop4.gremlin.structure.io.Storage

/**
 * This is a utility class that is for support of various testing activities and is not meant to be used in other
 * contexts. It is not explicitly in a test package given our dependency hierarchy.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
object TestSupport {
    const val TEST_DATA_RELATIVE_DIR = "test-case-data"

    /**
     * Creates a [File] reference that points to a directory relative to the supplied class in the
     * `/target` directory. Each `childPath` passed introduces a new sub-directory and all are placed
     * below the [.TEST_DATA_RELATIVE_DIR].  For example, calling this method with "a", "b", and "c" as the
     * `childPath` arguments would yield a relative directory like: `test-case-data/clazz/a/b/c`. It is
     * a good idea to use the test class for the `clazz` argument so that it's easy to find the data if
     * necessary after test execution.
     *
     *
     * Avoid using makeTestDataPath(...).getAbsolutePath() and makeTestDataPath(...).toString() that produces
     * platform-dependent paths, that are incompatible with regular expressions and escape characters.
     * Instead use [Storage.toPath]
     */
    fun makeTestDataPath(clazz: Class<*>, vararg childPath: String?): File {
        val root: File = getRootOfBuildDirectory(clazz)
        val cleanedPaths: List<String> =
            Stream.of(childPath).map { obj: TestSupport?, toClean: String -> cleanPathSegment(toClean) }
                .collect(Collectors.toList())

        // use the class name in the directory structure
        cleanedPaths.add(0, cleanPathSegment(clazz.getSimpleName()))
        val f = File(File(root, TEST_DATA_RELATIVE_DIR), String.join(Storage.FILE_SEPARATOR, cleanedPaths))
        if (!f.exists()) f.mkdirs()
        return f
    }

    /**
     * Creates a [File] reference that .  For example, calling this method with "a", "b", and "c" as the
     * `childPath` arguments would yield a relative directory like: `test-case-data/clazz/a/b/c`. It is
     * a good idea to use the test class for the `clazz` argument so that it's easy to find the data if
     * necessary after test execution.
     *
     * @return UNIX-formatted path to a directory in the underlying [Storage]. The directory is relative to the
     * supplied class in the `/target` directory. Each `childPath` passed introduces a new sub-directory
     * and all are placed below the [.TEST_DATA_RELATIVE_DIR]
     */
    fun makeTestDataDirectory(clazz: Class<*>, vararg childPath: String?): String {
        return Storage.toPath(makeTestDataPath(clazz, *childPath))
    }

    /**
     * @param clazz
     * @param fileName
     * @return UNIX-formatted path to a fileName in the underlying [Storage]. The file is relative to the
     * supplied class in the `/target` directory.
     */
    fun makeTestDataFile(clazz: Class<*>, fileName: String?): String {
        return Storage.toPath(File(makeTestDataPath(clazz), fileName))
    }

    /**
     * @param clazz
     * @param subdir
     * @param fileName
     * @return UNIX-formatted path to a subdir/fileName in the underlying [Storage]. The file is relative to the
     * supplied class in the `/target` directory.
     */
    fun makeTestDataFile(clazz: Class<*>, subdir: String?, fileName: String?): String {
        return Storage.toPath(File(makeTestDataPath(clazz, subdir), fileName))
    }

    /**
     * Gets and/or creates the root of the test data directory.  This  method is here as a convenience and should not
     * be used to store test data.  Use [.makeTestDataPath] instead.
     */
    fun getRootOfBuildDirectory(clazz: Class<*>): File {
        val root: File

        // build.dir gets sets during runs of tests with maven via the surefire configuration in the pom.xml
        // if that is not set as an environment variable, then the path is computed based on the location of the
        // requested class.  the computed version at least as far as intellij is concerned comes drops it into
        // /target/test-classes.  the build.dir had to be added because maven doesn't seem to like a computed path
        // as it likes to find that path in the .m2 directory and other weird places......
        val buildDirectory: String = System.getProperty("build.dir")
        if (null == buildDirectory) {
            val clsUri: String = clazz.getName().replace(".", "/") + ".class"
            val url: URL = clazz.getClassLoader().getResource(clsUri)
            val clsPath: String = url.getPath()
            val computePath: String = clsPath.substring(0, clsPath.length() - clsUri.length())
            root = File(computePath).getParentFile()
        } else {
            root = File(buildDirectory)
        }
        if (!root.exists()) root.mkdirs()
        return root
    }

    /**
     * Creates a [File] reference in the path returned from [.makeTestDataPath] in a subdirectory
     * called `temp`.
     */
    @Throws(IOException::class)
    fun generateTempFile(clazz: Class<*>, fileName: String?, fileNameSuffix: String?): File {
        val path: File = makeTestDataPath(clazz, "temp")
        if (!path.exists()) path.mkdirs()
        return File.createTempFile(fileName, fileNameSuffix, path)
    }

    /**
     * Copies a file stored as part of a resource to the file system in the path returned from
     * [.makeTestDataPath] in a subdirectory called `temp/resources`.
     */
    @Throws(IOException::class)
    fun generateTempFileFromResource(resourceClass: Class<*>, resourceName: String, extension: String): File {
        return generateTempFileFromResource(resourceClass, resourceClass, resourceName, extension)
    }

    /**
     * Copies a file stored as part of a resource to the file system in the path returned from
     * [.makeTestDataPath] in a subdirectory called `temp/resources`.
     */
    @Throws(IOException::class)
    fun generateTempFileFromResource(
        graphClass: Class<*>,
        resourceClass: Class<*>,
        resourceName: String,
        extension: String
    ): File {
        return generateTempFileFromResource(graphClass, resourceClass, resourceName, extension, true)
    }

    /**
     * Copies a file stored as part of a resource to the file system in the path returned from
     * [.makeTestDataPath] in a subdirectory called `temp/resources`.
     */
    @Throws(IOException::class)
    fun generateTempFileFromResource(
        graphClass: Class<*>, resourceClass: Class<*>,
        resourceName: String, extension: String, overwrite: Boolean
    ): File {
        val temp: File = makeTestDataPath(graphClass, "resources")
        val tempFile = File(temp, resourceName + extension)

        // these checks are present mostly for windows compatibility where an outputstream created on a non-existent
        // file will cause an error.
        if (tempFile.exists() && !overwrite) {
            // overwrite is disabled and file already exists -> reuse as-is
            return tempFile
        }
        if (!tempFile.getParentFile().exists()) {
            Files.createDirectories(tempFile.getParentFile().toPath())
        }
        // either the file does not exist or needs to be overwritten, drop it
        Files.deleteIfExists(tempFile.toPath())
        // create the new file
        Files.createFile(tempFile.toPath())
        FileOutputStream(tempFile).use { outputStream ->
            var data: Int
            resourceClass.getResourceAsStream(resourceName).use { inputStream ->
                while (inputStream.read().also { data = it } != -1) {
                    outputStream.write(data)
                }
            }
        }
        return tempFile
    }

    /**
     * Removes characters that aren't acceptable in a file path (mostly for windows).
     */
    fun cleanPathSegment(toClean: String): String {
        val cleaned: String = toClean.replaceAll("[.\\\\/:*?\"<>|\\[\\]\\(\\)]", "")
        if (cleaned.length() === 0) throw IllegalStateException("Path segment $toClean has not valid characters and is thus empty")
        return cleaned
    }
}