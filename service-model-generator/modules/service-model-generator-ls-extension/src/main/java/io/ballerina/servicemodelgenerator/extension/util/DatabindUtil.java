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

package io.ballerina.servicemodelgenerator.extension.util;

import io.ballerina.compiler.syntax.tree.ArrayTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.RecordFieldNode;
import io.ballerina.compiler.syntax.tree.RecordTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.servicemodelgenerator.extension.model.Function;
import io.ballerina.servicemodelgenerator.extension.model.MetaData;
import io.ballerina.servicemodelgenerator.extension.model.Parameter;
import io.ballerina.servicemodelgenerator.extension.model.Service;
import io.ballerina.servicemodelgenerator.extension.model.Value;
import io.ballerina.servicemodelgenerator.extension.model.context.ModelFromSourceContext;

import java.util.List;
import java.util.Map;

import static io.ballerina.servicemodelgenerator.extension.util.Constants.DATA_BINDING;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.DATA_BINDING_PROPERTY;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.DATA_BINDING_TEMPLATE;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.EMPTY_ARRAY;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.KIND_REQUIRED;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.getFunctionModel;

/**
 * Utility class for data binding parameter operations.
 *
 * @since 1.2.0
 */
public final class DatabindUtil {

    private DatabindUtil() {
    }

    /**
     * Finds matching functions from both service model and source code.
     *
     * @param service      The service model containing functions
     * @param functionName Name of the function to find
     * @param serviceNode  The ServiceDeclarationNode from source
     * @return FunctionMatch containing the matched functions, or null if target function not found
     */
    private static FunctionMatch findMatchingFunctions(Service service, String functionName,
                                                       ServiceDeclarationNode serviceNode) {
        List<FunctionDefinitionNode> functionNodesInSource = serviceNode.members().stream()
                .filter(member -> member instanceof FunctionDefinitionNode)
                .map(member -> (FunctionDefinitionNode) member)
                .toList();

        List<Function> functionsInSource = functionNodesInSource.stream()
                .map(member -> getFunctionModel(member, Map.of()))
                .toList();

        Function targetFunction = null;
        for (Function function : service.getFunctions()) {
            if (function.getName().getValue().equals(functionName)) {
                targetFunction = function;
                break;
            }
        }

        if (targetFunction == null) {
            return null;
        }

        // Find matching function in source
        Function sourceFunction = null;
        FunctionDefinitionNode sourceFunctionNode = null;
        for (int i = 0; i < functionsInSource.size(); i++) {
            Function function = functionsInSource.get(i);
            if (function.getName().getValue().equals(functionName)) {
                sourceFunction = function;
                sourceFunctionNode = functionNodesInSource.get(i);
                break;
            }
        }

        return new FunctionMatch(targetFunction, sourceFunction, sourceFunctionNode);
    }

    /**
     * Determines data binding configuration by analyzing source function. Extracts the data binding type from record
     * type descriptors if present.
     *
     * @param sourceFunction     The function from source code
     * @param sourceFunctionNode The FunctionDefinitionNode from source
     * @return DataBindingInfo containing enabled state, parameter type, and name
     */
    private static DataBindingInfo determineDataBindingInfo(Function sourceFunction,
                                                            FunctionDefinitionNode sourceFunctionNode) {
        boolean dataBindingEnabled = false;
        String paramType = "";
        String paramName = "";

        if (sourceFunction != null && !sourceFunction.getParameters().isEmpty() && sourceFunctionNode != null) {
            Parameter sourceParam = sourceFunction.getParameters().getFirst();
            paramName = sourceParam.getName().getValue();

            String dataBindingType = extractDataBindingType(sourceFunctionNode, paramName);

            if (dataBindingType != null) {
                dataBindingEnabled = true;
                paramType = dataBindingType;
            }
        }

        return new DataBindingInfo(dataBindingEnabled, paramType, paramName);
    }

    /**
     * Creates a data binding parameter with the specified configuration.
     *
     * @param paramType          The parameter type value
     * @param paramName          The parameter name value
     * @param dataBindingEnabled Whether the parameter should be enabled
     * @return The created Parameter object
     */
    private static Parameter createDataBindingParam(String paramType, String paramName,
                                                    boolean dataBindingEnabled) {
        Value parameterType = new Value.ValueBuilder()
                .valueType(Constants.VALUE_TYPE_TYPE)
                .value(paramType)
                .enabled(true)
                .editable(false)
                .build();

        Value parameterNameValue = new Value.ValueBuilder()
                .valueType(Constants.VALUE_TYPE_IDENTIFIER)
                .value(paramName)
                .enabled(true)
                .editable(false)
                .build();

        return new Parameter.Builder()
                .metadata(new MetaData("Data Binding", "Data binding parameter"))
                .kind(DATA_BINDING)
                .type(parameterType)
                .name(parameterNameValue)
                .enabled(dataBindingEnabled)
                .editable(true)
                .optional(false)
                .build();
    }

