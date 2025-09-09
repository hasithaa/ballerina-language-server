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

import io.ballerina.artifactsgenerator.Artifact;
import io.ballerina.artifactsgenerator.ArtifactGenerationDebouncer;
import io.ballerina.artifactsgenerator.ArtifactsCache;
import io.ballerina.designmodelgenerator.extension.request.ArtifactsRequest;
import io.ballerina.designmodelgenerator.extension.response.ArtifactsParams;
import io.ballerina.modelgenerator.commons.AbstractLSTest;
import org.ballerinalang.langserver.LSContextOperation;
import org.ballerinalang.langserver.commons.DocumentServiceContext;
import org.ballerinalang.langserver.commons.client.ExtendedLanguageClient;
import org.ballerinalang.langserver.commons.workspace.WorkspaceDocumentException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.ballerinalang.langserver.contexts.ContextBuilder;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Test cases for publishing artifacts.
 *
 * @since 1.0.0
 */
public class PublishArtifactsSubscriberTest extends AbstractLSTest {

    private final PublishArtifactsSubscriber publishArtifactsSubscriber = new PublishArtifactsSubscriber();

    @BeforeClass(dependsOnMethods = {"init"})
    public void initializeProject() {
        // Load the original project
        String sourcePath = getSourcePath("old");
        ArtifactsRequest request = new ArtifactsRequest(sourcePath);
        getResponse(request, "designModelService/artifacts");

        // Wait until the project cache is populated
        ArtifactsCache cache = ArtifactsCache.getInstance();
        long startTime = System.currentTimeMillis();
        long timeout = 5000; // 5 seconds timeout
        String newPath = null;
        try {
            while (System.currentTimeMillis() - startTime < timeout) {
                // Use reflection to access the private projectCache field
                Field projectCacheField = ArtifactsCache.class.getDeclaredField("projectCache");
                projectCacheField.setAccessible(true);
                @SuppressWarnings("unchecked")
                Map<String, Object> projectMap = (Map<String, Object>) projectCacheField.get(cache);
                if (projectMap != null && projectMap.containsKey(sourcePath)) {
                    // Get the artifact for old file
                    Object oldArtifact = projectMap.get(sourcePath);

                    // Create the new path by replacing the filename
                    Path path = Path.of(sourcePath);
                    Path parent = path.getParent();
                    newPath = parent.resolve("new").toString();

                    // Put the same artifact with new key
                    projectMap.remove(sourcePath);
                    projectMap.put(newPath, oldArtifact);
                    break;
                }
                Thread.sleep(100);
            }

            if (newPath == null) {
                Assert.fail("Timed out waiting for project cache to be populated");
            }
        } catch (Exception e) {
            Assert.fail("Error while setting up test data in project cache", e);
        }
    }

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = configDir.resolve(config);
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);

        // Create a document service context
        WorkspaceManager workspaceManager = languageServer.getWorkspaceManager();
        String sourcePath = getSourcePath(testConfig.source());
        Path filePath = Path.of(sourcePath);
        String fileUri;
        try {
            Path path = Path.of(sourcePath).toAbsolutePath().normalize();
            fileUri = path.toUri().toString();
        } catch (Exception e) {
            Assert.fail("Error while creating the file uri", e);
            return;
        }
        DocumentServiceContext documentServiceContext = ContextBuilder.buildDocumentServiceContext(
                fileUri,
                workspaceManager,
                LSContextOperation.TXT_DID_CHANGE,
                languageServer.getServerContext()
        );
        VersionedTextDocumentIdentifier versionedTextDocumentIdentifier = new VersionedTextDocumentIdentifier();
        List<TextDocumentContentChangeEvent> changeEvents =
                List.of(new TextDocumentContentChangeEvent(getText(sourcePath)));

        // Send the didChange notification
        try {
            workspaceManager.didChange(filePath,
                    new DidChangeTextDocumentParams(versionedTextDocumentIdentifier, changeEvents));
        } catch (WorkspaceDocumentException e) {
            Assert.fail("Error while sending didChange notification", e);
        }

        // Create a mock client using Mockito
        ExtendedLanguageClient mockClient = Mockito.mock(ExtendedLanguageClient.class);

        // Invoke the subscriber with mock client
        publishArtifactsSubscriber.onEvent(
                mockClient,
                documentServiceContext,
                languageServer.getServerContext());

        // Capture the artifacts published to the client - they are Object[] arrays
        ArgumentCaptor<Object> artifactsCaptor = ArgumentCaptor.forClass(Object.class);

        // Add a wait loop to verify that all scheduled tasks have completed
        // and the delayedMap is empty before proceeding with verification
        ArtifactGenerationDebouncer debouncer = ArtifactGenerationDebouncer.getInstance();
        long startTime = System.currentTimeMillis();
        long timeout = 5000; // 5 seconds timeout
        boolean isEmpty = false;
        try {
            // Wait for debouncer to finish processing (max 5 seconds)
            while (System.currentTimeMillis() - startTime < timeout) {
                // Use reflection to access the private delayedMap field
                Field delayedMapField = ArtifactGenerationDebouncer.class.getDeclaredField("delayedMap");
                delayedMapField.setAccessible(true);
                ConcurrentHashMap<?, ?> map =
                        (ConcurrentHashMap<?, ?>) delayedMapField.get(debouncer);

                // Check if the request for the fileUri is completed
                if (map.get(fileUri) == null) {
                    isEmpty = true;
                    break;
                }

                // Small delay to avoid tight loop
                Thread.sleep(100);
            }

            if (!isEmpty) {
                Assert.fail("Timed out waiting for debouncer to finish processing");
            }
        } catch (Exception e) {
            Assert.fail("Error while checking debouncer state", e);
        }

        // Verify the client was called with the expected artifacts
        Mockito.verify(mockClient).publishArtifacts(artifactsCaptor.capture());
        Object capturedValue = artifactsCaptor.getValue();

        @SuppressWarnings("unchecked")
        ArtifactsParams artifactsParams = (ArtifactsParams) capturedValue;
        Map<String, Map<String, Map<String, Artifact>>> expectedArtifacts = testConfig.output();

        // Retrieve and validate the captured URI
        String uri = artifactsParams.uri();
        if (uri == null) {
            Assert.fail("Failed to capture the uri");
        }
        if (!workspaceManager.projectRoot(filePath).toUri().toString().equals(uri)) {
            Assert.fail("Failed to capture the correct uri");
        }

        // Assert the published artifacts
        Map<String, Map<String, Map<String, Artifact>>> publishedArtifacts = artifactsParams.artifacts();
        if (!publishedArtifacts.equals(expectedArtifacts)) {
            TestConfig updatedConfig =
                    new TestConfig(testConfig.source(), testConfig.description(), publishedArtifacts);
//            updateConfig(configJsonPath, updatedConfig);
            compareJsonElements(gson.toJsonTree(publishedArtifacts), gson.toJsonTree(expectedArtifacts));
            Assert.fail(String.format("Failed test: '%s' (%s)", testConfig.source(), configJsonPath));
        }
    }

    @Override
    protected String getResourceDir() {
        return "publish_artifacts";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return PublishArtifactsSubscriberTest.class;
    }

    @Override
    protected String getApiName() {
        return "publishArtifacts";
    }

    @Test
    public void testReloadProjectWithPreExistingArtifacts() throws Exception {
        // Create mock document service context for reload project operation
        String sourcePath = getSourcePath("old");
        WorkspaceManager workspaceManager = languageServer.getWorkspaceManager();
        Path filePath = Path.of(sourcePath);
        String fileUri = filePath.toAbsolutePath().normalize().toUri().toString();
        DocumentServiceContext documentServiceContext = ContextBuilder.buildDocumentServiceContext(
                fileUri,
                workspaceManager,
                LSContextOperation.RELOAD_PROJECT,
                languageServer.getServerContext()
        );

        // Invoke the subscriber
        ExtendedLanguageClient mockClient = Mockito.mock(ExtendedLanguageClient.class);
        publishArtifactsSubscriber.onEvent(
                mockClient,
                documentServiceContext,
                languageServer.getServerContext()
        );

        // Wait for debouncer to complete (using project-level key)
        Path projectPath = workspaceManager.projectRoot(filePath);
        String projectKey = projectPath.toUri().toString();
        waitForDebouncerCompletion(projectKey);

        // Verify the client was called
        ArgumentCaptor<ArtifactsParams> artifactsCaptor = ArgumentCaptor.forClass(ArtifactsParams.class);
        Mockito.verify(mockClient).publishArtifacts(artifactsCaptor.capture());
        ArtifactsParams artifactsParams = artifactsCaptor.getValue();

        // Verify URI is project-level
        Assert.assertEquals(artifactsParams.uri(), projectKey);

        // Verify delta changes contain deletions and additions
        Map<String, Map<String, Map<String, Artifact>>> publishedArtifacts = artifactsParams.artifacts();
        Assert.assertNotNull(publishedArtifacts, "Published artifacts should not be null");

        // Should have deletions (from cached artifacts) and additions/updates (from current project)
        boolean hasDeletions = publishedArtifacts.values().stream()
                .anyMatch(categoryMap -> categoryMap.containsKey("deletions"));
        boolean hasAdditions = publishedArtifacts.values().stream()
                .anyMatch(categoryMap -> categoryMap.containsKey("additions"));

        Assert.assertTrue(hasDeletions || hasAdditions,
                "Should have either deletions or additions/updates in reload project delta");
    }

    @Test
    public void testReloadProjectWithoutPreExistingArtifacts() throws Exception {
        // Setup: Clear cache to simulate no pre-existing artifacts
        clearProjectCache();

        // Create mock document service context for reload project operation
        WorkspaceManager workspaceManager = languageServer.getWorkspaceManager();
        String sourcePath = getSourcePath("old");
        Path filePath = Path.of(sourcePath);
        String fileUri = filePath.toAbsolutePath().normalize().toUri().toString();

        DocumentServiceContext documentServiceContext = ContextBuilder.buildDocumentServiceContext(
                fileUri,
                workspaceManager,
                LSContextOperation.RELOAD_PROJECT,
                languageServer.getServerContext()
        );

        // Invoke the subscriber
        ExtendedLanguageClient mockClient = Mockito.mock(ExtendedLanguageClient.class);
        publishArtifactsSubscriber.onEvent(
                mockClient,
                documentServiceContext,
                languageServer.getServerContext()
        );

        // Wait for debouncer to complete (using project-level key)
        Path projectPath = workspaceManager.projectRoot(filePath);
        String projectKey = projectPath.toUri().toString();
        waitForDebouncerCompletion(projectKey);

        // Verify the client was called
        ArgumentCaptor<ArtifactsParams> artifactsCaptor = ArgumentCaptor.forClass(ArtifactsParams.class);
        Mockito.verify(mockClient).publishArtifacts(artifactsCaptor.capture());
        ArtifactsParams artifactsParams = artifactsCaptor.getValue();

        // Verify URI is project-level
        Assert.assertEquals(artifactsParams.uri(), projectKey);

        // Verify delta changes contain only additions (no pre-existing artifacts)
        Map<String, Map<String, Map<String, Artifact>>> publishedArtifacts = artifactsParams.artifacts();
        Assert.assertNotNull(publishedArtifacts, "Published artifacts should not be null");

        // Should only have additions, no deletions or updates
        boolean hasDeletions = publishedArtifacts.values().stream()
                .anyMatch(categoryMap -> categoryMap.containsKey("deletions"));
        boolean hasAdditions = publishedArtifacts.values().stream()
                .anyMatch(categoryMap -> categoryMap.containsKey("additions"));

        Assert.assertFalse(hasDeletions, "Should not have deletions when no pre-existing artifacts");
        Assert.assertTrue(hasAdditions, "Should have additions for new project artifacts");
    }

    private void waitForDebouncerCompletion(String key) throws Exception {
        ArtifactGenerationDebouncer debouncer = ArtifactGenerationDebouncer.getInstance();
        long startTime = System.currentTimeMillis();
        long timeout = 5000; // 5 seconds timeout

        while (System.currentTimeMillis() - startTime < timeout) {
            Field delayedMapField = ArtifactGenerationDebouncer.class.getDeclaredField("delayedMap");
            delayedMapField.setAccessible(true);
            ConcurrentHashMap<?, ?> map = (ConcurrentHashMap<?, ?>) delayedMapField.get(debouncer);

            if (map.get(key) == null) {
                return;
            }
            Thread.sleep(100);
        }
        Assert.fail("Timed out waiting for debouncer to finish processing");
    }

    private void clearProjectCache() throws Exception {
        ArtifactsCache cache = ArtifactsCache.getInstance();
        Field projectCacheField = ArtifactsCache.class.getDeclaredField("projectCache");
        projectCacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> projectMap = (Map<String, Object>) projectCacheField.get(cache);
        if (projectMap != null) {
            projectMap.clear();
        }
    }

    private record TestConfig(String source, String description,
                              Map<String, Map<String, Map<String, Artifact>>> output) {

        public String description() {
            return description == null ? "" : description;
        }
    }

}
