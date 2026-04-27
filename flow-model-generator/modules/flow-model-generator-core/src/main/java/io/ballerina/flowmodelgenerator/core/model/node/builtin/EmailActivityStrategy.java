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

import java.util.HashSet;
import java.util.Set;

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

    @Override
    public void setFormProperties(NodeBuilder nodeBuilder, NodeBuilder.TemplateContext context) {
        // SMTP Host
        nodeBuilder.properties().custom()
                .metadata()
                    .label("SMTP Host")
                    .description("SMTP server address (e.g., smtp.gmail.com). "
                            + "Tip: use the expression helper to make this configurable.")
                    .stepOut()
                .type().fieldType(Property.ValueType.EXPRESSION).ballerinaType("string").selected(true).stepOut()
                .codedata().kind(REQUIRED.name()).stepOut()
                .value("")
                .editable(true)
                .stepOut()
                .addProperty(HOST_KEY);

        // SMTP Port
        nodeBuilder.properties().custom()
                .metadata()
                    .label("SMTP Port")
                    .description("SMTP port (465 for SSL, 587 for TLS). "
                            + "Tip: use the expression helper to make this configurable.")
                    .stepOut()
                .type().fieldType(Property.ValueType.EXPRESSION).ballerinaType("int").selected(true).stepOut()
                .codedata().kind(REQUIRED.name()).stepOut()
                .value("587")
                .editable(true)
                .stepOut()
                .addProperty(PORT_KEY);

        // SMTP Username
        nodeBuilder.properties().custom()
                .metadata()
                    .label("Username")
                    .description("SMTP username (email address). "
                            + "Tip: use the expression helper to make this configurable.")
                    .stepOut()
                .type().fieldType(Property.ValueType.EXPRESSION).ballerinaType("string").selected(true).stepOut()
                .codedata().kind(REQUIRED.name()).stepOut()
                .value("")
                .editable(true)
                .stepOut()
                .addProperty(SMTP_USERNAME_KEY);

        // SMTP Password
        nodeBuilder.properties().custom()
                .metadata()
                    .label("Password")
                    .description("SMTP password or app password. "
                            + "Tip: use the expression helper to make this configurable.")
                    .stepOut()
                .type().fieldType(Property.ValueType.EXPRESSION).ballerinaType("string").selected(true).stepOut()
                .codedata().kind(REQUIRED.name()).stepOut()
                .value("")
                .editable(true)
                .stepOut()
                .addProperty(SMTP_PASSWORD_KEY);

        // To
        nodeBuilder.properties().custom()
                .metadata()
                    .label("To")
                    .description("Recipient email address(es)")
                    .stepOut()
                .type().fieldType(Property.ValueType.EXPRESSION).ballerinaType("string").selected(true).stepOut()
                .codedata().kind(REQUIRED.name()).stepOut()
                .value("")
                .editable(true)
                .stepOut()
                .addProperty(TO_KEY);

        // Subject
        nodeBuilder.properties().custom()
                .metadata()
                    .label("Subject")
                    .description("Email subject line")
                    .stepOut()
                .type().fieldType(Property.ValueType.EXPRESSION).ballerinaType("string").selected(true).stepOut()
                .codedata().kind(REQUIRED.name()).stepOut()
                .value("")
                .editable(true)
                .stepOut()
                .addProperty(SUBJECT_KEY);

        // Body
        nodeBuilder.properties().custom()
                .metadata()
                    .label("Body")
                    .description("Email message content")
                    .stepOut()
                .type().fieldType(Property.ValueType.EXPRESSION).ballerinaType("string").selected(true).stepOut()
                .codedata().kind(REQUIRED.name()).stepOut()
                .value("")
                .editable(true)
                .stepOut()
                .addProperty(BODY_KEY);

        // From (optional, often matches username)
        nodeBuilder.properties().custom()
                .metadata()
                    .label("From")
                    .description("Sender email address (often matches username)")
                    .stepOut()
                .type().fieldType(Property.ValueType.EXPRESSION).ballerinaType("string").selected(true).stepOut()
                .value("")
                .editable(true)
                .optional(true)
                .stepOut()
                .addProperty(FROM_KEY);

        // CC (optional)
        nodeBuilder.properties().custom()
                .metadata()
                    .label("CC")
                    .description("Carbon copy email addresses")
                    .stepOut()
                .type().fieldType(Property.ValueType.EXPRESSION).ballerinaType("string").selected(true).stepOut()
                .value("")
                .editable(true)
                .optional(true)
                .stepOut()
                .addProperty(CC_KEY);

        // BCC (optional)
        nodeBuilder.properties().custom()
                .metadata()
                    .label("BCC")
                    .description("Blind carbon copy email addresses")
                    .stepOut()
                .type().fieldType(Property.ValueType.EXPRESSION).ballerinaType("string").selected(true).stepOut()
                .value("")
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
                .append("port);\n");

        body.append("    check emailClient->sendMessage({\n");
        body.append("        to: toAddress,\n");
        body.append("        subject: subject,\n");
        body.append("        body: body\n");
        body.append("    });\n");

        return body.toString();
    }

    @Override
    public String getActivityFunctionParams(SourceBuilder sourceBuilder) {
        return "string host, int port, string smtpUsername, string smtpPassword, "
                + "string toAddress, string subject, string body";
    }

    @Override
    public String getActivityReturnType(SourceBuilder sourceBuilder) {
        return "error?";
    }

    @Override
    public Set<String[]> getRequiredImports(SourceBuilder sourceBuilder) {
        Set<String[]> imports = new HashSet<>();
        imports.add(new String[]{"ballerina", "email"});
        return imports;
    }

    @Override
    public String getDefaultFunctionNamePrefix() {
        return "sendEmail";
    }

    @Override
    public String getLabel() {
        return "Send Email (SMTP)";
    }

    @Override
    public String getDescription() {
        return "Create a new workflow activity to send an email via SMTP.";
    }
}
