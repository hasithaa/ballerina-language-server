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
 * Represents a migration tool with its configuration parameters.
 *
 * @param id              unique identifier for the migration tool
 * @param title           display name of the migration tool
 * @param commandName     command to execute the migration tool
 * @param requiredVersion minimum required version for the tool
 * @param needToPull      indicates if the tool needs to be pulled from central
 * @param description     description of the migration tool
 * @param parameters      list of parameters required by the migration tool
 * @since 1.2.0
 */
public record MigrationTool(int id, String title,
                            String commandName,
                            String requiredVersion,
                            Boolean needToPull,
                            String description,
                            List<MigrationToolParameter> parameters) {

    public static MigrationTool from(MigrationTool tool, Boolean needToPull) {
        return new MigrationTool(tool.id, tool.title, tool.commandName, tool.requiredVersion,
                needToPull, tool.description, tool.parameters);
    }
}
