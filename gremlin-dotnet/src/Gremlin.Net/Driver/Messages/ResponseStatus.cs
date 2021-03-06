#region License

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

#endregion

using System.Collections.Generic;
using Gremlin.Net.Driver.Exceptions;

namespace Gremlin.Net.Driver.Messages
{
    /// <summary>
    ///     Represents status information of a <see cref="ResponseMessage{T}"/>.
    /// </summary>
    public class ResponseStatus
    {
        /// <summary>
        ///     Gets or sets the <see cref="ResponseStatusCode"/>.
        /// </summary>
        public ResponseStatusCode Code { get; set; }

        /// <summary>
        ///     Gets or sets the attributes <see cref="Dictionary{TKey,TValue}"/> with protocol-level information.
        /// </summary>
        public Dictionary<string, object> Attributes { get; set; }

        /// <summary>
        ///     Gets or sets the message which is just a human-readable string usually associated with errors.
        /// </summary>
        public string Message { get; set; }
    }

    internal static class ResponseStatusExtensions
    {
        public static void ThrowIfStatusIndicatesError(this ResponseStatus status)
        {
            if (status.Code.IndicatesError())
                throw new ResponseException(status.Code, status.Attributes, $"{status.Code}: {status.Message}");
        }
    }
}