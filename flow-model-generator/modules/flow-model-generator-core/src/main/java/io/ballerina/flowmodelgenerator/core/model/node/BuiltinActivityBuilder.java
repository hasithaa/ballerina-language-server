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
import io.ballerina.modelgenerator.commons.CommonUtils;
import io.ballerina.tools.text.LineRange;
import org.eclipse.lsp4j.TextEdit;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.ballerina.flowmodelgenerator.core.Constants.Workflow.WORKFLOW_MODULE;
import static io.ballerina.flowmodelgenerator.core.Constants.Workflow.WORKFLOW_ORG;
import static io.ballerina.flowmodelgenerator.core.model.node.ActivityCallBuilder.CALL_ACTIVITY_METHOD;
import static io.ballerina.flowmodelgenerator.core.model.node.ActivityCallBuilder.resolveContextParamName;

/**
 * Builder for builtin activity nodes (REST, SOAP, Email).
 *
 * <p>Each variant maps to a function in the {@code ballerina/workflow.activity} module:
 * REST → {@code callRestAPI}, SOAP → {@code callSoapAPI}, Email → {@code sendEmail}.
 * The builder emits a single statement of the form
 * {@code <T> <var> = check ctx->callActivity(activity:<symbol>, { connection: <conn>, ... });}
 * — no wrapper function is generated.</p>
 *
 * <p>The builder owns the shared form fields ({@code connection}, {@code Databinding} for REST,
 * result variable name, and {@code check}) so the UI is uniform across variants. The
 * variant-specific {@link BuiltinActivityStrategy} contributes only the API-specific fields
 * and named-argument entries.</p>
 *
 * @since 1.8.0
 */
public class BuiltinActivityBuilder extends NodeBuilder {

    public static final String LABEL = "Workflow Activity";
    public static final String DESCRIPTION = "Create a new workflow activity for common integrations";

    public static final String CHECK_ERROR_KEY = "checkError";

    /**
     * Sentinel placeholder value carried by the {@code connection} property in form
     * templates and treated as "no connection picked" at source-generation time. The UI
     * uses this to surface a "create new connection" shortcut.
     */
    private static final String NEW_CONNECTION_SENTINEL = "NEW_CONNECTION";

    // The activity module that hosts callRestAPI / callSoapAPI / sendEmail.
    private static final String ACTIVITY_PKG_MODULE = "workflow.activity";
    private static final String ACTIVITY_MODULE_PREFIX = "activity";

    private static final String DEFAULT_REST_DATABINDING = "json";
    private static final String SOAP_RESPONSE_TYPE = "xml";

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

        // Connection field — delegates to FormBuilder.connectionSelector so the BI extension
        // renders a connection picker filtered by the strategy's `searchNodesKind`. When the
        // user has no compatible connection, `connectors` surfaces inline "Add new ..."
        // buttons (HTTP / SMTP / SOAP 1.1 / SOAP 1.2) that open a create-connection overlay.
        properties().connectionSelector(NEW_CONNECTION_SENTINEL, strategy.searchNodesKind(),
                strategy.connectors());

        // Strategy-specific API fields
        strategy.setFormProperties(this, context);

        // Post-fields: REST gets databinding + result; SOAP gets result; Email gets nothing.
        addPostProperties(strategy, context);

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

    private void addPostProperties(BuiltinActivityStrategy strategy, TemplateContext context) {
        if (strategy instanceof RestActivityStrategy) {
            // Databinding — TYPE field forwarded to callRestAPI as the `t` argument
            properties().custom()
                    .metadata()
                        .label("Databinding")
                        .description("Response data binding type (e.g., json, xml, record type)")
                        .stepOut()
                    .value(DEFAULT_REST_DATABINDING)
                    .type()
                        .fieldType(Property.ValueType.TYPE)
                        .selected(true)
                        .stepOut()
                    .editable(true)
                    .stepOut()
                    .addProperty(Property.TYPE_KEY);

            properties().data(Property.RESULT_NAME, context.getAllVisibleSymbolNames(),
                    Property.RESULT_NAME, Property.RESULT_DOC, false);
        } else if (strategy instanceof SoapActivityStrategy) {
            // Return type is fixed (xml|error); only expose the result variable name.
            properties().data(Property.RESULT_NAME, context.getAllVisibleSymbolNames(),
                    Property.RESULT_NAME, Property.RESULT_DOC, false);
        }
        // Email returns error?; no result variable, no return-type field.
    }

