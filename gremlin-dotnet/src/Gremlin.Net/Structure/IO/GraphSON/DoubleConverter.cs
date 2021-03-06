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

using System.Text.Json;

namespace Gremlin.Net.Structure.IO.GraphSON
{
    internal class DoubleConverter : NumberConverter<double>
    {
        protected override string GraphSONTypeName => "Double";
        private const string NaN = "NaN";
        private const string PositiveInfinity = "Infinity";
        private const string NegativeInfinity = "-Infinity";

        protected override dynamic FromJsonElement(JsonElement graphson)
        {
            if (graphson.ValueKind == JsonValueKind.String)
            {
                switch (graphson.GetString())
                {
                    case NaN:
                        return double.NaN;
                    case PositiveInfinity:
                        return double.PositiveInfinity;
                    case NegativeInfinity:
                        return double.NegativeInfinity;
                }
            }  
            return graphson.GetDouble();
        }

        protected override object ConvertInvalidNumber(double number)
        {
            if (double.IsNaN(number))
            {
                return NaN;
            }

            if (double.IsPositiveInfinity(number))
            {
                return PositiveInfinity;
            }

            if (double.IsNegativeInfinity(number))
            {
                return NegativeInfinity;
            }
            
            return number;
        }
    }
}