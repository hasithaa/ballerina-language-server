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

package io.ballerina.flowmodelgenerator.core.model.node.builtin;

import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.ballerina.modelgenerator.commons.ParameterData.Kind.REQUIRED;

/**
 * Strategy for generating email sending activities using ballerina/email.
 * Generates an activity function that creates an inline SmtpClient and sends a message.
 *
 * @since 1.8.0
 */
public class EmailActivityStrategy implements BuiltinActivityStrategy {

    // Property keys
    public static final String HOST_KEY = "host";
    public static final String PORT_KEY = "port";
    public static final String SMTP_USERNAME_KEY = "smtpUsername";
    public static final String SMTP_PASSWORD_KEY = "smtpPassword";
    public static final String TO_KEY = "toAddress";
    public static final String SUBJECT_KEY = "subject";
    public static final String BODY_KEY = "body";
    public static final String FROM_KEY = "fromAddress";
    public static final String CC_KEY = "cc";
    public static final String BCC_KEY = "bcc";

    private static final String STRING_TYPE = "string";
    private static final String STRING_ARRAY_TYPE = "string[]";
    private static final String INT_TYPE = "int";
    private static final String EMPTY_VALUE = "";
    private static final String DEFAULT_SMTP_PORT = "587";

    private static final String SMTP_HOST_LABEL = "SMTP Host";
    private static final String SMTP_HOST_DESCRIPTION = "SMTP server address (e.g., smtp.gmail.com). "
            + "Tip: use the expression helper to make this configurable.";
    private static final String SMTP_PORT_LABEL = "SMTP Port";
    private static final String SMTP_PORT_DESCRIPTION = "SMTP port (465 for SSL, 587 for TLS). "
            + "Tip: use the expression helper to make this configurable.";
    private static final String USERNAME_LABEL = "Username";
    private static final String USERNAME_DESCRIPTION = "SMTP username (email address). "
            + "Tip: use the expression helper to make this configurable.";
    private static final String PASSWORD_LABEL = "Password";
    private static final String PASSWORD_DESCRIPTION = "SMTP password or app password. "
            + "Tip: use the expression helper to make this configurable.";
    private static final String TO_LABEL = "To";
    private static final String TO_DESCRIPTION = "Recipient email addresses (one or more)";
    private static final String SUBJECT_LABEL = "Subject";
    private static final String SUBJECT_DESCRIPTION = "Email subject line";
    private static final String BODY_LABEL = "Body";
    private static final String BODY_DESCRIPTION = "Email message content";
    private static final String FROM_LABEL = "From";
    private static final String FROM_DESCRIPTION = "Sender email address (often matches username)";
    private static final String CC_LABEL = "CC";
    private static final String CC_DESCRIPTION = "Carbon copy email addresses";
    private static final String BCC_LABEL = "BCC";
    private static final String BCC_DESCRIPTION = "Blind carbon copy email addresses";

    private static final String EMAIL_PARAMS = "string host, int port, string smtpUsername, string smtpPassword, "
            + "string[] toAddress, string subject, string body, "
            + "string? fromAddress = (), string[] cc = [], string[] bcc = []";
    private static final String EMAIL_RETURN_TYPE = "error?";
    private static final String DEFAULT_FUNCTION_NAME_PREFIX = "sendEmail";
    private static final String STRATEGY_LABEL = "Send Email (SMTP)";
    private static final String STRATEGY_DESCRIPTION =
            "Create a new workflow activity to send an email via SMTP.";
    private static final String EMAIL_IMPORT_ORG = "ballerina";
    private static final String EMAIL_IMPORT_MODULE = "email";

