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

import java.io.IOException

/**
 * Represents an abstract view for one or more primitive byte arrays and NIO buffers.
 */
interface Buffer {
    /**
     * Returns the number of readable bytes.
     */
    fun readableBytes(): Int

    /**
     * Returns the reader index of this buffer.
     */
    fun readerIndex(): Int

    /**
     * Sets the reader index of this buffer.
     *
     * @throws IndexOutOfBoundsException
     * if its out of bounds.
     */
    fun readerIndex(readerIndex: Int): Buffer?

    /**
     * Returns the writer index of this buffer.
     */
    fun writerIndex(): Int

    /**
     * Sets the writer index of this buffer.
     */
    fun writerIndex(writerIndex: Int): Buffer?

    /**
     * Marks the current writer index in this buffer.
     */
    fun markWriterIndex(): Buffer?

    /**
     * Repositions the current writer index to the marked index in this buffer.
     */
    fun resetWriterIndex(): Buffer?

    /**
     * Returns the number of bytes (octets) this buffer can contain.
     */
    fun capacity(): Int

    /**
     * Returns `true` if and only if this buffer is backed by an
     * NIO direct buffer.
     */
    val isDirect: Boolean

    /**
     * Gets a boolean and advances the reader index.
     */
    fun readBoolean(): Boolean

    /**
     * Gets a byte and advances the reader index.
     */
    fun readByte(): Byte

    /**
     * Gets a 16-bit short integer and advances the reader index.
     */
    fun readShort(): Short

    /**
     * Gets a 32-bit integer at the current index and advances the reader index.
     */
    fun readInt(): Int

    /**
     * Gets a 64-bit integer  and advances the reader index.
     */
    fun readLong(): Long

    /**
     * Gets a 32-bit floating point number and advances the reader index.
     */
    fun readFloat(): Float

    /**
     * Gets a 64-bit floating point number and advances the reader index.
     */
    fun readDouble(): Double

    /**
     * Transfers this buffer's data to the specified destination starting at
     * the current reader index and advances the reader index.
     */
    fun readBytes(destination: ByteArray?): Buffer?

    /**
     * Transfers this buffer's data to the specified destination starting at
     * the current reader index and advances the reader index.
     *
     * @param destination The destination buffer
     * @param dstIndex the first index of the destination
     * @param length   the number of bytes to transfer
     */
    fun readBytes(destination: ByteArray?, dstIndex: Int, length: Int): Buffer?

    /**
     * Transfers this buffer's data to the specified destination starting at
     * the current reader index until the destination's position
     * reaches its limit, and advances the reader index.
     */
    fun readBytes(dst: ByteBuffer?): Buffer?

    /**
     * Transfers this buffer's data to the specified stream starting at the
     * current reader index and advances the index.
     *
     * @param length the number of bytes to transfer
     *
     * @throws IOException
     * if the specified stream threw an exception during I/O
     */
    @Throws(IOException::class)
    fun readBytes(out: OutputStream?, length: Int): Buffer?

    /**
     * Sets the specified boolean at the current writer index and advances the index.
     */
    fun writeBoolean(value: Boolean): Buffer?

    /**
     * Sets the specified byte at the current writer index and advances the index.
     */
    fun writeByte(value: Int): Buffer?

    /**
     * Sets the specified 16-bit short integer at the current writer index and advances the index.
     */
    fun writeShort(value: Int): Buffer?

    /**
     * Sets the specified 32-bit integer at the current writer index and advances the index.
     */
    fun writeInt(value: Int): Buffer?

    /**
     * Sets the specified 64-bit long integer at the current writer index and advances the index.
     */
    fun writeLong(value: Long): Buffer?

    /**
     * Sets the specified 32-bit floating point number at the current writer index and advances the index.
     */
    fun writeFloat(value: Float): Buffer?

    /**
     * Sets the specified 64-bit floating point number at the current writer index and advances the index.
     */
    fun writeDouble(value: Double): Buffer?

    /**
     * Transfers the specified source array's data to this buffer starting at the current writer index
     * and advances the index.
     */
    fun writeBytes(src: ByteArray?): Buffer?

    /**
     * Transfers the specified source byte data to this buffer starting at the current writer index
     * and advances the index.
     */
    fun writeBytes(src: ByteBuffer?): Buffer?

    /**
     * Transfers the specified source array's data to this buffer starting at the current writer index
     * and advances the index.
     */
    fun writeBytes(src: ByteArray?, srcIndex: Int, length: Int): Buffer?

    /**
     * Decreases the reference count by `1` and deallocates this object if the reference count reaches at
     * `0`.
     */
    fun release(): Boolean

    /**
     * Increases the reference count by `1`.
     */
    fun retain(): Buffer?

    /**
     * Returns the reference count of this object.
     */
    fun referenceCount(): Int

    /**
     * Returns the maximum number of NIO [ByteBuffer]s that consist this buffer.
     */
    fun nioBufferCount(): Int

    /**
     * Exposes this buffer's readable bytes as NIO ByteBuffer's instances.
     */
    fun nioBuffers(): Array<ByteBuffer?>?

    /**
     * Exposes this buffer's readable bytes as NIO ByteBuffer's instances.
     */
    fun nioBuffers(index: Int, length: Int): Array<ByteBuffer?>?

    /**
     * Exposes this buffer's readable bytes as a NIO [ByteBuffer]. The returned buffer
     * either share or contains the copied content of this buffer, while changing the position
     * and limit of the returned NIO buffer does not affect the indexes and marks of this buffer.
     */
    fun nioBuffer(): ByteBuffer?

    /**
     * Exposes this buffer's sub-region as an NIO [ByteBuffer].
     */
    fun nioBuffer(index: Int, length: Int): ByteBuffer?

    /**
     * Transfers this buffer's data to the specified destination starting at
     * the specified absolute `index`.
     * This method does not modify reader or writer indexes.
     */
    fun getBytes(index: Int, dst: ByteArray?): Buffer?
}