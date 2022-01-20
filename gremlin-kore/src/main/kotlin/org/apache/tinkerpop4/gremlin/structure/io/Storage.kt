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

import org.apache.tinkerpop4.gremlin.process.computer.KeyValue
import org.apache.tinkerpop4.gremlin.structure.Vertex
import java.io.File
import java.util.Iterator
import java.util.List

/**
 * Storage is a standard API that providers can implement to allow abstract UNIX-like file system for data sources.
 * The methods provided by Storage are similar in form and behavior to standard Linux operating system commands.
 *
 *  * A **name pattern** (file or directory) is a sequence of characters, not containing "/", leading spaces, trailing spaces.
 *  * A **name** (file or directory name) is a name pattern, not containing "*" or "?".
 *  * A **pattern** is a sequence of names separated with "/", optionally ending at a name pattern.
 * <pre>
 * &lt;pattern&gt; ::= &lt;absolute pattern&gt; |
 * &lt;relative pattern&gt;
 * &lt;absolute path&gt; ::= / [&lt;relative pattern&gt;]
 * &lt;relative path&gt; ::= &lt;name&gt; {/ &lt;name&gt;} [/ &lt;name pattern&gt;] [/] |
 * &lt;name pattern&gt; [/]
</pre> *
 *  * A **path** is a path is a pattern, not containing any name pattern.
 *
 * NOTE:
 *  1. Even though the syntax allows patterns with trailing "/", they are treated as referring the same
 * file or directory as the path without the trailing /
 *  1. This is an abstract file system abstracting the underlying physical file system if any. Thus, under Windows the
 * directories separator is still /, no matter that Windows uses \
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
interface Storage {
    /**
     * List all the data sources in the root directory.
     *
     * @return non-null list of files (data sources) and directories in the root directory (/)
     * @see .ls
     */
    fun ls(): List<String?>?

    /**
     * List all the files (e.g. data sources) and directories matching the location pattern.
     *
     * @param pattern non-null pattern specifying a set of files and directories. Cases:
     *  * a path to a file - specifies a single file to list
     *  * a path to a directory - specifies all files and directories immediately nested in that directory to list
     *  * pattern - specifies a set of files and directories to list
     *  * / - specifies the root directory to list its contents
     *
     * @return non-null list of files (data sources) and directories matching the pattern.
     */
    fun ls(pattern: String?): List<String?>?

    /**
     * Recursively copy all the data sources from the source location to the target location.
     *
     * @param sourcePattern non-null pattern specifying a set of files and directories. Cases:
     *  * a path to a file - specifies a single file
     *  * a path to a directory - specifies all files and directories nested (recursively) in that directory
     *  * pattern - specifies a set of files and directories
     *  * / - specifies the contents of the root directory (recursively)
     *
     * @param targetDirectory non-null directory where to copy to
     * @return whether data sources were copied
     */
    fun cp(sourcePattern: String?, targetDirectory: String?): Boolean

    /**
     * Determine whether the specified location has a data source.
     *
     * @param pattern non-null pattern specifying a set of files and directories. Examples:
     *  * a path to a file - specifies a single file
     *  * a path to a directory - specifies the contents of that directory as all files and directories immediately nested in it
     *  * pattern - specifies a set of files and directories
     *  * / - specifies the immediate contents of the root directory
     *
     *
     * @return true if the pattern specifies a non-empty set of files and directories
     */
    fun exists(pattern: String?): Boolean

    /**
     * Recursively remove the file (data source) at the specified location.
     *
     * NOTE: Some implementations derive the notion of the containing directory from the presence of the file,
     * so removing all files from a directory in those implementations removes also their directory.
     *
     * @param pattern non-null pattern specifying a set of files and directories. Examples:
     *  * a path to a file - specifies a single file
     *  * a path to a directory - specifies **that directory and** all files and directories recursively nested in it
     *  * pattern - specifies a set of files and directories
     *  * / - specifies the root directory
     *
     * @return true if all specified files and directories were removed
     */
    fun rm(pattern: String?): Boolean

    /**
     * Get a string representation of the specified number of lines at the data source location.
     *
     * @param location the data source location
     * @return an iterator of lines
     */
    fun head(location: String?): Iterator<String?>? {
        return this.head(location, Integer.MAX_VALUE)
    }

    /**
     * Get a string representation of the specified number of lines at the data source location.
     *
     * @param location   the data source location
     * @param totalLines the total number of lines to retrieve
     * @return an iterator of lines.
     */
    fun head(location: String?, totalLines: Int): Iterator<String?>?

    /**
     * Get the vertices at the specified graph location.
     *
     * @param location    the location of the graph (or the root location and search will be made)
     * @param readerClass the class of the parser that understands the graph format
     * @param totalLines  the total number of lines of the graph to return
     * @return an iterator of vertices.
     */
    fun head(location: String?, readerClass: Class?, totalLines: Int): Iterator<Vertex?>?

    /**
     * Get the vertices at the specified graph location.
     *
     * @param location    the location of the graph (or the root location and search will be made)
     * @param readerClass the class of the parser that understands the graph format
     * @return an iterator of vertices.
     */
    fun head(location: String?, readerClass: Class?): Iterator<Vertex?>? {
        return this.head(location, readerClass, Integer.MAX_VALUE)
    }

    /**
     * Get the [KeyValue] data at the specified memory location.
     *
     * @param location    the root location of the data
     * @param memoryKey   the memory key
     * @param readerClass the class of the parser that understands the memory format
     * @param totalLines  the total number of key-values to return
     * @return an iterator of key-values.
     */
    fun <K, V> head(
        location: String?,
        memoryKey: String?,
        readerClass: Class?,
        totalLines: Int
    ): Iterator<KeyValue<K, V>?>?

    /**
     * Get the [KeyValue] data at the specified memory location.
     *
     * @param location    the root location of the data
     * @param memoryKey   the memory key
     * @param readerClass the class of the parser that understands the memory format
     * @return an iterator of key-values.
     */
    fun <K, V> head(location: String?, memoryKey: String?, readerClass: Class?): Iterator<KeyValue<K, V>?>? {
        return this.head<Any, Any>(location, memoryKey, readerClass, Integer.MAX_VALUE)
    }

    companion object {
        /**
         * @param path non-null local file path
         * @return non-null, not empty path in the [Storage] file system.
         */
        fun toPath(path: File): String? {
            return toPath(path.getAbsolutePath())
        }

        /**
         * @param path non-null local file path
         * @return non-null, not empty path in the [Storage] file system.
         */
        fun toPath(path: String): String? {
            return path.replace("\\", FILE_SEPARATOR)
        }

        /**
         * The file and directory names separator in this uniform UNIX-like abstract file system
         */
        const val FILE_SEPARATOR = "/"
        const val ROOT_DIRECTORY = FILE_SEPARATOR
    }
}