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

import io.ballerina.flowmodelgenerator.core.model.ItemOption;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.Option;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.ballerina.modelgenerator.commons.ParameterData.Kind.REQUIRED;

/**
 * Strategy for generating SOAP API call activities using ballerina/soap.
 * Supports SOAP 1.1 and SOAP 1.2 clients with SendReceive / SendOnly operations.
 *
 * @since 1.8.0
 */
public class SoapActivityStrategy implements BuiltinActivityStrategy {

    // Property keys
    public static final String ENDPOINT_URL_KEY = "endpointUrl";
    public static final String SOAP_VERSION_KEY = "soapVersion";
    public static final String OPERATION_KEY = "operation";
    public static final String ACTION_KEY = "action";
    public static final String BODY_KEY = "soapBody";
    public static final String HEADERS_KEY = "headers";
    public static final String PATH_KEY = "path";
    public static final String CLIENT_CONFIG_KEY = "clientConfig";

    // SOAP version options
    private static final String SOAP_11 = "SOAP 1.1";
    private static final String SOAP_12 = "SOAP 1.2";

    // Operation options
    private static final String OP_SEND_RECEIVE = "SendReceive";
    private static final String OP_SEND_ONLY = "SendOnly";

    @Override
    public void setFormProperties(NodeBuilder nodeBuilder, NodeBuilder.TemplateContext context) {
        // Endpoint URL — required, TEXT + EXPRESSION
        nodeBuilder.properties().custom()
                .metadata()
                    .label("Endpoint URL")
                    .description("The SOAP service endpoint URL (e.g., http://www.dneonline.com/calculator.asmx?WSDL)")
                    .stepOut()
                .type().fieldType(Property.ValueType.TEXT).ballerinaType("string").selected(true).stepOut()
                .type().fieldType(Property.ValueType.EXPRESSION).ballerinaType("string").selected(false).stepOut()
                .codedata().kind(REQUIRED.name()).stepOut()
                .value("")
                .placeholder("http://www.example.com/service?WSDL")
                .editable(true)
                .stepOut()
                .addProperty(ENDPOINT_URL_KEY);

        // SOAP Version — DROPDOWN_CHOICE (SOAP 1.1 / SOAP 1.2)
        // SOAP 1.1 requires action; SOAP 1.2 makes it optional
        List<Option> versionOptions = List.of(
                new Option(SOAP_11, SOAP_11),
                new Option(SOAP_12, SOAP_12)
        );

        // Action sub-property shown as required for SOAP 1.1
        Property actionRequiredSubProp = new Property.Builder<Void>(null)
                .metadata()
                    .label("Action")
                    .description("SOAPAction header value (required for SOAP 1.1)")
                    .stepOut()
                .type().fieldType(Property.ValueType.TEXT).ballerinaType("string").selected(true).stepOut()
                .type().fieldType(Property.ValueType.EXPRESSION).ballerinaType("string").selected(false).stepOut()
                .value("")
                .placeholder("http://tempuri.org/Add")
                .editable(true)
                .build();

        // Action sub-property shown as optional for SOAP 1.2
        Property actionOptionalSubProp = new Property.Builder<Void>(null)
                .metadata()
                    .label("Action")
                    .description("SOAPAction header value (optional for SOAP 1.2)")
                    .stepOut()
                .type().fieldType(Property.ValueType.TEXT).ballerinaType("string").selected(true).stepOut()
                .type().fieldType(Property.ValueType.EXPRESSION).ballerinaType("string").selected(false).stepOut()
                .value("")
                .placeholder("http://tempuri.org/Add")
                .editable(true)
                .optional(true)
                .build();

        Map<String, Map<String, Property>> versionDynamicFields = new LinkedHashMap<>();
        versionDynamicFields.put(SOAP_11, Map.of(ACTION_KEY, actionRequiredSubProp));
        versionDynamicFields.put(SOAP_12, Map.of(ACTION_KEY, actionOptionalSubProp));

        nodeBuilder.properties().custom()
                .metadata()
                    .label("SOAP Version")
                    .description("SOAP protocol version (1.1 or 1.2)")
                    .stepOut()
                .type()
                    .fieldType(Property.ValueType.DROPDOWN_CHOICE)
                    .options(versionOptions)
                    .selected(true)
                    .stepOut()
                .codedata().kind(REQUIRED.name()).stepOut()
                .value(SOAP_11)
                .editable(true)
                .itemOptions(ItemOption.from(versionOptions))
                .dynamicFormFields(versionDynamicFields)
                .stepOut()
                .addProperty(SOAP_VERSION_KEY);

        // Hidden top-level action property (for form value storage and code generation)
        nodeBuilder.properties().custom()
                .metadata()
                    .label("Action")
                    .description("SOAPAction header value")
                    .stepOut()
                .type().fieldType(Property.ValueType.TEXT).ballerinaType("string").selected(true).stepOut()
                .value("")
                .editable(true)
                .optional(true)
                .hidden(true)
                .stepOut()
                .addProperty(ACTION_KEY);

        // Operation — DROPDOWN_CHOICE (SendReceive / SendOnly)
        List<Option> operationOptions = List.of(
                new Option(OP_SEND_RECEIVE, OP_SEND_RECEIVE),
                new Option(OP_SEND_ONLY, OP_SEND_ONLY)
        );

        nodeBuilder.properties().custom()
                .metadata()
                    .label("Operation")
                    .description("SOAP operation type")
                    .stepOut()
                .type()
                    .fieldType(Property.ValueType.DROPDOWN_CHOICE)
                    .options(operationOptions)
                    .selected(true)
                    .stepOut()
                .codedata().kind(REQUIRED.name()).stepOut()
                .value(OP_SEND_RECEIVE)
                .editable(true)
                .itemOptions(ItemOption.from(operationOptions))
                .stepOut()
                .addProperty(OPERATION_KEY);

        // SOAP Body — required, EXPRESSION xml
        nodeBuilder.properties().custom()
                .metadata()
                    .label("SOAP Body")
                    .description("The XML SOAP envelope/body payload")
                    .stepOut()
                .type().fieldType(Property.ValueType.EXPRESSION).ballerinaType("xml").selected(true).stepOut()
                .codedata().kind(REQUIRED.name()).stepOut()
                .value("")
                .editable(true)
                .stepOut()
                .addProperty(BODY_KEY);

        // Headers — optional, map<string|string[]>
        nodeBuilder.properties().custom()
                .metadata()
                    .label("Headers")
                    .description("HTTP headers as a map expression")
                    .stepOut()
                .type().fieldType(Property.ValueType.EXPRESSION)
                    .ballerinaType("map<string|string[]>").selected(true).stepOut()
                .value("")
                .editable(true)
                .optional(true)
                .stepOut()
                .addProperty(HEADERS_KEY);

        // Path — optional, appended to endpoint URL
        nodeBuilder.properties().custom()
                .metadata()
                    .label("Path")
                    .description("Optional path to append to the endpoint URL")
                    .stepOut()
                .type().fieldType(Property.ValueType.TEXT).ballerinaType("string").selected(true).stepOut()
                .type().fieldType(Property.ValueType.EXPRESSION).ballerinaType("string").selected(false).stepOut()
                .value("")
                .editable(true)
                .optional(true)
                .stepOut()
                .addProperty(PATH_KEY);

        // Client Config — optional, EXPRESSION for soap client configuration (security, etc.)
        nodeBuilder.properties().custom()
                .metadata()
                    .label("Client Config")
                    .description("SOAP client configuration record (e.g., security settings with outboundSecurity"
                            + " and inboundSecurity)")
                    .stepOut()
                .type().fieldType(Property.ValueType.EXPRESSION)
                    .ballerinaType("record {}").selected(true).stepOut()
                .value("")
                .editable(true)
                .optional(true)
                .stepOut()
                .addProperty(CLIENT_CONFIG_KEY);
    }

