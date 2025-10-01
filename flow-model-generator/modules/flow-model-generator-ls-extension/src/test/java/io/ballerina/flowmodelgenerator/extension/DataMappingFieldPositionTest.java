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

package io.ballerina.flowmodelgenerator.extension;

import com.google.gson.JsonElement;
import io.ballerina.flowmodelgenerator.extension.request.DataMapperFieldPositionRequest;
import io.ballerina.modelgenerator.commons.AbstractLSTest;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tests for the getting the field position and text edits.
 *
 * @since 1.0.0
 */
public class DataMappingFieldPositionTest extends AbstractLSTest {

    @DataProvider(name = "data-provider")
    @Override
    protected Object[] getConfigsList() {
        return new Object[][]{
                {Path.of("variable1.json")},
                {Path.of("variable1_1.json")},
                {Path.of("variable1_2.json")},
                {Path.of("variable2.json")},
                {Path.of("variable2_1.json")},
                {Path.of("variable3.json")},
                {Path.of("function_definition1.json")},
                {Path.of("variable4.json")},
        };
    }

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = configDir.resolve(config);
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);

        DataMapperFieldPositionRequest request =
                new DataMapperFieldPositionRequest(sourceDir.resolve(testConfig.source()).toAbsolutePath().toString(),
                        testConfig.codedata(), testConfig.propertyKey(), testConfig.targetField(),
                        testConfig.fieldId());
        JsonElement property = getResponseAndCloseFile(request, testConfig.source()).getAsJsonObject("property");

        if (!property.equals(testConfig.property())) {
            TestConfig updatedConfig = new TestConfig(testConfig.source(), testConfig.description(),
                    testConfig.codedata(), testConfig.propertyKey(), testConfig.targetField(), testConfig.fieldId(),
                    property);
//            updateConfig(configJsonPath, updatedConfig);
            Assert.fail(String.format("Failed test: '%s' (%s)", testConfig.description(), configJsonPath));
        }
    }

    @Override
    protected String getResourceDir() {
        return "data_mapper_field_position";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return DataMappingTypesTest.class;
    }

    @Override
    protected String getApiName() {
        return "fieldPosition";
    }

    @Override
    protected String getServiceName() {
        return "dataMapper";
    }

    /**
     * Represents the test configuration for the field position test.
     *
     * @param source      The source file name
     * @param description The description of the test
     * @param codedata    Details of the node
     * @param propertyKey The property key
     * @param targetField The target field to add the element
     * @param fieldId     The field ID to identify the specific field to get the position
     * @param property    Type property of the type of field ID
     */
    private record TestConfig(String source, String description, JsonElement codedata, String propertyKey,
                              String targetField, String fieldId, JsonElement property) {

        public String description() {
            return description == null ? "" : description;
        }
    }
}
