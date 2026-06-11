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
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;
import io.ballerina.modelgenerator.commons.FunctionData;
import org.eclipse.lsp4j.TextEdit;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static io.ballerina.flowmodelgenerator.core.Constants.Workflow.CALL_HUMAN_TASK_METHOD_NAME;
import static io.ballerina.flowmodelgenerator.core.Constants.Workflow.CONTEXT_CLASS_NAME;
import static io.ballerina.flowmodelgenerator.core.Constants.Workflow.HUMAN_TASK_DESCRIPTION;
import static io.ballerina.flowmodelgenerator.core.Constants.Workflow.HUMAN_TASK_LABEL;
import static io.ballerina.flowmodelgenerator.core.Constants.Workflow.WORKFLOW_MODULE;
import static io.ballerina.flowmodelgenerator.core.Constants.Workflow.WORKFLOW_ORG;
import static io.ballerina.modelgenerator.commons.ParameterData.Kind.DEFAULTABLE;
import static io.ballerina.modelgenerator.commons.ParameterData.Kind.REQUIRED;

/**
 * Represents a workflow human task node. Generates a {@code ctx->callHumanTask({...})} call
 * that blocks until a human completes the task or the optional timeout elapses.
 *
 * <p>Generated source example:
 * <pre>{@code
 * ApprovalDecision result = check ctx->callHumanTask({
 *     taskName: "approveExpense",
 *     title: "Approve order",
 *     userRoles: ["FINANCE_APPROVER"],
 *     payload: {"amount": 1200},
 *     timeout: {hours: 24}
 * });
 * }</pre>
 *
 * @since 1.9.0
 */
public class HumanTaskBuilder extends CallBuilder {

    public static final String LABEL = HUMAN_TASK_LABEL;
    public static final String DESCRIPTION = HUMAN_TASK_DESCRIPTION;
    public static final String DEFAULT_RETURN_TYPE = "anydata";

    // HumanTaskConfig field keys used as form property keys
    public static final String TASK_NAME_KEY = "taskName";
    public static final String TASK_NAME_LABEL = "Task Name";
    public static final String TASK_NAME_DOC =
            "Identifies the task type; used as the Temporal workflow type and child workflow ID";

    public static final String TITLE_KEY = "title";
    public static final String TITLE_LABEL = "Title";
    public static final String TITLE_DOC = "Short summary shown in the task inbox. Defaults to taskName when omitted";

    public static final String DESCRIPTION_KEY = "description";
    public static final String DESCRIPTION_FORM_LABEL = "Description";
    public static final String DESCRIPTION_DOC =
            "Additional context shown alongside the form. Optional";

    public static final String USER_ROLES_KEY = "userRoles";
    public static final String USER_ROLES_LABEL = "User Roles";
    public static final String USER_ROLES_DOC =
            "One or more roles permitted to complete this task. Defaults to [\"admin\"]";

    public static final String PAYLOAD_KEY = "payload";
    public static final String PAYLOAD_LABEL = "Payload";
    public static final String PAYLOAD_DOC =
            "Read-only JSON object rendered as key-value pairs next to the form";

    public static final String TIMEOUT_KEY = "timeout";
    public static final String TIMEOUT_LABEL = "Timeout";
    public static final String TIMEOUT_DOC =
            "Maximum time to wait. Omit (or pass ()) to wait indefinitely";

    public static final Set<String> EXCLUDED_SOURCE_KEYS = Set.of(
            Property.VARIABLE_KEY, Property.TYPE_KEY, Property.CHECK_ERROR_KEY);

    public static final String STRING_TYPE = "string";
    public static final String STRING_ARRAY_TYPE = "string[]";
    public static final String MAP_JSON_TYPE = "map<json>";
    public static final String DURATION_OPTIONAL_TYPE = "time:Duration?";

    @Override
    protected NodeKind getFunctionNodeKind() {
        return NodeKind.HUMAN_TASK;
    }

    @Override
    protected FunctionData.Kind getFunctionResultKind() {
        return FunctionData.Kind.REMOTE;
    }

    @Override
    public void setConcreteConstData() {
        metadata().label(LABEL).description(DESCRIPTION);
        codedata()
                .node(NodeKind.HUMAN_TASK)
                .org(WORKFLOW_ORG)
                .module(WORKFLOW_MODULE)
                .object(CONTEXT_CLASS_NAME)
                .symbol(CALL_HUMAN_TASK_METHOD_NAME);
    }

