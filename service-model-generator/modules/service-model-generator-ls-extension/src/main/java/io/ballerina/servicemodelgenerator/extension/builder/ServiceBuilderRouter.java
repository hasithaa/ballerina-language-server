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

package io.ballerina.servicemodelgenerator.extension.builder;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.projects.Document;
import io.ballerina.projects.Project;
import io.ballerina.servicemodelgenerator.extension.builder.service.AiChatServiceBuilder;
import io.ballerina.servicemodelgenerator.extension.builder.service.DefaultServiceBuilder;
import io.ballerina.servicemodelgenerator.extension.builder.service.GraphqlServiceBuilder;
import io.ballerina.servicemodelgenerator.extension.builder.service.HttpServiceBuilder;
import io.ballerina.servicemodelgenerator.extension.builder.service.RabbitMQServiceBuilder;
import io.ballerina.servicemodelgenerator.extension.builder.service.TCPServiceBuilder;
import io.ballerina.servicemodelgenerator.extension.builder.service.McpServiceBuilder;
import io.ballerina.servicemodelgenerator.extension.model.Service;
import io.ballerina.servicemodelgenerator.extension.model.ServiceInitModel;
import io.ballerina.servicemodelgenerator.extension.model.ServiceMetadata;
import io.ballerina.servicemodelgenerator.extension.model.context.AddModelContext;
import io.ballerina.servicemodelgenerator.extension.model.context.AddServiceInitModelContext;
import io.ballerina.servicemodelgenerator.extension.model.context.GetModelContext;
import io.ballerina.servicemodelgenerator.extension.model.context.GetServiceInitModelContext;
import io.ballerina.servicemodelgenerator.extension.model.context.ModelFromSourceContext;
import io.ballerina.servicemodelgenerator.extension.model.context.UpdateModelContext;
import io.ballerina.servicemodelgenerator.extension.model.request.ServiceModelRequest;
import io.ballerina.servicemodelgenerator.extension.util.ServiceModelUtils;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.TextEdit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import static io.ballerina.servicemodelgenerator.extension.util.Constants.AI;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.GRAPHQL;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.HTTP;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.MCP;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.RABBITMQ;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.TCP;

/**
 * ServiceBuilderRouter is responsible for routing service building requests to the appropriate service builder
 * based on the protocol type.
 *
 * @since 1.2.0
 */
public class ServiceBuilderRouter {

    private static final Map<String, Supplier<? extends ServiceNodeBuilder>> CONSTRUCTOR_MAP = new HashMap<>() {{
        put(HTTP, HttpServiceBuilder::new);
        put(AI, AiChatServiceBuilder::new);
        put(TCP, TCPServiceBuilder::new);
        put(RABBITMQ, RabbitMQServiceBuilder::new);
        put(GRAPHQL, GraphqlServiceBuilder::new);
        put(MCP, McpServiceBuilder::new);
    }};

    public static ServiceNodeBuilder getServiceBuilder(String protocol) {
        return CONSTRUCTOR_MAP.getOrDefault(protocol, DefaultServiceBuilder::new).get();
    }

    public static Optional<Service> getModelTemplate(String orgName, String moduleName) {
        NodeBuilder<?> serviceBuilder = getServiceBuilder(moduleName);
        GetModelContext context = GetModelContext.fromOrgAndModule(orgName, moduleName);
        Optional<?> modelTemplate = serviceBuilder.getModelTemplate(context);
        if (modelTemplate.isEmpty() || !(modelTemplate.get() instanceof Service)) {
            return Optional.empty();
        }
        return Optional.of((Service) modelTemplate.get());
    }

    public static Service getServiceFromSource(Node node, Project project,
                                               SemanticModel semanticModel,
                                               WorkspaceManager workspaceManager) {
        ServiceMetadata serviceMetadata = ServiceModelUtils.deriveServiceType(
                (ServiceDeclarationNode) node, semanticModel);
        if (Objects.isNull(serviceMetadata.orgName()) || Objects.isNull(serviceMetadata.packageName())) {
            return null;
        }
        NodeBuilder<Service> serviceBuilder = getServiceBuilder(serviceMetadata.moduleName());
        ModelFromSourceContext context = new ModelFromSourceContext(node, project, semanticModel,
                workspaceManager, serviceMetadata.serviceType(), serviceMetadata.orgName(),
                serviceMetadata.packageName(), serviceMetadata.moduleName());
        return serviceBuilder.getModelFromSource(context);
    }

    public static Map<String, List<TextEdit>> addService(Service service,
                                                         SemanticModel semanticModel, Project project,
                                                         WorkspaceManager workspaceManager,
                                                         String filePath, Document document) throws Exception {
        NodeBuilder<Service> serviceBuilder = getServiceBuilder(service.getModuleName());
        AddModelContext context = new AddModelContext(service, null, semanticModel, project,
                workspaceManager, filePath, document, null);
        return serviceBuilder.addModel(context);
    }

    public static Map<String, List<TextEdit>> updateService(Service service,
                                                            SemanticModel semanticModel,
                                                            WorkspaceManager workspaceManager,
                                                            String filePath, Document document,
                                                            ServiceDeclarationNode serviceNode) throws Exception {
        NodeBuilder<?> serviceBuilder = getServiceBuilder(service.getModuleName());
        UpdateModelContext context = new UpdateModelContext(service, null, semanticModel, null,
                workspaceManager, filePath, document, serviceNode, null);
        return serviceBuilder.updateModel(context);
    }

    public static ServiceInitModel getServiceInitModel(ServiceModelRequest request, Project project,
                                                       SemanticModel semanticModel, Document document) {
        ServiceNodeBuilder serviceBuilder = getServiceBuilder(request.moduleName());
        GetServiceInitModelContext context = new GetServiceInitModelContext(
                request.orgName(), request.pkgName(), request.moduleName(), project, semanticModel, document);
        return serviceBuilder.getServiceInitModel(context);
    }

    public static Map<String, List<TextEdit>> addServiceInitSource(ServiceInitModel serviceInitModel,
                                                                   SemanticModel semanticModel,
                                                                   Project project, WorkspaceManager workspaceManager,
                                                                   String filePath,
                                                                   Document document)
            throws Exception {
        ServiceNodeBuilder serviceBuilder = getServiceBuilder(serviceInitModel.getModuleName());
        AddServiceInitModelContext context = new AddServiceInitModelContext(serviceInitModel, semanticModel, project,
                workspaceManager, filePath, document);
        return serviceBuilder.addServiceInitSource(context);
    }
}
