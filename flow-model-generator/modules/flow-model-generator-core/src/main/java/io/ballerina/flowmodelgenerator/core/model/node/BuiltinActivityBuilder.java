/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com)
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

package io.ballerina.flowmodelgenerator.core.model.node;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.flowmodelgenerator.core.model.Codedata;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.builtin.BuiltinActivityStrategy;
import io.ballerina.flowmodelgenerator.core.model.node.builtin.EmailActivityStrategy;
import io.ballerina.flowmodelgenerator.core.model.node.builtin.RestActivityStrategy;
import io.ballerina.flowmodelgenerator.core.model.node.builtin.SoapActivityStrategy;
import io.ballerina.flowmodelgenerator.core.utils.FileSystemUtils;
import io.ballerina.flowmodelgenerator.core.utils.WorkflowUtil;
import io.ballerina.modelgenerator.commons.CommonUtils;
import io.ballerina.projects.Document;
import io.ballerina.tools.text.LineRange;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.ballerina.flowmodelgenerator.core.Constants.Workflow.DEFAULT_CTX_PARAM_NAME;
import static io.ballerina.flowmodelgenerator.core.Constants.Workflow.WORKFLOW_MODULE;
import static io.ballerina.flowmodelgenerator.core.Constants.Workflow.WORKFLOW_ORG;
import static io.ballerina.flowmodelgenerator.core.model.node.ActivityCallBuilder.CALL_ACTIVITY_METHOD;
import static io.ballerina.flowmodelgenerator.core.model.node.ActivityCallBuilder.addContextParameterToFunction;
import static io.ballerina.flowmodelgenerator.core.model.node.ActivityCallBuilder.getContextParamName;

/**
 * Builder for builtin activity nodes (REST, SOAP, Email).
 * Delegates form field definition and code generation to a {@link BuiltinActivityStrategy}
 * selected by the {@code codedata.symbol} value.
 *
 * <p>Generates two outputs:
 * <ol>
 *   <li>An {@code @workflow:Activity} annotated function with inline client code</li>
 *   <li>A {@code ctx->callActivity(activityName, args)} invocation at the insertion point</li>
 * </ol>
 *
 * @since 1.8.0
 */
public class BuiltinActivityBuilder extends NodeBuilder {

    public static final String LABEL = "Workflow Activity";
    public static final String DESCRIPTION = "Create a new workflow activity for common integrations";

    // Property keys used by the form
    public static final String ACTIVITY_NAME_KEY = "activityName";
    public static final String ACTIVITY_NAME_LABEL = "Activity Name";
    public static final String ACTIVITY_NAME_DOC = "Name of the generated activity function";
    public static final String CHECK_ERROR_KEY = "checkError";

    private static final Map<String, BuiltinActivityStrategy> STRATEGY_MAP = Map.of(
            "REST", new RestActivityStrategy(),
            "SOAP", new SoapActivityStrategy(),
            "EMAIL", new EmailActivityStrategy()
    );

    // Cached strategy for the current template instance — used so setConcreteConstData()
    // can re-apply the strategy-specific label/description on build() (which re-invokes
    // setConcreteConstData() and would otherwise reset them to the generic constants).
    private BuiltinActivityStrategy resolvedStrategy;

    @Override
    public void setConcreteConstData() {
        String label = resolvedStrategy != null ? resolvedStrategy.getLabel() : LABEL;
        String description = resolvedStrategy != null ? resolvedStrategy.getDescription() : DESCRIPTION;
        metadata().label(label).description(description);
        codedata().node(NodeKind.BUILTIN_ACTIVITY)
                .org(WORKFLOW_ORG)
                .module(WORKFLOW_MODULE);
    }

    @Override
    public void setConcreteTemplateData(TemplateContext context) {
        // Preserve activity type symbol so it round-trips through the form back to toSource()
        codedata().symbol(context.codedata().symbol());

        BuiltinActivityStrategy strategy = resolveStrategy(context.codedata());
        this.resolvedStrategy = strategy;

        metadata().label(strategy.getLabel()).description(strategy.getDescription());

        // Activity name field
        properties().functionNameTemplate(strategy.getDefaultFunctionNamePrefix(),
                context.getAllVisibleSymbolNames(), ACTIVITY_NAME_LABEL, ACTIVITY_NAME_DOC);

        // Delegate strategy-specific fields
        strategy.setFormProperties(this, context);

        // Delegate return type and variable name to strategy
        strategy.setPostProperties(this, context);

        // Check Error checkbox (default true — adds 'check' to propagate errors)
        properties().custom()
                .metadata()
                    .label("Check Error")
                    .description("Add 'check' to propagate errors. Uncheck to handle errors manually.")
                    .stepOut()
                .type().fieldType(Property.ValueType.FLAG).ballerinaType("boolean").selected(true).stepOut()
                .value("true")
                .editable(true)
                .optional(true)
                .stepOut()
                .addProperty(CHECK_ERROR_KEY);
    }

