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

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeDefinitionSymbol;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.BasicLiteralNode;
import io.ballerina.compiler.syntax.tree.DefaultableParameterNode;
import io.ballerina.compiler.syntax.tree.ExplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionSignatureNode;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.MappingConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MarkdownDocumentationLineNode;
import io.ballerina.compiler.syntax.tree.MarkdownDocumentationNode;
import io.ballerina.compiler.syntax.tree.MarkdownParameterDocumentationLineNode;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.MethodDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NameReferenceNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.ReturnTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;
import io.ballerina.modelgenerator.commons.CommonUtils;
import io.ballerina.projects.Document;
import io.ballerina.servicemodelgenerator.extension.model.Codedata;
import io.ballerina.servicemodelgenerator.extension.model.Function;
import io.ballerina.servicemodelgenerator.extension.model.FunctionReturnType;
import io.ballerina.servicemodelgenerator.extension.model.HttpResponse;
import io.ballerina.servicemodelgenerator.extension.model.MetaData;
import io.ballerina.servicemodelgenerator.extension.model.Parameter;
import io.ballerina.servicemodelgenerator.extension.model.Service;
import io.ballerina.servicemodelgenerator.extension.model.ServiceClass;
import io.ballerina.servicemodelgenerator.extension.model.TriggerProperty;
import io.ballerina.servicemodelgenerator.extension.model.Value;
import io.ballerina.servicemodelgenerator.extension.model.request.TriggerListRequest;
import io.ballerina.servicemodelgenerator.extension.model.request.TriggerRequest;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import org.ballerinalang.langserver.common.utils.NameUtil;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static io.ballerina.servicemodelgenerator.extension.util.Constants.ANNOT_PREFIX;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.GET;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.GRAPHQL_CONTEXT;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.GRAPHQL_FIELD;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.KIND_DEFAULT;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.KIND_DEFAULTABLE;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.KIND_MUTATION;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.KIND_QUERY;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.KIND_REMOTE;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.KIND_REQUIRED;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.KIND_RESOURCE;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.KIND_SUBSCRIPTION;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.NEW_LINE;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.REMOTE;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.RESOURCE;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.SPACE;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.SUBSCRIBE;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.VALUE_TYPE_EXPRESSION;
import static io.ballerina.servicemodelgenerator.extension.util.Constants.VALUE_TYPE_IDENTIFIER;
import static io.ballerina.servicemodelgenerator.extension.util.ServiceClassUtil.ServiceClassContext.SERVICE_DIAGRAM;
import static io.ballerina.servicemodelgenerator.extension.util.ServiceModelUtils.getProtocol;

/**
 * Common utility functions used in the project.
 *
 * @since 1.0.0
 */
public final class Utils {

    private Utils() {
    }

    /**
     * Convert the syntax-node line range into a lsp4j range.
     *
     * @param lineRange line range
     * @return {@link Range} converted range
     */
    public static Range toRange(LineRange lineRange) {
        return new Range(toPosition(lineRange.startLine()), toPosition(lineRange.endLine()));
    }

    /**
     * Converts syntax-node line position into a lsp4j position.
     *
     * @param position line position
     * @return {@link Range} converted range
     */
    public static Range toRange(LinePosition position) {
        return new Range(toPosition(position), toPosition(position));
    }

    /**
     * Converts syntax-node line position into a lsp4j position.
     *
     * @param linePosition - line position
     * @return {@link Position} converted position
     */
    public static Position toPosition(LinePosition linePosition) {
        return new Position(linePosition.line(), linePosition.offset());
    }

    public static void populateRequiredFuncsDesignApproachAndServiceType(Service service) {
        populateRequiredFunctions(service);
        populateServiceType(service);
        populateDesignApproach(service);
    }

    public static void populateRequiredFunctions(Service service) {
        Value value = service.getProperty(Constants.PROPERTY_REQUIRED_FUNCTIONS);
        if (Objects.nonNull(value) && value.isEnabledWithValue()) {
            String requiredFunction = value.getValue();
            service.getFunctions()
                    .forEach(function -> function.setEnabled(
                            function.getName().getValue().equals(requiredFunction)));
        }
    }

    private static void populateServiceType(Service service) {
        Value serviceValue = service.getServiceType();
        if (Objects.nonNull(serviceValue) && serviceValue.isEnabledWithValue()) {
            String serviceType = service.getServiceTypeName();
            if (Objects.nonNull(serviceType)) {
                getServiceByServiceType(serviceType.toLowerCase(Locale.ROOT))
                        .ifPresent(serviceTypeModel -> service.setFunctions(serviceTypeModel.getFunctions()));
            }
        }
    }

    public static void populateDesignApproach(Service service) {
        Value designApproach = service.getDesignApproach();
        if (Objects.nonNull(designApproach) && designApproach.isEnabled()
                && Objects.nonNull(designApproach.getChoices()) && !designApproach.getChoices().isEmpty()) {
            designApproach.getChoices().stream()
                    .filter(Value::isEnabled).findFirst()
                    .ifPresent(selectedApproach -> service.addProperties(selectedApproach.getProperties()));
            service.getProperties().remove(Constants.PROPERTY_DESIGN_APPROACH);
        }
    }

