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
import io.ballerina.flowmodelgenerator.core.model.PropertyTypeMemberInfo;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.ballerina.modelgenerator.commons.ParameterData.Kind.REQUIRED;

/**
 * Strategy for generating REST API call activities using ballerina/http.
 * Generates an activity function that creates an inline http:Client and invokes the specified HTTP method.
 *
 * @since 1.8.0
 */
public class RestActivityStrategy implements BuiltinActivityStrategy {

    // Property keys
    public static final String URL_KEY = "url";
    public static final String METHOD_KEY = "method";
    public static final String PAYLOAD_KEY = "payload";
    public static final String HEADERS_KEY = "headers";
    public static final String AUTH_CONFIG_KEY = "authConfig";

    // HTTP method options
    private static final String METHOD_GET = "GET";
    private static final String METHOD_POST = "POST";
    private static final String METHOD_PUT = "PUT";
    private static final String METHOD_DELETE = "DELETE";
    private static final String METHOD_PATCH = "PATCH";

    private static final String HTTP_PKG_INFO = "ballerina:http:2.16.0";
    private static final String HTTP_PKG_NAME = "http";
    private static final String RECORD_TYPE_KIND = "RECORD_TYPE";

    private static final String AUTH_BALLERINA_TYPE =
            "http:CredentialsConfig|http:BearerTokenConfig|http:JwtIssuerConfig"
                    + "|http:OAuth2ClientCredentialsGrantConfig|http:OAuth2PasswordGrantConfig"
                    + "|http:OAuth2RefreshTokenGrantConfig|http:OAuth2JwtBearerGrantConfig";

    private static final List<PropertyTypeMemberInfo> AUTH_TYPE_MEMBERS = List.of(
            new PropertyTypeMemberInfo("CredentialsConfig", HTTP_PKG_INFO, HTTP_PKG_NAME,
                    RECORD_TYPE_KIND, false),
            new PropertyTypeMemberInfo("BearerTokenConfig", HTTP_PKG_INFO, HTTP_PKG_NAME,
                    RECORD_TYPE_KIND, false),
            new PropertyTypeMemberInfo("JwtIssuerConfig", HTTP_PKG_INFO, HTTP_PKG_NAME,
                    RECORD_TYPE_KIND, false),
            new PropertyTypeMemberInfo("OAuth2ClientCredentialsGrantConfig", HTTP_PKG_INFO, HTTP_PKG_NAME,
                    RECORD_TYPE_KIND, false),
            new PropertyTypeMemberInfo("OAuth2PasswordGrantConfig", HTTP_PKG_INFO, HTTP_PKG_NAME,
                    RECORD_TYPE_KIND, false),
            new PropertyTypeMemberInfo("OAuth2RefreshTokenGrantConfig", HTTP_PKG_INFO, HTTP_PKG_NAME,
                    RECORD_TYPE_KIND, false),
            new PropertyTypeMemberInfo("OAuth2JwtBearerGrantConfig", HTTP_PKG_INFO, HTTP_PKG_NAME,
                    RECORD_TYPE_KIND, false)
    );

