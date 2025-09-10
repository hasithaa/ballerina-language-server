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

package io.ballerina.servicemodelgenerator.extension;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import io.ballerina.modelgenerator.commons.AbstractLSTest;
import io.ballerina.servicemodelgenerator.extension.model.Service;
import io.ballerina.servicemodelgenerator.extension.model.Value;
import io.ballerina.servicemodelgenerator.extension.model.request.ServiceSourceRequest;
import org.eclipse.lsp4j.TextEdit;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests for the service model source generator addService service.
 *
 * @since 1.0.0
 */
public class AddServiceTest extends AbstractLSTest {

    private static final Type TEXT_EDIT_LIST_TYPE = new TypeToken<Map<String, List<TextEdit>>>() {
    }.getType();

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = configDir.resolve(config);
        BufferedReader bufferedReader = Files.newBufferedReader(configJsonPath);
        TestConfig testConfig = gson.fromJson(bufferedReader, TestConfig.class);
        bufferedReader.close();

        ServiceSourceRequest request = new ServiceSourceRequest(
                sourceDir.resolve(testConfig.filePath()).toAbsolutePath().toString(), testConfig.service());
        JsonObject jsonMap = getResponse(request).getAsJsonObject("textEdits");

        Map<String, List<TextEdit>> actualTextEdits = gson.fromJson(jsonMap, TEXT_EDIT_LIST_TYPE);

        assertResults(actualTextEdits, testConfig, configJsonPath);
    }

    @Test
    public void testAddingUsingOpenAPISpec() throws IOException {
        Path configJsonPath = configDir.resolve("add_http_service_from_openapi_spec.json");
        BufferedReader bufferedReader = Files.newBufferedReader(configJsonPath);
        TestConfig testConfig = gson.fromJson(bufferedReader, TestConfig.class);
        bufferedReader.close();

        Service service = testConfig.service();
        Value designApproach = service.getDesignApproach();
        Value openApiSpecChoice = designApproach.getChoices().get(1);
        Value specProperty = openApiSpecChoice.getProperty("spec");
        Path openApiSpecPath = sourceDir.resolve("sample1/openapi.yaml").toAbsolutePath();
        specProperty.setValue(openApiSpecPath.toString());
        ServiceSourceRequest request = new ServiceSourceRequest(
                sourceDir.resolve(testConfig.filePath()).toAbsolutePath().toString(), testConfig.service());
        JsonObject jsonMap = getResponse(request).getAsJsonObject("textEdits");
        Map<String, List<TextEdit>> actualTextEdits = gson.fromJson(jsonMap, TEXT_EDIT_LIST_TYPE);

        assertResults(actualTextEdits, testConfig, configJsonPath);
    }

    private void assertResults(Map<String, List<TextEdit>> actualTextEdits, TestConfig testConfig, Path configJsonPath)
            throws IOException {
        boolean assertFailure = false;

        if (actualTextEdits.size() != testConfig.output().size()) {
            log.info("The number of text edits does not match the expected output.");
            assertFailure = true;
        }

        Map<String, List<TextEdit>> newMap = new HashMap<>();
        for (Map.Entry<String, List<TextEdit>> entry : actualTextEdits.entrySet()) {
            Path fullPath = Paths.get(entry.getKey());
            String relativePath = sourceDir.relativize(fullPath).toString();

            List<TextEdit> textEdits = testConfig.output().get(relativePath.replace("\\", "/"));
            if (textEdits == null) {
                log.info("No text edits found for the file: " + relativePath);
                assertFailure = true;
            } else if (!assertArray("text edits", entry.getValue(), textEdits)) {
                assertFailure = true;
            }

            newMap.put(relativePath, entry.getValue());
        }

        if (assertFailure) {
            TestConfig updatedConfig =
                    new TestConfig(testConfig.filePath(), testConfig.description(), testConfig.service(), newMap);
//            updateConfig(configJsonPath, updatedConfig);
            Assert.fail(String.format("Failed test: '%s' (%s)", testConfig.description(), configJsonPath));
        }
    }

    @Override
    protected String[] skipList() {
        return new String[]{
                "add_http_service_from_openapi_spec.json"
        };
    }

    @Override
    protected String getResourceDir() {
        return "add_service";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return AddServiceTest.class;
    }

    @Override
    protected String getServiceName() {
        return "serviceDesign";
    }

    @Override
    protected String getApiName() {
        return "addService";
    }

    /**
     * Represents the test configuration for the source generator test.
     *
     * @param filePath    The path to the source file.
     * @param description The description of the test.
     * @param service     The service to be added.
     * @param output      The expected output.
     */
    private record TestConfig(String filePath, String description, Service service,
                              Map<String, List<TextEdit>> output) {

        public String description() {
            return description == null ? "" : description;
        }
    }
}
