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
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.projects.Document;
import io.ballerina.servicemodelgenerator.extension.builder.function.DefaultFunctionBuilder;
import io.ballerina.servicemodelgenerator.extension.builder.function.GraphqlFunctionBuilder;
import io.ballerina.servicemodelgenerator.extension.builder.function.HttpFunctionBuilder;
import io.ballerina.servicemodelgenerator.extension.builder.function.McpFunctionBuilder;
import io.ballerina.servicemodelgenerator.extension.builder.function.RabbitMQFunctionBuilder;
import io.ballerina.servicemodelgenerator.extension.model.Function;
import io.ballerina.servicemodelgenerator.extension.model.ServiceMetadata;
import io.ballerina.servicemodelgenerator.extension.model.context.AddModelContext;
import io.ballerina.servicemodelgenerator.extension.model.context.GetModelContext;
import io.ballerina.servicemodelgenerator.extension.model.context.ModelFromSourceContext;
import io.ballerina.servicemodelgenerator.extension.model.context.UpdateModelContext;
import org.eclipse.lsp4j.TextEdit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static io.ballerina.servicemodelgenerator.extension.util.Constants.GRAPHQL;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.HTTP;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.MCP;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.RABBITMQ;
import static io.ballerina.servicemodelgenerator.extension.util.ServiceModelUtils.deriveServiceType;

/**
 * Represents the function builder router of the service model generator.
 *
 * @since 1.2.0
 */
public class FunctionBuilderRouter {
    private static final Map<String, Supplier<? extends NodeBuilder<Function>>> CONSTRUCTOR_MAP = new HashMap<>() {{
        put(HTTP, HttpFunctionBuilder::new);
        put(GRAPHQL, GraphqlFunctionBuilder::new);
        put(RABBITMQ, RabbitMQFunctionBuilder::new);
        put(MCP, McpFunctionBuilder::new);
    }};

    private static NodeBuilder<Function> getFunctionBuilder(String protocol) {
        return CONSTRUCTOR_MAP.getOrDefault(protocol, DefaultFunctionBuilder::new).get();
    }

    public static Optional<Function> getModelTemplate(String moduleName, String functionType) {
        NodeBuilder<Function> functionBuilder = getFunctionBuilder(moduleName);
        GetModelContext context = GetModelContext.fromServiceAndFunctionType(moduleName, functionType);
        return functionBuilder.getModelTemplate(context);
    }

    public static Map<String, List<TextEdit>> addFunction(String moduleName, Function function, String filePath,
                                                          Document document, NonTerminalNode node) throws Exception {
        NodeBuilder<Function> functionBuilder = getFunctionBuilder(moduleName);
        AddModelContext context = new AddModelContext(null, function, null, null, null, filePath, document, node);
        return functionBuilder.addModel(context);
    }

    public static Map<String, List<TextEdit>> updateFunction(String moduleName, Function function, String filePath,
                                                             Document document, FunctionDefinitionNode functionNode)
            throws Exception {
        NodeBuilder<Function> functionBuilder = getFunctionBuilder(moduleName);
        UpdateModelContext context = new UpdateModelContext(null, function, null, null, null, filePath,
                document, null, functionNode);
        return functionBuilder.updateModel(context);
    }

    public static Function getFunctionFromSource(String moduleName, SemanticModel semanticModel, Node functionNode) {
        ModelFromSourceContext context;
        if (functionNode.parent() instanceof ServiceDeclarationNode serviceDeclarationNode) {
            ServiceMetadata metadata = deriveServiceType(serviceDeclarationNode, semanticModel);
            context = new ModelFromSourceContext(functionNode, null, semanticModel, null,
                    metadata.serviceTypeIdentifier(), metadata.orgName(), metadata.packageName(),
                    metadata.moduleName());
        } else {
            context = new ModelFromSourceContext(functionNode, null, semanticModel, null,
                    moduleName, null, null, moduleName);
        }
        NodeBuilder<Function> functionBuilder = getFunctionBuilder(context.moduleName());
        return functionBuilder.getModelFromSource(context);
    }
}