    @Override
    public void setConcreteTemplateData(TemplateContext context) {
        metadata().label(LABEL).description(DESCRIPTION);
        codedata()
                .node(NodeKind.HUMAN_TASK)
                .org(WORKFLOW_ORG)
                .module(WORKFLOW_MODULE)
                .object(CONTEXT_CLASS_NAME)
                .symbol(CALL_HUMAN_TASK_METHOD_NAME);

        // taskName – required
        properties().custom()
                .metadata()
                    .label(TASK_NAME_LABEL)
                    .description(TASK_NAME_DOC)
                    .stepOut()
                .type(Property.ValueType.EXPRESSION, STRING_TYPE)
                .codedata()
                    .kind(REQUIRED.name())
                .stepOut()
                .value("")
                .editable(true)
                .stepOut()
                .addProperty(TASK_NAME_KEY);

        // title – optional
        properties().custom()
                .metadata()
                    .label(TITLE_LABEL)
                    .description(TITLE_DOC)
                    .stepOut()
                .type(Property.ValueType.EXPRESSION, STRING_TYPE)
                .codedata()
                    .kind(DEFAULTABLE.name())
                .stepOut()
                .value("")
                .optional(true)
                .editable(true)
                .stepOut()
                .addProperty(TITLE_KEY);

        // description – optional
        properties().custom()
                .metadata()
                    .label(DESCRIPTION_FORM_LABEL)
                    .description(DESCRIPTION_DOC)
                    .stepOut()
                .type(Property.ValueType.EXPRESSION, STRING_TYPE)
                .codedata()
                    .kind(DEFAULTABLE.name())
                .stepOut()
                .value("")
                .optional(true)
                .editable(true)
                .stepOut()
                .addProperty(DESCRIPTION_KEY);

        // userRoles – defaultable (default: ["admin"])
        properties().custom()
                .metadata()
                    .label(USER_ROLES_LABEL)
                    .description(USER_ROLES_DOC)
                    .stepOut()
                .type(Property.ValueType.EXPRESSION, STRING_ARRAY_TYPE)
                .codedata()
                    .kind(DEFAULTABLE.name())
                .stepOut()
                .value("[\"admin\"]")
                .optional(true)
                .editable(true)
                .stepOut()
                .addProperty(USER_ROLES_KEY);

        // payload – defaultable (default: {})
        properties().custom()
                .metadata()
                    .label(PAYLOAD_LABEL)
                    .description(PAYLOAD_DOC)
                    .stepOut()
                .type(Property.ValueType.EXPRESSION, MAP_JSON_TYPE)
                .codedata()
                    .kind(DEFAULTABLE.name())
                .stepOut()
                .value("{}")
                .optional(true)
                .editable(true)
                .stepOut()
                .addProperty(PAYLOAD_KEY);

        // timeout – optional
        properties().custom()
                .metadata()
                    .label(TIMEOUT_LABEL)
                    .description(TIMEOUT_DOC)
                    .stepOut()
                .type(Property.ValueType.EXPRESSION, DURATION_OPTIONAL_TYPE)
                .codedata()
                    .kind(DEFAULTABLE.name())
                .stepOut()
                .value("")
                .optional(true)
                .editable(true)
                .stepOut()
                .addProperty(TIMEOUT_KEY);

        // result type
        properties().custom()
                .metadata()
                    .label("Result Type")
                    .description("The expected return type of the human task result")
                    .stepOut()
                .type()
                    .fieldType(Property.ValueType.TYPE)
                    .selected(true)
                    .stepOut()
                .value(DEFAULT_RETURN_TYPE)
                .editable(true)
                .stepOut()
                .addProperty(Property.TYPE_KEY);

        // result variable
        properties().data(Property.RESULT_NAME, context.getAllVisibleSymbolNames(),
                Property.RESULT_NAME, Property.RESULT_DOC, false);

        // checkError
        properties().checkError(true);
    }

    @Override
    public Map<Path, List<TextEdit>> toSource(SourceBuilder sourceBuilder) {
        FlowNode flowNode = sourceBuilder.flowNode;

        Optional<Property> typeProp = sourceBuilder.getProperty(Property.TYPE_KEY);
        Optional<Property> variableProp = sourceBuilder.getProperty(Property.VARIABLE_KEY);
        Optional<Property> checkErrorProp = sourceBuilder.getProperty(Property.CHECK_ERROR_KEY);

        String resultType = typeProp
                .map(p -> p.value() != null ? p.value().toString() : DEFAULT_RETURN_TYPE)
                .orElse(DEFAULT_RETURN_TYPE);
        String variableName = variableProp
                .map(p -> p.value() != null ? p.value().toString() : "result")
                .orElse("result");
        boolean useCheck = checkErrorProp
                .map(p -> p.value() == null || !"false".equals(p.value().toString()))
                .orElse(true);

        String ctxParamName = ActivityCallBuilder.resolveContextParamName(sourceBuilder);

        // LHS: T variableName =
        sourceBuilder.token()
                .name(useCheck ? resultType : resultType + "|error")
                .whiteSpace()
                .name(variableName)
                .whiteSpace()
                .keyword(SyntaxKind.EQUAL_TOKEN);

        if (useCheck) {
            sourceBuilder.token().keyword(SyntaxKind.CHECK_KEYWORD);
        }

        // ctx->callHumanTask(
        sourceBuilder.token()
                .name(ctxParamName)
                .keyword(SyntaxKind.RIGHT_ARROW_TOKEN)
                .name(CALL_HUMAN_TASK_METHOD_NAME)
                .keyword(SyntaxKind.OPEN_PAREN_TOKEN);

        // Build the HumanTaskConfig record literal, skipping blank/null values.
        // Reuse ActivityCallBuilder.populateActivityCallArg (which emits { key: val, ... })
        // after pre-filtering so that unfilled optional fields are omitted.
        Map<String, Property> configProps = new LinkedHashMap<>();
        flowNode.properties().forEach((k, v) -> {
            if (!EXCLUDED_SOURCE_KEYS.contains(k)) {
                Object val = v == null ? null : v.value();
                if (val != null && !val.toString().trim().isEmpty()) {
                    configProps.put(k, v);
                }
            }
        });
        ActivityCallBuilder.populateActivityCallArg(sourceBuilder, configProps, Set.of());

        sourceBuilder.token()
                .keyword(SyntaxKind.CLOSE_PAREN_TOKEN)
                .endOfStatement();

        return sourceBuilder
                .textEdit()
                .acceptImport(WORKFLOW_ORG, WORKFLOW_MODULE)
                .build();
    }
}
