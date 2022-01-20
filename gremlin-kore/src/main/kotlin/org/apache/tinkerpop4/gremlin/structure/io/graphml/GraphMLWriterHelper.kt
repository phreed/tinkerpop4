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
package org.apache.tinkerpop4.gremlin.structure.io.graphml

import javax.xml.namespace.NamespaceContext

/**
 * A wrapper for the different `XMLStreamWriter` implementations.
 */
internal class GraphMLWriterHelper private constructor() {
    /**
     * @author Tijs Rademakers
     */
    internal abstract class DelegatingXMLStreamWriter(writer: XMLStreamWriter) : XMLStreamWriter {
        private val writer: XMLStreamWriter

        init {
            this.writer = writer
        }

        @Throws(XMLStreamException::class)
        fun writeStartElement(localName: String?) {
            writer.writeStartElement(localName)
        }

        @Throws(XMLStreamException::class)
        fun writeStartElement(namespaceURI: String?, localName: String?) {
            writer.writeStartElement(namespaceURI, localName)
        }

        @Throws(XMLStreamException::class)
        fun writeStartElement(prefix: String?, localName: String?, namespaceURI: String?) {
            writer.writeStartElement(prefix, localName, namespaceURI)
        }

        @Throws(XMLStreamException::class)
        fun writeEmptyElement(namespaceURI: String?, localName: String?) {
            writer.writeEmptyElement(namespaceURI, localName)
        }

        @Throws(XMLStreamException::class)
        fun writeEmptyElement(prefix: String?, localName: String?, namespaceURI: String?) {
            writer.writeEmptyElement(prefix, localName, namespaceURI)
        }

        @Throws(XMLStreamException::class)
        fun writeEmptyElement(localName: String?) {
            writer.writeEmptyElement(localName)
        }

        @Throws(XMLStreamException::class)
        fun writeEndElement() {
            writer.writeEndElement()
        }

        @Throws(XMLStreamException::class)
        fun writeEndDocument() {
            writer.writeEndDocument()
        }

        @Throws(XMLStreamException::class)
        fun close() {
            writer.close()
        }

        @Throws(XMLStreamException::class)
        fun flush() {
            writer.flush()
        }

        @Throws(XMLStreamException::class)
        fun writeAttribute(localName: String?, value: String?) {
            writer.writeAttribute(localName, value)
        }

        @Throws(XMLStreamException::class)
        fun writeAttribute(prefix: String?, namespaceURI: String?, localName: String?, value: String?) {
            writer.writeAttribute(prefix, namespaceURI, localName, value)
        }

        @Throws(XMLStreamException::class)
        fun writeAttribute(namespaceURI: String?, localName: String?, value: String?) {
            writer.writeAttribute(namespaceURI, localName, value)
        }

        @Throws(XMLStreamException::class)
        fun writeNamespace(prefix: String?, namespaceURI: String?) {
            writer.writeNamespace(prefix, namespaceURI)
        }

        @Throws(XMLStreamException::class)
        fun writeDefaultNamespace(namespaceURI: String?) {
            writer.writeDefaultNamespace(namespaceURI)
        }

        @Throws(XMLStreamException::class)
        fun writeComment(data: String?) {
            writer.writeComment(data)
        }

        @Throws(XMLStreamException::class)
        fun writeProcessingInstruction(target: String?) {
            writer.writeProcessingInstruction(target)
        }

        @Throws(XMLStreamException::class)
        fun writeProcessingInstruction(target: String?, data: String?) {
            writer.writeProcessingInstruction(target, data)
        }

        @Throws(XMLStreamException::class)
        fun writeCData(data: String?) {
            writer.writeCData(data)
        }

        @Throws(XMLStreamException::class)
        fun writeDTD(dtd: String?) {
            writer.writeDTD(dtd)
        }

        @Throws(XMLStreamException::class)
        fun writeEntityRef(name: String?) {
            writer.writeEntityRef(name)
        }

        @Throws(XMLStreamException::class)
        fun writeStartDocument() {
            writer.writeStartDocument()
        }

        @Throws(XMLStreamException::class)
        fun writeStartDocument(version: String?) {
            writer.writeStartDocument(version)
        }

        @Throws(XMLStreamException::class)
        fun writeStartDocument(encoding: String?, version: String?) {
            writer.writeStartDocument(encoding, version)
        }

        @Throws(XMLStreamException::class)
        fun writeCharacters(text: String?) {
            writer.writeCharacters(text)
        }

        @Throws(XMLStreamException::class)
        fun writeCharacters(text: CharArray?, start: Int, len: Int) {
            writer.writeCharacters(text, start, len)
        }

        @Throws(XMLStreamException::class)
        fun getPrefix(uri: String?): String {
            return writer.getPrefix(uri)
        }

        @Throws(XMLStreamException::class)
        fun setPrefix(prefix: String?, uri: String?) {
            writer.setPrefix(prefix, uri)
        }

        @Throws(XMLStreamException::class)
        fun setDefaultNamespace(uri: String?) {
            writer.setDefaultNamespace(uri)
        }

        @set:Throws(XMLStreamException::class)
        var namespaceContext: NamespaceContext
            get() = writer.getNamespaceContext()
            set(context) {
                writer.setNamespaceContext(context)
            }

