/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com)
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.projectservice.core;

import java.util.List;

/**
 * Represents a parameter for a migration tool.
 *
 * @param key          parameter key used in the tool configuration
 * @param label        human-readable label for the parameter
 * @param description  detailed description of what this parameter does
 * @param valueType    type of the parameter value (e.g., string, boolean, enum)
 * @param defaultValue default value for the parameter
 * @param options      list of valid options for enum-type parameters
 * @since 1.2.0
 */
public record MigrationToolParameter(String key, String label, String description, String valueType,
                                     String defaultValue, List<String> options) { }