    @Override
    public String generateActivityFunctionBody(SourceBuilder sourceBuilder) {
        Map<String, Property> properties = sourceBuilder.flowNode.properties();
        String soapVersion = getPropertyValue(properties, SOAP_VERSION_KEY, SOAP_11);
        String operation = getPropertyValue(properties, OPERATION_KEY, OP_SEND_RECEIVE);

        String clientModule = SOAP_12.equals(soapVersion) ? "soap12" : "soap11";
        boolean isSendOnly = OP_SEND_ONLY.equals(operation);

        StringBuilder body = new StringBuilder();

        // Create SOAP client
        String clientConfig = getPropertyValue(properties, CLIENT_CONFIG_KEY, "");
        body.append("    ").append(clientModule)
                .append(":Client soapClient = check new (endpointUrl");
        if (!clientConfig.isEmpty()) {
            body.append(", clientConfig");
        }
        body.append(");\n");

        // Build method call — action is always a parameter
        if (isSendOnly) {
            body.append("    check soapClient->sendOnly(soapBody");
        } else {
            body.append("    xml response = check soapClient->sendReceive(soapBody");
        }

        // Action is forwarded via the action parameter variable
        body.append(", action");

        // Headers parameter
        String headers = getPropertyValue(properties, HEADERS_KEY, "");
        if (!headers.isEmpty()) {
            body.append(", headers = headers");
        }

        // Path parameter
        String path = getPropertyValue(properties, PATH_KEY, "");
        if (!path.isEmpty()) {
            body.append(", path = path");
        }

        body.append(");\n");

        if (!isSendOnly) {
            body.append("    return response;\n");
        }

        return body.toString();
    }

