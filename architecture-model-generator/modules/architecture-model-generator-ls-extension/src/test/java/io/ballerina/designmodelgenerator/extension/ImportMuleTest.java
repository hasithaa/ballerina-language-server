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

package io.ballerina.designmodelgenerator.extension;

import com.google.gson.JsonObject;
import io.ballerina.modelgenerator.commons.AbstractLSTest;
import io.ballerina.projectservice.extension.request.ImportMuleRequest;
import io.ballerina.projectservice.extension.response.ImportMuleResponse;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Test class for the Mule import functionality.
 *
 * @since 1.2.0
 */
public class ImportMuleTest extends AbstractLSTest {

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = configDir.resolve(config);
        BufferedReader bufferedReader = Files.newBufferedReader(configJsonPath);
        TestConfig testConfig = gson.fromJson(bufferedReader, TestConfig.class);
        bufferedReader.close();

        ImportMuleRequest request = new ImportMuleRequest("ballerina", "",
                sourceDir.resolve(testConfig.projectPath()).toAbsolutePath().toString(), 
                testConfig.parameters());
        JsonObject response = getResponse(request).getAsJsonObject();

        ImportMuleResponse actualToolResponse = gson.fromJson(response, ImportMuleResponse.class);
        ImportMuleResponse expectedToolResponse = gson.fromJson(testConfig.output(), ImportMuleResponse.class);
        if (!actualToolResponse.equals(expectedToolResponse)) {
            TestConfig updatedConfig = new TestConfig(testConfig.description(), testConfig.projectPath(), 
                    testConfig.parameters(), response);
//            updateConfig(configJsonPath, updatedConfig);
            Assert.fail(String.format("Failed test: '%s' (%s)", testConfig.description(), configJsonPath));
        }
    }

    @Override
    protected String getResourceDir() {
        return "import-mule";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return ImportMuleTest.class;
    }

    @Override
    protected String getServiceName() {
        return "projectService";
    }

    @Override
    protected String getApiName() {
        return "importMule";
    }

    /**
     * Represents the test configuration.
     *
     * @param description Description of the test case
     * @param projectPath Path to the Mule project
     * @param parameters  Parameters for Mule import (e.g., forceVersion)
     * @param output      Expected output as a JSON object
     */
    private record TestConfig(String description, String projectPath, Map<String, String> parameters, 
                             JsonObject output) {

        public String description() {
            return description == null ? "" : description;
        }
        
        public Map<String, String> parameters() {
            return parameters == null ? Map.of() : parameters;
        }
    }
}
