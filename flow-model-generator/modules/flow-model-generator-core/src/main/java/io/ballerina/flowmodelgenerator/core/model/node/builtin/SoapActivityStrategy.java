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
import io.ballerina.flowmodelgenerator.core.model.Option;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.ballerina.modelgenerator.commons.ParameterData.Kind.REQUIRED;

/**
 * Strategy for generating SOAP API call activities using ballerina/soap.
 * Generates an activity function that creates an inline SOAP client and invokes sendReceive.
 *
 * @since 1.8.0
 */
public class SoapActivityStrategy implements BuiltinActivityStrategy {

    // Property keys
    public static final String ENDPOINT_URL_KEY = "endpointUrl";
    public static final String SOAP_VERSION_KEY = "soapVersion";
    public static final String WSS_USERNAME_KEY = "wssUsername";
    public static final String WSS_PASSWORD_KEY = "wssPassword";
    public static final String SOAP_ACTION_KEY = "soapAction";
    public static final String SOAP_BODY_KEY = "soapBody";

    // SOAP version options
    private static final String SOAP_11 = "1.1";
    private static final String SOAP_12 = "1.2";

    @Override
    public void setFormProperties(NodeBuilder nodeBuilder, NodeBuilder.TemplateContext context) {
        // Endpoint URL
        nodeBuilder.properties().custom()
                .metadata()
                    .label("Endpoint URL")
                    .description("The SOAP service endpoint URL")
                    .stepOut()
                .type().fieldType(Property.ValueType.EXPRESSION).ballerinaType("string").selected(true).stepOut()
                .codedata().kind(REQUIRED.name()).stepOut()
                .value("")
                .editable(true)
                .stepOut()
                .addProperty(ENDPOINT_URL_KEY);

        // SOAP Version
        nodeBuilder.properties().custom()
                .metadata()
                    .label("SOAP Version")
                    .description("SOAP protocol version")
                    .stepOut()
                .type()
                    .fieldType(Property.ValueType.SINGLE_SELECT)
                    .options(List.of(
                            new Option(SOAP_11, SOAP_11),
                            new Option(SOAP_12, SOAP_12)
                    ))
                    .selected(true)
                    .stepOut()
                .codedata().kind(REQUIRED.name()).stepOut()
                .value(SOAP_11)
                .editable(true)
                .stepOut()
                .addProperty(SOAP_VERSION_KEY);

        // WSS Username — optional; if both username and password are provided, WS-Security auth is used
        nodeBuilder.properties().custom()
                .metadata()
                    .label("WSS Username")
                    .description("WS-Security username for SOAP authentication (optional)")
                    .stepOut()
                .type().fieldType(Property.ValueType.EXPRESSION).ballerinaType("string").selected(true).stepOut()
                .value("")
                .editable(true)
                .optional(true)
                .stepOut()
                .addProperty(WSS_USERNAME_KEY);

        // WSS Password — optional; paired with username for WS-Security auth
        nodeBuilder.properties().custom()
                .metadata()
                    .label("WSS Password")
                    .description("WS-Security password for SOAP authentication (optional)")
                    .stepOut()
                .type().fieldType(Property.ValueType.EXPRESSION).ballerinaType("string").selected(true).stepOut()
                .value("")
                .editable(true)
                .optional(true)
                .stepOut()
                .addProperty(WSS_PASSWORD_KEY);

        // SOAP Action
        nodeBuilder.properties().custom()
                .metadata()
                    .label("SOAP Action")
                    .description("The SOAPAction header value (required for SOAP 1.1)")
                    .stepOut()
                .type().fieldType(Property.ValueType.EXPRESSION).ballerinaType("string").selected(true).stepOut()
                .codedata().kind(REQUIRED.name()).stepOut()
                .value("")
                .editable(true)
                .stepOut()
                .addProperty(SOAP_ACTION_KEY);

        // SOAP Body
        nodeBuilder.properties().custom()
                .metadata()
                    .label("SOAP Body")
                    .description("The XML SOAP body payload")
                    .stepOut()
                .type().fieldType(Property.ValueType.EXPRESSION).ballerinaType("xml").selected(true).stepOut()
                .codedata().kind(REQUIRED.name()).stepOut()
                .value("")
                .editable(true)
                .stepOut()
                .addProperty(SOAP_BODY_KEY);
    }

    @Override
    public String generateActivityFunctionBody(SourceBuilder sourceBuilder) {
        Map<String, Property> properties = sourceBuilder.flowNode.properties();
        String endpointUrl = getPropertyValue(properties, ENDPOINT_URL_KEY, "\"\"");
        String soapVersion = getPropertyValue(properties, SOAP_VERSION_KEY, SOAP_11);
        String soapAction = getPropertyValue(properties, SOAP_ACTION_KEY, "\"\"");
        String wssUsername = getPropertyValue(properties, WSS_USERNAME_KEY, "");
        String wssPassword = getPropertyValue(properties, WSS_PASSWORD_KEY, "");
        boolean hasWssAuth = !wssUsername.isEmpty() && !wssPassword.isEmpty();

        String clientType = SOAP_12.equals(soapVersion) ? "soap12" : "soap11";

        StringBuilder body = new StringBuilder();
        body.append("    ").append(clientType).append(":Client soapClient = check new (").append(endpointUrl);
        if (hasWssAuth) {
            body.append(", {security: {username: ").append(wssUsername)
                    .append(", password: ").append(wssPassword)
                    .append(", passwordType: ").append(clientType).append(":TEXT}}");
        }
        body.append(");\n");
        body.append("    xml response = check soapClient->sendReceive(soapBody, ").append(soapAction)
                .append(");\n");
        body.append("    return response;\n");

        return body.toString();
    }

    @Override
    public String getActivityFunctionParams(SourceBuilder sourceBuilder) {
        return "xml soapBody";
    }

    @Override
    public String getActivityReturnType(SourceBuilder sourceBuilder) {
        return "xml|error";
    }

    @Override
    public List<String> getConfigurableDeclarations(SourceBuilder sourceBuilder, String activityName) {
        return new ArrayList<>();
    }

    @Override
    public Set<String[]> getRequiredImports(SourceBuilder sourceBuilder) {
        Map<String, Property> properties = sourceBuilder.flowNode.properties();
        String soapVersion = getPropertyValue(properties, SOAP_VERSION_KEY, SOAP_11);

        Set<String[]> imports = new HashSet<>();
        if (SOAP_12.equals(soapVersion)) {
            imports.add(new String[]{"ballerina", "soap.soap12"});
        } else {
            imports.add(new String[]{"ballerina", "soap.soap11"});
        }
        return imports;
    }

    @Override
    public String getDefaultFunctionNamePrefix() {
        return "callSoap";
    }

    @Override
    public String getLabel() {
        return "Call SOAP API";
    }

    @Override
    public String getDescription() {
        return "Call a SOAP web service. Looking for more features?"
                + " Save and Edit Activity for more options.";
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