    @Override
    public void setFormProperties(NodeBuilder nodeBuilder, NodeBuilder.TemplateContext context) {
        // SMTP Host
        nodeBuilder.properties().custom()
                .metadata()
                .label(SMTP_HOST_LABEL)
                .description(SMTP_HOST_DESCRIPTION)
                    .stepOut()
            .type().fieldType(Property.ValueType.EXPRESSION).ballerinaType(STRING_TYPE).selected(true).stepOut()
                .codedata().kind(REQUIRED.name()).stepOut()
            .value(EMPTY_VALUE)
                .editable(true)
                .stepOut()
                .addProperty(HOST_KEY);

        // SMTP Port
        nodeBuilder.properties().custom()
                .metadata()
                .label(SMTP_PORT_LABEL)
                .description(SMTP_PORT_DESCRIPTION)
                    .stepOut()
            .type().fieldType(Property.ValueType.EXPRESSION).ballerinaType(INT_TYPE).selected(true).stepOut()
                .codedata().kind(REQUIRED.name()).stepOut()
            .value(DEFAULT_SMTP_PORT)
                .editable(true)
                .stepOut()
                .addProperty(PORT_KEY);

        // SMTP Username
        nodeBuilder.properties().custom()
                .metadata()
                .label(USERNAME_LABEL)
                .description(USERNAME_DESCRIPTION)
                    .stepOut()
            .type().fieldType(Property.ValueType.EXPRESSION).ballerinaType(STRING_TYPE).selected(true).stepOut()
                .codedata().kind(REQUIRED.name()).stepOut()
            .value(EMPTY_VALUE)
                .editable(true)
                .stepOut()
                .addProperty(SMTP_USERNAME_KEY);

        // SMTP Password
        nodeBuilder.properties().custom()
                .metadata()
                .label(PASSWORD_LABEL)
                .description(PASSWORD_DESCRIPTION)
                    .stepOut()
            .type().fieldType(Property.ValueType.EXPRESSION).ballerinaType(STRING_TYPE).selected(true).stepOut()
                .codedata().kind(REQUIRED.name()).stepOut()
            .value(EMPTY_VALUE)
                .editable(true)
                .stepOut()
                .addProperty(SMTP_PASSWORD_KEY);

        // To
        nodeBuilder.properties().custom()
                .metadata()
                    .label(TO_LABEL)
                    .description(TO_DESCRIPTION)
                    .stepOut()
                .type().fieldType(Property.ValueType.EXPRESSION).ballerinaType(STRING_ARRAY_TYPE)
                .selected(true).stepOut()
                .codedata().kind(REQUIRED.name()).stepOut()
                .value(EMPTY_VALUE)
                .editable(true)
                .stepOut()
                .addProperty(TO_KEY);

        // Subject
        nodeBuilder.properties().custom()
                .metadata()
                    .label(SUBJECT_LABEL)
                    .description(SUBJECT_DESCRIPTION)
                    .stepOut()
                .type().fieldType(Property.ValueType.EXPRESSION).ballerinaType(STRING_TYPE).selected(true).stepOut()
                .codedata().kind(REQUIRED.name()).stepOut()
                .value(EMPTY_VALUE)
                .editable(true)
                .stepOut()
                .addProperty(SUBJECT_KEY);

        // Body
        nodeBuilder.properties().custom()
                .metadata()
                    .label(BODY_LABEL)
                    .description(BODY_DESCRIPTION)
                    .stepOut()
                .type().fieldType(Property.ValueType.EXPRESSION).ballerinaType(STRING_TYPE).selected(true).stepOut()
                .codedata().kind(REQUIRED.name()).stepOut()
                .value(EMPTY_VALUE)
                .editable(true)
                .stepOut()
                .addProperty(BODY_KEY);

        // From (optional, often matches username)
        nodeBuilder.properties().custom()
                .metadata()
                    .label(FROM_LABEL)
                    .description(FROM_DESCRIPTION)
                    .stepOut()
                .type().fieldType(Property.ValueType.EXPRESSION).ballerinaType(STRING_TYPE).selected(true).stepOut()
                .value(EMPTY_VALUE)
                .editable(true)
                .optional(true)
                .stepOut()
                .addProperty(FROM_KEY);

        // CC (optional)
        nodeBuilder.properties().custom()
                .metadata()
                    .label(CC_LABEL)
                    .description(CC_DESCRIPTION)
                    .stepOut()
                .type().fieldType(Property.ValueType.EXPRESSION).ballerinaType(STRING_ARRAY_TYPE)
                .selected(true).stepOut()
                .value(EMPTY_VALUE)
                .editable(true)
                .optional(true)
                .stepOut()
                .addProperty(CC_KEY);

        // BCC (optional)
        nodeBuilder.properties().custom()
                .metadata()
                    .label(BCC_LABEL)
                    .description(BCC_DESCRIPTION)
                    .stepOut()
                .type().fieldType(Property.ValueType.EXPRESSION).ballerinaType(STRING_ARRAY_TYPE)
                .selected(true).stepOut()
                .value(EMPTY_VALUE)
                .editable(true)
                .optional(true)
                .stepOut()
                .addProperty(BCC_KEY);
    }

    @Override
    public String generateActivityFunctionBody(SourceBuilder sourceBuilder) {
        StringBuilder body = new StringBuilder();
        body.append("    email:SmtpClient emailClient = check new (")
                .append("host, ")
                .append("smtpUsername, ")
                .append("smtpPassword, ")
                .append("{port: port});");
        body.append("\n");

        body.append("    check emailClient->sendMessage({\n");
        body.append("        to: toAddress,\n");
        body.append("        subject: subject,\n");
        body.append("        body: body,\n");
        body.append("        'from: fromAddress,\n");
        body.append("        cc: cc,\n");
        body.append("        bcc: bcc\n");
        body.append("    });\n");

        return body.toString();
    }

    @Override
    public String getActivityFunctionParams(SourceBuilder sourceBuilder) {
        return EMAIL_PARAMS;
    }

    @Override
    public String getActivityReturnType(SourceBuilder sourceBuilder) {
        return EMAIL_RETURN_TYPE;
    }

    @Override
    public boolean shouldAddPostProperties() {
        return false;
    }

    @Override
    public List<Import> getRequiredImports(SourceBuilder sourceBuilder) {
        return List.of(new Import(EMAIL_IMPORT_ORG, EMAIL_IMPORT_MODULE));
    }

    @Override
    public List<String> getCallActivityArgs(SourceBuilder sourceBuilder) {
        Map<String, Property> props = sourceBuilder.flowNode.properties();
        List<String> args = new ArrayList<>();
        addArg(args, HOST_KEY, "host", props);
        addArg(args, PORT_KEY, "port", props);
        addArg(args, SMTP_USERNAME_KEY, "smtpUsername", props);
        addArg(args, SMTP_PASSWORD_KEY, "smtpPassword", props);
        addArg(args, TO_KEY, "toAddress", props);
        addArg(args, SUBJECT_KEY, "subject", props);
        addArg(args, BODY_KEY, "body", props);
        addArg(args, FROM_KEY, "fromAddress", props);
        addArg(args, CC_KEY, "cc", props);
        addArg(args, BCC_KEY, "bcc", props);
        return args;
    }

    private void addArg(List<String> args, String propKey, String paramName,
                        Map<String, Property> properties) {
        if (properties == null) {
            return;
        }
        Property prop = properties.get(propKey);
        if (prop != null && prop.value() != null && !prop.value().toString().isEmpty()) {
            args.add(paramName + ": " + prop.value());
        }
    }

    @Override
    public String getDefaultFunctionNamePrefix() {
        return DEFAULT_FUNCTION_NAME_PREFIX;
    }

    @Override
    public String getLabel() {
        return STRATEGY_LABEL;
    }

    @Override
    public String getDescription() {
        return STRATEGY_DESCRIPTION;
    }
}