    /**
     * Extracts the data binding type from a record type descriptor in a function parameter. For example, from a
     * parameter with type "record {*rabbitmq:AnydataMessage; Order content;}", this method extracts "Order".
     *
     * @param functionNode The FunctionDefinitionNode to analyze
     * @param paramName    The name of the parameter to extract the type from
     * @return The extracted data binding type, or null if not found
     */
    private static String extractDataBindingType(FunctionDefinitionNode functionNode, String paramName) {
        java.util.Optional<RequiredParameterNode> targetParam = functionNode.functionSignature().parameters().stream()
                .filter(paramNode -> paramNode instanceof RequiredParameterNode)
                .map(paramNode -> (RequiredParameterNode) paramNode)
                .filter(reqParam -> reqParam.paramName().isPresent() &&
                        reqParam.paramName().get().text().trim().equals(paramName))
                .findFirst();

        if (targetParam.isEmpty()) {
            return null;
        }

        Node recordParam = targetParam.get().typeName();

        if (recordParam instanceof ArrayTypeDescriptorNode arrayTypeNode) {
            recordParam = arrayTypeNode.memberTypeDesc();
        }

        if (!(recordParam instanceof RecordTypeDescriptorNode recordType)) {
            return null;
        }

        for (Node field : recordType.fields()) {
            if (!(field instanceof RecordFieldNode recordField)) {
                continue;
            }
            return recordField.typeName().toString().trim();
        }

        return null;
    }

    /**
     * Adds a data binding parameter to the specified function in the service. This method analyzes the source code to
     * determine if data binding is being used and adds the appropriate DATA_BINDING parameter.
     *
     * @param service      The service model containing the functions
     * @param functionName Name of the function to add the data binding parameter to
     * @param context      ModelFromSourceContext to access the source node
     */
    public static void addDataBindingParam(Service service, String functionName, ModelFromSourceContext context) {
        ServiceDeclarationNode serviceNode = (ServiceDeclarationNode) context.node();

        FunctionMatch match = findMatchingFunctions(service, functionName, serviceNode);
        if (match == null || match.targetFunction() == null) {
            return;
        }

        DataBindingInfo dataBindingInfo = determineDataBindingInfo(
                match.sourceFunction(),
                match.sourceFunctionNode()
        );

        Parameter dataBindingParam = createDataBindingParam(
                dataBindingInfo.paramType(),
                dataBindingInfo.paramName(),
                dataBindingInfo.enabled()
        );

        match.targetFunction().addParameter(dataBindingParam);
        match.targetFunction().getCodedata().setModuleName(service.getModuleName());
        match.targetFunction().addProperty(DATA_BINDING_PROPERTY,
                new Value.ValueBuilder().value("true").build()
        );
    }

    /**
     * Creates an inline record type for data binding. Ex. "record {*rabbitmq:AnydataMessage; Order content;}"
     *
     * @param requiredParamType The default parameter type (e.g., "rabbitmq:AnydataMessage")
     * @param dataBindingType   The data binding type (e.g., "Order")
     * @param payloadFieldName  The field name for the payload (e.g., "content")
     * @param isArray           Whether the parameter is an array type
     * @return The inline record type string
     */
    public static String createInlineRecordType(String requiredParamType, String dataBindingType,
                                                String payloadFieldName, boolean isArray) {
        return String.format(DATA_BINDING_TEMPLATE, requiredParamType, dataBindingType, payloadFieldName,
                isArray ? EMPTY_ARRAY : "");
    }

    /**
     * Processes the data binding parameter for functions during code generation. If a DATA_BINDING parameter is
     * enabled, it generates the inline anonymous record type and sets it as the type of the first REQUIRED parameter,
     * then disables the DATA_BINDING parameter.
     *
     * @param function          The function containing the parameters
     * @param requiredParamType The required parameter type (e.g., "rabbitmq:AnydataMessage")
     * @param payloadFieldName  The field name for the payload (e.g., "content")
     * @param isArray           Whether the parameter is an array type
     */
    public static void processDataBindingParameter(Function function, String requiredParamType,
                                                   String payloadFieldName, boolean isArray) {
        List<Parameter> parameters = function.getParameters();
        if (parameters.isEmpty()) {
            return;
        }

        // Find the enabled DATA_BINDING parameter
        Parameter dataBindingParam = null;
        for (Parameter param : parameters) {
            if (DATA_BINDING.equals(param.getKind()) && param.isEnabled()) {
                dataBindingParam = param;
                break;
            }
        }

        if (dataBindingParam == null) {
            return;
        }

        String dataBindingType = dataBindingParam.getType().getValue();
        if (dataBindingType == null || dataBindingType.isEmpty()) {
            return;
        }

        String inlineRecordType = createInlineRecordType(requiredParamType, dataBindingType, payloadFieldName, isArray);

        for (Parameter param : parameters) {
            if (KIND_REQUIRED.equals(param.getKind())) {
                param.getType().setValue(inlineRecordType);
                param.setEnabled(true);
                break;
            }
        }

        dataBindingParam.setEnabled(false);
    }

    /**
     * Record to hold matching function references from service and source.
     *
     * @param targetFunction     The function from the service model
     * @param sourceFunction     The function parsed from source code
     * @param sourceFunctionNode The FunctionDefinitionNode from source
     */
    public record FunctionMatch(
            Function targetFunction,
            Function sourceFunction,
            FunctionDefinitionNode sourceFunctionNode
    ) { }

    /**
     * Record to hold data binding configuration information.
     *
     * @param enabled   Whether data binding should be enabled
     * @param paramType The parameter type to use
     * @param paramName The parameter name to use
     */
    public record DataBindingInfo(
            boolean enabled,
            String paramType,
            String paramName
    ) { }
}