    @Override
    public Map<Path, List<TextEdit>> toSource(SourceBuilder sourceBuilder) {
        BuiltinActivityStrategy strategy = resolveStrategy(sourceBuilder.flowNode.codedata());

        // ---- Extract common properties ----
        Optional<Property> funcNameProp = sourceBuilder.getProperty(Property.FUNCTION_NAME_KEY);
        String activityName = funcNameProp
                .map(p -> p.value().toString())
                .orElseThrow(() -> new IllegalStateException("Activity name is required"));

        Optional<Property> variableProp = sourceBuilder.getProperty(Property.VARIABLE_KEY);
        String variableName = variableProp
                .map(p -> p.value().toString())
                .orElse("result");

        // Read return type from form property (TYPE_KEY), fall back to strategy default
        Optional<Property> typeProp = sourceBuilder.getProperty(Property.TYPE_KEY);
        String userReturnType = typeProp
                .map(p -> p.value() != null && !p.value().toString().isEmpty() ? p.value().toString() : null)
                .orElse(null);

        // Normalize: strip |error and standalone error/?  to find the actual base return type.
        // Examples: "json" → "json", "json|error" → "json", "error?" → "", "()|error" → "()"
        String returnType;
        if (userReturnType != null) {
            String[] typeParts = userReturnType.split("\\|");
            StringBuilder baseBuilder = new StringBuilder();
            for (String part : typeParts) {
                String p = part.trim().replaceAll("\\?$", "");
                if (!p.isEmpty() && !"error".equals(p)) {
                    if (baseBuilder.length() > 0) {
                        baseBuilder.append("|");
                    }
                    baseBuilder.append(p);
                }
            }
            String baseType = baseBuilder.toString().trim();
            if (!baseType.isEmpty() && !"()".equals(baseType)) {
                returnType = baseType + "|error";
            } else {
                returnType = "error?";
            }
        } else {
            returnType = strategy.getActivityReturnType(sourceBuilder);
        }
        boolean hasReturnValue = !"error?".equals(returnType);
        String params = strategy.getActivityFunctionParams(sourceBuilder);
        String functionBody = strategy.generateActivityFunctionBody(sourceBuilder);

        LineRange lineRange = sourceBuilder.flowNode.codedata().lineRange();
        if (lineRange == null) {
            throw new IllegalStateException("Line range is not available for the builtin activity node");
        }

        // ---- Step 1: Validate uniqueness BEFORE mutating sourceBuilder ----
        try {
            sourceBuilder.workspaceManager.loadProject(sourceBuilder.filePath);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load project: " + sourceBuilder.filePath, e);
        }
        SemanticModel semanticModel = FileSystemUtils.getSemanticModel(sourceBuilder.workspaceManager,
                sourceBuilder.filePath);

        boolean nameTaken = semanticModel.moduleSymbols().stream()
                .anyMatch(s -> s.getName().map(activityName::equals).orElse(false));
        if (nameTaken) {
            throw new IllegalStateException("Activity name '" + activityName
                    + "' already exists. Please choose a unique name.");
        }

        FunctionDefinitionNode functionNode = WorkflowUtil.findEnclosingWorkflowFunction(sourceBuilder);
        if (functionNode == null) {
            throw new IllegalStateException("Builtin activity call must be inside a workflow function");
        }

        Optional<String> optCtxParamName = getContextParamName(functionNode, semanticModel);
        String ctxParamName;
        if (optCtxParamName.isPresent()) {
            ctxParamName = optCtxParamName.get();
        } else {
            addContextParameterToFunction(sourceBuilder, functionNode);
            ctxParamName = DEFAULT_CTX_PARAM_NAME;
        }

        // ---- Step 2: Generate activity function definition ----
        StringBuilder declarationBuilder = new StringBuilder();

        // @workflow:Activity annotation + function definition
        declarationBuilder.append("@workflow:Activity\n");
        declarationBuilder.append("function ").append(activityName).append("(");
        declarationBuilder.append(params);
        declarationBuilder.append(") returns ").append(returnType).append(" {\n");
        declarationBuilder.append(functionBody);
        declarationBuilder.append("}\n");

        // Add the declaration as a text edit at end-of-file
        Path filePath = sourceBuilder.filePath;
        Document document = sourceBuilder.workspaceManager.document(filePath).orElse(null);
        int lastLine = 0;
        int lastCol = 0;
        if (document != null) {
            io.ballerina.tools.text.TextDocument textDoc = document.textDocument();
            int lineCount = textDoc.textLines().size();
            if (lineCount > 0) {
                lastLine = lineCount - 1;
                lastCol = textDoc.line(lastLine).length();
            }
        }
        Range endOfFileRange = CommonUtils.toRange(
                io.ballerina.tools.text.LinePosition.from(lastLine, lastCol));

        String declarationSource = "\n" + declarationBuilder;
        sourceBuilder.addTextEdit(filePath, new TextEdit(endOfFileRange, declarationSource));

        // ---- Step 3: Generate ctx->callActivity(activityName, args) at insertion point ----

        // Determine if check should be added
        Optional<Property> checkErrorProp = sourceBuilder.getProperty(CHECK_ERROR_KEY);
        boolean useCheck = checkErrorProp
                .map(p -> p.value() != null && "true".equals(p.value().toString()))
                .orElse(false);

        // Build the call statement
        if (useCheck && hasReturnValue) {
            // check mode with return value: <baseType> <variable> = check ctx->callActivity(...);
            sourceBuilder.token()
                    .name(returnType.replace("|error", ""))
                    .whiteSpace()
                    .name(variableName)
                    .whiteSpace()
                    .keyword(SyntaxKind.EQUAL_TOKEN);
        } else if (!useCheck && hasReturnValue) {
            // no check, return includes error: <returnType> <variable> = ctx->callActivity(...);
            sourceBuilder.token()
                    .name(returnType)
                    .whiteSpace()
                    .name(variableName)
                    .whiteSpace()
                    .keyword(SyntaxKind.EQUAL_TOKEN);
        }
        // For useCheck && !hasReturnValue: just check ctx->callActivity(...);
        // For !useCheck && !hasReturnValue: just ctx->callActivity(...);
        // (error? with no check — fire and forget, error is ignored)

        // Optionally add check keyword
        if (useCheck) {
            sourceBuilder.token().keyword(SyntaxKind.CHECK_KEYWORD);
        }
        sourceBuilder.token()
                .name(ctxParamName)
                .keyword(SyntaxKind.RIGHT_ARROW_TOKEN)
                .name(CALL_ACTIVITY_METHOD)
                .keyword(SyntaxKind.OPEN_PAREN_TOKEN)
                .name(activityName);

        // Add activity call arguments from form properties (execution params only)
        List<String> argEntries = strategy.getCallActivityArgs(sourceBuilder);
        if (!argEntries.isEmpty()) {
            sourceBuilder.token().keyword(SyntaxKind.COMMA_TOKEN);
            sourceBuilder.token().keyword(SyntaxKind.OPEN_BRACE_TOKEN);
            sourceBuilder.token().name(String.join(", ", argEntries));
            sourceBuilder.token().keyword(SyntaxKind.CLOSE_BRACE_TOKEN);
        }

        sourceBuilder.token()
                .keyword(SyntaxKind.CLOSE_PAREN_TOKEN)
                .endOfStatement();

        sourceBuilder.textEdit(SourceBuilder.SourceKind.STATEMENT,
                sourceBuilder.filePath, CommonUtils.toRange(lineRange));

        // ---- Step 4: Accept imports ----
        sourceBuilder.acceptImport(WORKFLOW_ORG, WORKFLOW_MODULE);
        List<BuiltinActivityStrategy.Import> requiredImports = strategy.getRequiredImports(sourceBuilder);
        for (BuiltinActivityStrategy.Import imp : requiredImports) {
            sourceBuilder.acceptImport(imp.org(), imp.module());
        }

        return sourceBuilder.build();
    }

    private BuiltinActivityStrategy resolveStrategy(Codedata codedata) {
        String symbol = codedata != null ? codedata.symbol() : null;
        if (symbol == null || !STRATEGY_MAP.containsKey(symbol)) {
            throw new IllegalStateException("Unknown builtin activity type: " + symbol);
        }
        return STRATEGY_MAP.get(symbol);
    }
}
