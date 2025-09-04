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

package io.ballerina.flowmodelgenerator.extension.dataloadermanager;

import com.google.gson.JsonObject;
import io.ballerina.flowmodelgenerator.extension.request.SearchRequest;
import io.ballerina.modelgenerator.commons.AbstractLSTest;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Test for searching data loaders.
 *
 * @since 1.1.1
 */
public class DataLoaderSearchTest extends AbstractLSTest {
    private static final String DATA_LOADER_KIND_NAME = "DATA_LOADER";

    @DataProvider(name = "data-provider")
    @Override
    protected Object[] getConfigsList() {
        return new Object[][]{
                {Path.of("data_loaders.json")},
                {Path.of("data_loaders_with_existing_ai.json")},
        };
    }

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = configDir.resolve(config);
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);
        String filePath = sourceDir.resolve(testConfig.source()).toAbsolutePath().toString();
        Map<String, String> queryMap = getQueryMap(testConfig);
        SearchRequest searchRequest = new SearchRequest(DATA_LOADER_KIND_NAME, filePath, null, queryMap);
        JsonObject searchResult = getResponse(searchRequest);
        if (!searchResult.equals(testConfig.expectedDataLoaders())) {
            TestConfig updatedConfig = new TestConfig(testConfig.source(), testConfig.query(), searchResult);
            // updateConfig(configJsonPath, updatedConfig);
            compareJsonElements(searchResult, testConfig.expectedDataLoaders());
            Assert.fail(String.format("Failed test: '%s' Actual: `%s` Expected `%s`", configJsonPath,
                    searchResult, testConfig.expectedDataLoaders()));
        }
    }

    private static Map<String, String> getQueryMap(TestConfig testConfig) {
        Map<String, String> queryMap = null;
        if (testConfig.query != null) {
            queryMap = new HashMap<>();
            queryMap.put("q", testConfig.query);
        }
        return queryMap;
    }

    @Override
    protected String getResourceDir() {
        return "data_loader_manager";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return DataLoaderSearchTest.class;
    }

    @Override
    protected String getApiName() {
        return "search";
    }

    /**
     * Represents the test configuration for the flow model getNodeTemplate API.
     *
     * @param source              The source file path
     * @param query               The query string to search
     * @param expectedDataLoaders The expected set of data loaders
     */
    private record TestConfig(String source, String query, JsonObject expectedDataLoaders) {

    }
}
