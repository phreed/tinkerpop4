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
package org.apache.tinkerpop4.gremlin.structure.io.binary.types

import org.apache.tinkerpop4.gremlin.structure.io.binary.DataType

class ByteCodeSerializer : SimpleTypeSerializer<Bytecode?>(DataType.BYTECODE) {
    @Override
    @Throws(IOException::class)
    protected fun readValue(buffer: Buffer, context: GraphBinaryReader): Bytecode {
        val result = Bytecode()
        val stepsLength: Int = buffer.readInt()
        for (i in 0 until stepsLength) {
            result.addStep(
                context.readValue(buffer, String::class.java, false),
                getInstructionArguments(buffer, context)
            )
        }
        val sourcesLength: Int = buffer.readInt()
        for (i in 0 until sourcesLength) {
            result.addSource(
                context.readValue(buffer, String::class.java, false),
                getInstructionArguments(buffer, context)
            )
        }
        return result
    }

    @Override
    @Throws(IOException::class)
    protected fun writeValue(value: Bytecode, buffer: Buffer, context: GraphBinaryWriter) {
        val steps: List<Bytecode.Instruction> = value.getStepInstructions()
        val sources: List<Bytecode.Instruction> = value.getSourceInstructions()
        // 2 buffers for the length + plus 2 buffers per each step and source
        writeInstructions(buffer, context, steps)
        writeInstructions(buffer, context, sources)
    }

    @Throws(IOException::class)
    private fun writeInstructions(
        buffer: Buffer, context: GraphBinaryWriter,
        instructions: List<Bytecode.Instruction>
    ) {
        context.writeValue(instructions.size(), buffer, false)
        for (instruction in instructions) {
            context.writeValue(instruction.getOperator(), buffer, false)
            fillArgumentsBuffer(instruction.getArguments(), buffer, context)
        }
    }

    companion object {
        @Throws(IOException::class)
        private fun getInstructionArguments(buffer: Buffer, context: GraphBinaryReader): Array<Object?> {
            val valuesLength: Int = buffer.readInt()
            val values: Array<Object?> = arrayOfNulls<Object>(valuesLength)
            for (j in 0 until valuesLength) {
                values[j] = context.read(buffer)
            }
            return values
        }

        @Throws(IOException::class)
        private fun fillArgumentsBuffer(arguments: Array<Object>, buffer: Buffer, context: GraphBinaryWriter) {
            context.writeValue(arguments.size, buffer, false)
            for (value in arguments) {
                context.write(value, buffer)
            }
        }
    }
}