    @Override
    public Map<Path, List<TextEdit>> toSource(SourceBuilder sourceBuilder) {
        BuiltinActivityStrategy strategy = resolveStrategy(sourceBuilder.flowNode.codedata());

        LineRange lineRange = sourceBuilder.flowNode.codedata().lineRange();
        if (lineRange == null) {
            throw new IllegalStateException("Line range is not available for the builtin activity node");
        }

        // ---- Resolve common form values ----
        Optional<Property> connectionProp = sourceBuilder.getProperty(Property.CONNECTION_KEY);
        String connection = connectionProp
                .map(p -> p.value() == null ? "" : p.value().toString())
                .orElse("");
        if (connection.isEmpty() || NEW_CONNECTION_SENTINEL.equals(connection)) {
            throw new IllegalStateException("A connection is required for the builtin activity. "
                    + "Pick a module-level final client from the Connection dropdown.");
        }

        Optional<Property> checkErrorProp = sourceBuilder.getProperty(CHECK_ERROR_KEY);
        boolean useCheck = checkErrorProp
                .map(p -> p.value() != null && "true".equals(p.value().toString()))
                .orElse(true);

        // Result variable name (only relevant when the activity has a return value)
        String variableName = sourceBuilder.getProperty(Property.VARIABLE_KEY)
                .map(p -> p.value() == null ? "result" : p.value().toString())
                .orElse("result");

        // ---- Determine LHS type / databinding / extra t arg for REST ----
        String lhsType;
        String databindingType = null;
        boolean hasReturnValue;
        if (strategy instanceof RestActivityStrategy) {
            databindingType = sourceBuilder.getProperty(Property.TYPE_KEY)
                    .map(p -> p.value() != null && !p.value().toString().isEmpty()
                            ? p.value().toString()
                            : DEFAULT_REST_DATABINDING)
                    .orElse(DEFAULT_REST_DATABINDING);
            lhsType = databindingType;
            hasReturnValue = true;
        } else if (strategy instanceof SoapActivityStrategy) {
            lhsType = SOAP_RESPONSE_TYPE;
            hasReturnValue = true;
        } else {
            lhsType = null;
            hasReturnValue = false;
        }

        // ---- Resolve workflow context parameter (adds one if missing) ----
        String ctxParamName = resolveContextParamName(sourceBuilder);

        // ---- Build the call statement ----
        // Pattern: <T> <var> = check ctx->callActivity(activity:<symbol>, { connection: <c>, ... });
        // Email pattern: check ctx->callActivity(activity:sendEmail, { ... });

        if (hasReturnValue) {
            if (useCheck) {
                sourceBuilder.token()
                        .name(lhsType)
                        .whiteSpace()
                        .name(variableName)
                        .whiteSpace()
                        .keyword(SyntaxKind.EQUAL_TOKEN);
            } else {
                // Without check, the call returns T|error; emit `T|error <var> =`
                sourceBuilder.token()
                        .name(lhsType + "|error")
                        .whiteSpace()
                        .name(variableName)
                        .whiteSpace()
                        .keyword(SyntaxKind.EQUAL_TOKEN);
            }
        }
        // For !hasReturnValue (Email): if useCheck, emit `check ...;`; otherwise just call.

        if (useCheck) {
            sourceBuilder.token().keyword(SyntaxKind.CHECK_KEYWORD);
        }

        sourceBuilder.token()
                .name(ctxParamName)
                .keyword(SyntaxKind.RIGHT_ARROW_TOKEN)
                .name(CALL_ACTIVITY_METHOD)
                .keyword(SyntaxKind.OPEN_PAREN_TOKEN)
                .name(ACTIVITY_MODULE_PREFIX)
                .keyword(SyntaxKind.COLON_TOKEN)
                .name(strategy.activityFunctionSymbol())
                .keyword(SyntaxKind.COMMA_TOKEN);

        // Args record: { connection: <c>, <strategy args> }
        // NOTE: callRestAPI has `typedesc<anydata> t = <>` but this is a contextual-inference
        // parameter — the type is inferred from the LHS assignment (e.g. `json result = check
        // ctx->callActivity(...)`) and must NOT be passed explicitly in the args record.
        List<String> argEntries = new ArrayList<>();
        argEntries.add("connection: " + connection);
        argEntries.addAll(strategy.getCallActivityArgs(sourceBuilder));

        sourceBuilder.token()
                .keyword(SyntaxKind.OPEN_BRACE_TOKEN)
                .name(String.join(", ", argEntries))
                .keyword(SyntaxKind.CLOSE_BRACE_TOKEN)
                .keyword(SyntaxKind.CLOSE_PAREN_TOKEN)
                .endOfStatement();

        sourceBuilder.textEdit(SourceBuilder.SourceKind.STATEMENT,
                sourceBuilder.filePath, CommonUtils.toRange(lineRange));

        // ---- Imports ----
        sourceBuilder.acceptImport(WORKFLOW_ORG, WORKFLOW_MODULE);
        sourceBuilder.acceptImport(WORKFLOW_ORG, ACTIVITY_PKG_MODULE);
        for (BuiltinActivityStrategy.Import imp : strategy.getRequiredImports(sourceBuilder)) {
            sourceBuilder.acceptImport(imp.org(), imp.module());
        }

        return sourceBuilder.build();
    }

    /**
     * Returns the strategy instance for the given builtin symbol ({@code "REST"},
     * {@code "SOAP"}, or {@code "EMAIL"}), or {@code null} if the symbol is not recognised.
     * Used by {@code CodeAnalyzer} when re-populating diagram node properties from source.
     */
    public static BuiltinActivityStrategy getStrategy(String symbol) {
        return symbol != null ? STRATEGY_MAP.get(symbol) : null;
    }

    private BuiltinActivityStrategy resolveStrategy(Codedata codedata) {
        String symbol = codedata != null ? codedata.symbol() : null;
        if (symbol == null || !STRATEGY_MAP.containsKey(symbol)) {
            throw new IllegalStateException("Unknown builtin activity type: " + symbol);
        }
        return STRATEGY_MAP.get(symbol);
    }
}