    @Override
    public String getActivityFunctionParams(SourceBuilder sourceBuilder) {
        Map<String, Property> properties = sourceBuilder.flowNode.properties();
        String soapVersion = getPropertyValue(properties, SOAP_VERSION_KEY, SOAP_11);
        String clientModule = SOAP_12.equals(soapVersion) ? "soap12" : "soap11";

        List<String> params = new ArrayList<>();
        params.add("string endpointUrl");
        params.add("xml soapBody");
        params.add("string action = \"\"");
        params.add("map<string|string[]>? headers = ()");
        params.add("string? path = ()");
        params.add(clientModule + ":ClientConfiguration? clientConfig = ()");

        return String.join(", ", params);
    }

    @Override
    public String getActivityReturnType(SourceBuilder sourceBuilder) {
        Map<String, Property> properties = sourceBuilder.flowNode.properties();
        String operation = getPropertyValue(properties, OPERATION_KEY, OP_SEND_RECEIVE);
        if (OP_SEND_ONLY.equals(operation)) {
            return "error?";
        }
        return "xml|error";
    }

    @Override
    public List<String> getCallActivityArgs(SourceBuilder sourceBuilder) {
        Map<String, Property> properties = sourceBuilder.flowNode.properties();

        List<String> args = new ArrayList<>();

        // endpointUrl — quote if TEXT-typed
        addQuotedArg(args, "endpointUrl", properties, ENDPOINT_URL_KEY);

        // soapBody — always an expression, no quoting
        String soapBody = getPropertyValue(properties, BODY_KEY, "");
        if (!soapBody.isEmpty()) {
            args.add("soapBody: " + soapBody);
        }

        // action — quote if TEXT-typed
        addQuotedArg(args, "action", properties, ACTION_KEY);

        // headers — expression, no quoting
        String headers = getPropertyValue(properties, HEADERS_KEY, "");
        if (!headers.isEmpty()) {
            args.add("headers: " + headers);
        }

        // path — quote if TEXT-typed
        addQuotedArg(args, "path", properties, PATH_KEY);

        // clientConfig — expression, no quoting
        String clientConfig = getPropertyValue(properties, CLIENT_CONFIG_KEY, "");
        if (!clientConfig.isEmpty()) {
            args.add("clientConfig: " + clientConfig);
        }

        return args;
    }

    /**
     * Adds a named argument, quoting the value as a Ballerina string literal when the
     * property's active type is TEXT (i.e., the user entered plain text, not an expression).
     */
    private void addQuotedArg(List<String> args, String paramName,
                               Map<String, Property> properties, String propKey) {
        if (properties == null) {
            return;
        }
        Property prop = properties.get(propKey);
        if (prop == null || prop.value() == null || prop.value().toString().isEmpty()) {
            return;
        }
        String value = prop.value().toString();
        if (isTextSelected(prop)) {
            value = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        }
        args.add(paramName + ": " + value);
    }

    /**
     * Returns true when the currently-selected type for the property is TEXT
     * (meaning the raw user input must be wrapped in Ballerina string quotes).
     */
    private boolean isTextSelected(Property prop) {
        if (prop.types() == null) {
            return false;
        }
        return prop.types().stream()
                .filter(io.ballerina.flowmodelgenerator.core.model.PropertyType::selected)
                .findFirst()
                .map(t -> t.fieldType() == Property.ValueType.TEXT)
                .orElse(false);
    }

    @Override
    public List<Import> getRequiredImports(SourceBuilder sourceBuilder) {
        Map<String, Property> properties = sourceBuilder.flowNode.properties();
        String soapVersion = getPropertyValue(properties, SOAP_VERSION_KEY, SOAP_11);
        if (SOAP_12.equals(soapVersion)) {
            return List.of(new Import("ballerina", "soap.soap12"));
        }
        return List.of(new Import("ballerina", "soap.soap11"));
    }

    @Override
    public String getDefaultFunctionNamePrefix() {
        return "callSoap";
    }

    @Override
    public String getDefaultFormReturnType() {
        return "xml";
    }

    @Override
    public void setPostProperties(NodeBuilder nodeBuilder, NodeBuilder.TemplateContext context) {
        // SOAP: only variable name, no return type field (return type is determined by Operation)
        nodeBuilder.properties().data(Property.RESULT_NAME, context.getAllVisibleSymbolNames(),
                Property.RESULT_NAME, Property.RESULT_DOC, false);
    }

    @Override
    public String getLabel() {
        return "Call SOAP API";
    }

    @Override
    public String getDescription() {
        return "Create a new workflow activity to call a SOAP web service.";
    }

    private String getPropertyValue(Map<String, Property> properties, String key, String defaultValue) {
        if (properties == null) {
            return defaultValue;
        }
        Property prop = properties.get(key);
        if (prop != null && prop.value() != null && !prop.value().toString().isEmpty()) {
            return prop.value().toString();
        }
        return defaultValue;
    }
}