    private static Optional<Service> getServiceByServiceType(String serviceType) {
        InputStream resourceStream = Utils.class.getClassLoader()
                .getResourceAsStream(String.format("services/%s.json", serviceType.replaceAll(":", ".")));
        if (resourceStream == null) {
            return Optional.empty();
        }

        try (JsonReader reader = new JsonReader(new InputStreamReader(resourceStream, StandardCharsets.UTF_8))) {
            return Optional.of(new Gson().fromJson(reader, Service.class));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public static Optional<ExpressionNode> getListenerExpression(ServiceDeclarationNode serviceNode) {
        SeparatedNodeList<ExpressionNode> expressions = serviceNode.expressions();
        if (expressions.isEmpty()) {
            return Optional.empty();
        }
        ExpressionNode expressionNode = expressions.get(0);
        return Optional.of(expressionNode);
    }

    public static Optional<Symbol> getHttpServiceContractSym(SemanticModel semanticModel,
                                                             TypeDescriptorNode serviceTypeDesc) {
        Optional<Symbol> svcTypeSymbol = semanticModel.symbol(serviceTypeDesc);
        if (svcTypeSymbol.isEmpty() || !(svcTypeSymbol.get() instanceof TypeReferenceTypeSymbol svcTypeRef)) {
            return Optional.empty();
        }
        Optional<Symbol> contractSymbol = semanticModel.types().getTypeByName("ballerina", "http", "",
                "ServiceContract");
        if (contractSymbol.isEmpty() || !(contractSymbol.get() instanceof TypeDefinitionSymbol contractTypeDef)) {
            return Optional.empty();
        }
        if (svcTypeRef.subtypeOf(contractTypeDef.typeDescriptor())) {
            return svcTypeSymbol;
        }
        return Optional.empty();
    }

    public static String getPath(NodeList<Node> paths) {
        return paths.stream().map(Node::toString).map(String::trim).collect(Collectors.joining(""));
    }

    public static Function getFunctionModel(MethodDeclarationNode methodDeclarationNode,
                                            Map<String, Value> annotations) {
        Function functionModel = Function.getNewFunctionModel(SERVICE_DIAGRAM);
        annotations.forEach(functionModel.getProperties()::put);

        Value functionName = functionModel.getName();
        functionName.setValue(methodDeclarationNode.methodName().text().trim());
        functionName.setValueType(VALUE_TYPE_IDENTIFIER);

        Value accessor = functionModel.getAccessor();
        for (Token qualifier : methodDeclarationNode.qualifierList()) {
            String qualifierText = qualifier.text().trim();
            if (qualifierText.matches(REMOTE)) {
                functionModel.setKind(KIND_REMOTE);
            } else if (qualifierText.matches(RESOURCE)) {
                functionModel.setKind(KIND_RESOURCE);
                accessor.setValue(methodDeclarationNode.methodName().text().trim());
                functionName.setValue(getPath(methodDeclarationNode.relativeResourcePath()));
            }
        }
        FunctionSignatureNode functionSignatureNode = methodDeclarationNode.methodSignature();
        Optional<ReturnTypeDescriptorNode> returnTypeDesc = functionSignatureNode.returnTypeDesc();
        if (returnTypeDesc.isPresent()) {
            FunctionReturnType returnType = functionModel.getReturnType();
            returnType.setValue(returnTypeDesc.get().type().toString().trim());
        }
        SeparatedNodeList<ParameterNode> parameters = functionSignatureNode.parameters();
        List<Parameter> parameterModels = new ArrayList<>();
        parameters.forEach(parameterNode -> {
            Optional<Parameter> parameterModel = getParameterModel(parameterNode);
            parameterModel.ifPresent(parameterModels::add);
        });
        functionModel.setParameters(parameterModels);
        functionModel.setCodedata(new Codedata(methodDeclarationNode.lineRange()));
        return functionModel;
    }

    public static Function getFunctionModel(FunctionDefinitionNode functionDefinitionNode,
                                            Map<String, Value> annotations) {
        Function functionModel = Function.getNewFunctionModel(SERVICE_DIAGRAM);
        annotations.forEach(functionModel.getProperties()::put);
        functionModel.setKind(KIND_DEFAULT);
        Value functionName = functionModel.getName();
        functionName.setValue(functionDefinitionNode.functionName().text().trim());
        functionName.setValueType(VALUE_TYPE_IDENTIFIER);

        Value accessor = functionModel.getAccessor();
        for (Token qualifier : functionDefinitionNode.qualifierList()) {
            String qualifierText = qualifier.text().trim();
            if (qualifierText.matches(REMOTE)) {
                functionModel.setKind(KIND_REMOTE);
                break;
            } else if (qualifierText.matches(RESOURCE)) {
                functionModel.setKind(KIND_RESOURCE);
                accessor.setValue(functionDefinitionNode.functionName().text().trim());
                functionName.setValue(getPath(functionDefinitionNode.relativeResourcePath()));
                break;
            }
        }

        FunctionSignatureNode functionSignatureNode = functionDefinitionNode.functionSignature();
        Optional<ReturnTypeDescriptorNode> returnTypeDesc = functionSignatureNode.returnTypeDesc();
        if (returnTypeDesc.isPresent()) {
            FunctionReturnType returnType = functionModel.getReturnType();
            returnType.setValue(returnTypeDesc.get().type().toString().trim());
        }
        SeparatedNodeList<ParameterNode> parameters = functionSignatureNode.parameters();
        List<Parameter> parameterModels = new ArrayList<>();
        parameters.forEach(parameterNode -> {
            Optional<Parameter> parameterModel = getParameterModel(parameterNode);
            parameterModel.ifPresent(parameterModels::add);
        });
        functionModel.setParameters(parameterModels);
        functionModel.setCodedata(new Codedata(functionDefinitionNode.lineRange()));
        functionModel.setCanAddParameters(true);
        updateFunctionDocs(functionDefinitionNode, functionModel);
        updateAnnotationAttachmentProperty(functionDefinitionNode, functionModel);
        return functionModel;
    }

    public static boolean isInitFunction(FunctionDefinitionNode functionDefinitionNode) {
        return functionDefinitionNode.functionName().text().trim().equals(Constants.INIT);
    }

    public static boolean isInitFunction(MethodDeclarationNode functionDefinitionNode) {
        return functionDefinitionNode.methodName().text().trim().equals(Constants.INIT);
    }

    public static Optional<Parameter> getParameterModel(ParameterNode parameterNode) {
        if (parameterNode instanceof RequiredParameterNode parameter) {
            if (parameter.paramName().isEmpty()) {
                return Optional.empty();
            }
            String paramName = parameter.paramName().get().text().trim();
            Parameter parameterModel = createParameter(paramName, KIND_REQUIRED,
                    parameter.typeName().toString().trim());
            return Optional.of(parameterModel);
        } else if (parameterNode instanceof DefaultableParameterNode parameter) {
            if (parameter.paramName().isEmpty()) {
                return Optional.empty();
            }
            String paramName = parameter.paramName().get().text().trim();
            Parameter parameterModel = createParameter(paramName, KIND_DEFAULTABLE,
                    parameter.typeName().toString().trim());
            Value defaultValue = parameterModel.getDefaultValue();
            defaultValue.setValue(parameter.expression().toString().trim());
            defaultValue.setValueType(VALUE_TYPE_EXPRESSION);
            defaultValue.setEnabled(true);
            return Optional.of(parameterModel);
        }
        return Optional.empty();
    }


    private static Parameter createParameter(String paramName, String paramKind, String typeName) {
        Parameter parameterModel = Parameter.getNewFunctionParameter();
        parameterModel.setMetadata(new MetaData(paramName, paramName));
        parameterModel.setKind(paramKind);
        parameterModel.getType().setValue(typeName);
        parameterModel.getName().setValue(paramName);
        return parameterModel;
    }

    public static Optional<String> getPath(TypeDefinitionNode serviceTypeNode) {
        Optional<MetadataNode> metadata = serviceTypeNode.metadata();
        if (metadata.isEmpty()) {
            return Optional.empty();
        }
        Optional<AnnotationNode> httpServiceConfig = metadata.get().annotations().stream()
                .filter(annotation -> annotation.annotReference().toString().trim().equals(
                        Constants.TYPE_HTTP_SERVICE_CONFIG))
                .findFirst();
        if (httpServiceConfig.isEmpty()) {
            return Optional.empty();
        }
        Optional<MappingConstructorExpressionNode> mapExpr = httpServiceConfig.get().annotValue();
        if (mapExpr.isEmpty()) {
            return Optional.empty();
        }
        Optional<SpecificFieldNode> basePathField = mapExpr.get().fields().stream()
                .filter(fieldNode -> fieldNode.kind().equals(SyntaxKind.SPECIFIC_FIELD))
                .map(fieldNode -> (SpecificFieldNode) fieldNode)
                .filter(fieldNode -> fieldNode.fieldName().toString().trim()
                        .equals(Constants.BASE_PATH))
                .findFirst();
        if (basePathField.isEmpty()) {
            return Optional.empty();
        }
        Optional<ExpressionNode> valueExpr = basePathField.get().valueExpr();
        if (valueExpr.isPresent() && valueExpr.get().kind().equals(SyntaxKind.STRING_LITERAL)) {
            String value = ((BasicLiteralNode) valueExpr.get()).literalToken().text();
            return Optional.of(value.substring(1, value.length() - 1));
        }
        return Optional.empty();
    }

    public static Optional<Function> getFunctionModel(String serviceType, String functionNameOrType) {
        String resourcePath =  String.format("functions/%s_%s.json", serviceType.toLowerCase(Locale.US),
                functionNameOrType.toLowerCase(Locale.US));
        InputStream resourceStream = Utils.class.getClassLoader()
                .getResourceAsStream(resourcePath);
        if (resourceStream == null) {
            return Optional.empty();
        }

        try (JsonReader reader = new JsonReader(new InputStreamReader(resourceStream, StandardCharsets.UTF_8))) {
            return Optional.of(new Gson().fromJson(reader, Function.class));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public static void populateListenerInfo(Service serviceModel, ServiceDeclarationNode serviceNode) {
        SeparatedNodeList<ExpressionNode> expressions = serviceNode.expressions();
        int size = expressions.size();
        if (size == 1) {
            serviceModel.getListener().setValue(getListenerExprName(expressions.get(0)));
        } else if (size > 1) {
            for (int i = 0; i < size; i++) {
                ExpressionNode expressionNode = expressions.get(i);
                serviceModel.getListener().addValue(getListenerExprName(expressionNode));
            }
        }
    }

    public static void updateAnnotationAttachmentProperty(ServiceDeclarationNode serviceNode,
                                                          Service service) {
        Optional<MetadataNode> metadata = serviceNode.metadata();
        if (metadata.isEmpty()) {
            return;
        }

        metadata.get().annotations().forEach(annotationNode -> {
            if (annotationNode.annotValue().isEmpty()) {
                return;
            }
            String annotName = annotationNode.annotReference().toString().trim();
            String[] split = annotName.split(":");
            annotName = split[split.length - 1];
            String propertyName = ANNOT_PREFIX + annotName;
            if (service.getProperties().containsKey(propertyName)) {
                Value property = service.getProperties().get(propertyName);
                property.setValue(annotationNode.annotValue().get().toSourceCode().trim());
            }
        });
    }

    public static void updateAnnotationAttachmentProperty(FunctionDefinitionNode functionDef,
                                                          Function function) {
        Optional<MetadataNode> metadata = functionDef.metadata();
        if (metadata.isEmpty()) {
            return;
        }

        metadata.get().annotations().forEach(annotationNode -> {
            if (annotationNode.annotValue().isEmpty()) {
                return;
            }
            String annotName = annotationNode.annotReference().toString().trim();
            String[] split = annotName.split(":");
            annotName = split[split.length - 1];
            String propertyName = ANNOT_PREFIX + annotName;
            if (function.getProperties().containsKey(propertyName)) {
                Value property = function.getProperties().get(propertyName);
                property.setValue(annotationNode.annotValue().get().toSourceCode().trim());
            }
        });
    }

    public static void updateServiceDocs(ServiceDeclarationNode serviceNode, Service service) {
        Optional<MetadataNode> metadata = serviceNode.metadata();
        if (metadata.isEmpty()) {
            return;
        }
        Optional<Node> docString = metadata.get().documentationString();
        if (docString.isEmpty() || docString.get().kind() != SyntaxKind.MARKDOWN_DOCUMENTATION) {
            return;
        }
        MarkdownDocumentationNode docNode = (MarkdownDocumentationNode) docString.get();
        StringBuilder serviceDoc = new StringBuilder();
        for (Node documentationLine : docNode.documentationLines()) {
            if (CommonUtils.isMarkdownDocumentationLine(documentationLine)) {
                NodeList<Node> nodes = ((MarkdownDocumentationLineNode) documentationLine).documentElements();
                nodes.stream().forEach(node -> serviceDoc.append(node.toSourceCode()));
            }
        }
        service.getDocumentation().setValue(serviceDoc.toString().stripTrailing());
    }

    public static void updateFunctionDocs(FunctionDefinitionNode functionDef, Function function) {
        Optional<MetadataNode> metadata = functionDef.metadata();
        if (metadata.isEmpty()) {
            return;
        }
        Optional<Node> docString = metadata.get().documentationString();
        if (docString.isEmpty() || docString.get().kind() != SyntaxKind.MARKDOWN_DOCUMENTATION) {
            return;
        }
        String doc = getFunctionDesc(functionDef);
        function.getDocumentation().setValue(doc);
        function.getParameters().forEach(parameter -> {
            if (!parameter.getName().getValue().equals(GRAPHQL_CONTEXT) &&
                    !parameter.getName().getValue().equals(GRAPHQL_FIELD)) {
                String paramDesc = getParamDesc(functionDef, parameter.getName().getValue());
                parameter.getDocumentation().setValue(paramDesc);
            }
        });
    }

    private static String getFunctionDesc(FunctionDefinitionNode funcDefNode) {
        Optional<MetadataNode> metadata = funcDefNode.metadata();
        Optional<Node> docString = metadata.get().documentationString();
        MarkdownDocumentationNode docNode = (MarkdownDocumentationNode) docString.get();
        StringBuilder description = new StringBuilder();
        for (Node documentationLine : docNode.documentationLines()) {
            if (CommonUtils.isMarkdownDocumentationLine(documentationLine)) {
                NodeList<Node> nodes = ((MarkdownDocumentationLineNode) documentationLine).documentElements();
                nodes.stream().forEach(node -> description.append(node.toSourceCode()));
            }
        }
        return description.toString().stripTrailing();
    }

    private static String getParamDesc(FunctionDefinitionNode funcDefNode, String paramName) {
        Optional<MetadataNode> metadata = funcDefNode.metadata();
        Optional<Node> docString = metadata.get().documentationString();
        MarkdownDocumentationNode docNode = (MarkdownDocumentationNode) docString.get();
        StringBuilder paramDoc = new StringBuilder();
        for (Node documentationLine : docNode.documentationLines()) {
            if (documentationLine.kind() == SyntaxKind.MARKDOWN_PARAMETER_DOCUMENTATION_LINE) {
                MarkdownParameterDocumentationLineNode docLine =
                        (MarkdownParameterDocumentationLineNode) documentationLine;
                String name = docLine.parameterName().text().trim();
                NodeList<Node> nodes = docLine.documentElements();
                if (paramName.equals(name) && !nodes.isEmpty()) {
                    nodes.stream().forEach(node -> paramDoc.append(node.toSourceCode()));
                }
            }
        }
        return paramDoc.toString().stripTrailing();
    }

    private static String getListenerExprName(ExpressionNode expressionNode) {
        if (expressionNode instanceof NameReferenceNode nameReferenceNode) {
            return nameReferenceNode.toSourceCode().trim();
        } else if (expressionNode instanceof ExplicitNewExpressionNode explicitNewExpressionNode) {
            return explicitNewExpressionNode.toSourceCode().trim();
        }
        return "";
    }

    public static boolean isPresent(Function functionModel, Function newFunction) {
        return newFunction.getName().getValue().equals(functionModel.getName().getValue()) &&
                (Objects.isNull(newFunction.getAccessor()) || Objects.isNull(functionModel.getAccessor()) ||
                        newFunction.getAccessor().getValue().equals(functionModel.getAccessor().getValue()));
    }

    public static void updateValue(Value target, Value source) {
        if (Objects.isNull(target) || Objects.isNull(source)) {
            return;
        }
        target.setEnabled(source.isEnabledWithValue());
        target.setValue(source.getValue());
        target.setValueType(source.getValueType());
    }

    public static void updateValue(FunctionReturnType target, FunctionReturnType source) {
        if (Objects.isNull(target) || Objects.isNull(source)) {
            return;
        }
        target.setEnabled(source.isEnabledWithValue());
        target.setValue(source.getValue());
        target.setValueType(source.getValueType());
        if (Objects.nonNull(source.getResponses())) {
            target.setResponses(source.getResponses());
        }
    }

    public static List<String> getAnnotationEdits(Service service) {
        Map<String, Value> properties = service.getProperties();
        List<String> annots = new ArrayList<>();
        for (Map.Entry<String, Value> property : properties.entrySet()) {
            Value value = property.getValue();
            if (Objects.nonNull(value.getCodedata()) && Objects.nonNull(value.getCodedata().getType()) &&
                    value.getCodedata().getType().equals("ANNOTATION_ATTACHMENT") && value.isEnabledWithValue()) {
                String ref = getProtocol(service.getModuleName()) + ":" + value.getCodedata().getOriginalName();
                String annotTemplate = "@%s%s".formatted(ref, value.getValue());
                annots.add(annotTemplate);
            }
        }
        return annots;
    }

    public static List<String> getAnnotationEdits(Function function, Map<String, String> imports) {
        Map<String, Value> properties = function.getProperties().entrySet().stream()
                        .filter(entry -> entry.getKey().startsWith(ANNOT_PREFIX))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        List<String> annots = new ArrayList<>();
        if (properties.isEmpty()) {
            return annots;
        }
        for (Map.Entry<String, Value> property : properties.entrySet()) {
            Value value = property.getValue();
            if (Objects.nonNull(value.getCodedata()) && Objects.nonNull(value.getCodedata().getType()) &&
                    value.getCodedata().getType().equals("ANNOTATION_ATTACHMENT") && value.isEnabledWithValue()) {
                Codedata codedata = value.getCodedata();
                String ref = getProtocol(codedata.getModuleName()) + ":" + codedata.getOriginalName();
                String annotTemplate = "@%s%s".formatted(ref, value.getValue());
                annots.add(annotTemplate);
                if (Objects.nonNull(value.getImports())) {
                    imports.putAll(value.getImports());
                }
            }
        }
        return annots;
    }

    public static String getDocumentationEdits(Service service) {
        String docs = "";
        if (Objects.nonNull(service.getDocumentation()) && service.getDocumentation().getValue() != null) {
            String formatted = getFormattedDesc(service.getDocumentation().getValue());
            docs += formatted;
        }
        return docs;
    }

    public static String getDocumentationEdits(ServiceClass serviceClass) {
        String docs = "";
        if (Objects.nonNull(serviceClass.documentation()) && serviceClass.documentation().getValue() != null) {
            String formatted = getFormattedDesc(serviceClass.documentation().getValue());
            docs += formatted;
        }
        return docs;
    }

    public static String getDocumentationEdits(Function function) {
        String docEdits = "";
        if (Objects.nonNull(function.getDocumentation()) && function.getDocumentation().getValue() != null) {
            String formatted = getFormattedDesc(function.getDocumentation().getValue());
            docEdits = formatted.isEmpty() ? docEdits : formatted;
        }
        for (Parameter parameter : function.getParameters()) {
            Value doc = parameter.getDocumentation();
            if (Objects.nonNull(doc) && parameter.isEnabled() && doc.getValue() != null) {
                String formatted = getFormattedParamDesc(doc.getValue(), parameter.getName().getValue());
                docEdits = formatted.isEmpty() ? docEdits : docEdits + NEW_LINE + formatted;
            }
        }
        return docEdits;
    }

    public static String getFormattedDesc(String desc) {
        if (desc.isBlank()) {
            return "";
        }
        String doc = CommonUtils.convertToBalDocs(desc);
        return doc.stripTrailing();
    }

    public static String getFormattedParamDesc(String desc, String paramName) {
        if (desc.isBlank()) {
            return "";
        }
        StringBuilder docBuilder = new StringBuilder();
        String[] docs = desc.trim().split(NEW_LINE);
        String paramDoc = String.join(" ", docs);
        docBuilder.append("# + ").append(paramName).append(" - ").append(paramDoc);
        return docBuilder.toString();
    }

    public static void addServiceAnnotationTextEdits(Service service, ServiceDeclarationNode serviceNode,
                                                    List<TextEdit> edits) {
        Token serviceKeyword = serviceNode.serviceKeyword();

        List<String> annots = getAnnotationEdits(service);
        String annotEdit = String.join(System.lineSeparator(), annots);

        Optional<MetadataNode> metadata = serviceNode.metadata();
        if (metadata.isEmpty()) { // metadata is empty and service has annotations
            if (!annotEdit.isEmpty()) {
                annotEdit += System.lineSeparator();
                edits.add(new TextEdit(toRange(serviceKeyword.lineRange().startLine()), annotEdit));
            }
            return;
        }
        NodeList<AnnotationNode> annotations = metadata.get().annotations();
        if (annotations.isEmpty()) { // metadata is present but no annotations
            if (!annotEdit.isEmpty()) {
                annotEdit += System.lineSeparator();
                edits.add(new TextEdit(toRange(metadata.get().lineRange()), annotEdit));
            }
            return;
        }

        // first annotation end line range
        int size = annotations.size();
        LinePosition firstAnnotationEndLinePos = annotations.get(0).lineRange().startLine();

        // last annotation end line range
        LinePosition lastAnnotationEndLinePos = annotations.get(size - 1).lineRange().endLine();

        LineRange range = LineRange.from(serviceKeyword.lineRange().fileName(),
                firstAnnotationEndLinePos, lastAnnotationEndLinePos);

        edits.add(new TextEdit(toRange(range), annotEdit));
    }

    public static void addServiceDocTextEdits(Service service, ServiceDeclarationNode serviceNode,
                                              List<TextEdit> edits) {
        Token serviceKeyword = serviceNode.serviceKeyword();

        String docEdit = getDocumentationEdits(service);

        Optional<MetadataNode> metadata = serviceNode.metadata();
        if (metadata.isEmpty()) { // metadata is empty and the service has documentation
            if (!docEdit.isEmpty()) {
                docEdit += NEW_LINE;
                edits.add(new TextEdit(toRange(serviceKeyword.lineRange().startLine()), docEdit));
            }
            return;
        }

        Optional<Node> documentationString = metadata.get().documentationString();
        if (documentationString.isEmpty()) { // metadata is present but no documentation
            if (!docEdit.isEmpty()) {
                docEdit += NEW_LINE;
                edits.add(new TextEdit(toRange(metadata.get().lineRange()), docEdit));
            }
            return;
        }

        LinePosition docStartLinePos = documentationString.get().lineRange().startLine();
        LinePosition docEndLinePos = documentationString.get().lineRange().endLine();
        LineRange range = LineRange.from(serviceKeyword.lineRange().fileName(), docStartLinePos, docEndLinePos);
        edits.add(new TextEdit(toRange(range), docEdit));
    }

    public static void addFunctionAnnotationTextEdits(Function function, FunctionDefinitionNode functionDef,
                                                      List<TextEdit> edits, Map<String, String> imports) {
        Token firstToken = functionDef.qualifierList().isEmpty() ? functionDef.functionKeyword()
                : functionDef.qualifierList().get(0);

        List<String> annots = getAnnotationEdits(function, imports);
        String annotEdit = String.join(System.lineSeparator(), annots);

        Optional<MetadataNode> metadata = functionDef.metadata();
        if (metadata.isEmpty()) { // metadata is empty and service has annotations
            if (!annotEdit.isEmpty()) {
                annotEdit += System.lineSeparator();
                edits.add(new TextEdit(toRange(firstToken.lineRange().startLine()), annotEdit));
            }
            return;
        }
        NodeList<AnnotationNode> annotations = metadata.get().annotations();
        if (annotations.isEmpty()) { // metadata is present but no annotations
            if (!annotEdit.isEmpty()) {
                annotEdit += System.lineSeparator();
                edits.add(new TextEdit(toRange(firstToken.lineRange().startLine()), annotEdit));
            }
            return;
        }

        // first annotation end line range
        int size = annotations.size();
        LinePosition firstAnnotationEndLinePos = annotations.get(0).lineRange().startLine();

        // last annotation end line range
        LinePosition lastAnnotationEndLinePos = annotations.get(size - 1).lineRange().endLine();

        LineRange range = LineRange.from(firstToken.lineRange().fileName(),
                firstAnnotationEndLinePos, lastAnnotationEndLinePos);

        edits.add(new TextEdit(toRange(range), annotEdit));
    }

    public static void addFunctionDocTextEdits(Function function, FunctionDefinitionNode functionDef,
                                               List<TextEdit> edits) {
        Token firstToken = functionDef.qualifierList().isEmpty() ? functionDef.functionKeyword()
                : functionDef.qualifierList().get(0);
        String docEdit = getDocumentationEdits(function);
        Optional<MetadataNode> metadata = functionDef.metadata();
        if (metadata.isEmpty()) { // metadata is empty and the service has documentation
            if (!docEdit.isEmpty()) {
                docEdit += System.lineSeparator();
                edits.add(new TextEdit(toRange(firstToken.lineRange().startLine()), docEdit));
            }
            return;
        }

        Optional<Node> documentationString = metadata.get().documentationString();
        if (documentationString.isEmpty()) { // metadata is present but no documentation
            if (!docEdit.isEmpty()) {
                docEdit += System.lineSeparator();
                edits.add(new TextEdit(toRange(metadata.get().lineRange().startLine()), docEdit));
            }
            return;
        }

        LinePosition docStartLinePos = documentationString.get().lineRange().startLine();
        LinePosition docEndLinePos = documentationString.get().lineRange().endLine();
        LineRange range = LineRange.from(firstToken.lineRange().fileName(), docStartLinePos, docEndLinePos);
        edits.add(new TextEdit(toRange(range), docEdit));
    }

    public static String getValueString(Value value) {
        if (Objects.isNull(value)) {
            return "";
        }
        if (!value.isEnabledWithValue()) {
            return "";
        }
        if (!value.getValue().trim().isEmpty()) {
            return !Objects.isNull(value.getValueType()) && value.getValueType().equals("STRING") ?
                    String.format("\"%s\"", value.getValue()) : value.getValue();
        }
        Map<String, Value> properties = value.getProperties();
        if (Objects.isNull(properties)) {
            return "";
        }
        List<String> params = new ArrayList<>();
        properties.forEach((key, val) -> {
            if (val.isEnabledWithValue()) {
                params.add(String.format("%s: %s", key, getValueString(val)));
            }
        });
        return String.format("{%s}", String.join(", ", params));
    }

    public enum FunctionAddContext {
        HTTP_SERVICE_ADD,
        TCP_SERVICE_ADD,
        GRAPHQL_SERVICE_ADD,
        TRIGGER_ADD,
        FUNCTION_ADD,
        RESOURCE_ADD
    }

    public enum FunctionSignatureContext {
        FUNCTION_ADD,
        HTTP_RESOURCE_ADD,
        FUNCTION_UPDATE
    }

    public static String generateFunctionDefSource(Function function, List<String> statusCodeResponses,
                                                   FunctionAddContext addContext,
                                                   FunctionSignatureContext signatureContext,
                                                   Map<String, String> imports) {
        StringBuilder builder = new StringBuilder();
        String documentation = getDocumentationEdits(function);
        if (!documentation.isEmpty()) {
            builder.append(documentation).append(NEW_LINE);
        }

        List<String> functionAnnotations = getAnnotationEdits(function, imports);
        if (!functionAnnotations.isEmpty()) {
            builder.append(String.join(NEW_LINE, functionAnnotations)).append(NEW_LINE);
        }

        String functionQualifiers = getFunctionQualifiers(function);
        if (!functionQualifiers.isEmpty()) {
            builder.append(functionQualifiers).append(SPACE);
        }
        builder.append("function ");

        // function accessor
        Value accessor = function.getAccessor();
        if (function.getKind().equals(KIND_RESOURCE) && Objects.nonNull(accessor) && accessor.isEnabledWithValue()) {
            builder.append(getValueString(accessor).toLowerCase(Locale.ROOT)).append(SPACE);
        }
        if (function.getKind().equals(KIND_SUBSCRIPTION)) {
            builder.append(SUBSCRIBE).append(SPACE);
        }
        if (function.getKind().equals(KIND_QUERY)) {
            builder.append(GET).append(SPACE);
        }

        // function identifier
        builder.append(getValueString(function.getName()));

        FunctionSignatureContext sigContext = addContext.equals(FunctionAddContext.HTTP_SERVICE_ADD) ?
                FunctionSignatureContext.HTTP_RESOURCE_ADD : signatureContext;
        String functionSignature = generateFunctionSignatureSource(function, statusCodeResponses, sigContext, imports);
        builder.append(functionSignature);

        FunctionReturnType returnType = function.getReturnType();

        boolean hasErrorInReturn = returnType.hasError() || addContext.equals(FunctionAddContext.HTTP_SERVICE_ADD) ||
                signatureContext.equals(FunctionSignatureContext.HTTP_RESOURCE_ADD);

        if (!hasErrorInReturn && Objects.nonNull(returnType.getValue())) {
            List<String> returnParts = Arrays.stream(returnType.getValue().split("\\|")).toList();
            hasErrorInReturn = returnParts.contains("error") || returnParts.contains("error?");
        }


        // function body
        builder.append("{").append(NEW_LINE);
        if (hasErrorInReturn) {
            builder.append("\tdo {").append(NEW_LINE);
            builder.append("\t} on fail error err {")
                    .append(NEW_LINE)
                    .append("\t\t// handle error")
                    .append(NEW_LINE)
                    .append("\t\treturn error(\"unhandled error\", err);")
                    .append(NEW_LINE)
                    .append("\t}")
                    .append(NEW_LINE);
        }
        builder.append("}");
        return builder.toString();
    }

    public static String generateFunctionSignatureSource(Function function, List<String> statusCodeResponses,
                                                         FunctionSignatureContext context,
                                                         Map<String, String> imports) {
        StringBuilder builder = new StringBuilder();
        builder.append("(");
        builder.append(generateFunctionParamListSource(function.getParameters(), imports));
        builder.append(")");

        FunctionReturnType returnType = function.getReturnType();
        boolean addError = context.equals(FunctionSignatureContext.HTTP_RESOURCE_ADD);
        if (Objects.nonNull(returnType)) {
            if (returnType.isEnabledWithValue()) {
                builder.append(" returns ");
                String returnTypeStr = getValueString(returnType);
                if (addError && !returnTypeStr.contains("error")) {
                    returnTypeStr = "error|" + returnTypeStr;
                }
                builder.append(returnTypeStr);
                if (Objects.nonNull(returnType.getImports())) {
                    imports.putAll(returnType.getImports());
                }
            } else if (returnType.isEnabled() && Objects.nonNull(returnType.getResponses()) &&
                    !returnType.getResponses().isEmpty()) {
                List<String> responses = new ArrayList<>(returnType.getResponses().stream()
                        .filter(HttpResponse::isEnabled)
                        .map(response -> HttpUtil.getStatusCodeResponse(response, statusCodeResponses, imports))
                        .filter(Objects::nonNull)
                        .toList());
                if (!responses.isEmpty()) {
                    if (addError && !statusCodeResponses.contains("error") && !responses.contains("error")) {
                        responses.addFirst("error");
                    }
                    builder.append(" returns ");
                    builder.append(String.join("|", responses));
                }
            }
        }
        builder.append(SPACE);
        return builder.toString();
    }

    private static String generateFunctionParamListSource(List<Parameter> parameters, Map<String, String> imports) {
        // sort params list where required params come first
        parameters.sort(new Parameter.RequiredParamSorter());

        List<String> params = new ArrayList<>();
        parameters.forEach(param -> {
            if (param.isEnabled()) {
                String paramDef;
                Value defaultValue = param.getDefaultValue();
                if (Objects.nonNull(defaultValue) && defaultValue.isEnabled() &&
                        Objects.nonNull(defaultValue.getValue()) && !defaultValue.getValue().isEmpty()) {
                    Value paramType = param.getType();
                    paramDef = String.format("%s %s = %s", getValueString(paramType), getValueString(param.getName()),
                            getValueString(defaultValue));
                    if (Objects.nonNull(paramType.getImports())) {
                        imports.putAll(paramType.getImports());
                    }
                } else {
                    Value paramType = param.getType();
                    if (Objects.nonNull(paramType.getImports())) {
                        imports.putAll(paramType.getImports());
                    }
                    paramDef = String.format("%s %s", getValueString(paramType), getValueString(param.getName()));
                }
                if (Objects.nonNull(param.getHttpParamType()) && !param.getHttpParamType().equals("Query")) {
                    paramDef = String.format("@http:%s %s", param.getHttpParamType(), paramDef);
                }
                params.add(paramDef);
            }
        });
        return String.join(", ", params);
    }

    public static String getFunctionQualifiers(Function function) {
        List<String> qualifiers = function.getQualifiers();
        qualifiers = Objects.isNull(qualifiers) ? new ArrayList<>() : qualifiers;
        String kind = function.getKind();
        switch (kind) {
            case KIND_QUERY, KIND_SUBSCRIPTION,
                 KIND_RESOURCE ->
                    qualifiers.add(RESOURCE);
            case KIND_REMOTE, KIND_MUTATION ->
                    qualifiers.add(REMOTE);

            default -> {
            }
        }
        return String.join(" ", qualifiers);
    }

    /**
     * Checks whether the given import exists in the given module part node.
     *
     * @param node module part node
     * @param org organization name
     * @param module module name
     * @return true if the import exists, false otherwise
     */
    public static boolean importExists(ModulePartNode node, String org, String module) {
        return node.imports().stream().anyMatch(importDeclarationNode -> {
            String moduleName = importDeclarationNode.moduleName().stream()
                    .map(IdentifierToken::text)
                    .collect(Collectors.joining("."));
            return importDeclarationNode.orgName().isPresent() &&
                    org.equals(importDeclarationNode.orgName().get().orgName().text()) &&
                    module.equals(moduleName);
        });
    }

    /**
     * Generates the import statement for the given organization and module.
     *
     * @param org organization name
     * @param module module name
     * @return generated import statement
     */
    public static String getImportStmt(String org, String module) {
        return String.format(Constants.IMPORT_STMT_TEMPLATE, org, module);
    }

    public static boolean filterTriggers(TriggerProperty triggerProperty, TriggerListRequest request) {
        return (request == null) ||
                ((request.organization() == null || request.organization().equals(triggerProperty.orgName())) &&
                (request.packageName() == null || request.packageName().equals(triggerProperty.packageName())) &&
                (request.keyWord() == null || triggerProperty.keywords().stream()
                        .anyMatch(keyword -> keyword.equalsIgnoreCase(request.keyWord()))) &&
                (request.query() == null || triggerProperty.keywords().stream()
                        .anyMatch(keyword -> keyword.contains(request.query()))));
    }

    public static boolean expectsTriggerByName(TriggerRequest request) {
        return request.id() == null && request.organization() != null && request.packageName() != null;
    }

    public static String generateVariableIdentifier(SemanticModel semanticModel, Document document,
                                                    LinePosition linePosition, String prefix) {
        Set<String> names = semanticModel.visibleSymbols(document, linePosition).parallelStream()
                .filter(s -> s.getName().isPresent())
                .map(s -> s.getName().get())
                .collect(Collectors.toSet());
        return NameUtil.generateVariableName(prefix, names);
    }

    public static String generateTypeIdentifier(SemanticModel semanticModel, Document document,
                                                    LinePosition linePosition, String prefix) {
        Set<String> names = semanticModel.visibleSymbols(document, linePosition).parallelStream()
                .filter(s -> s.getName().isPresent())
                .map(s -> s.getName().get())
                .collect(Collectors.toSet());
        return NameUtil.generateTypeName(prefix, names);
    }

    public static String upperCaseFirstLetter(String value) {
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1).toLowerCase(Locale.ROOT);
    }

    public static String removeLeadingSingleQuote(String input) {
        if (input != null && input.startsWith("'")) {
            return input.substring(1);
        }
        return input;
    }
}