    @Override
    public void setFormProperties(NodeBuilder nodeBuilder, NodeBuilder.TemplateContext context) {
        // URL — required, TEXT mode (default) + EXPRESSION mode
        nodeBuilder.properties().custom()
                .metadata()
                    .label("URL")
                    .description("The full URL of the REST API endpoint (e.g., https://api.example.com/orders)")
                    .stepOut()
                .type().fieldType(Property.ValueType.TEXT).ballerinaType("string").selected(true).stepOut()
                .type().fieldType(Property.ValueType.EXPRESSION).ballerinaType("string").selected(false).stepOut()
                .codedata().kind(REQUIRED.name()).stepOut()
                .value("")
                .placeholder("https://api.example.com/orders")
                .editable(true)
                .stepOut()
                .addProperty(URL_KEY);

        // Method — DROPDOWN_CHOICE with dynamicFormFields for payload
        List<Option> methodOptions = List.of(
                new Option(METHOD_GET, METHOD_GET),
                new Option(METHOD_POST, METHOD_POST),
                new Option(METHOD_PUT, METHOD_PUT),
                new Option(METHOD_DELETE, METHOD_DELETE),
                new Option(METHOD_PATCH, METHOD_PATCH)
        );

        // Build payload sub-property for dynamicFormFields (shown when POST/PUT/PATCH selected)
        Property payloadSubProp = new Property.Builder<Void>(null)
                .metadata()
                    .label("Payload")
                    .description("Request body payload")
                    .stepOut()
                .type().fieldType(Property.ValueType.EXPRESSION)
                    .ballerinaType("http:RequestMessage").selected(true).stepOut()
                .value("")
                .editable(true)
                .build();

        Map<String, Map<String, Property>> methodDynamicFields = new LinkedHashMap<>();
        methodDynamicFields.put(METHOD_GET, Map.of());
        methodDynamicFields.put(METHOD_POST, Map.of(PAYLOAD_KEY, payloadSubProp));
        methodDynamicFields.put(METHOD_PUT, Map.of(PAYLOAD_KEY, payloadSubProp));
        methodDynamicFields.put(METHOD_DELETE, Map.of());
        methodDynamicFields.put(METHOD_PATCH, Map.of(PAYLOAD_KEY, payloadSubProp));

        nodeBuilder.properties().custom()
                .metadata()
                    .label("Method")
                    .description("HTTP method to use")
                    .stepOut()
                .type()
                    .fieldType(Property.ValueType.DROPDOWN_CHOICE)
                    .options(methodOptions)
                    .selected(true)
                    .stepOut()
                .codedata().kind(REQUIRED.name()).stepOut()
                .value(METHOD_GET)
                .editable(true)
                .itemOptions(ItemOption.from(methodOptions))
                .dynamicFormFields(methodDynamicFields)
                .stepOut()
                .addProperty(METHOD_KEY);

        // Hidden top-level payload property (for form value storage and code generation)
        nodeBuilder.properties().custom()
                .metadata()
                    .label("Payload")
                    .description("Request body payload (for POST, PUT, PATCH)")
                    .stepOut()
                .type().fieldType(Property.ValueType.EXPRESSION)
                    .ballerinaType("http:RequestMessage").selected(true).stepOut()
                .value("")
                .editable(true)
                .optional(true)
                .hidden(true)
                .stepOut()
                .addProperty(PAYLOAD_KEY);

        // Auth Config — optional, RECORD_MAP_EXPRESSION with all http auth config types
        nodeBuilder.properties().custom()
                .metadata()
                    .label("Authentication")
                    .description("HTTP client authentication configuration. "
                            + "Select an auth type (Basic, Bearer, OAuth2, etc.) to configure credentials.")
                    .stepOut()
                .type().fieldType(Property.ValueType.RECORD_MAP_EXPRESSION)
                    .ballerinaType(AUTH_BALLERINA_TYPE)
                    .typeMembers(AUTH_TYPE_MEMBERS)
                    .selected(false).stepOut()
                .type().fieldType(Property.ValueType.EXPRESSION)
                    .ballerinaType("http:ClientAuthConfig?")
                    .selected(false).stepOut()
                .placeholder("()")
                .defaultValue("()")
                .editable(true)
                .optional(true)
                .stepOut()
                .addProperty(AUTH_CONFIG_KEY);

        // Headers — optional, map<string|string[]>
        nodeBuilder.properties().custom()
                .metadata()
                    .label("Headers")
                    .description("HTTP request headers as a map expression")
                    .stepOut()
                .type().fieldType(Property.ValueType.EXPRESSION)
                    .ballerinaType("map<string|string[]>").selected(true).stepOut()
                .value("")
                .editable(true)
                .optional(true)
                .stepOut()
                .addProperty(HEADERS_KEY);
    }