        @Throws(IllegalArgumentException::class)
        fun getProperty(name: String?): Object {
            return writer.getProperty(name)
        }
    }

    /**
     * @author Tijs Rademakers
     */
    class IndentingXMLStreamWriter(writer: XMLStreamWriter) : DelegatingXMLStreamWriter(writer) {
        private var state: Object = SEEN_NOTHING
        private val stateStack: Stack<Object> = Stack<Object>()
        private var indentStep = "  "
        private var depth = 0

        /**
         * Return the current indent step.
         *
         *
         *
         * Return the current indent step: each start tag will be indented by this
         * number of spaces times the number of ancestors that the element has.
         *
         *
         * @return The number of spaces in each indentation step, or 0 or less for no
         * indentation.
         * @see .setIndentStep
         */
        @Deprecated("Only return the length of the indent string.")
        fun getIndentStep(): Int {
            return indentStep.length()
        }

        /**
         * Set the current indent step.
         *
         * @param indentStep
         * The new indent step (0 or less for no indentation).
         * @see .getIndentStep
         */
        @Deprecated("Should use the version that takes string.")
        fun setIndentStep(indentStep: Int) {
            var indentStep = indentStep
            val s = StringBuilder()
            while (indentStep > 0) {
                s.append(' ')
                indentStep--
            }
            this.indentStep = s.toString()
        }

        fun setIndentStep(s: String) {
            indentStep = s
        }

        @Throws(XMLStreamException::class)
        private fun onStartElement() {
            stateStack.push(SEEN_ELEMENT)
            state = SEEN_NOTHING
            if (depth > 0) {
                super.writeCharacters("\n")
            }
            doIndent()
            depth++
        }

        @Throws(XMLStreamException::class)
        private fun onEndElement() {
            depth--
            if (state === SEEN_ELEMENT) {
                super.writeCharacters("\n")
                doIndent()
            }
            state = stateStack.pop()
        }

        @Throws(XMLStreamException::class)
        private fun onEmptyElement() {
            state = SEEN_ELEMENT
            if (depth > 0) {
                super.writeCharacters("\n")
            }
            doIndent()
        }

        /**
         * Print indentation for the current level.
         *
         * @exception org.xml.sax.SAXException
         * If there is an error writing the indentation characters, or if
         * a filter further down the chain raises an exception.
         */
        @Throws(XMLStreamException::class)
        private fun doIndent() {
            if (depth > 0) {
                for (i in 0 until depth) super.writeCharacters(indentStep)
            }
        }

        @Throws(XMLStreamException::class)
        override fun writeStartDocument() {
            super.writeStartDocument()
            super.writeCharacters("\n")
        }

        @Throws(XMLStreamException::class)
        override fun writeStartDocument(version: String?) {
            super.writeStartDocument(version)
            super.writeCharacters("\n")
        }

        @Throws(XMLStreamException::class)
        override fun writeStartDocument(encoding: String?, version: String?) {
            super.writeStartDocument(encoding, version)
            super.writeCharacters("\n")
        }

        @Throws(XMLStreamException::class)
        override fun writeStartElement(localName: String?) {
            onStartElement()
            super.writeStartElement(localName)
        }

        @Throws(XMLStreamException::class)
        override fun writeStartElement(namespaceURI: String?, localName: String?) {
            onStartElement()
            super.writeStartElement(namespaceURI, localName)
        }

        @Throws(XMLStreamException::class)
        override fun writeStartElement(
            prefix: String?, localName: String?,
            namespaceURI: String?
        ) {
            onStartElement()
            super.writeStartElement(prefix, localName, namespaceURI)
        }

        @Throws(XMLStreamException::class)
        override fun writeEmptyElement(namespaceURI: String?, localName: String?) {
            onEmptyElement()
            super.writeEmptyElement(namespaceURI, localName)
        }

        @Throws(XMLStreamException::class)
        override fun writeEmptyElement(
            prefix: String?, localName: String?,
            namespaceURI: String?
        ) {
            onEmptyElement()
            super.writeEmptyElement(prefix, localName, namespaceURI)
        }

        @Throws(XMLStreamException::class)
        override fun writeEmptyElement(localName: String?) {
            onEmptyElement()
            super.writeEmptyElement(localName)
        }

        @Throws(XMLStreamException::class)
        override fun writeEndElement() {
            onEndElement()
            super.writeEndElement()
        }

        @Throws(XMLStreamException::class)
        override fun writeCharacters(text: String?) {
            state = SEEN_DATA
            super.writeCharacters(text)
        }

        @Throws(XMLStreamException::class)
        override fun writeCharacters(text: CharArray?, start: Int, len: Int) {
            state = SEEN_DATA
            super.writeCharacters(text, start, len)
        }

        @Throws(XMLStreamException::class)
        override fun writeCData(data: String?) {
            state = SEEN_DATA
            super.writeCData(data)
        }

        companion object {
            private val SEEN_NOTHING: Object = Object()
            private val SEEN_ELEMENT: Object = Object()
            private val SEEN_DATA: Object = Object()
        }
    }
}