    @Override
    public String generateActivityFunctionBody(SourceBuilder sourceBuilder) {
        Map<String, Property> properties = sourceBuilder.flowNode.properties();
        String method = getPropertyValue(properties, METHOD_KEY, METHOD_GET);
        String returnType = getPropertyValue(properties, Property.TYPE_KEY, "json");

        boolean hasPayload = isPayloadMethod(method)
                && !getPropertyValue(properties, PAYLOAD_KEY, "").isEmpty();
        boolean hasHeaders = !getPropertyValue(properties, HEADERS_KEY, "").isEmpty();

        String authConfig = getPropertyValue(properties, AUTH_CONFIG_KEY, "");

        StringBuilder body = new StringBuilder();

        // Build inline http:Client with optional auth config
        body.append("    http:Client httpClient = check new (url");
        if (!authConfig.isEmpty() && !"()".equals(authConfig)) {
            body.append(", {auth: ").append(authConfig).append("}");
        }
        body.append(");\n");

        // Remote method call
        body.append("    ").append(returnType).append(" response = check httpClient->")
                .append(method.toLowerCase()).append("(\"\"");

        if (isPayloadMethod(method)) {
            body.append(", ").append(hasPayload ? "payload" : "()");
        }
        if (hasHeaders) {
            body.append(", headers = headers");
        }
        body.append(");\n");
        body.append("    return response;\n");

        return body.toString();
    }

    @Override
    public String getActivityFunctionParams(SourceBuilder sourceBuilder) {
        Map<String, Property> properties = sourceBuilder.flowNode.properties();
        String method = getPropertyValue(properties, METHOD_KEY, METHOD_GET);

        List<String> params = new ArrayList<>();
        params.add("string url");

        // Payload: only for methods that use a body
        if (isPayloadMethod(method)) {
            params.add("http:RequestMessage payload");
        }

        // Headers: always optional
        params.add("map<string|string[]>? headers = ()");

        return String.join(", ", params);
    }

    @Override
    public String getActivityReturnType(SourceBuilder sourceBuilder) {
        Map<String, Property> properties = sourceBuilder.flowNode.properties();
        String returnType = getPropertyValue(properties, Property.TYPE_KEY, "json");
        return returnType + "|error";
    }

    @Override
    public List<String> getCallActivityArgs(SourceBuilder sourceBuilder) {
        Map<String, Property> properties = sourceBuilder.flowNode.properties();
        String method = getPropertyValue(properties, METHOD_KEY, METHOD_GET);

        List<String> args = new ArrayList<>();

        // url — top-level property
        String url = getPropertyValue(properties, URL_KEY, "");
        if (!url.isEmpty()) {
            args.add("url: " + url);
        }

        // payload — top-level property (only for POST/PUT/PATCH)
        if (isPayloadMethod(method)) {
            String payloadValue = getPropertyValue(properties, PAYLOAD_KEY, "");
            if (!payloadValue.isEmpty()) {
                args.add("payload: " + payloadValue);
            }
        }

        // headers — top-level property
        String headers = getPropertyValue(properties, HEADERS_KEY, "");
        if (!headers.isEmpty()) {
            args.add("headers: " + headers);
        }

        return args;
    }

    @Override
    public Set<String[]> getRequiredImports(SourceBuilder sourceBuilder) {
        Set<String[]> imports = new HashSet<>();
        imports.add(new String[]{"ballerina", "http"});
        return imports;
    }

    @Override
    public String getDefaultFunctionNamePrefix() {
        return "callRest";
    }

    @Override
    public void setPostProperties(NodeBuilder nodeBuilder, NodeBuilder.TemplateContext context) {
        // Databinding — TYPE field for response data binding (replaces generic "Return Type")
        nodeBuilder.properties().custom()
                .metadata()
                    .label("Databinding")
                    .description("Response data binding type (e.g., json, xml, record type)")
                    .stepOut()
                .value(getDefaultFormReturnType())
                .type()
                    .fieldType(Property.ValueType.TYPE)
                    .selected(true)
                    .stepOut()
                .editable(true)
                .stepOut()
                .addProperty(Property.TYPE_KEY);

        // Result variable name
        nodeBuilder.properties().data(Property.RESULT_NAME, context.getAllVisibleSymbolNames(),
                Property.RESULT_NAME, Property.RESULT_DOC, false);
    }

    @Override
    public String getLabel() {
        return "Call REST API";
    }

    @Override
    public String getDescription() {
        return "Call a REST API endpoint. Looking for more features?"
                + " Save and Edit Activity for more options.";
    }

    private boolean isPayloadMethod(String method) {
        return METHOD_POST.equalsIgnoreCase(method) || METHOD_PUT.equalsIgnoreCase(method)
                || METHOD_PATCH.equalsIgnoreCase(method);
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

